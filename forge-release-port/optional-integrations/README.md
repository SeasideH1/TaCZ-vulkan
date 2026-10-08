# Preserved third-party integration modules

The active root project implements original TACZ core and the default gunpack. These nine external integration modules are preserved source-only, not installed, compiled, registered or advertised as supported on the target. No core gameplay source is excluded by a compiler filter.

`manifest.json` records every moved source and plugin resource with its byte hash. Each module has a separate `src/main` tree and its own status. Required pure-Java player animation support remains in the active core, with release fallback poses as the default profile.

The root Gradle build can archive these sources separately via `optionalIntegrationsSources`; it does not construct an unsupported binary.
