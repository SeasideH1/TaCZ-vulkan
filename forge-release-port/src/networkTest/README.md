# Focused target-API checks

Run from this repository:

```sh
python3 src/networkTest/run-tests.py
```

The runner uses the verified Minecraft, Fabric and Java-library classpaths in sibling directories and the Java 25 toolchain. It creates a timestamped report under `build/network-tests/`, including hashes of every tested source and separate compiler/test logs.

These are focused checks of production helpers and codecs, not a full-source build. The full production compile is a separate gate. No Minecraft client or server is booted by this suite, so it does not establish multiplayer, renderer, sound playback or gameplay parity.

Current coverage:

- `NetworkTransportTest`: message direction/type registration, counted-map bounds and duplicate rejection, protocol acknowledgements, game-thread scheduling/replies, reconnect identity and stale queued work
- `SyncedSerializerTest`: primitive/reload values, NBT serialization, supported dynamic conversion and mutable-value copy isolation
- `InputCompatibilityTest`: the target's actual SDL key/button mappings, modifier and action values, and interaction cancellation
- `BulletSpawnCodecTest`: all twenty release projectile-spawn fields, field order and double-precision velocity
- `GameThreadRepeaterTest`: deterministic executor/scheduler tests, cancellation and zero-delay publication races; not a real-time firing-cadence test
- `WalkDistanceFormulaTest`: the exact selected-release walking-distance arithmetic against the native avatar API
- `NativeMixinTargetTest`: exact target method descriptors, spawn shadow field and sprint/sensitivity invocation owners/counts; not a transformed-runtime mixin test
- `GunSoundRedirectTest`: actual target sound objects retain logical/file paths, pitch, volume distribution, weight and streaming metadata; not an audio playback test

- `ConfigEditSessionTest`: actual NightConfig files, detached edits/cancel/reset, scalar/list/enum validation, external-edit conflicts, all-scopes pre-validation, rollback after a later-scope persistence failure and retry

- `ClientTickDispatchTest`: client/UI ticks remain unconditional, paused synthetic player phases are skipped, and unpaused START/END ordering stays unchanged; no live pause-screen claim

The latest source set passed 345 assertions across these ten tests. The report for a run is the authoritative record for the exact source hashes tested.
