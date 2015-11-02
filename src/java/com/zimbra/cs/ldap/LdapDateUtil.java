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
