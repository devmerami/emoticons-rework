package maninthehouse.epicfight.capabilities.entity;

import maninthehouse.epicfight.animation.types.StaticAnimation;
import maninthehouse.epicfight.utils.game.IExtendedDamageSource.StunType;
import net.minecraft.entity.EntityLivingBase;

public abstract class MobData<T extends EntityLivingBase> extends LivingData<T>
{
    @Override
    public StaticAnimation getHitAnimation(StunType stunType)
    {
        return null;
    }
}
