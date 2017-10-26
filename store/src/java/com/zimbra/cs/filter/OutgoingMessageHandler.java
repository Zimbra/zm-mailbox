/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.MessageCallbackContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Mail filtering implementation for messages that are sent from a user's account.
 */
public final class OutgoingMessageHandler implements FilterHandler {

    private ParsedMessage parsedMessage;
    private Mailbox mailbox;
    private int defaultFolderId;
    private boolean noICal;
    private int defaultFlags;
    private String[] defaultTags;
    private int convId;
    private OperationContext octxt;

    public OutgoingMessageHandler(Mailbox mailbox, ParsedMessage pm, int sentFolderId, boolean noICal,
                                  int flags, String[] tags, int convId, OperationContext octxt) {
        this.mailbox = mailbox;
        this.parsedMessage = pm;
        this.defaultFolderId = sentFolderId;
        this.noICal = noICal;
        this.defaultFlags = flags;
        this.defaultTags = tags;
        this.convId = convId;
        this.octxt = octxt;
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
    public int getMessageSize() {
        try {
            return parsedMessage.getMimeMessage().getSize();
        } catch (Exception e) {
            ZimbraLog.filter.warn("Error in determining message size", e);
            return -1;
        }
    }

    @Override
    public ParsedMessage getParsedMessage() {
        return parsedMessage;
    }

    @Override
    public String getDefaultFolderPath()
    throws ServiceException {
        return mailbox.getFolderById(octxt, defaultFolderId).getPath();
    }

    @Override
    public Message explicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException {
        try {
            DeliveryOptions dopt = new DeliveryOptions().setFolderId(defaultFolderId).setConversationId(convId);
            dopt.setFlags(FilterUtil.getFlagBitmask(flagActions, defaultFlags));
            dopt.setTags(FilterUtil.getTagsUnion(tags, defaultTags));
            dopt.setNoICal(noICal);
            dopt.setCallbackContext(getMessageCallbackContext());
            return mailbox.addMessage(octxt, parsedMessage, dopt, null);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add sent message", e);
        }
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException {
        FilterUtil.redirect(octxt, mailbox, parsedMessage.getOriginalMessage(), destinationAddress);
    }

    @Override
    public void reply(String bodyTemplate) {
        ZimbraLog.filter.debug("Ignoring attempt to reply to outgoing message");
    }

    @Override
    public void notify(
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders)
            throws ServiceException, MessagingException {
        FilterUtil.notify(
                octxt, mailbox, parsedMessage, emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, origHeaders);
    }

    @Override
    public void notifyMailto(LmtpEnvelope envelope, String from, int importance,
            Map<String, String> options, String message, String mailto,
            Map<String, List<String>> mailtoParams) throws ServiceException, MessagingException {
        FilterUtil.notifyMailto(envelope, octxt, mailbox, parsedMessage, from, importance, options, message, mailto, mailtoParams);
    }

    @Override
    public void reject(String reason, LmtpEnvelope envelope) {
        ZimbraLog.filter.debug("Ignoring attempt to reject outgoing message");
    }

    @Override
    public void ereject(LmtpEnvelope envelope) {
        ZimbraLog.filter.debug("Ignoring attempt to perform 'ereject' command on outgoing message");
    }

    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException {
        return explicitKeep(flagActions, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String[] tags)
            throws ServiceException {
        return FilterUtil.addMessage(null, mailbox, parsedMessage, null, folderPath, noICal,
                                     FilterUtil.getFlagBitmask(flagActions, defaultFlags),
                                     FilterUtil.getTagsUnion(tags, defaultTags), convId, octxt, getMessageCallbackContext());
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
    public MessageCallbackContext getMessageCallbackContext() {
        return new Mailbox.MessageCallbackContext(Mailbox.MessageCallback.Type.sent);
    }
}
