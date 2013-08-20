/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.cs.index.LuceneFields;

/**
 * Query by MIME type.
 *
 * @author tim
 * @author ysasaki
 */
public final class TypeQuery extends LuceneQuery {

    private TypeQuery(String what) {
        super("type:", LuceneFields.L_MIMETYPE, what);
    }

    /**
     * Note: returns either a {@link TypeQuery} or a {@link SubQuery}
     * @return
     */
    public static Query createQuery(String what) {
        Collection<String> types = lookup(AttachmentQuery.CONTENT_TYPES_MULTIMAP, what);
        if (types.size() == 1) {
            return new TypeQuery(Lists.newArrayList(types).get(0));
        } else {
            List<Query> clauses = new ArrayList<Query>(types.size() * 2 - 1);
            for (String contentType : types) {
                if (!clauses.isEmpty()) {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TypeQuery(contentType));
            }
            return new SubQuery(clauses);
        }
    }


}
