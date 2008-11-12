/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Mail filtering implementation for messages already in the
 * user's mailbox.
 */
public class ExistingMessageHandler
extends FilterHandler {

    private Mailbox mMailbox;
    private int mMessageId;
    private MimeMessage mMimeMessage;
    private ParsedMessage mParsedMessage;
    private boolean mDeleteOriginal = true;
    private boolean mFiltered = false;
    
    public ExistingMessageHandler(Mailbox mbox, int messageId) {
        mMailbox = mbox;
        mMessageId = messageId;
    }
    
    public String getDefaultFolderPath() throws ServiceException {
        return getDefaultFolder().getPath();
    }
    
    private Folder getDefaultFolder()
    throws ServiceException {
        return mMailbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
    }

    public MimeMessage getMimeMessage()
    throws ServiceException {
        if (mMimeMessage == null) {
            Message msg = mMailbox.getMessageById(null, mMessageId);
            mMimeMessage = msg.getMimeMessage();
        }
        return mMimeMessage;
    }

    public ParsedMessage getParsedMessage()
    throws ServiceException {
        if (mParsedMessage == null) {
            mParsedMessage = new ParsedMessage(getMimeMessage(), mMailbox.attachmentsIndexingEnabled());
        }
        return mParsedMessage;
    }
    
    @Override
    public int getDefaultFlagBitmask() {
        return 0;
    }

    public boolean filtered() { return mFiltered; }

    @Override
    public void discard()
    throws ServiceException {
        ZimbraLog.filter.info("Discarding existing message with id %d.", mMessageId);
        mMailbox.delete(null, mMessageId, MailItem.TYPE_MESSAGE);
        mDeleteOriginal = false;
        mFiltered = true;
    }


    @Override
    public Message implicitKeep(int flagBitmask, String tags)
    throws ServiceException {
        ZimbraLog.filter.debug("Implicitly keeping existing message %d.", mMessageId);
        Message msg = mMailbox.getMessageById(null, mMessageId);
        updateTagsAndFlagsIfNecessary(msg, flagBitmask, tags);
        mDeleteOriginal = false;
        return msg;
    }

    @Override
    public Message explicitKeep(int flagBitmask, String tags)
    throws ServiceException {
        ZimbraLog.filter.debug("Explicitly keeping existing message %d.", mMessageId);
        Message msg = mMailbox.getMessageById(null, mMessageId);
        updateTagsAndFlagsIfNecessary(msg, flagBitmask, tags);
        mDeleteOriginal = false;
        return msg;
    }
    
    private void updateTagsAndFlagsIfNecessary(Message msg, int flagBitmask, String tags)
    throws ServiceException {
        long tagBitmask = Tag.tagsToBitmask(tags);
        if (((msg.getTagBitmask() & tagBitmask) != tagBitmask) ||
            (msg.getFlagBitmask() & flagBitmask) != flagBitmask) {
            ZimbraLog.filter.info("Updating flags to %d, tags to %d on message %d.",
                flagBitmask, tagBitmask, msg.getId());
            mMailbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE, flagBitmask, tagBitmask);
            mFiltered = true;
        }
    }
    
    @Override
    public ItemId fileInto(String folderPath, int flagBitmask, String tags) throws ServiceException {
        Message source = mMailbox.getMessageById(null, mMessageId);
        Folder currentFolder = mMailbox.getFolderById(null, source.getFolderId());
        
        if (currentFolder.getPath().equalsIgnoreCase(folderPath)) {
            ZimbraLog.filter.debug("Ignoring fileinto action for message %d.  It is already in %s.",
                mMessageId, folderPath);
            mDeleteOriginal = false;
            return null;
        }
        
        ZimbraLog.filter.info("Copying existing message %d to folder %s.", mMessageId, folderPath);
        if (isLocalExistingFolder(folderPath)) {
            // Copy item into to a local folder.
            Folder target = mMailbox.getFolderByPath(null, folderPath);
            Message newMsg = (Message) mMailbox.copy(null, mMessageId, MailItem.TYPE_MESSAGE, target.getId());
            mFiltered = true;

            // Apply flags and tags
            int flagBits = source.getFlagBitmask();
            long tagBits = Tag.tagsToBitmask(tags);
            mMailbox.setTags(null, newMsg.getId(), MailItem.TYPE_MESSAGE,
                source.getFlagBitmask() | flagBits, source.getTagBitmask() | tagBits);
            return new ItemId(mMailbox, mMessageId);
        }
        
        ItemId id = FilterUtil.addMessage(new SharedDeliveryContext(), mMailbox, getParsedMessage(),
            mMailbox.getAccount().getName(), folderPath, flagBitmask, tags);
        if (id != null) {
            mFiltered = true;
        }
        return id;
    }

    /**
     * Returns <tt>true</tt> if the folder path exists and is local to this mailbox. 
     */
    private boolean isLocalExistingFolder(String folderPath)
    throws ServiceException {
        Pair<Folder, String> folderAndPath = mMailbox.getFolderByPathLongestMatch(
            null, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
        Folder folder = folderAndPath.getFirst();
        String remainingPath = folderAndPath.getSecond();
        if (folder instanceof Mountpoint || !StringUtil.isNullOrEmpty(remainingPath)) {
            return false;
        }
        return true;
    }
    
    @Override
    public void redirect(String destinationAddress) {
        ZimbraLog.filter.debug("Ignoring attempt to redirect existing message %d to %s.",
            mMessageId, destinationAddress);
    }

    @Override
    public void afterFiltering() throws ServiceException {
        if (mDeleteOriginal) {
            ZimbraLog.filter.info("Deleting original message %d after filing to another folder.", mMessageId);
            mMailbox.delete(null, mMessageId, MailItem.TYPE_MESSAGE);
        }
    }
}
