# MoBends TFCP Compat

[English](README.md) | [中文](README_zh.md)

A client-side compatibility mod for Minecraft 1.7.10 that makes **TerraFirmaCraft Plus (TFC+)** clothing — shirts, pants, shorts, socks, boots, sandals, hats (cloth / straw / conical straw / fur), coats, robes, skirts and cloaks — follow the animated player body when **Mo'Bends** is installed. Without it, clothing stays in the vanilla pose while the body bends (walk / run / fall / fly / sneak), so garments visibly detach from the player.

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614+ |
| Mo'Bends (Reforged) | 1.1.0+ |
| TFC+ | 0.89.1 |
| UniMixins | any modern build (ships with GTNH, Angelica, RPL, etc.) |

## Install

Drop `mobends_tfcp_compat-<version>.jar` into your `mods/` folder alongside Mo'Bends and TFC+. That's it.

## Building from source

```bash
# Requires JDK 25 (pinned by gradle-daemon-jvm.properties — RFG daemon toolchain)
./gradlew setupDecompWorkspace
./gradlew build
# Output: build/libs/mobends_tfcp_compat-<version>.jar (Java 8 bytecode)
```

Mo'Bends and TFC+ are pulled from [CurseForge via CurseMaven](https://www.cursemaven.com/) at build time (no auth needed), so there's nothing to set up — just run the build. If you want to target newer versions of either mod, update the CurseMaven coordinates in `dependencies.gradle` to the new CurseForge file IDs.

## License

MIT. See [LICENSE](LICENSE).
