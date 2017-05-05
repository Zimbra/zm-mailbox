/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import com.zimbra.client.ZContact.Flag;
import com.zimbra.client.event.ZModifyContactEvent;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;

public class ZContactHit implements ZImapSearchHit {

    private String mId;
    private String mFlags;
    private String mTagIds;
    private String mSortField;
    private String mFileAsStr;
    private String mEmail, mEmail2, mEmail3, mWorkEmail1, mWorkEmail2, mWorkEmail3;
    private String mRevision;
    private String mFolderId;
    private String mType;
    private String mDlist;
    private long mMetaDataDate;
    private long mDate;
    private String mFullName;
    private String mFileAs;
    private String mNickname;
    private String mNamePrefix;
    private String mFirstName;
    private String mPhoneticFirstName;
    private String mMiddleName;
    private String mMaidenName;
    private String mLastName;
    private String mPhoneticLastName;
    private String mNameSuffix;
    private String mCompany;
    private String mPhoneticCompany;
    private Map<String, String> mAttrs;
    private Map<String, ZContactAttachmentInfo> mAttachments;
    private int imapUid;
    private int modSeq;

    public static class ZContactAttachmentInfo {
        private String mContentType;
        private String mFileName;
        private String mPart;
        private long mLength;

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

    public ZContactHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTagIds = e.getAttribute(MailConstants.A_TAGS, null);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mFileAsStr = e.getAttribute(MailConstants.A_FILE_AS_STR, null);
        mRevision = e.getAttribute(MailConstants.A_REVISION, null);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER,null);
        mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mMetaDataDate = e.getAttributeLong(MailConstants.A_CHANGE_DATE, 0) * 1000;
        imapUid = e.getAttributeInt(MailConstants.A_IMAP_UID, -1);
        modSeq = e.getAttributeInt(MailConstants.A_MODIFIED_SEQUENCE, -1);

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
        mEmail = attrs.get(ContactConstants.A_email);
        mEmail2 = attrs.get(ContactConstants.A_email2);
        mEmail3 = attrs.get(ContactConstants.A_email3);
        mWorkEmail1 = attrs.get(ContactConstants.A_workEmail1);
        mWorkEmail2 = attrs.get(ContactConstants.A_workEmail2);
        mWorkEmail3 = attrs.get(ContactConstants.A_workEmail3);

        mDlist = attrs.get(ContactConstants.A_dlist);
        mFullName = attrs.get(ContactConstants.A_fullName);
        mFileAs = attrs.get(ContactConstants.A_fileAs);
        mNickname = attrs.get(ContactConstants.A_nickname);
        mNamePrefix = attrs.get(ContactConstants.A_namePrefix);
        mFirstName = attrs.get(ContactConstants.A_firstName);
        mPhoneticFirstName = attrs.get(ContactConstants.A_phoneticFirstName);
        mMiddleName = attrs.get(ContactConstants.A_middleName);
        mMaidenName = attrs.get(ContactConstants.A_maidenName);
        mLastName = attrs.get(ContactConstants.A_lastName);
        mPhoneticLastName = attrs.get(ContactConstants.A_phoneticLastName);
        mNameSuffix = attrs.get(ContactConstants.A_nameSuffix);
        mCompany= attrs.get(ContactConstants.A_company);
        mPhoneticCompany= attrs.get(ContactConstants.A_phoneticCompany);
        mType = attrs.get(ContactConstants.A_type);
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        jo.put("tags", mTagIds);
        jo.put("flags", mFlags);
        jo.put("sortField", mSortField);
        jo.put("type", mType);
        jo.put("date", mDate);
        jo.put("fileAsStr", mFileAsStr);
        jo.put("revision", mRevision);
        jo.put("folderId", mFolderId);
        jo.put(ContactConstants.A_dlist, mDlist);
        jo.put(ContactConstants.A_email, mEmail);
        jo.put(ContactConstants.A_email2, mEmail2);
        jo.put(ContactConstants.A_email3, mEmail3);
        jo.put(ContactConstants.A_workEmail1, mWorkEmail1);
        jo.put(ContactConstants.A_workEmail2, mWorkEmail2);
        jo.put(ContactConstants.A_workEmail3, mWorkEmail3);
        jo.put(ContactConstants.A_fullName, mFullName);
        jo.putMap("attrs", mAttrs);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZContactHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public boolean isGroup() {
        return ContactConstants.TYPE_GROUP.equalsIgnoreCase(getType());
    }

    public boolean isContact() {
        return !isGroup();
    }

    public String getDlist() {
        return mDlist;
    }

    public String getType() {
        return mType;
    }

    public String getTagIds() {
        return mTagIds;
    }

    public boolean hasTags() {
        return mTagIds != null && mTagIds.length() > 0;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getEmail2() {
        return mEmail2;
    }

    public String getEmail3() {
        return mEmail3;
    }

    public String getWorkEmail1() {
        return mWorkEmail1;
    }

    public String getWorkEmail2() {
        return mWorkEmail2;
    }

    public String getWorkEmail3() {
        return mWorkEmail3;
    }

    public String getFileAsStr() {
        return mFileAsStr;
    }

    public String getFolderId() {
        return mFolderId;
    }

    @Override
    public String getId() {
        return mId;
    }

    public String getFlags() {
        return mFlags;
    }

    public Map<String, String> getAttrs() {
        return mAttrs;
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

    public String getAttachmentDataUrl(String name)
    throws ServiceException {
        String part = mAttachments.get(name).getPart();
        if (part == null) {
            throw ZClientException.CLIENT_ERROR("Invalid attachment name: " + name, null);
        }
        return String.format("?id=%s&part=%s", getId(), part);
    }

    public ZContactAttachmentInfo getAttachmentPartInfo(String name) {
        return mAttachments.get(name);
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(Flag.attachment.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZContact.Flag.flagged.getFlagChar()) != -1;
    }

    public String getRevision() {
        return mRevision;
    }

    @Override
    public String getSortField() {
        return mSortField;
    }

    public long getMetaDataChangedDate() {
        return mMetaDataDate;
    }

    public long getDate() {
        return mDate;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getFileAs() { return mFileAs; }
    public String getNickname() { return mNickname; }
    public String getNamePrefix() { return mNamePrefix; }
    public String getFirstName() { return mFirstName; };
    public String getPhoneticFirstName() { return mPhoneticFirstName; }
    public String getMiddleName() { return mMiddleName; }
    public String getMaidenName() { return mMaidenName; }
    public String getLastName() { return mLastName; }
    public String getPhoneticLastName() { return mPhoneticLastName; }
    public String getNameSuffix() { return mNameSuffix; }
    public String getCompany() { return mCompany; };
    public String getPhoneticCompany() { return mPhoneticCompany; }

    @Override
    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        if (event instanceof ZModifyContactEvent) {
            ZModifyContactEvent cevent = (ZModifyContactEvent) event;
            if (cevent.getId().equals(mId)) {
                mTagIds = cevent.getTagIds(mTagIds);
                mFolderId = cevent.getFolderId(mFolderId);
                mFlags = cevent.getFlags(mFlags);
                mRevision = cevent.getRevision(mRevision);
                mFileAsStr = cevent.getFileAsStr(mFileAsStr);
                mEmail = cevent.getEmail(mEmail);
                mEmail2 = cevent.getEmail(mEmail2);
                mEmail3 = cevent.getEmail(mEmail3);
                //mMetaDataChangedDate = cevent.getMetaDataChangedDate(mMetaDataChangedDate);
                Map<String, String> attrs = cevent.getAttrs(null);
                String dlist = attrs != null ? attrs.get(ContactConstants.A_dlist) : null;
                if (dlist != null) mDlist = dlist;
                mFullName = get(attrs, ContactConstants.A_fullName, mFullName);
                mFileAs = get(attrs, ContactConstants.A_fileAs, mFileAs);
                mNickname = get(attrs, ContactConstants.A_nickname, mNickname);
                mFirstName = get(attrs, ContactConstants.A_firstName, mFirstName);
                mPhoneticFirstName = get(attrs, ContactConstants.A_phoneticFirstName, mPhoneticFirstName);
                mLastName = get(attrs, ContactConstants.A_lastName, mLastName);
                mPhoneticLastName = get(attrs, ContactConstants.A_phoneticLastName, mPhoneticLastName);
                mCompany = get(attrs, ContactConstants.A_company, mCompany);
                mPhoneticCompany = get(attrs, ContactConstants.A_phoneticCompany, mPhoneticCompany);
                mAttrs = cevent.getAttrs(mAttrs);
            }
        }
    }

    private String get(Map<String,String> attrs, String key, String defaultValue) {
        String value = attrs != null ? attrs.get(key) : null;
        return value != null ? value : defaultValue;
    }

    @Override
    public int getItemId() throws ServiceException {
        return new ItemIdentifier(mId,  null).id;
    }

    @Override
    public int getParentId() throws ServiceException {
        return -1;
    }

    @Override
    public int getModifiedSequence() throws ServiceException {
        return modSeq;
    }

    @Override
    public MailItemType getMailItemType() throws ServiceException {
        return MailItemType.CONTACT;
    }

    @Override
    public int getImapUid() throws ServiceException {
        return imapUid;
    }

    @Override
    public int getFlagBitmask() throws ServiceException {
        return ZItem.Flag.toBitmask(mFlags);
    }

    @Override
    public String[] getTags() throws ServiceException {
        return mTagIds == null ? null : mTagIds.split(",");
    }
}
