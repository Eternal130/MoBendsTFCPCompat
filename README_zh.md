# MoBends TFCP Compat（Mo'Bends × TFC+ 兼容 mod）

[English](README.md) | [中文](README_zh.md)

一个 Minecraft 1.7.10 客户端兼容 mod，让 **TerraFirmaCraft Plus (TFC+)** 的衣物 —— 衬衫、裤子、短裤、袜子、靴子、凉鞋、帽子（布帽 / 草帽 / 圆锥草帽 / 兽首皮帽）、外套、长袍、裙子和披风 —— 在安装了 **Mo'Bends** 时跟随动画化的玩家身体一起弯曲。没有它，衣物会停留在原版姿态，而身体在走 / 跑 / 摔 / 飞 / 潜行时弯曲，导致衣物明显脱离玩家。

## 依赖

| 组件 | 版本 |
|------|------|
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614+ |
| Mo'Bends (Reforged) | 1.1.0+ |
| TFC+ | 0.89.1 |
| UniMixins | 任何现代版本（随 GTNH、Angelica、RPL 等发布） |

## 安装

把 `mobends_tfcp_compat-<version>.jar` 丢进 `mods/` 文件夹，和 Mo'Bends、TFC+ 放一起即可。

## 从源码构建

```bash
# 需要 JDK 25（由 gradle-daemon-jvm.properties 锁定 —— RFG daemon 工具链）
./gradlew setupDecompWorkspace
./gradlew build
# 产物：build/libs/mobends_tfcp_compat-<version>.jar（Java 8 字节码）
```

`libs/` 目录存放 Mo'Bends 和 TFC+ 的开发（deobf）jar 用于编译。若要适配更新版本的目标 mod，请替换这里的 jar。

## 许可证

MIT，详见 [LICENSE](LICENSE)。
