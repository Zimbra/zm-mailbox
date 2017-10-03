/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MailItem.TemporaryIndexingException;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.localconfig.LC;
/**
 * @since Aug 23, 2004
 */
public class Document extends MailItem {
    protected String contentType;
    protected String creator;
    protected String fragment;
    protected String lockOwner;
    protected long lockTimestamp;
    protected String description;
    protected boolean descEnabled;

    Document(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    Document(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
    }

    Document(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String getSender() {
        return getCreator();
    }

    public String getCreator() {
        return Strings.nullToEmpty(creator);
    }

    public String getFragment() {
        return Strings.nullToEmpty(fragment);
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public long getLockTimestamp() {
        return lockTimestamp;
    }

    public String getDescription() {
        return Strings.nullToEmpty(description);
    }

    public boolean isDescriptionEnabled() {
        return descEnabled;
    }

    @Override
    boolean isTaggable() {
        return true;
    }

    @Override
    boolean isCopyable() {
        return true;
    }

    @Override
    boolean isMovable() {
        return true;
    }

    @Override
    boolean isMutable() {
        return true;
    }

    @Override
    boolean canHaveChildren() {
        return true;
    }

    @Override
    int getMaxRevisions() throws ServiceException {
        return getAccount().getIntAttr(Provisioning.A_zimbraNotebookMaxRevisions, 0);
    }

    public List<IndexDocument> generateIndexData() throws TemporaryIndexingException {
        try {
            MailboxBlob mblob = getBlob();
            if (mblob == null) {
                ZimbraLog.index.warn("Unable to fetch blob for Document id=%d,ver=%d,vol=%s",
                        mId, mVersion, getLocator());
                throw new MailItem.TemporaryIndexingException();
            }

            ParsedDocument pd = null;
            pd = new ParsedDocument(mblob.getLocalBlob(), getName(), getContentType(),
                    getChangeDate(), getCreator(), getDescription(), isDescriptionEnabled());

            if (pd.hasTemporaryAnalysisFailure()) {
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

    @Override
    public List<IndexDocument> generateIndexDataAsync(boolean indexAttachments) throws TemporaryIndexingException {
        return this.generateIndexData();
    }

    @Override
    public void reanalyze(Object obj, long newSize) throws ServiceException {
        if (!(obj instanceof ParsedDocument)) {
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedDocument object", null);
        }
        if (mData.isSet(Flag.FlagInfo.UNCACHED)) {
            throw ServiceException.FAILURE("cannot reanalyze an old item revision", null);
        }
        ParsedDocument pd = (ParsedDocument) obj;

        // new revision has at least new date.
        markItemModified(Change.METADATA);

        // new revision might have new name.
        if (!mData.name.equals(pd.getFilename())) {
            markItemModified(Change.NAME);
        }

        contentType = pd.getContentType();
        creator = pd.getCreator();

        if(!LC.documents_disable_instant_parsing.booleanValue())
            fragment = pd.getFragment();

        mData.date = (int) (pd.getCreatedDate() / 1000L);
        mData.name = pd.getFilename();
        mData.setSubject(pd.getFilename());
        description = pd.getDescription();
        descEnabled = pd.isDescriptionEnabled();
        pd.setVersion(getVersion());

        if (mData.size != pd.getSize()) {
            markItemModified(Change.SIZE);
            mMailbox.updateSize(pd.getSize() - mData.size, false);
            getFolder().updateSize(0, 0, pd.getSize() - mData.size);
            mData.size = pd.getSize();
        }

        saveData(new DbMailItem(mMailbox));
    }

    protected static UnderlyingData prepareCreate(MailItem.Type type, int id, String uuid, Folder folder, String name,
            String mimeType, ParsedDocument pd, Metadata meta, CustomMetadata custom, int flags) throws ServiceException {

        return prepareCreate(type, id, uuid, folder, name, mimeType, pd, meta, custom, flags, LC.documents_disable_instant_parsing.booleanValue());
    }

    protected static UnderlyingData prepareCreate(MailItem.Type type, int id, String uuid, Folder folder, String name,
            String mimeType, ParsedDocument pd, Metadata meta, CustomMetadata custom, int flags, boolean skipParsing) throws ServiceException {
        if (folder == null || !folder.canContain(Type.DOCUMENT)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        if (!folder.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        name = validateItemName(name);

        CustomMetadataList extended = (custom == null ? null : custom.asList());

        Mailbox mbox = folder.getMailbox();

        UnderlyingData data = new UnderlyingData();
        data.uuid = uuid;
        data.id = id;
        data.type = type.toByte();
        data.folderId = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false)) {
            data.indexId = IndexStatus.DEFERRED.id();
        }
        data.imapId = id;
        data.date = (int) (pd.getCreatedDate() / 1000L);
        data.size = pd.getSize();
        data.name = name;
        data.setSubject(name);
        data.setBlobDigest(pd.getDigest());
        data.metadata = encodeMetadata(meta, DEFAULT_COLOR_RGB, 1, 1, extended, mimeType, pd.getCreator(),
                skipParsing ? null : pd.getFragment(), null, 0, pd.getDescription(), pd.isDescriptionEnabled(), null).toString();
        data.setFlags(flags);
       return data;
    }

    static Document create(int id, String uuid, Folder folder, String filename, String type, ParsedDocument pd,
            CustomMetadata custom, int flags) throws ServiceException {
        return create(id, uuid, folder, filename, type, pd, custom, flags, null);
    }
    static Document create(int id, String uuid, Folder folder, String filename, String type, ParsedDocument pd,
            CustomMetadata custom, int flags, MailItem parent) throws ServiceException {
        assert(id != Mailbox.ID_AUTO_INCREMENT);

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = prepareCreate(Type.DOCUMENT, id, uuid, folder, filename, type, pd, null, custom, flags);
        if (parent != null) {
            data.parentId = parent.mId;
        }
        data.contentChanged(mbox);

        ZimbraLog.mailop.info("Adding Document %s: id=%d, folderId=%d, folderName=%s",
                filename, data.id, folder.getId(), folder.getName());
        new DbMailItem(mbox).create(data);

        Document doc = new Document(mbox, data);
        doc.finishCreation(parent);
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
                creator = rev.get(Metadata.FN_CREATOR, null);
                fragment = rev.get(Metadata.FN_FRAGMENT, null);

                int version = (int) rev.getLong(Metadata.FN_VERSION, 1);
                if (version > 1 && rev.getLong(Metadata.FN_VERSION, 1) != 1) {
                    meta.put(Metadata.FN_VERSION, version);
                }
            } catch (ServiceException ignored) {
            }
        }

        super.decodeMetadata(meta);

        contentType = meta.get(Metadata.FN_MIME_TYPE);
        creator     = meta.get(Metadata.FN_CREATOR, creator);
        fragment    = meta.get(Metadata.FN_FRAGMENT, fragment);
        lockOwner   = meta.get(Metadata.FN_LOCK_OWNER, lockOwner);
        description = meta.get(Metadata.FN_DESCRIPTION, description);
        lockTimestamp = meta.getLong(Metadata.FN_LOCK_TIMESTAMP, 0);
        descEnabled = meta.getBool(Metadata.FN_DESC_ENABLED, true);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, mExtendedData, contentType, creator, fragment, lockOwner,
                lockTimestamp, description, descEnabled, rights);
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended,
            String mimeType, String creator, String fragment, String lockowner, long lockts, String description,
            boolean descEnabled, ACL rights) {
        if (meta == null) {
            meta = new Metadata();
        }
        meta.put(Metadata.FN_MIME_TYPE, mimeType);
        meta.put(Metadata.FN_CREATOR, creator);
        meta.put(Metadata.FN_FRAGMENT, fragment);
        meta.put(Metadata.FN_LOCK_OWNER, lockowner);
        meta.put(Metadata.FN_LOCK_TIMESTAMP, lockts);
        meta.put(Metadata.FN_DESCRIPTION, description);
        meta.put(Metadata.FN_DESC_ENABLED, descEnabled);
        return MailItem.encodeMetadata(meta, color, rights, metaVersion, version, extended);
    }

    private static final String CN_FRAGMENT  = "fragment";
    private static final String CN_MIME_TYPE = "mime_type";
    private static final String CN_FILE_NAME = "filename";
    private static final String CN_EDITOR    = "edited_by";
    private static final String CN_LOCKOWNER = "locked_by";
    private static final String CN_LOCKTIMESTAMP = "locked_at";
    private static final String CN_DESCRIPTION = "description";

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("type", getType());
        helper.add(CN_FILE_NAME, getName());
        helper.add(CN_EDITOR, getCreator());
        helper.add(CN_MIME_TYPE, contentType);
        appendCommonMembers(helper);
        helper.add(CN_FRAGMENT, fragment);
        if (description != null) {
            helper.add(CN_DESCRIPTION, description);
        }
        if (lockOwner != null) {
            helper.add(CN_LOCKOWNER, lockOwner);
        }
        if (lockTimestamp > 0) {
            helper.add(CN_LOCKTIMESTAMP, lockTimestamp);
        }
        return helper.toString();
    }

    @Override
    protected boolean trackUserAgentInMetadata() {
        return true;
    }

    @Override
    MailboxBlob setContent(StagedBlob staged, Object content) throws ServiceException, IOException {
        checkLock();
        return super.setContent(staged, content);
    }

    @Override
    boolean move(Folder target) throws ServiceException {
        checkLock();
        return super.move(target);
    }

    @Override
    void rename(String name, Folder target) throws ServiceException {
        String oldName = getName();
        super.rename(name, target);
        if (!oldName.equalsIgnoreCase(name))
            mMailbox.index.add(this);
    }

    @Override
    void lock(Account authuser) throws ServiceException {
        if (lockOwner != null && !lockOwner.equalsIgnoreCase(authuser.getId())) {
            throw MailServiceException.CANNOT_LOCK(mId, lockOwner);
        }
        lockOwner = authuser.getId();
        lockTimestamp = System.currentTimeMillis();
        markItemModified(Change.LOCK);
        saveMetadata();
    }

    @Override
    void unlock(Account authuser) throws ServiceException {
        if (lockOwner == null) {
            return;
        }
        if (!lockOwner.equalsIgnoreCase(authuser.getId()) && checkRights(ACL.RIGHT_ADMIN, authuser, false) == 0) {
            throw MailServiceException.CANNOT_UNLOCK(mId, lockOwner);
        }
        lockOwner = null;
        lockTimestamp = 0;
        markItemModified(Change.LOCK);
        saveMetadata();
    }

    protected void checkLock() throws ServiceException {
        if (lockOwner != null && !mMailbox.getLockAccount().getId().equalsIgnoreCase(lockOwner)) {
            throw MailServiceException.LOCKED(mId, lockOwner);
        }
    }

    @Override
    PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        for (Comment comment : mMailbox.getComments(null, mId, 0, -1, inDumpster())) {
            info.add(comment.getDeletionInfo());
        }
        return info;
    }

    @Override
    protected long getMaxAllowedExternalShareLifetime(Account account) {
        return account.getFileExternalShareLifetime();
    }

    @Override
    protected long getMaxAllowedInternalShareLifetime(Account account) {
        return account.getFileShareLifetime();
    }
}
