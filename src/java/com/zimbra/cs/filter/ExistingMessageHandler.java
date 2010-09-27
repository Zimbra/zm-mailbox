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

import java.util.Collection;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
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
    private boolean mKept = false;
    private boolean mFiled = false;
    private boolean mFiltered = false;
    private int mSize;
    
    public ExistingMessageHandler(Mailbox mbox, int messageId, int size) {
        mMailbox = mbox;
        mMessageId = messageId;
        mSize = size;
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
            Message msg = mMailbox.getMessageById(null, mMessageId);
            ParsedMessageOptions opt = new ParsedMessageOptions()
                .setContent(msg.getMimeMessage())
                .setAttachmentIndexing(mMailbox.attachmentsIndexingEnabled())
                .setSize(msg.getSize())
                .setDigest(msg.getDigest());
            mParsedMessage = new ParsedMessage(opt);
        }
        return mParsedMessage;
    }
    
    public boolean filtered() { return mFiltered; }

    @Override
    public void discard()
    throws ServiceException {
        ZimbraLog.filter.info("Discarding existing message with id %d.", mMessageId);
        mMailbox.delete(null, mMessageId, MailItem.TYPE_MESSAGE);
        mFiltered = true;
    }


    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        ZimbraLog.filter.debug("Implicitly keeping existing message %d.", mMessageId);
        Message msg = mMailbox.getMessageById(null, mMessageId);
        updateTagsAndFlagsIfNecessary(msg, flagActions, tags);
        mKept = true;
        return msg;
    }

    @Override
    public Message explicitKeep(Collection<ActionFlag> flagActions, String tags)
    throws ServiceException {
        ZimbraLog.filter.debug("Explicitly keeping existing message %d.", mMessageId);
        Message msg = mMailbox.getMessageById(null, mMessageId);
        updateTagsAndFlagsIfNecessary(msg, flagActions, tags);
        mKept = true;
        return msg;
    }
    
    private void updateTagsAndFlagsIfNecessary(Message msg, Collection<ActionFlag> flagActions, String tagString)
    throws ServiceException {
        long tags = msg.getTagBitmask() | Tag.tagsToBitmask(tagString);
        int flags = getFlagBitmask(msg, flagActions);
        if (msg.getTagBitmask() != tags || msg.getFlagBitmask() != flags) {
            ZimbraLog.filter.info("Updating flags to %d, tags to %d on message %d.",
                flags, tags, msg.getId());
            mMailbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE, flags, tags);
            mFiltered = true;
        }
    }
    
    /**
     * Applies flag actions to the given <tt>Message</tt>'s existing flags
     * and returns the result.  Does not modify the message.
     */
    private int getFlagBitmask(Message msg, Collection<ActionFlag> flagActions)
    throws ServiceException {
        int flags = msg.getFlagBitmask();
        for (ActionFlag action : flagActions) {
            Flag flag = mMailbox.getFlagById(action.getFlagId());
            if (action.isSetFlag()) {
                flags |= flag.getBitmask();
            } else {
                flags &= ~flag.getBitmask();
            }
        }
        return flags;
    }
    
    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String tags) throws ServiceException {
        Message source = mMailbox.getMessageById(null, mMessageId);
        
        // See if the message is already in the target folder.
        Folder targetFolder = null;
        try {
            targetFolder = mMailbox.getFolderByPath(null, folderPath);
        } catch (NoSuchItemException e) {
        }
        if (targetFolder != null && source.getFolderId() == targetFolder.getId()) {
            ZimbraLog.filter.debug("Ignoring fileinto action for message %d.  It is already in %s.",
                mMessageId, folderPath);
            updateTagsAndFlagsIfNecessary(source, flagActions, tags);
            return null;
        }
        
        ZimbraLog.filter.info("Copying existing message %d to folder %s.", mMessageId, folderPath);
        if (isLocalExistingFolder(folderPath)) {
            // Copy item into to a local folder.
            Folder target = mMailbox.getFolderByPath(null, folderPath);
            Message newMsg = (Message) mMailbox.copy(null, mMessageId, MailItem.TYPE_MESSAGE, target.getId());
            mFiltered = true;
            mFiled = true;

            // Apply flags and tags
            int flagBits = source.getFlagBitmask();
            long tagBits = Tag.tagsToBitmask(tags);
            mMailbox.setTags(null, newMsg.getId(), MailItem.TYPE_MESSAGE,
                source.getFlagBitmask() | flagBits, source.getTagBitmask() | tagBits);
            return new ItemId(mMailbox, mMessageId);
        }
        
        ItemId id = FilterUtil.addMessage(new DeliveryContext(), mMailbox, getParsedMessage(),
            mMailbox.getAccount().getName(), folderPath, getFlagBitmask(source, flagActions), tags);
        if (id != null) {
            mFiltered = true;
            mFiled = true;
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
        if (mFiled && !mKept) {
            ZimbraLog.filter.info("Deleting original message %d after filing to another folder.", mMessageId);
            mMailbox.delete(null, mMessageId, MailItem.TYPE_MESSAGE);
        }
    }

    public int getMessageSize() {
        return mSize;
    }
}
