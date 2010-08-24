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
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.List;


import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @since Sep 7, 2004
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

        @Override
        public String toString()  { return x + "," + y + "," + width + "," + height; }
    }

    private Rectangle mBounds;


    public Note(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_NOTE)
            throw new IllegalArgumentException();
    }

    @Override
    public String getSender() {
        return "";
    }

    /** Returns the <code>Note</code>'s content. */
    public String getText() {
        return getSubject();
    }

    /** Returns the <code>Note</code>'s bounding box.  When dimensions were
     *  not specified, the returned {@link Note.Rectangle} will have
     *  <code>0</code> for each omitted dimension. */
    public Rectangle getBounds() {
        return new Rectangle(mBounds);
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
    boolean isIndexed() {
        return true;
    }

    @Override
    boolean canHaveChildren() { return false; }


    /** Creates a new Note and persists it to the database.  A real
     *  nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     *
     * @param id        The id for the new note.
     * @param folder    The {@link Folder} to create the note in.
     * @param content   The note's body.
     * @param location  The note's onscreen bounding box.
     * @param color     The note's color.
     * @param custom    An optional extra set of client-defined metadata.
     * @perms {@link ACL#RIGHT_INSERT} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't hold notes
     *    <li><code>mail.INVALID_REQUEST</code> - if the note has no content
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see Folder#canContain(byte) */
    static Note create(int id, Folder folder, String content, Rectangle location, Color color, CustomMetadata custom)
    throws ServiceException {
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
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId     = mbox.generateIndexId(id);
        data.date        = mbox.getOperationTimestamp();
        data.subject     = content;
        data.metadata    = encodeMetadata(color, 1, custom, location);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding Note: id=%d, folderId=%d, folderName=%s.",
            data.id, folder.getId(), folder.getName());
        DbMailItem.create(mbox, data, null);

        Note note = new Note(mbox, data);
        note.finishCreation(null);
        return note;
    }

    @Override
    public List<IndexDocument> generateIndexData(boolean doConsistencyCheck) {
        String toIndex = getText();
        IndexDocument doc = new IndexDocument();
        doc.addContent(toIndex);
        doc.addSubject(toIndex);
        doc.addPartName(LuceneFields.L_PARTNAME_NOTE);
        return Collections.singletonList(doc);
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

        addRevision(false);
        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE);
        // XXX: should probably update both mData.size and the Mailbox's size
        mData.subject = content;
        mData.date    = mMailbox.getOperationTimestamp();
        saveData(null);
    }

    protected void saveSubject() throws ServiceException {
        mData.contentChanged(mMailbox);
        DbMailItem.saveSubject(this);
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

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mBounds = new Rectangle(meta.get(Metadata.FN_BOUNDS, null));
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, mBounds);
    }

    private static String encodeMetadata(Color color, int version, CustomMetadata custom, Rectangle bounds) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, version, extended, bounds).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, Rectangle bounds) {
        meta.put(Metadata.FN_BOUNDS, bounds);
        return MailItem.encodeMetadata(meta, color, version, extended);
    }


    private static final String CN_BOUNDS  = "bounds";

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("note: {");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_BOUNDS).append(": ").append(mBounds);
        sb.append("}");
        return sb.toString();
    }
}
