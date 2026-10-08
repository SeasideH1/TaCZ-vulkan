package com.tacz.guns.crafting;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.fabric.resource.CompositeGunPackResources;
import com.tacz.guns.init.*;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

/** Uses real installed pack bytes and Minecraft codecs/material matching, never mocked registries. */
public final class GunPackRecipeCompatibilityChecks {
    static int checks, recipes, baselineFailures;
    static HolderLookup.Provider lookup;
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static PackLocationInfo location(String name) { return new PackLocationInfo(name, Component.literal(name), PackSource.BUILT_IN, Optional.empty()); }
    static List<PackResources> jar(Path path) { return new FilePackResources.FileResourcesSupplier(path).openResources(location("zip"), new Pack.Metadata(Component.literal("zip"), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of())).toList(); }
    static CompositeGunPackResources directory(Path path) { return new CompositeGunPackResources(location("fixture"), List.of(new PathPackResources(location("fixture"),path)),null); }
    static byte[] read(IoSupplier<InputStream> source) throws Exception { try(var in = source.get()) { return in.readAllBytes(); } }
    static JsonObject json(byte[] bytes) { return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject(); }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), packRoot = Path.of(args[1]), out = Path.of(args[2]);
        SharedConstants.tryDetectVersion();
        var guard = Bootstrap.class.getDeclaredField("isBootstrapped"); guard.setAccessible(true); guard.setBoolean(null,true);
        try { ModDataComponents.COMPONENTS.registerAll(); ModBlocks.BLOCKS.registerAll(); ModItems.ITEMS.registerAll(); ModItems.registerGunTypes(); }
        finally { guard.setBoolean(null,false); }
        Bootstrap.bootStrap();
        var vanilla = net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        lookup = HolderLookup.Provider.create(Stream.concat(vanilla.listRegistries().filter(r -> !BuiltInRegistries.REGISTRY.containsKey(r.key().identifier())),BuiltInRegistries.REGISTRY.stream()));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(p -> p.apply());
        List<PackResources> tagPacks = new ArrayList<>();
        tagPacks.add(new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft").build(location("vanilla")).fullResources());
        try(var paths = Files.list(root.resolve("official-fabric/compile-nested"))) {
            tagPacks.addAll(jar(paths.filter(p -> p.getFileName().toString().startsWith("fabric-convention-tags-v2-")).findFirst().orElseThrow()));
        }
        try(var manager = new MultiPackResourceManager(PackType.SERVER_DATA,tagPacks)) {
            var tags = TagLoader.loadTagsForRegistry(manager,Registries.ITEM,(name,required) -> BuiltInRegistries.ITEM.get(name));
            BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM,tags)).apply();
            System.out.println("ACTUAL_ITEM_TAGS=" + tags.size());
        }
        List<Path> dirs;
        try(var paths = Files.list(packRoot)) { dirs = paths.filter(Files::isDirectory).toList(); }
        int gunFiles = 0, fractionalRates = 0;
        for(Path pack : dirs) {
            try(var paths = Files.walk(pack.resolve("data"))) {
                for(Path file : paths.filter(p -> p.toString().endsWith(".json") && p.toString().contains(File.separator+"data"+File.separator+"guns"+File.separator)).toList()) {
                    String text = Files.readString(file);
                    // Server reload reads a JsonElement; client network sync reads a String. Both must agree.
                    GunData server = CommonAssetsManager.GSON.fromJson(JsonParser.parseString(text),GunData.class);
                    GunData client = CommonAssetsManager.GSON.fromJson(text,GunData.class);
                    check(server.getBurstShootInterval()==client.getBurstShootInterval(),"Server/client rate mismatch " + file);
                    JsonObject definition = JsonParser.parseString(text).getAsJsonObject();
                    if(definition.has("burst_data") && definition.getAsJsonObject("burst_data").has("bpm")) {
                        double rate = definition.getAsJsonObject("burst_data").get("bpm").getAsDouble();
                        if(rate != Math.rint(rate)) { fractionalRates++; check(client.getBurstShootInterval()==(long)(60_000D/rate),"Fractional rate lost " + file); }
                    }
                    gunFiles++;
                }
            }
        }
        check(fractionalRates>0,"Renetti fractional BPM regression fixture missing");
        for(String rate : List.of("0","-1","\"NaN\"","\"Infinity\"")) {
            check(CommonAssetsManager.GSON.fromJson("{\"burst_data\":{\"bpm\":"+rate+"}}",GunData.class).getBurstShootInterval()==300,"Invalid BPM fallback " + rate);
        }
        check(CommonAssetsManager.GSON.fromJson("{\"burst_data\":{\"bpm\":200}}",GunData.class).getBurstShootInterval()==300,"Integer BPM regression");
        System.out.println("GUN_DATA_NETWORK_PARITY_PASS files="+gunFiles+" fractional_rates="+fractionalRates);
        byte[] firstLegacy = null;
        for(Path pack : dirs) {
            try(var resources = directory(pack); var paths = Files.walk(pack.resolve("data"))) {
                for(Path file : paths.filter(p -> p.toString().endsWith(".json") && p.toString().contains(File.separator + "recipes" + File.separator)).toList()) {
                    byte[] original = Files.readAllBytes(file);
                    JsonObject recipe = json(original);
                    if (!recipe.has("type")) continue;
                    String kind = recipe.get("type").getAsString();
                    if (!kind.equals("minecraft:crafting_shaped") && !kind.equals("minecraft:crafting_shapeless")) continue;
                    Path relative = pack.resolve("data").relativize(file);
                    Identifier id = Identifier.fromNamespaceAndPath(relative.getName(0).toString(),relative.subpath(1,relative.getNameCount()).toString().replace('\\','/').replaceFirst("^recipes/","recipe/"));
                    if (Recipe.DIRECT_CODEC.parse(lookup.createSerializationContext(JsonOps.INSTANCE),recipe).error().isPresent()) baselineFailures++;
                    byte[] upgraded = read(resources.getResource(PackType.SERVER_DATA,id));
                    Map<Identifier,IoSupplier<InputStream>> listed = new HashMap<>();
                    resources.listResources(PackType.SERVER_DATA,id.getNamespace(),"recipe",listed::put);
                    check(Arrays.equals(upgraded,read(listed.get(id))),"Direct/list resolution mismatch " + file);
                    verifyCraft(recipe,json(upgraded));
                    check(Arrays.equals(original,Files.readAllBytes(file)),"Pack modified " + file);
                    recipes++;
                    if(firstLegacy == null) firstLegacy = original;
                    System.out.println("PACK_RECIPE_PASS " + pack.getFileName() + " " + id);
                }
            }
        }
        check(recipes == 11,"Expected all 11 installed third-party vanilla recipes, got " + recipes);
        check(baselineFailures == 11,"Expected all 11 original recipes to reproduce parse failure");
        // ZIP gunpacks use the same adapter; a modern override must retain byte identity and precedence.
        Path zip = out.resolve("legacy-recipe-fixture.zip");
        try(var archive = new ZipOutputStream(Files.newOutputStream(zip))) {
            archive.putNextEntry(new ZipEntry("data/cod/recipes/fixture.json")); archive.write(firstLegacy); archive.closeEntry();
        }
        Identifier fixture = Identifier.parse("cod:recipe/fixture.json");
        byte[] modern;
        try(var resources = new CompositeGunPackResources(location("zip"),jar(zip),null)) {
            modern = read(resources.getResource(PackType.SERVER_DATA,fixture));
            verifyCraft(json(firstLegacy),json(modern));
        }
        Path override = out.resolve("modern-override");
        Path modernPath = override.resolve("data/cod/recipe/fixture.json"); Files.createDirectories(modernPath.getParent()); Files.write(modernPath,modern);
        List<PackResources> combined = new ArrayList<>(); combined.add(new PathPackResources(location("override"),override)); combined.addAll(jar(zip));
        try(var resources = new CompositeGunPackResources(location("combined"),combined,null)) {
            check(Arrays.equals(modern,read(resources.getResource(PackType.SERVER_DATA,fixture))),"Modern override modified");
            Map<Identifier,IoSupplier<InputStream>> listed = new HashMap<>(); resources.listResources(PackType.SERVER_DATA,"cod","recipe",listed::put);
            check(Arrays.equals(modern,read(listed.get(fixture))),"Enumeration override precedence changed");
        }
        // Custom gunsmith recipes and client assets must remain byte-for-byte unchanged.
        Path custom = override.resolve("data/cod/recipes/custom.json"); Files.createDirectories(custom.getParent());
        byte[] customBytes = "{\n  \"type\":\"tacz:gunsmith_table\",\"result\":{\"item\":\"unchanged\"}\n}".getBytes(StandardCharsets.UTF_8); Files.write(custom,customBytes);
        try(var resources = directory(override)) { check(Arrays.equals(customBytes,read(resources.getResource(PackType.SERVER_DATA,Identifier.parse("cod:recipe/custom.json")))),"Custom recipe changed"); }
        // Legacy SNBT goes through Mojang DFU too; preserve arbitrary BlockId and migrate known display tags.
        Path snbt = override.resolve("data/cod/recipes/snbt.json");
        JsonObject special = json(firstLegacy); JsonObject result = special.getAsJsonObject("result");
        result.addProperty("item","minecraft:paper"); result.addProperty("count",2); result.addProperty("nbt","{BlockId:\"cod:armory_weapon_box\",display:{Name:'{\"text\":\"Legacy name\"}'}}");
        special.addProperty("type","minecraft:crafting_shapeless"); special.remove("pattern"); special.remove("key");
        special.add("ingredients",JsonParser.parseString("[{\"item\":\"minecraft:iron_ingot\"}]") );
        Files.writeString(snbt,special.toString());
        try(var resources = directory(override)) {
            var recipe = (ShapelessRecipe) Recipe.DIRECT_CODEC.parse(lookup.createSerializationContext(JsonOps.INSTANCE),json(read(resources.getResource(PackType.SERVER_DATA,Identifier.parse("cod:recipe/snbt.json"))))).getOrThrow();
            var input = CraftingInput.of(1,1,List.of(new ItemStack(Items.IRON_INGOT)));
            check(recipe.matches(input,null),"Shapeless material mismatch"); var stack = recipe.assemble(input);
            check(stack.getCount()==2,"SNBT count lost"); check(stack.get(DataComponents.CUSTOM_NAME).getString().equals("Legacy name"),"DFU name lost");
            check(stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("BlockId").orElseThrow().equals("cod:armory_weapon_box"),"SNBT BlockId lost");
        }
        System.out.println("GUNPACK_RECIPE_COMPATIBILITY_PASS recipes="+recipes+" baseline_failures="+baselineFailures+" assertions="+checks);
    }

    static void verifyCraft(JsonObject original, JsonObject upgraded) {
        var recipe = (ShapedRecipe) Recipe.DIRECT_CODEC.parse(lookup.createSerializationContext(JsonOps.INSTANCE),upgraded).getOrThrow();
        List<ItemStack> materials = recipe.getIngredients().stream().map(i -> i.map(ingredient -> new ItemStack(ingredient.items().findFirst().orElseThrow().value())).orElse(ItemStack.EMPTY)).toList();
        var input = CraftingInput.of(recipe.getWidth(),recipe.getHeight(),materials);
        check(recipe.matches(input,null),"Real materials rejected");
        check(!recipe.matches(CraftingInput.of(1,1,List.of(new ItemStack(Items.DIRT))),null),"Wrong materials accepted");
        var result = recipe.assemble(input);
        JsonObject expected = original.getAsJsonObject("result");
        check(BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(expected.get("item").getAsString()),"Wrong workbench item");
        check(result.get(DataComponents.CUSTOM_DATA).copyTag().getString("BlockId").orElseThrow().equals(expected.getAsJsonObject("nbt").get("BlockId").getAsString()),"Workbench identity lost");
        check(result.getCount()==(expected.has("count")?expected.get("count").getAsInt():1),"Output count changed");
    }
}
