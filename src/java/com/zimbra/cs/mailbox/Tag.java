/*
 * Created on Jul 12, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public class Tag extends MailItem {
    public static final byte DEFAULT_COLOR = 0;

    private byte mColor;

    Tag(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_TAG && mData.type != TYPE_FLAG)
            throw new IllegalArgumentException();
    }


    public String getName() {
        return getSubject();
    }

    public byte getIndex() {
        return (byte) (mId - TAG_ID_OFFSET);
    }

    public long getBitmask() {
        return 1L << getIndex();
    }

    public byte getColor() {
        return mColor;
    }

    boolean canTag(MailItem item) {
        return item.isTaggable();
    }


    static long tagsToBitmask(String csv) {
        long bitmask = 0;
        if (csv != null && !csv.equals("")) {
            String[] tags = csv.split(",");
            for (int i = 0; i < tags.length; i++) {
                long value = 0;
                try {
                    value = Long.parseLong(tags[i]);
                } catch (NumberFormatException e) {
                    ZimbraLog.mailbox.error("unable to parse tags: '" + csv + "'", e);
                    throw e;
                }
                
                if (value < MailItem.TAG_ID_OFFSET || value >= MailItem.TAG_ID_OFFSET + MailItem.MAX_TAG_COUNT)
                    continue;
                // FIXME: should really check this against the existing tags in the mailbox
                bitmask |= 1L << (value - MailItem.TAG_ID_OFFSET);
            }
        }
        return bitmask;
    }

    static String bitmaskToTags(long bitmask) {
        if (bitmask == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; bitmask != 0 && i < MAX_TAG_COUNT - 1; i++)
            if ((bitmask & (1L << i)) != 0) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(i + TAG_ID_OFFSET);
                bitmask &= ~(1L << i);
            }
        return sb.toString();
    }


    boolean isTaggable()      { return false; }
    boolean isCopyable()      { return false; }
    boolean isMovable()       { return false; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return false; }
    boolean canHaveChildren() { return false; }


    static Tag create(Mailbox mbox, int id, String name, byte color)
    throws ServiceException {
        if (id < TAG_ID_OFFSET || id >= TAG_ID_OFFSET + MAX_TAG_COUNT)
            throw MailServiceException.INVALID_ID(id);
        Folder tagFolder = mbox.getFolderById(Mailbox.ID_FOLDER_TAGS);
        if (tagFolder == null)
            throw MailServiceException.NO_SUCH_FOLDER(Mailbox.ID_FOLDER_TAGS);
        name = validateTagName(name);
        try {
            // if we can successfully get a tag with that name, we've got a naming conflict
            mbox.getTagByName(name);
            throw MailServiceException.ALREADY_EXISTS(name);
        } catch (MailServiceException.NoSuchItemException nsie) {}

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = MailItem.TYPE_TAG;
        data.folderId    = tagFolder.getId();
        data.date        = mbox.getOperationTimestamp();
        data.subject     = name;
        data.metadata    = encodeMetadata(color);
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);

        Tag tag = new Tag(mbox, data);
        tag.finishCreation(null);
        return tag;
    }
    
    void rename(String name) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        name = validateTagName(name);
        if (name.equals(mData.subject))
            return;
        try {
            // if there's already a different tag with that name, we've got a naming conflict
            if (mMailbox.getTagByName(name) != this)
                throw MailServiceException.ALREADY_EXISTS(name);
        } catch (MailServiceException.NoSuchItemException nsie) {}

        markItemModified(Change.MODIFIED_NAME);
        mData.subject = name;
        saveSubject();
    }

    private static final String INVALID_PREFIX     = "\\";
    private static final String INVALID_CHARACTERS = ".*[:/\"\t\r\n].*";
    private static final int    MAX_TAG_LENGTH     = 128;

    private static String validateTagName(String name) throws ServiceException {
        if (name == null || name != StringUtil.stripControlCharacters(name))
            throw MailServiceException.INVALID_NAME(name);
        name = name.trim();
        if (name.equals("") || name.length() > MAX_TAG_LENGTH || name.startsWith(INVALID_PREFIX) ||
                name.matches(INVALID_CHARACTERS))
            throw MailServiceException.INVALID_NAME(name);
        return name;
    }

    void setColor(byte color) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);

        if (color == mColor)
            return;
        markItemModified(Change.MODIFIED_COLOR);
        mColor = color;
        saveMetadata();
    }

    void alterUnread(boolean unread) throws ServiceException {
        if (unread)
            throw ServiceException.INVALID_REQUEST("tags can only be marked read", null);
        if (!isUnread())
            return;

        // Decrement the in-memory unread count of each message.  Each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        List unreadData = DbMailItem.getUnreadMessages(this);
        for (Iterator it = unreadData.iterator(); it.hasNext(); ) {
            Message msg = mMailbox.getMessage((UnderlyingData) it.next());
            msg.updateUnread(unread ? 1 : -1);
        }

        // Mark all messages in this folder as read in the database
        DbMailItem.alterUnread(this, unread);
    }

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // remove the tag from all items in the database
        DbMailItem.clearTag(this);
        // dump entire item cache (necessary now because we reuse tag ids)
        mMailbox.purge(TYPE_MESSAGE);
        // remove tag from tag cache
        super.purgeCache(info, purgeItem);
    }


    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);
        mColor = (byte) meta.getLong(Metadata.FN_COLOR, DEFAULT_COLOR);
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mColor);
    }
    private static String encodeMetadata(byte color) {
        Metadata meta = new Metadata();
        if (color != DEFAULT_COLOR)
            meta.put(Metadata.FN_COLOR, color);
        return meta.toString();
    }


    private static final String CN_COLOR = "color";
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("tag: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_COLOR).append(": ").append(mColor);
        sb.append("}");
        return sb.toString();
    }
}
