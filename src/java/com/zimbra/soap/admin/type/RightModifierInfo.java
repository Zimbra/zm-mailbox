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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class RightModifierInfo {

    /**
     * @zm-api-field-tag deny-flag
     * @zm-api-field-description Deny flag - default is <b>0 (false)</b>
     */
    @XmlAttribute(name=AdminConstants.A_DENY /* deny */, required=false)
    private final ZmBoolean deny;

    /**
     * @zm-api-field-tag can-delegate-flag
     * @zm-api-field-description Flag whether can delegate - default is <b>0 (false)</b>
     */
    @XmlAttribute(name=AdminConstants.A_CAN_DELEGATE /* canDelegate */, required=false)
    private final ZmBoolean canDelegate;

    /**
     * @zm-api-field-tag disinheritSubGroups-flag
     * @zm-api-field-description disinheritSubGroups flag - default is <b>0 (false)</b>
     */
    @XmlAttribute(name=AdminConstants.A_DISINHERIT_SUB_GROUPS /* disinheritSubGroups */, required=false)
    private final ZmBoolean disinheritSubGroups;
    
    /**
     * @zm-api-field-tag subdomain-flag
     * @zm-api-field-description subDomain flag - default is <b>0 (false)</b>
     */
    @XmlAttribute(name=AdminConstants.A_SUB_DOMAIN /* subDomain */, required=false)
    private final ZmBoolean subDomain;

    /**
     * @zm-api-field-tag right
     * @zm-api-field-description Value is of the form : {right-name} | {inline-right} where
     * <br />
     * {right-name} = a system defined right name
     * <br />
     * {inline-right} = {op}.{target-type}.{attr-name}
     * <br />
     * {op} = set | get
     * <br />
     * {attr-name} = a valid attribute name on the specified target type
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightModifierInfo() {
        this((Boolean) null, (Boolean) null, (Boolean) null, (Boolean) null, (String) null);
    }

    public RightModifierInfo(Boolean deny, Boolean canDelegate,
                Boolean disInheritSubGroups, Boolean subDomain, String value) {
        this.deny = ZmBoolean.fromBool(deny);
        this.canDelegate = ZmBoolean.fromBool(canDelegate);
        this.disinheritSubGroups = ZmBoolean.fromBool(disInheritSubGroups);
        this.subDomain = ZmBoolean.fromBool(subDomain);
        this.value = value;
    }

    public Boolean getDeny() { return ZmBoolean.toBool(deny); }
    public Boolean getCanDelegate() { return ZmBoolean.toBool(canDelegate); }
    public Boolean getDisinheritSubGroups() { return ZmBoolean.toBool(disinheritSubGroups); }
    public Boolean getSubDomain() { return ZmBoolean.toBool(subDomain); }
    public String getValue() { return value; }
}
