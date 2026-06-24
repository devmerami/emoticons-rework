package maninthehouse.epicfight.client.renderer.entity;

import maninthehouse.epicfight.animation.AnimationPlayer;
import maninthehouse.epicfight.capabilities.entity.LivingData;
import maninthehouse.epicfight.client.model.ClientModel;
import maninthehouse.epicfight.client.model.ClientModels;
import maninthehouse.epicfight.model.Armature;
import maninthehouse.epicfight.utils.math.VisibleMatrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ArmatureRenderer<E extends EntityLivingBase, T extends LivingData<E>> {
	public void render(E entityIn, T entitydata, RenderLivingBase<E> renderer, double x, double y, double z, float partialTicks) {
		renderer.renderName(entityIn, x, y, z);
		ClientModel model = entitydata.getEntityModel(ClientModels.LOGICAL_CLIENT);
		Armature armature = model.getArmature();
		armature.initializeTransform();
		GlStateManager.pushMatrix();
		this.applyRotations(armature, entityIn, entitydata, x, y, z, partialTicks);
		entitydata.getClientAnimator().setPoseToModel(partialTicks);
		VisibleMatrix4f[] poses = armature.getJointTransforms();
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.getEntityTexture(entityIn));
		GlStateManager.disableCull();
		model.draw(poses);
		this.renderLayer(entityIn, entitydata, armature, partialTicks);
		GlStateManager.popMatrix();
	}

	protected void renderLayer(E entityIn, T entitydata, Armature armature, float partialTicks) {
		
	}
	
	protected abstract ResourceLocation getEntityTexture(E entityIn);
	
	protected boolean isVisible(E entityIn) {
		return !entityIn.isInvisible();
	}
	
	protected void applyRotations(Armature armature, E entityIn, T entitydata, double x, double y, double z, float partialTicks) {
		VisibleMatrix4f mat4f = entitydata.getModelMatrix(partialTicks);
		mat4f.m30 = 0.0F;
		mat4f.m31 = 0.0F;
		mat4f.m32 = 0.0F;
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0F, 0, 1, 0);
        GlStateManager.multMatrix(mat4f.toFloatBuffer());
	}
}
