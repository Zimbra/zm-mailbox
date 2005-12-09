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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;


/**
 * @author dkarp
 */
public class Document extends MailItem {

	Map<String,String> mFields;
	long   mVersion;
    String mContentType;
    String mFragment;

	public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
        if (mData.type != TYPE_DOCUMENT)
            throw new IllegalArgumentException();
	}

	public String getFragment() {
    	return mFields.get(Metadata.FN_FRAGMENT);
	}
	
	public long getVersion() {
		return Long.valueOf(mFields.get(Metadata.FN_VERSION));
	}
	
	public String getFilename() {
		return getSubject();
	}
	
    public String getContentType() {
    	return mFields.get(Metadata.FN_MIME_TYPE);
    }

    public InputStream getRawDocument() throws ServiceException {
        return MessageCache.getRawContent(this);
    }

    synchronized static long getNextVersion() {
    	return 0;  // XXX implement it
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

		Map<String,String> fields = new HashMap<String,String>();
        fields.put(Metadata.FN_MIME_TYPE, type);
        fields.put(Metadata.FN_VERSION, Long.toString(getNextVersion()));

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
        data.metadata    = encodeMetadata(DEFAULT_COLOR, fields);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
//        doc.reindex();
        return doc;
    }

    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        Metadata metaAttrs = meta.getMap(Metadata.FN_FIELDS);

        mFields = new HashMap<String,String>();
        for (Iterator it = metaAttrs.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            mFields.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor);
    }
    static String encodeMetadata(byte color, Map fields) {
        return encodeMetadata(new Metadata(), color, fields).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, Map fields) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
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
