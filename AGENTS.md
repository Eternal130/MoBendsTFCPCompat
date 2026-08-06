# PROJECT KNOWLEDGE BASE

**Generated:** 2026-08-06
**Commit:** 9b97b7f
**Branch:** master

## OVERVIEW

Client-side Minecraft 1.7.10 Forge mod. One Mixin injects Mo'Bends' whole-body animation transform into TFC+'s clothing renderer so clothes follow the bent body. Compiles to Java 8 bytecode, built with RetroFuturaGradle (GTNH convention plugin) on a JDK 25 daemon. 4 source files, 242 LOC.

## STRUCTURE

```
{root}/
├── src/main/java/com/eternal130/mobends_tfcp_compat/
│   ├── MoBendsTFCPCompat.java        # @Mod entry; @SidedProxy wiring; mod IDs
│   ├── CommonProxy.java              # server no-op; logs dep presence
│   ├── ClientProxy.java              # client proxy; logs dep presence (Mixin self-registers)
│   ├── MoBendsTransformApplier.java  # THE FIX: applies ModelBendsPlayer.postRender
│   └── mixin/
│       └── MixinRenderClothing.java  # @Inject push/postRender + pop around switchRender
├── src/main/resources/
│   ├── mcmod.info                    # Forge mod metadata (tokens substituted at build)
│   └── mixins.mobends_tfcp_compat.json  # Mixin config: required, JAVA_8, defaultRequire:1
├── libs/                             # Mo'Bends + TFC+ dev (deobf) jars — compile only
├── dependencies.gradle               # devOnlyNonPublishable(rfg.deobf(...)) for both jars
├── gradle.properties                 # modId, modGroup, usesMixins=true, mixinsPackage=mixin
├── build.gradle.kts                  # single line: gtnhconvention plugin
├── settings.gradle.kts               # GTNH Maven for RetroFuturaGradle
└── README.md                         # deep architectural doc — READ BEFORE EDITING THE MIXIN
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Change the fix logic | `MoBendsTransformApplier.java` | `apply()` is the entire fix — null-safe, no-ops on vanilla ModelBiped |
| Change injection point | `mixin/MixinRenderClothing.java` | Two `@Inject`s: before (push+apply) and AFTER switchRender (pop) |
| Add a Mixin class | `mixin/` + register in `mixins.mobends_tfcp_compat.json` | `mixinsPackage` is `mixin` per gradle.properties |
| Update target mod version | `dependencies.gradle` + replace jar in `libs/` | Then verify Mixin `target` strings still match |
| Mod ID / package constants | `MoBendsTFCPCompat.java` | `MODID`, `MODID_MOBENDS="mobends"`, `MODID_TFCP="terrafirmacraftplus"` |
| Why the fix is needed | `README.md` §"Cause" + §"How it works" | GL matrix slot reasoning — do not guess, read it |

## CODE MAP

23 symbols across 4 files. Reference centrality from codegraph (⚠️ = no tests cover this path):

| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `MoBendsTFCPCompat` | class | `MoBendsTFCPCompat.java:12` | 2 | `@Mod` entry; holds `@SidedProxy proxy`, forwards FML events |
| `MoBendsTFCPCompat.proxy` | field | `MoBendsTFCPCompat.java:23` | 3 | Client/Common proxy selector |
| `MoBendsTFCPCompat.MODID*` | const | `MoBendsTFCPCompat.java:16-18` | 2 | `mobends_tfcp_compat`, `mobends`, `terrafirmacraftplus` |
| `Tags.VERSION` | (generated) | build-time | 1 | Gradle token, class `com.eternal130.mobends_tfcp_compat.Tags` |
| `CommonProxy` | class | `CommonProxy.java:13` | 2 | Server base; `preInit` logs dep presence, `init`/`postInit` empty |
| `ClientProxy` | class | `ClientProxy.java:13` | 1 | Client override; only logs (Mixin self-registers via config) |
| `MoBendsTransformApplier` | class | `MoBendsTransformApplier.java:32` | 1 ⚠️ | `final`, private ctor — static utility only |
| `MoBendsTransformApplier.apply` | method | `MoBendsTransformApplier.java:48` | 1 ⚠️ | The fix: `bendsModel.postRender(0.0625F, entityHeight)` |
| `MoBendsTransformApplier.MODEL_SCALE` | const | `MoBendsTransformApplier.java:35` | 1 | `0.0625F` = vanilla 1/16 block scale |
| `MixinRenderClothing` | class | `mixin/MixinRenderClothing.java:37` | 0 ⚠️ | `@Mixin(RenderClothing.class, remap=false)`, abstract |
| `...$beforeSwitchRender` | method | `mixin/MixinRenderClothing.java:49` | 0 ⚠️ | `glPushMatrix` + `apply()` before `switchRender` |
| `...$afterSwitchRender` | method | `mixin/MixinRenderClothing.java:64` | 0 ⚠️ | `glPopMatrix` after `switchRender` (shift=AFTER) |

**Call graph:** `FML → MoBendsTFCPCompat.{pre,init,post}Init → proxy.*` (logging only). The actual fix path is `TFC+ Post handler → RenderClothing.doRender → [Mixin injection] → MoBendsTransformApplier.apply → ModelBendsPlayer.postRender`. The Mixin class is never instantiated by our code — TFC+'s `RenderClothing` is the host.

## CONVENTIONS

- **Java 8 bytecode output, JDK 25 build toolchain.** `enableModernJavaSyntax=false` means the **output jar** is J8 bytecode, but the build itself runs on a modern JDK: `gradle-daemon-jvm.properties` pins the daemon to JDK 25, and RFG's toolchain does the J8 downgrade at compile time. Do not introduce `var`, records, switch expressions, or `Stream.toList()` in source — they won't downgrade cleanly.
- **`gradle-daemon-jvm.properties` pins toolchain version.** This file requires a JDK matching `toolchainVersion` (currently 25) installed on the system. Gradle will try to auto-download from foojay if missing, but that endpoint has served broken packages (missing `javadoc`/`jar` executables) — install the system package (`openjdk-25-jdk-headless`) instead of relying on auto-provisioning.
- **One Mixin per file**, registered by simple class name in `mixins.mobends_tfcp_compat.json` (no FQCN in the array).
- **Mixin injection targets use full owner+name+descriptor** with `/`-separated internal names, never remapped names. `remap = false` on `@Mixin` because `RenderClothing` is a TFC+ class, not a vanilla one.
- **`@Inject` method naming:** `mobends_tfcp_compat$<verb>` (project modId prefix avoids collisions in the mixin merge).
- **Package layout is enforced by build:** `modGroup=com.eternal130.mobends_tfcp_compat`, `mixinsPackage=mixin`. Adding a package outside `modGroup` breaks the build.
- **Proxy classes are referenced by FQCN string** in `@SidedProxy` — renaming requires updating the string too.
- **No test framework configured.** The project ships zero tests; verification is manual (runClient, wear TFC+ clothing, observe).

## ANTI-PATTERNS (THIS PROJECT)

- **NEVER remove the `glPushMatrix`/`glPopMatrix` pair** around `postRender` in the Mixin. `switchRender` and its sub-models assume a balanced matrix on return — dropping the pop corrupts the GL stack and crashes TFC+'s remaining render.
- **NEVER target the 4-arg `doRender` overload.** Only the 5-arg `(EntityLivingBase, ItemStack, float, RenderPlayer, ItemStack[])` is the player-clothing path; the 4-arg variant is unrelated and patching it does nothing useful.
- **NEVER drop `ordinal = 0`** from the `switchRender` `@At`. Without it the injector fails to disambiguate if TFC+ ever adds a second call site, and `defaultRequire: 1` will refuse to start the game.
- **NEVER drop `defaultRequire: 1`.** Silent mixin failure is worse than a startup crash — a broken compat is much harder for users to diagnose than no compat.
- **NEVER replace the Mixin with a `RenderPlayerEvent.Post` listener.** That was the previous approach; it only worked for the local player (`entityPos ≈ cpPos` masked the bug) and broke catastrophically for remote players. See README §"F5 self-view vs. other players".
- **NEVER move the `postRender` call outside the push/pop.** It must execute inside the matrix scope TFC+ set up (after `-cpPos + entityPos`, scale, flips, yaw) for clothing to inherit the body lean.
- **Do not mark `enableModernJavaSyntax=true`** to "fix" anything — the **output jar** must stay J8 bytecode for MC 1.7.10 runtime compatibility. The build already runs on a modern JDK (25) via `gradle-daemon-jvm.properties`; `enableModernJavaSyntax=false` is what forces the J8 downgrade at compile time.

## UNIQUE STYLES

- **`devOnlyNonPublishable(rfg.deobf(project.files("libs/...")))`** — Mo'Bends and TFC+ are compile-only local dev jars, deobfuscated at configure time by RFG, never published as Maven deps. Don't switch to `implementation`/`compileOnly` — they won't deobf.
- **`Tags.VERSION` is a generated class** (`generateGradleTokenClass` in gradle.properties) substituted at build; never hand-edit, never hardcode a version in `@Mod`.
- **`disableSpotless = true` / `disableCheckstyle = true`** in gradle.properties — formatting is intentionally relaxed; do not "tidy" unrelated files.
- **`.editorconfig`** mandates 4-space indent for `.java`, 2-space for JSON/`mcmod.info`/`.md`, and `trim_trailing_whitespace = false` for `.md` (preserve markdown hard line breaks).
- **`@Mixin(..., remap = false)`** is mandatory on every Mixin here — the targets (`com.dunk.tfc.*`, `net.gobbob.mobends.*`) are mod classes, not notch/MCP names that need runtime remapping.

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

- **No tests exist.** codegraph flags `MoBendsTransformApplier` and `MixinRenderClothing` as fully untested. Verification is behavioral: launch `runClient`, equip TFC+ clothing, walk/run/fall/fly in F5, check both local-player and remote-player views.
- **Dependency jars in `libs/` are checked into the repo** (`mobends-1.1.0-dev.jar`, `terrafirmacraftplus-0.89.1-dev.jar`). Bumping a target mod version means replacing the jar AND re-validating the Mixin `target` descriptor strings against the new bytecode.
- **Mixin config `minVersion: 0.8.3-GTNH`** and `compatibilityLevel: JAVA_8` — this assumes UniMixins (ships with GTNH, Angelica, RPL). Don't lower `minVersion` to stock Sponge Mixin; the GTNH-specific refmap behavior is relied upon.
- **Two-part injection is load-bearing:** the before/after `@Inject` pair brackets `switchRender`. If you add a third injection, place it relative to `switchRender` deliberately — the matrix balance depends on push happening exactly once before and pop exactly once after.
- **`MoBendsTransformApplier` is `final` with a private constructor** on purpose — it is a namespace for one static helper, not a candidate for subclassing. Don't soften this to fit a pattern.
- **Iterating on the Mixin:** `defaultRequire: 1` means any miss (wrong descriptor, renamed target) hard-crashes the game at startup with a clear Mixin apply error. Treat that as a feature, not a bug — see README §"TFC+ signature changes".
