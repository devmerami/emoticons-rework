package maninthehouse.epicfight.capabilities.item;

import maninthehouse.epicfight.physics.Collider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

public class CapabilityItem
{
    public SoundEvent getHitSound()
    {
        return null;
    }

    public SoundEvent getSmashingSound()
    {
        return null;
    }

    public Collider getWeaponCollider()
    {
        return null;
    }

    public boolean canUseOnMount()
    {
        return true;
    }

    public boolean canBeRenderedBoth(ItemStack item)
    {
        return true;
    }
}
