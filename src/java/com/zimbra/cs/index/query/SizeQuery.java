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

import java.text.ParseException;

import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;

/**
 * Query by size.
 *
 * @author tim
 * @author ysasaki
 */
public final class SizeQuery extends Query {
    public enum Type {
        EQ("="), GT(">"), LT("<");

        private final String symbol;

        private Type(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private final Type type;
    private String mSizeStr;
    private long mSize;

    public SizeQuery(Type type, String size) throws ParseException {
        mSizeStr = size;

        char ch = mSizeStr.charAt(0);
        if (ch == '>') {
            this.type = Type.GT;
            mSizeStr = mSizeStr.substring(1);
        } else if (ch == '<') {
            this.type = Type.LT;
            mSizeStr = mSizeStr.substring(1);
        } else {
            this.type = type;
        }

        boolean hasEQ = false;
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
            throw new ParseException(size, 0);
        }

        if (hasEQ) {
            switch (this.type) {
                case GT:
                    mSize--; // correct since range constraint is strict >
                    break;
                case LT:
                    mSize++; // correct since range constraint is strict <
                    break;
            }
        }

        mSizeStr = String.valueOf(mSize);
    }

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        long highest = -1;
        long lowest = -1;

        switch (type) {
            case GT:
                highest = -1;
                lowest = mSize;
                break;
            case LT:
                highest = mSize;
                lowest = -1;
                break;
            case EQ:
                highest = mSize + 1;
                lowest = mSize - 1;
                break;
            default:
                assert(false);
        }
        op.addSizeClause(lowest, highest, evalBool(bool));
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SIZE");
        out.append(type);
        out.append(mSize);
    }
}
