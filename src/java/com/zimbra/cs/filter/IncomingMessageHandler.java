package com.zimbra.cs.filter;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;

/**
 * Mail filtering implementation for messages that arrive via LMTP or from
 * an external account.
 */
public class IncomingMessageHandler
extends FilterHandler {

    private SharedDeliveryContext mContext;
    private ParsedMessage mParsedMessage;
    private Mailbox mMailbox;
    private int mDefaultFolderId;
    private String mRecipientAddress;
    
    public IncomingMessageHandler(SharedDeliveryContext context, Mailbox mbox,
                                  String recipientAddress, ParsedMessage pm, int defaultFolderId) {
        mContext = context;
        mMailbox = mbox;
        mRecipientAddress = recipientAddress;
        mParsedMessage = pm;
        mDefaultFolderId = defaultFolderId;
    }
    
    public MimeMessage getMimeMessage() {
        return mParsedMessage.getMimeMessage();
    }

    public ParsedMessage getParsedMessage() {
        return mParsedMessage;
    }

    public String getDefaultFolderPath()
    throws ServiceException {
        return mMailbox.getFolderById(null, mDefaultFolderId).getPath();
    }

    @Override
    public int getDefaultFlagBitmask() {
        return Flag.BITMASK_UNREAD;
    }

    @Override
    public Message explicitKeep(int flagBitmask, String tags)
    throws ServiceException {
        return addMessage(mDefaultFolderId, flagBitmask, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, int flagBitmask, String tags)
    throws ServiceException {
        return FilterUtil.addMessage(mContext, mMailbox, mParsedMessage, mRecipientAddress, folderPath, flagBitmask, tags);
    }

    @Override
    public Message implicitKeep(int flagBitmask, String tags)
    throws ServiceException {
        int folderId = SpamHandler.isSpam(getMimeMessage()) ? Mailbox.ID_FOLDER_SPAM : mDefaultFolderId;
        return addMessage(folderId, flagBitmask, tags);
    }

    private Message addMessage(int folderId, int flagBitmask, String tags)
    throws ServiceException {
        Message msg = null;
        try {
            msg = mMailbox.addMessage(null, mParsedMessage, folderId,
                false, flagBitmask, tags, mRecipientAddress, mContext);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add incoming message", e);
        }
        return msg;
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException, MessagingException {
        FilterUtil.redirect(mMailbox, mParsedMessage.getMimeMessage(), destinationAddress);
    }
}
