# MoBends BendsCapeRenderer 物理机制分析

**源码**: `MoBends-Reforged/src/main/java/net/gobbob/mobends/client/renderer/entity/BendsCapeRenderer.java`
**数据源**: `Data_Player.java`（capeWavePhase 更新逻辑）

---

## 1. 数据结构

披风 = **16 段铰接 slab**（SLAB_AMOUNT = 16），每段：
- 尺寸：宽 10 × 长 1（MODEL_WIDTH=10, MODEL_LENGTH=16, 每段长 16/16=1）× 深 1
- 每段 rotationPointY = 1（下一段铰接点在本段顶部）
- slab[0].rotationPointY = 0（第一段固定在披风根部）
- 段间 childSlab 链接，渲染时递归

---

## 2. 物理机制（applyAnimation）

```
phase = playerData.getCapeWavePhase()   // 随时间递增的相位
for i in 0..15:
    waveOffset = i / 16                 // 段深度 0..1
    magnitude = 80/16 * (0.7 + waveOffset)  // 越靠下幅度越大: 3.5 → 8.5
    wave = cos(phase * 0.2 + waveOffset * 7.2) * magnitude
    if wave > magnitude * 0.35: wave = magnitude * 0.35   // 钳制向内折叠
    slabs[i].setRotateAngle(wave)
slabs[0].rotate(-10)                    // 根部额外 -10° 外倾
```

**波形特性**：
- **waveSpeed = 0.2**：相位随时间缓慢推进
- **waveFrequency = 7.2**：相邻段相位差 7.2/16 ≈ 0.45 rad ≈ 26°，形成从顶部传播到底部的**涟漪**
- **magnitude 递增**（0.7+offset）：底部摆动幅度比顶部大 → 柔性布料感
- **0.35 钳制**：wave 超过 magnitude*0.35 时截断 → 防止布料向内折叠进身体

---

## 3. hingeOffset 技巧（关键！）

```java
setRotateAngle(a):
    rotateAngle = a
    hingeOffset = (a < 0) ? MODEL_DEPTH : 0   // 深度=1

render(scale):
    translate(0, rotationPointY*scale, (rotationPointZ + hingeOffset)*scale)  // 移到铰链
    rotate(rotateAngle, 1,0,0)                                                // 绕X轴转
    translate(0, 0, -hingeOffset*scale)                                       // 抵消
    draw slab
    child.render(scale)   // 递归渲染子段
```

**hingeOffset 的意义**：当段向身体方向转（负角）时，铰链从 z=0 移到 z=1（板的前缘），旋转轴贴着板的前表面——这样板绕**自己的前边缘**折叠，而不是绕中心轴转。**这是"布料折叠"而非"板绕轴转"的关键**：
- 正角（向后）：铰链在 z=0（后表面），板向后翻
- 负角（向内）：铰链在 z=1（前表面），板向内折叠

---

## 4. 相位推进（Data_Player.update）

```java
capeWavePhase += capeWaveSpeed * ticksPerFrame
if capeWavePhase > 380: capeWavePhase -= 380   // 循环回绕
```

- **capeWaveSpeed 默认 1.0**，飞行冲刺时设为 **4.0**（更快飘动）
- 380 是回绕阈值（约 60 个周期）

---

## 5. RenderBendsPlayer 如何驱动（上下文）

```java
// 在 renderEquippedItems 的 cape 分支：
bipedBody.postRender(scale)                       // 进入 body frame（含 lean）
translate(0, -12*scale, 2.2*scale)               // 移到背部
// 移动/速度计算 → f5/f6/f7 旋转
rotate(6 + f6/2 + f5, 1,0,0)                     // 整体倾斜（跑步、上下坡）
rotate(f7/2, 0,0,1)                               // 侧摆（转身）
rotate(-f7/2, 0,1,0)                              // 偏航补偿
rotate(180, 0,1,0)                                // 翻转到背部
capeRenderer.applyAnimation(capeData)             // 16段涟漪
capeRenderer.render(scale)
```

**两层物理**：
1. **整体旋转**（来自玩家速度/转身的 f5/f6/f7）——披风作为整体随身体运动倾斜
2. **内部涟漪**（16 段 cosine 波）——布料自身柔性摆动

---

## 6. 总结：MoBends 披风飘动 = 

| 层 | 来源 | 作用 |
|----|------|------|
| 根部固定 | slabs[0] 铰链在身体背部 | 披风挂在肩部 |
| 整体倾斜 | f5/f6/f7（速度+转身） | 跑步时披风后飘，转身时侧摆 |
| 内部涟漪 | 16段 cosine 波（相位差 26°） | 柔性布料从上到下传播的波浪 |
| 折叠铰链 | hingeOffset 技巧 | 负角时绕板前缘折叠（不穿模） |
| 速度调制 | capeWaveSpeed 1.0→4.0 | 飞行冲刺时飘动加快 |

**核心可复用点**：16段铰接 + 深度递增幅度 + 相位差涟漪 + hingeOffset 前缘折叠 + 380 回绕相位。
