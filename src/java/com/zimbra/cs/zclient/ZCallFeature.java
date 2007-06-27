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
        this.mIsSubscribed = that.mIsSubscribed;
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
