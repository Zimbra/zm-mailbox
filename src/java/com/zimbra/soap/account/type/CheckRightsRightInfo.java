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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

public class CheckRightsRightInfo {
    @XmlAttribute(name=AccountConstants.A_ALLOW /* allow */, required=true)
    private ZmBoolean allow;
    
    @XmlValue
    String right;
    
    public CheckRightsRightInfo() {
        this(null, false);
    }
    
    public CheckRightsRightInfo(String right, boolean allow) {
        setRight(right);
        setAllow(allow);
    }
    
    private void setRight(String right) {
        this.right = right;
    }
    
    private void setAllow(boolean allow) {
        this.allow = ZmBoolean.fromBool(allow);
    }
    
    private String getRight() {
        return right;
    }
    
    private boolean getAllow() {
        return ZmBoolean.toBool(allow); 
    }
}
