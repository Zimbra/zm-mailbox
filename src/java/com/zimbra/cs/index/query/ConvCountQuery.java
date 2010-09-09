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
 * Query by conversation count.
 *
 * @author tim
 * @author ysasaki
 */
public final class ConvCountQuery extends Query {
    private int mLowestCount;
    private boolean mLowerEq;
    private int mHighestCount;
    private boolean mHigherEq;

    private ConvCountQuery(int mod, int qType, int lowestCount,
            boolean lowerEq, int highestCount, boolean higherEq) {
        super(mod, qType);

        mLowestCount = lowestCount;
        mLowerEq = lowerEq;
        mHighestCount = highestCount;
        mHigherEq = higherEq;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append("ConvCount(");
        out.append(mLowerEq ? ">=" : ">");
        out.append(mLowestCount);
        out.append(' ');
        out.append(mHigherEq? "<=" : "<");
        out.append(mHighestCount);
        return out.append(')');
    }

    @Override
    public QueryOperation getQueryOperation(boolean truthiness) {
        DBQueryOperation op = new DBQueryOperation();
        truthiness = calcTruth(truthiness);
        op.addConvCountClause(mLowestCount, mLowerEq, mHighestCount, mHigherEq, truthiness);
        return op;
    }

    public static Query create(int mod, int qType, String str) {
        if (str.charAt(0) == '<') {
            boolean eq = false;
            if (str.charAt(1) == '=') {
                eq = true;
                str = str.substring(2);
            } else {
                str = str.substring(1);
            }
            int num = Integer.parseInt(str);
            return new ConvCountQuery(mod, qType, -1, false, num, eq);
        } else if (str.charAt(0) == '>') {
            boolean eq = false;
            if (str.charAt(1) == '=') {
                eq = true;
                str = str.substring(2);
            } else {
                str = str.substring(1);
            }
            int num = Integer.parseInt(str);
            return new ConvCountQuery(mod, qType, num, eq, -1, false);
        } else {
            int num = Integer.parseInt(str);
            return new ConvCountQuery(mod, qType, num, true, num, true);
        }
    }
}
