package com.zimbra.cs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * A helper class for converting human readable size strings to bytes.
 *
 * Format : Number followed by a unit designation.
 * Unit designation:
 * B - bytes
 * KB - kilobytes
 * MB - megabytes
 * GB - gigabytes
 * TB - terabytes
 *
 * Uses a 1024 bytes per kilobyte graduation.
 */
public class MemoryUnitUtil {

	// Matches the format used by EHCache 2.5.1
    private static Pattern EHCACHE_251 = Pattern.compile("^(\\d+)(b?k?m?B?K?M?)$", Pattern.CASE_INSENSITIVE);

    public static Pattern MEMORYUNIT_PATTERN = Pattern.compile("^(\\d+)(B?[KB]?[MB]?[GB]?[TB]?)$", Pattern.CASE_INSENSITIVE);

    private int MULTIPLIER = 1024;

    public MemoryUnitUtil()
    {

    }

    public MemoryUnitUtil(int multiplier)
    {
        setMULTIPLIER(multiplier);
    }

    public static boolean isMemoryUnit(String value)
    {
        return StringUtils.isNotEmpty(value) && MEMORYUNIT_PATTERN.matcher(value).matches();
    }

    protected int computeMultiplier(String suffix)
    {
        int multiplier = 1;

        switch (suffix)
        {
        case "b":
        case "B":
            break;
        case "k":
        case "KB":
            multiplier = MULTIPLIER;
            break;
        case "m":
        case "MB":
            multiplier = MULTIPLIER * MULTIPLIER;
            break;
        case "GB":
            multiplier = MULTIPLIER * MULTIPLIER * MULTIPLIER;
            break;
        case "TB":
            multiplier = MULTIPLIER * MULTIPLIER * MULTIPLIER * MULTIPLIER;
            break;
        }

        return multiplier;
    }

    public long convertToBytes(String value)
    {
        Matcher matcher = EHCACHE_251.matcher(value);
        if (matcher.matches())
        {
            return Long.parseLong(matcher.group(1)) * computeMultiplier(matcher.group(2));
        }
        matcher = MEMORYUNIT_PATTERN.matcher(value);
        if (matcher.matches())
        {
            return Long.parseLong(matcher.group(1)) * computeMultiplier(matcher.group(2));
        }
        else
            return Long.parseLong(value);
    }

    public long getMULTIPLIER() {
        return MULTIPLIER;
    }

    public void setMULTIPLIER(int mULTIPLIER) {
        MULTIPLIER = mULTIPLIER;
    }
}
