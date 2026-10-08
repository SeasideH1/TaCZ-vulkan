import subprocess,re,json,hashlib,argparse
from pathlib import Path
parser=argparse.ArgumentParser(description="Read-only exact target member checks; not a Mixin transformation/boot test")
parser.add_argument("--javap", default="javap")
parser.add_argument("--minecraft-jar", type=Path, required=True)
parser.add_argument("--output", type=Path, default=Path("build/mixin-audit/descriptor-report.json"))
args=parser.parse_args()
j=args.javap
jar=args.minecraft_jar
checks=[
 ('net.minecraft.world.entity.LivingEntity','tick','()V'),
 ('net.minecraft.world.entity.LivingEntity','swing','(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z'),
 ('net.minecraft.world.entity.LivingEntity','actuallyHurt','(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V'),
 ('net.minecraft.world.entity.LivingEntity','knockback','(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V'),
 ('net.minecraft.world.entity.player.Player','updatePlayerPose','()V'),
 ('net.minecraft.server.level.ServerPlayer','restoreFrom','(Lnet/minecraft/server/level/ServerPlayer;Z)V'),
 ('net.minecraft.server.network.ServerGamePacketListenerImpl','handlePlayerCommand','(Lnet/minecraft/network/protocol/game/ServerboundPlayerCommandPacket;)V'),
 ('net.minecraft.server.network.ServerGamePacketListenerImpl','handlePlayerAction','(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V'),
 ('net.minecraft.server.level.ServerEntity','sendPairingData','(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V'),
 ('net.minecraft.server.packs.repository.PackRepository','net.minecraft.server.packs.repository.PackRepository','([Lnet/minecraft/server/packs/repository/RepositorySource;)V'),
 ('net.minecraft.world.level.ServerExplosion','interactWithBlocks','(Ljava/util/List;)V'),
 ('net.minecraft.world.level.ServerExplosion','createFire','(Ljava/util/List;)V'),
 ('net.minecraft.world.item.ItemStack','net.minecraft.world.item.ItemStack','(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V'),
 ('net.minecraft.world.item.ItemStack','set','(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;'),
 ('net.minecraft.world.item.ItemStack','set','(Lnet/minecraft/core/component/TypedDataComponent;)Ljava/lang/Object;'),
 ('net.minecraft.world.item.ItemStack','remove','(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;'),
 ('net.minecraft.world.item.ItemStack','applyComponents','(Lnet/minecraft/core/component/DataComponentPatch;)V'),
 ('net.minecraft.world.item.ItemStack','applyComponents','(Lnet/minecraft/core/component/DataComponentMap;)V'),
 ('net.minecraft.world.item.ItemStack','applyComponentsAndValidate','(Lnet/minecraft/core/component/DataComponentPatch;)V')]
cache={};results=[]
for cls,name,descriptor in checks:
 if cls not in cache:cache[cls]=subprocess.check_output([j,'-p','-s','-c','-classpath',str(jar),cls],text=True)
 text=cache[cls];found=bool(re.search(r'\b'+re.escape(name)+r'\([^\n]*\);\n\s*descriptor: '+re.escape(descriptor)+r'\n',text))
 results.append({'target':cls,'method':name,'descriptor':descriptor,'present':found})
for cls,field,descriptor in [
 ('net.minecraft.server.packs.repository.PackRepository','sources','Ljava/util/Set;'),
 ('net.minecraft.server.packs.repository.FolderRepositorySource','packType','Lnet/minecraft/server/packs/PackType;'),
 ('net.minecraft.server.level.ServerEntity','entity','Lnet/minecraft/world/entity/Entity;'),
 ('net.minecraft.server.network.ServerGamePacketListenerImpl','player','Lnet/minecraft/server/level/ServerPlayer;')]:
 if cls not in cache:cache[cls]=subprocess.check_output([j,'-p','-s','-c','-classpath',str(jar),cls],text=True)
 found=bool(re.search(r'\b'+re.escape(field)+r';\n\s*descriptor: '+re.escape(descriptor)+r'\n',cache[cls]))
 results.append({'target':cls,'field':field,'descriptor':descriptor,'present':found})
for method,invocation,expected in [('handlePlayerCommand','net/minecraft/server/level/ServerPlayer.setSprinting:(Z)V',2),('handlePlayerAction','net/minecraft/server/level/ServerPlayer.stopUsingItem:()V',1)]:
 text=cache['net.minecraft.server.network.ServerGamePacketListenerImpl'];match=re.search(r'public void '+method+r'\([^\n]*\).*?(?=\n  (?:public|private|protected))',text,re.S)
 count=match.group(0).count('// Method '+invocation) if match else 0
 results.append({'method':method,'invocation':invocation,'expected_count':expected,'count':count,'present':count==expected})
report={'kind':'TARGET_BYTECODE_MEMBER_AUDIT','minecraft':'26.4-snapshot-3','jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'checks':results,'passed':sum(x['present'] for x in results),'total':len(results),'mixin_transform':'NOT_RUN','game_launch':'NOT_RUN'}
out=args.output;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'passed':report['passed'],'total':report['total'],'failed':[x for x in results if not x['present']]},indent=2))

if report["passed"] != report["total"]: raise SystemExit(1)
