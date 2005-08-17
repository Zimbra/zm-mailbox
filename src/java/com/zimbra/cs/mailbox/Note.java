/*
 * Created on Sep 7, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.StringUtil;


/**
 * @author dkarp
 */
public class Note extends MailItem {

    public static class Rectangle {
        public int x, y, width, height;

        public Rectangle()  {}
        public Rectangle(Rectangle r)                              { x = r.x;  y = r.y;  width = r.width;  height = r.height; }
        public Rectangle(int ax, int ay, int awidth, int aheight)  { x = ax;  y = ay;  width = awidth;  height = aheight; }
        public Rectangle(String bounds) {
            if (bounds == null)
                return;
            String[] dimensions = bounds.split(",");
            if (dimensions.length == 2 || dimensions.length == 4) {
                x = Integer.parseInt(dimensions[0]);  y = Integer.parseInt(dimensions[1]);
                if (dimensions.length == 4) {
                    width = Integer.parseInt(dimensions[2]);  height = Integer.parseInt(dimensions[3]);
                }
            }
        }

        public boolean equals(Rectangle r) {
            return (r != null && x == r.x && y == r.y && width == r.width && height == r.height);
        }

        public String toString()  { return x + "," + y + "," + width + "," + height; }
    }

    public static final byte DEFAULT_COLOR = 0;
    
    private Rectangle mBounds;
    private byte      mColor;

    public Note(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_NOTE)
            throw new IllegalArgumentException();
    }

    public String getContent() {
        return getSubject();
    }

    public Rectangle getBounds() {
        return new Rectangle(mBounds);
    }
    
    public byte getColor() {
        return mColor;
    }


    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return true; }
    boolean canHaveChildren() { return false; }


    static Note create(int id, Folder folder, String content, Rectangle location, byte color) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_NOTE))
            throw MailServiceException.CANNOT_CONTAIN();
        content = StringUtil.stripControlCharacters(content);
        if (content == null || content.equals(""))
            throw ServiceException.INVALID_REQUEST("notes may not be empty", null);
        if (location == null)
            location = new Rectangle();

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_NOTE;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.date        = mbox.getOperationTimestamp();
        data.subject     = content;
        data.metadata    = encodeMetadata(location, color);
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);
        
        Note note = new Note(mbox, data);
        note.finishCreation(null);
        return note;
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexNote(redo, getMailboxId(), mId, this);
    }

    void setContent(String content) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        content = StringUtil.stripControlCharacters(content);
        if (content == null || content.equals(""))
            throw ServiceException.INVALID_REQUEST("notes may not be empty", null);
        if (content.equals(mData.subject))
            return;
        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE);
        // XXX: should probably update both mData.size and the Mailbox's size
        mData.subject = content;
        mData.date    = mMailbox.getOperationTimestamp();
        saveSubject();
    }

    void reposition(Rectangle bounds) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (bounds == null)
            throw ServiceException.INVALID_REQUEST("must specify bounds", null);

        if (bounds.equals(mBounds))
            return;
        markItemModified(Change.MODIFIED_POSITION);
        mBounds = new Rectangle(bounds);
        saveMetadata();
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


    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta  = new Metadata(metadata, this);
        mColor = (byte) meta.getLong(Metadata.FN_COLOR, DEFAULT_COLOR);
        mBounds = new Rectangle(meta.get(Metadata.FN_BOUNDS, null));
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mBounds, mColor);
    }
    private static String encodeMetadata(Rectangle bounds, byte color) {
        Metadata meta = new Metadata();
        meta.put(Metadata.FN_COLOR,  color);
        meta.put(Metadata.FN_BOUNDS, bounds);
        return meta.toString();
    }


    private static final String CN_COLOR   = "color";
    private static final String CN_BOUNDS  = "bounds";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("note: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_COLOR).append(": ").append(mColor).append(", ");
        sb.append(CN_BOUNDS).append(": ").append(mBounds);
        sb.append("}");
        return sb.toString();
    }
}
