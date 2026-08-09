# TFC+ Hat Rendering — Shape & Dispatch Reference

**Source:** `com.dunk.tfc.Render.Models.ModelHat` (TFC+ 0.89.1-dev jar)
**Caller:** `com.dunk.tfc.Render.RenderClothing.switchRender` → `case CLOTH_HAT/STRAW_HAT → hat.render(entity, item, renderer, partial, armor[3]!=null)`
**Model instance:** `new ModelHat(-0.1f)` (RenderClothing line 56) — the `-0.1f` is the `scaleFactor`/inflation passed to every `addBox`.

All hat items share `ModelHat`. The model carries every hat shape as child `ModelRenderer`s and toggles `showModel` per-item at render time. There is **no separate model class per hat** — one class, six dispatch branches.

## Geometry (ModelHat constructor, lines 53-89)

Frame: a `base` ModelRenderer acts as the parent; its rotation point is `(0, 4, 0)` (in head-local model units, i.e. 4 units below the head rotation point). `base` is empty (`addBox(0,0,0,0,0,0)`) — it exists only to carry the head's rotation and the `-0.2` pitch tilt.

| Part | Parent | addBox(x,y,z, w,h,d, inflate) | texOffset | Visible for |
|------|--------|-------------------------------|-----------|-------------|
| `base` | — | empty, rp `(0,4,0)` | — | always (invisible frame) |
| `hat` | base | `(-4,-9,-6, 8,4,10, f)` | (0,0) | cloth hat, straw hat |
| `hatBulge` | base | `(-4.5,-7,-6.5, 9,3,11, f-0.3)` | (0,14) | cloth hat (the rounded crown) |
| `strawBrim` | hat | `(-7,-6,-9, 14,1,16, f)` | (20,0) | straw hat (wide flat brim) |
| `animalHead` | base | `(-4,-10,-5, 8,4,6)` | (36,0) | fur hat (wolf/bear full head) |
| `animalSnout` | animalHead | `(-2,-8,-8, 4,3,4)` (+offsetZ tweak for bear) | (36,11) | fur hat |
| `animalEars` | animalHead | two boxes `(-3.5,-11.5,0, 2,3,1)` and `(1.5,-11.5,0, 2,3,1)` | (36,18) | fur hat |
| `animalFur` | base | `(-4.5,-9,-5.5, 9,6,6)` | (5,19) | fur hat (back fur) |
| `animalFur2` | animalFur | `(-4.5,-5.6,-6, 9,6,6, -0.1)` | (5,19) | fur hat (fur drape) |

`f` = the constructor's scaleFactor = **-0.1** for the live player model (RenderClothing instance). So `hat` is inflated -0.1 (slightly smaller), `hatBulge` inflated -0.4, `strawBrim` -0.1.

### altStrawHat (strawHat2) — custom quad cone

`strawHat2` does NOT use the ModelRenderer tree. When `item.getItem() == TFCItems.strawHat2`, ModelHat builds 14 `TexturedQuad`s from explicit `Vec3` vertices forming a **cone with a chin strap** (lines 485-651):
- Cone apex `PDir=(0,-11,-0.25)`, base corners at `(-7,-5,-7.5)/(7,-5,-7.5)/(-7,-5,7)/(7,-5,7)`.
- All direction vectors are rotated by `-bipedHead.rotateAngleX + 0.1`, `+bipedHead.rotateAngleY + bipedBody.rotateAngleY`, `+bipedHead.rotateAngleZ` — so the cone **swings with the head and body yaw**, not just the head.
- `headPosition = (0,4,0)` (sneak: -1.5y).
- 8 cone quads (tex region `10,0 - 19,10`) + 6 chin-strap quads (tex region `38,31 - 64,32`).
- Drawn directly via `quadList[i].draw(Tessellator.instance, 0.0625F)`; the ModelRenderer `base.render()` is **skipped** for altStrawHat.

## Dispatch (render() lines 152-221, by item identity)

The branch is selected by `item.getItem() == TFCItems.<x>`, NOT by ClothingType. ClothingType only routes `switchRender` into `ModelHat`; within ModelHat the item identity decides parts.

| Item (TFCItems) | ClothingType | Parts shown | Notes |
|-----------------|--------------|-------------|-------|
| `strawHat` | STRAW_HAT | `hat` + `strawBrim` | domed straw hat |
| `strawHat2` | STRAW_HAT | `strawBrim` + 14-quad cone (altStrawHat) | conical straw hat, no hat box |
| `bearFurHat` | CLOTH_HAT | `animalHead`+`snout`(offsetZ -1/32)+`ears`+`animalFur`+`animalFur2` | full bear head |
| `wolfFurHat` | CLOTH_HAT | `animalHead`+`snout`(offsetZ 0)+`ears`+`animalFur`+`animalFur2` | full wolf head |
| `woolHat` | CLOTH_HAT | `hat` + `hatBulge` | cloth cap |
| `cottonHat` | CLOTH_HAT | `hat` + `hatBulge` | cloth cap |
| `linenHat` | CLOTH_HAT | `hat` + `hatBulge` | cloth cap |
| `silkHat` | CLOTH_HAT | `hat` + `hatBulge` | cloth cap |
| `furHat` | CLOTH_HAT | `hat` + `hatBulge` | (generic fur, NOT the animal-head path) |
| `wolfFurGenericHat` | CLOTH_HAT | `hat` + `hatBulge` | (generic, NOT animal head) |
| `bearFurGenericHat` | CLOTH_HAT | `hat` + `hatBulge` | (generic, NOT animal head) |
| `leatherCoif` | CLOTH_HAT | `hat` + `hatBulge` | leather hood |
| *(any hat)* + helmet in armor[3] | — | **nothing** | `hasHelmet` short-circuits all parts off |

**Rule of thumb:** only `bearFurHat`/`wolfFurHat` (the head-form fur hats) and `strawHat`/`strawHat2` have special geometry. Every other CLOTH_HAT item renders as `hat + hatBulge`. A helmet in the head armor slot suppresses the hat entirely.

## Rotation & mounting (lines 417-422, 459-463)

```
base.rotateAngleX = bipedHead.rotateAngleX - 0.2   // -0.2 rad tilt forward
base.rotateAngleY = bipedHead.rotateAngleY
base.rotateAngleZ = bipedHead.rotateAngleZ
```
Plus, before rendering, a GL `glTranslatef(0, -1/16, 0)` (non-sneak) / `glTranslatef(0, 1/16, 0)` (sneak) is applied, then the `renderYawOffset` Y rotation. So the hat:
- inherits head yaw/pitch/roll (so it turns with the head),
- is tilted an extra **0.2 rad ≈ 11.5° forward** (the "worn" tilt),
- sits 4 units below the head rotation point (the `base` rp) and is nudged -1 unit by the GL translate.

`animalHead.rotateAngleX = 0.2`, `animalFur.rotateAngleX = -0.4`, `animalFur2.rotateAngleX = -0.9` — the fur hat parts have their own fixed pitches (the snout points slightly down, the fur drapes back).

## Texture layout (64×32)

| Region | Used by |
|--------|---------|
| `(0,0)-(8,10)` etc. | `hat` box faces |
| `(0,14)-(9,3+14)` | `hatBulge` box faces |
| `(20,0)-(14×16)` | `strawBrim` box faces |
| `(36,0)` region | `animalHead`/`snout`/`ears` |
| `(5,19)` region | `animalFur`/`animalFur2` |
| `(10,0)-(19,10)` | altStrawHat cone quads |
| `(38,31)-(64,32)` | altStrawHat chin strap |

Texture is `ie.getClothingTexture(entity, item, textureVariant)` — same path the compat mod already binds (e.g. `textures/models/armor/clothing/wool_hat_color.png`). The compat mod's current single `clothingHead.addBox(-4,-9,-6, 8,4,10, 0.5)` only reproduces the bare `hat` part with wrong inflation (0.5 vs -0.1) and omits `hatBulge`, `strawBrim`, and the entire animal/straw2 dispatch.

## Compat-mod gaps (what needs porting)

1. **Inflation**: compat uses `scaleFactor=0.5`; TFC+ uses `-0.1` for `hat` (→ `hatBulge` `-0.4`, `strawBrim` `-0.1`). The hat renders visibly larger/rounder than TFC+.
2. **Missing `hatBulge`**: the rounded crown on top of cloth hats is absent → cloth hats look like a bare band, not a cap.
3. **Missing `strawBrim`**: straw hats render as just the `hat` dome with no brim.
4. **No straw2 / fur dispatch**: `strawHat2` (cone), `bearFurHat`/`wolfFurHat` (animal head) all fall into the cloth-hat branch.
5. **Missing `-0.2` pitch tilt**: the hat doesn't tilt forward; it sits level.
6. **Missing `hasHelmet` suppression**: if the player wears a vanilla helmet, TFC+ hides the hat; the compat mod always draws it.

---

## OPEN ISSUES (2026-08-09 status — 未解决问题)

> 已修复的旧 gaps（inflation 已改为 -0.1/-0.4、hatBulge/strawBrim/-0.2 倾斜已实现、STRAW_HAT2 已添加）
> 以下是**当前仍未解决**的问题，按用户反馈逐条记录。

### 1. 帽子位置（strawHat2 圆锥挂点）— 未定稿

**现象**：`strawHat2` 圆锥草帽渲染位置偏低，卡在头中间。

**调试记录**（`ModelBipedClothingAdapter.configureHat()` 中 `strawHat2Cone.setRotationPoint(0, Y, 0)`）：

| Y 值 | 结果 |
|------|------|
| `4`（初版，对齐 TFC+ `headPosition=(0,4,0)`）| 偏低，卡在头中间 |
| `10`（以为要上移）| **更低**（方向反了——`rotationPointY` 在此挂载链中正 Y 向下） |
| `2`（减小正值）| 待用户测试 |

**结论**：`rotationPointY` 在 `clothingHead → strawHat2Cone` 挂载链中方向反向（正 Y 偏下，与 ModelRenderer 常规相反），最终值待验证。TFC+ 原版用 `headPosition=(0,4,0)` 但那是**直接画 quad**（无 rotationPoint 变换）；我们挂在 ModelRenderer 树上，坐标语义不同，不能直接照抄 TFC+ 的 4。

### 2. 帽子大小偏小 — 未验证

**现象**：兼容 mod 帽子比 TFC+ 渲染的偏小。

**已排除**：TFC+ 对玩家**没有**额外 `glScalef` 放大——`RenderClothing` 的 `glScalef(1.1/1.2)` 只在 `EntityMob`（怪物）时生效；`ModelHat` 内部 glScalef 全被注释。纹理尺寸一致（64×32，colortype 6）。

**待验证假设**：TFC+ 的 `base.setRotationPoint(0, 4, 0)` 使帽子**下沉 4 单位**、帽体包住头部中段（y -5..-1），视觉上更饱满；兼容 mod 的 `hatBase.setRotationPoint(0, 0, 0)` 让帽子顶在头上（y -9..-5），看起来更小。用户此前明确"高度不需要调整"——但若"偏小"确因位置差异，需要用户确认是否接受 TFC+ 式下沉（改变现有高度）。

### 3. 纹理绑定 — 未验证一致性

**现状**：兼容 mod 与 TFC+ 都走 `ie.getClothingTexture(entity, item, textureVariant)`，绑定同一纹理路径。已确认各部件 texOffset 与 TFC+ 相同（hat (0,0)、hatBulge (0,14)、strawBrim (20,0)、cone (10,0)-(19,10)、chin strap (38,31)-(64,32)）。

**未验证项**：
- `strawHat` 与 `strawHat2` 共用同一纹理文件 `straw_hat_color.png`，但纹理区不同（strawHat2 用 cone 区）。需实际确认 strawHat2 显示正确纹理。
- 各帽子的 `wool_hat_color.png` / `cotton_hat_color.png` 等纹理文件中，hatBulge 区 (0,14) 是否真的有圆顶图案（TFC+ 纹理布局为 colortype 6，未逐区核对）。
- 帽子**颜色染色**（`ItemClothing.getColor` → `glColor3f`）：TFC+ 在渲染前按物品颜色乘色，兼容 mod 未复刻——羊毛/棉布帽有染色变体时颜色可能不对。

### 4. 帽子种类未实现 — 兽首帽 + 头盔抑制

**已实现**：CLOTH_HAT（hat+hatBulge）、STRAW_HAT（hat+strawBrim）、STRAW_HAT2（圆锥+下巴带，位置未定稿）。

**未实现**：
- **兽首帽** `bearFurHat` / `wolfFurHat`（TFCItems）：需 `animalHead`+`animalSnout`+`animalEars`+`animalFur`+`animalFur2` 部件，纹理区 (36,0)/(36,11)/(36,18)/(5,19)。bear 的 snout 需 `offsetZ = -1/32`。
- **`hasHelmet` 抑制**：`switchRender` 传入 `armor[3] != null`，戴头盔时 TFC+ 完全不渲染帽子；兼容 mod 的 `onSpecialsPost` 循环没有该检查。
- 兽首帽的部件旋转：`animalHead.rotateAngleX=0.2`、`animalFur=-0.4`、`animalFur2=-0.9`（固定俯仰）。

