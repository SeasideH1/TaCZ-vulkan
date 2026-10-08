package com.tacz.guns.crafting;

import com.google.gson.*;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.network.CommonNetworkCache;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;
import java.util.*;

/** Fixture host validation against actual production predicates; no GUI/network/player simulation. */
public final class AttachmentHostFixtureChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Identifier id(String text){return Identifier.parse(text);}
    private static ItemStack gun(Identifier id){return GunItemBuilder.create().setId(id).build();}
    public static void main(String[] args)throws Exception {
        // Reuse the established real registry/component/tag/index bootstrap in a dedicated test JVM.
        AllRecipeCraftingChecks.main(new String[0]);
        Path plan=Path.of(args[0]);Path output=Path.of(args[1]);
        JsonArray cases=JsonParser.parseString(Files.readString(plan)).getAsJsonObject().getAsJsonArray("cases");
        JsonArray results=new JsonArray();int accepted=0,builtins=0,mismatches=0;
        Set<Identifier> attachmentIds=new HashSet<>();
        for(JsonElement element:cases){
            JsonObject stage=element.getAsJsonObject();if(!stage.get("kind").getAsString().equals("attachment"))continue;
            String name=stage.get("stage").getAsString();Identifier attachmentId=id(stage.get("id").getAsString());Identifier host=id(stage.get("gun").getAsString());
            check(attachmentIds.add(attachmentId),name+" duplicate attachment case");
            ItemStack gun=gun(host), attachment=AttachmentItemBuilder.create().setId(attachmentId).build();
            check(gun.getItem() instanceof IGun,name+" unavailable real gun item");check(attachment.getItem() instanceof IAttachment,name+" unavailable real attachment item");
            IGun api=(IGun)gun.getItem();IAttachment attachmentApi=(IAttachment)attachment.getItem();AttachmentType slot=attachmentApi.getType(attachment);
            check(api.getGunId(gun).equals(host),name+" incorrect host ID");check(slot==AttachmentType.valueOf(stage.get("slot").getAsString().toUpperCase(Locale.ROOT)),name+" fixture slot differs from actual index");
            boolean allowed=api.allowAttachment(gun,attachment),slotAllowed=api.allowAttachmentType(gun,slot),builtin=stage.get("builtin").getAsBoolean();
            JsonObject row=new JsonObject();row.addProperty("stage",name);row.addProperty("attachment",attachmentId.toString());row.addProperty("host",host.toString());row.addProperty("slot",slot.name());row.addProperty("allowAttachment",allowed);row.addProperty("allowAttachmentType",slotAllowed);
            List<String> legalHosts=new ArrayList<>();
            for(Identifier candidate:CommonNetworkCache.INSTANCE.gunIndex.keySet().stream().sorted().toList()){
                ItemStack candidateGun=gun(candidate);IGun candidateApi=(IGun)candidateGun.getItem();
                if(candidateApi.allowAttachment(candidateGun,attachment)&&candidateApi.allowAttachmentType(candidateGun,slot))legalHosts.add(candidate.toString());
            }
            JsonArray legal=new JsonArray();legalHosts.forEach(legal::add);row.add("legal_hosts_from_actual_predicates",legal);
            if(builtin){
                check(api.getBuiltInAttachmentId(gun,slot).equals(attachmentId),name+" declared builtin missing from host");
                ItemStack actual=api.getBuiltinAttachment(gun,slot);check(actual.getItem() instanceof IAttachment&&((IAttachment)actual.getItem()).getAttachmentId(actual).equals(attachmentId),name+" actual builtin accessor mismatch");
                row.addProperty("status","BUILTIN_ONLY_SEPARATE_INSTALL_NOT_APPLICABLE");builtins++;
            }else if(allowed&&slotAllowed){
                check(api.getAttachment(gun,slot).isEmpty(),name+" fixture unexpectedly has preinstalled attachment");
                api.installAttachment(gun,attachment);ItemStack installed=api.getAttachment(gun,slot);
                check(ItemStack.matches(installed,attachment),name+" real install accessor failed");
                check(attachment.getCount()==1,name+" Item API modified source stack; UI consumption is separate");
                api.unloadAttachment(gun,slot);check(api.getAttachment(gun,slot).isEmpty(),name+" real unload accessor failed");
                row.addProperty("status","HOST_PREDICATES_AND_ITEM_ACCESSORS_PASS");accepted++;
            }else{
                row.addProperty("status",legalHosts.isEmpty()?"NO_LEGAL_DEFAULT_HOST":"FIXTURE_HOST_MISMATCH");mismatches++;
            }
            results.add(row);
        }
        check(results.size()==99,"Expected99 fixture attachments");check(attachmentIds.equals(CommonNetworkCache.INSTANCE.attachmentIndex.keySet()),"Fixture IDs do not cover all99 actual indices");
        JsonObject report=new JsonObject();report.addProperty("kind","ATTACHMENT_FIXTURE_HOST_PREDICATE_CHECKS");report.addProperty("status",mismatches==0?"PASS":"FIXTURE_MISMATCH");report.addProperty("attachment_cases",results.size());report.addProperty("legal_install_pairs",accepted);report.addProperty("builtin_only",builtins);report.addProperty("mismatches",mismatches);report.addProperty("assertions",checks);report.addProperty("actual_ui_install_remove_and_conservation","NOT_RUN");report.add("cases",results);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        System.out.println("ATTACHMENT_HOST_FIXTURES_"+(mismatches==0?"PASS":"MISMATCH")+" cases="+results.size()+" legal="+accepted+" builtins="+builtins+" mismatches="+mismatches+" assertions="+checks+" actual_ui=NOT_RUN");
        if(mismatches!=0)throw new AssertionError("Actual predicate mismatches; see "+output);
    }
}
