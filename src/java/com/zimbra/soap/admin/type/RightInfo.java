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

@XmlAccessorType(XmlAccessType.NONE)
public class RightInfo {
    public enum RightType {
        preset, getAttrs, setAttrs, combo;

        public static RightType fromString(String s)
        throws ServiceException {
            try {
                return RightType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR("unknown right type: " + s, e);
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
                throw ServiceException.INVALID_REQUEST("unknown right category: " + s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag right-name
     * @zm-api-field-description Right name
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag right-type-getAttrs|setAttrs|combo|preset
     * @zm-api-field-description Right type.  Valid values : getAttrs | setAttrs | combo | preset
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private RightType type;

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type.
     * <table>
     * <tr> <td> <b>type=preset</b> </td> <td> Always present (exactly target type)</td> </tr>
     * <tr> <td> <b>type=getAttrs/setAttrs</b> </td> <td> Always present (comma separated target types)</td> </tr>
     * <tr> <td> <b>type=combo</b> </td> <td> Always NOT present </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_TARGET_TYPE /* targetType */, required=false)
    private String targetType;

    /**
     * @zm-api-field-tag right-class
     * @zm-api-field-description Right class
     * <table>
     * <tr> <td> <b>ADMIN</b> </td> <td> Admin right </td> </tr>
     * <tr> <td> <b>USER</b> </td> <td> User right </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_RIGHT_CLASS /* rightClass */, required=true)
    private RightClass rightClass;

    /**
     * @zm-api-field-tag right-description
     * @zm-api-field-description Right description
     */
    @XmlElement(name=AdminConstants.E_DESC /* desc */, required=true)
    private String desc;

    /**
     * @zm-api-field-description Attrs
     */
    @XmlElement(name=AdminConstants.E_ATTRS /* attrs */, required=false)
    private RightsAttrs attrs;

    /**
     * @zm-api-field-description Rights
     */
    @XmlElement(name=AdminConstants.E_RIGHTS /* rights */, required=false)
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
