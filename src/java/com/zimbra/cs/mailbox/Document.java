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

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ZimbraLog;


/**
 * @author dkarp
 */
public class Document extends MailItem {

	public static class DocumentRevision {
		private Document mParent;
		private Metadata mRev;
		
		public DocumentRevision(Document parent, Metadata rev) {
			mParent = parent;
			mRev = rev;
		}
		public int getRevId() throws ServiceException {
			return (int)mRev.getLong(Metadata.FN_REV_ID);
		}
		public String getCreator() throws ServiceException {
			return mRev.get(Metadata.FN_CREATOR);
		}
		public long getRevDate() throws ServiceException {
			return mRev.getLong(Metadata.FN_REV_DATE);
		}
		public long getRevSize() throws ServiceException {
			return mRev.getLong(Metadata.FN_REV_SIZE);
		}
		public long getVersion() throws ServiceException {
			return mRev.getLong(Metadata.FN_VERSION);
		}
		public InputStream getContent() throws ServiceException,IOException {
	    	StoreManager sm = StoreManager.getInstance();
	        return sm.getContent(sm.getMailboxBlob(mParent.getMailbox(), mParent.getId(), getRevId(), mParent.getVolumeId()));
		}
		public String getFragment() throws ServiceException {
	    	return mRev.get(Metadata.FN_FRAGMENT);
		}
	}
	
    protected String mContentType;
    protected String mFragment;
    protected MetadataList mRevisionList;

	public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
		mFragment = getLastRevision().getFragment();
	}

	public String getFragment() {
    	return mFragment;
	}
	
	public String getFilename() {
		return getSubject();
	}
	
    public String getContentType() {
    	return mContentType;
    }

	public String getCreator() throws ServiceException {
		return getLastRevision().getCreator();
	}
	
    public InputStream getRawDocument() throws IOException, ServiceException {
    	StoreManager sm = StoreManager.getInstance();
    	int revId = getLastRevision().getRevId();
        mBlob = sm.getMailboxBlob(mMailbox, mId, revId, mData.volumeId);
        if (mBlob == null)
            throw ServiceException.FAILURE("missing blob for id: " + getId(), null);
        return sm.getContent(mBlob);
    }

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }

    @Override
    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        if (!DebugConfig.disableIndexing &&
        		indexData != null &&
        		indexData instanceof ParsedDocument)
            Indexer.GetInstance().indexDocument(redo, mMailbox.getMailboxIndex(), mId, (ParsedDocument) indexData);
    }

    public DocumentRevision getRevision(int rev) throws ServiceException {
    	return new DocumentRevision(this, mRevisionList.getMap(rev-1));
    }
    
    public DocumentRevision getLastRevision() throws ServiceException {
    	return getRevision(mRevisionList.size());
    }
    
    public int getVersion() {
    	return mRevisionList.size();
    }
    
    public int getNextVersion() {
    	return getVersion() + 1;
    }
    
    private static Metadata getRevisionMetadata(int changeID, String author, ParsedDocument pd) throws ServiceException {
    	Metadata rev = new Metadata();
    	rev.put(Metadata.FN_REV_ID, changeID);
    	rev.put(Metadata.FN_CREATOR, author);
    	rev.put(Metadata.FN_REV_DATE, pd.getCreatedDate());
    	rev.put(Metadata.FN_REV_SIZE, pd.getSize());
        rev.put(Metadata.FN_FRAGMENT, pd.getFragment());
    	return rev;
    }
    
    public synchronized void addRevision(String author, ParsedDocument pd) throws ServiceException {
    	Metadata rev = getRevisionMetadata(mMailbox.getOperationChangeID(), author, pd);
    	rev.put(Metadata.FN_VERSION, getNextVersion());
    	mRevisionList.add(rev);
    	mFragment = pd.getFragment();
    	mData.size = pd.getSize();
        mData.contentChanged(mMailbox);
        mMailbox.updateSize(mData.size);
        DbMailItem.saveMetadata(this, pd.getSize(), encodeMetadata(new Metadata()).toString());
        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_DATE | Change.MODIFIED_CONTENT);
        pd.setVersion(getVersion());
        MessageCache.purge(this);
        mBlob = null;
    }
    
    public synchronized void purgeOldRevisions(int revToKeep) throws ServiceException, IOException {
    	int last = mRevisionList.size() - revToKeep;
    	StoreManager sm = StoreManager.getInstance();
    	while (last > 0) {
    		last--;
    		Metadata rev = mRevisionList.getMap(last);
    		if (rev == null) {
    			ZimbraLog.wiki.error("cannot find revision " + last + " in metadata " + getFilename());
    			continue;
    		}
    		int revid = (int)rev.getLong(Metadata.FN_REV_ID);
    		if (revid == 0)
    			break;
	        sm.delete(sm.getMailboxBlob(getMailbox(), getId(), revid, getVolumeId()));
	        rev.put(Metadata.FN_REV_ID, 0);
	        mRevisionList.mList.set(last, rev.mMap);  // rev is a copy.
    	}
        DbMailItem.saveMetadata(this, getSize(), encodeMetadata(new Metadata()).toString());
    }
    
    protected static UnderlyingData prepareCreate(byte tp, int id, Folder folder, short volumeId, String subject, String creator, String type, ParsedDocument pd, Document parent, Metadata meta) 
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

		Mailbox mbox = folder.getMailbox();
    	MetadataList revisions = new MetadataList();
    	Metadata rev = getRevisionMetadata(mbox.getOperationChangeID(), creator, pd);
    	rev.put(Metadata.FN_VERSION, 1);
    	revisions.add(rev);

    	UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = tp;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.imapId      = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.size        = pd.getSize();
        data.subject     = subject;
        data.blobDigest  = subject;
       	data.metadata    = encodeMetadata(meta, DEFAULT_COLOR, type, revisions).toString();
        
        return data;
    }

    static Document create(int id, Folder folder, short volumeId, String filename, String creator, String type, ParsedDocument pd, MailItem parent)
    throws ServiceException {
    	assert(id != Mailbox.ID_AUTO_INCREMENT);

        UnderlyingData data = prepareCreate(TYPE_DOCUMENT, id, folder, volumeId, filename, creator, type, pd, (Document) parent, null);
        if (parent != null)
            data.parentId = parent.getId();

        Mailbox mbox = folder.getMailbox();
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
        pd.setVersion(doc.getVersion());
        return doc;
    }

    @Override 
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mContentType = meta.get(Metadata.FN_MIME_TYPE);
        mRevisionList = meta.getList(Metadata.FN_REVISIONS);
    }

    @Override 
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mContentType, mRevisionList);
    }
    
    static Metadata encodeMetadata(Metadata meta, byte color, String mimeType, MetadataList revisions) {
    	if (meta == null) {
    		meta = new Metadata();
    	}
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_REVISIONS, revisions);
        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";
    private static final String CN_FILE_NAME = "filename";
    private static final String CN_EDITOR    = "edited_by";
    private static final String CN_VERSION   = "version";

    @Override 
    public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("document: {");
            sb.append(CN_FILE_NAME).append(": ").append(getFilename()).append(", ");
            sb.append(CN_EDITOR).append(": ").append(getCreator()).append(", ");
            sb.append(CN_VERSION).append(": ").append(getVersion()).append(", ");
            sb.append(CN_MIME_TYPE).append(": ").append(mContentType).append(", ");
            sb.append(CN_FRAGMENT).append(": ").append(mFragment);
            appendCommonMembers(sb).append(", ");
            sb.append("}");
        } catch (ServiceException se) {
        }
        return sb.toString();
    }
}
