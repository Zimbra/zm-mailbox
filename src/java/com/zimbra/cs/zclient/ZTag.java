/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyTagEvent;
import com.zimbra.common.soap.Element;

import java.util.Arrays;

public class ZTag implements Comparable, ZItem {

    private Color mColor;
    private String mId;
    private String mName;
    private int mUnreadCount;
        
    public enum Color {
        
        defaultColor(0),
        blue(1),
        cyan(2), 
        green(3),
        purple(4),
        red(5),
        yellow(6),
        pink(7),
        gray(8),
        orange(9);
        
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
        mColor = Color.fromString(e.getAttribute(MailConstants.A_COLOR, "0"));
        mId = e.getAttribute(MailConstants.A_ID);
        mName = e.getAttribute(MailConstants.A_NAME);
        mUnreadCount = (int) e.getAttributeLong(MailConstants.A_UNREAD, 0);
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
    	if (event instanceof ZModifyTagEvent) {
    		ZModifyTagEvent tevent = (ZModifyTagEvent) event;
    		mColor = tevent.getColor(mColor);
    		mName = tevent.getName(mName);
    		mUnreadCount = tevent.getUnreadCount(mUnreadCount);
    	}
    }

    public String getId() {
        return mId;
    }

    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
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
