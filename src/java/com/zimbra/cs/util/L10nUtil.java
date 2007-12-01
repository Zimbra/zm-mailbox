/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;

public class L10nUtil {

    /**
     * List all message keys here
     */
    public static enum MsgKey {

        // calendar messages

        calendarSubjectCancelled,
        calendarSubjectWithheld,
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

        // caldav messages
        caldavCalendarDescription,
        
        // ZimbraSync client invitation text
        zsApptNew,
        zsApptModified,
        zsApptInstanceModified,

        zsSubject,
        zsOrganizer,
        zsLocation,
        zsTime,
        zsStart,
        zsEnd,
        zsAllDay,
        zsRecurrence,
        zsInvitees,

        zsRecurDailyEveryDay,
        zsRecurDailyEveryWeekday,
        zsRecurDailyEveryNumDays,
        zsRecurWeeklyEveryWeekday,
        zsRecurWeeklyEveryNumWeeksDate,
        zsRecurMonthlyEveryNumMonthsDate,
        zsRecurMonthlyEveryNumMonthsNumDay,
        zsRecurYearlyEveryDate,
        zsRecurYearlyEveryMonthNumDay,
        zsRecurStart,
        zsRecurEndNone,
        zsRecurEndNumber,
        zsRecurEndByDate,
        zsRecurBlurb,
        
        // add other messages in the future...
    }


    private static final String MSG_FILE_BASENAME = "ZsMsg";

    // class loader that loads ZsMsg.properties files from
    // /opt/zimbra/conf/msgs directory
    private static ClassLoader sMsgClassLoader;
    public static ClassLoader getMsgClassLoader() { return sMsgClassLoader; } 

    private static Map<String, Locale> sLocaleMap;

    private static ClassLoader getClassLoader(String directory) {
        ClassLoader classLoader = null;
        try {
            String msgsDir = directory;
            // Append "/" at the end to tell URLClassLoader this is a
            // directory rather than a JAR file.
            if (!msgsDir.endsWith("/"))
                msgsDir = msgsDir + "/";
            URL urls[] = new URL[1];
            urls[0] = new URL("file://" + msgsDir);
            classLoader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            Zimbra.halt("Unable to initialize localization", e);
        }
        
        return classLoader;
    }
    
    static {
        String msgsDir = LC.localized_msgs_directory.value();
        sMsgClassLoader = getClassLoader(msgsDir);
        
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
        private Locale mInLocale;
        LocaleComparatorByDisplayName(Locale inLocale) {
            mInLocale = inLocale;
        }
        
        public int compare(Locale a, Locale b) {
            String da = a.getDisplayName(mInLocale);
            String db = b.getDisplayName(mInLocale);
            return da.compareTo(db);
        }
    }

    /**
     * Return all known locales sorted by their US English display name.
     * @return
     */
    public static Locale[] getAllLocalesSorted() {
        Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, new LocaleComparatorByDisplayName(Locale.US));
        return locales;
    }
    
    /**
     * Return all localized(i.e. translated) locales sorted by their inLocale display name 
     * @return
     */
    public static Locale[] getLocalesSorted(Locale inLocale) {
        return LocalizedClientLocales.getLocales(inLocale);
    }
    
    public static void flushLocaleCache() {
        LocalizedClientLocales.flushCache();
    }
    
    private static class LocalizedClientLocales {
        enum ClientResource {
            // I18nMsg,  // generated, all locales are there, so we don't count this resource
            AjxMsg, 
            ZMsg, 
            ZaMsg, 
            ZhMsg,
            ZmMsg
        }

        
        // set of localized(translated) locales
        static Set<Locale> sLocalizedLocales = null;
        
        // we cache the sorted list per display locale to avoid the array copy
        // and sorting each time for a GetLocale request
        static Map<Locale, Locale[]> sLocalizedLocalesSorted = null;
        
        private static void loadBundles() {
            sLocalizedLocales = new HashSet<Locale>();
            
            // String msgsDir = "/opt/zimbra/jetty/webapps/zimbra/WEB-INF/classes/messages";
            String msgsDir = LC.localized_client_msgs_directory.value();
            ZimbraLog.misc.info("Scanning installed locales from " + msgsDir);
            ClassLoader classLoader = getClassLoader(msgsDir);
            Locale[] allLocales = Locale.getAvailableLocales();
            
            // the en_US locale is always available
            ZimbraLog.misc.info("Adding locale " + Locale.US.toString() + " (always added)");
            sLocalizedLocales.add(Locale.US);
            
            for (Locale lc : allLocales) {
                for (ClientResource clientRes : ClientResource.values()) {
                    try {
                        ResourceBundle rb = ResourceBundle.getBundle(clientRes.name(), lc, classLoader);
                        Locale rbLocale = rb.getLocale();
                        if (rbLocale.equals(lc)) {
                            /*
                             * found a resource for the locale, a locale is considered "installed" as long as 
                             * any of its resource (the list in ClientResource) is present
                             */ 
                            ZimbraLog.misc.info("Adding locale " + lc.toString());
                            sLocalizedLocales.add(lc);
                            break;
                        }
                    } catch (MissingResourceException e) {
                    }
                }
            }
            
            /*
             * UI displays locales with country in sub menus. 
             * 
             * E.g. if there are:
             *      id: "zh_CN", name: "Chinese (China)"
             *      id: "zh_HK", name: "Chinese (Hong Kong)"
             *
             *      then the menu looks like:
             *          Chinese
             *                   Chinese (China)
             *                   Chinese (Hong Kong)
             *
             *      The UI relies on the presence of a "language only" entry 
             *      for the top level label "Chinese".    
             *      i.e. id: "zh", name: "Chinese"
             *          
             *      Thus we need to add a "language only" pseudo entry for locales that have 
             *      a country part but the "language only" entry is not already there.
             */
            Set<Locale> pseudoLocales = new HashSet<Locale>();
            for (Locale lc : sLocalizedLocales) {
                String language = lc.getLanguage();
                Locale lcLang = new Locale(language);
                if (!sLocalizedLocales.contains(lcLang) && !pseudoLocales.contains(lcLang)) {
                    ZimbraLog.misc.info("Adding locale " + lcLang.toString() + " (pseudo)");
                    pseudoLocales.add(lcLang);
                }
            }
            if (pseudoLocales.size() > 0) {
                sLocalizedLocales = SetUtil.union(sLocalizedLocales, pseudoLocales);
            }
        }
        
        public synchronized static Locale[] getLocales(Locale inLocale) {
            if (sLocalizedLocales == null)
                loadBundles();
            
            Locale[] sortedLocales = null;
            if (sLocalizedLocalesSorted == null)
                sLocalizedLocalesSorted = new HashMap<Locale, Locale[]>();
            else
                sortedLocales = sLocalizedLocalesSorted.get(inLocale);
            
            if (sortedLocales == null) {
                // cache the sorted list per display locale
                sortedLocales = sLocalizedLocales.toArray(new Locale[sLocalizedLocales.size()]);
                Arrays.sort(sortedLocales, new LocaleComparatorByDisplayName(inLocale));
                sLocalizedLocalesSorted.put(inLocale, sortedLocales);
            }
            return sortedLocales;
        }
        
        public synchronized static void flushCache() {
            sLocalizedLocales = null;
            sLocalizedLocalesSorted = null;
        }
    }
}
