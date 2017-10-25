/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.logger.EventLogger;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ErejectException;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MessageCallbackContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.service.util.SpamHandler.SpamReport;

/**
 * Mail filtering implementation for messages that arrive via LMTP or from
 * an external account.
 */
public final class IncomingMessageHandler implements FilterHandler {

    private OperationContext octxt;
    private DeliveryContext dctxt;
    private ParsedMessage parsedMessage;
    private Mailbox mailbox;
    private int defaultFolderId;
    private String recipientAddress;
    private int size;
    private boolean noICal;

    public IncomingMessageHandler(OperationContext octxt, DeliveryContext dctxt, Mailbox mbox,
                                  String recipientAddress, ParsedMessage pm, int size,
                                  int defaultFolderId, boolean noICal) {
        this.octxt = octxt;
        this.dctxt = dctxt;
        this.mailbox = mbox;
        this.recipientAddress = recipientAddress;
        this.parsedMessage = pm;
        this.size = size;
        this.defaultFolderId = defaultFolderId;
        this.noICal = noICal;
    }

    @Override
    public Message getMessage() {
        return null;
    }

    @Override
    public MimeMessage getMimeMessage() {
        return parsedMessage.getMimeMessage();
    }

    @Override
    public ParsedMessage getParsedMessage() {
        return parsedMessage;
    }

    @Override
    public String getDefaultFolderPath() throws ServiceException {
        return mailbox.getFolderById(octxt, defaultFolderId).getPath();
    }

    @Override
    public Message explicitKeep(Collection<ActionFlag> flagActions, String[] tags)
    throws ServiceException {
        return addMessage(defaultFolderId, flagActions, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String[] tags)
            throws ServiceException {
        ItemId id = FilterUtil.addMessage(dctxt, mailbox, parsedMessage, recipientAddress, folderPath,
                                          false, FilterUtil.getFlagBitmask(flagActions, Flag.BITMASK_UNREAD),
                                          tags, Mailbox.ID_AUTO_INCREMENT, octxt);

        // Do spam training if the user explicitly filed the message into
        // the spam folder (bug 37164).
        try {
            Folder folder = mailbox.getFolderByPath(octxt, folderPath);
            if (folder.getId() == Mailbox.ID_FOLDER_SPAM && id.isLocal()) {
                SpamReport report = new SpamReport(true, "filter", folderPath);
                SpamHandler.getInstance().handle(octxt, mailbox, id.getId(), MailItem.Type.MESSAGE, report);
            }
        } catch (NoSuchItemException e) {
            ZimbraLog.filter.debug("Unable to do spam training for message %s because folder path %s does not exist.",
                id, folderPath);
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to do spam training for message %s.", id, e);
        }

        return id;
    }

    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException {
        int folderId = mailbox.getAccount().isFeatureAntispamEnabled() && SpamHandler.isSpam(getMimeMessage()) ?
                Mailbox.ID_FOLDER_SPAM : defaultFolderId;
        return addMessage(folderId, flagActions, tags);
    }

    private Message addMessage(int folderId, Collection<ActionFlag> flagActions, String[] tags)
            throws ServiceException {
        try {
            DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setNoICal(noICal).setRecipientEmail(recipientAddress);
            dopt.setFlags(FilterUtil.getFlagBitmask(flagActions, Flag.BITMASK_UNREAD)).setTags(tags);
            MessageCallbackContext ctxt = new MessageCallbackContext(Mailbox.MessageCallback.Type.received);
            ctxt.setRecipient(recipientAddress);
            dopt.setCallbackContext(ctxt);
            return mailbox.addMessage(octxt, parsedMessage, dopt, dctxt);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add incoming message", e);
        }
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException {
        FilterUtil.redirect(octxt, mailbox, parsedMessage.getOriginalMessage(), destinationAddress);
    }

    @Override
    public void reply(String bodyTemplate) throws ServiceException, MessagingException {
        FilterUtil.reply(octxt, mailbox, parsedMessage, bodyTemplate);
    }

    @Override
    public void notify(
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders)
            throws ServiceException, MessagingException {
        FilterUtil.notify(
                octxt, mailbox, parsedMessage, emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, origHeaders);
    }

    @Override
    public void reject(String reason, LmtpEnvelope envelope) throws ServiceException, MessagingException {
        FilterUtil.reject(octxt, mailbox, parsedMessage, reason, envelope);
    }

    @Override
    public void ereject(LmtpEnvelope envelope) throws ErejectException {
        throw new ErejectException(
                "'ereject' action refuses delivery of a message. Sieve rule evaluation is cancelled");
    }

    @Override
    public void notifyMailto(LmtpEnvelope envelope, String from, int importance,
            Map<String, String> options, String message, String mailto,
            Map<String, List<String>> mailtoParams)
            throws ServiceException, MessagingException {
        FilterUtil.notifyMailto(envelope, octxt, mailbox, parsedMessage, from, importance, options, message, mailto, mailtoParams);
    }

    @Override
    public int getMessageSize() {
        return size;
    }

    @Override
    public void discard() {
    }

    @Override
    public void beforeFiltering() {
    }

    @Override
    public void afterFiltering() {
    }

    @Override
    public DeliveryContext getDeliveryContext() {
        return dctxt;
    }

    public void setParsedMessage(ParsedMessage pm) {
        this.parsedMessage = pm;
    }
}
