package mchorse.emoticons.utils;

import net.minecraft.util.ResourceLocation;

public final class ResourceLocationUtils
{
    private ResourceLocationUtils()
    {}

    public static String getDomain(ResourceLocation location)
    {
        String value = stringify(location);
        int separator = value.indexOf(':');

        return separator >= 0 ? value.substring(0, separator) : "minecraft";
    }

    public static String getPath(ResourceLocation location)
    {
        String value = stringify(location);
        int separator = value.indexOf(':');

        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    public static String getAssetPath(ResourceLocation location)
    {
        return "/assets/" + getDomain(location) + "/" + getPath(location);
    }

    private static String stringify(ResourceLocation location)
    {
        return location == null ? "" : location.toString();
    }
}
