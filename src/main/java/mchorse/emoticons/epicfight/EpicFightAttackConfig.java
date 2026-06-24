package mchorse.emoticons.epicfight;

import mchorse.emoticons.utils.ResourceLocationUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EpicFightAttackConfig
{
    private static final Map<String, EpicFightWeaponAnimationType> MAPPINGS = new HashMap<String, EpicFightWeaponAnimationType>();
    private static File file;

    public static void init(File configFolder)
    {
        file = new File(configFolder, "epicfight_attack.cfg");
        reload();
    }

    public static void reload()
    {
        if (file == null)
        {
            return;
        }

        Configuration config = new Configuration(file);
        config.addCustomCategoryComment(Configuration.CATEGORY_GENERAL, "Maps item registry names to Epic Fight attack animation types.\n"
                + "Format: modid:item=TYPE\n"
                + "Available types: SWORD, GREATSWORD, SPEAR, AXE\n"
                + "NBT is ignored automatically because matching uses only the item's registry name.");

        String[] entries = config.getStringList("entries", Configuration.CATEGORY_GENERAL, new String[] {
                "minecraft:wooden_sword=SWORD",
                "minecraft:stone_sword=SWORD",
                "minecraft:iron_sword=SWORD",
                "minecraft:golden_sword=SWORD",
                "minecraft:diamond_sword=SWORD",
                "minecraft:wooden_axe=AXE",
                "minecraft:stone_axe=AXE",
                "minecraft:iron_axe=AXE",
                "minecraft:golden_axe=AXE",
                "minecraft:diamond_axe=AXE",
                "minecraft:stick=SPEAR",
                "minecraft:iron_hoe=GREATSWORD",
                "minecraft:diamond_hoe=GREATSWORD"
        }, "Registry-to-animation mappings.");

        MAPPINGS.clear();

        for (String entry : entries)
        {
            if (entry == null)
            {
                continue;
            }

            String value = entry.trim();

            if (value.isEmpty())
            {
                continue;
            }

            int divider = value.indexOf('=');

            if (divider < 0)
            {
                continue;
            }

            String registryName = value.substring(0, divider).trim().toLowerCase(Locale.ROOT);
            EpicFightWeaponAnimationType type = EpicFightWeaponAnimationType.fromString(value.substring(divider + 1));

            if (!registryName.isEmpty() && type != EpicFightWeaponAnimationType.NONE)
            {
                MAPPINGS.put(registryName, type);
            }
        }

        if (config.hasChanged())
        {
            config.save();
        }
    }

    public static EpicFightWeaponAnimationType resolve(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return EpicFightWeaponAnimationType.NONE;
        }

        Item item = stack.getItem();
        ResourceLocation registryName = item.getRegistryName();

        if (registryName != null)
        {
            String fullName = registryName.toString().toLowerCase(Locale.ROOT);
            String path = ResourceLocationUtils.getPath(registryName).toLowerCase(Locale.ROOT);
            EpicFightWeaponAnimationType mapped = MAPPINGS.get(fullName);

            if (mapped != null)
            {
                return mapped;
            }

            if (path.contains("greatsword") || path.contains("great_sword") || path.contains("claymore"))
            {
                return EpicFightWeaponAnimationType.GREATSWORD;
            }

            if (path.contains("spear") || path.contains("lance") || path.contains("pike") || path.contains("halberd"))
            {
                return EpicFightWeaponAnimationType.SPEAR;
            }
        }

        if (item instanceof ItemSword)
        {
            return EpicFightWeaponAnimationType.SWORD;
        }

        if (item instanceof ItemAxe)
        {
            return EpicFightWeaponAnimationType.AXE;
        }

        return EpicFightWeaponAnimationType.NONE;
    }

    public static EpicFightWeaponAnimationType resolve(EntityLivingBase entity, ItemStack stack)
    {
        EpicFightWeaponAnimationType type = resolve(stack);

        return isUsable(entity, type) ? type : EpicFightWeaponAnimationType.NONE;
    }

    public static boolean requiresFreeOffhand(EpicFightWeaponAnimationType type)
    {
        return type == EpicFightWeaponAnimationType.GREATSWORD;
    }

    public static boolean isUsable(EntityLivingBase entity, EpicFightWeaponAnimationType type)
    {
        return !requiresFreeOffhand(type) || entity == null || entity.getHeldItemOffhand().isEmpty();
    }
}
