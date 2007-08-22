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
package com.zimbra.cs.im;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

/**
 * 
 */
public class IMPrivacyListNotification extends IMNotification {
    
    PrivacyList mList;
    
    IMPrivacyListNotification(PrivacyList list) { mList = list; }

    /* @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.common.soap.Element) */
    @Override
    public Element toXml(Element parent) throws ServiceException {
        Element e = this.create(parent, "privacy");
        return mList.toXml(e);
    }
    
    public String toString() { 
        try { 
            return toXml(null).toString(); 
        } catch (ServiceException ex) {
            ex.printStackTrace();
            return ex.toString(); 
        } 
    }
    
}
