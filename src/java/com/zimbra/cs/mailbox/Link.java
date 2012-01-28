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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;

public class Link extends Document {

    public Link(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
    }

    static Link create(Folder parent, int id, String uuid, String name, String ownerId, int remoteId, CustomMetadata custom) throws ServiceException {
        if (!parent.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the parent folder");
        }
        if (!parent.canContain(Type.LINK)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        name = validateItemName(name);
        Mailbox mbox = parent.getMailbox();

        UnderlyingData data = new UnderlyingData();
        data.uuid = uuid;
        data.id = id;
        data.type = Type.MOUNTPOINT.toByte();
        data.folderId = parent.getId();
        data.parentId = data.folderId;
        data.date = mbox.getOperationTimestamp();
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, ownerId, remoteId, custom);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding Link %s: id=%d, parentId=%d, parentName=%s.",
                name, data.id, parent.getId(), parent.getName());
        new DbMailItem(mbox).create(data);
        Link link = new Link(mbox, data);
        link.finishCreation(parent);
        return link;
    }
    
    private String mOwnerId;
    private int mRemoteId;
    
    public String getOwnerId() {
        return mOwnerId;
    }
    
    public int getRemoteId() {
        return mRemoteId;
    }
    
    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, mOwnerId, mRemoteId, mExtendedData);
    }
    
    private static String encodeMetadata(Color color, int metaVersion, int version, String owner, int remoteId, CustomMetadata custom) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, metaVersion, version, owner, remoteId, extended).toString();
    }
    
    private static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, String owner, int remoteId, CustomMetadataList extended) {
        meta.put(Metadata.FN_ACCOUNT_ID, owner);
        meta.put(Metadata.FN_REMOTE_ID, remoteId);
        return encodeMetadata(meta, color, null, metaVersion, version, extended);
    }
    
    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        if (meta == null)
            return;
        mOwnerId = meta.get(Metadata.FN_ACCOUNT_ID);
        mRemoteId = (int)meta.getLong(Metadata.FN_REMOTE_ID);
    }

    @Override
    public String toString() {
        Objects.ToStringHelper helper = Objects.toStringHelper(this);
        appendCommonMembers(helper);
        helper.add("type", getType());
        helper.add("owner", mOwnerId);
        helper.add("remoteId", mRemoteId);
        helper.add("text", getSubject());
        return helper.toString();
    }
}
