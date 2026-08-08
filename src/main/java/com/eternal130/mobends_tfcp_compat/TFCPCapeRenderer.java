package com.eternal130.mobends_tfcp_compat;

import net.gobbob.mobends.data.Data_Player;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * TFC+-shaped cloak animated with MoBends' cape cloth physics.
 *
 * <p><b>Shape (from TFC+ ModelCloak):</b>
 * <ul>
 *   <li>Horizontal shoulder plate: fixed, half-width 7 (14 total), depth 3, thickness 0.5.
 *       Parallel to the ground, covers the shoulders.</li>
 *   <li>Hanging cloth: total length 20 (TFC+ splits into 6+6+8), half-width 8 (16 total).
 *       The first segment is a trapezoid (top half-width 7 → bottom half-width 8). The whole
 *       hanging part is simulated as ONE cloth.</li>
 * </ul>
 *
 * <p><b>Physics (from MoBends BendsCapeRenderer):</b> the hanging cloth is split into
 * {@link #SLAB_COUNT} hinged slabs. Each slab rotates around its X hinge by a phase-delayed
 * cosine sampled from {@link Data_Player#getCapeWavePhase()} — magnitude grows with depth
 * (0.7+offset) so the bottom swings more (cloth feel), phase offset 7.2 gives a top-to-bottom
 * ripple, and waves are clamped to 0.35*magnitude so the cloth can't fold into the torso.
 * {@code hingeOffset} (rotate around the slab's front edge when the angle is negative) is
 * preserved so the cloth folds like fabric, not like a rigid board.
 *
 * <p>The trapezoid is kept: each slab's half-width starts at 7 and grows to 8 over the first
 * 6 units (the TFC+ trapezoid segment), staying 8 below that.
 */
public class TFCPCapeRenderer {

    private static final float TEX_W = 64F;
    private static final float TEX_H = 32F;

    private static final float SHOULDER_HALF_W = 7F;
    private static final float SHOULDER_THICK = 0.5F;
    private static final float SHOULDER_DEPTH = 3F;

    private static final int SLAB_COUNT = 20;
    private static final float HANG_LENGTH = 20F;
    private static final float SLAB_LEN = HANG_LENGTH / SLAB_COUNT;
    private static final float HANG_MAX_HALF_W = 8F;
    private static final float HANG_DEPTH = 0.5F;
    private static final float TRAPEZOID_LENGTH = 6F;
    private final Segment shoulder;
    private final Segment[] slabs;

    public TFCPCapeRenderer() {
        shoulder = new Segment(SHOULDER_HALF_W, SHOULDER_HALF_W, SHOULDER_THICK, SHOULDER_DEPTH, 0F, -SHOULDER_DEPTH, 16, 4, true);
        slabs = new Segment[SLAB_COUNT];
        for (int i = 0; i < SLAB_COUNT; i++) {
            float slabTopY = i * SLAB_LEN;
            float slabBotY = (i + 1) * SLAB_LEN;
            float topHalfW = halfWidthAt(slabTopY);
            float botHalfW = halfWidthAt(slabBotY);
            float hingeY = (i == 0) ? SHOULDER_THICK : SLAB_LEN;
            slabs[i] = new Segment(topHalfW, botHalfW, SLAB_LEN, HANG_DEPTH, hingeY, 0F, 16, 4 + (int) (i * SLAB_LEN), false);
            if (i > 0) {
                slabs[i - 1].setChild(slabs[i]);
            }
        }
        shoulder.setChild(slabs[0]);
    }

    private static float halfWidthAt(float y) {
        if (y < TRAPEZOID_LENGTH) {
            float t = y / TRAPEZOID_LENGTH;
            return SHOULDER_HALF_W + (HANG_MAX_HALF_W - SHOULDER_HALF_W) * t;
        }
        return HANG_MAX_HALF_W;
    }

    public void applyAnimation(Data_Player playerData) {
        double phase = (double) playerData.getCapeWavePhase();
        for (int i = 0; i < SLAB_COUNT; i++) {
            float waveSpeed = 0.2F;
            float waveFrequency = 7.2F;
            float waveOffset = (float) i / (float) SLAB_COUNT;
            float magnitude = 80.0F / (float) SLAB_COUNT * (0.7F + waveOffset);
            float wave = (float) (Math.cos(phase * (double) waveSpeed + (double) (waveOffset * waveFrequency)) * (double) magnitude);
            if (wave > magnitude * 0.35F) {
                wave = magnitude * 0.35F;
            }
            slabs[i].setRotateAngle(wave);
        }
        slabs[0].rotate(-10.0F);
    }

    public void render(float scale) {
        shoulder.render(scale);
    }

    private static final class Segment {
        final boolean fixed;
        final float length;
        final float depth;
        final float hingeY;
        final float hingeZ;
        float rotateAngle;
        float hingeOffset;
        Segment child;
        private final TexturedQuad[] quadList;
        private int displayList;
        private boolean compiled;

        Segment(float halfWidthTop, float halfWidthBottom, float length, float depth, float hingeY, float hingeZ,
                int tu, int tv, boolean fixed) {
            this.length = length;
            this.depth = depth;
            this.hingeY = hingeY;
            this.hingeZ = hingeZ;
            this.fixed = fixed;
            this.rotateAngle = 0F;
            this.hingeOffset = 0F;

            float x1t = -halfWidthTop;
            float x2t = halfWidthTop;
            float x1b = -halfWidthBottom;
            float x2b = halfWidthBottom;
            float y1 = 0F;
            float y2 = length;
            float z1 = 0F;
            float z2 = depth;

            PositionTextureVertex v0 = new PositionTextureVertex(x1t, y1, z1, 0F, 0F);
            PositionTextureVertex v1 = new PositionTextureVertex(x2t, y1, z1, 8F, 0F);
            PositionTextureVertex v2 = new PositionTextureVertex(x2b, y2, z1, 8F, 8F);
            PositionTextureVertex v3 = new PositionTextureVertex(x1b, y2, z1, 0F, 8F);
            PositionTextureVertex v4 = new PositionTextureVertex(x1t, y1, z2, 0F, 0F);
            PositionTextureVertex v5 = new PositionTextureVertex(x2t, y1, z2, 8F, 0F);
            PositionTextureVertex v6 = new PositionTextureVertex(x2b, y2, z2, 8F, 8F);
            PositionTextureVertex v7 = new PositionTextureVertex(x1b, y2, z2, 0F, 8F);

            int tV = tv;
            int tL = (int) length;
            quadList = new TexturedQuad[6];
            quadList[0] = new TexturedQuad(new PositionTextureVertex[]{v1, v2, v3, v0}, tu, tV, tu + 16, tV + tL, TEX_W, TEX_H);
            quadList[1] = new TexturedQuad(new PositionTextureVertex[]{v5, v4, v7, v6}, tu, tV, tu + 16, tV + tL, TEX_W, TEX_H);
            quadList[2] = new TexturedQuad(new PositionTextureVertex[]{v4, v5, v1, v0}, tu, tV, tu + 16, tV + 1, TEX_W, TEX_H);
            quadList[3] = new TexturedQuad(new PositionTextureVertex[]{v3, v2, v6, v7}, tu, tV, tu + 16, tV + 1, TEX_W, TEX_H);
            quadList[4] = new TexturedQuad(new PositionTextureVertex[]{v0, v3, v7, v4}, tu, tV, tu + 16, tV + 1, TEX_W, TEX_H);
            quadList[5] = new TexturedQuad(new PositionTextureVertex[]{v5, v6, v2, v1}, tu, tV, tu + 16, tV + 1, TEX_W, TEX_H);
        }

        void setChild(Segment c) { this.child = c; }

        void setRotateAngle(float a) {
            this.rotateAngle = a;
            this.hingeOffset = (a < 0.0F) ? depth : 0F;
        }

        void rotate(float d) { this.rotateAngle += d; }

        void render(float scale) {
            if (!compiled) compile(scale);
            GL11.glPushMatrix();
            GL11.glTranslatef(0F, hingeY * scale, (hingeZ + hingeOffset) * scale);
            if (!fixed) {
                GL11.glRotatef(rotateAngle, 1F, 0F, 0F);
            }
            GL11.glTranslatef(0F, 0F, (-hingeOffset) * scale);
            GL11.glCallList(displayList);
            if (child != null) child.render(scale);
            GL11.glPopMatrix();
        }

        private void compile(float scale) {
            displayList = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            Tessellator tessellator = Tessellator.instance;
            for (int i = 0; i < quadList.length; i++) {
                quadList[i].draw(tessellator, scale);
            }
            GL11.glEndList();
            compiled = true;
        }
    }
}
