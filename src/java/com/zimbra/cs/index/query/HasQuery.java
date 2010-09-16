/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.index.LuceneFields;

/**
 * Query by special objects.
 *
 * @author tim
 * @author ysasaki
 */
public final class HasQuery extends LuceneQuery {
    private static final Map<String, String> mMap = new HashMap<String, String>();

    static {
        addMapping(mMap, new String[] {"attachment", "att"}, "any");
        addMapping(mMap, new String[] {"phone"}, "phone");
        addMapping(mMap, new String[] {"u.po"}, "u.po");
        addMapping(mMap, new String[] {"ssn"}, "ssn");
        addMapping(mMap, new String[] {"url"}, "url");
    }

    public HasQuery(String what) {
        super("has:", LuceneFields.L_OBJECTS, lookup(mMap, what));
    }
}
