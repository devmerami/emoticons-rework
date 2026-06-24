package mchorse.emoticons.epicfight;

import maninthehouse.epicfight.animation.types.DynamicAnimation;
import maninthehouse.epicfight.animation.types.LinkAnimation;
import maninthehouse.epicfight.animation.types.StaticAnimation;
import mchorse.emoticons.capabilities.cosmetic.Cosmetic;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;

public class EpicFightAttackState
{
    private final EpicFightPlayerRenderer renderer = new EpicFightPlayerRenderer();
    private final EpicFightAnimationSelector.ComboState comboState = new EpicFightAnimationSelector.ComboState();

    private EpicFightPlayerData playerData;
    private AbstractClientPlayer player;
    private StaticAnimation activeAnimation;
    private boolean lastSwingInProgress;
    private boolean ladderAnimationActive;

    public void update(EntityLivingBase target)
    {
        if (!(target instanceof AbstractClientPlayer))
        {
            return;
        }

        EpicFightRuntime.initClient();

        if (!EpicFightRuntime.isReady())
        {
            return;
        }

        AbstractClientPlayer player = (AbstractClientPlayer) target;

        this.ensurePlayerData(player);

        EpicFightWeaponAnimationType weaponType = EpicFightAttackConfig.resolve(player, player.getHeldItemMainhand());
        this.ladderAnimationActive = weaponType != EpicFightWeaponAnimationType.NONE && player.isOnLadder();
        this.playerData.setLadderAnimationActive(this.ladderAnimationActive);
        this.playerData.applyWeaponProfile(weaponType);
        this.playerData.update();
        this.updateAttack(player, weaponType);
    }

    private void ensurePlayerData(AbstractClientPlayer player)
    {
        if (this.player != player || this.playerData == null)
        {
            this.player = player;
            this.playerData = new EpicFightPlayerData();
            this.playerData.onEntityConstructed(player);
            this.playerData.onEntityJoinWorld(player);
            this.activeAnimation = null;
            this.lastSwingInProgress = false;
            this.ladderAnimationActive = false;
            this.comboState.reset();
        }
    }

    private void updateAttack(AbstractClientPlayer player, EpicFightWeaponAnimationType weaponType)
    {
        boolean swingInProgress = player.isSwingInProgress;
        boolean swingStarted = swingInProgress && (!this.lastSwingInProgress || player.swingProgressInt == 0);

        this.lastSwingInProgress = swingInProgress;

        this.updateActionState();

        if (this.activeAnimation != null)
        {
            return;
        }

        if (weaponType != EpicFightWeaponAnimationType.NONE && swingStarted)
        {
            StaticAnimation nextAttack = EpicFightAnimationSelector.selectNextAttack(player, this.comboState);

            if (nextAttack != null)
            {
                this.startAction(nextAttack);
                this.suppressVanillaSwing(player);
            }
        }
    }

    private void updateActionState()
    {
        if (this.activeAnimation != null)
        {
            DynamicAnimation currentAnimation = this.playerData.getClientAnimator().getPlayer().getPlay();

            if (currentAnimation != this.activeAnimation && !(currentAnimation instanceof LinkAnimation))
            {
                this.activeAnimation = null;
            }
        }
    }

    private void startAction(StaticAnimation animation)
    {
        this.activeAnimation = animation;
        this.playerData.getClientAnimator().playAnimation(animation, 0.0F);

        if (this.player != null)
        {
            if (Cosmetic.get(this.player) instanceof Cosmetic)
            {
                ((Cosmetic) Cosmetic.get(this.player)).startClientAttackMovementLock(this.player, EpicFightAnimationSelector.getAnimationTicks(animation));
            }
        }
    }

    public boolean playRoll(AbstractClientPlayer player, boolean forward)
    {
        EpicFightRuntime.initClient();

        if (!EpicFightRuntime.isReady())
        {
            return false;
        }

        this.ensurePlayerData(player);
        this.updateActionState();

        if (this.activeAnimation != null || player.getRidingEntity() != null || !player.onGround)
        {
            return false;
        }

        StaticAnimation rollAnimation = EpicFightAnimationSelector.getRollAnimation(forward);

        if (rollAnimation == null)
        {
            return false;
        }

        this.playerData.applyWeaponProfile(EpicFightAttackConfig.resolve(player, player.getHeldItemMainhand()));
        this.startAction(rollAnimation);

        return true;
    }

    public boolean forceRoll(AbstractClientPlayer player, boolean forward)
    {
        EpicFightRuntime.initClient();

        if (!EpicFightRuntime.isReady())
        {
            return false;
        }

        this.ensurePlayerData(player);

        StaticAnimation rollAnimation = EpicFightAnimationSelector.getRollAnimation(forward);

        if (rollAnimation == null || player.getRidingEntity() != null || !player.onGround)
        {
            return false;
        }

        this.playerData.applyWeaponProfile(EpicFightAttackConfig.resolve(player, player.getHeldItemMainhand()));
        this.startAction(rollAnimation);

        return true;
    }

    public boolean shouldRender(AbstractClientPlayer player)
    {
        return this.player == player && this.playerData != null && (this.activeAnimation != null || player.getRidingEntity() != null);
    }

    public void render(AbstractClientPlayer player, double x, double y, double z, float partialTicks)
    {
        if (this.shouldRender(player))
        {
            this.renderer.renderPlayer(player, this.playerData, x, y, z, partialTicks);
        }
    }

    private void suppressVanillaSwing(AbstractClientPlayer player)
    {
        if (EpicFightAttackConfig.resolve(player.getHeldItem(EnumHand.MAIN_HAND)) == EpicFightWeaponAnimationType.NONE)
        {
            return;
        }

        player.isSwingInProgress = false;
        player.swingProgress = 0.0F;
        player.prevSwingProgress = 0.0F;
        player.swingProgressInt = 0;
    }
}
