/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.ArrayList;

public class ZCallFeatures {

    public static final String SELECTIVE_CALL_FORWARD_LIST_FULL = "voice.SELECTIVE_CALL_FORWARD_LIST_FULL";
    public static final String SELECTIVE_CALL_REJECT_LIST_FULL = "voice.SELECTIVE_CALL_REJECT_LIST_FULL";
    public static final String SELECTIVE_CALL_FORWARD_ALREADY_IN_LIST = "voice.SELECTIVE_CALL_FORWARD_ALREADY_IN_LIST";
    public static final String SELECTIVE_CALL_REJECT_ALREADY_IN_LIST = "voice.SELECTIVE_CALL_REJECT_ALREADY_IN_LIST";

    private ZMailbox mMbox;
    private ZPhone mPhone;
    private Map<String, ZCallFeature> mCallFeatures;
    private boolean mCallFeaturesLoaded;

    public ZCallFeatures(ZMailbox mbox, ZPhone phone) {
        mMbox = mbox;
        mPhone = phone;
        mCallFeatures = new HashMap<String, ZCallFeature>();
        mCallFeaturesLoaded = false;
    }

    public ZCallFeatures(ZMailbox mbox, ZPhone phone, Element e) throws ServiceException {
        this(mbox, phone);

        this.addFeature(VoiceConstants.E_ANON_CALL_REJECTION);
        this.addFeature(VoiceConstants.E_CALL_FORWARD);
        this.addFeature(VoiceConstants.E_SELECTIVE_CALL_FORWARD);
        this.addFeature(VoiceConstants.E_VOICE_MAIL_PREFS);
	
	this.addFeature(VoiceConstants.E_CALL_FORWARD_BUSY_LINE);
	this.addFeature(VoiceConstants.E_CALL_FORWARD_NO_ANSWER);
	this.addFeature(VoiceConstants.E_CALL_WAITING);
	
	this.addFeature(VoiceConstants.E_SELECTIVE_CALL_REJECTION);

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

    public void loadCallFeatures() throws ServiceException {
        if (!mCallFeaturesLoaded) {
            mCallFeaturesLoaded = true;
            mMbox.loadCallFeatures(this);
	    
	    this.getSubscribedFeatures();
        }
    }

    public synchronized ZCallFeature getFeature(String name) {
        ZCallFeature result = mCallFeatures.get(name);
        if (result == null) {
            result = addFeature(name);
        }
        return result;
    }

    public ZSelectiveCallForwarding getSelectiveCallForwarding() {
        return (ZSelectiveCallForwarding) getFeature(VoiceConstants.E_SELECTIVE_CALL_FORWARD);
    }
    
    public ZSelectiveCallRejection getSelectiveCallRejection() {
        return (ZSelectiveCallRejection) getFeature(VoiceConstants.E_SELECTIVE_CALL_REJECTION);
    }

    public ZVoiceMailPrefs getVoiceMailPrefs() {
        return (ZVoiceMailPrefs) getFeature(VoiceConstants.E_VOICE_MAIL_PREFS);
    }
    
    public synchronized ZCallFeature addFeature(String name) {
        ZCallFeature result;
        if (VoiceConstants.E_SELECTIVE_CALL_FORWARD.equals(name)) {
            result = new ZSelectiveCallForwarding(name);
        } else if (VoiceConstants.E_SELECTIVE_CALL_REJECTION.equals(name)) {
            result = new ZSelectiveCallRejection(name);
        } else if (VoiceConstants.E_VOICE_MAIL_PREFS.equals(name)) {
            result = new ZVoiceMailPrefs(name);
        } else {
            result = new ZCallFeature(name);
        }
        mCallFeatures.put(name, result);
        return result;
    }

    public Collection<ZCallFeature> getAllFeatures() {
        return mCallFeatures.values();
    }

    public boolean isEmpty() {
        return mCallFeatures.isEmpty();
    }

    public List<ZCallFeature> getSubscribedFeatures() {
        Collection<ZCallFeature> allFeatures = mCallFeatures.values();
        List<ZCallFeature> result = new ArrayList<ZCallFeature>();
        for (ZCallFeature feature : allFeatures) {
            if (feature.getIsSubscribed()) {
                result.add(feature);
            }
        }
        return result;
    }
}
