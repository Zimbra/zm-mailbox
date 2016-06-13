/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.schema.Schema;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.ldap.LdapUtil;

public class UBIDUtil {
    
    static ASN1OctetString newASN1OctetString(boolean isBinary, String value) {
        if (isBinary) {
            return new ASN1OctetString(ByteUtil.decodeLDAPBase64(value));
        } else {
            return new ASN1OctetString(value);
        }
    }
    
    static Attribute newAttribute(boolean isBinaryTransfer, String attrName, ASN1OctetString value) {
        String transferAttrName = LdapUtil.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new Attribute(transferAttrName, value);
    }
    
    static Attribute newAttribute(boolean isBinaryTransfer, String attrName, ASN1OctetString[] values) {
        String transferAttrName = LdapUtil.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new Attribute(transferAttrName, (Schema)null, values);
    }
}
