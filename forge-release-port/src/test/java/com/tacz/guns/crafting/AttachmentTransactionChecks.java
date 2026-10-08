package com.tacz.guns.crafting;

import com.google.gson.*;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.*;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.fabric.data.ItemStackData;
import com.tacz.guns.fabric.inventory.AttachmentRefit;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/** Exact packet core with real native slots; player cache/event and world delivery remain separate boundaries. */
public final class AttachmentTransactionChecks {
    static int checks,cases,magazineCases,magazineCountsTested;static List<String> arithmeticFailures=new ArrayList<>();
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Identifier id(String s){return Identifier.parse(s);}
    static ItemStack marked(Identifier id,String marker){ItemStack result=AttachmentItemBuilder.create().setId(id).build();result.set(DataComponents.CUSTOM_NAME,Component.literal(marker));var data=ItemStackData.read(result);data.putString("FixtureMarker",marker);ItemStackData.set(result,data);return result;}
    public static void main(String[] args)throws Exception {
        AllRecipeCraftingChecks.main(new String[0]);
        var plan=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        for(var e:plan.getAsJsonArray("cases")){var row=e.getAsJsonObject();if(row.get("kind").getAsString().equals("attachment")&&!row.get("builtin").getAsBoolean())run(row);}
        check(cases==97,"97 removable fixtures");check(magazineCases==17,"17 extended-mag/ammo-mod fixtures");
        sweepMagazineCounts();
        check(new HashSet<>(arithmeticFailures).equals(Set.of(
                "tacz:fn_evolys actual_capacity=150 installed=tacz:extended_mag_3 lost_round_cases=[97->96, 145->144, 146->144]",
                "tacz:m249 actual_capacity=200 installed=tacz:extended_mag_3 lost_round_cases=[121->120, 181->180, 182->180]")),
                "Original arithmetic parity differs from the user's accepted six inherited loss cases");
        JsonObject report=new JsonObject();report.addProperty("fixture_cases",cases);report.addProperty("extended_mag_cases",magazineCases);report.addProperty("assertions",checks);report.addProperty("valid_magazine_counts_tested",magazineCountsTested);report.addProperty("fixture_core_status","PASS");report.addProperty("real_server_player_cache_events_and_world_spawn","NOT_RUN");JsonArray issues=new JsonArray();arithmeticFailures.forEach(issues::add);report.add("magazine_count_sweep_failures",issues);
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        System.out.println("ATTACHMENT_TRANSACTION_FIXTURES_PASS cases="+cases+" magazine_cases="+magazineCases+" assertions="+checks+" broader_magazine_arithmetic_failures="+arithmeticFailures.size()+" real_player_world_effects=NOT_RUN");
        arithmeticFailures.stream().limit(12).forEach(System.out::println);
    }
    static void run(JsonObject row){
        String stage=row.get("stage").getAsString();Identifier host=id(row.get("gun").getAsString()),attachmentId=id(row.get("id").getAsString());
        var gunData=CommonNetworkCache.INSTANCE.gunIndex.get(host).getGunData();Identifier ammoId=gunData.getAmmoId();
        ItemStack gun=GunItemBuilder.create().setId(host).setAmmoCount(gunData.getAmmoAmount()).setAmmoInBarrel(true).build();AbstractGunItem gunApi=(AbstractGunItem)gun.getItem();
        AttachmentType slot=AttachmentType.valueOf(row.get("slot").getAsString().toUpperCase(Locale.ROOT));boolean mag=slot==AttachmentType.EXTENDED_MAG;
        if(mag)magazineCases++;
        var inventory=new SimpleContainer(36);inventory.setItem(0,gun);ItemStack first=marked(attachmentId,stage+" first");inventory.setItem(1,first);
        inventory.setItem(2,AmmoItemBuilder.create().setId(ammoId).setCount(row.get("reserve_stack_size").getAsInt()).build());inventory.setItem(3,inventory.getItem(2).copy());
        List<ItemStack> ground=new ArrayList<>();List<String> callbacks=new ArrayList<>();
        Consumer<ItemStack> deliver=ammo->{ItemStack rest=inventory.addItem(ammo);if(!rest.isEmpty())ground.add(rest.copy());};
        Consumer<ItemStack> returnAmmo=g->{callbacks.add("ammo");gunApi.dropAllAmmo(g,false,deliver);};
        long ammoBefore=ammoTotal(inventory,ground,gun,gunApi,ammoId);int beforeMagazine=gunApi.getCurrentAmmoCount(gun);
        Consumer<ItemStack> installRefresh=g->{callbacks.add("refresh");check(ItemStack.matches(gunApi.getAttachment(g,slot),first),stage+" refresh before install");check(inventory.getItem(1)==first,stage+" input replaced before property refresh");};
        check(AttachmentRefit.install(inventory,1,0,installRefresh,returnAmmo),stage+" install failed");
        check(callbacks.equals(mag?List.of("refresh","ammo"):List.of("refresh")),stage+" install callback order");
        check(attachmentTotal(inventory,gun,gunApi)==1,stage+" install attachment conservation");
        check(ammoTotal(inventory,ground,gun,gunApi,ammoId)==ammoBefore,stage+" install ammo conservation");
        check(gunApi.hasBulletInBarrel(gun),stage+" install chamber lost");check(gunApi.getCurrentAmmoCount(gun)==(mag?0:beforeMagazine),stage+" install magazine state");
        check(ItemStack.matches(gunApi.getAttachment(gun,slot),first),stage+" installed metadata lost");
        var nativeSlots=net.minecraft.core.NonNullList.withSize(6,ItemStack.EMPTY);
        gun.get(com.tacz.guns.init.ModDataComponents.ATTACHMENTS.get()).copyInto(nativeSlots);
        int nativeSlot=switch(slot){case SCOPE->0;case MUZZLE->1;case STOCK->2;case GRIP->3;case LASER->4;case EXTENDED_MAG->5;case NONE->throw new AssertionError("Not an attachment slot");};
        for(int i=0;i<6;i++)check(i==nativeSlot?ItemStack.matches(nativeSlots.get(i),first):nativeSlots.get(i).isEmpty(),stage+" native component slot "+i);
        int installedCapacity=AttachmentDataUtils.getAmmoCountWithAttachment(gun,gunData);
        var exclusive=gunData.getExclusiveAttachments().get(attachmentId);var attachmentData=exclusive!=null?exclusive:CommonNetworkCache.INSTANCE.attachmentIndex.get(attachmentId).getData();
        int level=mag?Math.clamp(attachmentData.getExtendedMagLevel(),0,3):0;int expectedCapacity=level==0||gunData.getExtendedMagAmmoAmount()==null?gunData.getAmmoAmount():gunData.getExtendedMagAmmoAmount()[level-1];
        check(installedCapacity==expectedCapacity,stage+" installed native capacity");roundtrip(gun,stage+" installed");
        ItemStack second=marked(attachmentId,stage+" replacement");inventory.setItem(4,second);gunApi.setCurrentAmmoCount(gun,installedCapacity);ammoBefore=ammoTotal(inventory,ground,gun,gunApi,ammoId);callbacks.clear();
        check(AttachmentRefit.install(inventory,4,0,g->{callbacks.add("refresh");check(ItemStack.matches(gunApi.getAttachment(g,slot),second),stage+" replacement refresh state");check(inventory.getItem(4)==second,stage+" replacement returned before refresh");},returnAmmo),stage+" replacement failed");
        check(ItemStack.matches(inventory.getItem(4),first),stage+" old attachment not returned exactly");check(ItemStack.matches(gunApi.getAttachment(gun,slot),second),stage+" replacement components");check(attachmentTotal(inventory,gun,gunApi)==2,stage+" replacement conservation");check(ammoTotal(inventory,ground,gun,gunApi,ammoId)==ammoBefore,stage+" replacement ammo conservation");
        check(callbacks.equals(mag?List.of("refresh","ammo"):List.of("refresh")),stage+" replacement order");
        gunApi.setAttachmentLock(gun,true);var locked=snapshot(inventory);callbacks.clear();check(!AttachmentRefit.install(inventory,4,0,g->callbacks.add("refresh"),returnAmmo),stage+" locked install accepted");check(!AttachmentRefit.unload(inventory,0,slot,item->inventory.addItem(item).isEmpty(),g->callbacks.add("refresh"),returnAmmo),stage+" locked unload accepted");check(same(inventory,locked)&&callbacks.isEmpty(),stage+" locked state mutated");gunApi.setAttachmentLock(gun,false);
        for(int i=1;i<36;i++)if(inventory.getItem(i).isEmpty())inventory.setItem(i,new ItemStack(Items.BEDROCK,64));
        var full=snapshot(inventory);callbacks.clear();check(!AttachmentRefit.unload(inventory,0,slot,item->inventory.addItem(item).isEmpty(),g->callbacks.add("refresh"),returnAmmo),stage+" full inventory unload accepted");check(same(inventory,full)&&callbacks.isEmpty(),stage+" full inventory unload mutated state");
        inventory.setItem(35,ItemStack.EMPTY);gunApi.setCurrentAmmoCount(gun,installedCapacity);ammoBefore=ammoTotal(inventory,ground,gun,gunApi,ammoId);callbacks.clear();
        check(AttachmentRefit.unload(inventory,0,slot,item->{callbacks.add("return");check(!gunApi.getAttachment(gun,slot).isEmpty(),stage+" unload before return check");return inventory.addItem(item).isEmpty();},g->{callbacks.add("refresh");check(gunApi.getAttachment(g,slot).isEmpty(),stage+" unload refresh before removal");},returnAmmo),stage+" unload with one slot failed");
        check(callbacks.equals(mag?List.of("return","refresh","ammo"):List.of("return","refresh")),stage+" unload callback order");
        check(ItemStack.matches(inventory.getItem(35),second),stage+" returned attachment components");check(attachmentTotal(inventory,gun,gunApi)==2,stage+" unload attachment conservation");check(ammoTotal(inventory,ground,gun,gunApi,ammoId)==ammoBefore,stage+" unload ammo conservation");check(gunApi.hasBulletInBarrel(gun),stage+" unload chamber lost");check(gunApi.getAttachment(gun,slot).isEmpty(),stage+" unload retained attachment");check(AttachmentDataUtils.getAmmoCountWithAttachment(gun,gunData)==gunData.getAmmoAmount(),stage+" capacity not reset");
        check(gunApi.getCurrentAmmoCount(gun)==(mag?0:installedCapacity),stage+" unload magazine state");
        for(var stack:snapshot(inventory))if(!stack.isEmpty())roundtrip(stack,stage+" inventory");for(var stack:ground){check(stack.getCount()<=stack.getMaxStackSize(),stage+" oversized ammo return");roundtrip(stack,stage+" ground-delivery boundary");}
        callbacks.clear();check(!AttachmentRefit.install(inventory,-1,0,g->callbacks.add("bad"),returnAmmo),stage+" negative slot accepted");check(!AttachmentRefit.unload(inventory,36,slot,item->true,g->callbacks.add("bad"),returnAmmo),stage+" out-of-bounds accepted");check(callbacks.isEmpty(),stage+" invalid slot called effects");cases++;
    }
    static void sweepMagazineCounts(){
        for(var e:CommonNetworkCache.INSTANCE.gunIndex.entrySet()){
            var data=e.getValue().getGunData();if(data.getReloadData().getType()==FeedType.FUEL)continue;
            var stack=GunItemBuilder.create().setId(e.getKey()).setAmmoCount(1).build();var api=(AbstractGunItem)stack.getItem();if(api.useInventoryAmmo(stack))continue;
            int max=data.getAmmoAmount();Identifier maximizingAttachment=null;
            for(var attachmentEntry:CommonNetworkCache.INSTANCE.attachmentIndex.entrySet()){
                if(attachmentEntry.getValue().getType()!=AttachmentType.EXTENDED_MAG)continue;
                ItemStack attachment=AttachmentItemBuilder.create().setId(attachmentEntry.getKey()).build();
                if(!api.allowAttachment(stack,attachment)||!api.allowAttachmentType(stack,AttachmentType.EXTENDED_MAG))continue;
                api.installAttachment(stack,attachment);int capacity=AttachmentDataUtils.getAmmoCountWithAttachment(stack,data);
                if(capacity>max){max=capacity;maximizingAttachment=attachmentEntry.getKey();}
            }
            api.unloadAttachment(stack,AttachmentType.EXTENDED_MAG);
            if(maximizingAttachment!=null)api.installAttachment(stack,AttachmentItemBuilder.create().setId(maximizingAttachment).build());
            check(AttachmentDataUtils.getAmmoCountWithAttachment(stack,data)==max,"Actual maximum magazine capacity "+e.getKey());
            List<String> failedCounts=new ArrayList<>();
            for(int n=1;n<=max;n++){magazineCountsTested++;api.setCurrentAmmoCount(stack,n);List<ItemStack> returned=new ArrayList<>();api.dropAllAmmo(stack,false,returned::add);int count=returned.stream().mapToInt(ItemStack::getCount).sum();if(count!=n||api.getCurrentAmmoCount(stack)!=0)failedCounts.add(n+"->"+count);}
            if(!failedCounts.isEmpty())arithmeticFailures.add(e.getKey()+" actual_capacity="+max+" installed="+maximizingAttachment+" lost_round_cases="+failedCounts);
        }
    }
    static long ammoTotal(SimpleContainer c,List<ItemStack> ground,ItemStack gun,IGun api,Identifier ammo){return api.getCurrentAmmoCount(gun)+(api.hasBulletInBarrel(gun)?1:0)+Stream.concat(snapshot(c).stream(),ground.stream()).filter(s->s.getItem() instanceof IAmmo a&&a.getAmmoId(s).equals(ammo)).mapToLong(ItemStack::getCount).sum();}
    static int attachmentTotal(SimpleContainer c,ItemStack gun,IGun api){int n=snapshot(c).stream().filter(s->s.getItem() instanceof IAttachment).mapToInt(ItemStack::getCount).sum();for(var type:AttachmentType.values())if(type!=AttachmentType.NONE)n+=api.getAttachment(gun,type).getCount();return n;}
    static List<ItemStack> snapshot(SimpleContainer c){return IntStream.range(0,c.getContainerSize()).mapToObj(i->c.getItem(i).copy()).toList();}
    static boolean same(SimpleContainer c,List<ItemStack> s){return IntStream.range(0,c.getContainerSize()).allMatch(i->ItemStack.matches(c.getItem(i),s.get(i)));}
    static void roundtrip(ItemStack stack,String why){var ops=AllRecipeCraftingChecks.lookup.createSerializationContext(NbtOps.INSTANCE);check(ItemStack.matches(stack,ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow()),why+" NBT");var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),AllRecipeCraftingChecks.registryAccess);try{ItemStack.STREAM_CODEC.encode(buffer,stack);check(ItemStack.matches(stack,ItemStack.STREAM_CODEC.decode(buffer)),why+" wire");check(buffer.readableBytes()==0,why+" trailing bytes");}finally{buffer.release();}}
}
