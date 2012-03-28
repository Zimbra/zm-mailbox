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

import com.google.common.base.Objects;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;

public class Comment extends MailItem {

    Comment(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
    }

    public static Comment create(Mailbox mbox, MailItem parent, int id, String uuid, String text, String creatorId, CustomMetadata custom) throws ServiceException {
        if (!(parent instanceof Document))
            throw MailServiceException.CANNOT_PARENT();

        UnderlyingData data = new UnderlyingData();
        data.uuid = uuid;
        data.id = id;
        data.type = Type.COMMENT.toByte();
        data.folderId = Mailbox.ID_FOLDER_COMMENTS;
        data.parentId = parent.mId;
        data.setSubject(text);
        data.date = mbox.getOperationTimestamp();
        data.size = text.length();
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, creatorId, custom);
        data.contentChanged(mbox);

        new DbMailItem(mbox).create(data);
        Comment comment = new Comment(mbox, data);
        comment.finishCreation(parent);

        return comment;
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
        return true;
    }

    @Override
    boolean isMovable() {
        return true;
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

    /**
     * The creator of the comment has full permission.
     */
    @Override
    protected boolean hasFullPermission(Account authuser) {
        if (authuser != null && authuser.getId().equals(mCreatorId))
            return true;
        return super.hasFullPermission(authuser);
    }

    private String mCreatorId;

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, mCreatorId, mExtendedData);
    }

    private static String encodeMetadata(Color color, int metaVersion, int version, String accountId, CustomMetadata custom) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, metaVersion, version, accountId, extended).toString();
    }

    private static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, String accountId, CustomMetadataList extended) {
        meta.put(Metadata.FN_CREATOR, accountId);
        return encodeMetadata(meta, color, null, metaVersion, version, extended);
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        if (meta == null)
            return;
        mCreatorId = meta.get(Metadata.FN_CREATOR);
    }

    @Override
    public String toString() {
        Objects.ToStringHelper helper = Objects.toStringHelper(this);
        appendCommonMembers(helper);
        helper.add("type", getType());
        helper.add("creator", mCreatorId);
        helper.add("text", getSubject());
        try {
            helper.add("parent", getParent());
        } catch (ServiceException e) {
        }
        return helper.toString();
    }
}
