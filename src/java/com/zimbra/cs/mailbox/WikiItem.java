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
package com.zimbra.cs.mailbox;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;

public class WikiItem extends Document {
	
	WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
	}
	
	@Override boolean isTaggable() { return true; }
	@Override boolean isCopyable() { return true; }
	@Override boolean isMovable() { return true; }
	@Override boolean isMutable() { return false; }
	@Override boolean isIndexed() { return true; }
	@Override boolean canHaveChildren() { return false; }

	@Override 
	Metadata encodeMetadata(Metadata meta) {
		return encodeMetadata(meta, mColor);
	}
    static String encodeMetadata(byte color, Map fields) {
        return encodeMetadata(new Metadata(), color, fields).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, Map fields) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        return Document.encodeMetadata(meta, color);
    }
	
    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
    	// XXX implement me
    }

	public String getWikiWord() {
		return mFields.get(Metadata.FN_WIKI_WORD);
	}
	
	public String getCreator() {
		return mFields.get(Metadata.FN_CREATOR);
	}
	
	public long getCreatedTime() {
		return getDate();
	}
	
    public byte[] getMessageContent() throws ServiceException {
        return MessageCache.getItemContent(this);
    }

    public InputStream getRawMessage() throws ServiceException {
        return MessageCache.getRawContent(this);
    }
    
	public static final String WIKI_CONTENT_TYPE = "text/html";
	
    static WikiItem create(int id, Folder folder, short volumeId, String wikiword, String author, int length, MailItem parent)
    throws ServiceException {

        Map<String,String> fields = new HashMap<String,String>();
		fields.put(Metadata.FN_WIKI_WORD, wikiword);
		fields.put(Metadata.FN_CREATOR, author);
		
        UnderlyingData data = prepareCreate(fields, TYPE_WIKI, id, folder, volumeId, wikiword, WIKI_CONTENT_TYPE, length);
        if (parent != null)
            data.parentId = parent.getId();

		Mailbox mbox = folder.getMailbox();
		data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        WikiItem wiki = new WikiItem(mbox, data);
        wiki.finishCreation(parent);
//        doc.reindex();
        return wiki;
    }
}
