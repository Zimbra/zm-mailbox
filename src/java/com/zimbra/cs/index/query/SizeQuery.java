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
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.index.query.parser.QueryParserException;

/**
 * Query by size.
 *
 * @author tim
 * @author ysasaki
 */
public final class SizeQuery extends Query {
    private String mSizeStr;
    private long mSize;

    public SizeQuery(int target, String size) throws QueryParserException {
        super(target);

        boolean hasEQ = false;

        mSizeStr = size;

        char ch = mSizeStr.charAt(0);
        if (ch == '>') {
            setQueryType(QueryParser.BIGGER);
            mSizeStr = mSizeStr.substring(1);
        } else if (ch == '<') {
            setQueryType(QueryParser.SMALLER);
            mSizeStr = mSizeStr.substring(1);
        }

        ch = mSizeStr.charAt(0);
        if (ch == '=') {
            mSizeStr = mSizeStr.substring(1);
            hasEQ = true;
        }

        char typeChar = '\0';

        typeChar = Character.toLowerCase(mSizeStr.charAt(mSizeStr.length() - 1));
        // strip "b" off end (optimize me)
        if (typeChar == 'b') {
            mSizeStr = mSizeStr.substring(0, mSizeStr.length() - 1);
            typeChar = Character.toLowerCase(mSizeStr.charAt(mSizeStr.length() - 1));
        }

        // size:100b size:1kb size:1mb bigger:10kb smaller:3gb
        //
        // n+{b,kb,mb}    // default is b
        int multiplier = 1;
        switch (typeChar) {
            case 'k':
                multiplier = 1024;
                break;
            case 'm':
                multiplier = 1024 * 1024;
                break;
        }

        if (multiplier > 1) {
            mSizeStr = mSizeStr.substring(0, mSizeStr.length() - 1);
        }

        try {
            mSize = Integer.parseInt(mSizeStr.trim()) * multiplier;
        } catch (NumberFormatException e) {
            throw new QueryParserException("PARSER_ERROR");
        }

        if (hasEQ) {
            if (getQueryType() == QueryParser.BIGGER) {
                mSize--; // correct since range constraint is strict >
            } else if (getQueryType() == QueryParser.SMALLER) {
                mSize++; // correct since range constraint is strict <
            }
        }

        mSizeStr = ZimbraAnalyzer.SizeTokenFilter.encodeSize(mSize);
        if (mSizeStr == null) {
            mSizeStr = "";
        }
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        DBQueryOperation op = new DBQueryOperation();

        truth = calcTruth(truth);

        long highest = -1, lowest = -1;

        switch (getQueryType()) {
            case QueryParser.BIGGER:
                highest = -1;
                lowest = mSize;
                break;
            case QueryParser.SMALLER:
                highest = mSize;
                lowest = -1;
                break;
            case QueryParser.SIZE:
                highest = mSize+1;
                lowest = mSize-1;
                break;
            default:
                assert(false);
        }
        op.addSizeClause(lowest, highest, truth);
        return op;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append(',');
        out.append(mSize);
        return out.append(')');
    }
}
