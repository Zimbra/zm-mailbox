/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.service.ServiceException;

/**
 * @author dkarp
 */
public class Document extends MailItem {
    protected String mContentType;
    protected String mCreator;
    protected String mFragment;

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

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }

    @Override int getMaxRevisions() throws ServiceException {
        return getAccount().getIntAttr(Provisioning.A_zimbraNotebookMaxRevisions, 0);
    }

    @Override public List<org.apache.lucene.document.Document> generateIndexData() throws ServiceException {
        ParsedDocument pd = null;
        synchronized(this.getMailbox()) {
            pd = new ParsedDocument(getContent(), getName(), getContentType(), getChangeDate(), getCreator());
        }
        
        List<org.apache.lucene.document.Document> toRet = new ArrayList<org.apache.lucene.document.Document>(1);
        toRet.add(pd.getDocument());
        
        return toRet;
    }

    @Override
    public synchronized void reanalyze(Object obj) throws ServiceException {
        if (!(obj instanceof ParsedDocument))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedDocument object", null);
        if ((mData.flags & Flag.BITMASK_UNCACHED) != 0)
            throw ServiceException.FAILURE("cannot reanalyze an old item revision", null);

        ParsedDocument pd = (ParsedDocument) obj;

        mFragment = pd.getFragment();
        mCreator  = pd.getCreator();
        pd.setVersion(getVersion());

        if (mData.size != pd.getSize()) {
            markItemModified(Change.MODIFIED_SIZE);
            mMailbox.updateSize(pd.getSize() - mData.size, false);
            getFolder().updateSize(0, pd.getSize() - mData.size);
            mData.size = pd.getSize();
        }

        saveData(null);
    }

    protected static UnderlyingData prepareCreate(byte type, int id, Folder folder, short volumeId, String name, String mimeType, ParsedDocument pd, Metadata meta) 
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_DOCUMENT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        name = validateItemName(name);

        Mailbox mbox = folder.getMailbox();

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = type;
        data.folderId    = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId = id;
        data.imapId      = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.size        = pd.getSize();
        data.name        = name;
        data.subject     = name;
        data.setBlobDigest(pd.getDigest());
        data.metadata    = encodeMetadata(meta, DEFAULT_COLOR, 1, mimeType, pd.getCreator(), pd.getFragment()).toString();

        return data;
    }

    static Document create(int id, Folder folder, short volumeId, String filename, String type, ParsedDocument pd)
    throws ServiceException {
        assert(id != Mailbox.ID_AUTO_INCREMENT);

        UnderlyingData data = prepareCreate(TYPE_DOCUMENT, id, folder, volumeId, filename, type, pd, null);

        Mailbox mbox = folder.getMailbox();
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(null);
        pd.setVersion(doc.getVersion());
        return doc;
    }

    @Override 
    void decodeMetadata(Metadata meta) throws ServiceException {
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
    }

    @Override 
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mVersion, mContentType, mCreator, mFragment);
    }

    static Metadata encodeMetadata(Metadata meta, byte color, int version, String mimeType, String creator, String fragment) {
        if (meta == null)
            meta = new Metadata();
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_CREATOR, creator);
        meta.put(Metadata.FN_FRAGMENT, fragment);
        return MailItem.encodeMetadata(meta, color, version);
    }


    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";
    private static final String CN_FILE_NAME = "filename";
    private static final String CN_EDITOR    = "edited_by";

    @Override 
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getNameForType(this)).append(": {");
        sb.append(CN_FILE_NAME).append(": ").append(getName()).append(", ");
        sb.append(CN_EDITOR).append(": ").append(getCreator()).append(", ");
        sb.append(CN_MIME_TYPE).append(": ").append(mContentType).append(", ");
        sb.append(CN_FRAGMENT).append(": ").append(mFragment);
        appendCommonMembers(sb).append(", ");
        sb.append("}");
        return sb.toString();
    }
}
