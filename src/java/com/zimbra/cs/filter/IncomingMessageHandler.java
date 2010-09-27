/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.Collection;

import javax.mail.internet.MimeMessage;

import org.apache.jsieve.mail.Action;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;

/**
 * Mail filtering implementation for messages that arrive via LMTP or from
 * an external account.
 */
public class IncomingMessageHandler
extends FilterHandler {

    private DeliveryContext mContext;
    private ParsedMessage mParsedMessage;
    private Mailbox mMailbox;
    private int mDefaultFolderId;
    private String mRecipientAddress;
    private int mSize;
    
    public IncomingMessageHandler(DeliveryContext context, Mailbox mbox,
                                  String recipientAddress, ParsedMessage pm, int size,
                                  int defaultFolderId) {
        mContext = context;
        mMailbox = mbox;
        mRecipientAddress = recipientAddress;
        mParsedMessage = pm;
        mSize = size;
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
    public Message explicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        return addMessage(mDefaultFolderId, flagActions, tags);
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        ItemId id = FilterUtil.addMessage(mContext, mMailbox, mParsedMessage, mRecipientAddress, folderPath, getFlagBitmask(flagActions), tags);
        
        // Do spam training if the user explicitly filed the message into
        // the spam folder (bug 37164).
        try {
            Folder folder = mMailbox.getFolderByPath(null, folderPath);
            if (folder.getId() == Mailbox.ID_FOLDER_SPAM && id.isLocal()) {
                SpamHandler.getInstance().handle(null, mMailbox, id.getId(), MailItem.TYPE_MESSAGE, true);
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
    public Message implicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        int folderId = SpamHandler.isSpam(getMimeMessage()) ? Mailbox.ID_FOLDER_SPAM : mDefaultFolderId;
        return addMessage(folderId, flagActions, tags);
    }

    private Message addMessage(int folderId, Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        Message msg = null;
        try {
            msg = mMailbox.addMessage(null, mParsedMessage, folderId,
                false, getFlagBitmask(flagActions), tags, mRecipientAddress, mContext);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to add incoming message", e);
        }
        return msg;
    }

    @Override
    public void redirect(String destinationAddress)
    throws ServiceException {
        FilterUtil.redirect(mMailbox, mParsedMessage.getMimeMessage(), destinationAddress);
    }

    private int getFlagBitmask(Collection<ActionFlag> flagActions) {
        int flagBits = Flag.BITMASK_UNREAD;
        for (Action action : flagActions) {
            ActionFlag flagAction = (ActionFlag) action;
            int flagId = flagAction.getFlagId();
            try {
                Flag flag = mMailbox.getFlagById(flagId);
                if (flagAction.isSetFlag())
                    flagBits |= flag.getBitmask();
                else
                    flagBits &= (~flag.getBitmask());
            } catch (ServiceException e) {
                ZimbraLog.filter.warn("Unable to flag message", e);
            }
        }
        return flagBits;
    }

    @Override
    public int getMessageSize() {
        return mSize;
    }
}
