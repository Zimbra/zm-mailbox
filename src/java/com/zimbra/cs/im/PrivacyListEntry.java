/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2010 Zimbra, Inc.
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
package com.zimbra.cs.im;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.SoapProtocol;

/**
 * 
 */
public class PrivacyListEntry {
    public static final byte BLOCK_MESSAGES = 0x1;
    public static final byte BLOCK_PRESENCE_OUT = 0x2;
    public static final byte BLOCK_PRESENCE_IN = 0x4;
    public static final byte BLOCK_IQ = 0x8;
    public static final byte BLOCK_ALL = 0xf;
    
    public static enum Action {
        allow, deny;
    }
    
    private IMAddr mAddr;
    private int mOrder;
    private byte mBlockTypes;
    private Action mAction;
    
    public PrivacyListEntry(IMAddr addr, int order, Action action, byte blockTypes) {
        mAddr = addr;
        mOrder = order;
        mBlockTypes = blockTypes;
        mAction = action;
    }

    public boolean isBlockMessages() {
        return (mBlockTypes & BLOCK_MESSAGES) != 0; 
    }
    public boolean isBlockPresenceOut() {
        return (mBlockTypes & BLOCK_PRESENCE_OUT) != 0; 
    }
    public boolean isBlockPresenceIn() {
        return (mBlockTypes & BLOCK_PRESENCE_IN) != 0; 
    }
    public boolean isBlockIQ() {
        return (mBlockTypes & BLOCK_IQ) != 0; 
    }
    public IMAddr getAddr() { return mAddr; }
    public int getOrder() { return mOrder; }
    public byte getTypes() { return mBlockTypes; }
    public Action getAction() { return mAction; }
    
    public String toString() { 
        try { 
            return toXml(null).toString(); 
        } catch (ServiceException ex) {
            ex.printStackTrace();
            return ex.toString(); 
        } 
    }
    
    public Element toXml(Element parent) throws ServiceException {
        Element item;
        if (parent != null)
            item = parent.addElement("item");
        else 
            item= Element.create(SoapProtocol.Soap12, "item");
        
        item.addAttribute(IMConstants.A_ADDRESS, getAddr().toString());
        item.addAttribute("action", mAction.name());
        item.addAttribute("order", getOrder());
        if (getTypes() != BLOCK_ALL) {
            if (isBlockMessages())
                item.addElement("message");
            if (isBlockPresenceOut())
                item.addElement("presence-out");
            if (isBlockPresenceIn())
                item.addElement("presence-in");
            if (isBlockIQ())
                item.addElement("iq");
        }
        return item;
    }
}
