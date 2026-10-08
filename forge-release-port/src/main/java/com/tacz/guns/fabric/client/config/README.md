# Native settings adapter

This implementation is the core configuration UI. `ClientConfigScreens` uses its constructor by default, so the config key opens the editor without any external GUI mod. Approved optional providers may use the existing factory-registration API.

`NativeConfigScreen(Screen parent)` is the production constructor. It uses the actual client/common/preload schemas; server/world settings are not part of this editor. The generic schema renderer also exposes additional core settings such as the third-person animation profile. `NativeConfigLabels` retains all 44 original active Cloth labels and category keys. Defaults, enum choices and ranges come from the schema rather than the sometimes-different values copied into the original optional UI.

The screen has category/search filtering, boolean switches, numeric/string editing, enum selection with crosshair previews, ordered string-list add/remove/move editing, per-entry reset, reset all, gun-pack directory opening, save and cancel. Nested string-list settings are editable as validated JSON. The parent screen is preserved through child pickers and cancellation.

`ConfigEditSession` is a detached transaction: draft changes, resets and cancellation do not change live settings or disk. Save validates every scope, rejects changes made outside the editor, and uses atomic per-file replacement. If a later scope fails, previously written scopes are rolled back; a rollback failure is attached to the original exception. These guarantees do not imply a crash-atomic transaction spanning multiple files.

Verification:
- `python3 src/networkTest/run-tests.py`: actual NightConfig model tests cover edits, reset/cancel, malformed scalar/list/enum values, persistence/reload, conflict rejection, forced later-file failure rollback and successful retry.
- `python3 src/networkTest/check-config-catalog.py`: checks the 44 active original Cloth save-consumer fields have matching native labels.
- The approved core source set now passes full exact-target compilation after optional integrations were preserved in separate source modules.
- Actual fourth-candidate game testing opened the screen with Alt+T, changed HoldToAim, canceled and reopened to verify the original value, then saved and verified the changed TOML value. Evidence: `validation/core-runs/20261008T063824Z/config-smoke.json` in the sibling workspace. Category/search/enum/list editing, focus, resizing and narration still need runtime coverage. This basic flow is not full GUI parity.
