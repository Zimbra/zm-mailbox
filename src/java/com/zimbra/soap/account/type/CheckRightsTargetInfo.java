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

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.account.Key.TargetBy;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.TargetType;
import com.zimbra.soap.type.ZmBoolean;

public class CheckRightsTargetInfo {
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=true)
    private TargetType targetType;

    @XmlAttribute(name=AccountConstants.A_BY /* by */, required=true)
    private TargetBy targetBy;

    @XmlAttribute(name=AccountConstants.A_KEY /* key */, required=true)
    private String targetKey;
    
    @XmlAttribute(name=AccountConstants.A_ALLOW /* allow */, required=true)
    private ZmBoolean allow;
    
    @XmlElement(name=AccountConstants.E_RIGHT /* right */, required=true)
    private List<CheckRightsRightInfo> rights = Lists.newArrayList();
    
    public CheckRightsTargetInfo() {
        this(null, null, null, false, null);
    }
    
    public CheckRightsTargetInfo(TargetType targetType, TargetBy targetBy,
            String targetKey) {
        this(targetType, targetBy, targetKey, false, null);
    }
    
    public CheckRightsTargetInfo(TargetType targetType, TargetBy targetBy,
            String targetKey, boolean allow, Iterable<CheckRightsRightInfo> rights) {
        setTargetType(targetType);
        setTargetBy(targetBy);
        setTargetKey(targetKey);
        setAllow(allow);
        if (rights != null) {
            setRights(rights);
        }
    }
    
    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }
    
    public void setTargetBy(TargetBy targetBy) {
        this.targetBy = targetBy;
    }
    
    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }
    
    public void setAllow(boolean allow) {
        this.allow = ZmBoolean.fromBool(allow);
    }
    
    public void setRights(Iterable<CheckRightsRightInfo> rights) {
        this.rights = Lists.newArrayList(rights);
    }
    
    public void addRight(CheckRightsRightInfo right) {
        rights.add(right);
    }
    
    public TargetType getTargetType() {
        return targetType;
    }
    
    public TargetBy getTargetBy() {
        return targetBy;
    }
    
    public String getTargetKey() {
        return targetKey;
    }
    
    public boolean getAllow() { 
        return ZmBoolean.toBool(allow); 
    }
    
    public List<CheckRightsRightInfo> getRights() {
        return rights;
    }
}
