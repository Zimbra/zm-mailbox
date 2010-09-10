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

import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.query.parser.QueryParser;

/**
 * Abstract base class for queries.
 * <p>
 * Very simple wrapper classes that each represent a node in the parse tree for
 * the query string.
 *
 * @author tim
 * @author ysasaki
 */
public abstract class Query {

    public enum Modifier {
        NONE(""), PLUS("+"), MINUS("-");

        private final String symbol;

        private Modifier(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private boolean mTruth = true;
    private Modifier modifier = Modifier.NONE;
    private int mQueryType;

    Query(int type) {
        mQueryType = type;
    }

    public final boolean getBool() {
        return mTruth;
    }

    final void setBool(boolean value) {
        mTruth = value;
    }

    final void setQueryType(int queryType) {
        mQueryType = queryType;
    }

    final int getQueryType() {
        return mQueryType;
    }

    public final Modifier getModifier() {
        return modifier;
    }

    public final void setModifier(Modifier mod) {
        modifier = mod;
    }

    /**
     * Reconstructs the query string.
     *
     * @param src parsed token value
     * @return query string which can be used for proxying
     */
    final String toQueryString(String src) {
        String img = unquotedTokenImage[mQueryType];
        if (img.equals("#")) {
            int delim = src.indexOf(':');
            if (delim <= 0 || delim >= src.length() - 2) {
                return img + src;
            }
            StringBuilder buf = new StringBuilder(img);
            buf.append(src.subSequence(0, delim + 1));
            buf.append('"');
            for (int i = delim + 1; i < src.length(); i++) {
                char ch = src.charAt(i);
                if (ch == '"') {
                    buf.append("\\\"");
                } else {
                    buf.append(ch);
                }
            }
            buf.append('"');
            return buf.toString();
        } else {
            return img + src;
        }
    }

    @Override
    public String toString() {
        return dump(new StringBuilder()).toString();
    }

    public StringBuilder dump(StringBuilder out) {
        out.append(modifier);
        out.append("Q(");
        out.append(QueryTypeString(getQueryType()));
        return out;
    }

    /**
     * Called by the optimizer, returns an initialized {@link QueryOperation}
     * of the requested type.
     */
    public abstract QueryOperation getQueryOperation(boolean truth);

    final boolean calcTruth(boolean truth) {
        if (modifier == Modifier.MINUS) {
            return !truth;
        } else {
            return truth;
        }
    }

    final String QueryTypeString(int qType) {
        switch (qType) {
            case QueryParser.CONTACT: return LuceneFields.L_CONTACT_DATA;
            case QueryParser.CONTENT: return LuceneFields.L_CONTENT;
            case QueryParser.MSGID: return LuceneFields.L_H_MESSAGE_ID;
            case QueryParser.ENVFROM: return LuceneFields.L_H_X_ENV_FROM;
            case QueryParser.ENVTO: return LuceneFields.L_H_X_ENV_TO;
            case QueryParser.FROM: return LuceneFields.L_H_FROM;
            case QueryParser.TO: return LuceneFields.L_H_TO;
            case QueryParser.CC: return LuceneFields.L_H_CC;
            case QueryParser.SUBJECT: return LuceneFields.L_H_SUBJECT;
            case QueryParser.IN: return "IN";
            case QueryParser.HAS: return "HAS";
            case QueryParser.FILENAME: return LuceneFields.L_FILENAME;
            case QueryParser.TYPE: return LuceneFields.L_MIMETYPE;
            case QueryParser.ATTACHMENT: return LuceneFields.L_ATTACHMENTS;
            case QueryParser.IS: return "IS";
            case QueryParser.DATE: return "DATE";
            case QueryParser.AFTER: return "AFTER";
            case QueryParser.BEFORE: return "BEFORE";
            case QueryParser.APPT_START: return "APPT-START";
            case QueryParser.APPT_END: return "APPT-END";
            case QueryParser.SIZE: return "SIZE";
            case QueryParser.BIGGER: return "BIGGER";
            case QueryParser.SMALLER: return "SMALLER";
            case QueryParser.TAG: return "TAG";
            case QueryParser.MY: return "MY";
            case QueryParser.MESSAGE: return "MESSAGE";
            case QueryParser.CONV: return "CONV";
            case QueryParser.CONV_COUNT: return "CONV-COUNT";
            case QueryParser.CONV_MINM: return "CONV_MINM";
            case QueryParser.CONV_MAXM: return "CONV_MAXM";
            case QueryParser.CONV_START: return "CONV-START";
            case QueryParser.CONV_END: return "CONV-END";
            case QueryParser.AUTHOR: return "AUTHOR";
            case QueryParser.TITLE: return "TITLE";
            case QueryParser.KEYWORDS: return "KEYWORDS";
            case QueryParser.COMPANY: return "COMPANY";
            case QueryParser.METADATA: return "METADATA";
            case QueryParser.ITEM: return "ITEMID";
            case QueryParser.FIELD: return LuceneFields.L_FIELD;
        }
        return "UNKNOWN:(" + qType + ")";
    }

    private static String[] unquotedTokenImage;
    static {
        unquotedTokenImage = new String[QueryParser.tokenImage.length];
        for (int i = 0; i < QueryParser.tokenImage.length; i++) {
            String str = QueryParser.tokenImage[i].substring(1, QueryParser.tokenImage[i].length() - 1);
            if ("FIELD".equals(str)) {
                unquotedTokenImage[i] = "#"; // bug 22969 -- problem with proxying field queries
            } else {
                unquotedTokenImage[i] = str;
            }
        }
    }

}
