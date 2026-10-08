#!/usr/bin/env python3
"""Read-only structural lookup of active mixin target classes/methods/fields.
Does not execute Minecraft, replace runtime validation, or verify injection sites.
"""
import json,re,struct,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
project=ROOT/'forge-release-port'
archive=zipfile.ZipFile(ROOT/'official-minecraft/client-26.4-snapshot-3.jar')
cache={}
def class_info(name):
    name=name.replace('.','/') if '/' not in name else name
    if name in cache:return cache[name]
    try:b=archive.read(name+'.class')
    except KeyError:return None
    pos=8
    def u1():
        nonlocal pos
        v=b[pos];pos+=1;return v
    def u2():
        nonlocal pos
        v=struct.unpack_from('>H',b,pos)[0];pos+=2;return v
    def u4():
        nonlocal pos
        v=struct.unpack_from('>I',b,pos)[0];pos+=4;return v
    cp=[None]*u2();i=1
    while i<len(cp):
        tag=u1()
        if tag==1:
            n=u2();cp[i]=b[pos:pos+n].decode('utf8',errors='replace');pos+=n
        elif tag in (7,8,16,19,20):cp[i]=u2()
        elif tag in (3,4):pos+=4
        elif tag in (5,6):pos+=8;i+=1
        elif tag in (9,10,11,12,17,18):pos+=4
        elif tag==15:pos+=3
        else:raise ValueError(tag)
        i+=1
    u2();u2();super_idx=u2();super_name=cp[cp[super_idx]] if super_idx else None
    for _ in range(u2()):u2()
    def members():
        nonlocal pos
        values=[]
        for _ in range(u2()):
            access=u2();n=cp[u2()];descriptor=cp[u2()]
            values.append({'name':n,'descriptor':descriptor,'access':access})
            for _ in range(u2()):
                u2();length=u4();pos+=length
        return values
    fields=members();methods=members();result={'name':name,'super':super_name,'fields':fields,'methods':methods};cache[name]=result;return result

def annotation_blocks(source,name):
    for match in re.finditer(r'@'+re.escape(name)+r'\s*\(',source):
        start=match.end();i=start;depth=1;quoted=False;escape=False
        while i<len(source) and depth:
            char=source[i]
            if quoted:
                if escape:escape=False
                elif char=='\\':escape=True
                elif char=='"':quoted=False
            elif char=='"':quoted=True
            elif char=='(':depth+=1
            elif char==')':depth-=1
            i+=1
        yield source[start:i-1]

def hierarchy(info):
    while info:
        yield info
        info=class_info(info['super']) if info['super'] else None

def resolve_target(token,imports):
    if token.startswith('net.') or token.startswith('com.'):
        bits=token.split('.');first=next((i for i,x in enumerate(bits) if x[:1].isupper()),len(bits)-1)
        return '/'.join(bits[:first]+['$'.join(bits[first:])])
    first,*nested=token.split('.');base=imports.get(first)
    if base:return base.replace('.','/')+''.join('$'+part for part in nested)
    return None

mod=json.loads((project/'src/main/resources/fabric.mod.json').read_text())
records=[];problems=[]
for cfg in mod.get('mixins',[]):
    cfg=cfg if isinstance(cfg,str) else cfg['config'];data=json.loads((project/'src/main/resources'/cfg).read_text())
    for side in ['mixins','client','server']:
        for suffix in data.get(side,[]):
            name=data['package']+'.'+suffix;path=project/'src/main/java'/Path(name.replace('.','/')+'.java');source=path.read_text()
            imports={x.rsplit('.',1)[-1]:x for x in re.findall(r'^import\s+([\w.]+);',source,re.M)}
            targets=[]
            for block in annotation_blocks(source,'Mixin'):
                targets += [resolve_target(token,imports) for token in re.findall(r'([\w.$]+)\.class',block)]
                m=re.search(r'targets\s*=\s*(\{[^}]*\}|"[^"]+")',block)
                if m:targets += [x.replace('.','/') for x in re.findall(r'"([^"]+)"',m.group(1))]
            if not targets or None in targets:problems.append({'mixin':name,'problem':'Unresolved target syntax','targets':targets})
            infos=[class_info(target) for target in targets if target]
            for target,info in zip([t for t in targets if t],infos):
                if info is None:problems.append({'mixin':name,'problem':'Target class absent','target':target})
            checks=[]
            for annotation in ['Inject','Redirect','ModifyArg','ModifyArgs','ModifyVariable','ModifyConstant','ModifyExpressionValue','ModifyReturnValue','WrapOperation','WrapMethod']:
                for block in annotation_blocks(source,annotation):
                    m=re.search(r'\bmethod\s*=\s*(\{[^}]*\}|"[^"]+")',block)
                    if not m:continue
                    for selector in re.findall(r'"([^"]+)"',m.group(1)):
                        method=selector.split('(',1)[0];descriptor='('+selector.split('(',1)[1] if '(' in selector else None
                        found=[{'target':info['name'],'matches':[entry for entry in info['methods'] if entry['name']==method and (descriptor is None or entry['descriptor']==descriptor)]} for info in infos if info]
                        ok=all(item['matches'] for item in found) and bool(found)
                        checks.append({'annotation':annotation,'selector':selector,'found_on_each_direct_target':ok,'matches':found})
                        if not ok:problems.append({'mixin':name,'problem':'Injection selector absent on direct target','selector':selector})
            for annotation,kind in [('Accessor','fields'),('Invoker','methods')]:
                for block in annotation_blocks(source,annotation):
                    names=re.findall(r'"([^"]+)"',block)
                    if not names:continue
                    member=names[0];ok=all(any(entry['name']==member for owner in hierarchy(info) for entry in owner[kind]) for info in infos if info) and bool(infos)
                    checks.append({'annotation':annotation,'member':member,'found_in_target_hierarchy':ok})
                    if not ok:problems.append({'mixin':name,'problem':annotation+' member absent','member':member})
            for shadow in re.finditer(r'@Shadow(?:\s*\([^)]*\))?',source):
                end=source.find(';',shadow.end())
                if end<0:continue
                declaration=source[shadow.end():end]
                declaration=re.sub(r'@\w+(?:\([^)]*\))?', '', declaration).strip()
                if '(' in declaration:
                    member_match=re.search(r'(\w+)\s*\(',declaration)
                    kind='methods'
                else:
                    member_match=re.search(r'(\w+)\s*$',declaration)
                    kind='fields'
                if not member_match:continue
                member=member_match.group(1)
                if member.startswith('shadow$'):member=member[7:]
                found=[]
                for info in infos:
                    if not info:continue
                    found.append([{'owner':owner['name'],**entry} for owner in hierarchy(info) for entry in owner[kind] if entry['name']==member])
                ok=bool(found) and all(found)
                checks.append({'annotation':'Shadow','member':member,'kind':kind,'found_in_target_hierarchy':ok,'matches':found})
                if not ok:problems.append({'mixin':name,'problem':'Shadow member absent','member':member,'kind':kind})
            records.append({'mixin':name,'side':side,'targets':targets,'checks':checks})
report={'scope':'Read-only class/member existence. Shadow member names are checked; exact Shadow types, callbacks, invocation-site presence and actual Mixin application are NOT proven.','active_mixins':len(records),'checked_selectors':sum(len(x['checks']) for x in records),'problems':problems,'records':records}
(ROOT/'logs/mixin-target-structural-audit.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='records'},indent=2))
