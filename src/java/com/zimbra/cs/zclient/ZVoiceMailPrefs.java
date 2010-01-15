/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.cs.account.Provisioning;

import java.util.HashMap;

public class ZVoiceMailPrefs extends ZCallFeature {
    private HashMap<String, String> mMap;

	public ZVoiceMailPrefs(String name) {
        super(name);
        mMap = new HashMap<String, String>();
    }

    public String getEmailNotificationAddress() {
        String address = this.get(VoiceConstants.A_vmPrefEmailNotifAddress);
        return address == null ? "" : address;
    }

    public void setEmailNotificationAddress(String address) {
        mMap.put(VoiceConstants.A_vmPrefEmailNotifAddress, address);
    }

    public boolean getPlayDateAndTimeInMsgEnv() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayDateAndTimeInMsgEnv);
    }
    
    public void setPlayDateAndTimeInMsgEnv(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefPlayDateAndTimeInMsgEnv, value ? "true":"false");
    }
    
    public boolean getAutoPlayNewMsgs() {
        return this.getBoolean(VoiceConstants.A_vmPrefAutoPlayNewMsgs);
    }
    
    public void setAutoPlayNewMsgs(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefAutoPlayNewMsgs, value ? "true":"false");
    }
    
    public String getPromptLevel() {
        return this.get(VoiceConstants.A_vmPrefPromptLevel);
    }
    
    public void setPromptLevel(String level) {
        if (level != null && (level.equals("RAPID") || level.equals("STANDARD") || level.equals("EXTENDED")))
            mMap.put(VoiceConstants.A_vmPrefPromptLevel, level);
    }
    
    public boolean getPlayCallerNameInMsgEnv() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayCallerNameInMsgEnv);
    }
    
    public void setPlayCallerNameInMsgEnv(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefPlayCallerNameInMsgEnv, value ? "true":"false");
    }
    
    public boolean getSkipPinEntry() {
        return this.getBoolean(VoiceConstants.A_vmPrefSkipPinEntry);
    }
    
    public void setSkipPinEntry(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefSkipPinEntry, value ? "true":"false");
    }
    
    public String getUserLocale() {
        return this.get(VoiceConstants.A_vmPrefUserLocale);
    }
    
    public void setUserLocale(String locale) {
        mMap.put(VoiceConstants.A_vmPrefUserLocale, locale);
    }
    
    public String getAnsweringLocale() {
        return this.get(VoiceConstants.A_vmPrefAnsweringLocale);
    }
    
    public void setAnsweringLocale(String locale) {
        mMap.put(VoiceConstants.A_vmPrefAnsweringLocale, locale);
    }

    public String getGreetingType() {
        return this.get(VoiceConstants.A_vmPrefGreetingType);
    }

    public void setGreetingType(String type) {
        mMap.put(VoiceConstants.A_vmPrefGreetingType, type);
    }

    public boolean getEmailNotifStatus() {
        return this.getBoolean(VoiceConstants.A_vmPrefEmailNotifStatus);
    }
    
    public void setEmailNotifStatus(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefEmailNotifStatus, value ? "true":"false");
    }

    public boolean getPlayTutorial() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayTutorial);
    }
    
    public void setPlayTutorial(boolean value) {
        mMap.put(VoiceConstants.A_vmPrefPlayTutorial, value ? "true":"false");
    }

    public int getVoiceItemsPerPage() {
        return this.getInt(VoiceConstants.A_zimbraPrefVoiceItemsPerPage);
    }
    
    public void setVoiceItemsPerPage(int value) {
        mMap.put(VoiceConstants.A_zimbraPrefVoiceItemsPerPage, Integer.toString(value));
    }
    

    public String get(String key) {
        return mMap.get(key);
    }

    public long getLong(String name) {
	String v = get(name);
	try {
		return v == null ? -1 : Long.parseLong(v);
	} catch (NumberFormatException e) {
		return -1;
	}
    }

    public int getInt(String name) {
        String v = get(name);
        try {
            return v == null ? -1 : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public boolean getBoolean(String name) {
	String v = get(name);
	try {
		return (v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || Integer.parseInt(v) != 0));
	} catch (NumberFormatException e) {
		return false;
	}
    }

    public synchronized void assignFrom(ZCallFeature that) {
        super.assignFrom(that);
        if (that instanceof ZVoiceMailPrefs) {
            HashMap<String, String> thatMap = ((ZVoiceMailPrefs) that).mMap;
            for (String name : thatMap.keySet()) {
                String value = thatMap.get(name);
                mMap.put(name, value);
            }
        }
    }

    synchronized void fromElement(Element element) throws ServiceException {
        super.fromElement(element);
        for (Element prefElement : element.listElements(VoiceConstants.E_PREF)) {
            String name = prefElement.getAttribute(MailConstants.A_NAME);
            String value = prefElement.getText();
            mMap.put(name, value);
        }
    }

    void toElement(Element element) throws ServiceException {
        super.toElement(element);
        for (String name : mMap.keySet()) {
            String value = mMap.get(name);
            Element prefElement = element.addElement(VoiceConstants.E_PREF);
            prefElement.addAttribute(MailConstants.A_NAME, name);
            prefElement.setText(value);
        }
    }
}
