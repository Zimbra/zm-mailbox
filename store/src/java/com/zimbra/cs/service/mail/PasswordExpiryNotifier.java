/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class PasswordExpiryNotifier implements Runnable {

    private static final int PASSWORD_REMINDER_THREAD_COUNT = LC.zimbra_password_expiry_reminder_thread_count.intValue();
    private static final int PASSWORD_REMINDER_BATCH_SIZE = 500;
    private static final DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

    public static void main(String[] args) {
        Thread thread = new Thread(new PasswordExpiryNotifier());
        thread.start();
    }

    /**
     * Sending password reminder emails to user accounts based on the following conditions:
     * - Accounts with passwords approaching their maximum age
     * - Accounts where password reminder notifications are enabled
     *
     * If an error occurs during the reminder sending process, it is logged for further analysis.
     */
    protected static void sendReminder() {
        try {
            LdapProv ldapProv = LdapProv.getInst();
            findAccountsWithMaxAgePasswordAndReminderInheritedFromCos(ldapProv);
            findAccountsWithMaxAgePasswordAndReminderSet(ldapProv);
        } catch (ServiceException e) {
            ZimbraLog.store.error("Failed to send reminder", e);
        }
    }

    /**
     * Finds accounts that meet the following conditions:
     * - The account has the password expiry reminder feature explicitly enabled.
     * - The account's password is not set to never expire (i.e., the maximum password age is not zero).
     *
     * This method searches for accounts that have the password expiry reminder feature enabled and also have
     * a non-zero password max age. It processes the results in batches and, for each batch, a task is created
     * for sending notifications.
     *
     * @param ldapProv The instance of the LdapProv used to perform the LDAP search and handle directory operations.
     * @throws ServiceException If there is an issue during the LDAP search or processing of the accounts.
     */
    private static void findAccountsWithMaxAgePasswordAndReminderSet(LdapProv ldapProv) throws ServiceException {
        SearchAccountsOptions searchAcctOpts = new SearchAccountsOptions(
                new String[]{
                        Provisioning.A_zimbraPasswordMaxAge,
                        Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled,
                        Provisioning.A_zimbraPasswordModifiedTime,
                        Provisioning.A_zimbraMailDeliveryAddress,
                        Provisioning.A_cn,
                        Provisioning.A_zimbraPrefLocale});
        ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
        // filtering accounts with set attributes
        ZLdapFilter filterServer = filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraMailHost, LC.zimbra_server_hostname.value());
        ZLdapFilter filterPasswordExpiryReminderEnabled = filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled, "TRUE");
        ZLdapFilter filterPasswordMaxAge = filterFactory.negate(filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraPasswordMaxAge, "0"));
        ZLdapFilter filterReminderAndMaxAgeEnabled = filterFactory.andWith(filterPasswordExpiryReminderEnabled, filterPasswordMaxAge);
        searchAcctOpts.setFilter(filterFactory.andWith(filterServer, filterReminderAndMaxAgeEnabled));
        searchAcctOpts.setResultPageSize(PASSWORD_REMINDER_BATCH_SIZE);
        searchAcctOpts.setUseControl(true);
        searchAcctOpts.setLimit(PASSWORD_REMINDER_BATCH_SIZE);
        int offset = 0;
        List<NamedEntry> accountsWithAttributesSet;
        ExecutorService emailSenderExecutor = Executors.newFixedThreadPool(PASSWORD_REMINDER_THREAD_COUNT);
        do {
            searchAcctOpts.setOffset(offset);
            accountsWithAttributesSet = ldapProv.searchDirectory(searchAcctOpts);
            final List<NamedEntry> batch = accountsWithAttributesSet;
            emailSenderExecutor.submit(() -> filterAccountsAndSendMail(batch));
            offset += PASSWORD_REMINDER_BATCH_SIZE;
        } while (!accountsWithAttributesSet.isEmpty());
        emailSenderExecutor.shutdown();
        // wait for the executor to finish processing all tasks
        while (!emailSenderExecutor.isTerminated()) {
        }
        // after all emails are processed and sent, clear the file
        clearSentEmailsFile();
    }

    /**
     * Finds accounts that meet the following conditions:
     * - The account has the password expiry reminder feature enabled.
     * - The account's password is not set to never expire (i.e., max password age is not zero).
     *
     * This method first filters the Classes of Service (COS) that have the password expiry reminder feature enabled
     * and ensure their password max age is not set to zero. Then, it retrieves accounts that inherit these attributes
     * from their COS and have the corresponding feature enabled.
     *
     * The results are processed in batches, and for each batch, a thread is created to send notifications.
     *
     * @param ldapProv The instance of the LdapProv used to perform the LDAP search and manage the directory operations.
     * @throws ServiceException If there is an issue during the LDAP search or processing of the accounts.
     */
    private static void findAccountsWithMaxAgePasswordAndReminderInheritedFromCos(LdapProv ldapProv) throws ServiceException {
        SearchAccountsOptions searchAccFromCosOpts = new SearchAccountsOptions(
                new String[]{
                        Provisioning.A_zimbraPasswordMaxAge,
                        Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled,
                        Provisioning.A_zimbraPasswordModifiedTime,
                        Provisioning.A_zimbraMailDeliveryAddress,
                        Provisioning.A_cn,
                        Provisioning.A_zimbraPrefLocale,
                        Provisioning.A_zimbraCOSId});
        SearchDirectoryOptions searchCosOpts = new SearchDirectoryOptions(
                new String[]{
                        Provisioning.A_zimbraPasswordMaxAge,
                        Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled}
        );
        ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
        // filtering Coses
        ZLdapFilter filterPasswordExpiryReminderEnabledCos = filterFactory.cosesWithLdapFeatureCheck(Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled, "TRUE");
        ZLdapFilter filterPasswordMaxAgeCos = filterFactory.negate(filterFactory.cosesWithLdapFeatureCheck(Provisioning.A_zimbraPasswordMaxAge, "0"));
        searchCosOpts.setFilter(filterFactory.andWith(filterPasswordExpiryReminderEnabledCos, filterPasswordMaxAgeCos));
        searchCosOpts.addType(SearchDirectoryOptions.ObjectType.coses);
        List<NamedEntry> coses=  ldapProv.searchDirectory(searchCosOpts);
        List<String> cosIds = coses.stream()
                .map(NamedEntry::getId)
                .collect(Collectors.toList());
        // filtering Accounts with inherited attributes
        ZLdapFilter filterServer = filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraMailHost, LC.zimbra_server_hostname.value());
        ZLdapFilter filterReminderEnabledAtCos = filterFactory.accountsByCosesAndFeatureCheck(cosIds,Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled);
        searchAccFromCosOpts.setFilter(filterFactory.andWith(filterServer, filterReminderEnabledAtCos));
        searchAccFromCosOpts.setResultPageSize(PASSWORD_REMINDER_BATCH_SIZE);
        searchAccFromCosOpts.setUseControl(true);
        searchAccFromCosOpts.setLimit(PASSWORD_REMINDER_BATCH_SIZE);
        int offset = 0;
        List<NamedEntry> accountsWithAttributesInherited;
        ExecutorService emailSenderExecutor = Executors.newFixedThreadPool(PASSWORD_REMINDER_THREAD_COUNT);
        do {
            searchAccFromCosOpts.setOffset(offset);
            accountsWithAttributesInherited = ldapProv.searchDirectory(searchAccFromCosOpts);
            final List<NamedEntry> batch = accountsWithAttributesInherited;
            emailSenderExecutor.submit(() -> filterAccountsAndSendMail(batch));
            offset += PASSWORD_REMINDER_BATCH_SIZE;
        } while (!accountsWithAttributesInherited.isEmpty());
        emailSenderExecutor.shutdown();
        // wait for the executor to finish processing all tasks
        while (!emailSenderExecutor.isTerminated()) {
        }
        // after all emails are processed and sent, clear the file
        clearSentEmailsFile();
    }

    /**
     * Clears the contents of the file specified by the constant `SENT_EMAILS_FILE` by truncating it.
     * This method is used to empty the file after all emails have been successfully sent, ensuring
     * that the list of sent emails is reset for future operations.
     *
     * @throws IOException If an error occurs while writing to the file.
     */
    private static void clearSentEmailsFile() {
        try {
            // create a new FileWriter with append=false to truncate the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(EmailTracker.getSentEmailsFile(), false))) {
                writer.write(""); // writing an empty string to truncate the file
            }
        } catch (IOException e) {
            ZimbraLog.store.error("Failed to clear the sent emails file: " + e.getMessage());
        }
    }

    /**
     * Filtering accounts in a given batch and sending password expiry reminder emails to those
     * that meet the following conditions:
     * - The account's password was modified recently.
     * - The password is approaching expiration (i.e., within the next 10 days).
     *
     * @param batch A list of {@link NamedEntry} objects representing the accounts to be processed.
     */
    private static void filterAccountsAndSendMail(List<NamedEntry> batch) {
        for (NamedEntry ne : batch) {
            Account account = (Account) ne;
            int maxAge = account.getIntAttr(Provisioning.A_zimbraPasswordMaxAge, 0);
            Date lastChange = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
            if (lastChange != null) {
                long last = lastChange.getTime();
                long curr = System.currentTimeMillis();
                long expires = last + (Constants.MILLIS_PER_DAY * maxAge);
                String expiresOn = formatter.format(new Date(expires));
                long deadline = Math.round((float) (curr - expires) / -86400000);
                if (deadline > 0 && deadline <= 10) {
                    sendMail(account, deadline, expiresOn);
                }
            }
        }
    }

    /**
     * Sending a password expiry reminder email to the user account.
     *
     * The email contains information about the following:
     * - The number of days remaining before the account's password expires.
     * - The expiration date of the password.
     * - A reminder to reset the password to maintain access to the account.
     *
     * @param account The {@link Account} object representing the user account to which the reminder will be sent.
     * @param deadline The number of days remaining until the account's password expires.
     * @param expiresOn A string representing the exact expiration date of the account's password.
     *
     * @throws RuntimeException If there is an error while sending the email (e.g., failure in setting up the SMTP session, message creation, or transport).
     */
    private static void sendMail(Account account, long deadline, String expiresOn) {
        try {
            if (EmailTracker.isEmailSent(account.getAttr(Provisioning.A_zimbraMailDeliveryAddress))) {
                return;
            }
            MimeMultipart mmp = new ZMimeMultipart("alternative");
            Session session = JMSession.getSmtpSession(account);
            MimeMessage mimeMessage = new Mime.FixedMimeMessage(session);
            mimeMessage.setFrom(new InternetAddress("no-reply", "Password Expiration Notification"));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(account.getAttr(Provisioning.A_zimbraMailDeliveryAddress)));
            String userName = account.getAttr(Provisioning.A_cn);
            Locale locale = account.getLocale();
            String textBodyMessage = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierBodyText, locale, userName, (int)deadline, expiresOn);
            String htmlBodyMessage = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierBodyHtml, locale, userName, (int)deadline, expiresOn);
            String subject = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierSubject, locale, (int) deadline);
            String charset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(textBodyMessage, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);
            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new AccountUtil.HtmlPartDataSource(htmlBodyMessage)));
            mmp.addBodyPart(htmlPart);
            mimeMessage.setSubject(subject, CharsetUtil.checkCharset(subject, charset));
            mimeMessage.setContent(mmp);
            mimeMessage.saveChanges();
            Transport.send(mimeMessage);
            EmailTracker.markEmailAsSent(account.getAttr(Provisioning.A_zimbraMailDeliveryAddress));
        } catch (Exception e) {
            ZimbraLog.store.error("Failed to send email for account %s ", account.getName(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        sendReminder();
    }
}