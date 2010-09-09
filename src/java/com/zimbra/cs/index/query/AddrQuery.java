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
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A simpler way of expressing (to:FOO or from:FOO or cc:FOO).
 *
 * @author tim
 * @author ysasaki
 */
public final class AddrQuery extends SubQuery {

    // bitmask for choosing "FROM/TO/CC" of messages...used for AddrQuery and MeQuery
    public static final int ADDR_BITMASK_FROM = 0x1;
    public static final int ADDR_BITMASK_TO =   0x2;
    public static final int ADDR_BITMASK_CC =   0x4;

    private AddrQuery(int mod, List<Query> exp) {
        super(mod, exp);
    }

    public static Query createFromTarget(Mailbox mbox,
            Analyzer analyzer, int mod, int target, String text)
            throws ServiceException {
        int bitmask = 0;
        switch (target) {
            case QueryParser.TOFROM:
                bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM;
                break;
            case QueryParser.TOCC:
                bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_CC;
                break;
            case QueryParser.FROMCC:
                bitmask = ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                break;
            case QueryParser.TOFROMCC:
                bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                break;
        }
        return createFromBitmask(mbox, analyzer, mod, text, bitmask);
    }

    public static Query createFromBitmask(Mailbox mbox,
            Analyzer analyzer, int mod, String text,
            int operatorBitmask) throws ServiceException {
        ArrayList<Query> clauses = new ArrayList<Query>();
        boolean atFirst = true;

        if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
            clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.FROM, text));
            atFirst = false;
        }
        if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.TO, text));
        }
        if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
            if (atFirst) {
                atFirst = false;
            } else {
                clauses.add(new ConjQuery(ConjQuery.OR));
            }
            clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.CC, text));
        }
        return new AddrQuery(mod, clauses);
    }
}
