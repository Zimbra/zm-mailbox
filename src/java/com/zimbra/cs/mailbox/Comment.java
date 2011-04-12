/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;

public class Comment extends MailItem {

    Comment(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
    }

    public static Comment create(Mailbox mbox, MailItem parent, int id, String text, String creatorId, CustomMetadata custom) throws ServiceException {
        if (!(parent instanceof Document))
            throw MailServiceException.CANNOT_PARENT();
        
        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.COMMENT.toByte();
        data.folderId = Mailbox.ID_FOLDER_COMMENTS;
        data.parentId = parent.mId;
        data.setSubject(text);
        data.date = mbox.getOperationTimestamp();
        data.size = text.length();
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, creatorId, custom);
        data.contentChanged(mbox);
        
        new DbMailItem(mbox).create(data);
        Comment comment = new Comment(mbox, data);
        comment.finishCreation(parent);
        
        return comment;
    }
    
    public static Collection<Comment> getComments(Mailbox mbox, MailItem parent, int offset, int length) throws ServiceException {
        ArrayList<Comment> comments = loadComments(mbox, parent);
        if (comments.size() <= offset) {
            return Collections.emptyList();
        }
        int last = length == -1 ? comments.size() : Math.min(comments.size(), offset + length);
        return comments.subList(offset, last);
    }
    
    private static ArrayList<Comment> loadComments(Mailbox mbox, MailItem parent) throws ServiceException {
        List<UnderlyingData> listData = DbMailItem.getByParent(parent, SortBy.DATE_DESC);
        ArrayList<Comment> ret = new ArrayList<Comment>();
        for (UnderlyingData data : listData) {
            MailItem item = mbox.getItem(data);
            if (item instanceof Comment) {
                ret.add((Comment)item);
            }
        }
        // cache the array somewhere
        return ret;
    }

    
    @Override
    public String getSender() {
        return "";
    }

    @Override
    boolean isTaggable() {
        return true;
    }

    @Override
    boolean isCopyable() {
        return false;
    }

    @Override
    boolean isMovable() {
        return false;
    }

    @Override
    boolean isMutable() {
        return false;
    }

    @Override
    boolean canHaveChildren() {
        // return true if we want comments to be made on comments
        return false;
    }

    /**
     * Content of the comment with the maximum of 1024 bytes.
     * 
     * @return
     */
    public String getText() {
        return getSubject();
    }
    
    public String getCreatorAccountId() {
        return mCreatorId;
    }
    
    public Account getCreatorAccount() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        return prov.getAccountById(mCreatorId);
    }
    
    private String mCreatorId;
    
    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mCreatorId, mExtendedData);
    }
    
    private static String encodeMetadata(Color color, int version, String accountId, CustomMetadata custom) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, version, accountId, extended).toString();
    }
    
    private static Metadata encodeMetadata(Metadata meta, Color color, int version, String accountId, CustomMetadataList extended) {
        meta.put(Metadata.FN_CREATOR, accountId);
        return encodeMetadata(meta, color, version, extended);
    }
    
    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        if (meta == null)
            return;
        mCreatorId = meta.get(Metadata.FN_CREATOR);
    }
}
