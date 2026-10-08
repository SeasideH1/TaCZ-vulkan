package com.tacz.guns.crafting;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.fabric.data.ItemStackData;
import com.tacz.guns.fabric.inventory.EntityInventory;
import com.tacz.guns.init.*;
import com.tacz.guns.resource.network.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.flag.FeatureFlagSet;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Native recipe/inventory leaf checks. Does not emulate ServerPlayer, menu packets or dropped entities. */
public final class AllRecipeCraftingChecks {
    // Publication update 2026-10-09: use the runner's working directory or an explicit project root.
    static final Path ROOT=Path.of(System.getProperty("tacz.test.projectRoot", ".")).toAbsolutePath().normalize();
    static final Path PACK=ROOT.resolve("src/main/resources/assets/tacz/custom/tacz_default_gun/data/tacz");
    static int checks,recipes,insufficientCases,fullCases; static RegistryAccess registryAccess; static HolderLookup.Provider lookup;
    static void check(boolean ok,String why){ checks++; if(!ok)throw new AssertionError(why); }
    static Identifier id(String text){return Identifier.parse(text);}
    static PackLocationInfo location(String name){return new PackLocationInfo(name,Component.literal(name),PackSource.BUILT_IN,Optional.empty());}
    static List<PackResources> jar(Path path,String name){return new FilePackResources.FileResourcesSupplier(path).openResources(location(name),new Pack.Metadata(Component.literal(name),PackCompatibility.COMPATIBLE,FeatureFlagSet.of(),List.of())).toList();}
    public static void main(String[] args)throws Exception {
        SharedConstants.tryDetectVersion();
        // Dedicated test JVM lifecycle bridge, not a Fabric bootstrap verdict.
        var guard=Bootstrap.class.getDeclaredField("isBootstrapped"); guard.setAccessible(true); guard.setBoolean(null,true);
        try {ModDataComponents.COMPONENTS.registerAll();ModBlocks.BLOCKS.registerAll();ModItems.ITEMS.registerAll();ModItems.registerGunTypes();}
        finally {guard.setBoolean(null,false);}
        Bootstrap.bootStrap();
        var vanilla=net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        var paintings=new MappedRegistry<PaintingVariant>(Registries.PAINTING_VARIANT,Lifecycle.stable());
        vanilla.lookupOrThrow(Registries.PAINTING_VARIANT).listElements().forEach(h->Registry.register(paintings,h.key(),h.value()));
        Registry.register(paintings,id("tacz:blood_strike_1"),PaintingVariant.DIRECT_CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(Files.readString(ROOT.resolve("src/main/resources/data/tacz/painting_variant/blood_strike_1.json")))).getOrThrow());
        paintings.freeze();
        lookup=HolderLookup.Provider.create(Stream.concat(Stream.concat(vanilla.listRegistries().filter(r->!r.key().equals(Registries.PAINTING_VARIANT) && !BuiltInRegistries.REGISTRY.containsKey(r.key().identifier())), BuiltInRegistries.REGISTRY.stream()),Stream.of(paintings)));
        registryAccess=new RegistryAccess.ImmutableRegistryAccess(Stream.concat(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registries(),Stream.of(new RegistryAccess.RegistryEntry<>(Registries.PAINTING_VARIANT,paintings))));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(p->p.apply());
        List<PackResources> resources=new ArrayList<>();
        resources.add(new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft").build(location("vanilla")).fullResources());
        Path convention=Files.list(ROOT.resolve("../official-fabric/compile-nested")).filter(p->p.getFileName().toString().startsWith("fabric-convention-tags-v2-")).findFirst().orElseThrow();
        resources.addAll(jar(convention,"fabric_convention_tags"));
        try(var manager=new MultiPackResourceManager(PackType.SERVER_DATA,resources)) {
            var tags=TagLoader.loadTagsForRegistry(manager,Registries.ITEM,(name,required)->BuiltInRegistries.ITEM.get(name));
            BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM,tags)).apply();
            System.out.println("Actual item tags loaded="+tags.size());
        }
        Map<DataType,Map<Identifier,String>> network=new EnumMap<>(DataType.class);
        load(network,DataType.ATTACHMENT_TAGS,"tacz_tags/attachments");load(network,DataType.GUN_DATA,"data/guns");load(network,DataType.ATTACHMENT_DATA,"data/attachments");
        load(network,DataType.GUN_INDEX,"index/guns");load(network,DataType.AMMO_INDEX,"index/ammo");load(network,DataType.ATTACHMENT_INDEX,"index/attachments");
        CommonNetworkCache.INSTANCE.fromNetwork(network);
        check(CommonNetworkCache.INSTANCE.gunIndex.size()==54,"All54 gun indices");
        check(CommonNetworkCache.INSTANCE.attachmentIndex.size()==99,"All99 attachment indices");
        check(CommonNetworkCache.INSTANCE.ammoIndex.size()==24,"All24 ammo indices");
        List<Path> paths;try(var stream=Files.walk(PACK.resolve("recipes"))){paths=new ArrayList<>(stream.filter(p->p.toString().endsWith(".json")).sorted().toList());}
        paths.add(ROOT.resolve("src/main/resources/data/tacz/recipes/misc/blood_strike_1.json"));
        for(Path path:paths)checkRecipe(path);
        check(recipes==173,"All173 source recipes");
        var duplicate=List.of(new GunSmithTableIngredient(Ingredient.of(Items.IRON_INGOT),8),new GunSmithTableIngredient(Ingredient.of(Items.IRON_INGOT),8));
        var synthetic=new SimpleContainer(new ItemStack(Items.IRON_INGOT,8));
        boolean accepted=GunSmithTableCrafting.consumeMaterials(EntityInventory.of(synthetic),duplicate,false);
        System.out.println("BASELINE_OVERLAP_CHARACTERIZATION requested=16 available=8 accepted="+accepted+" remaining="+total(synthetic));
        check(accepted&&total(synthetic)==0,"Unchanged release overlap behavior changed unexpectedly");
        System.out.println("ALL_RECIPE_CRAFTING_PASS recipes="+recipes+" assertions="+checks+" insufficient_cases="+insufficientCases+" full_inventory_cases="+fullCases+" actual_menu_runtime=NOT_RUN output_entity_spawn=NOT_RUN overlap_limitation=CONFIRMED_BASELINE");
    }
    static void load(Map<DataType,Map<Identifier,String>> cache,DataType type,String directory)throws Exception {
        Path root=PACK.resolve(directory);Map<Identifier,String> values=new LinkedHashMap<>();
        try(var files=Files.walk(root)){for(Path path:files.filter(p->p.toString().endsWith(".json")).sorted().toList())values.put(id("tacz:"+root.relativize(path).toString().replace("\\","/").replace(".json","")),Files.readString(path));}
        cache.put(type,values);
    }
    static void checkRecipe(Path path)throws Exception {
        JsonObject json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        String name=path.getFileName().toString();
        GunSmithTableRecipe recipe=GunSmithTableSerializer.CODEC.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();
        recipe.setId(id("tacz:"+(path.startsWith(PACK)?PACK.resolve("recipes").relativize(path).toString().replace("\\","/").replace(".json",""):"misc/blood_strike_1")));recipe.init();recipes++;
        List<GunSmithTableIngredient> ingredients=recipe.getInputs();List<Item> chosen=new ArrayList<>();
        for(var ingredient:ingredients){check(!ingredient.getIngredient().isEmpty(),name+" loaded tag empty");chosen.add(ingredient.getIngredient().items().sorted(Comparator.comparing(h->h.unwrapKey().orElseThrow().identifier().toString())).findFirst().orElseThrow().value());}
        for(int i=0;i<ingredients.size();i++)for(int j=0;j<ingredients.size();j++)if(i!=j){
            Ingredient other=ingredients.get(j).getIngredient();
            check(ingredients.get(i).getIngredient().items().noneMatch(other::acceptsItem),name+" default ingredient overlap "+i+"/"+j);
        }
        int required=ingredients.stream().mapToInt(GunSmithTableIngredient::getCount).sum();
        SimpleContainer exact=inventory(ingredients,chosen,-1,false);int before=total(exact);
        check(GunSmithTableCrafting.consumeMaterials(EntityInventory.of(exact),ingredients,false),name+" exact input rejected");
        check(before-total(exact)==required,name+" conservation exact");check(total(exact)==0,name+" exact leftovers");
        for(int missing=0;missing<ingredients.size();missing++){
            var insufficient=inventory(ingredients,chosen,missing,false);var snapshot=snapshot(insufficient);
            check(!GunSmithTableCrafting.consumeMaterials(EntityInventory.of(insufficient),ingredients,false),name+" insufficient accepted "+missing);
            check(same(insufficient,snapshot),name+" insufficient modified inventory "+missing);insufficientCases++;
        }
        var full=inventory(ingredients,chosen,-1,true);var fullBefore=snapshot(full);int fullTotal=total(full);
        check(IntStream.range(0,36).allMatch(i->!full.getItem(i).isEmpty()),name+" fixture not full");
        check(GunSmithTableCrafting.consumeMaterials(EntityInventory.of(full),ingredients,false),name+" full input rejected");
        check(fullTotal-total(full)==required,name+" full conservation");
        for(int i=0;i<36;i++)if(fullBefore.get(i).is(Items.BEDROCK))check(ItemStack.matches(full.getItem(i),fullBefore.get(i)),name+" changed unrelated metadata");fullCases++;
        var creative=new SimpleContainer(36);check(GunSmithTableCrafting.consumeMaterials(EntityInventory.of(creative),ingredients,true),name+" creative rejected");check(total(creative)==0,name+" creative mutated");
        var output=recipe.getResultItem(registryAccess);check(!output.isEmpty(),name+" empty output");
        JsonObject expected=json.getAsJsonObject("result");int count=expected.has("count")?expected.get("count").getAsInt():1;check(output.getCount()==count,name+" output count");
        String type=expected.get("type").getAsString();
        if(type.equals("gun")){
            check(output.getItem() instanceof IGun,name+" expected gun");IGun gun=(IGun)output.getItem();Identifier gunId=id(expected.get("id").getAsString());
            check(gun.getGunId(output).equals(gunId),name+" gun id");check(gun.getCurrentAmmoCount(output)==(expected.has("ammo_count")?expected.get("ammo_count").getAsInt():0),name+" ammo count");check(!gun.hasBulletInBarrel(output),name+" chamber");
            check(gun.getFireMode(output)==CommonNetworkCache.INSTANCE.gunIndex.get(gunId).getGunData().getFireModeSet().getFirst(),name+" fire mode");
            if(expected.has("attachments"))for(var entry:expected.getAsJsonObject("attachments").entrySet()){var attachment=gun.getAttachment(output,AttachmentType.valueOf(entry.getKey().toUpperCase(Locale.ROOT)));check(attachment.getItem() instanceof IAttachment&&((IAttachment)attachment.getItem()).getAttachmentId(attachment).equals(id(entry.getValue().getAsString())),name+" attachment "+entry.getKey());}
        }else if(type.equals("ammo")){check(output.getItem() instanceof IAmmo&&((IAmmo)output.getItem()).getAmmoId(output).equals(id(expected.get("id").getAsString())),name+" ammo id");}
        else if(type.equals("attachment")){check(output.getItem() instanceof IAttachment&&((IAttachment)output.getItem()).getAttachmentId(output).equals(id(expected.get("id").getAsString())),name+" attachment id");}
        else {check(output.is(Items.PAINTING),name+" custom painting");check(output.get(DataComponents.PAINTING_VARIANT).unwrapKey().orElseThrow().identifier().equals(id("tacz:blood_strike_1")),name+" painting variant");check(output.get(DataComponents.CUSTOM_NAME).getString().equals("item.tacz.painting.blood_strike_1"),name+" painting custom name");}
        var drops=GunSmithTableCrafting.splitOutput(output);check(drops.stream().mapToInt(ItemStack::getCount).sum()==output.getCount(),name+" drop quantity conservation");
        if(recipe.getId().equals(id("tacz:ammo/22wmr")))check(drops.stream().map(ItemStack::getCount).toList().equals(List.of(64,36)),"Exact 100-round .22 WMR split");
        for(var drop:drops){check(drop.getCount()<=drop.getMaxStackSize(),name+" oversized physical drop");check(ItemStack.isSameItemSameComponents(drop,output),name+" drop components");var nbtOps=lookup.createSerializationContext(NbtOps.INSTANCE);var encoded=ItemStack.CODEC.encodeStart(nbtOps,drop).getOrThrow();check(ItemStack.matches(drop,ItemStack.CODEC.parse(nbtOps,encoded).getOrThrow()),name+" physical result NBT roundtrip");}
        if(output.getCount()>99)check(ItemStack.CODEC.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE),output).error().isPresent(),name+" baseline oversized codec failure characterization");
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),registryAccess);
        try {GunSmithTableSerializer.STREAM_CODEC.encode(buffer,recipe);var wire=GunSmithTableSerializer.STREAM_CODEC.decode(buffer);check(buffer.readableBytes()==0,name+" trailing bytes");check(ItemStack.matches(output,wire.getOutput()),name+" recipe wire output");check(wire.getId().equals(recipe.getId()),name+" wire id");check(wire.getInputs().size()==ingredients.size(),name+" wire materials");}finally{buffer.release();}
        output.set(DataComponents.CUSTOM_NAME,Component.literal("Mutated caller"));output.setCount(99);check(!ItemStack.matches(output,recipe.getOutput()),name+" result copy isolation");check(recipe.getOutput().getCount()==count,name+" recipe count mutated");
    }
    static SimpleContainer inventory(List<GunSmithTableIngredient> inputs,List<Item> chosen,int missing,boolean full){
        SimpleContainer c=new SimpleContainer(36);int slot=0;
        for(int i=0;i<inputs.size();i++){int n=inputs.get(i).getCount()-(i==missing?1:0)+(full?1:0);while(n>0){int amount=Math.min(n,32);ItemStack stack=new ItemStack(chosen.get(i),amount);stack.set(DataComponents.CUSTOM_NAME,Component.literal("material "+i+"/"+slot));c.setItem(slot++,stack);n-=amount;}}
        if(full)while(slot<36){ItemStack filler=new ItemStack(Items.BEDROCK,64);filler.set(DataComponents.CUSTOM_NAME,Component.literal("untouched "+slot));c.setItem(slot++,filler);}
        return c;
    }
    static int total(SimpleContainer c){return IntStream.range(0,c.getContainerSize()).map(i->c.getItem(i).getCount()).sum();}
    static List<ItemStack> snapshot(SimpleContainer c){return IntStream.range(0,c.getContainerSize()).mapToObj(i->c.getItem(i).copy()).toList();}
    static boolean same(SimpleContainer c,List<ItemStack> snapshot){return IntStream.range(0,c.getContainerSize()).allMatch(i->ItemStack.matches(c.getItem(i),snapshot.get(i)));}
}
