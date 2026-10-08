import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.fabricmc.classtweaker.api.*;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import org.objectweb.asm.*;

/** Compile-only application of published transitive Fabric API class tweaks.
 * Never used for runtime launch; original official game JAR stays unchanged.
 */
public final class ApplyPublishedFabricTweaks {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("input.jar output.jar rules...");
        ClassTweaker tweaks = ClassTweaker.newInstance();
        ClassTweakerReader reader = ClassTweakerReader.create(ClassTweakerVisitor.transitiveOnly(tweaks));
        for (int i = 2; i < args.length; i++) reader.read(Files.readAllBytes(Path.of(args[i])), "official");
        int changed = 0;
        try (ZipFile input = new ZipFile(args[0]); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            var entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry source = entries.nextElement();
                if (source.isDirectory()) continue;
                byte[] data;
                try (var stream = input.getInputStream(source)) { data = stream.readAllBytes(); }
                if (source.getName().endsWith(".class")) {
                    byte[] before = data;
                    ClassReader cr = new ClassReader(data);
                    ClassWriter cw = new ClassWriter(0);
                    cr.accept(tweaks.createClassVisitor(Opcodes.ASM9, cw, (name, bytes) -> {
                        throw new IllegalStateException("Unexpected generated class in compile-only transitive API: " + name);
                    }), 0);
                    data = cw.toByteArray();
                    if (!Arrays.equals(before, data)) changed++;
                }
                ZipEntry target = new ZipEntry(source.getName());
                target.setTime(0);
                output.putNextEntry(target);
                output.write(data);
                output.closeEntry();
            }
        }
        System.out.println("Published transitive targets=" + tweaks.getTargets().size() + ", rewritten class entries=" + changed);
    }
}
