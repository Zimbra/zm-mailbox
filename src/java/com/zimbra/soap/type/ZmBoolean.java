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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.zimbra.soap.json.jackson.ZmBooleanSerializer;

/**
 * Zimbra SOAP uses the string "1" to represent true and the string "0" to represent false in XML
 * BUT Zimbra SOAP uses "true" and "false" in JSON.  "0" and "1" are acceptable in XML
 *
 * See this extract from XML Schema Part 2 at http://www.w3.org/TR/xmlschema-2/#boolean
 *  3.2.2 boolean
 *
 *  [Definition:] boolean has the value space required to support the mathematical concept of :
 *  binary-valued logic: {true, false}
 *
 *  3.2.2.1 Lexical representation
 *
 *  An instance of a datatype that is defined as boolean can have the following legal literals {true, false, 1, 0}.
 *  3.2.2.2 Canonical representation
 *
 *  The canonical representation for boolean is the set of literals {true, false}.
 *
 * This enum has 4 valid values but is meant to represent just 2 values.  It would be nice to use something like:
 *     @XmlJavaTypeAdapter(ZmBooleanAdapter.class)
 * to be 100% sure that only "0" and "1" will get output but that causes schemagen to use type "xs:string"
 * when writing types corresponding to fields of this class instead of "zmBoolean".
 *
 * JAXB class setters for fields of type ZmBoolean should go via Boolean to ensure that JAXB objects only get
 * populated with ZmBoolean.ZERO and ZmBoolean.ONE
 */

@XmlType
@XmlEnum(String.class)
@JsonSerialize(using=ZmBooleanSerializer.class)
public enum ZmBoolean {
    @XmlEnumValue("0") ZERO,
    @XmlEnumValue("1") ONE,
    @XmlEnumValue("false") FALSE,
    @XmlEnumValue("true") TRUE;

    public static ZmBoolean fromBool(Boolean val) {
        if (val == null) {
            return null;
        } else {
            return val ? ONE : ZERO;
        }
    }

    /**
     * Convert {@link ZmBoolean} to {@link Boolean} equivalent - May return null
     */
    public static Boolean toBool(ZmBoolean val) {
        return (val == null) ? null : ((val.equals(ONE)) || (val.equals(TRUE)));
    }

    /**
     * Convert {@link ZmBoolean} to boolean equivalent - If {@link val} is null returns {@link defaultval}
     */
    public static boolean toBool(ZmBoolean val, boolean defaultVal) {
        return (val == null) ? defaultVal : ((val.equals(ONE)) || (val.equals(TRUE)));
    }
}
