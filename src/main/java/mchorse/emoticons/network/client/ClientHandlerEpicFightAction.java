package mchorse.emoticons.network.client;

import mchorse.emoticons.capabilities.cosmetic.Cosmetic;
import mchorse.emoticons.network.common.PacketEpicFightAction;
import mchorse.mclib.network.ClientMessageHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientHandlerEpicFightAction extends ClientMessageHandler<PacketEpicFightAction>
{
    @Override
    @SideOnly(Side.CLIENT)
    public void run(EntityPlayerSP player, PacketEpicFightAction message)
    {
        Entity entity = player.world.getEntityByID(message.id);

        if (!(entity instanceof EntityLivingBase) || entity == player)
        {
            return;
        }

        Cosmetic cap = (Cosmetic) Cosmetic.get(entity);

        if (cap == null)
        {
            return;
        }

        if (message.action == PacketEpicFightAction.ROLL_FORWARD)
        {
            cap.playRollFromServer((EntityLivingBase) entity, true);
        }
        else if (message.action == PacketEpicFightAction.ROLL_BACKWARD)
        {
            cap.playRollFromServer((EntityLivingBase) entity, false);
        }
    }
}
