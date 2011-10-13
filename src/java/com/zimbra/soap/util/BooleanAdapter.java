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
package com.zimbra.soap.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.google.common.base.Strings;

/**
 * {@link XmlAdapter} that maps:
 * <ul>
 *  <li>boolean (JAXB) to 0|1 (XML)
 *  <li>0|1|true|false (XML) to boolean (JAXB)
 * </ul>
 *
 * @author ysasaki
 */
public final class BooleanAdapter extends XmlAdapter<String, Boolean> {

    @Override
    public String marshal(Boolean value) {
        return value == null ? null : value ? "1" : "0";
    }

    @Override
    public Boolean unmarshal(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        } else if ("0".equals(0) || "false".equalsIgnoreCase(value)){
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(value + " is not a boolean (0|1|true|false)");
    }

}
