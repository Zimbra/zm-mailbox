/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.zclient.event.ZModifyContactEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import org.json.JSONException;

import java.util.Map;

public class ZContactHit implements ZSearchHit {

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
    private float mScore;
    private long mMetaDataDate;
    private long mDate;
    private String mFullName;
        
    public ZContactHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTagIds = e.getAttribute(MailConstants.A_TAGS, null);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        mFileAsStr = e.getAttribute(MailConstants.A_FILE_AS_STR, null);
        mRevision = e.getAttribute(MailConstants.A_REVISION, null);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER);
        mType = e.getAttribute(MailConstants.A_CONTACT_TYPE, null);
        mEmail = e.getAttribute(Contact.A_email, null);
        mEmail2 = e.getAttribute(Contact.A_email2, null);
        mEmail3 = e.getAttribute(Contact.A_email3, null);
        mWorkEmail1 = e.getAttribute(Contact.A_workEmail1, null);
        mWorkEmail2 = e.getAttribute(Contact.A_workEmail2, null);
        mWorkEmail3 = e.getAttribute(Contact.A_workEmail3, null);
        mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mMetaDataDate = e.getAttributeLong(MailConstants.A_MODIFIED_DATE, 0) * 1000;
        mDlist = e.getAttribute(Contact.A_dlist, null);
        mFullName = e.getAttribute(Contact.A_fullName,null);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        jo.put("tags", mTagIds);
        jo.put("flags", mFlags);
        jo.put("sortField", mSortField);
        jo.put("type", mType);
        jo.put("score", mScore);
        jo.put("date", mDate);
        jo.put("fileAsStr", mFileAsStr);
        jo.put("revision", mRevision);
        jo.put("folderId", mFolderId);
        jo.put(Contact.A_dlist, mDlist);
        jo.put(Contact.A_email, mEmail);
        jo.put(Contact.A_email2, mEmail2);
        jo.put(Contact.A_email3, mEmail3);
        jo.put(Contact.A_workEmail1, mWorkEmail1);
        jo.put(Contact.A_workEmail2, mWorkEmail2);
        jo.put(Contact.A_workEmail3, mWorkEmail3);
        jo.put(Contact.A_fullName, mFullName);
        return jo;
    }
    
    public String toString() {
        return String.format("[ZContactHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public boolean isGroup() {
        return Contact.TYPE_GROUP.equalsIgnoreCase(getType());
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

    public String getId() {
        return mId;
    }

    public String getFlags() {
        return mFlags;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZContact.Flag.flagged.getFlagChar()) != -1;
    }

    public String getRevision() {
        return mRevision;
    }

    public float getScore() {
        return mScore;
    }

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
                String dlist = attrs != null ? attrs.get(Contact.A_dlist) : null;
                if (dlist != null) mDlist = dlist;
            }
        }
	}
}
