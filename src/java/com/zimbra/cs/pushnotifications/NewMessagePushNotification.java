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

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ZmgDevice;

public class NewMessagePushNotification implements PushNotification {

    private int conversationId = 0;
    private int messageId = 0;
    private String subject = "";
    private String sender = "";
    private String fragment = "";
    private String recipient = "";
    private String senderDisplayName = "";
    private int unreadCount = 0;
    private ZmgDevice device = null;
    private String type;
    private String action;

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
                                      String sender, String senderDisplayName, String recipient,
                                      ZmgDevice device, String fragment, int unreadCount,
                                      String type, String op) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.subject = message;
        this.sender = sender;
        this.senderDisplayName = senderDisplayName;
        this.recipient = recipient;
        this.fragment = fragment;
        this.device = device;
        this.unreadCount = unreadCount;
        this.type = type;
        this.action = op;
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
     * @return the senderDisplayName
     */
    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    
    /**
     * @param senderDisplayName the senderDisplayName to set
     */
    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    /**
     * @return the recipient
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * @param recipient the recipient to set
     */
    public void setRecipient(String recipient) {
        this.recipient = recipient;
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
     * @param unreadCount
     *            the unreadCount to set
     */
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    private String getPayloadForApns() {
        JSONObject aps = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            aps.put(APNS_SOUND, "default");
            aps.put(APNS_BADGE, unreadCount);
            aps.put(APNS_ALERT, "From: " + sender + "\n" + subject);

            payload.put(APNS_APS, aps);
            payload.put(CID, conversationId);
            payload.put(ID, messageId);
            payload.put(SENDER_ADDRESS, sender);
            payload.put(SENDER_DISPLAY_NAME, senderDisplayName);
            payload.put(RECIPIENT_ADDRESS, recipient);
            payload.put(FRAGMENT, fragment);
            payload.put(TYPE, type);
            payload.put(ACTION, action);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating APNS payload for new message notification", e);
            return "";
        }

        return payload.toString();
    }

    private String getPayloadForGcm() {
        JSONObject gcmData = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            gcmData.put(CID, conversationId);
            gcmData.put(ID, messageId);
            gcmData.put(SUBJECT, subject);
            gcmData.put(SENDER_ADDRESS, sender);
            gcmData.put(SENDER_DISPLAY_NAME, senderDisplayName);
            gcmData.put(RECIPIENT_ADDRESS, recipient);
            gcmData.put(FRAGMENT, fragment);
            gcmData.put(UNREAD_COUNT, unreadCount);
            gcmData.put(TYPE, type);
            gcmData.put(ACTION, action);

            Collection<String> registrationIds = new ArrayList<String>();
            registrationIds.add(device.getRegistrationId());
            payload.put(GCM_REGISTRATION_IDS, registrationIds);
            payload.put(GCM_DATA, gcmData);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating GCM payload for new message notification", e);
            return "";
        }

        return payload.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getPayload()
     */
    @Override
    public String getPayload() {
        switch (device.getPushProvider()) {
        case PROVIDER_IDENTIFIER_GCM:
            return getPayloadForGcm();

        case PROVIDER_IDENTIFIER_APNS:
            return getPayloadForApns();

        default:
            return "";
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
