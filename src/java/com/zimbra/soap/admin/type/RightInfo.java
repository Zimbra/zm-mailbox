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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class RightInfo {
    public enum RightType {
        preset, getAttrs, setAttrs, combo;
        
        public static RightType fromString(String s)
        throws ServiceException {
            try {
                return RightType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                        "unknown right type: " + s, e);
            }
        }
    }

    public enum RightClass {
        ALL, ADMIN, USER;
    
        public static RightClass fromString(String s)
        throws ServiceException {
            try {
                return RightClass.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "unknown right category: " + s, e);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private String name;
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private RightType type;
    @XmlAttribute(name=AdminConstants.A_TARGET_TYPE, required=false)
    private String targetType;
    @XmlAttribute(name=AdminConstants.A_RIGHT_CLASS, required=true)
    private RightClass rightClass;
    @XmlElement(name=AdminConstants.E_DESC, required=true)
    private String desc;

    @XmlElement(name=AdminConstants.E_ATTRS, required=false)
    private RightsAttrs attrs;
    @XmlElement(name=AdminConstants.E_RIGHTS, required=false)
    private ComboRights rights;

    public RightInfo() {
    }

    public void setName(String name) { this.name = name; }

    public String getName() { return name; }

    public void setType(RightType type) { this.type = type; }

    public RightType getType() { return type; }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetType() { return targetType; }

    public void setRightClass(RightClass rightClass) {
        this.rightClass = rightClass;
    }

    public RightClass getRightClass() { return rightClass; }

    public void setDesc(String desc) { this.desc = desc; }

    public String getDesc() { return desc; }

    public void setAttrs(RightsAttrs attrs) { this.attrs = attrs; }

    public RightsAttrs getAttrs() { return attrs; }

    public void setRights(ComboRights rights) { this.rights = rights; }

    public ComboRights getRights() { return rights; }
}
