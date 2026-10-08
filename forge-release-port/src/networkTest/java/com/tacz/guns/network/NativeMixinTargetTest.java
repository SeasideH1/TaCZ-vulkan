package com.tacz.guns.network;

import java.lang.classfile.ClassFile;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.jar.JarFile;

/** Static descriptor gate against the selected official Minecraft jar, not a mixin/runtime boot claim. */
public final class NativeMixinTargetTest {
    public static void main(String[] arguments) throws Exception {
        String[][] methods = {
            {"net/minecraft/client/gui/Hud", "extractRenderState", "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"},
            {"net/minecraft/client/KeyboardHandler", "keyPress", "(JILnet/minecraft/client/input/KeyEvent;)V"},
            {"net/minecraft/client/MouseHandler", "onButton", "(JLnet/minecraft/client/input/MouseButtonInfo;I)V"},
            {"net/minecraft/client/MouseHandler", "onScroll", "(JDD)V"},
            {"net/minecraft/client/Minecraft", "startAttack", "()Z"},
            {"net/minecraft/client/Minecraft", "continueAttack", "(Z)V"},
            {"net/minecraft/client/Minecraft", "startUseItem", "()V"},
            {"net/minecraft/client/KeyMapping$Category", "label", "()Lnet/minecraft/network/chat/Component;"},
            {"net/minecraft/client/KeyMapping$Category", "id", "()Lnet/minecraft/resources/Identifier;"},
            {"net/minecraft/world/entity/Entity", "tick", "()V"},
            {"net/minecraft/client/multiplayer/MultiPlayerGameMode", "ensureHasSentCarriedItem", "()V"},
            {"net/minecraft/server/level/ServerEntity", "sendPairingData", "(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"}
        };
        int assertions = 0;
        try (JarFile jar = new JarFile(arguments[0])) {
            for (String[] target : methods) {
                var entry = jar.getJarEntry(target[0] + ".class");
                if (entry == null) throw new AssertionError("Missing mixin target class " + target[0]);
                var parsed = ClassFile.of().parse(jar.getInputStream(entry).readAllBytes());
                long matches = parsed.methods().stream().filter(method -> method.methodName().stringValue().equals(target[1])
                        && method.methodType().stringValue().equals(target[2])).count();
                if (matches != 1) throw new AssertionError("Expected exact target: " + String.join(" ", target));
                assertions++;
            }
            String[][] invocations = {
                {"net/minecraft/client/player/LocalPlayer", "aiStep", "()V", "net/minecraft/client/player/LocalPlayer", "setSprinting", "(Z)V", "4"},
                {"net/minecraft/client/MouseHandler", "turnPlayer", "(D)V", "net/minecraft/client/player/LocalPlayer", "turn", "(DD)V", "1"}
            };
            for (String[] target : invocations) {
                var parsed = ClassFile.of().parse(jar.getInputStream(jar.getJarEntry(target[0] + ".class")).readAllBytes());
                var method = parsed.methods().stream().filter(candidate -> candidate.methodName().stringValue().equals(target[1])
                        && candidate.methodType().stringValue().equals(target[2])).findFirst().orElseThrow();
                long matchingInvokes = method.code().orElseThrow().elementStream()
                        .filter(element -> element instanceof InvokeInstruction invoke
                                && invoke.owner().asInternalName().equals(target[3])
                                && invoke.name().stringValue().equals(target[4]) && invoke.type().stringValue().equals(target[5])).count();
                if (matchingInvokes != Long.parseLong(target[6])) throw new AssertionError("Wrapped invocation count changed: " + String.join(" ", target));
                assertions++;
            }
            var serverEntity = ClassFile.of().parse(jar.getInputStream(jar.getJarEntry("net/minecraft/server/level/ServerEntity.class")).readAllBytes());
            if (serverEntity.fields().stream().noneMatch(field -> field.fieldName().stringValue().equals("entity")
                    && field.fieldType().stringValue().equals("Lnet/minecraft/world/entity/Entity;"))) {
                throw new AssertionError("Spawn bundle mixin shadow field differs");
            }
            assertions++;
        }
        System.out.println("NativeMixinTargetTest: " + assertions + " exact-target assertions passed");
    }
}
