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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.InputStream;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.service.ServiceException;

public class WikiItem extends Document {
	
	String mWikiWord;
	
	WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
	}
	
	@Override 
	Metadata encodeMetadata(Metadata meta) {
		meta.put(Metadata.FN_WIKI_WORD, mWikiWord);
		return super.encodeMetadata(meta);
	}
	
	public String getWikiWord() {
		return getFilename();
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
    
    @Override 
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mWikiWord = meta.get(Metadata.FN_WIKI_WORD);
    }

    public static final String WIKI_CONTENT_TYPE = "text/html";
	
    static WikiItem create(int id, Folder folder, short volumeId, String wikiword, String author, ParsedDocument pd, MailItem parent)
    throws ServiceException {

        Metadata meta = new Metadata();
		meta.put(Metadata.FN_WIKI_WORD, wikiword);
		
        UnderlyingData data = prepareCreate(TYPE_WIKI, id, folder, volumeId, wikiword, author, WIKI_CONTENT_TYPE, pd, (Document)parent, meta);

		Mailbox mbox = folder.getMailbox();
		data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        WikiItem wiki = new WikiItem(mbox, data);
        wiki.finishCreation(parent);
        pd.setVersion(wiki.getVersion());
        return wiki;
    }

    private static final String CN_WIKIWORD = "wikiword";
    private static final String CN_EDITOR    = "edited_by";
    private static final String CN_VERSION   = "version";
    
    @Override 
    public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("wikiitem: {");
            sb.append(CN_WIKIWORD).append(": ").append(getWikiWord()).append(", ");
            sb.append(CN_EDITOR).append(": ").append(getCreator()).append(", ");
            sb.append(CN_VERSION).append(": ").append(getVersion()).append(", ");
            appendCommonMembers(sb).append(", ");
            sb.append("}");
        } catch (ServiceException se) {
        }
        return sb.toString();
    }
}
