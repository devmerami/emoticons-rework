package mchorse.emoticons.client;

import mchorse.emoticons.api.animation.model.AnimatorEmoticonsController;
import mchorse.emoticons.skin_n_bones.api.bobj.BOBJArmature;
import mchorse.emoticons.skin_n_bones.api.bobj.BOBJBone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class EmoticonsArmorLayerRenderer
{
    private static final float PIXEL_SCALE = 0.0625F;
    private static final Field LAYER_RENDERERS = ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h");

    public static void render(RenderPlayer renderer, AnimatorEmoticonsController animator, EntityLivingBase entity, double x, double y, double z, float partialTicks)
    {
        if (renderer == null || animator == null || animator.animation == null || animator.animation.meshes.isEmpty())
        {
            return;
        }

        BOBJArmature armature = animator.animation.meshes.get(0).getCurrentArmature();
        LayerBipedArmor armorLayer = getArmorLayer(renderer);

        if (armature == null || armorLayer == null)
        {
            return;
        }

        float limbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float ageInTicks = entity.ticksExisted + partialTicks;
        float yaw = interpolateYaw(entity, partialTicks);
        float netHeadYaw = interpolate(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks) - yaw;
        float headPitch = interpolate(entity.prevRotationPitch, entity.rotationPitch, partialTicks);

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GL11.glTranslated(x, y, z);
        GL11.glScalef(animator.userConfig.scale, animator.userConfig.scale, animator.userConfig.scale);

        if (entity.isPlayerSleeping())
        {
            GlStateManager.rotate(((EntityPlayer) entity).getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
        }
        else
        {
            GL11.glRotatef(180 - (yaw - 180), 0.0F, 1.0F, 0.0F);
        }

        renderSlot(renderer, armorLayer, animator, armature, entity, EntityEquipmentSlot.HEAD, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
        renderSlot(renderer, armorLayer, animator, armature, entity, EntityEquipmentSlot.CHEST, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
        renderSlot(renderer, armorLayer, animator, armature, entity, EntityEquipmentSlot.LEGS, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
        renderSlot(renderer, armorLayer, animator, armature, entity, EntityEquipmentSlot.FEET, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static void renderSlot(RenderPlayer renderer, LayerBipedArmor armorLayer, AnimatorEmoticonsController animator, BOBJArmature armature, EntityLivingBase entity, EntityEquipmentSlot slot, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
    {
        ItemStack stack = entity.getItemStackFromSlot(slot);

        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor))
        {
            return;
        }

        ItemArmor item = (ItemArmor) stack.getItem();

        if (item.getEquipmentSlot() != slot)
        {
            return;
        }

        ModelBiped armorModel = ForgeHooksClient.getArmorModel(entity, stack, slot, armorLayer.getModelFromSlot(slot));

        if (armorModel == null || armorModel.getClass() == ModelBiped.class)
        {
            return;
        }

        prepareModel(renderer, armorModel, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
        setModelSlotVisible(armorModel, slot);

        ResourceLocation texture = armorLayer.getArmorResource(entity, stack, slot, null);

        if (item.hasOverlay(stack))
        {
            int color = item.getColor(stack);
            float red = (float) (color >> 16 & 255) / 255.0F;
            float green = (float) (color >> 8 & 255) / 255.0F;
            float blue = (float) (color & 255) / 255.0F;

            renderModel(animator, armature, armorModel, slot, texture, red, green, blue);
            renderModel(animator, armature, armorModel, slot, armorLayer.getArmorResource(entity, stack, slot, "overlay"), 1F, 1F, 1F);
        }
        else
        {
            renderModel(animator, armature, armorModel, slot, texture, 1F, 1F, 1F);
        }
    }

    private static void prepareModel(RenderPlayer renderer, ModelBiped armorModel, EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
    {
        armorModel.isSneak = entity.isSneaking();
        armorModel.isRiding = entity.isRiding() && entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit();
        armorModel.isChild = entity.isChild();
        armorModel.swingProgress = entity.getSwingProgress(partialTicks);

        if (renderer.getMainModel() != null)
        {
            armorModel.setModelAttributes(renderer.getMainModel());
        }

        armorModel.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        armorModel.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, PIXEL_SCALE, entity);
    }

    private static void renderModel(AnimatorEmoticonsController animator, BOBJArmature armature, ModelBiped armorModel, EntityEquipmentSlot slot, ResourceLocation texture, float red, float green, float blue)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(red, green, blue, 1F);

        if (slot == EntityEquipmentSlot.HEAD)
        {
            renderPart(animator, armature, animator.userConfig.head, armorModel.bipedHead, true);
        }
        else if (slot == EntityEquipmentSlot.CHEST)
        {
            renderPart(animator, armature, "body", armorModel.bipedBody, false);
            renderPart(animator, armature, "right_arm", armorModel.bipedRightArm, false);
            renderPart(animator, armature, "left_arm", armorModel.bipedLeftArm, false);
        }
        else if (slot == EntityEquipmentSlot.LEGS)
        {
            renderPart(animator, armature, "low_body", armorModel.bipedBody, false);
            renderPart(animator, armature, "right_leg", armorModel.bipedRightLeg, false);
            renderPart(animator, armature, "left_leg", armorModel.bipedLeftLeg, false);
        }
        else if (slot == EntityEquipmentSlot.FEET)
        {
            renderPart(animator, armature, "low_leg_right", armorModel.bipedRightLeg, false);
            renderPart(animator, armature, "low_left_leg", armorModel.bipedLeftLeg, false);
        }
    }

    private static void renderPart(AnimatorEmoticonsController animator, BOBJArmature armature, String boneName, ModelRenderer part, boolean head)
    {
        if (part == null || !part.showModel || part.isHidden)
        {
            return;
        }

        BOBJBone bone = armature.bones.get(boneName);

        if (bone == null)
        {
            return;
        }

        float rotationPointX = part.rotationPointX;
        float rotationPointY = part.rotationPointY;
        float rotationPointZ = part.rotationPointZ;
        float rotateAngleX = part.rotateAngleX;
        float rotateAngleY = part.rotateAngleY;
        float rotateAngleZ = part.rotateAngleZ;
        float offsetX = part.offsetX;
        float offsetY = part.offsetY;
        float offsetZ = part.offsetZ;

        part.rotationPointX = 0F;
        part.rotationPointY = 0F;
        part.rotationPointZ = 0F;
        part.rotateAngleX = 0F;
        part.rotateAngleY = 0F;
        part.rotateAngleZ = 0F;
        part.offsetX = 0F;
        part.offsetY = 0F;
        part.offsetZ = 0F;

        GlStateManager.pushMatrix();
        animator.setupMatrix(bone);

        if (head)
        {
            GlStateManager.translate(0.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        }

        renderPartGeometry(part);
        GlStateManager.popMatrix();

        part.rotationPointX = rotationPointX;
        part.rotationPointY = rotationPointY;
        part.rotationPointZ = rotationPointZ;
        part.rotateAngleX = rotateAngleX;
        part.rotateAngleY = rotateAngleY;
        part.rotateAngleZ = rotateAngleZ;
        part.offsetX = offsetX;
        part.offsetY = offsetY;
        part.offsetZ = offsetZ;
    }

    private static void renderPartGeometry(ModelRenderer part)
    {
        part.render(PIXEL_SCALE);
    }

    private static void setModelSlotVisible(ModelBiped armorModel, EntityEquipmentSlot slot)
    {
        armorModel.setVisible(false);

        if (slot == EntityEquipmentSlot.HEAD)
        {
            armorModel.bipedHead.showModel = true;
            armorModel.bipedHeadwear.showModel = true;
        }
        else if (slot == EntityEquipmentSlot.CHEST)
        {
            armorModel.bipedBody.showModel = true;
            armorModel.bipedRightArm.showModel = true;
            armorModel.bipedLeftArm.showModel = true;
        }
        else if (slot == EntityEquipmentSlot.LEGS)
        {
            armorModel.bipedBody.showModel = true;
            armorModel.bipedRightLeg.showModel = true;
            armorModel.bipedLeftLeg.showModel = true;
        }
        else if (slot == EntityEquipmentSlot.FEET)
        {
            armorModel.bipedRightLeg.showModel = true;
            armorModel.bipedLeftLeg.showModel = true;
        }
    }

    private static LayerBipedArmor getArmorLayer(RenderPlayer renderer)
    {
        for (LayerRenderer<EntityLivingBase> layer : getLayers(renderer))
        {
            if (layer instanceof LayerBipedArmor)
            {
                return (LayerBipedArmor) layer;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<LayerRenderer<EntityLivingBase>> getLayers(RenderPlayer renderer)
    {
        try
        {
            return (List<LayerRenderer<EntityLivingBase>>) LAYER_RENDERERS.get(renderer);
        }
        catch (IllegalAccessException e)
        {
            return Collections.emptyList();
        }
    }

    private static float interpolateYaw(EntityLivingBase entity, float partialTicks)
    {
        float yaw = interpolate(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);

        if (entity.isRiding())
        {
            Entity vehicle = entity.getRidingEntity();

            if (vehicle instanceof EntityMinecart)
            {
                yaw = interpolate(vehicle.prevRotationYaw, vehicle.rotationYaw, partialTicks) + 90;
            }
        }

        return yaw;
    }

    private static float interpolate(float prev, float current, float partialTicks)
    {
        float result;

        for (result = current - prev; result < -180.0F; result += 360.0F)
        {}

        while (result >= 180.0F)
        {
            result -= 360.0F;
        }

        return prev + partialTicks * result;
    }
}
