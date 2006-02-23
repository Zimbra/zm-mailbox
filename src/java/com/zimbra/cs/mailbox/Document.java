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

import java.io.InputStream;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.service.ServiceException;


/**
 * @author dkarp
 */
public class Document extends MailItem {

	long   mVersion;
    String mContentType;
    String mFragment;

	public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
	}

	public String getFragment() {
    	return mFragment;
	}
	
	public long getVersion() {
		return mVersion;
	}
	
	public String getFilename() {
		return getSubject();
	}
	
    public String getContentType() {
    	return mContentType;
    }

    public InputStream getRawDocument() throws ServiceException {
        return MessageCache.getRawContent(this);
    }

    synchronized static long getNextVersion(Document parent) {
    	if (parent != null) {
    		return parent.getVersion() + 1;
    	} else {
    		return 1;
    	}
    }

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return false; }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }

    protected static UnderlyingData prepareCreate(byte tp, int id, Folder folder, short volumeId, String subject, String type, int length, Document parent, Metadata meta) 
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

		Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = tp;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.size        = length;
        data.subject     = subject;
        data.blobDigest  = subject;
       	data.metadata    = encodeMetadata(meta, DEFAULT_COLOR, type, getNextVersion(parent)).toString();
        
        return data;
    }

    static Document create(int id, Folder folder, short volumeId, String filename, String type, int length, MailItem parent)
    throws ServiceException {
    	assert(id != Mailbox.ID_AUTO_INCREMENT);
    	assert(parent instanceof Document);

        UnderlyingData data = prepareCreate(TYPE_DOCUMENT, id, folder, volumeId, filename, type, length, (Document) parent, null);
        if (parent != null)
            data.parentId = parent.getId();

        Mailbox mbox = folder.getMailbox();
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(null);
//        doc.reindex();
        return doc;
    }

    @Override 
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mContentType = meta.get(Metadata.FN_MIME_TYPE);
        mVersion = Long.valueOf(meta.get(Metadata.FN_VERSION));
    }

    @Override 
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mContentType, mVersion);
    }
    
    static Metadata encodeMetadata(Metadata meta, byte color, String mimeType, long version) {
    	if (meta == null) {
    		meta = new Metadata();
    	}
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_VERSION, Long.toString(version));
        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";

    @Override 
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
