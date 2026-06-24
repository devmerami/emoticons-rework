package mchorse.emoticons.network.server;

import mchorse.emoticons.capabilities.cosmetic.Cosmetic;
import mchorse.emoticons.epicfight.EpicFightAnimationSelector;
import mchorse.emoticons.network.Dispatcher;
import mchorse.emoticons.network.common.PacketEpicFightAction;
import mchorse.mclib.network.ServerMessageHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;

public class ServerHandlerEpicFightAction extends ServerMessageHandler<PacketEpicFightAction>
{
    private static final double FORWARD_ROLL_IMPULSE = 1.35D;
    private static final double BACKWARD_ROLL_IMPULSE = -0.95D;

    @Override
    public void run(EntityPlayerMP player, PacketEpicFightAction message)
    {
        Cosmetic cap = (Cosmetic) Cosmetic.get(player);

        if (cap == null || cap.getEmote() != null || cap.isActionLocked() || cap.isRollOnCooldown())
        {
            return;
        }

        boolean forward;

        if (message.action == PacketEpicFightAction.ROLL_FORWARD)
        {
            forward = true;
        }
        else if (message.action == PacketEpicFightAction.ROLL_BACKWARD)
        {
            forward = false;
        }
        else
        {
            return;
        }

        if (!player.onGround || player.isRiding())
        {
            return;
        }

        int lockTicks = EpicFightAnimationSelector.getRollTicks(forward);

        if (lockTicks <= 0)
        {
            return;
        }

        this.applyRollImpulse(player, forward);
        cap.startRollLock(lockTicks);

        PacketEpicFightAction packet = new PacketEpicFightAction(player.getEntityId(), message.action);

        Dispatcher.sendTo(packet, player);
        Dispatcher.sendToTracked(player, packet);
    }

    private void applyRollImpulse(EntityPlayerMP player, boolean forward)
    {
        Vec3d look = player.getLookVec();
        Vec3d horizontal = new Vec3d(look.x, 0.0D, look.z);

        if (horizontal.lengthSquared() < 1.0E-4D)
        {
            return;
        }

        player.setSprinting(false);
        player.motionX = 0.0D;
        player.motionZ = 0.0D;
        horizontal = horizontal.normalize().scale(forward ? FORWARD_ROLL_IMPULSE : BACKWARD_ROLL_IMPULSE);
        player.motionX += horizontal.x;
        player.motionZ += horizontal.z;
        player.velocityChanged = true;
    }
}
