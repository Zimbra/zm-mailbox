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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A simpler way of expressing (to:FOO or from:FOO or cc:FOO).
 *
 * @author tim
 * @author ysasaki
 */
public final class AddrQuery extends SubQuery {

    public enum Address {
        FROM, TO, CC
    }

    private AddrQuery(List<Query> clauses) {
        super(clauses);
    }

    public static AddrQuery create(Mailbox mbox, Analyzer analyzer,
            Set<Address> addrs, String text) throws ServiceException {
        List<Query> clauses = new ArrayList<Query>();
        boolean atFirst = true;

        if (addrs.contains(Address.FROM)) {
            clauses.add(new TextQuery(mbox, analyzer, QueryParser.FROM, text));
            atFirst = false;
        }

        if (addrs.contains(Address.TO)) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, QueryParser.TO, text));
        }

        if (addrs.contains(Address.CC)) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, QueryParser.CC, text));
        }

        return new AddrQuery(clauses);
    }
}
