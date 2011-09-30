/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.service.util.ItemId;

/**
 * Mail filtering implementation for messages already in the user's mailbox.
 */
public final class ExistingMessageHandler implements FilterHandler {

    private final OperationContext octxt;
    private final Mailbox mailbox;
    private final int messageId;
    private final int size;
    private Message message;
    private MimeMessage mimeMessage;
    private ParsedMessage parsedMessage;
    private boolean kept = false;
    private boolean filed = false;
    private boolean filtered = false;

    public ExistingMessageHandler(OperationContext octxt, Mailbox mbox, int messageId, int size) {
        this.octxt = octxt;
        this.mailbox = mbox;
        this.messageId = messageId;
        this.size = size;
    }

    @Override
    public String getDefaultFolderPath() throws ServiceException {
        return getDefaultFolder().getPath();
    }

    private Folder getDefaultFolder() throws ServiceException {
        return mailbox.getFolderById(octxt, Mailbox.ID_FOLDER_INBOX);
    }

    @Override
    public Message getMessage() throws ServiceException {
        if (message == null) {
            message = mailbox.getMessageById(octxt, messageId);
        }
        return message;
    }

    @Override
    public MimeMessage getMimeMessage() throws ServiceException {
        if (mimeMessage == null) {
            mimeMessage = getMessage().getMimeMessage();
        }
        return mimeMessage;
    }

    @Override
    public ParsedMessage getParsedMessage() throws ServiceException {
        if (parsedMessage == null) {
            Message msg = getMessage();
            ParsedMessageOptions opt = new ParsedMessageOptions()
                .setContent(msg.getMimeMessage())
                .setAttachmentIndexing(mailbox.attachmentsIndexingEnabled())
                .setSize(msg.getSize())
                .setDigest(msg.getDigest());
            parsedMessage = new ParsedMessage(opt);
        }
        return parsedMessage;
    }

    public boolean filtered() {
        return filtered;
    }

    @Override
    public void discard() throws ServiceException {
        ZimbraLog.filter.info("Discarding existing message with id %d.", messageId);
        mailbox.delete(octxt, messageId, MailItem.Type.MESSAGE);
        filtered = true;
    }


    @Override
    public Message implicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException {
        ZimbraLog.filter.debug("Implicitly keeping existing message %d.", messageId);
        Message msg = getMessage();
        updateTagsAndFlagsIfNecessary(msg, flagActions, tags);
        kept = true;
        return msg;
    }

    @Override
    public Message explicitKeep(Collection<ActionFlag> flagActions, String[] tags) throws ServiceException {
        ZimbraLog.filter.debug("Explicitly keeping existing message %d.", messageId);
        Message msg = getMessage();
        updateTagsAndFlagsIfNecessary(msg, flagActions, tags);
        kept = true;
        return msg;
    }

    private void updateTagsAndFlagsIfNecessary(Message msg, Collection<ActionFlag> flagActions, String[] newTags)
            throws ServiceException {
        String[] existingTags = msg.getTags();
        String[] tags = FilterUtil.getTagsUnion(existingTags, newTags);
        int flags = FilterUtil.getFlagBitmask(flagActions, msg.getFlagBitmask());
        if (!Sets.newHashSet(existingTags).equals(Sets.newHashSet(tags)) || msg.getFlagBitmask() != flags) {
            ZimbraLog.filter.info("Updating flags to %d, tags to %s on message %d.", flags, tags, msg.getId());
            mailbox.setTags(octxt, msg.getId(), MailItem.Type.MESSAGE, flags, tags);
            filtered = true;
        }
    }

    @Override
    public ItemId fileInto(String folderPath, Collection<ActionFlag> flagActions, String[] tags)
            throws ServiceException {
        Message source = getMessage();

        // See if the message is already in the target folder.
        Folder targetFolder = null;
        try {
            targetFolder = mailbox.getFolderByPath(octxt, folderPath);
        } catch (NoSuchItemException ignored) {
        }
        if (targetFolder != null && source.getFolderId() == targetFolder.getId()) {
            ZimbraLog.filter.debug("Ignoring fileinto action for message %d.  It is already in %s.",
                messageId, folderPath);
            updateTagsAndFlagsIfNecessary(source, flagActions, tags);
            return null;
        }

        ZimbraLog.filter.info("Copying existing message %d to folder %s.", messageId, folderPath);
        if (isLocalExistingFolder(folderPath)) {
            // Copy item into to a local folder.
            Folder target = mailbox.getFolderByPath(octxt, folderPath);
            Message newMsg = (Message) mailbox.copy(octxt, messageId, MailItem.Type.MESSAGE, target.getId());
            filtered = true;
            filed = true;

            // Apply flags and tags
            mailbox.setTags(octxt, newMsg.getId(), MailItem.Type.MESSAGE,
                    FilterUtil.getFlagBitmask(flagActions, source.getFlagBitmask()),
                    FilterUtil.getTagsUnion(source.getTags(), tags));
            return new ItemId(mailbox, messageId);
        }

        ItemId id = FilterUtil.addMessage(new DeliveryContext(), mailbox, getParsedMessage(),
                                          mailbox.getAccount().getName(), folderPath, false,
                                          FilterUtil.getFlagBitmask(flagActions, source.getFlagBitmask()),
                                          tags, Mailbox.ID_AUTO_INCREMENT, octxt);
        if (id != null) {
            filtered = true;
            filed = true;
        }
        return id;
    }

    /**
     * Returns <tt>true</tt> if the folder path exists and is local to this mailbox.
     */
    private boolean isLocalExistingFolder(String folderPath)
    throws ServiceException {
        Pair<Folder, String> folderAndPath = mailbox.getFolderByPathLongestMatch(
            octxt, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
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
            messageId, destinationAddress);
    }

    @Override
    public void reply(String bodyTemplate) {
        ZimbraLog.filter.debug("Ignoring attempt to reply to existing message %d", messageId);
    }

    @Override
    public void notify(
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders) {
        ZimbraLog.filter.debug("Ignoring attempt to notify for existing message %d", messageId);
    }

    @Override
    public void beforeFiltering() {
    }

    @Override
    public void afterFiltering() throws ServiceException {
        if (filed && !kept) {
            ZimbraLog.filter.info("Deleting original message %d after filing to another folder.", messageId);
            mailbox.delete(octxt, messageId, MailItem.Type.MESSAGE);
        }
    }

    @Override
    public int getMessageSize() {
        return size;
    }

}
