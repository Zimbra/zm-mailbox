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
import org.json.JSONException;

public class ZCallHit implements ZSearchHit {

	private String mId;
    private String mSortField;
    private float mScore;
    private long mDate;
    private long mDuration;
    private ZPhone mCaller;
    private ZPhone mRecipient;

    public ZCallHit(Element e) throws ServiceException {
    	mId = "ZCallHit";
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        mDate = e.getAttributeLong(MailConstants.A_DATE);
        mDuration = e.getAttributeLong(VoiceConstants.A_VMSG_DURATION) * 1000;
		for (Element el : e.listElements(VoiceConstants.E_CALLPARTY)) {
            String addressType = el.getAttribute(MailConstants.A_ADDRESS_TYPE, null);
            if (ZEmailAddress.EMAIL_TYPE_FROM.equals(addressType)) {
                mCaller = new ZPhone(el.getAttribute(VoiceConstants.A_PHONENUM));
            } else {
				mRecipient = new ZPhone(el.getAttribute(VoiceConstants.A_PHONENUM));
			}
        }
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

    public ZPhone getCaller() { return mCaller; }

	public ZPhone getRecipient() { return mRecipient; }

    public String getDisplayCaller() { return mCaller.getDisplay(); }

	public String getDisplayRecipient() { return mRecipient.getDisplay(); }

    public long getDate() { return mDate; }

    public long getDuration() { return mDuration; }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        // No-op.
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("sortField", mSortField);
        zjo.put("score", mScore);
        zjo.put("date", mDate);
        zjo.put("duration", mDuration);
        zjo.put("caller", mCaller);
        zjo.put("recipient", mRecipient);
        return zjo;
    }

    public String toString() {
        return String.format("[ZCallHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
