/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyContactEvent;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;

public class ZContact extends ZBaseItem implements ToZJSONObject {

    /** "File as" setting: &nbsp;<code>Last, First</code> */
    public static final String FA_LAST_C_FIRST = "1";
    /** "File as" setting: &nbsp;<code>First Last</code> */
    public static final String FA_FIRST_LAST = "2";
    /** "File as" setting: &nbsp;<code>Company</code> */
    public static final String FA_COMPANY = "3";
    /** "File as" setting: &nbsp;<code>Last, First (Company)</code> */
    public static final String FA_LAST_C_FIRST_COMPANY = "4";
    /** "File as" setting: &nbsp;<code>First Last (Company)</code> */
    public static final String FA_FIRST_LAST_COMPANY = "5";
    /** "File as" setting: &nbsp;<code>Company (Last, First)</code> */
    public static final String FA_COMPANY_LAST_C_FIRST = "6";
    /** "File as" setting: &nbsp;<code>Company (First Last)</code> */
    public static final String FA_COMPANY_FIRST_LAST = "7";
    /** "File as" setting: <i>[explicitly specified "file as" string]</i> */
    public static final String FA_EXPLICIT = "8";

    public static class ZContactAttachmentInfo {
        private final String mContentType;
        private final String mFileName;
        private final String mPart;
        private final long mLength;

        public ZContactAttachmentInfo(String part, String fileName, String contentType, long length) {
            mPart = part;
            mFileName = fileName;
            mContentType = contentType;
            mLength = length;
        }

        public String getContentType() {
            return mContentType;
        }
        public String getFileName() {
            return mFileName;
        }
        public String getPart() {
            return mPart;
        }
        public long getLength() {
            return mLength;
        }
    }

    private String mRefId;
    private String mFolderId;
    private String mRevision;
    private long mDate;
    private long mMetaDataChangedDate;
    private Map<String, String> mAttrs;
    private Map<String, ZContact> mMembers;
    private Map<String, ZContactAttachmentInfo> mAttachments;
    private boolean mGalContact;
    private boolean isDirty;
    private ContactMemberType mContactMemberType;

    /* Note for contacts, use a more limited set of flags than in ZItem */
    public enum Flag {
        flagged('f'),
        attachment('a');

        private char mFlagChar;

        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Flag(char flagChar) {
            mFlagChar = flagChar;
        }
    }

    public enum ContactMemberType {
        contact("C"),
        galContact("G"),
        inlineContact("I");

        private String contactMemberType;

        public String getContactMemberType() { return contactMemberType;}

        ContactMemberType(String type) {
            contactMemberType = type;
        }
    }

    /**
     * Checks the type of the contact member.
     * @return true if the contact member is an inline contact.
     */
    public boolean isTypeI() {
        return (mContactMemberType == ContactMemberType.inlineContact);
    }

    /**
     * Constructor called only in case of inline contacts
     * @param id email address of the inline contact
     * @throws ServiceException
     */
    public ZContact(String id) throws ServiceException {
        super(id);
        isDirty = false;
        mContactMemberType = ContactMemberType.inlineContact;
    }

    public ZContact(Element e, boolean galContact, ZMailbox mailbox) throws ServiceException {
        this(e, mailbox);
        mGalContact = galContact;
        mContactMemberType = galContact ? ContactMemberType.galContact : ContactMemberType.contact;
    }

    public ZContact(Element e, ZMailbox zmailbox) throws ServiceException {
        super(e.getAttribute(MailConstants.A_ID),
                e.getAttributeInt(MailConstants.A_IMAP_UID, -1),
                e.getAttributeInt(MailConstants.A_MODIFIED_SEQUENCE, 0));
        isDirty = false;
        mMailbox = zmailbox;
        mRefId = e.getAttribute(MailConstants.A_REF, null);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTagIds = e.getAttribute(MailConstants.A_TAGS, null);
        mRevision = e.getAttribute(MailConstants.A_REVISION, null);
        mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mMetaDataChangedDate = e.getAttributeLong(MailConstants.A_CHANGE_DATE, 0) * 1000;

        HashMap<String, String> attrs = new HashMap<String, String>();
        HashMap<String, ZContactAttachmentInfo> attachments = new HashMap<String, ZContactAttachmentInfo>();

        for (Element attrEl : e.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = attrEl.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            String part = attrEl.getAttribute(MailConstants.A_PART, null);
            if (part != null) {
                String fileName = attrEl.getAttribute(MailConstants.A_CONTENT_FILENAME, null);
                String contentType = attrEl.getAttribute(MailConstants.A_CONTENT_TYPE, null);
                long size = attrEl.getAttributeLong(MailConstants.A_SIZE, 0);
                attachments.put(name, new ZContactAttachmentInfo(part, fileName, contentType, size));
            } else {
                attrs.put(name, attrEl.getText());
            }
        }

        mAttrs = Collections.unmodifiableMap(attrs);
        mAttachments = Collections.unmodifiableMap(attachments);

        HashMap<String, ZContact> members = new HashMap<String, ZContact>();
        for (Element memberEl : e.listElements(MailConstants.E_CONTACT_GROUP_MEMBER)) {
            String id = memberEl.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_VALUE);
            String type = memberEl.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_TYPE);
            Element cnEl = memberEl.getOptionalElement(MailConstants.E_CONTACT);
            ZContact contact = null;
            if (cnEl != null)
                contact = new ZContact(cnEl, type.equals("G") ? true : false, zmailbox);
            else
                /**
                 * Inline contacts only have the email address as value and type as I.
                 */
                contact = new ZContact(id);
            members.put(id, contact);
        }
        mMembers = Collections.unmodifiableMap(members);
    }

    public String getFolderId() {
        return mFolderId;
    }

    @Override
    public int getFolderIdInMailbox() throws ServiceException {
        return getIdInMailbox(getFolderId());
    }

    public ZFolder getFolder() throws ServiceException {
        return mMailbox.getFolderById(mFolderId);
    }

    @Override
    public String getId() {
        return mId;
    }

    public String getRefId() {
        return mRefId;
    }

    public boolean isGalContact() {
        return mGalContact;
    }

    public boolean isGroup() { return (getAttrs().get("type") != null && getAttrs().get("type").equals("group")); }

    public boolean getIsGroup() { return isGroup(); }

    @Deprecated
    public List<ZEmailAddress> getGroupMembers() throws ServiceException {
        return ZEmailAddress.parseAddresses(getAttrs().get("dlist"), ZEmailAddress.EMAIL_TYPE_TO);
    }

    /**
     * @return first email from email/2/3 that is set, or an empty string
     */
    public String getDisplayEmail() {
        if (!StringUtil.isNullOrEmpty(getAttrs().get("email")))
            return getAttrs().get("email");
        else if (!StringUtil.isNullOrEmpty(getAttrs().get("email2")))
            return getAttrs().get("email2");
        else if (!StringUtil.isNullOrEmpty(getAttrs().get("email3")))
            return getAttrs().get("email3");
        else
            return "";
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        if (imapUid >= 0) {
            jo.put("imapUid", imapUid);
        }
        jo.put("folderId", mFolderId);
        jo.put("flags", mFlags);
        jo.put("tagIds", mTagIds);
        jo.put("date", mDate);
        jo.put("metaDataChangedDate", mMetaDataChangedDate);
        jo.put("revision", mRevision);
        jo.put("isFlagged", isFlagged());
        jo.put("isGalContact", isGalContact());
        jo.put("isGroup", isGroup());
        jo.put("hasFlags", hasFlags());
        jo.put("hasTags", hasTags());
        jo.putMap("attrs", mAttrs);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZContact %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public Map<String, String> getAttrs() {
        return mAttrs;
    }

    public Map<String, ZContact> getMembers() {
        return mMembers;
    }
    /**
     * Returns the attachment names, or an empty set.
     */
    public Set<String> getAttachmentNames() {
        return mAttachments.keySet();
    }

    public String getAttachmentPartName(String name) {
        return mAttachments.get(name).getPart();
    }

    public InputStream getAttachmentData(String name)
    throws ServiceException {
        String part = mAttachments.get(name).getPart();
        if (part == null) {
            throw ZClientException.CLIENT_ERROR("Invalid attachment name: " + name, null);
        }
        String url = String.format("?id=%s&part=%s", getId(), part);
        return mMailbox.getRESTResource(url);
    }

    public InputStream getAttachmentDataForSharedContact(String name, String ownerEmail)
        throws ServiceException {
        ZimbraLog.misc.info("Attachment data for contact: :%s", ownerEmail) ;
            String part = mAttachments.get(name).getPart();
            if (part == null) {
                throw ZClientException.CLIENT_ERROR("Invalid attachment name: " + name, null);
            }
            String url = String.format("?id=%s&part=%s", getId(), part);
            return mMailbox.getRESTResourceForSharedMailItem(url, ownerEmail);
        }

    public ZContactAttachmentInfo getAttachmentPartInfo(String name) {
        return mAttachments.get(name);
    }

    @Override
    public long getDate() {
        return mDate;
    }

    public long getMetaDataChangedDate() {
        return mMetaDataChangedDate;
    }

    public String getRevision() {
        return mRevision;
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(Flag.attachment.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(Flag.flagged.getFlagChar()) != -1;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void modifyNotification(ZModifyContactEvent cevent) throws ServiceException {
        if (cevent.getId().equals(mId)) {
            mTagIds = cevent.getTagIds(mTagIds);
            mFolderId = cevent.getFolderId(mFolderId);
            mFlags = cevent.getFlags(mFlags);
            mRevision = cevent.getRevision(mRevision);
            mMetaDataChangedDate = cevent.getDate(mDate);
            mMetaDataChangedDate = cevent.getMetaDataChangedDate(mMetaDataChangedDate);
            mAttrs = cevent.getAttrs(mAttrs);
            if(isGroup())
                isDirty = true;
        }
	}

    public void delete() throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.deleteContact(mId);
    }

    public void deleteItem() throws ServiceException {
        delete();
    }

    public void trash() throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.trashContact(mId);
    }

    public void flag(boolean flagged) throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.flagContact(mId, flagged);
    }

    public void tag(String nameOrId, boolean tagged) throws ServiceException {
        ZTag tag = mMailbox.getTag(nameOrId);
        if (tag == null)
            throw ZClientException.CLIENT_ERROR("unknown tag: "+nameOrId, null);
        else
           tag(tag, tagged);
    }

    public void tag(ZTag tag, boolean tagged) throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.tagContact(mId, tag.getId(), tagged);
    }

    public void move(String pathOrId) throws ServiceException {
        ZFolder destFolder = mMailbox.getFolder(pathOrId);
        if (destFolder == null)
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        else
            move(destFolder);
    }

    public void move(ZFolder destFolder) throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.moveContact(mId, destFolder.getId());
    }

    public void modify(Map<String,String> attrs, boolean replace) throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.modifyContact(mId, replace, attrs);
    }

    // TODO: better handling of folder/tag ids
    public void update(String destFolderId, String tagList, String myflags) throws ServiceException {
        if (isGalContact()) throw ZClientException.CLIENT_ERROR("can't modify GAL contact", null);
        mMailbox.updateContact(mId, destFolderId, tagList, myflags);
    }

    @Override
    public MailItemType getMailItemType() {
        return MailItemType.CONTACT;
    }

    @Override
    public long getSize() {
        //currently there is no code path that calls this method
        throw new UnsupportedOperationException("ZContact method not supported yet");
    }

    @Override
    public InputStream getContentStream() throws ServiceException {
        return mMailbox.getRESTResource(String.format("?id=%s", this.getId()));
    }

    /**
     * @return the UID the item is referenced by in the IMAP server.  Returns <tt>0</tt> for items that require
     * renumbering because of moves.
     * The "IMAP UID" will be the same as the item ID unless the item has been moved after the mailbox owner's first
     * IMAP session. */
    @Override
    public int getImapUid() {
        if (imapUid >= 0) {
            return imapUid;
        }
        if (isGalContact()) {
            return 0;  /* TODO: is this the best thing to do? */
        }
        ZimbraLog.mailbox.debug("ZContact getImapUid() - regetting UID");
        ZContact zc = null;
        try {
            /* Perhaps, this ZContact object was not created in a way which included Imap UID information (or
             * a renumber is under way).  Using this mechanism guarantees that the Imap UID will be asked for,
             * or if there is a cache hit, the mechanism that put the entry in the cache should have ensured
             * Imap UID info was provided. */
            zc = getMailbox().getContact(mId);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.debug("ZContact getImapUid() - getContact failed", e);
            return 0;
        }
        if (null == zc) {
            return 0;
        }
        imapUid = (zc.imapUid <=0 ) ? 0 : zc.imapUid;
        return imapUid;
    }
}
