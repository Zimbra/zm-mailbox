/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2023 Synacor, Inc.
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

package com.zimbra.common.util;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class L10nUtil {

    /**
     * List all message keys here
     */
    public static enum MsgKey {

        replySubjectPrefix,

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

        // Calendar Forward Notifications
        calendarForwardNotificationSubject,
        calendarForwardNotificationBody,
        calendarForwardNotificationBodyHtml,

        // calendar item reminder alerts
        apptReminderEmailSubject,
        apptReminderEmailBody,
        apptReminderEmailBodyHtml,
        taskReminderEmailSubject,
        taskReminderEmailBody,
        taskReminderEmailBodyHtml,

        noLocation,
        apptReminderSmsText,
        taskReminderSmsText,
        deviceSendVerificationCodeText,

        // caldav messages
        caldavCalendarDescription,

        // carddav messages
        carddavAddressbookDescription,

        // default fragment for encrypted mail
        encryptedMessageFragment,

        // share notification
        mail,
        calendar,
        task,
        addressBook,
        briefcase,

        shareNotifSubject,
        sharedBySubject,
        shareNotifBodyText,
        shareNotifBodyHtml,

        shareModifySubject,
        shareModifyBodyText,
        shareModifyBodyHtml,

        shareRevokeSubject,
        shareRevokeBodyText,
        shareRevokeBodyHtml,

        shareExpireSubject,
        shareExpireBodyText,
        shareExpireBodyHtml,

        shareNotifBodyAddedToGroup1,
        shareNotifBodyAddedToGroup2,

        shareNameDefault,

        shareNotifBodyGranteeRoleViewer,
        shareNotifBodyGranteeRoleManager,
        shareNotifBodyGranteeRoleAdmin,
        shareNotifBodyGranteeRoleNone,
        shareNotifBodyGranteeRoleCustom,

        shareNotifBodyFolderDesc,
        shareNotifyFileBodyDesc,
        shareNotifBodyExternalShareText,
        shareNotifBodyExternalShareHtml,
        shareNotifBodyNotesText,
        shareNotifBodyNotesHtml,

        shareNotifBodyActionRead,
        shareNotifBodyActionWrite,
        shareNotifBodyActionInsert,
        shareNotifBodyActionDelete,
        shareNotifBodyActionAction,
        shareNotifBodyActionAdmin,
        shareNotifBodyActionPrivate,
        shareNotifBodyActionFreebusy,
        shareNotifBodyActionSubfolder,
        shareNotifBodyActionNone,
        //////////////////////

        // group subscription request
        dlSubscriptionRequestSubject,
        dlSubscribeRequestText,
        dlUnsubscribeRequestText,

        // group subscription response
        dlSubscriptionResponseSubject,
        dlSubscribeResponseAcceptedText,
        dlSubscribeResponseRejectedText,
        dlUnsubscribeResponseAcceptedText,
        dlUnsubscribeResponseRejectedText,

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
        errUnsupportedFormat,
        errResourceNotAllowedOnPort,
        errMissingBlob,

        passwordViolation,

        domainAggrQuotaWarnMsgSubject,
        domainAggrQuotaWarnMsgBody,

        // mobile notification
        mobile_notification_skip_item_subject,
        mobile_notification_skip_item_message,
        mobile_notification_skip_item_reason_cannot_permit,
        mobile_notification_skip_item_reason_choke_device,
        mobile_notification_skip_item_reason_other,
        mobile_notification_skip_item_attachment_name,

        mobile_partial_failure_report_subject,
        mobile_partial_failure_report_message,
        mobile_send_failure_report_subject,
        mobile_send_failure_report_message,

        //TODO remove octopus
        octopus_share_notification_email_subject,
        octopus_share_notification_email_message,
        octopus_share_notification_email_accept,
        octopus_share_notification_email_ignore,
        octopus_share_notification_email_bodyFolderDesc,

        //Spnego 401 error page
        spnego_401_error_message,
        spnego_redirect_message,
        spnego_browser_setup_message,
        spnego_browser_setup_wiki,

        errorTitle,
        zipFile,

        //sieve
        seiveRejectMDNSubject,
        seiveRejectMDNErrorMsg,

        //forwarding email address verification
        verifyEmailSubject,
        verifyEmailBodyText,
        verifyEmailBodyHtml,

        // recovery email address verification
        verifyRecoveryEmailSubject,
        verifyRecoveryEmailBodyText,
        verifyRecoveryEmailBodyHtml,
        sendPasswordRecoveryEmailSubject,
        sendPasswordRecoveryEmailBodyText,
        sendPasswordRecoveryEmailBodyHtml,

        //send password reset email for new users
        sendPasswordResetEmailSubject,
        sendPasswordResetEmailBodyText,
        sendPasswordResetEmailBodyHtml,

        // send mobile data management notification emails
        sendMDMNotificationEmailHtmlOpen,
        sendMDMNotificationEmailHtmlClose,
        sendMDMNotificationEmailBreakLine,
        sendMDMNotificationEmailSuccess,
        sendMDMNotificationEmailFailure,
        sendMDMNotificationEmailHtmlTableRowOpen,
        sendMDMNotificationEmailHtmlTableRowClose,
        sendMDMNotificationEmailHtmlTableDataOpen,
        sendMDMNotificationEmailHtmlTableDataClose,
        sendMDMNotificationEmailHtmlTableOpen,
        sendMDMNotificationEmailHtmlTableClose,
        sendMDMNotificationEmailHtmlDeviceTableHeader,
        sendMDMNotificationEmailNoDevicesFoundMsg,

        // two-factor email address verification
        twoFactorAuthEmailSubject,
        twoFactorAuthEmailBodyText,
        twoFactorAuthEmailBodyHtml,

        // send Two Factor Auth Code for email method
        twoFactorAuthCodeEmailSubject,
        twoFactorAuthCodeEmailBodyText,
        twoFactorAuthCodeEmailBodyHtml

        // add other messages in the future...
    }

    public static final String MSG_FILE_BASENAME = "ZsMsg";
    public static final String L10N_MSG_FILE_BASENAME = "L10nMsg";
    public static final String MSG_RIGHTS_FILE_BASENAME = "ZsMsgRights";

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

    @VisibleForTesting
    public static void setMsgClassLoader(String directory) {
        sMsgClassLoader = getClassLoader(directory);
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
        return getMessage(true /* shoutIfMissing */, basename, key, lc, args);
    }

    public static String getMessage(boolean shoutIfMissing, String basename, String key, Locale lc, Object... args) {
        ResourceBundle rb = null;
        try {
            if (lc == null) {
                lc = Locale.getDefault();
            }
            rb = ResourceBundle.getBundle(basename, lc, sMsgClassLoader);
            String fmt = rb.getString(key);
            if (fmt != null && args != null && args.length > 0) {
                return MessageFormat.format(fmt, args);
            } else {
                return fmt;
            }
        } catch (MissingResourceException e) {
            if (rb == null) {
                if (shoutIfMissing) {
                    ZimbraLog.misc.warn("no resource bundle found for (basename='%s', key='%s' locale='%s')",
                            basename, key, lc, e);
                } else {
                    ZimbraLog.misc.info("no resource bundle found for (basename='%s', key='%s' locale='%s')",
                            basename, key, lc);
                }
            } else {
                if (shoutIfMissing) {
                    ZimbraLog.misc.warn("no resource string found in bundle for (basename='%s', key='%s' locale='%s')",
                            basename, key, lc, e);
                } else {
                    ZimbraLog.misc.info("no resource string found in bundle for (basename='%s', key='%s' locale='%s')",
                            basename, key, lc);
                }
            }
            return null;
        }
    }

    /**
     * Shortned version of the getBundleKeySet method that uses the default message bundle as the base name
     * @param lc The locale, if null it will use the default locale
     * @return A set of keys if any found, will not return null
     */
    public static Set<String> getBundleKeySet(Locale lc) {
        return getBundleKeySet(MSG_FILE_BASENAME, lc);
    }

    /**
     * Gets the list of keys for a given bundle for a given locale
     * @param basename The name of the bundle (ex ZMsgs)
     * @param lc The locale, null will use the default system locale
     * @return A set of keys if any found, will not return null
     */
    public static Set<String> getBundleKeySet(String basename, Locale lc) {
        ResourceBundle rb;
        try {
            if (lc == null) {
                lc = Locale.getDefault();
            }
            rb = ResourceBundle.getBundle(basename, lc, sMsgClassLoader);
            Set<String> result = new HashSet<String>();
            Enumeration<String> keysEnum =  rb.getKeys();
            while (keysEnum.hasMoreElements()) {
                result.add(keysEnum.nextElement());
            }
            return result;
        } catch (MissingResourceException e) {
            // just return an empty set if we can't find the bundle
            return Collections.emptySet();
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
                for (MsgKey key : msgkeys) {
                    messages.add(getMessage(key, locale));
                }
            }
        }
        messages.remove(null);
        return messages;
    }

    public static class MatchingPropertiesFilter implements FilenameFilter {
        private final String[] prefixes;

        public MatchingPropertiesFilter(Object[] basenames) {
            prefixes = new String[basenames.length * 2];
            int i = 0;
            for (Object basename : basenames) {
                prefixes[i++] = basename + ".";
                prefixes[i++] = basename + "_";
            }
        }

        @Override
        public boolean accept(File dir, String name) {
            if (!name.endsWith(".properties")) {
                return false;
            }
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Returns the locale corresponding to the given properties file.
     *  Note that a filename like <tt>ZmMsg.properties</tt> will return
     *  <tt>null</tt>, <u>not</u> <tt>en_US</tt>. */
    public static Locale getLocaleForPropertiesFile(File file, boolean debug) {
        String[] localeParts = file.getName().split("\\.")[0].split("_");
        if (localeParts.length == 2) {
            if (debug) {
                ZimbraLog.misc.debug("        found locale: " + localeParts[1]);
            }
            return new Locale(localeParts[1]);
        } else if (localeParts.length == 3) {
            if (debug) {
                ZimbraLog.misc.debug("        found locale: " + localeParts[1] + " " + localeParts[2]);
            }
            return new Locale(localeParts[1], localeParts[2]);
        } else if (localeParts.length == 4) {
            if (debug) {
                ZimbraLog.misc.debug("        found locale: " + localeParts[1] + " " + localeParts[2] + " " + localeParts[3]);
            }
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
                    if (parts.length == 1) {
                        lc = new Locale(parts[0]);
                    } else if (parts.length == 2) {
                        lc = new Locale(parts[0], parts[1]);
                    } else if (parts.length >= 3) {
                        lc = new Locale(parts[0], parts[1], parts[2]);
                    }
                    if (lc != null) {
                        sLocaleMap.put(name, lc);
                    }
                }
            }
        }
        return lc;
    }

}
