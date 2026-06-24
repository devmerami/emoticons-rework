package mchorse.emoticons.epicfight;

import maninthehouse.epicfight.animation.Joint;
import maninthehouse.epicfight.client.model.ClientModel;
import maninthehouse.epicfight.client.model.ClientModels;
import maninthehouse.epicfight.client.model.custom.CustomModelBakery;
import maninthehouse.epicfight.client.renderer.entity.BipedRenderer;
import maninthehouse.epicfight.model.Armature;
import maninthehouse.epicfight.utils.math.Vec3f;
import maninthehouse.epicfight.utils.math.VisibleMatrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class EpicFightPlayerRenderer extends BipedRenderer<AbstractClientPlayer, EpicFightPlayerData>
{
    private static final Field LAYER_RENDERERS = ReflectionHelper.findField(net.minecraft.client.renderer.entity.RenderLivingBase.class, "layerRenderers", "field_177097_h");
    private static final ResourceLocation ENCHANTED_ITEM_GLINT_RES = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private final VisibleMatrix4f correctionMatrix = this.createCorrectionMatrix();
    private final Map<ResourceLocation, ClientModel> armorModelCache = new HashMap<ResourceLocation, ClientModel>();
    private final Map<ModelBiped, ClientModel> customArmorModelCache = new HashMap<ModelBiped, ClientModel>();
    private final Map<String, ResourceLocation> armorTextureCache = new HashMap<String, ResourceLocation>();

    public void renderPlayer(AbstractClientPlayer player, EpicFightPlayerData playerData, double x, double y, double z, float partialTicks)
    {
        RenderPlayer renderer = Minecraft.getMinecraft().getRenderManager().getSkinMap().get(player.getSkinType());

        if (renderer != null)
        {
            this.render(player, playerData, renderer, x, y, z, partialTicks);
        }
    }

    @Override
    protected void applyRotations(Armature armature, AbstractClientPlayer entityIn, EpicFightPlayerData entitydata, double x, double y, double z, float partialTicks)
    {
        super.applyRotations(armature, entityIn, entitydata, x, y, z, partialTicks);
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected ResourceLocation getEntityTexture(AbstractClientPlayer entityIn)
    {
        return entityIn.getLocationSkin();
    }

    @Override
    protected void renderLayer(AbstractClientPlayer entityIn, EpicFightPlayerData entitydata, Armature armature, float partialTicks)
    {
        this.resetLayerRenderState(entityIn);
        this.renderVanillaLayers(entityIn, entitydata, armature, partialTicks);
        this.resetLayerRenderState(entityIn);
        this.renderArmorLayers(entityIn, armature, partialTicks);
        this.resetLayerRenderState(entityIn);
        this.renderHeadItem(entityIn, armature);
        this.resetLayerRenderState(entityIn);
        this.renderItemInHand(entityIn, armature, entityIn.getHeldItemMainhand(), EnumHand.MAIN_HAND);
        this.resetLayerRenderState(entityIn);
        this.renderItemInHand(entityIn, armature, entityIn.getHeldItemOffhand(), EnumHand.OFF_HAND);
    }

    private void resetLayerRenderState(AbstractClientPlayer player)
    {
        int brightness = player.getBrightnessForRender();
        float blockLight = brightness & 65535;
        float skyLight = brightness >> 16;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, blockLight, skyLight);
    }

    private void renderVanillaLayers(AbstractClientPlayer entityIn, EpicFightPlayerData entitydata, Armature armature, float partialTicks)
    {
        RenderPlayer renderer = Minecraft.getMinecraft().getRenderManager().getSkinMap().get(entityIn.getSkinType());

        if (renderer == null)
        {
            return;
        }

        ModelPlayer model = renderer.getMainModel();
        float ageInTicks = entityIn.ticksExisted + partialTicks;
        float netHeadYaw = entityIn.rotationYawHead - entityIn.renderYawOffset;
        float headPitch = entityIn.rotationPitch;

        model.swingProgress = 0.0F;
        model.isSneak = entityIn.isSneaking();
        model.isRiding = entityIn.isRiding();
        model.isChild = entityIn.isChild();
        model.setLivingAnimations(entityIn, entityIn.limbSwing, entityIn.limbSwingAmount, partialTicks);
        model.setRotationAngles(entityIn.limbSwing, entityIn.limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, entityIn);
        this.applyLayerPose(model, armature);

        for (LayerRenderer layer : this.getLayerRenderers(renderer))
        {
            if (layer instanceof LayerHeldItem || layer instanceof LayerCustomHead || layer instanceof LayerBipedArmor)
            {
                continue;
            }

            layer.doRenderLayer(entityIn, entityIn.limbSwing, entityIn.limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, 0.0625F);
        }
    }

    private void renderArmorLayers(AbstractClientPlayer player, Armature armature, float partialTicks)
    {
        RenderPlayer renderer = Minecraft.getMinecraft().getRenderManager().getSkinMap().get(player.getSkinType());
        LayerBipedArmor armorLayer = this.getArmorLayer(renderer);
        VisibleMatrix4f[] poses = armature.getJointTransforms();

        if (renderer == null || armorLayer == null)
        {
            return;
        }

        float limbSwingAmount = player.prevLimbSwingAmount + (player.limbSwingAmount - player.prevLimbSwingAmount) * partialTicks;
        float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks);
        float ageInTicks = player.ticksExisted + partialTicks;
        float netHeadYaw = player.rotationYawHead - player.renderYawOffset;
        float headPitch = player.rotationPitch;

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values())
        {
            if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR)
            {
                continue;
            }

            this.renderArmorLayer(player, renderer, armorLayer, armature, poses, slot, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
        }
    }

    private void renderArmorLayer(AbstractClientPlayer player, RenderPlayer renderer, LayerBipedArmor armorLayer, Armature armature, VisibleMatrix4f[] poses, EntityEquipmentSlot slot, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
    {
        ItemStack stack = player.getItemStackFromSlot(slot);

        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor))
        {
            return;
        }

        ItemArmor armorItem = (ItemArmor) stack.getItem();

        if (armorItem.getEquipmentSlot() != slot)
        {
            return;
        }

        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        boolean hasOverlay = armorItem.hasOverlay(stack);

        GlStateManager.pushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        if (slot == EntityEquipmentSlot.HEAD)
        {
            ModelBiped armorModel = ForgeHooksClient.getArmorModel(player, stack, slot, armorLayer.getModelFromSlot(slot));

            if (armorModel == null)
            {
                GlStateManager.popMatrix();
                return;
            }

            this.prepareArmorModel(renderer, armorModel, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks);
            textureManager.bindTexture(armorLayer.getArmorResource(player, stack, slot, null));

            if (hasOverlay)
            {
                int color = armorItem.getColor(stack);
                float red = (float) (color >> 16 & 255) / 255.0F;
                float green = (float) (color >> 8 & 255) / 255.0F;
                float blue = (float) (color & 255) / 255.0F;

                GlStateManager.color(red, green, blue, 1.0F);
            }
            else
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            this.renderArmorModel(armorModel, armature, slot);

            if (hasOverlay)
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                textureManager.bindTexture(armorLayer.getArmorResource(player, stack, slot, "overlay"));
                this.renderArmorModel(armorModel, armature, slot);
            }

            if (stack.hasEffect())
            {
                this.renderEnchantedGlint(player, armorModel, armature, slot, partialTicks);
            }
        }
        else
        {
            ModelBiped armorModel = ForgeHooksClient.getArmorModel(player, stack, slot, armorLayer.getModelFromSlot(slot));

            if (armorModel == null)
            {
                GlStateManager.popMatrix();
                return;
            }

            textureManager.bindTexture(armorLayer.getArmorResource(player, stack, slot, null));

            if (hasOverlay)
            {
                int color = armorItem.getColor(stack);
                float red = (float) (color >> 16 & 255) / 255.0F;
                float green = (float) (color >> 8 & 255) / 255.0F;
                float blue = (float) (color & 255) / 255.0F;

                GlStateManager.color(red, green, blue, 1.0F);
            }
            else
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            ClientModel bakedModel = CustomModelBakery.bakeBipedCustomArmorModel(armorModel, armorItem);

            if (bakedModel == null)
            {
                GlStateManager.popMatrix();
                return;
            }

            bakedModel.draw(poses);

            if (hasOverlay)
            {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                textureManager.bindTexture(armorLayer.getArmorResource(player, stack, slot, "overlay"));
                bakedModel.draw(poses);
            }

            if (stack.hasEffect())
            {
                this.renderEnchantedGlint(player, bakedModel, partialTicks, poses);
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private ClientModel getArmorModel(AbstractClientPlayer player, ItemArmor armorItem, ItemStack stack, EntityEquipmentSlot slot)
    {
        ResourceLocation registryName = armorItem.getRegistryName();

        if (registryName != null)
        {
            ClientModel cached = this.armorModelCache.get(registryName);

            if (cached != null)
            {
                return cached;
            }
        }

        ModelBiped originalModel = this.getOriginalArmorModel(player, slot);
        ModelBiped customModel = armorItem.getArmorModel(player, stack, slot, originalModel);
        ClientModel resolvedModel;

        if (customModel != null)
        {
            resolvedModel = this.customArmorModelCache.get(customModel);

            if (resolvedModel == null)
            {
                resolvedModel = CustomModelBakery.bakeBipedCustomArmorModel(customModel, armorItem);

                if (resolvedModel != null)
                {
                    this.customArmorModelCache.put(customModel, resolvedModel);
                }
            }
        }
        else
        {
            resolvedModel = this.getDefaultArmorModel(slot);
        }

        if (registryName != null && resolvedModel != null)
        {
            this.armorModelCache.put(registryName, resolvedModel);
        }

        return resolvedModel;
    }

    private ModelBiped getOriginalArmorModel(AbstractClientPlayer player, EntityEquipmentSlot slot)
    {
        RenderPlayer renderer = Minecraft.getMinecraft().getRenderManager().getSkinMap().get(player.getSkinType());

        if (renderer != null)
        {
            for (LayerRenderer layer : this.getLayerRenderers(renderer))
            {
                if (layer instanceof LayerArmorBase)
                {
                    return (ModelBiped) ((LayerArmorBase<?>) layer).getModelFromSlot(slot);
                }
            }
        }

        return new ModelBiped(slot == EntityEquipmentSlot.LEGS ? 0.5F : 1.0F);
    }

    private ClientModel getDefaultArmorModel(EntityEquipmentSlot slot)
    {
        ClientModels models = ClientModels.LOGICAL_CLIENT;

        switch (slot)
        {
            case HEAD:
                return models.ITEM_HELMET;
            case CHEST:
                return models.ITEM_CHESTPLATE;
            case LEGS:
                return models.ITEM_LEGGINS;
            case FEET:
                return models.ITEM_BOOTS;
            default:
                return null;
        }
    }

    private ResourceLocation getArmorTexture(ItemStack stack, AbstractClientPlayer player, EntityEquipmentSlot slot, String type)
    {
        ItemArmor armorItem = (ItemArmor) stack.getItem();
        String texture = armorItem.getArmorMaterial().getName();
        String domain = "minecraft";
        int separator = texture.indexOf(':');

        if (separator != -1)
        {
            domain = texture.substring(0, separator);
            texture = texture.substring(separator + 1);
        }

        String texturePath = String.format("%s:textures/models/armor/%s_layer_%d%s.png",
            domain,
            texture,
            slot == EntityEquipmentSlot.LEGS ? 2 : 1,
            type == null ? "" : String.format("_%s", type));

        texturePath = ForgeHooksClient.getArmorTexture(player, stack, texturePath, slot, type);

        ResourceLocation location = this.armorTextureCache.get(texturePath);

        if (location == null)
        {
            location = new ResourceLocation(texturePath);
            this.armorTextureCache.put(texturePath, location);
        }

        return location;
    }

    private void prepareArmorModel(RenderPlayer renderer, ModelBiped armorModel, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float partialTicks)
    {
        armorModel.isSneak = player.isSneaking();
        armorModel.isRiding = player.isRiding();
        armorModel.isChild = player.isChild();
        armorModel.swingProgress = 0.0F;

        if (renderer.getMainModel() != null)
        {
            armorModel.setModelAttributes(renderer.getMainModel());
        }

        armorModel.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);
        armorModel.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, player);
    }

    private void renderArmorModel(ModelBiped armorModel, Armature armature, EntityEquipmentSlot slot)
    {
        this.setModelSlotVisible(armorModel, slot);

        if (slot == EntityEquipmentSlot.HEAD)
        {
            this.renderArmorPart(armature, "Head", armorModel.bipedHead);
            this.renderArmorPart(armature, "Head", armorModel.bipedHeadwear);
        }
        else if (slot == EntityEquipmentSlot.CHEST)
        {
            this.renderArmorPart(armature, "Chest", armorModel.bipedBody);
            this.renderArmorPart(armature, "Arm_R", armorModel.bipedRightArm);
            this.renderArmorPart(armature, "Arm_L", armorModel.bipedLeftArm);
        }
        else if (slot == EntityEquipmentSlot.LEGS)
        {
            this.renderArmorPart(armature, "Torso", armorModel.bipedBody);
            this.renderArmorPart(armature, "Thigh_R", armorModel.bipedRightLeg);
            this.renderArmorPart(armature, "Thigh_L", armorModel.bipedLeftLeg);
        }
        else if (slot == EntityEquipmentSlot.FEET)
        {
            this.renderArmorPart(armature, "Leg_R", armorModel.bipedRightLeg);
            this.renderArmorPart(armature, "Leg_L", armorModel.bipedLeftLeg);
        }
    }

    private void renderArmorPart(Armature armature, String jointName, ModelRenderer part)
    {
        if (part == null || !part.showModel || part.isHidden)
        {
            return;
        }

        Joint joint = armature.findJointByName(jointName);

        if (joint == null)
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

        part.rotationPointX = 0.0F;
        part.rotationPointY = 0.0F;
        part.rotationPointZ = 0.0F;
        part.rotateAngleX = 0.0F;
        part.rotateAngleY = 0.0F;
        part.rotateAngleZ = 0.0F;
        part.offsetX = 0.0F;
        part.offsetY = 0.0F;
        part.offsetZ = 0.0F;

        VisibleMatrix4f modelMatrix = new VisibleMatrix4f();
        VisibleMatrix4f.scale(new Vec3f(-1.0F, -1.0F, 1.0F), modelMatrix, modelMatrix);
        VisibleMatrix4f.mul(joint.getAnimatedTransform(), modelMatrix, modelMatrix);

        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(modelMatrix.toFloatBuffer());
        this.renderPartGeometry(part);
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

    private void renderPartGeometry(ModelRenderer part)
    {
        part.render(0.0625F);
    }

    private void renderEnchantedGlint(AbstractClientPlayer player, ModelBiped armorModel, Armature armature, EntityEquipmentSlot slot, float partialTicks)
    {
        float age = player.ticksExisted + partialTicks;
        Minecraft.getMinecraft().getTextureManager().bindTexture(ENCHANTED_ITEM_GLINT_RES);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(514);
        GlStateManager.depthMask(false);
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);

        for (int i = 0; i < 2; ++i)
        {
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
            GlStateManager.color(0.38F, 0.19F, 0.608F, 1.0F);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
            GlStateManager.rotate(30.0F - i * 60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, age * (0.001F + i * 0.003F) * 20.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            this.renderArmorModel(armorModel, armature, slot);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
        GlStateManager.disableBlend();
        Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
    }

    private void renderEnchantedGlint(AbstractClientPlayer player, ClientModel armorModel, float partialTicks, VisibleMatrix4f[] poses)
    {
        float age = player.ticksExisted + partialTicks;
        Minecraft.getMinecraft().getTextureManager().bindTexture(ENCHANTED_ITEM_GLINT_RES);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(514);
        GlStateManager.depthMask(false);
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);

        for (int i = 0; i < 2; ++i)
        {
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
            GlStateManager.color(0.38F, 0.19F, 0.608F, 1.0F);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
            GlStateManager.rotate(30.0F - i * 60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, age * (0.001F + i * 0.003F) * 20.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            armorModel.draw(poses);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
        GlStateManager.disableBlend();
        Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
    }

    private LayerBipedArmor getArmorLayer(RenderPlayer renderer)
    {
        if (renderer == null)
        {
            return null;
        }

        for (LayerRenderer layer : this.getLayerRenderers(renderer))
        {
            if (layer instanceof LayerBipedArmor)
            {
                return (LayerBipedArmor) layer;
            }
        }

        return null;
    }

    private void setModelSlotVisible(ModelBiped armorModel, EntityEquipmentSlot slot)
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

    private void renderHeadItem(AbstractClientPlayer player, Armature armature)
    {
        ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);

        if (stack.isEmpty())
        {
            return;
        }

        Item item = stack.getItem();

        if (item instanceof ItemArmor && ((ItemArmor) item).getEquipmentSlot() == EntityEquipmentSlot.HEAD)
        {
            return;
        }

        RenderPlayer renderer = Minecraft.getMinecraft().getRenderManager().getSkinMap().get(player.getSkinType());

        if (renderer == null || !(renderer.getMainModel() instanceof ModelBiped))
        {
            return;
        }

        ModelRenderer head = ((ModelBiped) renderer.getMainModel()).bipedHead;
        Joint joint = armature.findJointByName("Head");

        if (joint == null)
        {
            return;
        }

        VisibleMatrix4f modelMatrix = new VisibleMatrix4f();
        VisibleMatrix4f.scale(new Vec3f(-0.94F, -0.94F, 0.94F), modelMatrix, modelMatrix);

        if (player.isChild())
        {
            VisibleMatrix4f.translate(new Vec3f(0.0F, -0.65F, 0.0F), modelMatrix, modelMatrix);
        }

        VisibleMatrix4f.mul(joint.getAnimatedTransform(), modelMatrix, modelMatrix);

        head.rotateAngleX = 0.0F;
        head.rotateAngleY = 0.0F;
        head.rotateAngleZ = 0.0F;
        head.rotationPointX = 0.0F;
        head.rotationPointY = 0.0F;
        head.rotationPointZ = 0.0F;

        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(modelMatrix.toFloatBuffer());
        new LayerCustomHead(head).doRenderLayer(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0925F);
        GlStateManager.popMatrix();
    }

    private void applyLayerPose(ModelPlayer model, Armature armature)
    {
        this.applyJointRotation(model.bipedBody, armature.findJointByName("Chest"));
        this.applyJointRotation(model.bipedHead, armature.findJointByName("Head"));
        this.applyJointRotation(model.bipedRightArm, armature.findJointByName("Arm_R"));
        this.applyJointRotation(model.bipedLeftArm, armature.findJointByName("Arm_L"));
        this.applyJointRotation(model.bipedRightLeg, armature.findJointByName("Thigh_R"));
        this.applyJointRotation(model.bipedLeftLeg, armature.findJointByName("Thigh_L"));
        ModelBase.copyModelAngles(model.bipedHead, model.bipedHeadwear);
        ModelBase.copyModelAngles(model.bipedBody, model.bipedBodyWear);
        ModelBase.copyModelAngles(model.bipedRightArm, model.bipedRightArmwear);
        ModelBase.copyModelAngles(model.bipedLeftArm, model.bipedLeftArmwear);
        ModelBase.copyModelAngles(model.bipedRightLeg, model.bipedRightLegwear);
        ModelBase.copyModelAngles(model.bipedLeftLeg, model.bipedLeftLegwear);
    }

    private void applyJointRotation(ModelRenderer renderer, Joint joint)
    {
        if (renderer == null || joint == null)
        {
            return;
        }

        VisibleMatrix4f pose = joint.getAnimatedTransform();
        float row00 = pose.m00;
        float row01 = pose.m10;
        float row10 = pose.m01;
        float row11 = pose.m11;
        float row12 = pose.m21;
        float row20 = pose.m02;
        float row21 = pose.m12;
        float row22 = pose.m22;
        float yaw = (float) Math.asin(this.clamp(-row20));
        float cosYaw = (float) Math.cos(yaw);
        float pitch;
        float roll;

        if (Math.abs(cosYaw) > 1.0E-4F)
        {
            pitch = (float) Math.atan2(row21, row22);
            roll = (float) Math.atan2(row10, row00);
        }
        else
        {
            pitch = (float) Math.atan2(-row12, row11);
            roll = 0.0F;
        }

        renderer.rotateAngleX = pitch;
        renderer.rotateAngleY = yaw;
        renderer.rotateAngleZ = roll;
    }

    @SuppressWarnings("unchecked")
    private List<LayerRenderer> getLayerRenderers(RenderPlayer renderer)
    {
        try
        {
            return (List<LayerRenderer>) LAYER_RENDERERS.get(renderer);
        }
        catch (IllegalAccessException e)
        {
            return Collections.emptyList();
        }
    }

    private void renderItemInHand(AbstractClientPlayer player, Armature armature, ItemStack stack, EnumHand hand)
    {
        if (stack.isEmpty())
        {
            return;
        }

        Joint joint = armature.findJointByName(hand == EnumHand.MAIN_HAND ? "Tool_R" : "Tool_L");

        if (joint == null)
        {
            return;
        }

        VisibleMatrix4f modelMatrix = new VisibleMatrix4f(this.correctionMatrix);
        VisibleMatrix4f.mul(joint.getAnimatedTransform(), modelMatrix, modelMatrix);

        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(modelMatrix.toFloatBuffer());
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, TransformType.THIRD_PERSON_RIGHT_HAND);
        GlStateManager.popMatrix();
    }

    private VisibleMatrix4f createCorrectionMatrix()
    {
        VisibleMatrix4f matrix = new VisibleMatrix4f();
        VisibleMatrix4f.rotate((float) Math.toRadians(-80), new Vec3f(1.0F, 0.0F, 0.0F), matrix, matrix);
        VisibleMatrix4f.translate(new Vec3f(0.0F, 0.1F, 0.0F), matrix, matrix);

        return matrix;
    }

    private float clamp(float value)
    {
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

}
