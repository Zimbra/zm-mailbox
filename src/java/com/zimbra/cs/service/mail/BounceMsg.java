/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailMimeMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.mail.ParseMimeMessage.MessageAddresses;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public final class BounceMsg extends MailDocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element msgElem = request.getElement(MailConstants.E_MSG);
        ItemId iid = new ItemId(msgElem.getAttribute(MailConstants.A_ID), zsc);

        MailSender msender = mbox.getMailSender().setSaveToSent(false).setRedirectMode(true).setSkipHeaderUpdate(true);

        Upload upload = null;
        try {
            InputStream is;
            if (iid.belongsTo(mbox)) {
                is = mbox.getMessageById(octxt, iid.getId()).getContentStream();
            } else if (iid.isLocal()) {
                Mailbox mboxSrc = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                is = mboxSrc.getMessageById(octxt, iid.getId()).getContentStream();
            } else {
                upload = UserServlet.getRemoteResourceAsUpload(zsc.getAuthToken(), iid, Maps.<String, String>newHashMap());
                is = upload.getInputStream();
            }

            JavaMailMimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSmtpSession(acct), is);
            addResentHeaders(msgElem, mm, zsc, octxt, acct, msender);
            msender.sendMimeMessage(octxt, mbox, mm);
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("error generating new message", me);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error fetching remote message", ioe);
        } finally {
            // always want to delete the upload; msender.setUploads() deletes only on success
            if (upload != null) {
                FileUploadServlet.deleteUpload(upload);
            }
        }

        Element response = zsc.createElement(MailConstants.BOUNCE_MSG_RESPONSE);
        return response;
    }

    /** Adds a full set of {@code Resent-*} headers to the {@code MimeMessage}.
     *  Addressees are retrieved from the {@code <m>} element; note that "to"
     *  {@code <e>} elements map to {@code Resent-To} addresses, "from" {@code
     *  <e>} elements map to {@code Resent-From} addresses, etc.  Validates
     *  {@code Resent-From} and {@code Resent-Sender} in the same manner as
     *  {@code From} and {@code Sender} are treated in normal mail send.
     *  Updates the {@link MailSender}'s envelope with the sender and recipient
     *  {@code Resent-*} addresses. */
    MailSender addResentHeaders(Element msgElem, JavaMailMimeMessage mm, ZimbraSoapContext zsc, OperationContext octxt,
            Account acct, MailSender msender) throws MessagingException, ServiceException {
        MessageAddresses maddrs = getResentAddressees(msgElem, acct, msender);

        // RFC 5322 section 3.6.6:
        //    When resent fields are used, the "Resent-From:" and "Resent-Date:"
        //    fields MUST be sent.  The "Resent-Message-ID:" field SHOULD be sent.
        //    "Resent-Sender:" SHOULD NOT be used if "Resent-Sender:" would be
        //    identical to "Resent-From:".

        // UniqueValue.getUniqueMessageIDValue() isn't visible, so get a message-id another way
        String msgid = new JavaMailMimeMessage(mm.getSession()).getMessageID();
        mm.addHeader("Resent-Message-ID", msgid);

        List<String> recipients = new ArrayList<String>(5);
        recipients.addAll(addResentRecipientHeader(mm, "Resent-Bcc", maddrs.get(EmailType.BCC.toString())));
        recipients.addAll(addResentRecipientHeader(mm, "Resent-Cc", maddrs.get(EmailType.CC.toString())));
        recipients.addAll(addResentRecipientHeader(mm, "Resent-To", maddrs.get(EmailType.TO.toString())));
        if (recipients.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("no recipients specified", null);
        }
        InternetAddress rfrom = ArrayUtil.getFirstElement(maddrs.get(EmailType.FROM.toString()));
        InternetAddress rsender = ArrayUtil.getFirstElement(maddrs.get(EmailType.SENDER.toString()));
        Pair<InternetAddress, InternetAddress> fromsender = msender.getSenderHeaders(rfrom, rsender,
                acct, getAuthenticatedAccount(zsc), octxt != null ? octxt.isUsingAdminPrivileges() : false);
        InternetAddress from = fromsender.getFirst();
        InternetAddress sender = fromsender.getSecond();
        assert(from != null);
        if (sender != null) {
            mm.addHeader("Resent-Sender", sender.toString());
        }
        mm.addHeader("Resent-From", from.toString());

        mm.addHeader("Resent-Date", DateUtil.toRFC822Date(new Date(System.currentTimeMillis())));

        mm.saveChanges();

        // now that we've updated the MimeMessage's headers, we can update the MailSender's envelope
        msender.setEnvelopeFrom(from.getAddress());
        msender.setRecipients(recipients.toArray(new String[recipients.size()]));
        return msender;
    }

    /** Retrieves the sender and recipient addresses from the {@code <m>}
     *  element.  Note that "to" {@code <e>} elements map to {@code Resent-To}
     *  addresses, "from" {@code <e>} elements map to {@code Resent-From}
     *  addresses, etc.  The authenticated user's default charset is used
     *  to 2047-encode the display names when needed.
     * @param msgElem  The {@code <m>} element containing the addresses.
     * @param acct     The authenticated user.
     * @param msender  The {@link MailSender} to be used to send the message.
     * @return a {@link MessageAddresses} element encapsulating all the
     *         addresses specified by {@code <e>} subelements. */
    MessageAddresses getResentAddressees(Element msgElem, Account acct, MailSender msender) throws ServiceException {
        String defaultCharset = acct.getPrefMailDefaultCharset();
        if (Strings.isNullOrEmpty(defaultCharset)) {
            defaultCharset = MimeConstants.P_CHARSET_UTF8;
        }

        MessageAddresses maddrs = new MessageAddresses();
        for (Element e : msgElem.listElements(MailConstants.E_EMAIL)) {
            try {
                maddrs.add(e, defaultCharset);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("error generating addressees", ioe);
            }
        }
        if (maddrs.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("no recipients specified", null);
        }
        return maddrs;
    }

    /** Adds a new header to the {@link MimeMessage} consisting of the given
     *  the {@link InternetAddress}es.  (Note that this is an add and not a
     *  set, as you can have multiple sets of {@code Resent-*} headers on a
     *  message if the message has been resent repeatedly.)
     * @param mm     The message to add headers to.
     * @param name   The name of the header being added.
     * @param addrs  The addrs being serialized to the header.
     * @return a non-{@code null} {@code List} of the email parts of the added
     *         addresses. */
    List<String> addResentRecipientHeader(MimeMessage mm, String name, InternetAddress[] addrs)
    throws MessagingException {
        if (addrs == null || addrs.length == 0)
            return Collections.emptyList();

        mm.addHeader(name, Joiner.on(", ").join(addrs));

        List<String> recipients = new ArrayList<String>(5);
        for (InternetAddress addr : addrs) {
            recipients.add(addr.getAddress());
        }
        return recipients;
    }
}
