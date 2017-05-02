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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyVoiceMailItemEvent;
import org.json.JSONException;

public class ZVoiceMailItemHit implements ZSearchHit {

    private String mId;
    private String mSortField;
    private String mFlags;
    private String mSoundUrl;
    private long mDate;
    private long mDuration;
    private ZPhone mCaller;

    public ZVoiceMailItemHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        Element content = e.getOptionalElement(MailConstants.E_CONTENT);
        if (content != null) {
            mSoundUrl = content.getAttribute(MailConstants.A_URL);
        }
        mDate = e.getAttributeLong(MailConstants.A_DATE);
        mDuration = e.getAttributeLong(VoiceConstants.A_VMSG_DURATION) * 1000;
        for (Element el : e.listElements(VoiceConstants.E_CALLPARTY)) {
            String t = el.getAttribute(MailConstants.A_ADDRESS_TYPE, null);
            if (ZEmailAddress.EMAIL_TYPE_FROM.equals(t)) {
                mCaller = new ZPhone(el.getAttribute(VoiceConstants.A_PHONENUM, null), el.getAttribute(MailConstants.A_PERSONAL, null));
                break;
            }
        }
    }

    private ZVoiceMailItemHit() { }

    public static ZVoiceMailItemHit deserialize(String value, String phone) throws ServiceException {
        ZVoiceMailItemHit result = new ZVoiceMailItemHit();
        String[] array = value.split("/");
        result.mId = array[0];
        result.mSortField = array[1];
        result.mFlags = array[2];
        result.mDate = Long.parseLong(array[3]);
        result.mDuration = Long.parseLong(array[4]);
        result.mCaller = new ZPhone(array[5], array[6]);
        result.mSoundUrl = "/service/extension/velodrome/voice/~/voicemail?phone=" + phone + "&id=" + result.mId;
        return result;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getSortField() {
        return mSortField;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.HIGH_PRIORITY.getFlagChar()) != -1;
    }

    public boolean isPrivate() {
        return hasFlags() && mFlags.indexOf(VoiceConstants.FLAG_UNFORWARDABLE) != -1;
    }

    public boolean isUnheard() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.UNREAD.getFlagChar()) != -1;
    }

    public ZPhone getCaller() { return mCaller; }

    public String getDisplayCaller() { return mCaller.getDisplay(); }

    public String getSoundUrl() { return mSoundUrl; }

    public long getDate() { return mDate; }

    public long getDuration() { return mDuration; }

    @Override
    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        if (event instanceof ZModifyVoiceMailItemEvent) {
            ZModifyVoiceMailItemEvent voiceEvent = (ZModifyVoiceMailItemEvent) event;
            boolean isUnheard = !voiceEvent.getIsHeard();
            if (isUnheard != isUnheard()) {
                setFlag(ZMessage.Flag.UNREAD.getFlagChar(), isUnheard);
                voiceEvent.setMadeChange();
            }
        }
    }

    public String serialize() {
        String flags = mFlags == null ? "" : mFlags;
        return  mId + "/" +
                mSortField + "/" +
                flags + "/" +
                mDate + "/" +
                mDuration + "/" +
                mCaller.getName() + "/" +
                mCaller.getCallerId();
    }

    private void setFlag(char flagChar, boolean on) {
        if (on) {
            mFlags += flagChar;
        } else {
            mFlags = mFlags.replace(Character.toString(flagChar), "");
        }
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject ();
        zjo.put("id", mId);
        zjo.put("sortField", mSortField);
        zjo.put("flags", mFlags);
        zjo.put("soundUrl", mSoundUrl);
        zjo.put("date", mDate);
        zjo.put("duration", mDuration);
        zjo.put("caller", mCaller);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZVoiceMailItemHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
