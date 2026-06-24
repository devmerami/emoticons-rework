package mchorse.emoticons.epicfight;

import maninthehouse.epicfight.animation.types.StaticAnimation;
import maninthehouse.epicfight.gamedata.Animations;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

public final class EpicFightAnimationSelector
{
    private EpicFightAnimationSelector()
    {}

    public static StaticAnimation selectNextAttack(EntityLivingBase entity, ComboState comboState)
    {
        EpicFightRuntime.initServer();

        EpicFightWeaponAnimationType weaponType = EpicFightAttackConfig.resolve(entity, entity.getHeldItemMainhand());
        boolean mounted = entity.getRidingEntity() != null;
        boolean sprintingAttack = entity.onGround && entity.isSprinting() && entity.limbSwingAmount > 0.01F;

        if (weaponType == EpicFightWeaponAnimationType.SWORD)
        {
            boolean dualSword = EpicFightAttackConfig.resolve(entity.getHeldItemOffhand()) == EpicFightWeaponAnimationType.SWORD;

            if (mounted)
            {
                return Animations.SWORD_MOUNT_ATTACK;
            }

            if (sprintingAttack)
            {
                return dualSword ? Animations.SWORD_DUAL_DASH : Animations.SWORD_DASH;
            }

            if (dualSword)
            {
                comboState.dualSwordCombo = (comboState.dualSwordCombo + 1) % 3;

                return comboState.dualSwordCombo == 0 ? Animations.SWORD_DUAL_AUTO_1 : comboState.dualSwordCombo == 1 ? Animations.SWORD_DUAL_AUTO_2 : Animations.SWORD_DUAL_AUTO_3;
            }

            comboState.swordCombo = (comboState.swordCombo + 1) % 3;

            return comboState.swordCombo == 0 ? Animations.SWORD_AUTO_1 : comboState.swordCombo == 1 ? Animations.SWORD_AUTO_2 : Animations.SWORD_AUTO_3;
        }

        if (weaponType == EpicFightWeaponAnimationType.GREATSWORD)
        {
            if (sprintingAttack)
            {
                return Animations.GREATSWORD_DASH;
            }

            comboState.greatswordCombo = (comboState.greatswordCombo + 1) % 2;

            return comboState.greatswordCombo == 0 ? Animations.GREATSWORD_AUTO_1 : Animations.GREATSWORD_AUTO_2;
        }

        if (weaponType == EpicFightWeaponAnimationType.SPEAR)
        {
            if (mounted)
            {
                return Animations.SPEAR_MOUNT_ATTACK;
            }

            if (sprintingAttack)
            {
                return Animations.SPEAR_DASH;
            }

            if (entity.getHeldItemOffhand().isEmpty())
            {
                comboState.spearCombo = (comboState.spearCombo + 1) % 2;

                return comboState.spearCombo == 0 ? Animations.SPEAR_TWOHAND_AUTO_1 : Animations.SPEAR_TWOHAND_AUTO_2;
            }

            return Animations.SPEAR_ONEHAND_AUTO;
        }

        if (weaponType == EpicFightWeaponAnimationType.AXE)
        {
            if (mounted)
            {
                return Animations.SWORD_MOUNT_ATTACK;
            }

            if (sprintingAttack)
            {
                return Animations.AXE_DASH;
            }

            comboState.axeCombo = (comboState.axeCombo + 1) % 2;

            return comboState.axeCombo == 0 ? Animations.AXE_AUTO1 : Animations.AXE_AUTO2;
        }

        return null;
    }

    public static int getAnimationTicks(StaticAnimation animation)
    {
        if (animation == null)
        {
            return 0;
        }

        return Math.max(1, MathHelper.ceil(animation.getTotalTime() * 20.0F));
    }

    public static StaticAnimation getRollAnimation(boolean forward)
    {
        EpicFightRuntime.initServer();

        return forward ? Animations.BIPED_ROLL_FORWARD : Animations.BIPED_ROLL_BACKWARD;
    }

    public static int getRollTicks(boolean forward)
    {
        return getAnimationTicks(getRollAnimation(forward));
    }

    public static class ComboState
    {
        private int swordCombo;
        private int dualSwordCombo;
        private int spearCombo;
        private int greatswordCombo;
        private int axeCombo;

        public void reset()
        {
            this.swordCombo = 0;
            this.dualSwordCombo = 0;
            this.spearCombo = 0;
            this.greatswordCombo = 0;
            this.axeCombo = 0;
        }
    }
}
