/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.config;

import com.tacz.guns.fabric.config.FabricConfigSpec;
import java.nio.file.Path;

/** Standalone pre-pack configuration, deliberately outside the normal config directory. */
public final class PreLoadModConfig {
    private final FabricConfigSpec spec;
    private final String fileName;
    public PreLoadModConfig(FabricConfigSpec spec, String fileName) { this.spec = spec; this.fileName = fileName; }
    public FabricConfigSpec getSpec() { return spec; }
    public String getFileName() { return fileName; }
    public void load(Path directory) { spec.load(directory.resolve(fileName)); }
    public void save() { spec.save(); }
    public Path getFullPath() { return spec.getPath(); }
}
