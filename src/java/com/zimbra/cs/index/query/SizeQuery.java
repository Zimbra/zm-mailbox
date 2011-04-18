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

import java.text.ParseException;

import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

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
    private long size;

    public SizeQuery(Type type, String text) throws ParseException {

        char ch = text.charAt(0);
        if (ch == '>') {
            this.type = Type.GT;
            text = text.substring(1);
        } else if (ch == '<') {
            this.type = Type.LT;
            text = text.substring(1);
        } else {
            this.type = type;
        }

        boolean hasEQ = false;
        ch = text.charAt(0);
        if (ch == '=') {
            text = text.substring(1);
            hasEQ = true;
        }

        char typeChar = '\0';

        typeChar = Character.toLowerCase(text.charAt(text.length() - 1));
        // strip "b" off end (optimize me)
        if (typeChar == 'b') {
            text = text.substring(0, text.length() - 1);
            typeChar = Character.toLowerCase(text.charAt(text.length() - 1));
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
            text = text.substring(0, text.length() - 1);
        }

        try {
            size = Integer.parseInt(text.trim()) * multiplier;
        } catch (NumberFormatException e) {
            throw new ParseException(text, 0);
        }

        if (hasEQ) {
            switch (this.type) {
                case GT:
                    size--; // correct since range constraint is strict >
                    break;
                case LT:
                    size++; // correct since range constraint is strict <
                    break;
            }
        }
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        switch (type) {
            case GT:
                op.addSizeRange(size, false, -1, false, evalBool(bool));
                break;
            case LT:
                op.addSizeRange(-1, false, size, false, evalBool(bool));
                break;
            case EQ:
                op.addSizeRange(size, true, size, true, evalBool(bool));
                break;
            default:
                assert false : type;
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SIZE:");
        out.append(type);
        out.append(size);
    }
}
