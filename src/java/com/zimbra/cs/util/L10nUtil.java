/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.zimbra.cs.localconfig.LC;

public class L10nUtil {

    /**
     * List all message keys here
     */
    public static enum MsgKey {

        // calendar messages

        calendarSubjectCancelled,
        calendarCancelRemovedFromAttendeeList,
        calendarCancelAppointment,
        calendarCancelAppointmentInstance,
        calendarCancelAppointmentInstanceWhich,

        calendarReplySubjectAccept,
        calendarReplySubjectTentative,
        calendarReplySubjectDecline,

        calendarDefaultReplyAccept,
        calendarDefaultReplyTentativelyAccept,
        calendarDefaultReplyDecline,
        calendarDefaultReplyOther,
        calendarResourceDefaultReplyAccept,
        calendarResourceDefaultReplyTentativelyAccept,
        calendarResourceDefaultReplyDecline,

        calendarResourceReplyOriginalInviteSeparatorLabel,

        calendarResourceConflictDateTimeFormat,
        calendarResourceConflictTimeOnlyFormat,

        calendarResourceDeclineReasonRecurring,
        calendarResourceDeclineReasonConflict,
        calendarResourceConflictScheduledBy,

        // add other messages in the future...
    }


    private static final String MSG_FILE_BASENAME = "ZsMsg";

    // class loader that loads ZsMsg.properties files from
    // /opt/zimbra/conf/msgs directory
    private static ClassLoader sMsgClassLoader;

    private static Map<String, Locale> sLocaleMap;

    static {
        try {
            String msgsDir = LC.localized_msgs_directory.value();
            // Append "/" at the end to tell URLClassLoader this is a
            // directory rather than a JAR file.
            if (!msgsDir.endsWith("/"))
                msgsDir = msgsDir + "/";
            URL urls[] = new URL[1];
            urls[0] = new URL("file://" + msgsDir);
            sMsgClassLoader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            Zimbra.halt("Unable to initialize localization", e);
        }

        Locale[] locales = Locale.getAvailableLocales();
        sLocaleMap = new HashMap<String, Locale>(locales.length);
        for (Locale lc : locales) {
            sLocaleMap.put(lc.toString(), lc);
        }
    }

    /**
     * Get the message for specified key in given locale, applying variable
     * substitutions with any args.  ({0}, {1}, etc.)
     * @param key message key
     * @param lc locale
     * @param args variable-length argument list for {N} variable substitution
     * @return
     */
    public static String getMessage(MsgKey key,
                                    Locale lc,
                                    Object... args) {
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle(MSG_FILE_BASENAME, lc, sMsgClassLoader);
            String fmt = rb.getString(key.toString());
            if (fmt != null && args != null && args.length > 0)
                return MessageFormat.format(fmt, args);
            else
                return fmt;
        } catch (MissingResourceException e) {
            ZimbraLog.misc.error("Resource bundle \"" + MSG_FILE_BASENAME +
                                 "\" not found (locale=" + lc.toString() + ")",
                                 e);
            return null;
        }
    }

    /**
     * Lookup a Locale object from locale string specified in
     * language[_country[_variant]] format, e.g. en_US.
     * @param locale
     * @return
     */
    public static Locale lookupLocale(String name) {
        Locale lc = null;
        if (name != null) {
            synchronized (sLocaleMap) {
                lc = sLocaleMap.get(name);
                if (lc == null) {
                    String parts[] = name.split("_");
                    if (parts.length == 1)
                        lc = new Locale(parts[0]);
                    else if (parts.length == 2)
                        lc = new Locale(parts[0], parts[1]);
                    else if (parts.length >= 3)
                        lc = new Locale(parts[0], parts[1], parts[2]);
                    if (lc != null)
                        sLocaleMap.put(name, lc);
                }
            }
        }
        return lc;
    }

    private static class LocaleComparatorByDisplayName
    implements Comparator<Locale> {
        public int compare(Locale a, Locale b) {
            String da = a.getDisplayName(Locale.US);
            String db = b.getDisplayName(Locale.US);
            return da.compareTo(db);
        }
    }

    /**
     * Return all known locales sorted by their US English display name.
     * @return
     */
    public static Locale[] getAllLocalesSorted() {
        Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, new LocaleComparatorByDisplayName());
        return locales;
    }
}
