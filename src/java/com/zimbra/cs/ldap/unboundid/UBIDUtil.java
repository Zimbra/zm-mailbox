package com.zimbra.cs.ldap.unboundid;

import java.util.List;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.schema.Schema;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.ldap.LdapUtilCommon;

public class UBIDUtil {
    
    
    static ASN1OctetString newASN1OctetString(boolean isBinary, String value) {
        if (isBinary) {
            return new ASN1OctetString(ByteUtil.decodeLDAPBase64(value));
        } else {
            return new ASN1OctetString(value);
        }
    }
    
    static Attribute newAttribute(boolean isBinaryTransfer, String attrName, ASN1OctetString value) {
        String transferAttrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new Attribute(transferAttrName, value);
    }
    
    static Attribute newAttribute(boolean isBinaryTransfer, String attrName, ASN1OctetString[] values) {
        String transferAttrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new Attribute(transferAttrName, (Schema)null, values);
    }
}
