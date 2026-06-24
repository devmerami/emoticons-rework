package mchorse.emoticons.network.common;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketEpicFightAction implements IMessage
{
    public static final byte ROLL_FORWARD = 1;
    public static final byte ROLL_BACKWARD = 2;

    public int id;
    public byte action;

    public PacketEpicFightAction()
    {}

    public PacketEpicFightAction(int id, byte action)
    {
        this.id = id;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.id = buf.readInt();
        this.action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.id);
        buf.writeByte(this.action);
    }
}
