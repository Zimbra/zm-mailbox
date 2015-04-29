/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.pushnotifications;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.ZmgDevice;

public class NewMessagePushNotification implements PushNotification {

    private int conversationId = 0;
    private int messageId = 0;
    private String subject = "";
    private String sender = "";
    private String recipientAddress = "";
    private String fragment = "";
    private int unreadCount = 0;
    private ZmgDevice device = null;

    /**
     * @param conversationId
     * @param messageId
     * @param message
     * @param sender
     * @param recipientAddress
     * @param device
     * @param fragment
     */
    public NewMessagePushNotification(int conversationId, int messageId, String message,
                                      String sender, String recipientAddress, ZmgDevice device,
                                      String fragment, int unreadCount) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.subject = message;
        this.sender = sender;
        this.recipientAddress = recipientAddress;
        this.fragment = fragment;
        this.device = device;
        this.unreadCount = unreadCount;
    }

    /**
     * @return the conversationId
     */
    public int getConversationId() {
        return conversationId;
    }

    /**
     * @param conversationId
     *            the conversationId to set
     */
    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * @return the messageId
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * @param messageId
     *            the messageId to set
     */
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    /**
     * @return the message
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject
     *            the message to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @param sender
     *            the sender to set
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * @return the recipientAddress
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * @param recipientAddress
     *            the recipientAddress to set
     */
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    /**
     * @return the fragment
     */
    public String getFragment() {
        return fragment;
    }

    /**
     * @param fragment
     *            the fragment to set
     */
    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    
    /**
     * @return the unreadCount
     */
    public int getUnreadCount() {
        return unreadCount;
    }

    
    /**
     * @param unreadCount the unreadCount to set
     */
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    private Map<String, String> getPayloadForApns() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(APNS_SOUND, "default");
        map.put(APNS_BADGE, String.valueOf(unreadCount));
        map.put(APNS_ALERT, "From: " + sender + "\n" + subject);
        map.put(APNS_CID, String.valueOf(conversationId));
        map.put(APNS_MSG_ID, String.valueOf(messageId));
        map.put(APNS_MSG_ID, fragment);
        map.put(APNS_RECIPIENT_ADDRESS, recipientAddress);
        return map;
    }

    private Map<String, String> getPayloadForGcm() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(GCM_CID, String.valueOf(conversationId));
        map.put(GCM_MSG_ID, String.valueOf(messageId));
        map.put(GCM_SUBJECT, subject);
        map.put(GCM_SENDER, sender);
        map.put(GCM_FRAGMENT, fragment);
        map.put(GCM_RECIPIENT_ADDRESS, recipientAddress);
        map.put(GCM_UNREAD_COUNT, String.valueOf(unreadCount));
        return map;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getPayload()
     */
    @Override
    public Map<String, String> getPayload() {
        switch (device.getPushProvider()) {
        case PROVIDER_IDENTIFIER_GCM:
            return getPayloadForGcm();

        case PROVIDER_IDENTIFIER_APNS:
            return getPayloadForApns();

        default:
            return Collections.<String, String> emptyMap();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getDevice()
     */
    @Override
    public ZmgDevice getDevice() {
        return device;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.zimbra.cs.pushnotifications.PushNotification#setDevice(com.zimbra
     * .cs.account.ZmgDevice)
     */
    @Override
    public void setDevice(ZmgDevice device) {
        this.device = device;
    }
}
