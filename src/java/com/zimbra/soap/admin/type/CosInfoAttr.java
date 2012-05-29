/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class CosInfoAttr extends Attr {

    /**
     * @zm-api-field-tag is-cos-attr
     * @zm-api-field-description Flags that this is a Class Of Service (COS) attribute.
     */
    @XmlAttribute(name=AdminConstants.A_C /* c */, required=false)
    private ZmBoolean cosAttr;

    /**
     * @zm-api-field-tag perm-denied
     * @zm-api-field-description Flags that the value of this attribute has been suppressed for permissions reasons
     */
    @XmlAttribute(name=AccountConstants.A_PERM_DENIED /* pd */, required=false)
    private ZmBoolean permDenied;

    public CosInfoAttr() {
        this(null, null, null);
    }

    protected CosInfoAttr(String key, String value) {
        this(key, value, null);
    }

    protected CosInfoAttr(String key, String value, Boolean isCosAttr) {
        super(key, value);
        this.cosAttr = ZmBoolean.fromBool(isCosAttr);
    }

    public static CosInfoAttr fromNameValue(String key, String value) {
        return new CosInfoAttr(key, value);
    }

    public void setCosAttr(Boolean isCosAttr) { this.cosAttr = ZmBoolean.fromBool(isCosAttr); }
    public Boolean getCosAttr() { return ZmBoolean.toBool(cosAttr); }
    public void setPermDenied(Boolean isPermDenied) { this.permDenied = ZmBoolean.fromBool(isPermDenied); }
    public Boolean getPermDenied() { return ZmBoolean.toBool(permDenied); }

    @Override
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("cosAttr", cosAttr)
            .add("permDenied", cosAttr);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
