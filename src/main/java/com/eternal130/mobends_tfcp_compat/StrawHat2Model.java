package com.eternal130.mobends_tfcp_compat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * The conical straw hat (TFC+ {@code strawHat2}) as a ModelRenderer, so it rides the
 * adapter's head hierarchy (clothingHead rotation) like the other hat parts.
 *
 * <p>Shape mirrors TFC+ {@code ModelHat} altStrawHat branch: a 4-sided cone (base
 * 14×14.5, height 6) plus a chin strap. TFC+ draws these as raw TexturedQuads with the
 * head/body yaw baked into the vertices and skips the ModelRenderer tree entirely
 * (its {@code strawBrim} child never actually renders because its parent {@code hat}
 * has {@code showModel=false}). This port keeps the same geometry but lets the parent
 * chain carry the rotation, so it follows the animated head smoothly.
 *
 * <p>Texture regions (64×32): cone faces {@code (10,0)-(19,10)}, chin strap
 * {@code (38,31)-(64,32)} — same as TFC+.
 */
public class StrawHat2Model extends ModelRenderer {

    private static final float TEX_W = 64F;
    private static final float TEX_H = 32F;

    private final TexturedQuad[] quadList;
    private int displayList;
    private boolean compiled;
    private float scaleX = 1.0F;
    private float scaleY = 1.0F;
    private float scaleZ = 1.0F;

    public StrawHat2Model(ModelBase base) {
        super(base, 0, 0);

        PositionTextureVertex tl = v(-7F, -5F, -7.5F);
        PositionTextureVertex tr = v(7F, -5F, -7.5F);
        PositionTextureVertex bl = v(-7F, -5F, 7F);
        PositionTextureVertex br = v(7F, -5F, 7F);
        PositionTextureVertex apex = v(0F, -11F, -0.25F);

        PositionTextureVertex chinLF = v(-4F, 0.35F, 0F);
        PositionTextureVertex chinRF = v(4F, 0.35F, 0F);
        PositionTextureVertex chinLB = v(-4F, 0.35F, -1F);
        PositionTextureVertex chinRB = v(4F, 0.35F, -1F);
        PositionTextureVertex strapLF = v(-6F, -5.5F, 0F);
        PositionTextureVertex strapRF = v(6F, -5.5F, 0F);
        PositionTextureVertex strapLB = v(-6F, -5.5F, -1F);
        PositionTextureVertex strapRB = v(6F, -5.5F, -1F);

        quadList = new TexturedQuad[6];
        quadList[0] = coneQuad(br, bl, apex);            // back
        quadList[1] = coneQuad(bl, tl, apex);            // left
        quadList[2] = coneQuad(tl, tr, apex);            // front
        quadList[3] = coneQuad(tr, br, apex);            // right
        quadList[4] = strapQuad(strapLB, strapLF, chinLF, chinLB);
        quadList[5] = strapQuad(strapRB, strapRF, chinRF, chinRB);
    }

    private static PositionTextureVertex v(float x, float y, float z) {
        return new PositionTextureVertex(x, y, z, 0F, 0F);
    }

    private static TexturedQuad coneQuad(PositionTextureVertex a, PositionTextureVertex b, PositionTextureVertex apex) {
        return quad(a, b, apex, apex, 10F, 0F, 19F, 10F);
    }

    private static TexturedQuad strapQuad(PositionTextureVertex a, PositionTextureVertex b,
            PositionTextureVertex c, PositionTextureVertex d) {
        return quad(a, b, c, d, 38F, 31F, 64F, 32F);
    }

    private static TexturedQuad quad(PositionTextureVertex a, PositionTextureVertex b,
            PositionTextureVertex c, PositionTextureVertex d,
            float u0, float v0, float u1, float v1) {
        return new TexturedQuad(new PositionTextureVertex[]{
            a.setTexturePosition(u0 / TEX_W, v0 / TEX_H),
            b.setTexturePosition(u1 / TEX_W, v0 / TEX_H),
            c.setTexturePosition(u1 / TEX_W, v1 / TEX_H),
            d.setTexturePosition(u0 / TEX_W, v1 / TEX_H)});
    }

    public StrawHat2Model setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        return this;
    }

    @Override
    public void render(float scale) {
        if (!this.showModel || this.isHidden) return;
        if (!compiled) compile(scale);
        GL11.glPushMatrix();
        GL11.glTranslatef(this.offsetX, this.offsetY, this.offsetZ);
        if (this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
            if (this.rotationPointX != 0.0F || this.rotationPointY != 0.0F || this.rotationPointZ != 0.0F) {
                GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
            }
        } else {
            GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
            if (this.rotateAngleZ != 0.0F) {
                GL11.glRotatef(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
            }
            if (this.rotateAngleY != 0.0F) {
                GL11.glRotatef(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
            }
            if (this.rotateAngleX != 0.0F) {
                GL11.glRotatef(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
            }
        }
        if (this.scaleX != 1.0F || this.scaleY != 1.0F || this.scaleZ != 1.0F) {
            GL11.glScalef(this.scaleX, this.scaleY, this.scaleZ);
        }
        GL11.glCallList(displayList);
        if (this.childModels != null) {
            for (Object o : this.childModels) {
                ((ModelRenderer) o).render(scale);
            }
        }
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
