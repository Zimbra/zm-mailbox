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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SMIMEStoreType;

@XmlAccessorType(XmlAccessType.NONE)
public class SMIMEPublicCertInfo {

    /**
     * @zm-api-field-tag cert-store-type
     * @zm-api-field-description Certificate store type
     * <br />
     * Valid store types:
     * <table>
     * <tr> <td> <b>CONTACT</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>GAL</b> </td> <td> GAL (internal and external) </td> </tr>
     * <tr> <td> <b>LDAP</b> </td> <td> external LDAP (see GetSMIMEConfig and ModifySMIMEConfig </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_STORE /* store */, required=true)
    private SMIMEStoreType storeType;

    /**
     * @zm-api-field-tag cert-store-field
     * @zm-api-field-description Field containing the certificate
     */
    @XmlAttribute(name=AccountConstants.A_FIELD /* field */, required=true)
    private String field;

    /**
     * @zm-api-field-tag base64-encoded-cert
     * @zm-api-field-description Base64 encoded cert
     */
    @XmlValue
    private String value;

    private SMIMEPublicCertInfo() {
    }

    private SMIMEPublicCertInfo(SMIMEStoreType storeType, String field, String value) {
        setStoreType(storeType);
        setField(field);
        setValue(value);
    }

    public static SMIMEPublicCertInfo createForStoreTypeFieldAndValue(
                SMIMEStoreType storeType, String field, String value) {
        return new SMIMEPublicCertInfo(storeType, field, value);
    }

    public void setStoreType(SMIMEStoreType storeType) { this.storeType = storeType; }
    public void setField(String field) { this.field = field; }
    public void setValue(String value) { this.value = value; }

    public SMIMEStoreType getStoreType() { return storeType; }
    public String getField() { return field; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("storeType", storeType)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
