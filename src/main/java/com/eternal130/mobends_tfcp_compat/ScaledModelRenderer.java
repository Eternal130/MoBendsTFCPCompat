package com.eternal130.mobends_tfcp_compat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

/**
 * ModelRenderer with a per-axis scale applied around the rotation point,
 * mirroring {@code ModelRendererBends.setScale}. Vanilla ModelRenderer has no
 * scale field; hat parts need one so all hat geometry can be shrunk by the same
 * uniform factor the TFC+ hat model applies.
 *
 * <p>The display list is rebuilt from {@link #cubeList} because vanilla
 * {@code compiled/displayList/compileDisplayList} are private - a subclass
 * cannot re-render the boxes with a scale.
 */
public class ScaledModelRenderer extends ModelRenderer {

    private float scaleX = 1.0F;
    private float scaleY = 1.0F;
    private float scaleZ = 1.0F;
    private int displayList;
    private boolean compiled;

    public ScaledModelRenderer(ModelBase base, int texX, int texY) {
        super(base, texX, texY);
        this.setTextureOffset(texX, texY);
    }

    public ScaledModelRenderer setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        return this;
    }

    @Override
    public void render(float scale) {
        if (!this.isHidden && this.showModel) {
            if (!compiled) {
                compileDisplayList(scale);
            }

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

            GL11.glCallList(this.displayList);

            if (this.childModels != null) {
                for (int i = 0; i < this.childModels.size(); ++i) {
                    ((ModelRenderer) this.childModels.get(i)).render(scale);
                }
            }

            GL11.glPopMatrix();
        }
    }

    private void compileDisplayList(float scale) {
        displayList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        Tessellator tessellator = Tessellator.instance;
        for (int i = 0; i < this.cubeList.size(); ++i) {
            ((ModelBox) this.cubeList.get(i)).render(tessellator, scale);
        }
        GL11.glEndList();
        compiled = true;
    }
}
