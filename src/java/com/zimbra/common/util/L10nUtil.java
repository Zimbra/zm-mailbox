/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.*;

import com.zimbra.common.localconfig.LC;

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
        calendarResourceDefaultReplyPartiallyAccept,
        calendarResourceDefaultReplyPartiallyDecline,
        calendarResourceDefaultReplyTentativelyAccept,
        calendarResourceDefaultReplyDecline,
        calendarResourceDefaultReplyPermissionDenied,

        calendarResourceReplyOriginalInviteSeparatorLabel,

        calendarResourceConflictDateTimeFormat,
        calendarResourceConflictTimeOnlyFormat,
        calendarResourceConflictDateOnlyFormat,

        calendarResourceDeclinedInstances,
        calendarResourceDeclineReasonRecurring,
        calendarResourceDeclineReasonConflict,
        calendarResourceConflictScheduledBy,

        calendarUserReplyPermissionDenied,

        // calendar reminder alerts
        calendarReminderEmailSubject,
        calendarReminderEmailDescription,

        // caldav messages
        caldavCalendarDescription,

        // carddav messages
        carddavAddressbookDescription,

        // share notification
        shareNotifSubject,

        shareNotifBodyIntro,

        shareNotifBodyAddedToGroup1,
        shareNotifBodyAddedToGroup2,

        shareNotifBodyGranteeRoleViewer,
        shareNotifBodyGranteeRoleManager,
        shareNotifBodyGranteeRoleAdmin,

        shareNotifBodySharedItem,
        shareNotifBodyFolderDesc,
        shareNotifBodyOwner,
        shareNotifBodyGrantee,
        shareNotifBodyRole,
        shareNotifBodyAllowedActions,
        shareNotifBodyNotes,

        shareNotifBodyActionRead,
        shareNotifBodyActionWrite,
        shareNotifBodyActionInsert,
        shareNotifBodyActionDelete,
        shareNotifBodyActionAction,
        shareNotifBodyActionAdmin,
        shareNotifBodyActionPrivate,
        shareNotifBodyActionFreebusy,
        shareNotifBodyActionSubfolder,
        //////////////////////

        // read-receipt notification body
        readReceiptNotification,

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

        wikiTOC,
        wikiActions,
        wikiDocName,
        wikiModifiedBy,
        wikiModifiedOn,
        wikiVersion,
        wikiPageHistory,
        wikiBy,

        Notebook,

        errAttachmentDownloadDisabled,
        errInvalidId,
        errInvalidImapId,
        errInvalidPath,
        errInvalidRequest,
        errMailboxNotFound,
        errMessageNotFound,
        errMissingUploadId,
        errMustAuthenticate,
        errNoSuchAccount,
        errNoSuchItem,
        errNoSuchUpload,
        errNotImplemented,
        errPartNotFound,
        errPermissionDenied,
        errUnsupportedFormat

        // add other messages in the future...
    }


    public static final String MSG_FILE_BASENAME = "ZsMsg";
    public static final String L10N_MSG_FILE_BASENAME = "L10nMsg";

    public static final String P_LOCALE_ID = "loc";
    //	public static final String P_FALLBACK_LOCALE_ID = "javax.servlet.jsp.jstl.fmt.fallbackLocale";

    // class loader that loads ZsMsg.properties files from
    // /opt/zimbra/conf/msgs directory
    private static ClassLoader sMsgClassLoader = getClassLoader(LC.localized_msgs_directory.value());

    private static Map<String, Locale> sLocaleMap = new HashMap<String, Locale>();

    private static ClassLoader getClassLoader(String directory) {
        ClassLoader classLoader = null;
        try {
            URL urls[] = new URL[] { new File(directory).toURL() };
            classLoader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            try {
                ZimbraLog.system.fatal("Unable to initialize localization", e);
            } finally {
                Runtime.getRuntime().halt(1);
            }
        }

        return classLoader;
    }

    public static ClassLoader getMsgClassLoader() {
        return sMsgClassLoader;
    } 

    public static String getMessage(MsgKey key, Object... args) {
        return getMessage(key.toString(), (Locale) null, args);
    }

    public static String getMessage(String key, Object... args) {
        return getMessage(key, (Locale) null, args);
    }

    public static String getMessage(MsgKey key, HttpServletRequest request, Object... args) {
        return getMessage(key.toString(), request, args);
    }

    public static String getMessage(String key, HttpServletRequest request, Object... args) {
        Locale locale = null;
        if (request != null) {
            locale = lookupLocale(request.getParameter(P_LOCALE_ID));
        }
        // TODO: If not specified in params, get locale from config
        if (locale == null && request != null) {
            locale = request.getLocale();
        }
        return getMessage(key, locale, args);
    }

    /**
     * Get the message for specified key in given locale, applying variable
     * substitutions with any args.  ({0}, {1}, etc.)
     * @param key message key
     * @param lc locale
     * @param args variable-length argument list for {N} variable substitution
     * @return
     */
    public static String getMessage(MsgKey key, Locale lc, Object... args) {
        return getMessage(key.toString(), lc, args);
    }

    public static String getMessage(String key, Locale lc, Object... args) {
        return getMessage(MSG_FILE_BASENAME, key, lc, args);
    }

    public static String getMessage(String basename, String key, Locale lc, Object... args) {
        ResourceBundle rb;
        try {
            if (lc == null)
                lc = Locale.getDefault();
            rb = ResourceBundle.getBundle(basename, lc, sMsgClassLoader);
            String fmt = rb.getString(key);
            if (fmt != null && args != null && args.length > 0)
                return MessageFormat.format(fmt, args);
            else
                return fmt;
        } catch (MissingResourceException e) {
            ZimbraLog.misc.error("no resource bundle for base name " + basename + " can be found, " + 
                    "(locale=" + key + ")", e);
            return null;
        }
    }

    /** Returns the set of localized server messages for the given keys across
     *  all installed locales on the system. */
    public static Set<String> getMessagesAllLocales(MsgKey... msgkeys) {
        String msgsDir = LC.localized_msgs_directory.value();
        File dir = new File(msgsDir);
        if (!dir.exists()) {
            ZimbraLog.misc.info("message directory does not exist: " + msgsDir);
            return Collections.emptySet();
        } else if (!dir.isDirectory()) {
            ZimbraLog.misc.info("message directory is not a directory: " + msgsDir);
            return Collections.emptySet();
        }

        Set<String> messages = new HashSet<String>();
        for (File file : dir.listFiles(new MatchingPropertiesFilter(new String[] { MSG_FILE_BASENAME }))) {
            Locale locale = getLocaleForPropertiesFile(file, false);
            if (locale != null) {
                for (MsgKey key : msgkeys)
                    messages.add(getMessage(key, locale));
            }
        }
        messages.remove(null);
        return messages;
    }

    static class MatchingPropertiesFilter implements FilenameFilter {
        private final String[] prefixes;

        MatchingPropertiesFilter(Object[] basenames) {
            prefixes = new String[basenames.length * 2];
            int i = 0;
            for (Object basename : basenames) {
                prefixes[i++] = basename + ".";
                prefixes[i++] = basename + "_";
            }
        }

        public boolean accept(File dir, String name) {
            if (!name.endsWith(".properties"))
                return false;
            for (String prefix : prefixes) {
                if (name.startsWith(prefix))
                    return true;
            }
            return false;
        }
    }

    /** Returns the locale corresponding to the given properties file.
     *  Note that a filename like <tt>ZmMsg.properties</tt> will return
     *  <tt>null</tt>, <u>not</u> <tt>en_US</tt>. */
    static Locale getLocaleForPropertiesFile(File file, boolean debug) {
        String[] localeParts = file.getName().split("\\.")[0].split("_");
        if (localeParts.length == 2) {
            if (debug)
                ZimbraLog.misc.debug("        found locale: " + localeParts[1]);
            return new Locale(localeParts[1]);
        } else if (localeParts.length == 3) {
            if (debug)
                ZimbraLog.misc.debug("        found locale: " + localeParts[1] + " " + localeParts[2]);
            return new Locale(localeParts[1], localeParts[2]);
        } else if (localeParts.length == 4) {
            if (debug)
                ZimbraLog.misc.debug("        found locale: " + localeParts[1] + " " + localeParts[2] + " " + localeParts[3]);
            return new Locale(localeParts[1], localeParts[2], localeParts[3]);
        }
        return null;
    }

    /**
     * Lookup a Locale object from locale string specified in
     * language[_country[_variant]] format, e.g. en_US.
     * @param name
     * @return
     */
    public static Locale lookupLocale(String name) {
        Locale lc = null;
        if (name != null) {
            synchronized (sLocaleMap) {
                lc = sLocaleMap.get(name);
                if (lc == null) {
                    String parts[] = name.indexOf('_') != -1 ? name.split("_") : name.split("-");
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

    private static class LocaleComparatorByDisplayName implements Comparator<Locale> {
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
        if (ZimbraLog.misc.isDebugEnabled()) {
            ZimbraLog.misc.debug("L10nUtil: flushing locale cache");
        }
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

        /*
         * load only those supported by JAVA
         */ 
        private static void loadBundlesByJavaLocal(Set<Locale> locales, String msgsDir) {
            ClassLoader classLoader = getClassLoader(msgsDir);
            Locale[] allLocales = Locale.getAvailableLocales();

            for (Locale locale : allLocales) {
                for (ClientResource clientRes : ClientResource.values()) {
                    try {
                        ResourceBundle rb = ResourceBundle.getBundle(clientRes.name(), locale, classLoader);
                        Locale rbLocale = rb.getLocale();
                        if (rbLocale.equals(locale)) {
                            /*
                             * found a resource for the locale, a locale is considered "installed" as long as 
                             * any of its resource (the list in ClientResource) is present
                             */ 
                            ZimbraLog.misc.info("Adding locale " + locale.toString());
                            locales.add(locale);
                            break;
                        }
                    } catch (MissingResourceException e) {
                    }
                }
            }
        }

        /*
         * scan disk
         */
        private static void loadBundlesByDiskScan(Set<Locale> locales, String msgsDir) {
            File dir = new File(msgsDir);
            if (!dir.exists()) {
                ZimbraLog.misc.info("message directory does not exist:" + msgsDir);
                return;
            }
            if (!dir.isDirectory()) {
                ZimbraLog.misc.info("message directory is not a directory:" + msgsDir);
                return;
            }

            for (File file : dir.listFiles(new MatchingPropertiesFilter(ClientResource.values()))) {
                ZimbraLog.misc.debug("loadBundlesByDiskScan processing file: " + file.getName());
                Locale locale = getLocaleForPropertiesFile(file, true);
                if (locale != null && !locales.contains(locale)) {
                    ZimbraLog.misc.info("Adding locale " + locale);
                    locales.add(locale);
                }
            }
        }

        private static void loadBundles() {
            sLocalizedLocales = new HashSet<Locale>();

            // String msgsDir = "/opt/zimbra/jetty/webapps/zimbra/WEB-INF/classes/messages";
            String msgsDir = LC.localized_client_msgs_directory.value();
            ZimbraLog.misc.info("Scanning installed locales from " + msgsDir);

            // the en_US locale is always available
            ZimbraLog.misc.info("Adding locale " + Locale.US.toString() + " (always added)");
            sLocalizedLocales.add(Locale.US);

            // loadBundlesByJavaLocal(sLocalizedLocales, msgsDir);
            loadBundlesByDiskScan(sLocalizedLocales, msgsDir);

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
             *      UI relies on the presence of a "language only" entry 
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
