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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Query by MIME type.
 *
 * @author tim
 * @author ysasaki
 */
public final class TypeQuery extends LuceneQuery {

    private TypeQuery(String what, Set<MailItem.Type> types) {
        super("type:", LuceneFields.L_MIMETYPE, what, types);
    }

    public static Query createQuery(String what) {
        return createQuery(what, EnumSet.noneOf(MailItem.Type.class));
    }

    /**
     * Note: returns either a {@link TypeQuery} or a {@link SubQuery}
     * @return
     */
    public static Query createQuery(String what, Set<MailItem.Type> itemTypes) {
        Collection<String> types = lookup(AttachmentQuery.CONTENT_TYPES_MULTIMAP, what);
        if (types.size() == 1) {
            return new TypeQuery(Lists.newArrayList(types).get(0), itemTypes);
        } else {
            List<Query> clauses = new ArrayList<Query>(types.size() * 2 - 1);
            for (String contentType : types) {
                if (!clauses.isEmpty()) {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TypeQuery(contentType, itemTypes));
            }
            return new SubQuery(clauses);
        }
    }


}
