/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Strings;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.ExtAuthTokenKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.TokenUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.account.ShareInfo.NotificationSender.HtmlPartDataSource;

/**
 * @author schemers
 */
public class ModifyPrefs extends AccountDocumentHandler {

    public static final String PREF_PREFIX = "zimbraPref";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        OperationContext octxt = getOperationContext(zsc, context);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        HashMap<String, Object> prefs = new HashMap<String, Object>();
        Map<String, Set<String>> name2uniqueAttrValues = new HashMap<String, Set<String>>();
        for (KeyValuePair kvp : request.listKeyValuePairs(AccountConstants.E_PREF, AccountConstants.A_NAME)) {
            String name = kvp.getKey(), value = kvp.getValue();
            char ch = name.length() > 0 ? name.charAt(0) : 0;
            int offset = ch == '+' || ch == '-' ? 1 : 0;
            if (!name.startsWith(PREF_PREFIX, offset))
                throw ServiceException.INVALID_REQUEST("pref name must start with " + PREF_PREFIX, null);

            AttributeInfo attrInfo = AttributeManager.getInstance().getAttributeInfo(name.substring(offset));
            if (attrInfo == null) {
                throw ServiceException.INVALID_REQUEST("no such attribute: " + name, null);
            }
            if (attrInfo.isCaseInsensitive()) {
                String valueLowerCase = Strings.nullToEmpty(value).toLowerCase();
                if (name2uniqueAttrValues.get(name) == null) {
                    Set<String> set = new HashSet<String>();
                    set.add(valueLowerCase);
                    name2uniqueAttrValues.put(name, set);
                    StringUtil.addToMultiMap(prefs, name, value);
                } else {
                    Set<String> set = name2uniqueAttrValues.get(name);
                    if (set.add(valueLowerCase)) {
                        StringUtil.addToMultiMap(prefs, name, value);
                    }
                }
            } else {
                StringUtil.addToMultiMap(prefs, name, value);
            }
        }

        if (prefs.containsKey(Provisioning.A_zimbraPrefMailForwardingAddress)) {
            if (!account.getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingEnabled, false)) {
                throw ServiceException.PERM_DENIED("forwarding not enabled");
            } else {
                if (account.getBooleanAttr(
                    Provisioning.A_zimbraFeatureMailForwardingVerificationEnabled, false)) {
                    /*
                     * forwarding address verification enabled, store the email
                     * ID in 'zimbraFeatureMailForwardingVerificationAddress'
                     * till the time it's verified
                     */
                    String emailIdToVerify = (String) prefs
                        .get(Provisioning.A_zimbraPrefMailForwardingAddress);
                    prefs.remove(Provisioning.A_zimbraPrefMailForwardingAddress);
                    StringUtil.addToMultiMap(prefs,
                        Provisioning.A_zimbraFeatureMailForwardingVerificationAddress,
                        emailIdToVerify);
                    Account authAccount = getAuthenticatedAccount(zsc);
                    sendEmailVerificationLink(authAccount, account, emailIdToVerify, octxt, mbox);
                }
            }
        }

        // call modifyAttrs and pass true to checkImmutable
        Provisioning.getInstance().modifyAttrs(account, prefs, true, zsc.getAuthToken());

        Element response = zsc.createElement(AccountConstants.MODIFY_PREFS_RESPONSE);
        return response;
    }

    private static void sendEmailVerificationLink(Account authAccount, Account account,
        String emailIdToVerify, OperationContext octxt, Mailbox mbox) throws ServiceException {
        Locale locale = authAccount.getLocale();
        String ownerAcctDisplayName = account.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = account.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.verifyEmailSubject, locale,
            ownerAcctDisplayName);
        String charset = authAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset,
            MimeConstants.P_CHARSET_UTF8);
        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSmtpSession(authAccount));
            mm.setSubject(subject, CharsetUtil.checkCharset(subject, charset));
            mm.setSentDate(new Date());
            mm.setFrom(AccountUtil.getFriendlyEmailAddress(account));
            mm.setSender(AccountUtil.getFriendlyEmailAddress(authAccount));
            mm.setRecipient(javax.mail.Message.RecipientType.TO,
                new JavaMailInternetAddress(emailIdToVerify));
            long expiry = account.getFeatureMailForwardingVerificationExpiry();
            Date now = new Date();
            long expiryTime = now.getTime() + expiry;
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String gmtDate = format.format(expiryTime);
            String url = getEmailVerificationURL(account, expiryTime, emailIdToVerify);
            if (ZimbraLog.account.isDebugEnabled()) {
                ZimbraLog.account.debug(
                    "Expiry of Forwarding address verification link sent to %s is %s",
                    emailIdToVerify, gmtDate);
                ZimbraLog.account.debug("Forwarding address verification URL sent to %s is %s",
                    emailIdToVerify, url);
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.verifyEmailBodyText, locale,
                ownerAcctDisplayName, url, gmtDate);
            MimeMultipart mmp = new ZMimeMultipart("alternative");
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(mimePartText, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.verifyEmailBodyHtml, locale,
                ownerAcctDisplayName, url, gmtDate);
            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(mimePartHtml)));
            mmp.addBodyPart(htmlPart);
            mm.setContent(mmp);
            mm.saveChanges();
            mbox.getMailSender().sendMimeMessage(octxt, mbox, true, mm, null, null, null, null,
                false);
        } catch (MessagingException e) {
            ZimbraLog.account.warn("Failed to send verification link to email ID: '" + emailIdToVerify +"'", e);
            throw ServiceException.FAILURE("Failed to send verification link to email ID: "+emailIdToVerify, e);
        }
    }

    private static String getEmailVerificationURL(Account account, long expiry,
        String externalUserEmail) throws ServiceException {
        StringBuilder encodedBuff = new StringBuilder();
        BlobMetaData.encodeMetaData("accountId", account.getId(), encodedBuff);
        BlobMetaData.encodeMetaData("expiry", expiry, encodedBuff);
        BlobMetaData.encodeMetaData("emailAddressUnderVerification", externalUserEmail, encodedBuff);
        String data = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
        ExtAuthTokenKey key = ExtAuthTokenKey.getCurrentKey();
        String hmac = TokenUtil.getHmac(data, key.getKey());
        String encoded = key.getVersion() + "_" + hmac + "_" + data;
        String path = "/service/extuserprov/?p=" + encoded;
        return ZimbraServlet.getServiceUrl(account.getServer(),
            Provisioning.getInstance().getDomain(account), path);
    }
}
