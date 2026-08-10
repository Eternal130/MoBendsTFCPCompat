package com.eternal130.mobends_tfcp_compat;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;

/**
 * Dynamic coat/robe skirt: 12 TexturedQuads forming the flared hem panels
 * below the waist (outer side panels + open/closed front panels + back
 * panels), ported 1:1 from TFC+ {@code ModelCoat.render}.
 *
 * <p>The skirt vertices are recomputed every frame from the live leg
 * rotations ({@code sourceModel.bipedXxxLeg.rotateAngle*}) so the hem
 * stretches with the stride and never clips through the legs — same
 * mechanism as TFC+. {@code length} is 7 for coats, 10 for robes.
 *
 * <p><b>Frame conversion:</b> TFC+ computes the vertex positions in its own
 * flipped render frame. The full linear chain there — {@code doRender}'s
 * S(-1,-1,1).R(180,Z).R(180,X) plus the 180-Y flip around the quad draws —
 * collapses to R(180,Z) = diag(-1,-1,1) (x and y negated, z kept). The compat
 * mod draws at {@code RenderPlayerEvent.Specials.Post} in the MoBends body
 * frame, which has the same S(-1,-1,1) orientation as the vanilla model frame
 * (so x and y map 1:1). TFC+'s "front" verts carry positive z (front is -z in
 * the render frame), so z must be negated to land the front panels in front.
 * TFC+'s raw x is mirrored vs. the model parts (its "right"-leg panels use
 * bipedRightLeg.rotationPointX + 6 = +4.7, but the right leg sits at model x
 * -1.9), so x is negated to land on the correct side. Net conversion applied
 * at vertex build: p_conv = (-x, y, -z) — verified against TFC+ in-game.
 */
public class CoatSkirtModel {

    private final float length;
    private final float scaleFactor;
    private final float flare;
    private final boolean open;

    /**
     * Extra forward offset on the hem's front panels, in TFC+ frame coords
     * (+z = front there; {@link #v(float, float, float)} negates z, so these
     * render at z -(2.6..2.85) - 0.35 = -2.95..-3.2 in the render frame, where
     * the player's front is -z). The armor leggings (ModelBiped 0.75, drawn in
     * the armor pass before clothing) have their front face at z -2.75, so
     * without this the hem front (-2.6..-2.85) z-fights the leggings (护腿和
     * 下摆重合); +0.35 puts the hem 0.2..0.45 in front of them (下摆包围护腿).
     */
    private static final float FRONT_CLEARANCE = 0.35F;

    public CoatSkirtModel(float length, float scaleFactor, float flare, boolean open) {
        this.length = length;
        this.scaleFactor = scaleFactor;
        this.flare = flare;
        this.open = open;
    }

    public void render(EntityLivingBase entity, ModelBiped source, float renderScale) {
        if (source == null) return;

        Vec3 rightLegVector = Vec3.createVectorHelper(0F, length, 0F);
        Vec3 rightLegVector2 = Vec3.createVectorHelper(0F, length, 0F);
        Vec3 leftLegVector = Vec3.createVectorHelper(0F, length, 0F);
        Vec3 leftLegVector2 = Vec3.createVectorHelper(0F, length, 0F);

        float buttZdisplacement;
        float crotchZdisplacement;
        if (entity.isSneaking()) {
            buttZdisplacement = -0.4F;
            crotchZdisplacement = 0.4F;
        } else {
            buttZdisplacement = crotchZdisplacement = 0F;
        }

        rightLegVector.rotateAroundZ(source.bipedRightLeg.rotateAngleZ + flare);
        rightLegVector.rotateAroundY(source.bipedRightLeg.rotateAngleY);
        rightLegVector2.rotateAroundZ(source.bipedRightLeg.rotateAngleZ + flare);
        rightLegVector2.rotateAroundY(source.bipedRightLeg.rotateAngleY);
        leftLegVector.rotateAroundZ(source.bipedLeftLeg.rotateAngleZ - flare);
        leftLegVector.rotateAroundY(source.bipedLeftLeg.rotateAngleY);
        leftLegVector2.rotateAroundZ(source.bipedLeftLeg.rotateAngleZ - flare);
        leftLegVector2.rotateAroundY(source.bipedLeftLeg.rotateAngleY);

        if (entity.isSneaking()) {
            rightLegVector2.rotateAroundX(0.4F);
            leftLegVector2.rotateAroundX(0.4F);
        } else {
            rightLegVector.rotateAroundX(-0.1F);
            leftLegVector.rotateAroundX(-0.1F);
        }

        if (entity.ridingEntity != null) {
            rightLegVector.rotateAroundX(-0.4F);
            leftLegVector.rotateAroundX(-0.4F);
        }

        if (source.bipedRightLeg.rotateAngleX < 0) {
            rightLegVector.rotateAroundX(Math.min(source.bipedRightLeg.rotateAngleX, -flare));
            rightLegVector2.rotateAroundX(Math.max(source.bipedRightLeg.rotateAngleX * 0.35F, flare));
            if (entity.isSneaking()) {
                rightLegVector.rotateAroundX(-0.1F);
                leftLegVector.rotateAroundX(-0.1F);
            }
        } else if (source.bipedRightLeg.rotateAngleX > 0) {
            rightLegVector2.rotateAroundX(Math.max(source.bipedRightLeg.rotateAngleX, flare));
            rightLegVector.rotateAroundX(Math.min(source.bipedRightLeg.rotateAngleX * 0.35F, -flare));
            if (entity.isSneaking()) {
                rightLegVector2.rotateAroundX(-0.3F);
                leftLegVector2.rotateAroundX(-0.3F);
            }
        }

        if (source.bipedLeftLeg.rotateAngleX > 0) {
            leftLegVector2.rotateAroundX(Math.max(source.bipedLeftLeg.rotateAngleX, flare));
            leftLegVector.rotateAroundX(Math.min(source.bipedLeftLeg.rotateAngleX * 0.35F, -flare));
            if (entity.isSneaking()) {
                leftLegVector2.rotateAroundX(-0.3F);
                rightLegVector2.rotateAroundX(-0.3F);
            }
        } else if (source.bipedLeftLeg.rotateAngleX < 0) {
            leftLegVector.rotateAroundX(Math.min(source.bipedLeftLeg.rotateAngleX, -flare));
            leftLegVector2.rotateAroundX(Math.max(source.bipedLeftLeg.rotateAngleX * 0.35F, flare));
            if (entity.isSneaking()) {
                leftLegVector.rotateAroundX(-0.1F);
                rightLegVector.rotateAroundX(-0.1F);
            }
        }

        float velocity = (float) Math.sqrt(
            Math.pow(entity.lastTickPosX - entity.posX, 2)
            + Math.pow(((entity.lastTickPosY - entity.posY) + Math.abs(entity.lastTickPosY - entity.posY)) / 2F, 2)
            + Math.pow(entity.lastTickPosZ - entity.posZ, 2));
        rightLegVector2.rotateAroundX(Math.min(0.4F, velocity * 0.5F));
        leftLegVector2.rotateAroundX(Math.min(0.4F, velocity * 0.5F));

        Vec3 rightLegOuterVector;
        Vec3 leftLegOuterVector;
        if (!this.open) {
            rightLegOuterVector = Vec3.createVectorHelper(rightLegVector.xCoord, rightLegVector.yCoord, rightLegVector.zCoord);
            leftLegOuterVector = Vec3.createVectorHelper(leftLegVector.xCoord, leftLegVector.yCoord, leftLegVector.zCoord);
        } else {
            rightLegVector2.rotateAroundZ(Math.min(1F + (float) Math.sin((entity.worldObj.getTotalWorldTime() % 20) * Math.PI / 20d) * 0.1F, velocity * 0.75F));
            rightLegVector.rotateAroundZ(Math.min(1F + (float) Math.sin(((entity.worldObj.getTotalWorldTime() + 5) % 20) * Math.PI / 20d) * 0.1F, velocity * 1.5F));
            leftLegVector.rotateAroundZ(Math.max(-1F - (float) Math.sin(((entity.worldObj.getTotalWorldTime() + 10) % 20) * Math.PI / 20d) * 0.1F, -velocity * 1.5F));
            leftLegVector2.rotateAroundZ(Math.max(-1F - (float) Math.sin(((entity.worldObj.getTotalWorldTime() + 15) % 20) * Math.PI / 20d) * 0.1F, -velocity * 0.75F));
            rightLegOuterVector = Vec3.createVectorHelper(rightLegVector.xCoord, rightLegVector.yCoord, rightLegVector.zCoord);
            leftLegOuterVector = Vec3.createVectorHelper(leftLegVector.xCoord, leftLegVector.yCoord, leftLegVector.zCoord);
            rightLegVector.rotateAroundX(Math.max(-1, -0.7F * velocity));
            leftLegVector.rotateAroundX(Math.max(-1, -0.7F * velocity));
        }

        float sneakLegDepth = entity.isSneaking() ? -0.8F : 0F;
        float sneakButtHeight = entity.isSneaking() ? -0.2F : 0F;

        // === Right leg vertices ===
        float hipX = source.bipedRightLeg.rotationPointX + 6 + scaleFactor;
        float hipY = source.bipedRightLeg.rotationPointY;
        float hipZ = source.bipedRightLeg.rotationPointZ + 2 + scaleFactor;
        if (entity.isSneaking()) {
            hipY += 2F;
            hipZ += -8F;
        }

        PositionTextureVertex rightFrontOuterLeg = v(0.25F + hipX + (float) rightLegOuterVector.xCoord,
            hipY + (float) rightLegOuterVector.yCoord,
            0.25F + hipZ + crotchZdisplacement + (float) rightLegOuterVector.zCoord + FRONT_CLEARANCE);
        PositionTextureVertex rightFrontInnerLeg = v(hipX - (4F + scaleFactor) + (float) rightLegVector.xCoord,
            hipY + (float) rightLegVector.yCoord,
            hipZ + crotchZdisplacement + (float) rightLegVector.zCoord + FRONT_CLEARANCE);
        PositionTextureVertex rightBackOuterLeg = v(0.25F + hipX + (float) rightLegVector2.xCoord,
            sneakButtHeight + hipY + (float) rightLegVector2.yCoord,
            sneakLegDepth - 0.25F + buttZdisplacement + hipZ - (4F + scaleFactor * 2) + (float) rightLegVector2.zCoord);
        PositionTextureVertex rightBackInnerLeg = v(hipX - (4F + scaleFactor) + (float) rightLegVector2.xCoord,
            sneakButtHeight + hipY + (float) rightLegVector2.yCoord,
            sneakLegDepth - 0.25F + buttZdisplacement + hipZ - (4F + scaleFactor * 2) + (float) rightLegVector2.zCoord);
        PositionTextureVertex rightFrontOuterHip = v(hipX, hipY, hipZ + crotchZdisplacement + FRONT_CLEARANCE);
        PositionTextureVertex rightFrontInnerHip = v(hipX - (4F + scaleFactor), hipY, hipZ + crotchZdisplacement + FRONT_CLEARANCE);
        PositionTextureVertex rightBackOuterHip = v(hipX, sneakButtHeight + hipY,
            sneakLegDepth + hipZ + buttZdisplacement - (4F + 0.25F + scaleFactor * 2));
        PositionTextureVertex rightBackInnerHip = v(hipX - (4F + scaleFactor), sneakButtHeight + hipY,
            sneakLegDepth + hipZ + buttZdisplacement - (4F + 0.25F + scaleFactor * 2));

        // === Left leg vertices ===
        hipX = source.bipedLeftLeg.rotationPointX - 7 + scaleFactor;
        hipY = source.bipedLeftLeg.rotationPointY;
        hipZ = source.bipedLeftLeg.rotationPointZ + 2 + scaleFactor;
        if (entity.isSneaking()) {
            hipY += 2F;
            hipZ += -8F;
        }

        float leftBackYd = 0;
        float rightBackYd = 0;
        if (source.bipedLeftLeg.rotateAngleX > 0) {
            leftBackYd = -0.5F;
        } else if (source.bipedLeftLeg.rotateAngleX < 0) {
            rightBackYd = -0.5F;
        }

        PositionTextureVertex leftFrontOuterLeg = v(-0.25F + hipX + (float) leftLegOuterVector.xCoord,
            hipY + (float) leftLegOuterVector.yCoord,
            0.25F + hipZ + crotchZdisplacement + (float) leftLegOuterVector.zCoord + FRONT_CLEARANCE);
        PositionTextureVertex leftFrontInnerLeg = v(hipX + (4F + scaleFactor) + (float) leftLegVector.xCoord,
            hipY + (float) leftLegVector.yCoord,
            hipZ + crotchZdisplacement + (float) leftLegVector.zCoord + FRONT_CLEARANCE);
        PositionTextureVertex leftBackOuterLeg = v(-0.25F + hipX + (float) leftLegVector2.xCoord,
            sneakButtHeight + hipY + leftBackYd + (float) leftLegVector2.yCoord,
            sneakLegDepth - 0.25F + hipZ + buttZdisplacement - (4F + 0.25F + scaleFactor * 2) + (float) leftLegVector2.zCoord);
        PositionTextureVertex leftBackInnerLeg = v(hipX + (4F + scaleFactor) + (float) leftLegVector2.xCoord,
            sneakButtHeight + hipY + leftBackYd + (float) leftLegVector2.yCoord,
            sneakLegDepth - 0.25F + hipZ + buttZdisplacement - (4F + 0.25F + scaleFactor * 2) + (float) leftLegVector2.zCoord);
        PositionTextureVertex leftFrontOuterHip = v(hipX, hipY, hipZ + crotchZdisplacement + FRONT_CLEARANCE);
        PositionTextureVertex leftFrontInnerHip = v(hipX + (4F + scaleFactor), hipY, hipZ + crotchZdisplacement + FRONT_CLEARANCE);
        PositionTextureVertex leftBackOuterHip = v(hipX, sneakButtHeight + hipY + leftBackYd,
            sneakLegDepth + buttZdisplacement + hipZ - (4F + 0.25F + scaleFactor * 2));
        PositionTextureVertex leftBackInnerHip = rightBackInnerHip;
        if (!open) {
            leftFrontInnerHip = rightFrontInnerHip;
        }

        int texX = 40;
        int texY = 15;
        TexturedQuad[] quads = new TexturedQuad[12];

        quads[2] = quad(new PositionTextureVertex[] { rightBackOuterHip, rightFrontOuterHip, rightFrontOuterLeg, rightBackOuterLeg },
            texX + 0, texY + 0, texX + 4, texY + 4);
        quads[3] = quad(new PositionTextureVertex[] { rightFrontOuterHip, rightBackOuterHip, rightBackOuterLeg, rightFrontOuterLeg },
            texX + 0, texY + 0, texX + 4, texY + 4);
        quads[10] = quad(new PositionTextureVertex[] { leftBackOuterHip, leftFrontOuterHip, leftFrontOuterLeg, leftBackOuterLeg },
            texX + 12, texY + 0, texX + 16, texY + 4);
        quads[11] = quad(new PositionTextureVertex[] { leftFrontOuterHip, leftBackOuterHip, leftBackOuterLeg, leftFrontOuterLeg },
            texX + 12, texY + 0, texX + 16, texY + 4);

        if (open) {
            quads[0] = quad(new PositionTextureVertex[] { rightFrontInnerLeg, rightFrontOuterLeg, rightFrontOuterHip, rightFrontInnerHip },
                texX + 4, texY + 0, texX + 8, texY + 4);
            quads[1] = quad(new PositionTextureVertex[] { rightFrontInnerHip, rightFrontOuterHip, rightFrontOuterLeg, rightFrontInnerLeg },
                texX + 4, texY + 0, texX + 8, texY + 4);
            quads[8] = quad(new PositionTextureVertex[] { leftFrontOuterHip, leftFrontInnerHip, leftFrontInnerLeg, leftFrontOuterLeg },
                texX + 8, texY + 0, texX + 12, texY + 4);
            quads[9] = quad(new PositionTextureVertex[] { leftFrontOuterLeg, leftFrontInnerLeg, leftFrontInnerHip, leftFrontOuterHip },
                texX + 8, texY + 0, texX + 12, texY + 4);
        }

        if (source.bipedRightLeg.rotateAngleX < 0) {
            quads[4] = quad(new PositionTextureVertex[] { rightBackOuterHip, rightBackInnerHip, leftBackInnerLeg, rightBackOuterLeg },
                texX + 20, texY + 0, texX + 24, texY + 4);
            quads[5] = quad(new PositionTextureVertex[] { rightBackInnerHip, rightBackOuterHip, rightBackOuterLeg, leftBackInnerLeg },
                texX + 20, texY + 0, texX + 24, texY + 4);
            quads[6] = quad(new PositionTextureVertex[] { leftBackInnerLeg, leftBackOuterLeg, leftBackOuterHip, leftBackInnerHip },
                texX + 16, texY + 0, texX + 20, texY + 4);
            quads[7] = quad(new PositionTextureVertex[] { leftBackInnerHip, leftBackOuterHip, leftBackOuterLeg, leftBackInnerLeg },
                texX + 16, texY + 0, texX + 20, texY + 4);
            if (!open) {
                quads[0] = quad(new PositionTextureVertex[] { rightFrontOuterHip, rightFrontInnerHip, rightFrontInnerLeg, rightFrontOuterLeg },
                    texX + 20, texY + 0, texX + 24, texY + 4);
                quads[1] = quad(new PositionTextureVertex[] { rightFrontInnerHip, rightFrontOuterHip, rightFrontOuterLeg, rightFrontInnerLeg },
                    texX + 20, texY + 0, texX + 24, texY + 4);
                quads[8] = quad(new PositionTextureVertex[] { leftFrontOuterLeg, leftFrontOuterHip, leftFrontInnerHip, rightFrontInnerLeg },
                    texX + 16, texY + 0, texX + 20, texY + 4);
                quads[9] = quad(new PositionTextureVertex[] { rightFrontInnerLeg, leftFrontInnerHip, leftFrontOuterHip, leftFrontOuterLeg },
                    texX + 16, texY + 0, texX + 20, texY + 4);
            }
        } else {
            quads[4] = quad(new PositionTextureVertex[] { rightBackOuterHip, rightBackInnerHip, rightBackInnerLeg, rightBackOuterLeg },
                texX + 20, texY + 0, texX + 24, texY + 4);
            quads[5] = quad(new PositionTextureVertex[] { rightBackInnerHip, rightBackOuterHip, rightBackOuterLeg, rightBackInnerLeg },
                texX + 20, texY + 0, texX + 24, texY + 4);
            quads[6] = quad(new PositionTextureVertex[] { leftBackOuterLeg, leftBackOuterHip, leftBackInnerHip, rightBackInnerLeg },
                texX + 16, texY + 0, texX + 20, texY + 4);
            quads[7] = quad(new PositionTextureVertex[] { rightBackInnerLeg, leftBackInnerHip, leftBackOuterHip, leftBackOuterLeg },
                texX + 16, texY + 0, texX + 20, texY + 4);
            if (!open) {
                quads[0] = quad(new PositionTextureVertex[] { rightFrontOuterHip, rightFrontInnerHip, leftFrontInnerLeg, rightFrontOuterLeg },
                    texX + 20, texY + 0, texX + 24, texY + 4);
                quads[1] = quad(new PositionTextureVertex[] { rightFrontInnerHip, rightFrontOuterHip, rightFrontOuterLeg, leftFrontInnerLeg },
                    texX + 20, texY + 0, texX + 24, texY + 4);
                quads[8] = quad(new PositionTextureVertex[] { leftFrontInnerLeg, leftFrontOuterLeg, leftFrontOuterHip, leftFrontInnerHip },
                    texX + 16, texY + 0, texX + 20, texY + 4);
                quads[9] = quad(new PositionTextureVertex[] { leftFrontInnerHip, leftFrontOuterHip, leftFrontOuterLeg, leftFrontInnerLeg },
                    texX + 16, texY + 0, texX + 20, texY + 4);
            }
        }

        Tessellator tessellator = Tessellator.instance;
        for (int i = 0; i < quads.length; ++i) {
            if (quads[i] != null) {
                quads[i].draw(tessellator, renderScale);
            }
        }
    }

    /**
     * Build a quad; frame conversion R(180,Z) = (-x, y, -z) is applied to the vertex
     * positions inside {@link #v(float, float, float)}. UVs are pixel coords into the
     * 64x32 clothing texture, normalized by TexturedQuad.
     */
    private TexturedQuad quad(PositionTextureVertex[] vertices, int u1, int v1, int u2, int v2) {
        return new TexturedQuad(vertices, u1, v1, u2, v2, 64, 32);
    }

    private PositionTextureVertex v(float x, float y, float z) {
        return new PositionTextureVertex(-x, y, -z, 0.0F, 0.0F);
    }
}
