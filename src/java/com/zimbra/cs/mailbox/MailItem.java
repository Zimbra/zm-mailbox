/*
 * Created on Aug 12, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.*;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public abstract class MailItem implements Comparable {

    public static final byte TYPE_FOLDER       = 1;
    public static final byte TYPE_SEARCHFOLDER = 2;
    public static final byte TYPE_TAG          = 3;
    public static final byte TYPE_CONVERSATION = 4;
    public static final byte TYPE_MESSAGE      = 5;
    public static final byte TYPE_CONTACT      = 6;
    // public static final byte TYPE_INVITE       = 7;   // SKIP 7 FOR NOW!
    public static final byte TYPE_DOCUMENT     = 8;
    public static final byte TYPE_NOTE         = 9;
    public static final byte TYPE_FLAG         = 10;
    public static final byte TYPE_APPOINTMENT  = 11;
    public static final byte TYPE_VIRTUAL_CONVERSATION = 12;

    public static final byte TYPE_UNKNOWN          = -1;
    public static final byte FIRST_SEARCHABLE_TYPE = TYPE_MESSAGE;
    
    private static String[] TYPE_NAMES = {
        null,
        "folder",
        "search folder",
        "tag",
        "conversation",
        "message",
        "contact",
        "invite",
        "document",
        "note",
        "flag",
        "appointment",
        "virtual conversation"
    };

    /** @return the human-readable name for the specified type. */
    public static String getNameForType(MailItem item) {
        return getNameForType(item == null ? TYPE_UNKNOWN : item.getType());
    }
    public static String getNameForType(byte type) {
        return (type == TYPE_UNKNOWN ? "unknown" : TYPE_NAMES[type]);
    }

    public static final int FLAG_UNCHANGED = 0x80000000;
    public static final long TAG_UNCHANGED = (1L << 31);

    public static final int TAG_ID_OFFSET  = 64;
    public static final int MAX_FLAG_COUNT = 31;
    public static final int MAX_TAG_COUNT  = 63;

	public static final class Array {
		private static final int INITIAL_ARRAY_SIZE = 3;

		public int[] array;
		public int length = 0;

		Array()           { array = new int[INITIAL_ARRAY_SIZE]; }
		Array(int value) { add(value); }
		Array(String csv) {
	    	if (csv == null || csv.equals(""))
	    		return;
        	String[] tags = csv.split(",");
        	grow(tags.length);
        	for (int i = 0; i < tags.length; i++)
         		add(Integer.parseInt(tags[i]));
		}
		Array(Array other) {
			grow(other.array.length);
			length = other.length;
			for (int i = 0; i < length; i++)
				array[i] = other.array[i];
		}

		void grow(int needed) {
			int oldLength = (array == null ? 0 : array.length);
			if (length + needed > oldLength) {
				int[] newArray = new int[oldLength + INITIAL_ARRAY_SIZE];
				if (array != null)
					System.arraycopy(array, 0, newArray, 0, length);
				array = newArray;
			}
		}

		boolean contains(int value) {
			for (int i = 0; i < length; i++)
				if (array[i] == value)
					return true;
			return false;		}

		void add(int value) {
			grow(1);
			array[length++] = value;
		}
		void add(Array other) {
			grow(other.length);
			for (int i = 0; i < other.length; i++)
				array[length++] = other.array[i];
		}

		void remove(int value) { remove(value, true); }
		void remove(int value, boolean removeAll) {
			for (int i = 0; i < length; i++)
				if (array[i] == value) {
					array[i--] = array[length-- - 1];
					if (!removeAll)
						break;
				}
		}

        void sort(boolean ascending) {
            Arrays.sort(array, 0, length);
            if (!ascending)
                for (int i = 0; i < length / 2; i++) {
                    int other = length - 1 - i, tmp = array[i];
                    array[i] = array[other];  array[other] = tmp;
                }
        }

        int count(int value) {
            int count = 0;
            for (int i = 0; i < length; i++)
                if (array[i] == value)
                    count++;
            return count;
        }

        public String toString() {
			if (length == 0)
				return null;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(array[i]);
			}
			return sb.toString();
		}
	}

	protected final class TagSet {
		private Array mTags;

		TagSet()           { mTags = new Array(); }
		TagSet(Tag tag)    { mTags = new Array(tag.getId()); }
		TagSet(String csv) {
		    mTags = new Array();
		    if (csv == null || csv.equals(""))
		        return;
		    String[] tags = csv.split(",");
		    for (int i = 0; i < tags.length; i++) {
		        long value = 0;
		        try {
		            value = Long.parseLong(tags[i]);
		        } catch (NumberFormatException e) {
		            ZimbraLog.mailbox.error("Unable to parse tags: '" + csv + "'", e);
		            throw e;
		        }
		        if (value == 0)      continue;
		        else if (value < 0)  updateFlags((int) (-value), true);
		        else                 updateTags(value, true);
		    }
		}

		boolean contains(Tag tag)   { return contains(tag.getId()); }
		boolean contains(int tagId) { return mTags.contains(tagId); }

        int count(Tag tag)   { return count(tag.getId()); }
        int count(int tagId) { return mTags.count(tagId); }

		boolean update(Tag tag, boolean add)                    { return update(tag.getId(), add, false); }
		boolean update(Tag tag, boolean add, boolean affectAll) { return update(tag.getId(), add, affectAll); }
		boolean update(int tagId, boolean add)                  { return update(tagId, add, false); }
		boolean update(int tagId, boolean add, boolean affectAll) {
			if (add)
				mTags.add(tagId);
			else
				mTags.remove(tagId, affectAll);
			recalculate();
			return true;
		}

		void updateFlags(int flags, boolean add) {
    		for (int j = 0; flags != 0 && j < MAX_FLAG_COUNT; j++) {
    			int mask = 1 << j; 
    			if ((flags & mask) != 0) {
    				if (add)  mTags.add(-j - 1);
    				else      mTags.remove(-j - 1, false);
    				flags &= ~mask;
    			}
    		}
    		recalculate();
		}
		void updateTags(long tags, boolean add) {
    		for (int j = 0; tags != 0 && j < MAX_TAG_COUNT; j++) {
    			long mask = 1L << j; 
    			if ((tags & mask) != 0) {
    				// should really check to make sure the tag is reasonable
    				if (add)  mTags.add(j + TAG_ID_OFFSET);
    				else      mTags.remove(j + TAG_ID_OFFSET, false);
    				tags &= ~mask;
    			}
    		}
    		recalculate();
		}

		void recalculate() {
			mData.flags = 0;
			mData.tags  = 0;
			for (int i = 0; i < mTags.length; i++) {
				int value = mTags.array[i];
				if (value < 0)
					mData.flags |= 1 << (-value - 1);
				else
					mData.tags |= 1L << (value - TAG_ID_OFFSET);
			}
		}

		public String toString() { return mTags.toString(); }
	}

    public static final class UnderlyingData {
        public int    id;
        public byte   type;
        public int    parentId = -1;
        public int    folderId = -1;
        public int    indexId  = -1;
        public String blobDigest;
        public int    date;
        public long   size;
        public int    flags;
        public long   tags;
        public String sender;
        public String subject;
        public String metadata;
        public int    modMetadata;
        public int    modContent;
        public short  volumeId = -1;
        public String inheritedTags;
        public String children;
        public int    unreadCount;
        public int    messageCount;

        public boolean isUnread() {
            return (unreadCount > 0);
        }
        
        public UnderlyingData duplicate(int newId, int newFolder) {
            UnderlyingData data = new UnderlyingData();
            data.id          = newId;
            data.type        = type;
            data.folderId    = newFolder;
            data.parentId    = parentId;
            data.indexId     = indexId;
            data.blobDigest  = blobDigest;
            data.date        = date;
            data.size        = size;
            data.flags       = flags;
            data.tags        = tags;
            data.subject     = subject;
            data.volumeId    = volumeId;
            data.unreadCount = unreadCount;
            return data;
        }
    }

    public static final class TargetConstraint {
        public static final short EXCLUDE_TRASH  = 0x01;
        public static final short EXCLUDE_SPAM   = 0x02;
        public static final short EXCLUDE_SENT   = 0x04;
        public static final short EXCLUDE_OTHERS = 0x08;
        public static final short INCLUDE_QUERY  = 0x10;

        private static final char ENC_NO_TRASH = 't';
        private static final char ENC_NO_SPAM  = '$';
        private static final char ENC_NO_SENT  = 's';
        private static final char ENC_NO_OTHER = 'o';
        private static final char ENC_QUERY    = 'q';

        private short  exclusions;
        private String query;

        private Mailbox mailbox;
        private int     sentFolder = -1;

        public TargetConstraint(Mailbox mbox, short exclude)       { this(mbox, exclude, null); }
        public TargetConstraint(Mailbox mbox, String includeQuery) { this(mbox, INCLUDE_QUERY, includeQuery); }
        public TargetConstraint(Mailbox mbox, short exclude, String includeQuery) {
            mailbox = mbox;
            if (includeQuery == null || includeQuery.trim().length() == 0)
                exclusions = (short) (exclude & ~INCLUDE_QUERY);
            else {
                exclusions = (short) (exclude | INCLUDE_QUERY);
                query = includeQuery;
            }
        }

        public static TargetConstraint parseConstraint(Mailbox mbox, String encoded) throws ServiceException {
            if (encoded == null)
                return null;
            short exclusions = 0;
            String query = null;
            loop: for (int i = 0; i < encoded.length(); i++)
                switch (encoded.charAt(i)) {
                    case ENC_NO_TRASH:  exclusions |= EXCLUDE_TRASH;       break;
                    case ENC_NO_SPAM:   exclusions |= EXCLUDE_SPAM;        break;
                    case ENC_NO_SENT:   exclusions |= EXCLUDE_SENT;        break;
                    case ENC_NO_OTHER:  exclusions |= EXCLUDE_OTHERS;      break;
                    case ENC_QUERY:     exclusions |= INCLUDE_QUERY;
                                        query = encoded.substring(i + 1);  break loop;
                    default:  throw ServiceException.INVALID_REQUEST("invalid encoded constraint: " + encoded, null);
                }
            return new TargetConstraint(mbox, exclusions, query);
        }
        public String toString() {
            if (exclusions == 0)
                return null;
            StringBuffer sb = new StringBuffer();
            if ((exclusions & EXCLUDE_TRASH) != 0)   sb.append(ENC_NO_TRASH);
            if ((exclusions & EXCLUDE_SPAM) != 0)    sb.append(ENC_NO_SPAM);
            if ((exclusions & EXCLUDE_SENT) != 0)    sb.append(ENC_NO_SENT);
            if ((exclusions & EXCLUDE_OTHERS) != 0)  sb.append(ENC_NO_OTHER);
            if ((exclusions & INCLUDE_QUERY) != 0)   sb.append(ENC_QUERY).append(query);
            return sb.toString();
        }

        boolean checkItem(MailItem item) throws ServiceException {
            // FIXME: doesn't support EXCLUDE_QUERY
            if ((exclusions & (EXCLUDE_TRASH | EXCLUDE_SPAM | EXCLUDE_SENT | EXCLUDE_OTHERS)) == 0)
                return true;
            if ((exclusions & EXCLUDE_TRASH) != 0 && item.inTrash())
                return false;
            if ((exclusions & EXCLUDE_SPAM) != 0 && item.inSpam())
                return false;
            if ((exclusions & EXCLUDE_SENT) != 0 && item.getFolderId() == getSentFolder())
                return false;
            if ((exclusions & EXCLUDE_OTHERS) != 0 && !item.inTrash() && !item.inSpam() && item.getFolderId() != getSentFolder())
                return false;
            return true;
        }
        private int getSentFolder() {
            if (sentFolder != -1) {
                sentFolder = Mailbox.ID_FOLDER_SENT;
                try {
                    String sent = mailbox.getAccount().getAttr(Provisioning.A_liquidPrefSentMailFolder, null);
                    if (sent != null)
                        sentFolder = mailbox.getFolderByPath(sent).getId();
                } catch (ServiceException e) { }
            }
            return sentFolder;
        }
    }

	protected int            mId;
	protected UnderlyingData mData;
	protected Mailbox        mMailbox;
	protected TagSet         mInheritedTagSet;
	protected Array          mChildren;
    protected MailboxBlob    mBlob;

	MailItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		if (data == null)
			throw new IllegalArgumentException();
        validateFlags(data.flags);
		mId      = data.id;
		mData    = data;
		mMailbox = mbox;
		if (mData.inheritedTags != null && !mData.inheritedTags.equals("") && trackTags())
	    	mInheritedTagSet = new TagSet(mData.inheritedTags);
		mData.inheritedTags = null;
		if (mData.children != null && !mData.children.equals("") && canHaveChildren())
			mChildren = new Array(mData.children);
		mData.children = null;
		decodeMetadata(mData.metadata);
		mData.metadata = null;
		// store the item in the mailbox's cache
		mbox.cache(this);
	}

    public int getId() {
        return mData.id;
    }

    public byte getType() {
        return mData.type;
    }

    public Mailbox getMailbox() {
        return mMailbox;
    }

    public int getMailboxId() {
        return mMailbox.getId();
    }

    public int getParentId() {
        return mData.parentId;
    }

    public int getFolderId() {
        return mData.folderId;
    }

    public short getVolumeId() {
    	return mData.volumeId;
    }

    public int getIndexId() {
        return mData.indexId;
    }

    public String getDigest() {
        return (mData.blobDigest == null ? "" : mData.blobDigest);
    }

    public long getDate() {
        return mData.date * 1000L;
    }

    public int getModifiedSequence() {
        return mData.modMetadata;
    }

    public int getSavedSequence() {
        return mData.modContent;
    }

    public long getSize() {
        return mData.size;
    }

    public String getSubject() {
        return (mData.subject == null ? "" : mData.subject);
    }

    public int getUnreadCount() {
        return mData.unreadCount;
    }

    /** @return the "external" flag bitmask, which includes {@link Flag#FLAG_UNREAD}. */
    public int getFlagBitmask() {
        int flags = mData.flags;
        if (isUnread())
            flags = flags | Flag.FLAG_UNREAD;
        return flags;
    }

    /** @return the external string representation of this item's flags,
     *  which includes the state of {@link Flag#FLAG_UNREAD}. */
    public String getFlagString() {
        String baseFlags = isUnread() ? Flag.UNREAD_FLAG_ONLY : "";
        if (mData.flags == 0)
            return baseFlags;
        return Flag.bitmaskToFlags(mData.flags, new StringBuffer(baseFlags)).toString();
    }

    public long getTagBitmask() {
        return mData.tags;
    }

    public String getTagString() {
        if (mData.tags == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        long tags = mData.tags;
        for (int i = 0; tags != 0 && i < MAX_TAG_COUNT - 1; i++)
            if ((tags & (1L << i)) != 0) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(i + TAG_ID_OFFSET);
                tags &= ~(1L << i);
            }
        return sb.toString();
    }

    public static long tagsToBitmask(String csv) {
        long bitmask = 0;
        if (csv != null && !csv.equals("")) {
            String[] tags = csv.split(",");
            for (int i = 0; i < tags.length; i++) {
                long value = 0;
                try {
                    value = Long.parseLong(tags[i]);
                } catch (NumberFormatException e) {
                    ZimbraLog.mailbox.error("Unable to parse tags: '" + csv + "'", e);
                    throw e;
                }
                
                if (value < TAG_ID_OFFSET || value >= TAG_ID_OFFSET + MAX_TAG_COUNT)
                    continue;
                // FIXME: should really check this against the existing tags in the mailbox
                bitmask |= 1L << (value - TAG_ID_OFFSET);
            }
        }
        return bitmask;
    }
    
	public boolean isTagged(Tag tag) {
		long bitmask = (tag == null ? 0 : (tag instanceof Flag ? mData.flags : mData.tags));
		return ((bitmask & tag.getBitmask()) != 0);
	}

	private boolean isTagged(int tagId) {
		long bitmask = (tagId < 0 ? mData.flags : mData.tags);
		int position = (tagId < 0 ? -tagId - 1 : tagId - TAG_ID_OFFSET);
		return ((bitmask & (1L << position)) != 0);
	}

	public boolean isUnread() {
		return mData.unreadCount > 0;
	}

    public boolean isFlagged() {
        return isTagged(Flag.ID_FLAG_FLAGGED);
    }

    public boolean hasAttachment() {
        return isTagged(Flag.ID_FLAG_ATTACHED);
    }

    public boolean inMailbox() throws ServiceException {
        return !inSpam() && !inTrash();
    }

    public boolean inTrash() throws ServiceException {
        if (mData.folderId <= Mailbox.HIGHEST_SYSTEM_ID)
            return (mData.folderId == Mailbox.ID_FOLDER_TRASH);
        Folder folder = null;
        synchronized (mMailbox) {
            folder = mMailbox.getFolderById(getFolderId());
        }
        return folder.inTrash();
    }

    public boolean inSpam() {
        return (mData.folderId == Mailbox.ID_FOLDER_SPAM);
    }


    public synchronized MailboxBlob getBlob() throws IOException, ServiceException {
        if (mBlob == null && mData.blobDigest != null) {
            mBlob = StoreManager.getInstance().getMailboxBlob(mMailbox, mId, mData.modContent, mData.volumeId);
            if (mBlob == null)
                throw ServiceException.FAILURE("missing blob for id: " + mId + ", change: " + mData.modContent, null);
        }
        return mBlob;
    }


    public int compareTo(Object o) {
        if (this == o)
            return 0;
        MailItem that = (MailItem) o;
        long myDate = getModifiedSequence(), theirDate = that.getModifiedSequence();
        return (myDate < theirDate ? -1 : (myDate == theirDate ? 0 : 1));
    }


    /**
     * Index the item.
     * @param redo Redo recorder
     * @param indexData extra data to index; each subclass of MailItem
     *                  should interpret this argument differently;
     *                  currently only Message class uses this argument,
     *                  for passing in a ParsedMessage
     * @throws ServiceException
     */
    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // override in subclasses that support indexing
    }

    MailItem getCachedParent() throws ServiceException {
        if (mData.parentId == -1)
            return null;
        return mMailbox.getCachedItem(new Integer(mData.parentId));
    }
	MailItem getParent() throws ServiceException {
		if (mData.parentId == -1)
			return null;
		return mMailbox.getItemById(mData.parentId, TYPE_UNKNOWN);
	}

	Folder getFolder() throws ServiceException {
		Folder folder = mMailbox.getFolderById(mData.folderId);
		if (folder == null)
			throw MailServiceException.NO_SUCH_FOLDER(mData.folderId);
		return folder;
	}

    boolean checkChangeID() throws ServiceException {
        return mMailbox.checkItemChangeID(this);
    }


	abstract boolean isTaggable();
    abstract boolean isCopyable();
	abstract boolean isMovable();
    abstract boolean isMutable();
    abstract boolean isIndexed();
	abstract boolean canHaveChildren();
	boolean trackTags()               { return isTaggable(); }
	boolean trackUnread()             { return true; }
	boolean canParent(MailItem child) { return canHaveChildren(); }

	static MailItem getById(Mailbox mbox, int id) throws ServiceException {
		return getById(mbox, id, TYPE_UNKNOWN);
	}
    static MailItem getById(Mailbox mbox, int id, byte type) throws ServiceException {
        return mbox.getItem(DbMailItem.getById(mbox, id, type));
    }
    static List getById(Mailbox mbox, Collection ids, byte type) throws ServiceException {
        if (ids == null || ids.isEmpty())
            return Collections.EMPTY_LIST;
        List items = new ArrayList(), data = DbMailItem.getById(mbox, ids, type);
        for (int i = 0; i < data.size(); i++)
            items.add(mbox.getItem((UnderlyingData) data.get(i)));
        return items;
    }

	static MailItem constructItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		if (data == null)
			throw noSuchItem(data.id, data.type);
		switch (data.type) {
			case TYPE_FOLDER:       return new Folder(mbox, data);
			case TYPE_SEARCHFOLDER: return new SearchFolder(mbox, data);
			case TYPE_TAG:          return new Tag(mbox, data);
			case TYPE_CONVERSATION: return new Conversation(mbox,data);
			case TYPE_MESSAGE:      return new Message(mbox, data);
			case TYPE_CONTACT:      return new Contact(mbox,data);
			case TYPE_DOCUMENT:     return new Document(mbox, data);
			case TYPE_NOTE:         return new Note(mbox, data);
            case TYPE_APPOINTMENT:  return new Appointment(mbox, data);
			default:                return null;
		}
	}
	public static MailServiceException noSuchItem(int id, byte type) {
		switch (type) {
			case TYPE_FOLDER:
			case TYPE_SEARCHFOLDER: return MailServiceException.NO_SUCH_FOLDER(id);
			case TYPE_FLAG:
			case TYPE_TAG:          return MailServiceException.NO_SUCH_TAG(id);
			case TYPE_CONVERSATION: return MailServiceException.NO_SUCH_CONV(id);
			case TYPE_MESSAGE:      return MailServiceException.NO_SUCH_MSG(id);
			case TYPE_CONTACT:      return MailServiceException.NO_SUCH_CONTACT(id);
//			case TYPE_INVITE:       return MailServiceException.NO_SUCH_MSG(id);
			case TYPE_DOCUMENT:     return MailServiceException.NO_SUCH_DOC(id);
            case TYPE_NOTE:         return MailServiceException.NO_SUCH_NOTE(id);
            case TYPE_APPOINTMENT:  return MailServiceException.NO_SUCH_APPT(id);
			default:                return MailServiceException.NO_SUCH_ITEM(id);
		}
	}


    void markItemCreated() {
        mMailbox.markItemCreated(this);
    }
    void markItemDeleted() {
        mMailbox.markItemDeleted(this);
    }
    void markItemModified(int reason) {
        mMailbox.markItemModified(this, reason);
    }

    void uncacheChildren() throws ServiceException {
		if (mChildren != null)
			for (int i = 0; i < mChildren.length; i++)
				mMailbox.uncacheItem(new Integer(mChildren.array[i]));
	}


	protected void finishCreation(MailItem parent) throws ServiceException {
	    markItemCreated();

	    // let the parent know it's got a new child
	    if (parent != null)
	        parent.addChild(this);

	    // sanity-check the location of the newly-created item
	    Folder folder = getFolder();
	    if (!folder.canContain(this))
	        throw MailServiceException.CANNOT_CONTAIN();

        // update mailbox and folder sizes
        mMailbox.updateSize(mData.size);
        folder.updateSize(mData.size);

	    // let the folder and tags know if the new item is unread
	    folder.updateUnread(mData.unreadCount);
	    updateTagUnread(mData.unreadCount);

	    // keep folder message counts correct
	    if (this instanceof Message)
	        folder.updateMessageCount(1);
	}

    void detach() throws ServiceException  { }

	void alterUnread(boolean unread) throws ServiceException {
	    // detect NOOPs and bail
	    if (unread == isUnread())
	        return;
	    else if (!mMailbox.mUnreadFlag.canTag(this))
	        throw MailServiceException.CANNOT_TAG();

	    markItemModified(Change.MODIFIED_UNREAD);
	    updateUnread(unread ? 1 : -1);
	    DbMailItem.alterUnread(this, unread);
	}
    void alterUnread(boolean unread, TargetConstraint tcon) throws ServiceException {
        alterUnread(unread);
    }

	void alterTag(Tag tag, boolean add) throws ServiceException {
	    if (tag == null)
	        throw ServiceException.INVALID_REQUEST("no tag supplied when trying to tag item " + mId, null);
	    if (!isTaggable() || (add && !tag.canTag(this)))
	        throw MailServiceException.CANNOT_TAG();
	    if (tag.getId() == Flag.ID_FLAG_UNREAD)
	        throw ServiceException.FAILURE("unread state must be set with alterUnread()", null);
	    // detect NOOPs and bail
	    if (add == isTagged(tag))
	        return;
	    markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
	    
	    // change our and our children's cached tags
	    tagChanged(tag, add);
	    
	    // tell our parent about the tag change (note: must precede DbMailItem.alterTags)
	    MailItem parent = getCachedParent();
	    if (parent != null)
	        parent.inheritedTagChanged(tag, add, true);
	    
	    // Since we're adding/removing a tag, the tag's unread count
	    // is affected by this item's unread count
	    if (tag.trackUnread() && mData.unreadCount > 0)
	        tag.updateUnread((add ? 1 : -1) * mData.unreadCount);
	    
	    // alter our tags in the DB
	    DbMailItem.alterTag(this, tag, add);
	}
    void alterTag(Tag tag, boolean add, TargetConstraint tcon) throws ServiceException {
        alterTag(tag, add);
    }
	
	protected void tagChanged(Tag tag, boolean add) throws ServiceException {
	    markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
	    mData.modMetadata = mMailbox.getOperationChangeID();
	    
	    if (tag instanceof Flag) {
	        if (add)  mData.flags |= tag.getBitmask();
	        else      mData.flags &= ~tag.getBitmask();
	    } else {
	        if (add)  mData.tags |= tag.getBitmask();
	        else      mData.tags &= ~tag.getBitmask();
	    }
	    
	    if (canHaveChildren() && mChildren != null)
	        for (int i = 0; i < mChildren.length; i++) {
	            MailItem child = mMailbox.getCachedItem(new Integer(mChildren.array[i]));
	            if (child != null)
	                child.inheritedTagChanged(tag, add, false);
	        }
	}
	
	protected void inheritedTagChanged(Tag tag, boolean add, boolean onChild) {
		if (!trackTags() || tag == null)
			return;
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);

		if (mInheritedTagSet != null)
			mInheritedTagSet.update(tag, add);
		else if (add)
			mInheritedTagSet = new TagSet(tag);
	}

	protected void updateUnread(int delta) throws ServiceException {
		if (delta == 0 || !trackUnread())
			return;
        markItemModified(Change.MODIFIED_UNREAD);

		// update our unread count (should we check that we don't have too many unread?)
		mData.unreadCount += delta;
		if (mData.unreadCount < 0)
			throw ServiceException.FAILURE(
                "inconsistent state: unread < 0 for " + getClass().getName() + " " + mId, null);

		// update the folder's unread count
		getFolder().updateUnread(delta);

		// update the parent's unread count
		MailItem parent = getCachedParent();
		if (parent != null)
			parent.updateUnread(delta);

		// tell the tags about the new unread item
        updateTagUnread(delta);
	}

    /**
     * Adds <code>delta</code> to the unread count of each <code>Tag</code> assigned
     * to this <code>MailItem</code>.
     */
    protected void updateTagUnread(int delta) throws ServiceException {
        if (delta == 0 || !isTaggable() || mData.tags == 0)
            return;
        long tags = mData.tags;
        for (int i = 0; tags != 0 && i < MAX_TAG_COUNT; i++) {
            long mask = 1L << i;
            if ((tags & mask) != 0) {
                Tag tag = mMailbox.getTagById(i + TAG_ID_OFFSET);
                if (tag != null)
                    tag.updateUnread(delta);
                tags &= ~mask;
            }
        }
    }

    /**
	 * Updates the set of user-defined tags.  All existing user-defined tags
	 * are removed and the supplied tags are added.  All referenced tag IDs
	 * must exist in the mailbox.
     * @param tagIDs comma-separated list of tag IDs
	 * @throws ServiceException
	 */
	void setTags(int flags, long tags) throws ServiceException {
        // FIXME: more optimal would be to do this with a single db UPDATE...

        // handle flags first...
        if (flags != mData.flags) {
            markItemModified(Change.MODIFIED_FLAGS);
            for (int i = 0; i < mMailbox.mFlags.length; i++) {
                Flag flag = mMailbox.mFlags[i];
                if (flag != null && (flags & flag.getBitmask()) != (mData.flags & flag.getBitmask()))
                    alterTag(flag, !isTagged(flag));
            }
        }

        // then handle tags...
        if (tags != mData.tags) {
            markItemModified(Change.MODIFIED_TAGS);
            for (int i = 0; i < MAX_TAG_COUNT; i++) {
                long mask = 1L << i;
                if ((tags & mask) != (mData.tags & mask)) {
                    Tag tag = null;
                    try {
                        tag = mMailbox.getTagById(i + TAG_ID_OFFSET);
                    } catch (MailServiceException.NoSuchItemException nsie) {
                        continue;
                    }
                    alterTag(tag, !isTagged(tag));
                }
            }
        }
	}
    void setTags(int flags, long tags, TargetConstraint tcon) throws ServiceException {
        setTags(flags, tags);
    }

    MailItem copy(Folder folder, int id) throws IOException, ServiceException {
        if (!isCopyable())
            throw MailServiceException.CANNOT_COPY(mId);
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        // mark as copied for indexing purposes
        if (!isTagged(mMailbox.mCopiedFlag))
            alterTag(mMailbox.mCopiedFlag, true);

        mMailbox.updateContactCount(this instanceof Contact ? 1 : 0);

        // if the copy or original is in Spam, put the copy in its own conversation
        MailItem parent = getParent();
        boolean detach = parent == null || parent.getId() <= 0 || isTagged(mMailbox.mDraftFlag) || inSpam() != folder.inSpam();

        UnderlyingData data = mData.duplicate(id, folder.getId());
        if (detach)
            data.parentId = -1;
        data.metadata    = encodeMetadata();
        data.modMetadata = mMailbox.getOperationChangeID();
        data.modContent  = mMailbox.getOperationChangeID();
        DbMailItem.copy(this, id, folder.getId(), data.parentId, data.metadata);

        MailItem item = constructItem(mMailbox, data);
        item.finishCreation(detach ? null : parent);

        MailboxBlob srcBlob = getBlob();
        if (srcBlob != null) {
            MailboxBlob blob = StoreManager.getInstance().link(srcBlob.getBlob(), mMailbox, item.getId(), data.modContent, item.getVolumeId());
            mMailbox.markOtherItemDirty(blob);
        }
        return item;
    }

    void move(Folder folder) throws ServiceException {
		if (mData.folderId == folder.getId())
			return;
		markItemModified(Change.MODIFIED_FOLDER);
		if (!isMovable())
			throw MailServiceException.IMMUTABLE_OBJECT(mId);
		if (!folder.canContain(this))
			throw MailServiceException.CANNOT_CONTAIN();

        Folder oldFolder = getFolder();
        if (this instanceof Message) {
            oldFolder.updateMessageCount(-1);
            folder.updateMessageCount(1);
        }

        oldFolder.updateSize(-getSize());
        folder.updateSize(getSize());

        if (!inTrash() && folder.inTrash()) {
            // moving something to Trash also marks it as read
            if (mData.unreadCount > 0)
                alterUnread(false);
        } else {
    		oldFolder.updateUnread(-mData.unreadCount);
    		folder.updateUnread(mData.unreadCount);
        }
        // moving a message (etc.) to Spam removes it from its conversation
        if (!inSpam() && folder.inSpam())
            detach();

		DbMailItem.setFolder(this, folder);
		folderChanged(folder);
	}
    void move(Folder folder, TargetConstraint tcon) throws ServiceException {
        move(folder);
    }

	protected void folderChanged(Folder newFolder) throws ServiceException {
        if (mData.folderId == newFolder.getId())
            return;
		markItemModified(Change.MODIFIED_FOLDER);
        mData.modMetadata = mMailbox.getOperationChangeID();
		mData.folderId = newFolder.getId();
		if (mChildren != null)
			for (int i = 0; i < mChildren.length; i++) {
				MailItem child = mMailbox.getCachedItem(new Integer(mChildren.array[i]));
				if (child != null)
					child.folderChanged(newFolder);
			}
	}

	protected void addChild(MailItem child) throws ServiceException {
		markItemModified(Change.MODIFIED_CHILDREN);
		if (!canParent(child))
			throw MailServiceException.CANNOT_PARENT();
		if (mMailbox != child.getMailbox())
			throw MailServiceException.WRONG_MAILBOX();

		// update child list
		if (mChildren == null)
			mChildren = new Array(child.getId());
		else if (!mChildren.contains(child.getId()))
			mChildren.add(child.getId());

		// update inherited tags, if applicable
		if (trackTags() && (child.mData.tags != 0 || child.mData.flags != 0)) {
            int oldFlags = mData.flags;
            long oldTags = mData.tags;

			if (mInheritedTagSet == null)
				mInheritedTagSet = new TagSet();
			if (child.mData.tags != 0)
				mInheritedTagSet.updateTags(child.mData.tags, true);
			if (child.mData.flags != 0)
				mInheritedTagSet.updateFlags(child.mData.flags, true);

            if (mData.flags != oldFlags)
                markItemModified(Change.MODIFIED_FLAGS);
            if (mData.tags != oldTags)
                markItemModified(Change.MODIFIED_TAGS);
        }

		// update unread counts
		updateUnread(child.mData.unreadCount);
	}

	protected void removeChild(MailItem child) throws ServiceException {
        markItemModified(Change.MODIFIED_CHILDREN);

		// update child list
		if (mChildren == null || !mChildren.contains(child.getId()))
			throw MailServiceException.IS_NOT_CHILD();
		mChildren.remove(child.getId());
        
        // Remove parent reference from the child 
        child.mData.parentId = -1;

		// update inherited tags, if applicable
		if (mInheritedTagSet != null && (child.mData.tags != 0 || child.mData.flags != 0)) {
            int oldFlags = mData.flags;
            long oldTags = mData.tags;

			if (child.mData.tags != 0)
				mInheritedTagSet.updateTags(child.mData.tags, false);
			if (child.mData.flags != 0)
				mInheritedTagSet.updateFlags(child.mData.flags, false);

            if (mData.flags != oldFlags)
                markItemModified(Change.MODIFIED_FLAGS);
            if (mData.tags != oldTags)
                markItemModified(Change.MODIFIED_TAGS);
		}

		// update unread counts
		updateUnread(-child.mData.unreadCount);
	}

	public static class PendingDelete {
        public int  rootId;
        public boolean incomplete;
		public long size;
		public List itemIds   = new ArrayList();
        public List unreadIds = new ArrayList();
        public List cascadeIds;
        public List indexIds  = new ArrayList();
        public Set  sharedIndex;
        public List blobs     = new ArrayList();
        public int  contacts  = 0;
        public Map  messages  = new HashMap();

        PendingDelete add(PendingDelete other) {
            if (other != null) {
                size     += other.size;
                contacts += other.contacts;
                itemIds.addAll(other.itemIds);
                unreadIds.addAll(other.unreadIds);
                blobs.addAll(other.blobs);
            }
            return this;
        }
	}

    static final boolean DELETE_ITEM = false, DELETE_CONTENTS = true;

    void delete() throws ServiceException {
        delete(DELETE_ITEM, null);
    }
    void delete(boolean childrenOnly) throws ServiceException {
        delete(childrenOnly, null);
    }
    void delete(TargetConstraint tcon) throws ServiceException {
        delete(DELETE_ITEM, tcon);
    }
    void delete(boolean childrenOnly, TargetConstraint tcon) throws ServiceException {
        if (!childrenOnly && !isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);

        // get the full list of things that are being removed
        PendingDelete info = getDeletionInfo(tcon);
        if (childrenOnly || info.incomplete) {
            // make sure to take the container's ID out of the list of deleted items
            info.itemIds.remove(new Integer(mId));
        } else {
            // update parent item's child list
            MailItem parent = getParent();
            if (parent != null)
                parent.removeChild(this);
        }

        assert(info != null && info.itemIds != null);
        mMailbox.markItemDeleted(info.itemIds);

        // update the mailbox's size
		mMailbox.updateSize(-info.size);
        mMailbox.updateContactCount(-info.contacts);

        // update conversations and unread counts on folders and tags
		propagateDeletion(info);

        // actually delete the item from the DB
        if (info.incomplete)
            DbMailItem.delete(mMailbox, info.itemIds);
        else if (childrenOnly)
            DbMailItem.deleteContents(this);
        else
            DbMailItem.delete(this);

        // remove the deleted item(s) from the mailbox's cache
		purgeCache(info, !childrenOnly && !info.incomplete);

        // cascade any other deletes
        if (info.cascadeIds != null && !info.cascadeIds.isEmpty()) {
            DbMailItem.delete(mMailbox, info.cascadeIds);
            mMailbox.markItemDeleted(info.cascadeIds);
            info.itemIds.addAll(info.cascadeIds);
        }

        // deal with index sharing
        if (info.sharedIndex != null && !info.sharedIndex.isEmpty())
        	DbMailItem.resolveSharedIndex(mMailbox, info);

        mMailbox.markOtherItemDirty(info);

        // write a deletion record for later sync
        if (mMailbox.isTrackingSync() && info.itemIds.size() > 0)
            DbMailItem.writeTombstone(mMailbox, info);

        // don't actually delete the blobs or index entries here; wait until after the commit
	}

	PendingDelete getDeletionInfo() throws ServiceException {
        Integer id = new Integer(mId);
		PendingDelete info = new PendingDelete();
        info.rootId = mId;
		info.size   = mData.size;
		info.itemIds.add(id);
        if (mData.unreadCount != 0 && mMailbox.mUnreadFlag.canTag(this))
            info.unreadIds.add(id);
        if (mData.indexId > 0) {
            if (!isTagged(mMailbox.mCopiedFlag))
                info.indexIds.add(new Integer(mData.indexId));
            else
                (info.sharedIndex = new HashSet()).add(new Integer(mData.indexId));
        }
        if (mData.blobDigest != null && mData.blobDigest.length() > 0)
			try {
                MailboxBlob mblob = StoreManager.getInstance().getMailboxBlob(mMailbox, mId, mData.modContent, mData.volumeId);
                if (mblob == null)
                    ZimbraLog.mailbox.error("missing blob for id: " + mId + ", change: " + mData.modContent);
                else
                    info.blobs.add(mblob);
			} catch (Exception e) { }
        int isMessage = (this instanceof Message ? 1 : 0);
        info.messages.put(new Integer(getFolderId()), new DbMailItem.LocationCount(isMessage, getSize()));
		return info;
	}
    PendingDelete getDeletionInfo(TargetConstraint tcon) throws ServiceException {
        return getDeletionInfo();
    }

	void propagateDeletion(PendingDelete info) throws ServiceException {
        for (Iterator it = info.messages.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Folder folder = mMailbox.getFolderById(((Integer) entry.getKey()).intValue());
            DbMailItem.LocationCount lc = (DbMailItem.LocationCount) entry.getValue();
            folder.updateMessageCount(-lc.count);
            folder.updateSize(-lc.size);
        }

        if (info.unreadIds.isEmpty())
            return;
        // FIXME: try to get these from cache (use mMailbox.getItemById[])
        List unreadData = DbMailItem.getById(mMailbox, info.unreadIds, TYPE_MESSAGE);
        for (Iterator it = unreadData.iterator(); it.hasNext(); )
            mMailbox.getItem((UnderlyingData) it.next()).updateUnread(-1);
	}

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
		// uncache cascades to uncache children
		if (purgeItem)
			mMailbox.uncache(this);
	}


    abstract String encodeMetadata();
    abstract Metadata   decodeMetadata(String metadata) throws ServiceException;

    protected void saveMetadata() throws ServiceException {
        saveMetadata(encodeMetadata());
    }
    protected void saveMetadata(String metadata) throws ServiceException {
        mData.modMetadata = mMailbox.getOperationChangeID();
        DbMailItem.saveMetadata(this, mData.size, metadata);
    }

    protected void saveSubject() throws ServiceException {
        mData.modMetadata = mMailbox.getOperationChangeID();
        DbMailItem.saveSubject(this, mData.size);
    }

    protected void saveData(String sender) throws ServiceException {
        saveData(sender, encodeMetadata());
    }
    protected void saveData(String sender, String metadata) throws ServiceException {
        mData.modMetadata = mMailbox.getOperationChangeID();
        DbMailItem.saveData(this, mData.size, mData.flags, sender, metadata);
    }


    private static final String CN_ID             = "id";
    private static final String CN_TYPE           = "type";
    private static final String CN_PARENT_ID      = "parent_id";
    private static final String CN_FOLDER_ID      = "folder_id";
    private static final String CN_DATE           = "date";
    private static final String CN_SIZE           = "size";
    private static final String CN_REVISION       = "rev";
    private static final String CN_BLOB_DIGEST    = "digest";
    private static final String CN_UNREAD_COUNT   = "unread";
    private static final String CN_FLAGS          = "flags";
    private static final String CN_TAGS           = "tags";
    private static final String CN_SUBJECT        = "subject";
    private static final String CN_INHERITED_TAGS = "inherited";
    private static final String CN_CHILDREN       = "children";

    protected StringBuffer appendCommonMembers(StringBuffer sb) {
        sb.append(CN_ID).append(": ").append(mId).append(", ");
        sb.append(CN_TYPE).append(": ").append(mData.type).append(", ");
        if (mData.parentId > 0)
        	sb.append(CN_PARENT_ID).append(": ").append(mData.parentId).append(", ");
        sb.append(CN_FOLDER_ID).append(": ").append(mData.folderId).append(", ");
        sb.append(CN_DATE).append(": ").append(mData.date).append(", ");
        sb.append(CN_SIZE).append(": ").append(mData.size).append(", ");
        sb.append(CN_REVISION).append(": ").append(mData.modContent).append(", ");
        sb.append(CN_UNREAD_COUNT).append(": ").append(mData.unreadCount).append(", ");
        if (mData.flags != 0)
        	sb.append(CN_FLAGS).append(": ").append(getFlagString()).append(", ");
        if (mData.tags != 0)
        	sb.append(CN_TAGS).append(": [").append(getTagString()).append("], ");
        if (mInheritedTagSet != null)
        	sb.append(CN_INHERITED_TAGS).append(": [").append(mInheritedTagSet.toString()).append("], ");
        if (mData.subject != null)
            sb.append(CN_SUBJECT).append(": ").append(mData.subject).append(", ");
        if (mChildren != null)
        	sb.append(CN_CHILDREN).append(": [").append(mChildren.toString()).append("], ");
        if (mData.blobDigest != null)
            sb.append(CN_BLOB_DIGEST).append(": ").append(mData.blobDigest);
        return sb;
    }

    private void validateFlags(int flags)
    throws ServiceException {
        if ((flags & ~Flag.FLAGS_ALL) > 0)
            throw ServiceException.FAILURE("Invalid value for flags: " + flags, null);
    }
}
