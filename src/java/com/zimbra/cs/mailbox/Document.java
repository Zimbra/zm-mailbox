/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
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

    public InputStream getRawDocument() throws IOException, ServiceException {
        MailboxBlob blob = getBlob();
        if (blob == null) {
            StringBuffer sb = new StringBuffer("Missing document content: mailbox=");
            sb.append(getMailboxId()).append(", id=").append(mId);
            throw ServiceException.FAILURE(sb.toString(), null);
        }
        return StoreManager.getInstance().getContent(blob);
    }


    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
	boolean isMutable()       { return false; }
    boolean isIndexed()       { return true; }
	boolean canHaveChildren() { return false; }


    static Document create(int id, Folder folder, String filename, String type, File content, MailItem parent) throws ServiceException {
    	assert(id != Mailbox.ID_AUTO_INCREMENT);
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (content == null || content.equals(""))
            throw ServiceException.INVALID_REQUEST("document may not be empty", null);

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_DOCUMENT;
        if (parent != null)
            data.parentId = parent.getId();
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.date        = mbox.getOperationTimestamp();
        data.size        = (int) content.length();
        data.subject     = filename;
        data.metadata    = encodeMetadata(type, null);
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
//        doc.reindex();
        return doc;
    }


    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);
        mFragment    = meta.get(Metadata.FN_FRAGMENT, null);
        mContentType = meta.get(Metadata.FN_MIME_TYPE, Mime.CT_DEFAULT);
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mContentType, mFragment);
    }
    private static String encodeMetadata(String contentType, String fragment) {
        Metadata meta = new Metadata();
        meta.put(Metadata.FN_FRAGMENT,  fragment);
        meta.put(Metadata.FN_MIME_TYPE, contentType);
        return meta.toString();
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
