package com.zimbra.cs.filter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Collection;

/**
 * Mail filtering implementation for messages that are sent from a user's account.
 */
public class OutgoingMessageHandler extends FilterHandler {

    private ParsedMessage mParsedMessage;
    private Mailbox mMailbox;
    private int mDefaultFolderId;
    private boolean mNoICal;
    private int mDefaultFlags;
    private String mDefaultTags;
    private int mConvId;
    private OperationContext mOctxt;

    public OutgoingMessageHandler(Mailbox mailbox, ParsedMessage pm, int sentFolderId, boolean noICal,
                                  int flags, String tags, int convId, OperationContext octxt) {
        mMailbox = mailbox;
        mParsedMessage = pm;
        mDefaultFolderId = sentFolderId;
        mNoICal = noICal;
        mDefaultFlags = flags;
        mDefaultTags = tags;
        mConvId = convId;
        mOctxt = octxt;
    }

    public MimeMessage getMimeMessage() {
        return mParsedMessage.getMimeMessage();
    }

    public int getMessageSize() {
        try {
            return mParsedMessage.getMimeMessage().getSize();
        } catch (Exception e) {
            ZimbraLog.filter.warn("Error in determining message size", e);
            return -1;
        }
    }

    public ParsedMessage getParsedMessage() {
        return mParsedMessage;
    }

    public String getDefaultFolderPath()
    throws ServiceException {
        return mMailbox.getFolderById(null, mDefaultFolderId).getPath();
    }

    @Override
    public Message explicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        try {
            return mMailbox.addMessage(mOctxt, mParsedMessage, mDefaultFolderId, mNoICal,
                                       FilterUtil.getFlagBitmask(flagActions, mDefaultFlags, mMailbox),
                                       FilterUtil.getTagsUnion(tags, mDefaultTags), mConvId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add sent message", e);
        }
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException {
        FilterUtil.redirect(mMailbox, mParsedMessage.getMimeMessage(), destinationAddress);
    }

    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String tags) throws ServiceException {
        return explicitKeep(flagActions, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String tags) throws ServiceException {
        return FilterUtil.addMessage(null, mMailbox, mParsedMessage, null, folderPath, mNoICal,
                                     FilterUtil.getFlagBitmask(flagActions, mDefaultFlags, mMailbox),
                                     FilterUtil.getTagsUnion(tags, mDefaultTags), mConvId, mOctxt);
    }
}
