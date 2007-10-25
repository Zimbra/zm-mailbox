/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyVoiceMailItemEvent;

public class ZVoiceMailItemHit implements ZSearchHit {

	private String mId;
    private String mSortField;
    private float mScore;
    private String mFlags;
    private String mSoundUrl;
    private long mDate;
    private long mDuration;
    private ZPhone mCaller;

    public ZVoiceMailItemHit(Element e) throws ServiceException {
    	mId = e.getAttribute(MailConstants.A_ID);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        Element content = e.getOptionalElement(MailConstants.E_CONTENT);
        if (content != null) {
            mSoundUrl = content.getAttribute(MailConstants.A_URL);
        }
        mDate = e.getAttributeLong(MailConstants.A_DATE);
        mDuration = e.getAttributeLong(VoiceConstants.A_VMSG_DURATION) * 1000;
        for (Element el : e.listElements(VoiceConstants.E_CALLPARTY)) {
            String t = el.getAttribute(MailConstants.A_ADDRESS_TYPE, null);
            if (ZEmailAddress.EMAIL_TYPE_FROM.equals(t)) {
                mCaller = new ZPhone(el.getAttribute(VoiceConstants.A_PHONENUM));
                break;
            }
        }
    }

    private ZVoiceMailItemHit() { }

    public static ZVoiceMailItemHit deserialize(String value, String phone) throws ServiceException {
        ZVoiceMailItemHit result = new ZVoiceMailItemHit();
        String[] array = value.split("/");
        result.mId = array[0];
        result.mSortField = array[1];
        result.mFlags = array[2];
        result.mDate = Long.parseLong(array[3]);
        result.mDuration = Long.parseLong(array[4]);
        result.mCaller = new ZPhone(array[5]);
        result.mScore = 0;
        result.mSoundUrl =
            "/service/extension/velodrome/voice/~/voicemail?phone=" +
            phone + "&id=" + result.mId;
        return result;
    }

    public String getId() {
		return mId;
	}

	public float getScore() {
		return mScore;
	}

	public String getSortField() {
		return mSortField;
	}

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.highPriority.getFlagChar()) != -1; 
    }

    public boolean isPrivate() {
        return hasFlags() && mFlags.indexOf(VoiceConstants.FLAG_UNFORWARDABLE) != -1;
    }

    public boolean isUnheard() {
		return hasFlags() && mFlags.indexOf(ZMessage.Flag.unread.getFlagChar()) != -1;
	}

	public ZPhone getCaller() { return mCaller; }

    public String getDisplayCaller() { return mCaller.getDisplay(); }

    public String getSoundUrl() { return mSoundUrl; }

    public long getDate() { return mDate; }

    public long getDuration() { return mDuration; }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
		if (event instanceof ZModifyVoiceMailItemEvent) {
			setFlag(ZMessage.Flag.unread.getFlagChar(), !((ZModifyVoiceMailItemEvent) event).getIsHeard());
		}
	}

    public String serialize() {
        return  mId + "/" +
                mSortField + "/" +
                mFlags + "/" +
                mDate + "/" +
                mDuration + "/" +
                mCaller.getName();
    }

	private void setFlag(char flagChar, boolean on) {
		if (on) {
			mFlags += flagChar;
		} else {
			mFlags = mFlags.replace(Character.toString(flagChar), "");
		}
	}

}
