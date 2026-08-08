package com.eternal130.mobends_tfcp_compat;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

/**
 * Replaces TFC+ clothing models with a vanilla {@link ModelBiped} subclass that mirrors
 * MoBends' {@code ModelBendsPlayer} part hierarchy and box dimensions.
 *
 * <p><b>Why mirror MoBends' hierarchy:</b> MoBends' body lean (sprint, fly, swim) is applied
 * to {@code bipedBody.rotateAngleX}. Arms and head are children of body, so they inherit the
 * lean automatically. If arms/head are flat siblings of body (as in vanilla ModelBiped), they
 * don't follow the lean — clothing would detach from the body during sprint/fly/swim. This
 * adapter re-parents bipedHead/bipedRightArm/bipedLeftArm as children of bipedBody, matching
 * MoBends' structure so the inheritance works.
 *
 * <p><b>Geometry source:</b> box dimensions (addBox width/height/depth) and texture offsets
 * come from TFC+ clothing models so the stock clothing PNGs sample correctly. Rotation points
 * come from MoBends so parts pivot at the same world positions as the player body.
 *
 * <p><b>Animation source:</b> {@code syncFromModelBiped} copies rotateAngle from MoBends'
 * model into this adapter's parts each frame. {@code setRotationAngles} is overridden to NOP
 * so vanilla walk/swing math doesn't overwrite the synced pose.
 */
public class ModelBipedClothingAdapter extends ModelBiped {

    public enum ClothingType {
        SHIRT, PANTS, SOCKS, HAT, COAT, NULL;
    }

    private final ClothingType type;
    private final float scaleFactor;
    private ModelRenderer clothingBody;
    private ModelRenderer clothingHead;
    private ModelRenderer clothingArmR;
    private ModelRenderer clothingArmL;
    private ModelRenderer clothingLegR;
    private ModelRenderer clothingLegL;
    private ModelRenderer clothingForeLegR;
    private ModelRenderer clothingForeLegL;

    public ModelBipedClothingAdapter(ClothingType type, float scaleFactor) {
        super(0.0F, 0.0F, 64, 32);
        this.type = type;
        this.scaleFactor = 0.5F;
        this.isChild = false;
        configureGeometry();
    }

    @Override
    public void setRotationAngles(float swing, float swingAmount, float ageInTicks,
            float headYaw, float headPitch, float scale, Entity entity) {
    }

    private void configureGeometry() {
        bipedHead.showModel = false;
        bipedHeadwear.showModel = false;
        bipedRightArm.showModel = false;
        bipedLeftArm.showModel = false;
        bipedRightLeg.showModel = false;
        bipedLeftLeg.showModel = false;
        bipedCloak.showModel = false;
        bipedBody.cubeList.clear();
        bipedBody.showModel = true;
        bipedBody.setRotationPoint(0F, 12F, 0F);

        clothingBody = new ModelRenderer(this, 16, 0);
        clothingBody.setRotationPoint(0F, 0F, 0F);
        bipedBody.addChild(clothingBody);

        clothingHead = new ModelRenderer(this, 0, 0);
        clothingHead.setRotationPoint(0F, -12F, 0F);
        clothingBody.addChild(clothingHead);

        clothingArmR = new ModelRenderer(this, 16, 16);
        clothingArmR.setRotationPoint(-5F, -10F, 0F);
        clothingBody.addChild(clothingArmR);

        clothingArmL = new ModelRenderer(this, 16, 16);
        clothingArmL.mirror = true;
        clothingArmL.setRotationPoint(5F, -10F, 0F);
        clothingBody.addChild(clothingArmL);

        bipedRightLeg.cubeList.clear();
        bipedRightLeg.showModel = true;
        bipedRightLeg.setRotationPoint(-1.9F, 12F, 0F);
        bipedLeftLeg.cubeList.clear();
        bipedLeftLeg.showModel = true;
        bipedLeftLeg.setRotationPoint(1.9F, 12F, 0F);

        clothingLegR = new ModelRenderer(this, 16, 16);
        clothingLegR.setRotationPoint(0F, 0F, 0F);
        bipedRightLeg.addChild(clothingLegR);

        clothingLegL = new ModelRenderer(this, 16, 16);
        clothingLegL.mirror = true;
        clothingLegL.setRotationPoint(0F, 0F, 0F);
        bipedLeftLeg.addChild(clothingLegL);

        clothingForeLegR = new ModelRenderer(this, 16, 16);
        clothingForeLegR.setRotationPoint(0F, 6F, -2F);
        clothingLegR.addChild(clothingForeLegR);

        clothingForeLegL = new ModelRenderer(this, 16, 16);
        clothingForeLegL.mirror = true;
        clothingForeLegL.setRotationPoint(0F, 6F, -2F);
        clothingLegL.addChild(clothingForeLegL);

        clothingBody.showModel = true;
        clothingHead.showModel = true;
        clothingArmR.showModel = true;
        clothingArmL.showModel = true;
        clothingLegR.showModel = true;
        clothingLegL.showModel = true;
        clothingForeLegR.showModel = true;
        clothingForeLegL.showModel = true;

        clothingBody.cubeList.clear();
        clothingHead.cubeList.clear();
        clothingArmR.cubeList.clear();
        clothingArmL.cubeList.clear();
        clothingLegR.cubeList.clear();
        clothingLegL.cubeList.clear();
        clothingForeLegR.cubeList.clear();
        clothingForeLegL.cubeList.clear();

        switch (type) {
            case SHIRT:
            case COAT:
                configureShirtOrCoat();
                break;
            case PANTS:
                configurePants();
                break;
            case SOCKS:
                configureSocks();
                break;
            case HAT:
                configureHat();
                break;
            default:
                break;
        }
    }

    private void configureShirtOrCoat() {
        float pad = (type == ClothingType.COAT) ? 0.5F : 0.0F;
        float armPad = (type == ClothingType.COAT) ? 0.3F : 0.0F;
        clothingBody.setTextureOffset(16, 0);
        clothingBody.addBox(-4F, -12F, -2F, 8, 12, 4, scaleFactor + pad);
        clothingArmR.setTextureOffset(16, 16);
        clothingArmR.addBox(-3F, -2F, -2F, 4, 6, 4, scaleFactor + armPad);
        clothingArmL.setTextureOffset(16, 16);
        clothingArmL.addBox(-1F, -2F, -2F, 4, 6, 4, scaleFactor + armPad);
    }

    private void configurePants() {
        clothingBody.setTextureOffset(16, 0);
        clothingBody.addBox(-4F, -12F, -2F, 8, 12, 4, scaleFactor);
        clothingLegR.setTextureOffset(16, 16);
        clothingLegR.addBox(-2F, 0F, -2F, 4, 6, 4, scaleFactor);
        clothingLegL.setTextureOffset(16, 16);
        clothingLegL.addBox(-2F, 0F, -2F, 4, 6, 4, scaleFactor);
        clothingForeLegR.setTextureOffset(16, 16);
        clothingForeLegR.addBox(-2F, 0F, 0F, 4, 6, 4, scaleFactor);
        clothingForeLegL.setTextureOffset(16, 16);
        clothingForeLegL.addBox(-2F, 0F, 0F, 4, 6, 4, scaleFactor);
    }

    private void configureSocks() {
        clothingForeLegR.setTextureOffset(16, 23);
        clothingForeLegR.addBox(-2F, 2F, -2F, 4, 4, 4, scaleFactor);
        clothingForeLegL.setTextureOffset(16, 23);
        clothingForeLegL.addBox(-2F, 2F, -2F, 4, 4, 4, scaleFactor);
    }

    private void configureHat() {
        clothingHead.setTextureOffset(0, 0);
        clothingHead.addBox(-4F, -9F, -6F, 8, 4, 10, scaleFactor);
    }

    public void syncFromModelBiped(ModelBiped source) {
        if (source == null) return;
        bipedBody.rotateAngleX = source.bipedBody.rotateAngleX;
        bipedBody.rotateAngleY = source.bipedBody.rotateAngleY;
        bipedBody.rotateAngleZ = source.bipedBody.rotateAngleZ;
        clothingHead.rotateAngleX = source.bipedHead.rotateAngleX;
        clothingHead.rotateAngleY = source.bipedHead.rotateAngleY;
        clothingHead.rotateAngleZ = source.bipedHead.rotateAngleZ;
        clothingArmR.rotateAngleX = source.bipedRightArm.rotateAngleX;
        clothingArmR.rotateAngleY = source.bipedRightArm.rotateAngleY;
        clothingArmR.rotateAngleZ = source.bipedRightArm.rotateAngleZ;
        clothingArmL.rotateAngleX = source.bipedLeftArm.rotateAngleX;
        clothingArmL.rotateAngleY = source.bipedLeftArm.rotateAngleY;
        clothingArmL.rotateAngleZ = source.bipedLeftArm.rotateAngleZ;
        clothingLegR.rotateAngleX = source.bipedRightLeg.rotateAngleX;
        clothingLegR.rotateAngleY = source.bipedRightLeg.rotateAngleY;
        clothingLegR.rotateAngleZ = source.bipedRightLeg.rotateAngleZ;
        clothingLegL.rotateAngleX = source.bipedLeftLeg.rotateAngleX;
        clothingLegL.rotateAngleY = source.bipedLeftLeg.rotateAngleY;
        clothingLegL.rotateAngleZ = source.bipedLeftLeg.rotateAngleZ;
        if (source instanceof net.gobbob.mobends.client.model.entity.ModelBendsPlayer) {
            net.gobbob.mobends.client.model.entity.ModelBendsPlayer bm =
                (net.gobbob.mobends.client.model.entity.ModelBendsPlayer) source;
            clothingForeLegR.rotateAngleX = bm.bipedRightForeLeg.rotateAngleX;
            clothingForeLegR.rotateAngleY = bm.bipedRightForeLeg.rotateAngleY;
            clothingForeLegR.rotateAngleZ = bm.bipedRightForeLeg.rotateAngleZ;
            clothingForeLegL.rotateAngleX = bm.bipedLeftForeLeg.rotateAngleX;
            clothingForeLegL.rotateAngleY = bm.bipedLeftForeLeg.rotateAngleY;
            clothingForeLegL.rotateAngleZ = bm.bipedLeftForeLeg.rotateAngleZ;
        }
        this.isSneak = source.isSneak;
        this.aimedBow = source.aimedBow;
        this.isChild = source.isChild;
        this.onGround = source.onGround;
        this.heldItemLeft = source.heldItemLeft;
        this.heldItemRight = source.heldItemRight;
    }
}
