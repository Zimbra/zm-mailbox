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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.service.ServiceException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;

public class ZCallFeatures {
    private ZMailbox mMbox;
    private ZPhone mPhone;
    private Map<String, ZCallFeature> mCallFeatures;
    private boolean mCallFeaturesDefined;

    public ZCallFeatures(ZMailbox mbox, ZPhone phone) {
        mMbox = mbox;
        mPhone = phone;
        mCallFeatures = new HashMap<String, ZCallFeature>();
        mCallFeaturesDefined = false;
    }

    public ZCallFeatures(ZMailbox mbox, ZPhone phone, Element e) throws ServiceException {
        this(mbox, phone);
        String[] names = getCallFeatureNames();
        for (String name : names) {
            mCallFeatures.put(name, new ZCallFeature(name, false));
        }
        names = getVoiceMailFeatureNames();
        for (String name : names) {
            mCallFeatures.put(name, new ZCallFeature(name, true));
        }
        List<Element> elements = e.listElements(VoiceConstants.E_CALL_FEATURE);
        for (Element element : elements) {
            String name = element.getAttribute(MailConstants.A_NAME);
            ZCallFeature feature = mCallFeatures.get(name);
            if (feature != null) {
                feature.setIsSubscribed(true);
            }
        }
    }

    public ZPhone getPhone() { return mPhone; }

    public synchronized Map<String, ZCallFeature> getCallFeatures(ZMailbox mbox) throws ServiceException {
        if (!mCallFeaturesDefined) {
            mCallFeaturesDefined = true;
            mbox.loadCallFeatures(this);
        }
        return mCallFeatures;
    }

    public ZCallFeature getCallFeature(String name) throws ServiceException {
        return getCallFeatures(mMbox).get(name);
    }

    public ZCallFeature findCallFeature(String name) {
        return mCallFeatures.get(name);
    }

    public ZCallFeature addCallFeature(String name, boolean isVoiceMailPref) {
        ZCallFeature result = new ZCallFeature(name, isVoiceMailPref);
        mCallFeatures.put(name, result);
        return result;
    }

    public void removeCallFeature(String name) {
        mCallFeatures.remove(name);
    }

    public Collection<ZCallFeature> getFeatureList() {
        return mCallFeatures.values();
    }

    public boolean isEmpty() {
        return mCallFeatures.isEmpty();
    }

    public String[] getCallFeatureNames() {
        return new String[] {
            VoiceConstants.E_ANON_CALL_REJECTION, VoiceConstants.E_CALL_FORWARD,
            VoiceConstants.E_SELECTIVE_CALL_FORWARD, VoiceConstants.E_VOICE_MAIL_PREFS
        };
    }

    public String[] getVoiceMailFeatureNames() {
        return new String[] { VoiceConstants.A_vmPrefEmailNotifAddress };
    }
}
