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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
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

public class ZPhoneAccount {
    private ZFolder mFolder;
    private ZPhone mPhone;
    private ZCallFeatures mCallFeatures;

    public ZPhoneAccount(Element e, ZMailbox mbox) throws ServiceException {
        mPhone = new ZPhone(e.getAttribute(MailConstants.A_NAME));
        mFolder = new ZFolder(e.getElement(MailConstants.E_FOLDER), null);
        mCallFeatures = new ZCallFeatures(mbox, mPhone, e.getElement(VoiceConstants.E_CALL_FEATURES));
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
}
