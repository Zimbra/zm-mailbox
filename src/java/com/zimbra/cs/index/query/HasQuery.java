/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.cs.index.LuceneFields;

/**
 * Query by special objects.
 *
 * @author tim
 * @author ysasaki
 */
public final class HasQuery extends LuceneQuery {
    private static final Map<String, String> MAP = ImmutableMap.<String, String>builder()
        .put("attachment", "any")
        .put("att", "any")
        .put("phone", "phone")
        .put("u.po", "u.po")
        .put("ssn", "ssn")
        .put("url", "url")
        .build();

    public HasQuery(String what) {
        super("has:", LuceneFields.L_OBJECTS, lookup(MAP, what));
    }

}
