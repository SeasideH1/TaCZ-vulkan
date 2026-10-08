# Core integration boundary

The source modules in this directory are preserved research inputs. They do not ship in the core artifact and are not claimed to support Minecraft 26.4 snapshot 3. A future migrated integration must compile against its real target API and pass its own runtime checks before being packaged.

## Startup

A migrated Fabric addon can implement `com.tacz.guns.init.CompatRegistry.Integration` and expose it through a `tacz:common_integration` or `tacz:client_integration` entrypoint in its own mod descriptor. The interface supplies `supportedMods()` and `initialize()`.

Common initialization runs after TACZ registration and before resource reload setup. Client initialization runs at client startup after core resource/renderer registration. A present third-party mod without a registered compatible adapter is reported in the log. Neither its name nor an old Forge implementation is treated as proof of compatibility.

## Available hooks

- `ClientIntegrationHooks.registerCamera`: crosshair visibility and signed camera recoil deltas. Returning false from recoil leaves the update to the native camera path.
- `ClientIntegrationHooks.registerShadowPass`: reports the current shader shadow-render pass.
- `ClientIntegrationHooks.registerShootFeedback`: supplies controller/haptic feedback for a shot.
- `ScriptIntegrationHooks.register`: immediate common, client, or server script event delivery, retaining constructor-time cancellation behavior. No KubeJS engine is included by this boundary.
- `ClientConfigScreens.register`: optional settings screen factory. The core's default factory is the real native settings editor.

Registrations return closeable handles. Camera and shader providers are single-owner and reject conflicting registration. Listener registrations are independent even when the same callback object is registered twice. Closing an old handle does not remove a later registration of that object.

Rendering acceleration, recipe-viewer registration, Carry On behavior, and other engine-specific features still require an actual migrated addon; these hooks do not claim to implement those external systems.

## Core animation profile

`render.ThirdPersonAnimationMode = RELEASE_FALLBACK` is the minimal official Forge + TACZ comparison profile. `AUTHORED_CLIPS` explicitly enables the retained player-animation evaluator and pack clips, corresponding to the original optional PlayerAnimator path. The evaluator remains part of the core source rather than one of these external compatibility modules.
