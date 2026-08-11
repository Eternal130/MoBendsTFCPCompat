# PROJECT KNOWLEDGE BASE

**Generated:** 2026-08-11
**Commit:** dcdad51
**Branch:** clothing-as-armor-pass

## OVERVIEW

Client-side Minecraft 1.7.10 Forge mod that makes TFC+ clothing (shirts, pants, coats, hats, boots, cloaks) follow Mo'Bends' animated player body. The mod takes over TFC+ clothing rendering entirely: two Mixins neutralise TFC+'s own clothing pass and the one line that clobbers Mo'Bends' leggings model; a Forge event subscriber (`MobendsClothingRenderer`) re-renders every clothing piece inside Mo'Bends' coordinate frame at `RenderPlayerEvent.Specials.Post`, using a `ModelBiped` subclass that mirrors Mo'Bends' part hierarchy. Compiles to Java 8 bytecode, built with RetroFuturaGradle (GTNH convention plugin) on a JDK 25 daemon. 11 source files, 1790 LOC.

## STRUCTURE

```
{root}/
├── src/main/java/com/eternal130/mobends_tfcp_compat/
│   ├── MoBendsTFCPCompat.java        # @Mod entry; @SidedProxy wiring; mod IDs
│   ├── CommonProxy.java              # server no-op; logs dep presence
│   ├── ClientProxy.java              # client proxy; logs dep presence + registers MobendsClothingRenderer on EVENT_BUS
│   ├── MobendsClothingRenderer.java  # THE FIX (pass 1): @SubscribeEvent at Specials.Pre/Post — renders all clothing
│   ├── ModelBipedClothingAdapter.java# ModelBiped subclass mirroring MoBends hierarchy + per-type TFC+ geometry
│   ├── CoatSkirtModel.java           # flared hem (coat/robe/skirt) as 12 TexturedQuads, rebuilt per-frame from leg angles
│   ├── TFCPCapeRenderer.java         # TFC+-shaped cloak animated with MoBends' cape cloth physics (20 hinged slabs)
│   ├── StrawHat2Model.java           # conical straw hat as a ModelRenderer (4-sided cone + chin strap)
│   ├── ScaledModelRenderer.java      # ModelRenderer with per-axis scale (for hat parts)
│   └── mixin/
│       ├── MixinRenderClothing.java     # @Inject HEAD cancellable on RenderClothing.doRender → disables TFC+ pass
│       └── MixinPlayerRenderHandler.java# @Inject RETURN on PlayerRenderHandler.onPlayerRenderTick → restore ModelBendsPlayer armor model
├── src/main/resources/
│   ├── mcmod.info                    # Forge mod metadata (tokens substituted at build)
│   └── mixins.mobends_tfcp_compat.json  # Mixin config: required, JAVA_8, defaultRequire:1, 2 mixins
├── dependencies.gradle               # CurseMaven coords (rfg.deobf) for Mo'Bends + TFC+
├── gradle.properties                 # modId, modGroup, usesMixins=true, mixinsPackage=mixin
├── build.gradle.kts                  # single line: gtnhconvention plugin
├── settings.gradle.kts               # GTNH Maven for RetroFuturaGradle
├── README.md                         # user-facing doc + architecture overview
├── README_zh.md                      # 中文版 README
├── CLOAK_SHAPE.md                    # TFC+ ModelCloak shape reference (Chinese)
├── HAT_SHAPE.md                      # TFC+ ModelHat shape & dispatch reference
├── MOBENDS_CAPE_PHYSICS.md           # MoBends BendsCapeRenderer physics analysis (Chinese)
└── AGENTS.md                         # THIS FILE
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Change clothing render logic | `MobendsClothingRenderer.java` | `onSpecialsPost()` is the per-frame loop — binds texture, syncs pose, dispatches by `ClothingType` |
| Add/rework a clothing piece's geometry | `ModelBipedClothingAdapter.java` | `configureGeometry()` switch on `ClothingType` → `configureShirtOrCoat/Pants/Footwear/Hat/FurHat/Robe/Cloak` |
| Mirror Mo'Bends pose into clothing | `ModelBipedClothingAdapter.syncFromModelBiped` | Copies rotateAngle from the live ModelBendsPlayer; `setRotationAngles` is NOP'd so vanilla walk math doesn't overwrite |
| Change which TFC+ ClothingType maps to which adapter type | `MobendsClothingRenderer.mapClothingType` | TFC+ `THINSHIRT/THINCOAT/HEAVYCOAT/...` collapse to adapter's `SHIRT/COAT` |
| Tweak per-type box inflation | `MobendsClothingRenderer.clothingScale` | PANTS=0.25, SOCKS=0.2, BOOTS=0.6, COAT/ROBE=0.6, default 0.5 — values from TFC+ models |
| Coat/robe/skirt hem draping | `CoatSkirtModel.java` | 12 TexturedQuads rebuilt from leg angles each frame; vertex frame conversion `(-x,y,-z)` is load-bearing |
| Cloak shape + physics | `TFCPCapeRenderer.java` | 20 hinged slabs, MoBends cosine wave; driven from `MobendsClothingRenderer.renderCloak` which supplies MoBends `Data_Player` |
| Hat geometry | `ModelBipedClothingAdapter.configureHat/configureFurHat` + `StrawHat2Model` + `ScaledModelRenderer` | `hatBase` carries the -0.2 worn tilt + 1.1x scale |
| Change whether TFC+'s clothing pass runs | `mixin/MixinRenderClothing.java` | HEAD-cancellable `@Inject` — drop this and TFC+ renders double (clothes-on-clothes z-fight) |
| Change leggings-model restore | `mixin/MixinPlayerRenderHandler.java` | `@Inject RETURN` on `onPlayerRenderTick` re-caches a `ModelBendsPlayer(0.5F)` after TFC+ clobbers `modelArmor` |
| Add a Mixin class | `mixin/` + register in `mixins.mobends_tfcp_compat.json` | `mixinsPackage` is `mixin` per gradle.properties |
| Update target mod version | `dependencies.gradle` + bump CurseMaven fileId | Then re-verify both Mixin `target`/`method` strings + `mapClothingType`/`configureXxx` against new TFC+ bytecode |
| Mod ID / package constants | `MoBendsTFCPCompat.java` | `MODID`, `MODID_MOBENDS="mobends"`, `MODID_TFCP="terrafirmacraftplus"` |
| Why the fix is needed | `README.md` §"Cause" + §"How it works" | Frame-mismatch reasoning + hierarchy-mirroring rationale — READ BEFORE EDITING THE RENDERER OR ADAPTER |

## CODE MAP

46 symbols across 11 files. Reference centrality from codegraph (⚠️ = no tests cover this path):

| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `MoBendsTFCPCompat` | class | `MoBendsTFCPCompat.java:14` | 2 | `@Mod` entry; holds `@SidedProxy proxy`, forwards FML events |
| `MoBendsTFCPCompat.proxy` | field | `MoBendsTFCPCompat.java:23` | 3 | Client/Common proxy selector |
| `MoBendsTFCPCompat.MODID*` | const | `MoBendsTFCPCompat.java:16-18` | 2 | `mobends_tfcp_compat`, `mobends`, `terrafirmacraftplus` |
| `Tags.VERSION` | (generated) | build-time | 1 | Gradle token, class `com.eternal130.mobends_tfcp_compat.Tags` |
| `CommonProxy` | class | `CommonProxy.java:13` | 2 | Server base; `preInit` logs dep presence, `init`/`postInit` empty |
| `ClientProxy` | class | `ClientProxy.java:16` | 1 | Client override; `init` registers `MobendsClothingRenderer` on EVENT_BUS |
| `MobendsClothingRenderer` | class | `MobendsClothingRenderer.java:42` | 1 ⚠️ | Forge `@SubscribeEvent` host; ctor inits cape renderer + 3 skirt models |
| `MobendsClothingRenderer.onPre` | method | `MobendsClothingRenderer.java:73` | 0 ⚠️ | `RenderPlayerEvent.Pre` HIGHEST — diagnostic logging only |
| `MobendsClothingRenderer.onSpecialsPre` | method | `MobendsClothingRenderer.java:87` | 0 ⚠️ | `Specials.Pre` HIGHEST — sets `e.renderCape=false` when player has a cloak |
| `MobendsClothingRenderer.onSpecialsPost` | method | `MobendsClothingRenderer.java:96` | 0 ⚠️ | **THE FIX (pass 1):** per-clothing-item loop — map type, sync pose, bind tex, tint, dispatch adapter/skirt/cloak |
| `MobendsClothingRenderer.renderCloak` | method | `MobendsClothingRenderer.java:243` | 1 ⚠️ | Wraps `TFCPCapeRenderer`: enters body frame, applies vanilla cape rotation, applies MoBends wave |
| `MobendsClothingRenderer.applyClothingTint` | method | `MobendsClothingRenderer.java:218` | 1 ⚠️ | Dye color × wetness; leather-sandals extra tint |
| `MobendsClothingRenderer.mapClothingType` | method | `MobendsClothingRenderer.java:357` | 1 ⚠️ | TFC+ ClothingType → adapter ClothingType (collapses THIN/HEAVY variants) |
| `MobendsClothingRenderer.clothingScale` | method | `MobendsClothingRenderer.java:328` | 1 ⚠️ | Per-type box inflation (PANTS=0.25, SOCKS=0.2, …) |
| `MobendsClothingRenderer.collectClothing` | method | `MobendsClothingRenderer.java:381` | 2 ⚠️ | Gathers IEquipable items from `extraEquipInventory` (or `PlayerInfo`) + `armorInventory` |
| `ModelBipedClothingAdapter` | class | `ModelBipedClothingAdapter.java:29` | 1 ⚠️ | `extends ModelBiped`; mirrors MoBends part hierarchy; one instance per ClothingType |
| `ModelBipedClothingAdapter.ClothingType` | enum | `ModelBipedClothingAdapter.java:31` | 4 | SHIRT/PANTS/SHORTS/SOCKS/BOOTS/FULLBOOTS/SANDALS/CLOTH_HAT/STRAW_HAT/STRAW_HAT2/FUR_HAT_BEAR/FUR_HAT_WOLF/COAT/ROBE/SKIRT/CLOAK |
| `ModelBipedClothingAdapter.configureGeometry` | method | `ModelBipedClothingAdapter.java:84` | 1 ⚠️ | Builds the part tree (body→head/arms/legs/skirt) + dispatches `configureXxx` per type |
| `ModelBipedClothingAdapter.syncFromModelBiped` | method | `ModelBipedClothingAdapter.java:391` | 1 ⚠️ | Per-frame pose copy; uses MoBends `ModelRendererBends.sync` for arms (rot + pre_rotation for attack anims) |
| `CoatSkirtModel` | class | `CoatSkirtModel.java:33` | 1 ⚠️ | 12-quad flared hem; rebuilt each frame from live leg rotations |
| `CoatSkirtModel.render` | method | `CoatSkirtModel.java:58` | 1 ⚠️ | Computes 4 leg vectors × rotations → 16 hip/leg vertices → 12 TexturedQuads |
| `TFCPCapeRenderer` | class | `TFCPCapeRenderer.java:34` | 1 ⚠️ | TFC+-shaped cloak: shoulder plate + 20 hinged slabs |
| `TFCPCapeRenderer.applyAnimation` | method | `TFCPCapeRenderer.java:80` | 1 ⚠️ | MoBends cosine wave: magnitude grows with depth, 0.35 clamp, root -10° offset |
| `StrawHat2Model` | class | `StrawHat2Model.java:26` | 0 ⚠️ | `extends ModelRenderer`; 4-sided cone + chin strap as TexturedQuads |
| `ScaledModelRenderer` | class | `ScaledModelRenderer.java:21` | 0 ⚠️ | `extends ModelRenderer`; adds per-axis scale (rebuilt display list — vanilla fields are private) |
| `MixinRenderClothing` | class | `mixin/MixinRenderClothing.java:27` | 0 ⚠️ | `@Mixin(RenderClothing.class, remap=false)`, abstract |
| `...$disableFloatThird` | method | `mixin/MixinRenderClothing.java:32` | 0 ⚠️ | `@Inject HEAD cancellable` on the float-3rd-arg `doRender` → `ci.cancel()` |
| `MixinPlayerRenderHandler` | class | `mixin/MixinPlayerRenderHandler.java:35` | 0 ⚠️ | `@Mixin(PlayerRenderHandler.class, remap=false)`, abstract |
| `...$restoreBendsArmorModel` | method | `mixin/MixinPlayerRenderHandler.java:41` | 0 ⚠️ | `@Inject RETURN` on `onPlayerRenderTick` — re-caches `ModelBendsPlayer(0.5F)` into `renderer.modelArmor` |

**Call graph:** `FML → MoBendsTFCPCompat.init → ClientProxy.init → MinecraftForge.EVENT_BUS.register(MobendsClothingRenderer)`. Per-frame: vanilla `RenderPlayer` fires `RenderPlayerEvent.Specials.Post` → `MobendsClothingRenderer.onSpecialsPost` → for each clothing item: `mapClothingType` → cached `ModelBipedClothingAdapter.syncFromModelBiped + render` (+ `CoatSkirtModel.render` for COAT/ROBE/SKIRT, `renderCloak → TFCPCapeRenderer` for CLOAK). Separately: TFC+ `PlayerRenderHandler.onPlayerRenderTick` runs → `MixinPlayerRenderHandler.restoreBendsArmorModel` re-injects `ModelBendsPlayer`. The Mixin `MixinRenderClothing.disableFloatThird` cancels TFC+'s own `RenderClothing.doRender` so the two clothing passes don't double-render.

## CONVENTIONS

- **Java 8 bytecode output, JDK 25 build toolchain.** `enableModernJavaSyntax=false` means the **output jar** is J8 bytecode, but the build itself runs on a modern JDK: `gradle-daemon-jvm.properties` pins the daemon to JDK 25, and RFG's toolchain does the J8 downgrade at compile time. Do not introduce `var`, records, switch expressions, or `Stream.toList()` in source — they won't downgrade cleanly.
- **`gradle-daemon-jvm.properties` pins toolchain version.** This file requires a JDK matching `toolchainVersion` (currently 25) installed on the system. Gradle will try to auto-download from foojay if missing, but that endpoint has served broken packages (missing `javadoc`/`jar` executables) — install the system package (`openjdk-25-jdk-headless`) instead of relying on auto-provisioning.
- **One Mixin per file**, registered by simple class name in `mixins.mobends_tfcp_compat.json` (no FQCN in the array).
- **Mixin injection targets use full owner+name+descriptor** with `/`-separated internal names, never remapped names. `remap = false` on `@Mixin` because `RenderClothing` and `PlayerRenderHandler` are TFC+ classes, not vanilla ones.
- **`@Inject` method naming:** `mobends_tfcp_compat$<verb>` (project modId prefix avoids collisions in the mixin merge).
- **Package layout is enforced by build:** `modGroup=com.eternal130.mobends_tfcp_compat`, `mixinsPackage=mixin`. Adding a package outside `modGroup` breaks the build.
- **Proxy classes are referenced by FQCN string** in `@SidedProxy` — renaming requires updating the string too.
- **No test framework configured.** The project ships zero tests; verification is manual (runClient, wear each TFC+ clothing piece, walk/run/fall/fly in F5, check both local-player and remote-player views).
- **`MobendsClothingRenderer` is registered in `init`, not `preInit`** — by `init` all mods' classes are guaranteed loaded, and the renderer's constructor references TFC+ classes (`ModelCloak`, `TFCItems.*`).
- **Adapter instances are cached per `ClothingType`** in `adapterCache` (a `HashMap`), not per-player or per-item. Pose is re-synced each frame so a single shared instance serves every player. `SKIRT` bypasses the cache (skirt has no `ModelBiped` form).

## ANTI-PATTERNS (THIS PROJECT)

- **NEVER remove the `MixinRenderClothing` HEAD-cancellable.** Without it TFC+'s `RenderClothing.doRender` runs alongside `MobendsClothingRenderer`, drawing every clothing piece twice in different frames (immediate z-fighting, doubled draw calls). The cancel is the whole reason a separate compat renderer can exist.
- **NEVER remove the `MixinPlayerRenderHandler` armor-model restore.** TFC+'s `PlayerRenderHandler.onPlayerRenderTick` contains `e.renderer.modelArmor = new ModelBiped(0.75f)` which, on first frame, overwrites Mo'Bends' `ModelBendsPlayer(0.5F)` leggings model. Without the restore you get vanilla `ModelBox` leggings with no bend animation and the "belt box at the feet" symptom.
- **NEVER drop `defaultRequire: 1`.** Both Mixins target TFC+ method names/descriptors; if TFC+ renames either, silent failure is far worse than a startup crash — a broken compat is much harder for users to diagnose than no compat.
- **NEVER render clothing at `RenderPlayerEvent.Post`.** Use `Specials.Post`. `Post` fires **after** Mo'Bends' `glPushMatrix/postRender/popMatrix` block has already been popped — clothing rendered there sits in vanilla pose space and the body lean is lost. `Specials.Post` still has Mo'Bends' matrix on the stack. (Earlier iteration made exactly this mistake.)
- **NEVER flatten the adapter's part hierarchy.** Arms and head are children of `bipedBody` (not siblings) so they inherit the body lean. Restoring vanilla `ModelBiped` flat structure makes arms/head lag behind during sprint/fly/swim — clothing visibly detaches.
- **NEVER override `setRotationAngles` to call `super`.** It's NOP'd on purpose: vanilla walk/swing math would overwrite the pose synced from Mo'Bends by `syncFromModelBiped`. Animation data must come exclusively from the live Mo'Bends model.
- **NEVER wrap `adapter.render` in a `glScalef(1,-1,-1)` bracket.** The compat mod renders at `Specials.Post` inside Mo'Bends' frame, which has no `R(180,X)` flip. (The old bracket trick applied to the now-deleted `RenderClothing.doRender` injection; keeping it here would invert the clothing instead of fixing it.)
- **NEVER reintroduce `MoBendsTransformApplier` / a `postRender(scale,height)` call.** That class is deleted. The new approach doesn't re-apply the global body transform — it inherits it from the matrix stack and mirrors the per-part pose, which is both more correct and simpler.
- **NEVER move `CoatSkirtModel` rendering outside its `glPushMatrix/glPopMatrix`.** It draws raw `TexturedQuad`s with hand-computed vertices; if you let it leak into the next item's matrix the hem drifts.
- **NEVER change `CoatSkirtModel.v()` to `(x,y,z)`.** The vertex frame conversion is `(-x, y, -z)` — TFC+'s "front" verts carry +z (render frame front is -z) and its x is mirrored vs the model parts. Dropping either negation lands the hem on the wrong side / inside the body.
- **NEVER drop the `flyingSprint` cape-speed override in `renderCloak`.** When the player is flying+sprinting the cape must skip the vanilla `f6/f7` rotations and use `capeWaveSpeed=4.0` directly, or the cloak clips through the player at high pitch.
- **NEVER target the RenderPlayer-3rd-arg `doRender` overload of `RenderClothing`.** Only the float-3rd `(EntityLivingBase, ItemStack, float, RenderPlayer, ItemStack[])` is the live player-clothing path; the RenderPlayer-3rd variant is dead code with zero callers.
- **Do not mark `enableModernJavaSyntax=true`** to "fix" anything — the **output jar** must stay J8 bytecode for MC 1.7.10 runtime compatibility. The build already runs on a modern JDK (25) via `gradle-daemon-jvm.properties`; `enableModernJavaSyntax=false` is what forces the J8 downgrade at compile time.

## UNIQUE STYLES

- **`devOnlyNonPublishable(rfg.deobf("curse.maven:<slug>-<projectId>:<fileId>"))`** — Mo'Bends and TFC+ are pulled from CurseForge via CurseMaven (the repo is auto-registered by `includeWellKnownRepositories=true` in gradle.properties). `rfg.deobf` runs the obfuscated CurseForge jars through the MCP stable-12 deobf pipeline so we compile against deobfuscated names. Don't switch to `implementation`/`compileOnly` — they won't deobf. Never commit the jars to `libs/` — it risks CurseForge ToS / license violations and the directory is gitignored.
- **`Tags.VERSION` is a generated class** (`generateGradleTokenClass` in gradle.properties) substituted at build; never hand-edit, never hardcode a version in `@Mod`.
- **`disableSpotless = true` / `disableCheckstyle = true`** in gradle.properties — formatting is intentionally relaxed; do not "tidy" unrelated files.
- **`.editorconfig`** mandates 4-space indent for `.java`, 2-space for JSON/`mcmod.info`/`.md`, and `trim_trailing_whitespace = false` for `.md` (preserve markdown hard line breaks).
- **`@Mixin(..., remap = false)`** is mandatory on every Mixin here — the targets (`com.dunk.tfc.*`) are mod classes, not notch/MCP names that need runtime remapping.
- **Adapter arms use MoBends' `ModelRendererBends`** (not vanilla `ModelRenderer`) so they can `sync()` from the live `ModelBendsPlayer` arms — this carries `pre_rotation` smooth vectors, which is how attack animations (`Animation_Attack_Punch` etc.) land on the clothing sleeves. Legs and head stay vanilla `ModelRenderer` (no MoBends-specific data beyond `rotateAngle`).
- **`CoatSkirtModel` ports TFC+'s `ModelCoat.render` 1:1** but with a vertex conversion `(-x, y, -z)` because TFC+ draws in its own flipped frame (full chain collapses to `R(180,Z)`) while the compat mod draws in Mo'Bends' frame at `Specials.Post`.
- **`TFCPCapeRenderer` is a hybrid:** TFC+ shape (shoulder plate + trapezoidal first slab + hanging cloth), MoBends physics (cosine wave over 20 slabs, `hingeOffset` front-edge fold, `Data_Player.getCapeWavePhase`). See `MOBENDS_CAPE_PHYSICS.md` and `CLOAK_SHAPE.md`.

## COMMANDS

```bash
# Requires JDK 25 (pinned by gradle-daemon-jvm.properties — daemon toolchain)
./gradlew setupDecompWorkspace   # first-time only; sets up deobf MCP mappings
./gradlew build                  # → build/libs/mobends_tfcp_compat-<version>.jar (J8 bytecode)
./gradlew runClient              # launch MC 1.7.10 with both dev jars on classpath
./gradlew runServer              # dedicated server (proxy is a no-op there)
```

`./gradlew setupCIWorkspace` is what jitpack.yml runs pre-build.

**Build stack layering (verified):** gradle daemon (JDK 25, from `gradle-daemon-jvm.properties`) → RFG toolchain auto-detects system JDK 25 → compiles with JDK 25 javac → forces J8 bytecode via `enableModernJavaSyntax=false` → `reobfJar` remaps to MC 1.7.10 notch names. Do **not** rely on foojay auto-provisioning — it has served broken packages (missing `javadoc`/`jar`); install `openjdk-25-jdk-headless` from apt.

## NOTES

- **No tests exist.** codegraph flags the entire render/adapter/mixin surface as untested. Verification is behavioral: launch `runClient`, equip each TFC+ clothing piece (shirt, pants, shorts, socks, boots, sandals, cloth/straw/straw2/fur hats, coat, robe, skirt, cloak), walk/run/fall/fly in F5, check both local-player and remote-player views.
- **Mo'Bends and TFC+ are pulled from CurseMaven, not committed jars.** Bumping a target mod version means looking up the new CurseForge file, copying its fileId into `dependencies.gradle` (coord `curse.maven:<slug>-<projectId>:<fileId>`), and re-validating: both Mixin `target`/`method` strings, `mapClothingType`'s TFC+ ClothingType set, and every `configureXxx` against the new TFC+ model bytecode. Never commit mod jars to `libs/` — gitignored, license risk.
- **Mixin config `minVersion: 0.8.3-GTNH`** and `compatibilityLevel: JAVA_8` — this assumes UniMixins (ships with GTNH, Angelica, RPL). Don't lower `minVersion` to stock Sponge Mixin; the GTNH-specific refmap behavior is relied upon.
- **Two Mixins, two different jobs.** `MixinRenderClothing` cancels (disable TFC+ pass); `MixinPlayerRenderHandler` restores (re-cache MoBends armor model). They are independent — one can be removed without breaking the other's compile, but removing either reintroduces a distinct visual bug.
- **Adapter cache is keyed by `ClothingType`, not by item.** A shirt and a thin shirt share one `SHIRT` adapter instance — geometry differences (sleeveless) are decided at adapter construction via `isSleevelessShirt(item)`, which means the FIRST shirt seen seeds the cached adapter. If TFC+ adds a shirt that needs different geometry beyond sleeveless, the cache key must widen.
- **`collectClothing` has two code paths.** Modern TFC+ exposes `extraEquipInventory` on `InventoryPlayer` (found by reflection); older builds keep it in `PlayerManagerTFC.getPlayerInfoFromName(...).myExtraItems`. The reflection result is memoized in `extraEquipFieldResolved` so the lookup cost is paid once.
- **Iterating on a Mixin:** `defaultRequire: 1` means any miss (wrong descriptor, renamed target) hard-crashes the game at startup with a clear Mixin apply error. Treat that as a feature, not a bug — see README §"TFC+ signature changes".
- **Per-part vs. whole-body transform (architectural note):** the previous approach (deleted) re-applied Mo'Bends' whole-body GL transform via `ModelBendsPlayer.postRender` inside TFC+'s render. The current approach inherits the whole-body transform from the matrix stack at `Specials.Post` and copies only per-part rotations into the adapter. This is why `glScalef(1,-1,-1)` brackets no longer appear anywhere — they existed to undo a frame flip that the new injection point never introduces.
