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
 * Created on Sep 7, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.util.StringUtil;


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

    private Rectangle mBounds;


    public Note(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_NOTE)
            throw new IllegalArgumentException();
    }

    /** Returns the <code>Note</code>'s content. */
    public String getContent() {
        return getSubject();
    }

    /** Returns the <code>Note</code>'s bounding box.  When dimensions were
     *  not specified, the returned {@link Note.Rectangle} will have
     *  <code>0</code> for each omitted dimension. */
    public Rectangle getBounds() {
        return new Rectangle(mBounds);
    }


    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return true; }
    boolean canHaveChildren() { return false; }


    /** Creates a new Note and persists it to the database.  A real
     *  nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id        The id for the new note.
     * @param folder    The {@link Folder} to create the note in.
     * @param volumeId  The volume to persist the note's blob in.
     * @param content   The note's body.
     * @param location  The note's onscreen bounding box.
     * @param color     The note's color.
     * @perms {@link ACL#RIGHT_INSERT} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't hold notes
     *    <li><code>mail.INVALID_REQUEST</code> - if the note has no content
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see Folder#canContain(byte) */
    static Note create(int id, Folder folder, short volumeId, String content, Rectangle location, byte color) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_NOTE))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the folder");
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
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.subject     = content;
        data.metadata    = encodeMetadata(color, location);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);
        
        Note note = new Note(mbox, data);
        note.finishCreation(null);
        return note;
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            mMailbox.getMailboxIndex().indexNote(mMailbox, redo, mId, this);
    }

    void setContent(String content) throws ServiceException {
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the note");
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
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the note");
        if (bounds == null)
            throw ServiceException.INVALID_REQUEST("must specify bounds", null);

        if (bounds.equals(mBounds))
            return;
        markItemModified(Change.MODIFIED_POSITION);
        mBounds = new Rectangle(bounds);
        saveMetadata();
    }

    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mBounds = new Rectangle(meta.get(Metadata.FN_BOUNDS, null));
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mBounds);
    }
    private static String encodeMetadata(byte color, Rectangle bounds) {
        return encodeMetadata(new Metadata(), color, bounds).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, Rectangle bounds) {
        meta.put(Metadata.FN_BOUNDS, bounds);
        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_BOUNDS  = "bounds";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("note: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_BOUNDS).append(": ").append(mBounds);
        sb.append("}");
        return sb.toString();
    }
}
