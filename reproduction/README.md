# Build inputs and corresponding source

Run from the repository root with Python 3.12+ and JDK 25. The reviewed direct
compiler/packager is the candidate build path; Gradle parity is not established.

```powershell
python reproduction/rehydrate.py --fetch --local-jdk
python toolchains/prepare-fabric-compile-api.py --java-home "$env:JAVA_HOME"
python toolchains/direct-build.py forge-release-port --java-home "$env:JAVA_HOME"
python toolchains/package-runtime-candidate.py
```

On Linux use `"$JAVA_HOME"`. Alternatively, `rehydrate.py --fetch --install-jdk`
retrieves the pinned Linux x64 JDK; then omit `--java-home`. The pinned Linux
toolchain and a locally supplied JDK may produce different output bytes.

`build-inputs.lock.json` supplies official HTTPS URLs, sizes, hashes and classpath
order. Inputs are checked before use. Existing mismatched files cause an error;
the downloader does not silently replace them. `--local-jdk` skips the Linux JDK
archive. `--assets`, `--with-server` and `--with-gradle` are optional downloads,
not required for the direct client/core compile. No game or login is launched.

The compile-API tool applies Fabric API's published transitive access rules to a
**compile-only** game JAR. Runtime uses the original official game client.
The direct compiler compiles every active core Java source, disables annotation
processing, and records source/resource/build-input hashes. The runtime packager
refuses stale sources and bundles the nine declared pure Java libraries, checking
their original bytes, licenses, entrypoints and mixin declarations.

Final output: `forge-release-port/build/direct/<timestamp>/*-runtime-candidate-<hash>.jar`.
Local build reports contain machine-specific paths and are excluded from Git.

## License/source materials

- `dependency-sources/`: original corresponding-source JARs for all nine bundled libraries, with provenance and hashes.
- `source-reference/`: original SimpleBedrockModel source archive for the adapted LGPL files.
- `license-evidence/`: original upstream license/notice texts, Maven POMs and provenance.
- `source-provenance.json`: pinned upstream and candidate18 source hashes before publication notices; no local absolute paths.

The preserved Gradle Wrapper is a separate Apache-2.0 build tool. Its license is
included at `forge-release-port/gradle/wrapper/LICENSE`; source and attribution
are available in the [Gradle 9.7.1 repository](https://github.com/gradle/gradle/tree/v9.7.1).
It is not bundled into the mod runtime. The direct build does not execute it.

When distributing your own binary, retain the applicable notices and provide its
matching Corresponding Source under the applicable GPL/LGPL requirements. Do not
present a different checkout or an older candidate as the source of that binary.
This repository does not redistribute game/JDK binaries or third-party gunpacks.
