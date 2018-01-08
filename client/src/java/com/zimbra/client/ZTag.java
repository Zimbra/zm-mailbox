/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyTagEvent;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZTag extends ZTagInfo implements Comparable<ZTag>, ZimbraTag {

    private Color mColor;

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
        orange(9),
        rgbColor;

        private long mValue;
        private static Map<String,Color> colorMap = new HashMap<String,Color>();

        static {
          for (Color c : Color.values())
                colorMap.put(c.toString(), c);
        }

        public long getValue() { return mValue; }

        public static Color fromString(String s) throws ServiceException {
            if (s != null) {
                s = s.toLowerCase();
                if (colorMap.containsKey(s)) {
                    return colorMap.get(s);
                }
            }
            return Color.values()[com.zimbra.common.mailbox.Color.getMappedColor(s)];
        }

        public Color setRgbColor(String s) {
            mValue = new com.zimbra.common.mailbox.Color(s).getValue();
            return this;
        }

        public String getRgbColor() {
            return new com.zimbra.common.mailbox.Color(mValue).toString();
        }

        Color(long value) { mValue = value; }
        Color() {}
    }

    public ZTag(Element e, ZMailbox mailbox) throws ServiceException {
        super(e, mailbox);
        String rgb = e.getAttribute(MailConstants.A_RGB, null);
        // Server reports color or rgb attribute on mail items but not both.
        // If rgb, map the color to the rgb value. If the attr is color, return the value as is.
        if (rgb != null) {
            mColor =  Color.rgbColor.setRgbColor(rgb);
        } else {
            String s = e.getAttribute(MailConstants.A_COLOR, "0");
            mColor = Color.values()[(byte)Long.parseLong(s)];
        }
    }

    public void modifyNotification(ZModifyTagEvent tevent) throws ServiceException {
	    mColor = tevent.getColor(mColor);
	    name = tevent.getName(name);
	    unreadCount = tevent.getUnreadCount(unreadCount);
	    retentionPolicy = tevent.getRetentionPolicy(retentionPolicy);
    }

    public Color getColor() {
        return mColor;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", id);
        zjo.put("name", name);
        zjo.put("color", mColor.name());
        zjo.put("unreadCount", unreadCount);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZTag %s]", name);
    }

    @Override
    public int compareTo(ZTag other) {
        return getName().compareToIgnoreCase(other.getName());
    }

    public void delete() throws ServiceException { mailbox.deleteTag(id); }

    public void deleteItem() throws ServiceException { delete(); }

    public void markRead() throws ServiceException { mailbox.markTagRead(id); }

    public void modifyColor(ZTag.Color color) throws ServiceException { mailbox.modifyTagColor(id, color); }

    public void rename(String newName) throws ServiceException { mailbox.renameTag(id, newName); }

    @Override
    public int getTagId() {
        return Integer.valueOf(getId());
    }

    @Override
    public String getTagName() {
        return getName();
    }
}
