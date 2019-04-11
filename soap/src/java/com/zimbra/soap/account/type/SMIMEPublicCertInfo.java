/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SMIMEStoreType;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_SMIME_PUBLIC_CERT_INFO, description="SMIMEPublicCertInfo")
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

    @GraphQLQuery(name=GqlConstants.STORE_TYPE, description="store type")
    public SMIMEStoreType getStoreType() { return storeType; }
    @GraphQLQuery(name=GqlConstants.FIELD, description="field containing the certificate")
    public String getField() { return field; }
    @GraphQLQuery(name=GqlConstants.VALUE, description="Base64 encoded cert")
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("storeType", storeType)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
