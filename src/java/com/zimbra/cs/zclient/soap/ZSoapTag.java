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

package com.zimbra.cs.zclient.soap;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.soap.Element;

class ZSoapTag implements ZTag, ZSoapItem {

    private Color mColor;
    private String mId;
    private String mName;
    private int mUnreadCount;
    
    ZSoapTag(Element e) throws ServiceException {
        mColor = Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, 0);
    }

    void modifyNotification(Element e) throws ServiceException {
        mColor = Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        int newColor = (int) e.getAttributeLong(MailService.A_COLOR, mColor.getValue());
        if (newColor != mColor.getValue())
            mColor = Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        mName = e.getAttribute(MailService.A_NAME, mName);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, mUnreadCount);
    }

    public Color getColor() {
        return mColor;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct("ZTag");
        sb.add("id", mId);
        sb.add("name", mName);
        sb.add("color", mColor.name());
        sb.add("unreadCount", mUnreadCount);
        sb.endStruct();
        return sb.toString();
    }

    public int compareTo(Object o) {
        if (!(o instanceof ZTag)) return 0;
        ZTag other = (ZTag) o;
        return getName().compareToIgnoreCase(other.getName());
    }

}
