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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.service.ServiceException;

public class ZPhoneAccount {
    private ZFolder mFolder;
    private ZPhone mPhone;
    private ZCallFeatures mCallFeatures;
	private boolean mHasVoiceMail;

	public ZPhoneAccount(Element e, ZMailbox mbox) throws ServiceException {
        mPhone = new ZPhone(e.getAttribute(MailConstants.A_NAME));
        mFolder = new ZVoiceFolder(e.getElement(MailConstants.E_FOLDER), null);
        mCallFeatures = new ZCallFeatures(mbox, mPhone, e.getElement(VoiceConstants.E_CALL_FEATURES));
		mHasVoiceMail = e.getAttributeBool(VoiceConstants.E_VOICEMSG);
	}

    public ZFolder getRootFolder() {
        return mFolder;
    }

    public ZPhone getPhone() {
        return mPhone;
    }

    public ZCallFeatures getCallFeatures() throws ServiceException {
        mCallFeatures.loadCallFeatures();
        return mCallFeatures;
    }

	public boolean getHasVoiceMail() {
		return mHasVoiceMail;
	}
}
