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
 * Query by modseq number.
 *
 * @author tim
 * @author ysasaki
 */
public final class ModseqQuery extends Query {
    static enum Operator {
        EQ, GT, GTEQ, LT, LTEQ;
    }

    private final int modseq;
    private final Operator operator;

    public ModseqQuery(String changeId) {
        if (changeId.charAt(0) == '<') {
            if (changeId.charAt(1) == '=') {
                operator = Operator.LTEQ;
                changeId = changeId.substring(2);
            } else {
                operator = Operator.LT;
                changeId = changeId.substring(1);
            }
        } else if (changeId.charAt(0) == '>') {
            if (changeId.charAt(1) == '=') {
                operator = Operator.GTEQ;
                changeId = changeId.substring(2);
            } else {
                operator = Operator.GT;
                changeId = changeId.substring(1);
            }
        } else {
            operator = Operator.EQ;
        }
        modseq = Integer.parseInt(changeId);
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        long highest = -1;
        long lowest = -1;
        boolean lowestEq = false;
        boolean highestEq = false;

        switch (operator) {
            case EQ:
                highest = modseq;
                lowest = modseq;
                highestEq = true;
                lowestEq = true;
                break;
            case GT:
                lowest = modseq;
                break;
            case GTEQ:
                lowest = modseq;
                lowestEq = true;
                break;
            case LT:
                highest = modseq;
                break;
            case LTEQ:
                highest = modseq;
                highestEq = true;
                break;
        }

        op.addModSeqRange(lowest, lowestEq, highest, highestEq, evalBool(bool));
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("MODSEQ:");
        out.append(operator);
        out.append(modseq);
    }
}
