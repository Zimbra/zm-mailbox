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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.util.JMSession;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
            List<NamedEntry> accounts = findAccountsWithMaxAgePasswordAndReminderEnabled(ldapProv);
            ExecutorService emailSenderExecutor = Executors.newFixedThreadPool(PASSWORD_REMINDER_THREAD_COUNT);
            for (int i = 0; i < accounts.size(); i += PASSWORD_REMINDER_BATCH_SIZE) {
                final List<NamedEntry> batch = accounts.subList(i, Math.min(i + PASSWORD_REMINDER_BATCH_SIZE, accounts.size()));
                emailSenderExecutor.submit(() -> filterAccountsAndSendMail(batch));
            }
            emailSenderExecutor.shutdown();
        } catch (ServiceException e) {
            ZimbraLog.store.error("Failed to send reminder", e);
        }
    }

    /**
     * Finding accounts that meet the following conditions:
     * - The account has the password expiry reminder feature enabled
     * - The account's password is not set to never expire (i.e., max age is not zero)
     *
     * @param ldapProv The instance of the LdapProv used to perform the LDAP search.
     * @return A list of {@link NamedEntry} objects representing the accounts that meet the conditions.
     * @throws ServiceException If there is an issue during the LDAP search.
     */
    private static List<NamedEntry> findAccountsWithMaxAgePasswordAndReminderEnabled(LdapProv ldapProv) throws ServiceException {
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(
                new String[]{
                        Provisioning.A_zimbraPasswordMaxAge,
                        Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled,
                        Provisioning.A_zimbraPasswordModifiedTime,
                        Provisioning.A_zimbraMailDeliveryAddress,
                        Provisioning.A_cn});
        ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
        ZLdapFilter filterServer = filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraMailHost, LC.zimbra_server_hostname.value());
        ZLdapFilter filterPasswordExpiryReminderEnabled = filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraFeaturePasswordExpiryReminderEnabled, "TRUE");
        ZLdapFilter filterPasswordMaxAge = filterFactory.negate(filterFactory.accountsWithLdapFeatureCheck(Provisioning.A_zimbraPasswordMaxAge, "0"));
        ZLdapFilter filterReminderAndMaxAgeEnabled = filterFactory.andWith(filterPasswordExpiryReminderEnabled, filterPasswordMaxAge);
        searchOpts.setFilter(filterFactory.andWith(filterServer, filterReminderAndMaxAgeEnabled));
        searchOpts.setIncludeType(SearchAccountsOptions.IncludeType.ACCOUNTS_ONLY);
        return ldapProv.searchDirectory(searchOpts);
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
            Session session = JMSession.getSmtpSession(account);
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress("no-reply", "Password Expiration Notification"));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(account.getAttr(Provisioning.A_zimbraMailDeliveryAddress)));
            String userName = account.getAttr(Provisioning.A_cn);
            Locale locale = account.getLocale();
            String textBodyMessage = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierBodyText, locale, userName, (int)deadline, expiresOn);
            String htmlBodyMessage = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierBodyHtml, locale, userName, (int)deadline, expiresOn);
            String subject = L10nUtil.getMessage(L10nUtil.MsgKey.passwordExpiryNotifierSubject, locale, (int) deadline);
            mimeMessage.setSubject(subject);
            Multipart multipart = new MimeMultipart();
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(textBodyMessage);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBodyMessage, "text/html");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            mimeMessage.setContent(multipart);
            Transport.send(mimeMessage);
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
