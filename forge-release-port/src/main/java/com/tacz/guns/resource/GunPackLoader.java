/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.fabric.resource.CompositeGunPackResources;
import com.tacz.guns.util.GetJarResources;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum GunPackLoader implements RepositorySource {
    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("GunPackFinder");
    /** Compatibility for callers that explicitly select a side. Repository sources use forType. */
    public PackType packType = PackType.SERVER_DATA;
    private boolean firstLoad = true;

    public static RepositorySource forType(PackType type) {
        return consumer -> consumer.accept(INSTANCE.discoverExtensions(type));
    }

    @Override public void loadPacks(Consumer<Pack> consumer) {
        consumer.accept(discoverExtensions(packType));
    }

    public Pack discoverExtensions() { return discoverExtensions(packType); }

    public synchronized Pack discoverExtensions(PackType type) {
        Path path = FabricLoader.getInstance().getGameDir().resolve("tacz");
        try { Files.createDirectories(path); }
        catch (IOException e) { throw new IllegalStateException("Cannot create gunpack directory " + path, e); }
        PreLoadConfig.load(path);
        if (firstLoad) {
            if (!PreLoadConfig.override.get()) {
                for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
                    GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), path, entry.extraDirName());
                }
            }
            firstLoad = false;
        }
        List<GunPack> gunPacks = scanExtensions(path);
        PackLocationInfo location = new PackLocationInfo("tacz_resources", Component.literal("TACZ Resources"), PackSource.BUILT_IN, Optional.empty());
        Path iconPath = getModIcon("tacz");
        IoSupplier<InputStream> icon = iconPath == null ? null : IoSupplier.create(iconPath);
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override public PackMetadataResources openMetadata(PackLocationInfo info) {
                return new CompositeGunPackResources(info, List.of(), icon);
            }
            @Override public Stream<PackResources> openResources(PackLocationInfo info, Pack.Metadata metadata) {
                List<PackResources> resources = new ArrayList<>();
                try {
                    for (GunPack gunPack : gunPacks) {
                        if (Files.isDirectory(gunPack.path())) resources.add(new PathPackResources(info, gunPack.path()));
                        else new FilePackResources.FileResourcesSupplier(gunPack.path()).openResources(info, metadata).forEach(resources::add);
                    }
                    return Stream.of(new CompositeGunPackResources(info, resources, icon));
                } catch (RuntimeException e) {
                    resources.forEach(PackResources::close);
                    throw e;
                }
            }
        };
        return Objects.requireNonNull(Pack.readMetaAndCreate(location, supplier, type,
                new PackSelectionConfig(true, Pack.Position.BOTTOM, true)), "TACZ synthetic pack metadata");
    }

    public static @Nullable Path getModIcon(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .flatMap(mod -> mod.findPath("icon.png")).filter(Files::exists).orElse(null);
    }

    private static GunPack fromDirPath(Path path) throws IOException {
        Path packInfoFilePath = path.resolve("gunpack.meta.json");
        try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
            PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (info == null) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
                return null;
            }

            if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", packInfoFilePath.getFileName());
                return null;
            }

            return new GunPack(path, info.getName());
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
            GunMod.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static GunPack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry extDescriptorEntry = zipFile.getEntry("gunpack.meta.json");
            if (extDescriptorEntry == null) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No gunpack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(extDescriptorEntry)) {
                PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (info == null) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
                    return null;
                }

                if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                    GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
                }

                return new GunPack(path, info.getName());
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<GunPack> scanExtensions(Path extensionsPath) {
        List<GunPack> gunPacks = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)){
            for (Path entry : stream) {
                GunPack gunPack = null;
                if (Files.isDirectory(entry)) {
                    gunPack = fromDirPath(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    gunPack = fromZipPath(entry);
                }
                if (gunPack != null) {
                    GunMod.LOGGER.info(MARKER, "- {}, Main namespace: {}", gunPack.path.getFileName(), gunPack.name);
                    gunPacks.add(gunPack);
                }
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", extensionsPath, e);
        }

        return gunPacks;
    }

    private static boolean modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        for (String modId : dependencies.keySet()) {
            if (!modVersionMatch(modId, dependencies.get(modId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean modVersionMatch(String modId, String version) throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        return FabricLoader.getInstance().getModContainer(modId).map(mod -> {
            DefaultArtifactVersion modVersion = new DefaultArtifactVersion(mod.getMetadata().getVersion().getFriendlyString());
            return versionRange.containsVersion(modVersion);
        }).orElse(false);
    }


    public record GunPack(Path path, String name) {
    }
}
