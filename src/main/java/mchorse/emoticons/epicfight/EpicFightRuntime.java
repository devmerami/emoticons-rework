package mchorse.emoticons.epicfight;

import maninthehouse.epicfight.client.model.ClientModels;
import maninthehouse.epicfight.gamedata.Animations;
import maninthehouse.epicfight.gamedata.Models;
import net.minecraftforge.fml.relauncher.Side;

public class EpicFightRuntime
{
    private static boolean serverInitialized;
    private static boolean clientInitialized;
    private static boolean failed;

    public static void initServer()
    {
        if (serverInitialized || failed)
        {
            return;
        }

        try
        {
            Models.LOGICAL_SERVER.buildArmatureData();
            Animations.registerAnimations(Side.SERVER);
            serverInitialized = true;
        }
        catch (Exception e)
        {
            failed = true;
            e.printStackTrace();
        }
    }

    public static void initClient()
    {
        if (clientInitialized || failed)
        {
            return;
        }

        try
        {
            initServer();

            if (failed)
            {
                return;
            }

            ClientModels.LOGICAL_CLIENT.buildArmatureData();
            ClientModels.LOGICAL_CLIENT.buildMeshData();
            Animations.registerAnimations(Side.CLIENT);
            clientInitialized = true;
        }
        catch (Exception e)
        {
            failed = true;
            e.printStackTrace();
        }
    }

    public static boolean isReady()
    {
        return clientInitialized && !failed;
    }
}
