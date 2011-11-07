/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;

public class DiscoverRightsEmail {
    @XmlAttribute(name=AccountConstants.A_ADDR, required=true)
    private String addr;
    
    public DiscoverRightsEmail() {
        this(null);
    }
    
    public DiscoverRightsEmail(String addr) {
        setAddr(addr);
    }
    
    public void setAddr(String addr) {
        this.addr = addr;
    }
    
    public String getAddr() {
        return addr;
    }
}
