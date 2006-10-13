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

import java.util.Arrays;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZTag implements Comparable, ZItem {

    private Color mColor;
    private String mId;
    private String mName;
    private int mUnreadCount;
        
    public enum Color {
        
        orange(0),
        blue(1),
        cyan(2), 
        green(3),
        purple(4),
        red(5),
        yellow(6);
        
        private int mValue;

        public int getValue() { return mValue; }

        public static Color fromString(String s) throws ServiceException {
            try {
                return Color.values()[Integer.parseInt(s)];
            } catch (NumberFormatException e) {
            } catch (IndexOutOfBoundsException e) {
            }
            
            try {
                return Color.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid color: "+s+", valid values: "+Arrays.asList(Color.values()), e);
            }
        }

        Color(int value) { mValue = value; } 
    }

    public ZTag(Element e) throws ServiceException {
        mColor = Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, 0);
    }

    public void modifyNotification(Element e) throws ServiceException {
        int newColor = (int) e.getAttributeLong(MailService.A_COLOR, mColor.getValue());
        if (newColor != mColor.getValue())
            mColor = Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        mName = e.getAttribute(MailService.A_NAME, mName);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, mUnreadCount);
    }
    
    public String getId() {
        return mId;
    }

    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
     * @see #getPath() 
     * 
     */
    public String getName() {
        return mName;
    }

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount() {
        return mUnreadCount;
    }

    public Color getColor() {
        return mColor;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
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
