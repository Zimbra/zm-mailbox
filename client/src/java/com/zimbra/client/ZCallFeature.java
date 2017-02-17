/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.common.soap.VoiceConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZCallFeature {
    private String mName;
    private boolean mIsSubscribed;
    private boolean mIsActive;
    private Map<String, String> mData;

    public ZCallFeature(String name) {
        mName = name;
        mIsSubscribed = false;
        mIsActive = false;
        mData = new HashMap<String, String>();
    }

    public boolean getIsSubscribed() { return mIsSubscribed; }
    public void setIsSubscribed(boolean isSubscribed) { mIsSubscribed = isSubscribed; }

    public boolean getIsActive() { return mIsActive; }
	public void setIsActive(boolean isActive) { mIsActive = isActive; }

	public String getData(String key) { return mData.get(key); }
	public void setData(String key, String value) { mData.put(key, value); }
    public void clearData() { mData.clear(); }

	public String getName() { return mName; }
	public void setName(String name) { mName = name; }

    public synchronized void assignFrom(ZCallFeature that) {
        this.mName = that.mName;
        this.mIsActive = that.mIsActive;
        this.mData.clear();
        this.mData.putAll(that.mData);
    }

    synchronized void fromElement(Element element) throws ServiceException {
       Iterator<Element.Attribute> iter = element.attributeIterator();
        while (iter.hasNext()) {
            Element.Attribute attribute = iter.next();
            String key = attribute.getKey();
            String value = attribute.getValue();
            if (VoiceConstants.A_ACTIVE.equals(key)) {
                mIsActive = Element.parseBool(key, value);
            } else if (VoiceConstants.A_SUBSCRIBED.equals(key)) {
                mIsSubscribed = Element.parseBool(key, value);
            } else {
                setData(key, value);
            }
        }
    }
    void toElement(Element element) throws ServiceException {
        element.addAttribute(VoiceConstants.A_SUBSCRIBED, true);
        element.addAttribute(VoiceConstants.A_ACTIVE, mIsActive);
        for (String key : mData.keySet()) {
            Element subEl = element.addElement(key);
            String value = mData.get(key);
            if (value != null) {
                subEl.setText(value);
            }
        }
    }
}
