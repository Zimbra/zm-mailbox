/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

import java.util.HashMap;
import java.util.Set;

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
        this.set(VoiceConstants.A_vmPrefEmailNotifAddress, address);
    }

    public boolean getPlayDateAndTimeInMsgEnv() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayDateAndTimeInMsgEnv);
    }
    
    public void setPlayDateAndTimeInMsgEnv(boolean value) {
        this.set(VoiceConstants.A_vmPrefPlayDateAndTimeInMsgEnv, value);
    }
    
    public boolean getAutoPlayNewMsgs() {
        return this.getBoolean(VoiceConstants.A_vmPrefAutoPlayNewMsgs);
    }
    
    public void setAutoPlayNewMsgs(boolean value) {
        this.set(VoiceConstants.A_vmPrefAutoPlayNewMsgs, value);
    }
    
    public String getPromptLevel() {
        return this.get(VoiceConstants.A_vmPrefPromptLevel);
    }
    
    public void setPromptLevel(String level) {
        if (level != null && (level.equals("RAPID") || level.equals("STANDARD") || level.equals("EXTENDED"))) {
            this.set(VoiceConstants.A_vmPrefPromptLevel, level);
        }
    }
    
    public boolean getPlayCallerNameInMsgEnv() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayCallerNameInMsgEnv);
    }
    
    public void setPlayCallerNameInMsgEnv(boolean value) {
        this.set(VoiceConstants.A_vmPrefPlayCallerNameInMsgEnv, value);
    }
    
    public boolean getSkipPinEntry() {
        return this.getBoolean(VoiceConstants.A_vmPrefSkipPinEntry);
    }
    
    public void setSkipPinEntry(boolean value) {
        this.set(VoiceConstants.A_vmPrefSkipPinEntry, value);
    }
    
    public String getUserLocale() {
        return this.get(VoiceConstants.A_vmPrefUserLocale);
    }
    
    public void setUserLocale(String locale) {
        this.set(VoiceConstants.A_vmPrefUserLocale, locale);
    }
    
    public String getAnsweringLocale() {
        return this.get(VoiceConstants.A_vmPrefAnsweringLocale);
    }
    
    public void setAnsweringLocale(String locale) {
        this.set(VoiceConstants.A_vmPrefAnsweringLocale, locale);
    }

    public String getGreetingType() {
        return this.get(VoiceConstants.A_vmPrefGreetingType);
    }

    public void setGreetingType(String type) {
        this.set(VoiceConstants.A_vmPrefGreetingType, type);
    }

    public boolean getEmailNotifStatus() {
        return this.getBoolean(VoiceConstants.A_vmPrefEmailNotifStatus);
    }
    
    public void setEmailNotifStatus(boolean value) {
        this.set(VoiceConstants.A_vmPrefEmailNotifStatus, value);
    }

    public boolean getPlayTutorial() {
        return this.getBoolean(VoiceConstants.A_vmPrefPlayTutorial);
    }
    
    public void setPlayTutorial(boolean value) {
        this.set(VoiceConstants.A_vmPrefPlayTutorial, value);
    }

    public int getVoiceItemsPerPage() {
        return this.getInt(VoiceConstants.A_zimbraPrefVoiceItemsPerPage);
    }
    
    public void setVoiceItemsPerPage(int value) {
        this.set(VoiceConstants.A_zimbraPrefVoiceItemsPerPage, value);
    }

    public boolean getEmailNotifTrans() {
        return this.getBoolean(VoiceConstants.A_vmPrefEmailNotifTrans);
    }
    
    public void setEmailNotifTrans(boolean value) {
        this.set(VoiceConstants.A_vmPrefEmailNotifTrans, value);
    }

    public boolean getEmailNotifAttach() {
        return this.getBoolean(VoiceConstants.A_vmPrefEmailNotifAttach);
    }
    
    public void setEmailNotifAttach(boolean value) {
        this.set(VoiceConstants.A_vmPrefEmailNotifAttach, value);
    }

    public Set<String> keySet() {
        return mMap.keySet();
    }
    public String get(String key) {
        return mMap.get(key);
    }
    public void set(String key, String value) {
        mMap.put(key, value);
    }
    public void set(String key, boolean value) {
        this.set(key, value ? "true" : "false");
    }
    public void set(String key, int value) {
        this.set(key, Integer.toString(value));
    }


    public long getLong(String name) {
        String v = this.get(name);
        try {
            return v == null ? -1 : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public int getInt(String name) {
        String v = this.get(name);
        try {
            return v == null ? -1 : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public boolean getBoolean(String name) {
        String v = this.get(name);
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
                this.set(name, value);
            }
        }
    }

    synchronized void fromElement(Element element) throws ServiceException {
        super.fromElement(element);
        for (Element prefElement : element.listElements(VoiceConstants.E_PREF)) {
            String name = prefElement.getAttribute(MailConstants.A_NAME);
            String value = prefElement.getText();
            this.set(name, value);
        }
    }

    void toElement(Element element) throws ServiceException {
        super.toElement(element);
        for (String name : this.keySet()) {
            String value = this.get(name);
            Element prefElement = element.addElement(VoiceConstants.E_PREF);
            prefElement.addAttribute(MailConstants.A_NAME, name);
            prefElement.setText(value);
        }
    }
}
