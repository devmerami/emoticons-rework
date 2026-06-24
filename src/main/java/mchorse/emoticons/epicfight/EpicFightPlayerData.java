package mchorse.emoticons.epicfight;

import maninthehouse.epicfight.animation.LivingMotion;
import maninthehouse.epicfight.animation.types.StaticAnimation;
import maninthehouse.epicfight.capabilities.entity.LivingData;
import maninthehouse.epicfight.client.animation.AnimatorClient;
import maninthehouse.epicfight.gamedata.Animations;
import maninthehouse.epicfight.gamedata.Models;
import maninthehouse.epicfight.model.Model;
import maninthehouse.epicfight.utils.game.IExtendedDamageSource.StunType;
import net.minecraft.client.entity.AbstractClientPlayer;

public class EpicFightPlayerData extends LivingData<AbstractClientPlayer>
{
    private EpicFightWeaponAnimationType currentProfile = EpicFightWeaponAnimationType.NONE;
    private boolean ladderAnimationActive;

    @Override
    protected void initAnimator(AnimatorClient animatorClient)
    {
        this.commonBipedCreatureAnimatorInit(animatorClient);
        animatorClient.addLivingAnimation(LivingMotion.RUNNING, Animations.BIPED_RUN);
        animatorClient.addLivingAnimation(LivingMotion.SNEAKING, Animations.BIPED_SNEAK);
        animatorClient.addLivingAnimation(LivingMotion.SWIMMING, Animations.BIPED_SWIM);
        animatorClient.addLivingAnimation(LivingMotion.FLOATING, Animations.BIPED_FLOAT);
        animatorClient.addLivingAnimation(LivingMotion.FLYING, Animations.BIPED_FLYING);
        animatorClient.addLivingAnimation(LivingMotion.JUMPING, Animations.BIPED_JUMP);
        animatorClient.addLivingAnimation(LivingMotion.KNEELING, Animations.BIPED_KNEEL);
        animatorClient.setCurrentLivingMotionsToDefault();
    }

    public void applyWeaponProfile(EpicFightWeaponAnimationType profile)
    {
        AnimatorClient animatorClient = this.getClientAnimator();

        if (animatorClient == null || this.currentProfile == profile)
        {
            return;
        }

        animatorClient.resetModifiedLivingMotions();

        if (profile == EpicFightWeaponAnimationType.SPEAR)
        {
            animatorClient.addModifiedLivingMotion(LivingMotion.RUNNING, Animations.BIPED_RUN_HELDING_WEAPON);
        }
        else if (profile == EpicFightWeaponAnimationType.GREATSWORD)
        {
            animatorClient.addModifiedLivingMotion(LivingMotion.IDLE, Animations.BIPED_IDLE_MASSIVE_HELD);
            animatorClient.addModifiedLivingMotion(LivingMotion.WALKING, Animations.BIPED_WALK_MASSIVE_HELD);
            animatorClient.addModifiedLivingMotion(LivingMotion.RUNNING, Animations.BIPED_RUN_MASSIVE_HELD);
            animatorClient.addModifiedLivingMotion(LivingMotion.JUMPING, Animations.BIPED_JUMP_MASSIVE_HELD);
            animatorClient.addModifiedLivingMotion(LivingMotion.KNEELING, Animations.BIPED_KNEEL_MASSIVE_HELD);
            animatorClient.addModifiedLivingMotion(LivingMotion.SNEAKING, Animations.BIPED_SNEAK_MASSIVE_HELD);
        }

        this.currentProfile = profile;
    }

    public void setLadderAnimationActive(boolean ladderAnimationActive)
    {
        this.ladderAnimationActive = ladderAnimationActive;
    }

    public boolean isLadderAnimationActive()
    {
        return this.ladderAnimationActive;
    }

    @Override
    public void updateMotion()
    {
        this.currentMixMotion = LivingMotion.NONE;

        if (this.orgEntity.getHealth() <= 0.0F)
        {
            this.currentMotion = LivingMotion.DEATH;
        }
        else if (this.orgEntity.getRidingEntity() != null)
        {
            this.currentMotion = LivingMotion.MOUNT;
        }
        else if (this.orgEntity.capabilities.isFlying)
        {
            this.currentMotion = LivingMotion.FLYING;
        }
        else if (this.orgEntity.isOnLadder())
        {
            this.currentMotion = Math.abs(this.orgEntity.motionY) > 0.01D || this.orgEntity.limbSwingAmount > 0.01F ? LivingMotion.WALKING : LivingMotion.IDLE;
        }
        else if (this.orgEntity.isInWater())
        {
            this.currentMotion = this.orgEntity.limbSwingAmount > 0.01F ? LivingMotion.SWIMMING : LivingMotion.FLOATING;
        }
        else if (!this.orgEntity.onGround)
        {
            this.currentMotion = this.orgEntity.motionY < -0.05D ? LivingMotion.FALL : LivingMotion.JUMPING;
        }
        else if (this.orgEntity.isSneaking())
        {
            this.currentMotion = this.orgEntity.limbSwingAmount > 0.01F ? LivingMotion.SNEAKING : LivingMotion.KNEELING;
        }
        else if (this.orgEntity.isSprinting() && this.orgEntity.limbSwingAmount > 0.01F)
        {
            this.currentMotion = LivingMotion.RUNNING;
        }
        else if (this.orgEntity.limbSwingAmount > 0.01F)
        {
            this.currentMotion = LivingMotion.WALKING;
        }
        else
        {
            this.currentMotion = LivingMotion.IDLE;
        }
    }

    @Override
    public <M extends Model> M getEntityModel(Models<M> modelDB)
    {
        return this.orgEntity != null && "slim".equals(this.orgEntity.getSkinType()) ? modelDB.ENTITY_BIPED_SLIM_ARM : modelDB.ENTITY_BIPED;
    }

    @Override
    public StaticAnimation getHitAnimation(StunType stunType)
    {
        return Animations.BIPED_HIT_SHORT;
    }
}
