/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class Document extends MailItem {
    protected String mContentType;
    protected String mCreator;
    protected String mFragment;
    protected String mLockOwner;
    protected long   mLockTimestamp;
    protected String mDescription;

    public Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
    }

    public String getContentType() {
        return mContentType;
    }

    @Override
    public String getSender() {
        return getCreator();
    }

    public String getCreator() {
        return mCreator == null ? "" : mCreator;
    }

    public String getFragment() {
    	return mFragment == null ? "" : mFragment;
    }
    
    public String getLockOwner() {
        return mLockOwner;
    }
    
    public long getLockTimestamp() {
        return mLockTimestamp;
    }
    
    public String getDescription() {
        return mDescription == null ? "" : mDescription;
    }

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }

    @Override int getMaxRevisions() throws ServiceException {
        return getAccount().getIntAttr(Provisioning.A_zimbraNotebookMaxRevisions, 0);
    }

    @Override public List<IndexDocument> generateIndexData(boolean doConsistencyCheck) throws MailItem.TemporaryIndexingException {
        ParsedDocument pd = null;
        try {
            MailboxBlob mblob = getBlob();
            if (mblob == null) {
                ZimbraLog.index.warn("Unable to fetch blob for Document id "+mId+" version "+mVersion+" on volume "+getLocator());  
                throw new MailItem.TemporaryIndexingException();
            }

            synchronized (mMailbox) {
                pd = new ParsedDocument(mblob.getLocalBlob(), getName(), getContentType(), getChangeDate(), getCreator(), getDescription());
                if (pd.hasTemporaryAnalysisFailure())
                    throw new MailItem.TemporaryIndexingException();
            }

            IndexDocument doc = pd.getDocument();
            if (doc != null) {
                List<IndexDocument> toRet = new ArrayList<IndexDocument>(1);
                toRet.add(doc);
                return toRet;
            } else {
                return new ArrayList<IndexDocument>(0);
            }
        } catch (IOException e) {
            ZimbraLog.index.warn("Error generating index data for Wiki Document "+getId()+". Item will not be indexed", e);
            return new ArrayList<IndexDocument>(0);
        } catch (ServiceException e) {
            ZimbraLog.index.warn("Error generating index data for Wiki Document "+getId()+". Item will not be indexed", e);
            return new ArrayList<IndexDocument>(0);
        }
    }

    @Override public void reanalyze(Object obj) throws ServiceException {
        if (!(obj instanceof ParsedDocument))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedDocument object", null);
        if ((mData.flags & Flag.BITMASK_UNCACHED) != 0)
            throw ServiceException.FAILURE("cannot reanalyze an old item revision", null);

        ParsedDocument pd = (ParsedDocument) obj;

        mContentType = pd.getContentType();
        mCreator     = pd.getCreator();
        mFragment    = pd.getFragment();
        mData.date    = (int) (pd.getCreatedDate() / 1000L);
        mData.name    = pd.getFilename();
        mData.subject = pd.getFilename();
        mDescription  = pd.getDescription();
        pd.setVersion(getVersion());

        if (mData.size != pd.getSize()) {
            markItemModified(Change.MODIFIED_SIZE);
            mMailbox.updateSize(pd.getSize() - mData.size, false);
            getFolder().updateSize(0, 0, pd.getSize() - mData.size);
            mData.size = pd.getSize();
        }

        saveData(null);
    }

    protected static UnderlyingData prepareCreate(byte type, int id, Folder folder, String name, String mimeType,
                                                  ParsedDocument pd, Metadata meta, CustomMetadata custom) 
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        name = validateItemName(name);

        CustomMetadataList extended = (custom == null ? null : custom.asList());

        Mailbox mbox = folder.getMailbox();

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = type;
        data.folderId    = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId = mbox.generateIndexId(id);
        data.imapId      = id;
        data.date        = (int) (pd.getCreatedDate() / 1000L);
        data.size        = pd.getSize();
        data.name        = name;
        data.subject     = name;
        data.setBlobDigest(pd.getDigest());
        data.metadata    = encodeMetadata(meta, DEFAULT_COLOR_RGB, 1, extended, mimeType, pd.getCreator(), pd.getFragment(), null, 0, pd.getDescription()).toString();
        return data;
    }

    static Document create(int id, Folder folder, String filename, String type, ParsedDocument pd, CustomMetadata custom)
    throws ServiceException {
        assert(id != Mailbox.ID_AUTO_INCREMENT);

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = prepareCreate(TYPE_DOCUMENT, id, folder, filename, type, pd, null, custom);
        data.contentChanged(mbox);

        ZimbraLog.mailop.info("Adding Document %s: id=%d, folderId=%d, folderName=%s.", filename, data.id, folder.getId(), folder.getName());
        DbMailItem.create(mbox, data, null);

        Document doc = new Document(mbox, data);
        doc.finishCreation(null);
        pd.setVersion(doc.getVersion());
        return doc;
    }
    
    @Override void decodeMetadata(Metadata meta) throws ServiceException {
        // roll forward from the old versioning mechanism (old revisions are lost)
        MetadataList revlist = meta.getList(Metadata.FN_REVISIONS, true);
        if (revlist != null && !revlist.isEmpty()) {
            try {
                Metadata rev = revlist.getMap(revlist.size() - 1);
                mCreator = rev.get(Metadata.FN_CREATOR, null);
                mFragment = rev.get(Metadata.FN_FRAGMENT, null);

                int version = (int) rev.getLong(Metadata.FN_VERSION, 1);
                if (version > 1 && rev.getLong(Metadata.FN_VERSION, 1) != 1)
                    meta.put(Metadata.FN_VERSION, version);
            } catch (ServiceException e) {
            }
        }

        super.decodeMetadata(meta);

        mContentType = meta.get(Metadata.FN_MIME_TYPE);
        mCreator     = meta.get(Metadata.FN_CREATOR, mCreator);
        mFragment    = meta.get(Metadata.FN_FRAGMENT, mFragment);
        mLockOwner   = meta.get(Metadata.FN_LOCK_OWNER, mLockOwner);
        mDescription = meta.get(Metadata.FN_DESCRIPTION, mDescription);
        mLockTimestamp = meta.getLong(Metadata.FN_LOCK_TIMESTAMP, 0);
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, mContentType, mCreator, mFragment, mLockOwner, mLockTimestamp, mDescription);
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, String mimeType, String creator, String fragment, String lockowner, long lockts, String description) {
        if (meta == null)
            meta = new Metadata();
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_CREATOR, creator);
        meta.put(Metadata.FN_FRAGMENT, fragment);
        meta.put(Metadata.FN_LOCK_OWNER, lockowner);
        meta.put(Metadata.FN_LOCK_TIMESTAMP, lockts);
        meta.put(Metadata.FN_DESCRIPTION, description);
        return MailItem.encodeMetadata(meta, color, version, extended);
    }

    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";
    private static final String CN_FILE_NAME = "filename";
    private static final String CN_EDITOR    = "edited_by";
    private static final String CN_LOCKOWNER = "locked_by";
    private static final String CN_LOCKTIMESTAMP = "locked_at";
    private static final String CN_DESCRIPTION = "description";

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getNameForType(this)).append(": {");
        sb.append(CN_FILE_NAME).append(": ").append(getName()).append(", ");
        sb.append(CN_EDITOR).append(": ").append(getCreator()).append(", ");
        sb.append(CN_MIME_TYPE).append(": ").append(mContentType).append(", ");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_FRAGMENT).append(": ").append(mFragment);
        if (mDescription != null)
            sb.append(CN_DESCRIPTION).append(": ").append(mDescription);
        if (mLockOwner != null)
            sb.append(CN_LOCKOWNER).append(": ").append(mLockOwner);
        if (mLockTimestamp > 0)
            sb.append(CN_LOCKTIMESTAMP).append(": ").append(mLockTimestamp);
        sb.append("}");
        return sb.toString();
    }

    @Override protected boolean trackUserAgentInMetadata() {
        return true;
    }

    @Override MailboxBlob setContent(StagedBlob staged, Object content) throws ServiceException, IOException {
        checkLock();
        return super.setContent(staged, content);
    }
    
    @Override boolean move(Folder target) throws ServiceException {
        checkLock();
        return super.move(target);
    }
    
    @Override void lock(Account authuser) throws ServiceException {
        if (mLockOwner != null && 
                !mLockOwner.equalsIgnoreCase(authuser.getId()))
            throw MailServiceException.CANNOT_LOCK(mId, mLockOwner);
        mLockOwner = authuser.getId();
        mLockTimestamp = System.currentTimeMillis();
        markItemModified(Change.MODIFIED_METADATA);
        saveMetadata();
    }
    
    @Override void unlock(Account authuser) throws ServiceException {
        if (mLockOwner == null)
            return;
        if (!mLockOwner.equalsIgnoreCase(authuser.getId()) &&
                checkRights(ACL.RIGHT_ADMIN, authuser, false) == 0)
            throw MailServiceException.CANNOT_UNLOCK(mId, mLockOwner);
        mLockOwner = null;
        mLockTimestamp = 0;
        markItemModified(Change.MODIFIED_METADATA);
        saveMetadata();
    }

    protected void checkLock() throws ServiceException {
        Account authenticatedAccount = mMailbox.getAuthenticatedAccount();
        if (authenticatedAccount == null)
            authenticatedAccount = mMailbox.getAccount();
        if (mLockOwner != null && 
                !authenticatedAccount.getId().equalsIgnoreCase(mLockOwner))
            throw MailServiceException.LOCKED(mId, mLockOwner);
    }
}
