/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.service.ServiceException;

public class ZPhoneAccount {
    private ZFolder folder;
    private ZPhone phone;
    private ZCallFeatures callFeatures;
	private boolean hasVoiceMail;
    private String phoneType;

	public ZPhoneAccount(Element e, ZMailbox mbox) throws ServiceException {
        phone = new ZPhone(e.getAttribute(MailConstants.A_NAME));
        folder = new ZVoiceFolder(e.getElement(MailConstants.E_FOLDER), null, mbox);
        callFeatures = new ZCallFeatures(mbox, phone, e.getElement(VoiceConstants.E_CALL_FEATURES));
		hasVoiceMail = e.getAttributeBool(VoiceConstants.E_VOICEMSG);
        try{
            phoneType = e.getAttribute(VoiceConstants.A_TYPE);
        }
        catch(ServiceException ex){
            phoneType = null;
        }

	}

    public ZFolder getRootFolder() {
        return folder;
    }

    public ZPhone getPhone() {
        return phone;
    }

    public ZCallFeatures getCallFeatures() throws ServiceException {
        callFeatures.loadCallFeatures();
        return callFeatures;
    }

	public boolean getHasVoiceMail() {
		return hasVoiceMail;
	}

    public String getPhoneType() {
        return phoneType;
    }
}
