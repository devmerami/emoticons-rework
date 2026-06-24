package mchorse.emoticons.client;

import mchorse.emoticons.common.EmoteAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public final class WheelMenuController
{
    public static final String[] EMOTE_KEYS = new String[] {
        "yes",
        "no",
        "slow_clap",
        "laughing",
        "thinking",
        "threatening",
        "crying",
        "bow"
    };
    public static final String[] DISPLAY_LABELS = new String[] {
        "\u0414\u0430",
        "\u041d\u0435\u0442",
        "\u041f\u043e\u0445\u043b\u043e\u043f\u0430\u0442\u044c",
        "\u0421\u043c\u0435\u0445",
        "\u0417\u0430\u0434\u0443\u043c\u0430\u0442\u044c\u0441\u044f",
        "\u0423\u0433\u0440\u043e\u0437\u0430",
        "\u0421\u043a\u043e\u0440\u0431\u044c",
        "\u041f\u0440\u0438\u0432\u0435\u0442\u0441\u0442\u0432\u043e\u0432\u0430\u0442\u044c"
    };
    public static final String CENTER_LABEL = "\u0412\u042b\u0411\u0415\u0420\u0418\u0422\u0415 \u042d\u041c\u041e\u0426\u0418\u042e";
    private static final long GLOBAL_COOLDOWN_MS = 5000L;

    private static long globalCooldownEndsAt;
    private static int lastActivatedSegment = -1;

    private WheelMenuController()
    {}

    public static boolean activateSegment(int segmentIndex)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;

        if (player == null || segmentIndex < 0 || segmentIndex >= EMOTE_KEYS.length)
        {
            return false;
        }

        long currentTime = Minecraft.getSystemTime();

        if (globalCooldownEndsAt > currentTime)
        {
            return false;
        }

        globalCooldownEndsAt = currentTime + GLOBAL_COOLDOWN_MS;
        lastActivatedSegment = segmentIndex;
        EmoteAPI.setEmoteClient(EMOTE_KEYS[segmentIndex], player);

        return true;
    }

    public static long getRemainingCooldownMillis()
    {
        return Math.max(0L, globalCooldownEndsAt - Minecraft.getSystemTime());
    }

    public static long getRemainingCooldownSeconds()
    {
        return (long) Math.ceil(getRemainingCooldownMillis() / 1000.0D);
    }

    public static int getLastActivatedSegment()
    {
        return lastActivatedSegment;
    }
}
