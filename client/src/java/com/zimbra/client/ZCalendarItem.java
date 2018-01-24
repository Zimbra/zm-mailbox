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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyMessageEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZCalendarItem implements ZItem, ToZJSONObject {

    public enum Flag {
        flagged('f'),
        attachment('a');

        private char mFlagChar;

        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Flag(char flagChar) {
            mFlagChar = flagChar;

        }
    }

    private String mId;
    private String mFlags;
    private String mTags;
    private String mFolderId;
    private long mDate;
    private long mSize;
    private String mUID;
    private List<ZInvite> mInvites;

    public ZCalendarItem(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        mUID = e.getAttribute(MailConstants.A_UID, null);
        mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mSize = e.getAttributeLong(MailConstants.A_SIZE);
        mInvites = new ArrayList<ZInvite>();
        for (Element inviteEl : e.listElements(MailConstants.E_INVITE)) {
            mInvites.add(new ZInvite(inviteEl));
        }
    }

    public void modifyNotification(ZModifyMessageEvent mevent) throws ServiceException {
        if (mevent.getId().equals(mId)) {
            mFlags = mevent.getFlags(mFlags);
            mTags = mevent.getTagIds(mTags);
            mFolderId = mevent.getFolderId(mFolderId);
        }
    }

    /**
     *
     * @return UID of item
     */
    public String getUid() {
        return mUID;
    }

    public long getSize() {
        return mSize;
    }

    public List<ZInvite> getInvites() {
        return mInvites;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getUuid() {
        return null;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean hasTags() {
        return mTags != null && mTags.length() > 0;
    }

   @Override
public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("flags", mFlags);
        zjo.put("tags", mTags);
        zjo.put("date", mDate);
        zjo.put("folderId", mFolderId);
        zjo.put("size", mSize);
        zjo.put("uid", mUID);
        zjo.put("invites", mInvites);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZCalendarItem %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public String getFlags() {
        return mFlags;
    }

    public String getTagIds() {
        return mTags;
    }

    public String getFolderId() {
        return mFolderId;
    }

    public long getDate() {
        return mDate;
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.ATTACHED.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FLAGGED.getFlagChar()) != -1;
    }

}
