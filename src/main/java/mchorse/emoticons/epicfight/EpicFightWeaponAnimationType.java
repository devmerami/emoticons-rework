package mchorse.emoticons.epicfight;

public enum EpicFightWeaponAnimationType
{
    NONE,
    SWORD,
    GREATSWORD,
    SPEAR,
    AXE;

    public static EpicFightWeaponAnimationType fromString(String value)
    {
        if (value == null)
        {
            return NONE;
        }

        try
        {
            return EpicFightWeaponAnimationType.valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            return NONE;
        }
    }
}
