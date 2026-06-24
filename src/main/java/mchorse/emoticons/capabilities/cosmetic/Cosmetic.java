package mchorse.emoticons.capabilities.cosmetic;

import maninthehouse.epicfight.animation.types.StaticAnimation;
import mchorse.emoticons.ClientProxy;
import mchorse.emoticons.Emoticons;
import mchorse.emoticons.api.animation.model.AnimatorEmoticonsController;
import mchorse.emoticons.blockbuster.BBIntegration;
import mchorse.emoticons.client.EmoticonsArmorLayerRenderer;
import mchorse.emoticons.common.EmoteAPI;
import mchorse.emoticons.common.emotes.Emote;
import mchorse.emoticons.epicfight.EpicFightAnimationSelector;
import mchorse.emoticons.epicfight.EpicFightAttackState;
import mchorse.emoticons.skin_n_bones.api.animation.model.ActionConfig;
import mchorse.emoticons.skin_n_bones.api.animation.model.ActionPlayback;
import mchorse.emoticons.skin_n_bones.api.bobj.BOBJArmature;
import mchorse.mclib.client.render.RenderLightmap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Vector4f;

public class Cosmetic implements ICosmetic
{
    private static final int ROLL_COOLDOWN_TICKS = 100;

    @SideOnly(Side.CLIENT)
    public AnimatorEmoticonsController animator;

    @SideOnly(Side.CLIENT)
    public ActionPlayback emoteAction;

    @SideOnly(Side.CLIENT)
    public EpicFightAttackState attackAnimation;

    @SideOnly(Side.CLIENT)
    public RenderPlayer renderer;

    public Emote emote;

    /* Trackers */
    private int emoteTimer;
    private double lastX;
    private double lastY;
    private double lastZ;
    private long lastUpdate = System.currentTimeMillis();
    private int actionLockTicks;
    private int attackMovementLockTicks;
    private int movementInputLockTicks;
    private int rollCooldownTicks;
    private int rollInvulnerableTicks;
    private boolean pendingAllowedHit;
    private boolean lastServerSwingInProgress;
    private final EpicFightAnimationSelector.ComboState serverComboState = new EpicFightAnimationSelector.ComboState();

    public static ICosmetic get(Entity entity)
    {
        return entity.getCapability(CosmeticProvider.COSMETIC, null);
    }

    @Override
    public void setEmote(Emote emote, EntityLivingBase target)
    {
        if (target.world.isRemote)
        {
            this.stopAction(target);
        }

        this.emote = emote;
        this.emoteTimer = 0;

        if (target.world.isRemote)
        {
            this.setActionEmote(emote, target);
        }

        if (BBIntegration.isLoaded() && target instanceof EntityPlayerMP)
        {
            BBIntegration.recordEmote(emote == null ? "" : emote.name, (EntityPlayer) target);
        }
    }

    @Override
    public Emote getEmote()
    {
        return this.emote;
    }

    @Override
    public void update(EntityLivingBase target)
    {
        if (target.world.isRemote)
        {
            this.updateClient(target);
        }
        else
        {
            this.updateServerCombatState(target);

            if (this.emote != null)
            {
                double diff = Math.abs((target.posX - this.lastX) + (target.posY - this.lastY) + (target.posZ - this.lastZ));

                if (diff > 0.015 || (!this.emote.looping && this.emoteTimer >= this.emote.duration))
                {
                    EmoteAPI.setEmote("", (EntityPlayerMP) target);
                }

                this.emoteTimer++;
            }

            this.lastX = target.posX;
            this.lastY = target.posY;
            this.lastZ = target.posZ;
        }
    }

    private void updateServerCombatState(EntityLivingBase target)
    {
        boolean swingInProgress = target.isSwingInProgress;
        boolean swingStarted = swingInProgress && !this.lastServerSwingInProgress;

        this.lastServerSwingInProgress = swingInProgress;

        if (swingStarted && this.actionLockTicks <= 0)
        {
            this.beginAttackLock(target);
        }

        this.tickActionTimers();
    }

    private void tickActionTimers()
    {
        if (this.actionLockTicks > 0)
        {
            this.actionLockTicks--;

            if (this.actionLockTicks <= 0)
            {
                this.actionLockTicks = 0;
                this.pendingAllowedHit = false;
            }
        }

        if (this.attackMovementLockTicks > 0)
        {
            this.attackMovementLockTicks--;
        }

        if (this.movementInputLockTicks > 0)
        {
            this.movementInputLockTicks--;
        }

        if (this.rollCooldownTicks > 0)
        {
            this.rollCooldownTicks--;
        }

        if (this.rollInvulnerableTicks > 0)
        {
            this.rollInvulnerableTicks--;
        }
    }

    public boolean beginAttackLock(EntityLivingBase target)
    {
        StaticAnimation animation = EpicFightAnimationSelector.selectNextAttack(target, this.serverComboState);

        if (animation == null)
        {
            return false;
        }

        this.clearHorizontalMomentum(target);
        this.startActionLock(EpicFightAnimationSelector.getAnimationTicks(animation), true);
        this.attackMovementLockTicks = Math.max(this.attackMovementLockTicks, EpicFightAnimationSelector.getAnimationTicks(animation));
        this.movementInputLockTicks = Math.max(this.movementInputLockTicks, EpicFightAnimationSelector.getAnimationTicks(animation));

        return true;
    }

    public void startActionLock(int ticks, boolean allowHit)
    {
        if (ticks <= 0)
        {
            return;
        }

        this.actionLockTicks = Math.max(this.actionLockTicks, ticks);
        this.pendingAllowedHit = this.pendingAllowedHit || allowHit;
    }

    public void startRollLock(int ticks)
    {
        if (ticks <= 0)
        {
            return;
        }

        this.startActionLock(ticks, false);
        this.movementInputLockTicks = Math.max(this.movementInputLockTicks, ticks);
        this.rollCooldownTicks = Math.max(this.rollCooldownTicks, ROLL_COOLDOWN_TICKS);
        this.rollInvulnerableTicks = Math.max(this.rollInvulnerableTicks, ticks);
    }

    public void clearHorizontalMomentum(EntityLivingBase target)
    {
        target.setSprinting(false);
        target.motionX = 0.0D;
        target.motionZ = 0.0D;
    }

    public boolean isActionLocked()
    {
        return this.actionLockTicks > 0;
    }

    public boolean isAttackMovementLocked()
    {
        return this.attackMovementLockTicks > 0;
    }

    public boolean isMovementInputLocked()
    {
        return this.movementInputLockTicks > 0;
    }

    @SideOnly(Side.CLIENT)
    public boolean shouldHideFirstPersonPreview(EntityLivingBase target)
    {
        if (this.emote != null)
        {
            return true;
        }

        return target instanceof AbstractClientPlayer && this.attackAnimation != null && this.attackAnimation.shouldRender((AbstractClientPlayer) target);
    }

    public void startClientAttackMovementLock(EntityLivingBase target, int ticks)
    {
        if (ticks <= 0)
        {
            return;
        }

        this.clearHorizontalMomentum(target);
        this.attackMovementLockTicks = Math.max(this.attackMovementLockTicks, ticks);
        this.movementInputLockTicks = Math.max(this.movementInputLockTicks, ticks);
    }

    public boolean isRollOnCooldown()
    {
        return this.rollCooldownTicks > 0;
    }

    public boolean isRollInvulnerable()
    {
        return this.rollInvulnerableTicks > 0;
    }

    public boolean consumeAllowedHit()
    {
        if (this.pendingAllowedHit)
        {
            this.pendingAllowedHit = false;

            return true;
        }

        return false;
    }

    @SideOnly(Side.CLIENT)
    private void updateClient(EntityLivingBase target)
    {
        this.tickActionTimers();

        if (this.emote != null)
        {
            double diff = Math.abs((target.posX - this.lastX) + (target.posY - this.lastY) + (target.posZ - this.lastZ));

            if (diff > 0.015 || (!this.emote.looping && this.emoteTimer >= this.emote.duration))
            {
                this.setEmote(null, target);
            }
        }

        this.lastX = target.posX;
        this.lastY = target.posY;
        this.lastZ = target.posZ;

        if (this.emote != null && this.emoteAction != null)
        {
            if (this.emote.sound != null && this.emoteAction.getTick(0) == 0)
            {
                target.world.playSound(target.posX, target.posY, target.posZ, this.emote.sound, SoundCategory.MASTER, 0.33F, 1, false);
            }

            this.emote.updateEmote(target, this.animator, (int) this.emoteAction.getTick(0));
            this.emoteTimer++;
        }

        if (ClientProxy.lastUpdate > this.lastUpdate)
        {
            this.setupAnimator(target);
            this.lastUpdate = ClientProxy.lastUpdate;
        }

        if (this.animator != null)
        {
            this.animator.update(target);
        }

        if (this.attackAnimation == null)
        {
            this.attackAnimation = new EpicFightAttackState();
        }

        this.attackAnimation.update(target);
    }

    @SideOnly(Side.CLIENT)
    public boolean playRoll(EntityLivingBase target, boolean forward)
    {
        if (this.emote != null || this.isActionLocked() || this.isRollOnCooldown() || !(target instanceof AbstractClientPlayer))
        {
            return false;
        }

        if (this.attackAnimation == null)
        {
            this.attackAnimation = new EpicFightAttackState();
        }

        if (this.attackAnimation.playRoll((AbstractClientPlayer) target, forward))
        {
            this.clearHorizontalMomentum(target);
            int rollTicks = EpicFightAnimationSelector.getRollTicks(forward);

            this.startActionLock(rollTicks, false);
            this.rollCooldownTicks = Math.max(this.rollCooldownTicks, ROLL_COOLDOWN_TICKS);
            this.rollInvulnerableTicks = Math.max(this.rollInvulnerableTicks, rollTicks);

            return true;
        }

        return false;
    }

    @SideOnly(Side.CLIENT)
    public void playRollFromServer(EntityLivingBase target, boolean forward)
    {
        if (!(target instanceof AbstractClientPlayer))
        {
            return;
        }

        if (this.attackAnimation == null)
        {
            this.attackAnimation = new EpicFightAttackState();
        }

        if (this.attackAnimation.forceRoll((AbstractClientPlayer) target, forward))
        {
            this.clearHorizontalMomentum(target);
            int rollTicks = EpicFightAnimationSelector.getRollTicks(forward);

            this.startActionLock(rollTicks, false);
            this.rollCooldownTicks = Math.max(this.rollCooldownTicks, ROLL_COOLDOWN_TICKS);
            this.rollInvulnerableTicks = Math.max(this.rollInvulnerableTicks, rollTicks);
        }
    }

    @SideOnly(Side.CLIENT)
    private void stopAction(EntityLivingBase target)
    {
        if (this.emote != null)
        {
            this.emote.stopAnimation(this.animator);
        }
    }

    @SideOnly(Side.CLIENT)
    private void setActionEmote(Emote emote, EntityLivingBase target)
    {
        if (this.animator == null)
        {
            this.setupAnimator(target);
        }

        if (emote != null)
        {
            ActionConfig config = this.animator.config.config.actions.getConfig("emote_" + emote.name);

            this.emoteAction = this.animator.animation.createAction(null, config, emote.looping);
            this.animator.setEmote(this.emoteAction);

            emote.startAnimation(this.animator);
        }
        else
        {
            this.emoteAction = null;
            this.animator.setEmote(null);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean render(EntityLivingBase entity, double x, double y, double z, float partialTicks)
    {
        if (this.animator == null)
        {
            this.setupAnimator(entity);
        }

        boolean disable = Emoticons.disableAnimations.get();

        if (!disable && this.emote == null && entity instanceof AbstractClientPlayer && this.attackAnimation != null && this.attackAnimation.shouldRender((AbstractClientPlayer) entity))
        {
            this.attackAnimation.render((AbstractClientPlayer) entity, x, y, z, partialTicks);

            return true;
        }

        boolean render = this.animator != null && (!disable || this.emote != null);

        if (render)
        {
            if (entity instanceof AbstractClientPlayer)
            {
                AbstractClientPlayer player = (AbstractClientPlayer) entity;
                String type = player.getSkinType() + this.getPrefix();

                if (!type.equals(this.animator.animationName))
                {
                    this.animator.animationName = type;
                    this.animator.animation = null;
                    this.animator.fetchAnimation();
                }

                this.animator.userConfig.meshes.get("body").texture = player.getLocationSkin();
            }

            this.animator.render(this.emote, entity, x, y, z, 0, partialTicks);
            EmoticonsArmorLayerRenderer.render(this.renderer, this.animator, entity, x, y, z, partialTicks);

            BOBJArmature armature = this.animator.animation.meshes.get(0).getCurrentArmature();
            Minecraft mc = Minecraft.getMinecraft();

            if (RenderLightmap.canRenderNamePlate(entity))
            {
                RenderManager manager = mc.getRenderManager();
                Vector4f vec = this.animator.calcPosition(entity, armature.bones.get("head"), 0F, 0F, 0F, partialTicks);
                float pYaw = manager.playerViewY;
                float pPitch = manager.playerViewX;
                boolean frontal = mc.gameSettings.thirdPersonView == 2;

                float nx = vec.x - (float) manager.viewerPosX;
                float ny = vec.y - (float) manager.viewerPosY + 0.7F;
                float nz = vec.z - (float) manager.viewerPosZ;

                EntityRenderer.drawNameplate(mc.fontRenderer, entity.getDisplayName().getFormattedText(), nx, ny, nz, -6, pYaw, pPitch, frontal, entity.isSneaking());
            }

            if (this.emote != null && this.emoteAction != null && !Minecraft.getMinecraft().isGamePaused())
            {
                int tick = (int) this.emoteAction.getTick(0);

                this.emote.progressAnimation(entity, armature, this.animator, tick, partialTicks);
            }
        }

        return render;
    }

    @SideOnly(Side.CLIENT)
    public void setRenderer(RenderPlayer renderer)
    {
        this.renderer = renderer;
    }

    @SideOnly(Side.CLIENT)
    private String getPrefix()
    {
        int mode = Emoticons.modelType.get();

        if (mode == 1)
        {
            return "_simple";
        }
        else if (mode == 2)
        {
            return "_3d";
        }
        else if (mode == 3)
        {
            return "_simple_plus";
        }

        return "";
    }

    @SideOnly(Side.CLIENT)
    public void setupAnimator(EntityLivingBase entity)
    {
        AbstractClientPlayer player = (AbstractClientPlayer) entity;

        this.animator = new AnimatorEmoticonsController(player.getSkinType(), new NBTTagCompound());

        NBTTagCompound meshes = new NBTTagCompound();
        NBTTagCompound body = new NBTTagCompound();

        meshes.setTag("body", body);
        body.setString("Texture", player.getLocationSkin().toString());

        this.animator.userData.setTag("Meshes", meshes);
        this.animator.fetchAnimation();
    }
}
