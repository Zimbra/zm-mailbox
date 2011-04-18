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

import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by conversation count.
 *
 * @author tim
 * @author ysasaki
 */
public final class ConvCountQuery extends Query {
    private int lowestCount;
    private boolean lowerEq;
    private int highestCount;
    private boolean higherEq;

    private ConvCountQuery(int lowestCount, boolean lowerEq, int highestCount, boolean higherEq) {
        this.lowestCount = lowestCount;
        this.lowerEq = lowerEq;
        this.highestCount = highestCount;
        this.higherEq = higherEq;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("ConvCount:");
        out.append(lowerEq ? ">=" : ">");
        out.append(lowestCount);
        out.append(' ');
        out.append(higherEq? "<=" : "<");
        out.append(highestCount);
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        op.addConvCountRange(lowestCount, lowerEq, highestCount, higherEq, evalBool(bool));
        return op;
    }

    public static Query create(String term) {
        if (term.charAt(0) == '<') {
            boolean eq = false;
            if (term.charAt(1) == '=') {
                eq = true;
                term = term.substring(2);
            } else {
                term = term.substring(1);
            }
            int num = Integer.parseInt(term);
            return new ConvCountQuery(-1, false, num, eq);
        } else if (term.charAt(0) == '>') {
            boolean eq = false;
            if (term.charAt(1) == '=') {
                eq = true;
                term = term.substring(2);
            } else {
                term = term.substring(1);
            }
            int num = Integer.parseInt(term);
            return new ConvCountQuery(num, eq, -1, false);
        } else {
            int num = Integer.parseInt(term);
            return new ConvCountQuery(num, true, num, true);
        }
    }
}
