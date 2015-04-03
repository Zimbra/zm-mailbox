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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.ZmgDevice;

public class NewMessagePushNotification implements PushNotification {

    private String conversationId = "";
    private String messageId = "";
    private String subject = "";
    private String sender = "";
    private String recipientAddress = "";
    private String fragment = "";
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
    public NewMessagePushNotification(String conversationId, String messageId, String message,
                                      String sender, String recipientAddress, ZmgDevice device,
                                      String fragment) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.subject = message;
        this.sender = sender;
        this.recipientAddress = recipientAddress;
        this.fragment = fragment;
        this.device = device;
    }

    /**
     * @return the conversationId
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * @param conversationId
     *            the conversationId to set
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * @return the messageId
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * @param messageId
     *            the messageId to set
     */
    public void setMessageId(String messageId) {
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

    /* (non-Javadoc)
     * @see com.zimbra.cs.pushnotifications.PushNotification#getPayloadForGcm()
     */
    public Map<String, String> getPayloadForGcm() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("data.cid", conversationId);
        map.put("data.msgId", messageId);
        map.put("data.subject", subject);
        map.put("data.sender", sender);
        map.put("data.fragment", fragment);
        map.put("data.recipientAddress", recipientAddress);
        return map;
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
