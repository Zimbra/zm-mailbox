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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.event.ZModifyEvent;

public class ZCallHit implements ZSearchHit {

	private String mId;
    private String mSortField;
    private float mScore;
    private long mDate;
    private long mDuration;
    private ZPhone mCaller;

    public ZCallHit(Element e) throws ServiceException {
    	mId = "ZCallHit";
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
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

    public String getDisplayCaller() { return mCaller.getDisplay(); }

    public long getDate() { return mDate; }

    public long getDuration() { return mDuration; }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        // No-op.
    }

}
