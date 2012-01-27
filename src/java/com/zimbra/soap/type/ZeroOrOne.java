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

@XmlType
@XmlEnum(Integer.class)
public enum ZeroOrOne {
    @XmlEnumValue("0") ZERO,
    @XmlEnumValue("1") ONE;

    public static ZeroOrOne fromBool(boolean val) {
        if (val) {
            return ONE;
        } else {
            return ZERO;
        }
    }
    public static boolean toBool(ZeroOrOne val) {
        if (val.equals(ONE)) {
            return true;
        } else {
            return false;
        }
    }
}
