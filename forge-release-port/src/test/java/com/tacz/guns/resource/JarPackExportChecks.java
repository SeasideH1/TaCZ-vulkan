package com.tacz.guns.resource;

import com.tacz.guns.util.GetJarResources;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

/** Actual class-loader and production exporter check; first export does not use loader backup paths. */
public final class JarPackExportChecks {
    private static int assertions;
    private static void check(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("Check " + assertions);
    }
    private static void entry(JarOutputStream jar, String path, byte[] data) throws Exception {
        jar.putNextEntry(new JarEntry(path));
        jar.write(data);
        jar.closeEntry();
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "jar-export-");
        Path source = root.resolve("Anchor.java");
        Files.writeString(source, "package fixture; public final class Anchor { }");
        check(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", root.toString(), source.toString()) == 0);
        byte[] anchor = Files.readAllBytes(root.resolve("fixture/Anchor.class"));
        byte[] metadata = "{\"name\":\"fixture\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] nested = { 0, 1, 2, 3, 4, 127, -1 };
        String pack = "assets/fixture/custom/default";
        for (boolean directories : new boolean[] { false, true }) {
            Path file = root.resolve(directories ? "with-directories.jar" : "files-only.jar");
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
                entry(jar, "fixture/Anchor.class", anchor);
                if (directories) {
                    entry(jar, pack + "/", new byte[0]);
                    entry(jar, pack + "/nested/", new byte[0]);
                }
                entry(jar, pack + "/gunpack.meta.json", metadata);
                entry(jar, pack + "/nested/binary.bin", nested);
            }
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { file.toUri().toURL() }, null)) {
                Class<?> type = Class.forName("fixture.Anchor", true, loader);
                check((type.getResource("/" + pack) != null) == directories);
                Path output = root.resolve(directories ? "exported" : "missing");
                GetJarResources.copyModDirectory(type, "/" + pack, output, "default");
                check(Files.exists(output.resolve("default/gunpack.meta.json")) == directories);
                if (directories) {
                    check(Arrays.equals(metadata, Files.readAllBytes(output.resolve("default/gunpack.meta.json"))));
                    check(Arrays.equals(nested, Files.readAllBytes(output.resolve("default/nested/binary.bin"))));
                    check(Files.isRegularFile(output.resolve(".export-state.json")));
                    Path sentinel = output.resolve("default/user-change.txt");
                    Files.writeString(sentinel, "keep unchanged export");
                    GetJarResources.copyModDirectory(type, "/" + pack, output, "default");
                    check(Files.readString(sentinel).equals("keep unchanged export"));
                }
            }
        }
        System.out.println("PASS " + assertions + " real JAR lookup/export assertions; files-only JAR reproduces missing default export");
    }
}
