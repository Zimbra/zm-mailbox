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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;

public class WikiItem extends MailItem {

	public static String WIKI_WORD = "wikiWord";
	public static String AUTHOR = "author";
	public static String MODIFIED_TIME = "modifiedTime";
	
	WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
	}
	
	private Map<String,String> mFields;
	
	@Override boolean isTaggable() { return false; }
	@Override boolean isCopyable() { return false; }
	@Override boolean isMovable() { return false; }
	@Override boolean isMutable() { return false; }
	@Override boolean isIndexed() { return false; }
	@Override boolean canHaveChildren() { return false; }

	@Override 
	Metadata encodeMetadata(Metadata meta) {
		return encodeMetadata(meta, mColor);
	}
    private static String encodeMetadata(byte color, Map fields) {
        return encodeMetadata(new Metadata(), color, fields).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, Map fields) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        return MailItem.encodeMetadata(meta, color);
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

	public String getWikiWord() {
		return mFields.get(WIKI_WORD);
	}
	
	public String getAuthor() {
		return mFields.get(WIKI_WORD);
	}
	
	public String getModifiedTime() {
		return mFields.get(MODIFIED_TIME);
	}
	
	public static WikiItem create(Mailbox mbox, UnderlyingData data, ParsedMessage pm, String author) throws ServiceException {
		Map<String,String> fields = new HashMap<String,String>();
		fields.put(WIKI_WORD, pm.getSubject());
		fields.put(AUTHOR, author);
		fields.put(MODIFIED_TIME, Long.toString(pm.getDateHeader()));

		data.type     = TYPE_WIKI;
        data.sender   = pm.getSender();
        data.subject  = pm.getNormalizedSubject();
        data.metadata = encodeMetadata(DEFAULT_COLOR, fields);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        WikiItem wikiItem = new WikiItem(mbox, data);
        wikiItem.finishCreation(null);
        return wikiItem;
	}
}
