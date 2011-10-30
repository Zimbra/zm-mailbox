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
