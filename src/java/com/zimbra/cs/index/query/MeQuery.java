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

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query messages "to me", "from me", "cc me" or any combination thereof.
 *
 * @author tim
 * @author ysasaki
 */
public final class MeQuery extends SubQuery {

    MeQuery(List<Query> exp) {
        super(exp);
    }

    public static Query create(Mailbox mbox, Analyzer analyzer,
            int operatorBitmask) throws ServiceException {
        ArrayList<Query> clauses = new ArrayList<Query>();
        Account acct = mbox.getAccount();
        boolean atFirst = true;
        if ((operatorBitmask & AddrQuery.ADDR_BITMASK_FROM) != 0) {
            clauses.add(new SentQuery(mbox, true));
            atFirst = false;
        }
        if ((operatorBitmask & AddrQuery.ADDR_BITMASK_TO) != 0) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, QueryParser.TO, acct.getName()));
        }
        if ((operatorBitmask & AddrQuery.ADDR_BITMASK_CC) != 0) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, QueryParser.CC, acct.getName()));
        }

        String[] aliases = acct.getMailAlias();
        for (String alias : aliases) {
            if ((operatorBitmask & AddrQuery.ADDR_BITMASK_TO) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, QueryParser.TO, alias));
            }
            if ((operatorBitmask & AddrQuery.ADDR_BITMASK_CC) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, QueryParser.CC, alias));
            }
        }
        return new MeQuery(clauses);
    }
}
