/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("cosAttr", cosAttr)
            .add("permDenied", cosAttr);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
