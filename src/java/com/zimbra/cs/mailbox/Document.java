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

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;


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
		public Blob getBlob() throws ServiceException {
	    	StoreManager sm = StoreManager.getInstance();
	        return sm.getMailboxBlob(mParent.getMailbox(), mParent.getId(), getRevId(), mParent.getVolumeId()).getBlob();
		}
		public String getFragment() throws ServiceException {
	    	return mRev.get(Metadata.FN_FRAGMENT);
		}
	};
	
    protected String mContentType;
    protected String mFragment;
    protected MetadataList mRevisionList;

	public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
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
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexDocument(redo, mMailbox.getMailboxIndex(), mId, this);
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
    
    private static Metadata getRevisionMetadata(Mailbox mbox, String author, byte[] contents) throws ServiceException {
    	Metadata rev = new Metadata();
    	rev.put(Metadata.FN_REV_ID, mbox.getOperationChangeID());
    	rev.put(Metadata.FN_CREATOR, author);
    	rev.put(Metadata.FN_REV_DATE, mbox.getOperationTimestampMillis());
    	rev.put(Metadata.FN_REV_SIZE, contents.length);
        rev.put(Metadata.FN_FRAGMENT, Fragment.getFragment(contents));
    	return rev;
    }
    
    public synchronized void addRevision(String author, byte[] contents) throws ServiceException {
    	Metadata rev = getRevisionMetadata(mMailbox, author, contents);
    	rev.put(Metadata.FN_VERSION, getNextVersion());
    	mFragment = rev.get(Metadata.FN_FRAGMENT);
    	mRevisionList.add(rev);
    	mData.size = contents.length;
        mData.contentChanged(mMailbox);
        DbMailItem.saveMetadata(this, contents.length, encodeMetadata(new Metadata()).toString());
        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_DATE | Change.MODIFIED_CONTENT);
    }
    
    protected static UnderlyingData prepareCreate(byte tp, int id, Folder folder, short volumeId, String subject, String creator, String type, byte[] contents, Document parent, Metadata meta) 
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

		Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
    	MetadataList revisions = new MetadataList();
    	Metadata rev = getRevisionMetadata(mbox, creator, contents);
    	rev.put(Metadata.FN_VERSION, 1);
    	revisions.add(rev);
        data.id          = id;
        data.type        = tp;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.size        = contents.length;
        data.subject     = subject;
        data.blobDigest  = subject;
       	data.metadata    = encodeMetadata(meta, DEFAULT_COLOR, type, rev.get(Metadata.FN_FRAGMENT), revisions).toString();
        
        return data;
    }

    static Document create(int id, Folder folder, short volumeId, String filename, String creator, String type, byte[] contents, MailItem parent)
    throws ServiceException {
    	assert(id != Mailbox.ID_AUTO_INCREMENT);
    	assert(parent instanceof Document);

        UnderlyingData data = prepareCreate(TYPE_DOCUMENT, id, folder, volumeId, filename, creator, type, contents, (Document) parent, null);
        if (parent != null)
            data.parentId = parent.getId();

        Mailbox mbox = folder.getMailbox();
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
//        doc.reindex();
        return doc;
    }

    @Override 
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mContentType = meta.get(Metadata.FN_MIME_TYPE);
        mFragment = meta.get(Metadata.FN_FRAGMENT);
        mRevisionList = meta.getList(Metadata.FN_REVISIONS);
    }

    @Override 
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mContentType, mFragment, mRevisionList);
    }
    
    static Metadata encodeMetadata(Metadata meta, byte color, String mimeType, String fragment, MetadataList revisions) {
    	if (meta == null) {
    		meta = new Metadata();
    	}
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_FRAGMENT, fragment);
        meta.put(Metadata.FN_REVISIONS, revisions);
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
