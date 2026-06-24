package maninthehouse.epicfight.capabilities.entity.player;

import maninthehouse.epicfight.animation.types.StaticAnimation;
import maninthehouse.epicfight.capabilities.entity.LivingData;
import maninthehouse.epicfight.entity.event.EntityEventListener;
import maninthehouse.epicfight.utils.game.IExtendedDamageSource.StunType;
import net.minecraft.entity.player.EntityPlayer;

public abstract class PlayerData<T extends EntityPlayer> extends LivingData<T>
{
    public EntityEventListener getEventListener()
    {
        return EntityEventListener.EMPTY;
    }

    public float getAttackSpeed()
    {
        return 4.0F;
    }

    @Override
    public StaticAnimation getHitAnimation(StunType stunType)
    {
        return null;
    }
}
