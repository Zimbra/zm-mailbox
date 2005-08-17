/*
 * Created on Aug 18, 2004
 */
package com.liquidsys.coco.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.db.DbMailItem;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.PendingModifications.Change;
import com.liquidsys.coco.util.StringUtil;


/**
 * @author dkarp
 */
public class Folder extends MailItem {
	public static final byte FOLDER_IS_IMMUTABLE    = 0x01;
    public static final byte FOLDER_NO_UNREAD_COUNT = 0x02;

	private byte      mAttributes;
	private ArrayList mSubfolders;
    private Folder    mParent;

	Folder(Mailbox mbox, UnderlyingData ud) throws ServiceException {
		super(mbox, ud);
		if (mData.type != TYPE_FOLDER && mData.type != TYPE_SEARCHFOLDER)
			throw new IllegalArgumentException();
	}


    public int getMessageCount() {
        return mData.messageCount;
    }

    public String getName() {
		return (mData.subject == null ? "" : mData.subject);
	}

    public String getPath() {
        if (mId == Mailbox.ID_FOLDER_ROOT || mId == Mailbox.ID_FOLDER_USER_ROOT)
            return "/";
        String parentPath = mParent.getPath();
        return parentPath + (parentPath.equals("/") ? "" : "/") + getName();
    }

    public byte getAttributes() {
	    return mAttributes;
	}

    public boolean inTrash() throws ServiceException {
        if (mId <= Mailbox.HIGHEST_SYSTEM_ID)
            return (mId == Mailbox.ID_FOLDER_TRASH);
        return mParent.inTrash();
    }

	public boolean inSpam() {
		return (mId == Mailbox.ID_FOLDER_SPAM);
	}
    
    public boolean isHidden() throws ServiceException {
        switch (mId) {
            case Mailbox.ID_FOLDER_USER_ROOT:  return false;
            case Mailbox.ID_FOLDER_ROOT:       return true;
            default:                           return mParent.isHidden();
        }
    }

    public boolean hasSubfolders() {
        return (mSubfolders != null && !mSubfolders.isEmpty());
    }

    Folder findSubfolder(String name) {
        if (name == null || mSubfolders == null)
            return null;
        for (int i = 0; i < mSubfolders.size(); i++) {
            Folder subfolder = (Folder) mSubfolders.get(i);
            if (subfolder != null && name.equalsIgnoreCase(subfolder.getName()))
                return subfolder;
        }
        return null;
    }

    private static final class SortByName implements Comparator {
        public int compare(Object o1, Object o2) {
            String n1 = ((Folder) o1).getName();
            String n2 = ((Folder) o2).getName();
            return n1.compareToIgnoreCase(n2);
        }
    }

    public List getSubfolders() {
        if (mSubfolders == null)
            return null;
        Collections.sort(mSubfolders, new SortByName());
        return Collections.unmodifiableList(mSubfolders);
    }

    /** Returns a <code>List</code> that includes this folder and all its subfolders.
     *  The tree traversal is done depth-first, so this folder is the first element in
     *  the list, followed by its children, then its grandchildren, ... */
    public List getSubfolderHierarchy() {
        return accumulateHierarchy(new ArrayList());
    }
    private List accumulateHierarchy(List list) {
        list.add(this);
        if (mSubfolders != null)
            for (Iterator it = mSubfolders.iterator(); it.hasNext(); )
                ((Folder) it.next()).accumulateHierarchy(list);
        return list;
    }

    void updateSize(long delta) {
        if (delta == 0)
            return;
        // if we go negative, that's OK!  just pretend we're at 0.
        markItemModified(Change.MODIFIED_SIZE);
        mData.size = Math.max(0, mData.size + delta);
    }

    boolean isTaggable()       { return false; }
    boolean isCopyable()       { return false; }
    boolean isMovable()        { return ((mAttributes & FOLDER_IS_IMMUTABLE) == 0); }
    public boolean isMutable() { return ((mAttributes & FOLDER_IS_IMMUTABLE) == 0); }
    boolean isIndexed()        { return false; }
    boolean canHaveChildren()  { return true; }
    boolean trackUnread()      { return ((mAttributes & FOLDER_NO_UNREAD_COUNT) == 0); }

	boolean canParent(MailItem child)  { return (child instanceof Folder); }
	boolean canContain(MailItem child) {
        if (!canContain(child.getType()))
            return false;
		else if (child instanceof Folder) {
			// may not contain our parents or grandparents (c.f. Back to the Future)
			for (Folder folder = this; folder.getId() != Mailbox.ID_FOLDER_ROOT; folder = folder.mParent)
				if (folder.getId() == child.getId())
					return false;
		}
		return true;
	}
    boolean canContain(byte type) {
        if ((type == TYPE_TAG) != (mId == Mailbox.ID_FOLDER_TAGS))
            return false;
        else if ((type == TYPE_CONVERSATION) != (mId == Mailbox.ID_FOLDER_CONVERSATIONS))
            return false;
        else if (type == TYPE_FOLDER && mId == Mailbox.ID_FOLDER_SPAM)
            return false;
        return true;
    }


	static Folder create(Mailbox mbox, int id, Folder parent, String name) throws ServiceException {
		return create(mbox, id, parent, name, (byte) 0);
	}
	static Folder create(Mailbox mbox, int id, Folder parent, String name, byte attributes) throws ServiceException {
        if (id != Mailbox.ID_FOLDER_ROOT) {
    		if (parent == null || !parent.canContain(TYPE_FOLDER))
    			throw MailServiceException.CANNOT_CONTAIN();
            name = validateFolderName(name);
    		if (parent.findSubfolder(name) != null)
    			throw MailServiceException.ALREADY_EXISTS(name);
        }

		UnderlyingData data = new UnderlyingData();
		data.id          = id;
		data.type        = TYPE_FOLDER;
		data.folderId    = (id == Mailbox.ID_FOLDER_ROOT ? id : parent.getId());
		data.parentId    = data.folderId;
        data.date        = mbox.getOperationTimestamp();
        data.subject     = name;
		data.metadata    = encodeMetadata(attributes);
		data.modMetadata = mbox.getOperationChangeID();
		data.modContent  = mbox.getOperationChangeID();
		DbMailItem.create(mbox, data);

		Folder folder = new Folder(mbox, data);
		folder.finishCreation(parent);
		return folder;
	}

	void rename(String name, Folder parent) throws ServiceException {
		if (!isMutable())
			throw MailServiceException.IMMUTABLE_OBJECT(mId);
		name = validateFolderName(name);

        if (!name.equals(mData.subject)) {
            markItemModified(Change.MODIFIED_NAME);
    		Folder existingFolder = parent.findSubfolder(name);
    		if (existingFolder != null && existingFolder != this)
    			throw MailServiceException.ALREADY_EXISTS(name);
    		mData.subject = name;
    		saveSubject();
        }

        if (parent != mParent)
            move(parent);
	}

    private static final String INVALID_CHARACTERS = ".*[:/\"\t\r\n].*";
    private static final int    MAX_FOLDER_LENGTH  = 128;

    protected static String validateFolderName(String name) throws ServiceException {
        if (name == null || name != StringUtil.stripControlCharacters(name))
            throw MailServiceException.INVALID_NAME(name);
        name = name.trim();
        if (name.equals("") || name.length() > MAX_FOLDER_LENGTH || name.matches(INVALID_CHARACTERS))
            throw MailServiceException.INVALID_NAME(name);
        return name;
    }

    void alterUnread(boolean unread) throws ServiceException {
        if (unread)
            throw ServiceException.INVALID_REQUEST("folders can only be marked read", null);
        if (!isUnread())
            return;

        // decrement the in-memory unread count of each message.  each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        List unreadData = DbMailItem.getUnreadMessages(this);
        Array targets = new Array();
        boolean missed = false;
        for (Iterator it = unreadData.iterator(); it.hasNext(); ) {
            Message msg = mMailbox.getMessage((UnderlyingData) it.next());
            if (msg.checkChangeID()) {
                msg.updateUnread(-1);
                targets.add(msg.getId());
            } else
                missed = true;
        }

        // mark all messages in this folder as read in the database
        if (!missed)
            DbMailItem.alterUnread(this, unread);
        else
            DbMailItem.alterUnread(mMailbox, targets, unread);
    }
    
    private void recursiveUpdateUnread(boolean unread) throws ServiceException {
        alterUnread(unread);
        if (mSubfolders != null)
            for (Iterator it = mSubfolders.iterator(); it.hasNext(); )
                ((Folder) it.next()).recursiveUpdateUnread(unread);
    }

	protected void updateUnread(int delta) throws ServiceException {
		if (delta == 0 || !trackUnread())
			return;
		markItemModified(Change.MODIFIED_UNREAD);

		// update our unread count (should we check that we don't have too many unread?)
		mData.unreadCount += delta;
		if (mData.unreadCount < 0)
			throw ServiceException.FAILURE("inconsistent state: unread < 0 for item " + mId, null);
	}

    protected void move(Folder folder) throws ServiceException {
        markItemModified(Change.MODIFIED_FOLDER | Change.MODIFIED_PARENT);
        if (mData.folderId == folder.getId())
            return;
        if (!isMovable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        // moving a folder to the Trash marks its contents as read
        if (!inTrash() && folder.inTrash())
            recursiveUpdateUnread(false);

        // tell the folder's old and new parents
        mParent.removeChild(this);
        folder.addChild(this);

        // and update the folder's data (in memory and DB)
        DbMailItem.setFolder(this, folder);
        mData.folderId = folder.getId();
        mData.parentId = folder.getId();
        mData.modMetadata = mMailbox.getOperationChangeID();
    }

	protected void addChild(MailItem child) throws ServiceException {
		if (child == null || !canParent(child))
			throw MailServiceException.CANNOT_CONTAIN();
		else if (child == this) {
			if (mId != Mailbox.ID_FOLDER_ROOT)
				throw MailServiceException.CANNOT_CONTAIN();
		} else if (!(child instanceof Folder))
			super.addChild(child);
		else {
			markItemModified(Change.MODIFIED_CHILDREN);
			Folder subfolder = (Folder) child;
			if (mSubfolders == null)
				mSubfolders = new ArrayList();
			else {
                Folder existing = findSubfolder(subfolder.getName());
                if (existing == child)
                    return;
                else if (existing != null)
    				throw MailServiceException.ALREADY_EXISTS(subfolder.getName());
            }
			mSubfolders.add(subfolder);
            subfolder.mParent = this;
		}
	}

	protected void removeChild(MailItem child) throws ServiceException {
		if (child == null)
			throw MailServiceException.CANNOT_CONTAIN();
		else if (!(child instanceof Folder))
			super.removeChild(child);
		else {
			markItemModified(Change.MODIFIED_CHILDREN);
            Folder subfolder = (Folder) child;
			if (mSubfolders == null)
				throw MailServiceException.IS_NOT_CHILD();
			int index = mSubfolders.indexOf(subfolder);
			if (index == -1)
				throw MailServiceException.IS_NOT_CHILD();
			mSubfolders.remove(index);
            subfolder.mParent = null;
		}
	}

    void updateMessageCount(int delta) throws ServiceException {
        if (delta == 0 || !trackUnread())
            return;

        markItemModified(Change.MODIFIED_MSG_COUNT);
        mData.messageCount += delta;
        if (mData.messageCount < 0)
            throw ServiceException.FAILURE("inconsistent state: msg count < 0 for folder " + mId, null);
    }

    /** Deletes this folder and all its subfolders. */
    void delete(TargetConstraint tcon) throws ServiceException {
        List subfolders = getSubfolderHierarchy();
        for (int i = subfolders.size() - 1; i >= 0; i--) {
            Folder subfolder = (Folder) subfolders.get(i);
            subfolder.deleteSingleFolder();
        }
    }

    void empty(boolean includeSubfolders) throws ServiceException {
        // kill all subfolders, if so requested
        if (includeSubfolders) {
            List subfolders = getSubfolderHierarchy();
            // we DO NOT include *this* folder, the first in the list...
            for (int i = subfolders.size() - 1; i >= 1; i--) {
                Folder subfolder = (Folder) subfolders.get(i);
                subfolder.deleteSingleFolder();
            }
        }

        // now we can empty *this* folder
        delete(DELETE_CONTENTS);
    }

    /** Deletes just this folder without affecting its subfolders. */
    private void deleteSingleFolder() throws ServiceException {
        super.delete(hasSubfolders() ? DELETE_CONTENTS : DELETE_ITEM);
    }

	PendingDelete getDeletionInfo() throws ServiceException {
        return DbMailItem.getLeafNodes(this);
    }

    void propagateDeletion(PendingDelete info) throws ServiceException {
        if (info.incomplete)
            info.cascadeIds = DbMailItem.markDeletionTargets(mMailbox, info.itemIds);
        else
            info.cascadeIds = DbMailItem.markDeletionTargets(this);
        super.propagateDeletion(info);
    }

	void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
		// when deleting a folder, need to purge conv cache!
        mMailbox.purge(TYPE_CONVERSATION);
		super.purgeCache(info, purgeItem);
	}

	void uncacheChildren() throws ServiceException {
		if (mSubfolders != null)
			for (Iterator it = mSubfolders.iterator(); it.hasNext(); )
				mMailbox.uncache((MailItem) it.next());
	}


    static void purgeMessages(Mailbox mbox, Folder folder, int beforeDate) throws ServiceException {
        if (beforeDate <= 0 || beforeDate >= mbox.getOperationTimestamp())
            return;
        boolean allFolders = (folder == null);
        List folders = (allFolders ? null : folder.getSubfolderHierarchy());

        // get the full list of things that are being removed
        PendingDelete info = DbMailItem.getLeafNodes(mbox, folders, beforeDate, allFolders);
        if (info.itemIds.isEmpty())
            return;
        mbox.markItemDeleted(info.itemIds);

        // update message counts
        for (Iterator it = info.messages.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            int folderID = ((Integer) entry.getKey()).intValue();
            int msgCount = ((DbMailItem.LocationCount) entry.getValue()).count;
            mbox.getFolderById(folderID).updateMessageCount(-msgCount);
        }

        // update the mailbox's size
        mbox.updateSize(-info.size);
        mbox.updateContactCount(-info.contacts);

        // update unread counts on folders and tags
        List unreadData = DbMailItem.getById(mbox, info.unreadIds, TYPE_MESSAGE);
        for (Iterator it = unreadData.iterator(); it.hasNext(); )
            mbox.getItem((UnderlyingData) it.next()).updateUnread(-1);

        // remove the deleted item(s) from the mailbox's cache
        if (!info.itemIds.isEmpty()) {
            info.cascadeIds = DbMailItem.markDeletionTargets(mbox, info.itemIds);
            mbox.purge(TYPE_CONVERSATION);
        }

        // actually delete the items from the DB
        DbMailItem.delete(mbox, info.itemIds);
        mbox.markOtherItemDirty(info);

        // also delete any conversations whose messages have all been removed
        if (info.cascadeIds != null && !info.cascadeIds.isEmpty()) {
            DbMailItem.delete(mbox, info.cascadeIds);
            mbox.markItemDeleted(info.cascadeIds);
            info.itemIds.addAll(info.cascadeIds);
        }

        // deal with index sharing
        if (!info.sharedIndex.isEmpty())
            DbMailItem.resolveSharedIndex(mbox, info);

        // write a deletion record for later sync
        if (mbox.isTrackingSync() && info.itemIds.size() > 0)
            DbMailItem.writeTombstone(mbox, info);

        // don't actually delete the blobs or index entries here; wait until after the commit
    }

    protected void saveSubject() throws ServiceException {
        mData.modMetadata = mMailbox.getOperationChangeID();
        DbMailItem.saveSubject(this, 0);
    }


	Metadata decodeMetadata(String metadata) throws ServiceException {
		Metadata meta = new Metadata(metadata, this);
		mAttributes = (byte) meta.getLong(Metadata.FN_ATTRS, 0);
        return meta;
	}
	
	String encodeMetadata() {
		return encodeMetadata(mAttributes);
	}
	static String encodeMetadata(byte attributes) {
		Metadata meta = new Metadata();
		if (attributes != 0)
			meta.put(Metadata.FN_ATTRS, attributes);
		return meta.toString();
	}


    private static final String CN_ATTRIBUTES = "attributes";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("folder: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_ATTRIBUTES).append(": ").append(mAttributes);
        sb.append("}");
        return sb.toString();
    }
}
