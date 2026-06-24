package mchorse.emoticons.capabilities;

import mchorse.emoticons.Emoticons;
import mchorse.emoticons.capabilities.cosmetic.Cosmetic;
import mchorse.emoticons.capabilities.cosmetic.CosmeticProvider;
import mchorse.emoticons.capabilities.cosmetic.ICosmetic;
import mchorse.emoticons.epicfight.EpicFightAttackConfig;
import mchorse.emoticons.epicfight.EpicFightWeaponAnimationType;
import mchorse.emoticons.network.Dispatcher;
import mchorse.emoticons.network.common.PacketGameMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

/**
 * Capability handler class
 */
public class CapabilitiesHandler
{
    /**
     * Resource location for cosmetic capability 
     */
    public static final ResourceLocation COSMETIC = new ResourceLocation(Emoticons.MOD_ID, "cosmetic");

    /**
     * Attach player capabilities
     */
    @SubscribeEvent
    public void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof EntityPlayer)
        {
            event.addCapability(COSMETIC, new CosmeticProvider());
        }
    }

    @SubscribeEvent
    public void onUpdateEntity(PlayerTickEvent event)
    {
        if (event.phase == Phase.START)
        {
            return;
        }

        EntityPlayer entity = event.player;
        ICosmetic cap = Cosmetic.get(entity);

        if (cap != null)
        {
            cap.update(entity);

            if (cap instanceof Cosmetic)
            {
                this.freezeAttackMovement(entity, (Cosmetic) cap);
            }
        }

        this.enforceGreatswordOffhandRule(entity);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event)
    {
        if (event.getEntityPlayer().world.isRemote)
        {
            return;
        }

        Cosmetic cap = (Cosmetic) Cosmetic.get(event.getEntityPlayer());

        if (cap == null)
        {
            return;
        }

        if (!cap.isActionLocked())
        {
            cap.beginAttackLock(event.getEntityPlayer());
        }

        if (cap.consumeAllowedHit())
        {
            return;
        }

        if (cap.isActionLocked())
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        EntityLivingBase entity = event.getEntityLiving();

        if (entity.world.isRemote || !(entity instanceof EntityPlayer))
        {
            return;
        }

        Cosmetic cap = (Cosmetic) Cosmetic.get(entity);

        if (cap != null && cap.isRollInvulnerable())
        {
            event.setCanceled(true);
        }
    }

    /**
     * When player logs on the server, request 
     */
    @SubscribeEvent
    public void playerLogsIn(PlayerLoggedInEvent event)
    {
        Dispatcher.sendTo(new PacketGameMode(), (EntityPlayerMP) event.player);
    }

    private void enforceGreatswordOffhandRule(EntityPlayer player)
    {
        if (player.world.isRemote)
        {
            return;
        }

        ItemStack offhandStack = player.getHeldItemOffhand();

        if (!EpicFightAttackConfig.requiresFreeOffhand(EpicFightAttackConfig.resolve(offhandStack)))
        {
            return;
        }

        ItemStack extracted = offhandStack.copy();

        player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);

        if (!player.inventory.addItemStackToInventory(extracted))
        {
            player.dropItem(extracted, false);
        }

        if (player instanceof EntityPlayerMP)
        {
            ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
        }
    }

    private void freezeAttackMovement(EntityPlayer player, Cosmetic cap)
    {
        if (!cap.isAttackMovementLocked())
        {
            return;
        }

        player.setSprinting(false);
        player.motionX = 0.0D;
        player.motionZ = 0.0D;
        player.moveForward = 0.0F;
        player.moveStrafing = 0.0F;
    }
}
