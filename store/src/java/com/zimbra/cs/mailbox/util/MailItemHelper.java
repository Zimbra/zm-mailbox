/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.util;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class MailItemHelper {

    public static final String VOL_ID_LOCATOR_SEPARATOR = "@@";

    public static Optional<Short> findMyVolumeId(String locator) {

        if (StringUtils.isNumeric(locator)) {
            return Optional.of(Short.valueOf(locator));
        }

        if (locator.contains(VOL_ID_LOCATOR_SEPARATOR)) {
            String[] parts = locator.split(VOL_ID_LOCATOR_SEPARATOR, 2);
            if (parts.length ==  2 && StringUtils.isNumeric(parts[0])) {
                return Optional.of(Short.valueOf(parts[0]));
            }
        }
        return Optional.empty();
    }
}