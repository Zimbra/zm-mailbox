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

import java.util.Map;
import java.util.HashMap;

public class ZCallFeature {
    private String mName;
    private boolean mIsSubscribed;
    private boolean mIsActive;
    private Map<String, String> mData;
    private boolean mIsVoiceMailPref;
    private String mText;

    public ZCallFeature(String name, boolean isVoiceMailPref) {
        mName = name;
        mIsSubscribed = false;
        mIsActive = false;
        mData = new HashMap<String, String>();
        mText = null;
        mIsVoiceMailPref = isVoiceMailPref;
    }

    public boolean getIsSubscribed() { return mIsSubscribed; }
    public void setIsSubscribed(boolean isSubscribed) { mIsSubscribed = isSubscribed; }

    public boolean getIsActive() { return mIsActive; }
	public void setIsActive(boolean isActive) { mIsActive = isActive; }

    Map<String, String> getData() { return mData; }
	public String getData(String key) { return mData.get(key); }
	public void setData(String key, String value) { mData.put(key, value); }
    public void clearData() { mData.clear(); }

    public void setText(String text) { mText = text; }
    public String getText() { return mText; }

	public boolean getIsVoiceMailPref() { return mIsVoiceMailPref; }

	public String getName() { return mName; }
	public void setName(String name) { mName = name; }

    public void assignFrom(ZCallFeature that) {
        this.mName = that.mName;
        this.mIsSubscribed = that.mIsSubscribed;
        this.mIsActive = that.mIsActive;
        this.mData.clear();
        this.mData.putAll(that.mData);
        this.mText = that.mText;
        this.mIsVoiceMailPref = that.mIsVoiceMailPref;
    }

}
