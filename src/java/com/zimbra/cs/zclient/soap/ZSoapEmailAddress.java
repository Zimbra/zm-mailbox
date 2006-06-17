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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.soap;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.soap.Element;

public class ZSoapEmailAddress implements ZEmailAddress {

    private String mAddress;
    private String mContent;
    private String mDisplay;
    private String mPersonal;
    private String mType;

    static ZSoapEmailAddress getAddress(Element e, Map<String, ZSoapEmailAddress> cache) throws ServiceException {
        ZSoapEmailAddress addr;

        String id = e.getAttribute(MailService.A_ID, null);
        String ref = e.getAttribute(MailService.A_REF, null);
        if (ref != null && cache != null) {
            return cache.get(ref);
        }
        addr = new ZSoapEmailAddress();
        addr.mAddress = e.getAttribute(MailService.A_ADDRESS, null);
        addr.mContent = e.getText();
        addr.mDisplay = e.getAttribute(MailService.A_DISPLAY, null);
        addr.mPersonal = e.getAttribute(MailService.A_PERSONAL, null);
        addr.mType = e.getAttribute(MailService.A_TYPE, "");
        
        if (cache != null && id != null) {
            cache.put(id, addr);
        }
        return addr;
    }
    
    public String getAddress() {
        return mAddress;
    }

    public String getContent() {
        return mContent;
    }

    public String getDisplay() {
        return mDisplay;
    }

    public String getPersonal() {
        return mPersonal;
    }

    public String getType() {
        return mType;
    }
    
    public String toString() {
        return String.format("email: { address: %s, content: %s, display: %s, personal: %s, type: %s }", 
                mAddress, mContent, mDisplay, mPersonal, mType);
    }
}
