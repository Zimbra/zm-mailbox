package com.zimbra.cs.ldap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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
        //first 14 are mandatory. the rest are optional.
        if (time.length() < 14) {
            return null;
        }
        TimeZone tz;
        boolean trailingZ = false;
        if (time.endsWith("Z")) {
            trailingZ = true;
            tz = TimeZone.getTimeZone("GMT");
        } else {
            tz = TimeZone.getDefault();
        }
        int year = Integer.parseInt(time.substring(0, 4));
        int month = Integer.parseInt(time.substring(4, 6)) - 1;  // months are 0 base
        int date = Integer.parseInt(time.substring(6, 8));
        int hour = Integer.parseInt(time.substring(8, 10));
        int min = Integer.parseInt(time.substring(10, 12));
        int sec = Integer.parseInt(time.substring(12, 14));
        Calendar calendar = new GregorianCalendar(tz);
        calendar.clear();
        calendar.set(year, month, date, hour, min, sec);
        if (time.length() >= 16 + trailLen(trailingZ) && time.charAt(14) == '.') {

            int fractionLen = time.length() - 15 - trailLen(trailingZ);
            if (fractionLen > 3) {
                //java Date object is only millisecond precision; drop the micros if present
                fractionLen = 3;
            }
            assert(fractionLen > 0);
            int fractionRaw = Integer.parseInt(time.substring(15, 15 + fractionLen));
            int factor = 1;
            for (int i = fractionLen; i < 3; i++) {
                factor *= 10;
            }
            int millis = fractionRaw * factor;
            calendar.set(Calendar.MILLISECOND, millis);
        }
        return calendar.getTime();
    }

    private static int trailLen(boolean trailingChar) {
        return trailingChar ? 1 : 0;
    }

}
