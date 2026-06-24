package maninthehouse.epicfight.capabilities.entity;

import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.entity.EntityLivingBase;

public class DataKeys
{
    public static final DataParameter<Float> STUN_ARMOR = EntityDataManager.createKey(EntityLivingBase.class, DataSerializers.FLOAT);
}
