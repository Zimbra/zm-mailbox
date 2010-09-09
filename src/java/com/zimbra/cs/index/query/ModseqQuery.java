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

import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;

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

    private int mValue;
    private Operator mOp;

    public ModseqQuery(int mod, int target, String changeId) {
        super(mod, target);

        if (changeId.charAt(0) == '<') {
            if (changeId.charAt(1) == '=') {
                mOp = Operator.LTEQ;
                changeId = changeId.substring(2);
            } else {
                mOp = Operator.LT;
                changeId = changeId.substring(1);
            }
        } else if (changeId.charAt(0) == '>') {
            if (changeId.charAt(1) == '=') {
                mOp = Operator.GTEQ;
                changeId = changeId.substring(2);
            } else {
                mOp = Operator.GT;
                changeId = changeId.substring(1);
            }
        } else {
            mOp = Operator.EQ;
        }
        mValue = Integer.parseInt(changeId);
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        DBQueryOperation op = new DBQueryOperation();
        truth = calcTruth(truth);

        long highest = -1, lowest = -1;
        boolean lowestEq = false;
        boolean highestEq = false;

        switch (mOp) {
            case EQ:
                highest = mValue;
                lowest = mValue;
                highestEq = true;
                lowestEq = true;
                break;
            case GT:
                lowest = mValue;
                break;
            case GTEQ:
                lowest = mValue;
                lowestEq = true;
                break;
            case LT:
                highest = mValue;
                break;
            case LTEQ:
                highest = mValue;
                highestEq = true;
                break;
        }

        op.addModSeqClause(lowest, lowestEq, highest, highestEq, truth);
        return op;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append(',');
        out.append(mOp);
        out.append(' ');
        out.append(mValue);
        return out.append(')');
    }
}
