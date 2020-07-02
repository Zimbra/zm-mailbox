/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.query;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailItem;

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

    public HasQuery(String what, Set<MailItem.Type> types) {
        super("has:", LuceneFields.L_OBJECTS, lookup(MAP, what), types);
    }

}
