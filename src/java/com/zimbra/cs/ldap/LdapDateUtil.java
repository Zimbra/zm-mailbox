/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class LdapDateUtil {
    public static final String ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT_LEGACY = "yyyyMMddHHmmss'Z'";
    public static final String ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT_WITH_MS = "yyyyMMddHHmmss.SSS'Z'";

    private LdapDateUtil() {
    }

    /**
     * to LDAP generalized time string
     */
    public static String toGeneralizedTime(Date date) {
        boolean enabled = false;
        Server server = Provisioning.getInstance().getLocalServerIfDefined();
        enabled = server == null ? false : server.isLdapGentimeFractionalSecondsEnabled();
        if (enabled) {
            return toGeneralizedTimeWithMs(date);
        } else {
            return toGeneralizedTimeLegacyFormat(date);
        }
    }

    public static String toGeneralizedTimeLegacyFormat(Date date) {
        return toGeneralizedTime(date, ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT_LEGACY);
    }

    public static String toGeneralizedTimeWithMs(Date date) {
        return toGeneralizedTime(date, ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT_WITH_MS);
    }

    private static String toGeneralizedTime(Date date, String format) {
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        TimeZone tz = fmt.getCalendar().getTimeZone();
        Date gmtDate;
        if (tz.inDaylightTime(date)) {
            gmtDate = new Date(date.getTime() - (tz.getRawOffset() + tz.getDSTSavings()));
        } else {
            gmtDate = new Date(date.getTime() - tz.getRawOffset());
        }
        return fmt.format(gmtDate);
    }

    /**
     * from LDAP generalized time string
     */
    public static Date parseGeneralizedTime(String time) {
        return DateUtil.parseGeneralizedTime(time);
    }
}
