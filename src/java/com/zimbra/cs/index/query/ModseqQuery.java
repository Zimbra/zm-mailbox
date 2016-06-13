/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

    @Override
    public void sanitizedDump(StringBuilder out) {
        out.append("MODSEQ:");
        out.append(operator);
        out.append("$NUM");
    }
}
