# MoBends TFCP Compat

A small client-side compatibility mod that fixes "clothing/body separation" when **Mo'Bends** and **TerraFirmaCraft Plus (TFC+)** are loaded together on Minecraft 1.7.10.

## Symptom

Wearing TFC+ clothing (shirts, pants, coats, …) while Mo'Bends is active causes the clothing to stay in the vanilla pose while the player body bends — so when walking/running/falling/flying the clothes visibly detach from the body.

## Cause (one-paragraph version)

Mo'Bends applies its whole-body animation transform (lean, offset, flight pitch) inside `RenderBendsPlayer.rotateCorpse` via `ModelBendsPlayer.postRender(scale, height)`. That call sits inside the `glPushMatrix/glPopMatrix` block of `RenderLivingEntity.doRender`, which has already been popped by the time `RenderPlayerEvent.Post` fires. TFC+ renders clothing from that Post event, so its clothing pass runs in vanilla pose space and never sees Mo'Bends' body transform.

## Fix

A Mixin into `com.dunk.tfc.Render.RenderClothing.doRender(...)` injects `ModelBendsPlayer.postRender(0.0625F, entityHeight)` at the correct matrix slot: **after** TFC+ has finished rebuilding "entity at feet, vanilla model space" (the `-cpPos + entityPos` repositioning, `glScalef(-1,-1,1)`, the 180° flips, and the renderYawOffset rotation), and **before** `switchRender` draws the clothing boxes. That slot exactly mirrors where Mo'Bends itself applies the transform during the main render pass, so clothing inherits the body lean.

No changes are made to either Mo'Bends or TFC+ — this mod is purely additive.

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614+ |
| Mo'Bends (Reforged) | 1.1.0+ |
| TFC+ | 0.89.1+ |
| UniMixins | any modern build (ships with GTNH, Angelica, RPL, etc.) |

If either Mo'Bends or TFC+ is missing the mod logs a warning and does nothing — it will not crash your game.

## Install

Drop `mobends_tfcp_compat-1.0.0.jar` into your `mods/` folder alongside Mo'Bends and TFC+. That's it.

## Building from source

```bash
# Requires JDK 25 (pinned by gradle-daemon-jvm.properties — RFG daemon toolchain)
./gradlew setupDecompWorkspace
./gradlew build
# Output: build/libs/mobends_tfcp_compat-<version>.jar (Java 8 bytecode)
```

The `libs/` directory holds the Mo'Bends and TFC+ dev (deobf) jars for compilation. Update them if you target newer versions of either mod.

## How it works (details)

```
RenderClothing.doRender(entity, item, partial, renderer, armor)
├── glPushMatrix
├── glTranslated(-cpPos)                  ← TFC+ rebuilds view space → world space
├── makeAdjustments                       ← +entityPos, glScalef(-1,-1,1), glTranslatef(0,-24s,0)
├── glRotatef(180, 0,0,1) / (180, 1,0,0) ← vanilla ModelRenderer orientation
├── glRotatef(renderYawOffset, 0,1,0)    ← (applied inside switchRender's models)
├── bindTexture, onEquippedRender
│
├── ┌── Mixin: glPushMatrix                ← INJECTION POINT (before switchRender)
│   ├── Mixin: ModelBendsPlayer.postRender(0.0625F, entity.height)
│   │            ↑ applies renderOffset, centerQuat lean (pivot at body mid-height), renderRotation
│   ├── switchRender(...) → renders shirt/pants/coat/... boxes  ← INHERITS BODY LEAN
│   └── Mixin: glPopMatrix                 ← AFTER switchRender
│
└── glPopMatrix
```

The push/pop pair around `postRender` is critical so the matrix stays balanced for TFC+'s remaining cleanup.

## Limitations & caveats

- **F5 self-view vs. other players.** The fix is verified correct for both local and remote players. (An earlier iteration using `RenderPlayerEvent.Post` to wrap `RENDER_CLOTHING.render()` only worked for the local player because `entityPos ≈ cpPos` hid the bug; remote players broke catastrophically. The Mixin approach puts the transform in the right matrix slot for everyone.)
- **TFC+ signature changes.** If a future TFC+ renames `doRender` or `switchRender`, or changes their signatures, the Mixin will fail to apply and the game will refuse to start (this is deliberate — `defaultRequire: 1` — silent failure is worse). Update the Mixin `target` strings to match.
- **Mo'Bends superhero fallback.** When Mo'Bends detects a Fisk/Legends superhero suit it swaps its model to vanilla `ModelBiped`. The applier no-ops in that case (`modelBipedMain` is not a `ModelBendsPlayer`), so clothing renders in vanilla pose as TFC+ intended.

## License

MIT. See [LICENSE](LICENSE).
