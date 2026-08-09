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
        SHIRT, PANTS, SHORTS, SOCKS, CLOTH_HAT, STRAW_HAT, STRAW_HAT2,
        FUR_HAT_BEAR, FUR_HAT_WOLF, COAT, ROBE, CLOAK, NULL;
    }

    private final ClothingType type;
    private final float scaleFactor;
    private ModelRenderer clothingBody;
    private ModelRenderer clothingHead;
    private ScaledModelRenderer hatBase;
    private ModelRenderer hatCrown;
    private ModelRenderer hatBulge;
    private ModelRenderer strawBrim;
    private StrawHat2Model strawHat2Cone;
    private ModelRenderer animalHead;
    private ModelRenderer animalSnout;
    private ModelRenderer animalEars;
    private ModelRenderer animalFur;
    private ModelRenderer animalFur2;
    private ModelRenderer clothingArmR;
    private ModelRenderer clothingArmL;
    private ModelRenderer clothingLegR;
    private ModelRenderer clothingLegL;
    private ModelRenderer clothingForeLegR;
    private ModelRenderer clothingForeLegL;
    private ModelRenderer clothingSkirt;

    public ModelBipedClothingAdapter(ClothingType type, float scaleFactor) {
        super(0.0F, 0.0F, 64, 32);
        this.type = type;
        this.scaleFactor = scaleFactor;
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

        clothingSkirt = new ModelRenderer(this, 16, 0);
        clothingSkirt.setRotationPoint(0F, 0F, 0F);
        clothingBody.addChild(clothingSkirt);

        bipedCloak.cubeList.clear();
        bipedCloak.showModel = true;

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
        clothingSkirt.cubeList.clear();

        switch (type) {
            case SHIRT:
            case COAT:
                configureShirtOrCoat();
                break;
            case ROBE:
                configureRobe();
                break;
            case CLOAK:
                configureCloak();
                break;
            case PANTS:
                configurePants(false);
                break;
            case SHORTS:
                configurePants(true);
                break;
            case SOCKS:
                configureSocks();
                break;
            case CLOTH_HAT:
            case STRAW_HAT:
            case STRAW_HAT2:
                configureHat();
                break;
            case FUR_HAT_BEAR:
                configureFurHat(true);
                break;
            case FUR_HAT_WOLF:
                configureFurHat(false);
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

    private void configurePants(boolean shorts) {
        // 2-high hip belt (TFC+ ModelPants body box); a full torso box maps the
        // texture's belt band (rows 0-7) to the chest via front-face V 2..14.
        clothingBody.setTextureOffset(16, 0);
        clothingBody.addBox(-4F, -2F, -2F, 8, 2, 4, scaleFactor);
        clothingLegR.setTextureOffset(16, 16);
        clothingLegR.addBox(-2F, 0F, -2F, 4, 6, 4, scaleFactor);
        clothingLegL.setTextureOffset(16, 16);
        clothingLegL.addBox(-2F, 0F, -2F, 4, 6, 4, scaleFactor);
        // Thigh segment covers y 12..18 (6 high). Long pants add a 2-high shin
        // (y 18..20) so the combined length matches TFC+ ModelPants' 8-high leg
        // (y 12..20); shorts end at the thigh. The shin rides the foreleg part
        // so it follows Mo'Bends' knee bend, and stays above the ankle so socks
        // remain visible (TFC+ pants also stop short of the foot).
        if (!shorts) {
            // texOffset (16,24) splices shin v 24..26 with thigh 18..24 into the
            // exact TFC+ leg window [18,26] (16,16 would sample 16..18 outside it).
            clothingForeLegR.setTextureOffset(16, 24);
            clothingForeLegR.addBox(-2F, 0F, 0F, 4, 2, 4, scaleFactor);
            clothingForeLegL.setTextureOffset(16, 24);
            clothingForeLegL.addBox(-2F, 0F, 0F, 4, 2, 4, scaleFactor);
        }
    }

    private void configureSocks() {
        clothingForeLegR.setTextureOffset(16, 23);
        clothingForeLegR.addBox(-2F, 2F, 0F, 4, 4, 4, scaleFactor);
        clothingForeLegL.setTextureOffset(16, 23);
        clothingForeLegL.addBox(-2F, 2F, 0F, 4, 4, 4, scaleFactor);
    }

    private void configureHat() {
        createHatBase();

        if (type == ClothingType.STRAW_HAT2) {
            strawHat2Cone = new StrawHat2Model(this);
            strawHat2Cone.setScale(1.1F, 1.1F, 1.1F);
            strawHat2Cone.setRotationPoint(0F, -1F, 0F);
            clothingHead.addChild(strawHat2Cone);
            return;
        }

        hatCrown = new ModelRenderer(this, 0, 0);
        hatCrown.addBox(-4F, -9F, -6F, 8, 4, 10, -0.1F);
        hatBase.addChild(hatCrown);

        if (type == ClothingType.STRAW_HAT) {
            strawBrim = new ModelRenderer(this, 20, 0);
            strawBrim.addBox(-7F, -6F, -9F, 14, 1, 16, -0.1F);
            hatCrown.addChild(strawBrim);
        } else {
            hatBulge = new ModelRenderer(this, 0, 14);
            hatBulge.addBox(-4.5F, -7F, -6.5F, 9, 3, 11, -0.4F);
            hatBase.addChild(hatBulge);
        }
    }

    /**
     * Bear/wolf fur hats (TFCItems.bearFurHat / wolfFurHat). Mirrors TFC+ ModelHat's
     * animal branch; snout offsetZ -1/32 on bear, 0 on wolf.
     */
    private void configureFurHat(boolean bear) {
        createHatBase();

        animalHead = new ModelRenderer(this, 36, 0);
        animalHead.addBox(-4F, -10F, -5F, 8, 4, 6);
        animalHead.rotateAngleX = 0.2F;
        hatBase.addChild(animalHead);

        animalSnout = new ModelRenderer(this, 36, 11);
        animalSnout.addBox(-2F, -8F, -8F, 4, 3, 4);
        animalSnout.offsetZ = bear ? -1F / 32F : 0F;
        animalHead.addChild(animalSnout);

        animalEars = new ModelRenderer(this, 36, 18);
        animalEars.addBox(-3.5F, -11.5F, 0F, 2, 3, 1);
        animalEars.addBox(1.5F, -11.5F, 0F, 2, 3, 1);
        animalHead.addChild(animalEars);

        animalFur = new ModelRenderer(this, 5, 19);
        animalFur.addBox(-4.5F, -9F, -5.5F, 9, 6, 6);
        animalFur.rotateAngleX = -0.4F;
        hatBase.addChild(animalFur);

        animalFur2 = new ModelRenderer(this, 5, 19);
        animalFur2.addBox(-4.5F, -5.6F, -6F, 9, 6, 6, -0.1F);
        animalFur2.rotateAngleX = -0.9F;
        animalFur.addChild(animalFur2);
    }

    /**
     * Mirrors TFC+ ModelHat's {@code base} frame: empty parent at the head carrying
     * the -0.2 worn tilt; 1.1x scale applies to every hat part (user-validated).
     */
    private void createHatBase() {
        hatBase = new ScaledModelRenderer(this, 0, 0);
        hatBase.setScale(1.1F, 1.1F, 1.1F);
        hatBase.setRotationPoint(0F, 0F, 0F);
        hatBase.rotateAngleX = -0.2F;
        clothingHead.addChild(hatBase);
    }

    private void configureRobe() {
        clothingBody.setTextureOffset(16, 0);
        clothingBody.addBox(-4F, -12F, -2F, 8, 12, 4, scaleFactor + 0.6F);
        clothingArmR.setTextureOffset(16, 16);
        clothingArmR.addBox(-3F, -2F, -2F, 4, 6, 4, scaleFactor + 0.3F);
        clothingArmL.setTextureOffset(16, 16);
        clothingArmL.addBox(-1F, -2F, -2F, 4, 6, 4, scaleFactor + 0.3F);
        clothingSkirt.setTextureOffset(16, 0);
        clothingSkirt.addBox(-5F, 0F, -3F, 10, 10, 6, scaleFactor + 0.8F);
    }

    private void configureCloak() {
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
