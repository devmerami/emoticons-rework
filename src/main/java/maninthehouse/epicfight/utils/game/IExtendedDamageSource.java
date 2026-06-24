package maninthehouse.epicfight.utils.game;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EntityDamageSource;

public class IExtendedDamageSource extends EntityDamageSource
{
    public enum DamageType
    {
        PHYSICAL
    }

    public enum StunType
    {
        SHORT,
        LONG,
        HOLD
    }

    private float impact;

    private IExtendedDamageSource(String damageType, Entity source)
    {
        super(damageType, source);
    }

    public static IExtendedDamageSource causeMobDamage(EntityLivingBase entity, StunType stunType, DamageType damageType, int animationId)
    {
        return new IExtendedDamageSource("emoticons.attack", entity);
    }

    public void setArmorIgnore(float armorIgnore)
    {}

    public void setImpact(float impact)
    {
        this.impact = impact;
    }

    public float getImpact()
    {
        return this.impact;
    }
}
