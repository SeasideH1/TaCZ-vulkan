package com.tacz.guns.crafting;

import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.AttachmentsTagManager;
import com.tacz.guns.fabric.resource.CompositeGunPackResources;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.Profiler;
import java.nio.file.Path;
import java.util.*;

/** Exercises the actual resource-stack loader, which direct JSON fixtures bypass. */
public class AttachmentResourceChecks extends AttachmentsTagManager {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        var location = new PackLocationInfo("test", Component.literal("test"), PackSource.BUILT_IN, Optional.empty());
        var directory = new PathPackResources(location, Path.of(args[0]));
        var composite = new CompositeGunPackResources(location, List.of(directory), null);
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(composite))) {
            var loader = new AttachmentResourceChecks();
            var data = loader.prepare(resources, Profiler.get());
            loader.apply(data, resources, Profiler.get());
            System.out.println("SCANNED_TAGS=" + data.size());
            System.out.println("M4A1_ALLOWED=" + loader.getAllowAttachmentTags(Identifier.parse("tacz:m4a1")));
            System.out.println("SCOPE_TAG=" + loader.getAttachmentTags(Identifier.parse("tacz:scope_scope")));
            if (loader.getAllowAttachmentTags(Identifier.parse("tacz:m4a1")) == null) throw new AssertionError("M4A1 whitelist missing");
            if (loader.getAttachmentTags(Identifier.parse("tacz:scope_scope")) == null) throw new AssertionError("Scope tags missing");
        }
    }
}
