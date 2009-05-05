/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.DeliveryContext;
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
    
    public IncomingMessageHandler(DeliveryContext context, Mailbox mbox,
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
