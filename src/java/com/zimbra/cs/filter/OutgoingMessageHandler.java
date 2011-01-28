package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Mail filtering implementation for messages that are sent from a user's account.
 */
public class OutgoingMessageHandler extends FilterHandler {

    private ParsedMessage parsedMessage;
    private Mailbox mailbox;
    private int defaultFolderId;
    private boolean noICal;
    private int defaultFlags;
    private String defaultTags;
    private int convId;
    private OperationContext octxt;

    public OutgoingMessageHandler(Mailbox mailbox, ParsedMessage pm, int sentFolderId, boolean noICal,
                                  int flags, String tags, int convId, OperationContext octxt) {
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
    public Message explicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        try {
            return mailbox.addMessage(octxt, parsedMessage, defaultFolderId, noICal,
                                       FilterUtil.getFlagBitmask(flagActions, defaultFlags, mailbox),
                                       FilterUtil.getTagsUnion(tags, defaultTags), convId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add sent message", e);
        }
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException {
        FilterUtil.redirect(octxt, mailbox, parsedMessage.getMimeMessage(), destinationAddress);
    }

    @Override
    public void reply(String bodyTemplate) {
        ZimbraLog.filter.debug("Ignoring attempt to reply to outgoing message");
    }

    @Override
    public void notify(String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes)
            throws ServiceException, MessagingException {
        FilterUtil.notify(octxt, mailbox, parsedMessage, emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes);
    }

    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String tags) throws ServiceException {
        return explicitKeep(flagActions, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String tags) throws ServiceException {
        return FilterUtil.addMessage(null, mailbox, parsedMessage, null, folderPath, noICal,
                                     FilterUtil.getFlagBitmask(flagActions, defaultFlags, mailbox),
                                     FilterUtil.getTagsUnion(tags, defaultTags), convId, octxt);
    }
}
