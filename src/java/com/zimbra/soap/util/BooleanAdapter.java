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

/**
 * {@link XmlAdapter} that maps boolean (JAXB) to 0|1 (XML).
 *
 * @author ysasaki
 */
public final class BooleanAdapter extends XmlAdapter<Integer, Boolean> {

    @Override
    public Integer marshal(Boolean value) {
        return value == null ? null : value ? 1 : 0;
    }

    @Override
    public Boolean unmarshal(Integer value) {
        return value == null ? null : value == 1;
    }

}
