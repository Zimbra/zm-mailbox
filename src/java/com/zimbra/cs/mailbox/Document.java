/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.StoreManager;


/**
 * @author dkarp
 */
public class Document extends MailItem {

    private String  mContentType;
    private String  mFragment;
    private HashMap mAttributes;

	public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
        if (mData.type != TYPE_DOCUMENT)
            throw new IllegalArgumentException();
	}

    private String getContentType() {
    	return mContentType;
    }

    public InputStream getRawDocument() throws ServiceException {
        return MessageCache.getRawContent(this);
    }


    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
	boolean isMutable()       { return false; }
    boolean isIndexed()       { return true; }
	boolean canHaveChildren() { return false; }


    static Document create(int id, Folder folder, short volumeId, String filename, String type, File content, MailItem parent)
    throws ServiceException {
    	assert(id != Mailbox.ID_AUTO_INCREMENT);
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        if (content == null)
            throw ServiceException.INVALID_REQUEST("content may not be empty", null);

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_DOCUMENT;
        if (parent != null)
            data.parentId = parent.getId();
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.size        = (int) content.length();
        data.subject     = filename;
        data.metadata    = encodeMetadata(DEFAULT_COLOR, type, null);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
//        doc.reindex();
        return doc;
    }


    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mFragment    = meta.get(Metadata.FN_FRAGMENT, null);
        mContentType = meta.get(Metadata.FN_MIME_TYPE, Mime.CT_DEFAULT);
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mContentType, mFragment);
    }
    private static String encodeMetadata(byte color, String contentType, String fragment) {
        return encodeMetadata(new Metadata(), color, contentType, fragment).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, String contentType, String fragment) {
        meta.put(Metadata.FN_FRAGMENT,  fragment);
        meta.put(Metadata.FN_MIME_TYPE, contentType);
        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("message: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_MIME_TYPE).append(": ").append(mContentType).append(", ");
        sb.append(CN_FRAGMENT).append(": ").append(mFragment);
        sb.append("}");
        return sb.toString();
    }
}
