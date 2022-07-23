package com.zimbra.cs.mailbox.util;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class MailItemHelper {

    public static final String VOL_ID_LOCATOR_SEPARATOR = "@@";

    public static Optional<Short> findMyVolumeId(String locator) {

        if (StringUtils.isNumeric(locator)) {
            return Optional.of(Short.valueOf(locator));
        }

        if(locator.contains(VOL_ID_LOCATOR_SEPARATOR)) {
            String[] parts = locator.split(VOL_ID_LOCATOR_SEPARATOR, 2);
            if (parts.length ==  2 && StringUtils.isNumeric(parts[0])) {
                return Optional.of(Short.valueOf(parts[0]));
            }
        }
        return Optional.empty();
    }
}
