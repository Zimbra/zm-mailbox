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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.MailServiceException;

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
    private boolean mTruth = true;
    private int mModifierType;
    private int mQueryType;

    Query(int mod, int type) {
        mModifierType = mod;
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

    /**
     * Used by the QueryParser when building up the list of Query terms.
     *
     * @param mod
     */
    public final void setModifier(int mod) {
        mModifierType = mod;
    }

    @Override
    public String toString() {
        return dump(new StringBuilder()).toString();
    }

    public StringBuilder dump(StringBuilder out) {
        out.append(modToString());
        out.append("Q(");
        out.append(QueryTypeString(getQueryType()));
        return out;
    }

    /**
     * Called by the optimizer, this returns an initialized QueryOperation of the requested type.
     *
     * @param type
     * @param truth
     * @return
     */
    public abstract QueryOperation getQueryOperation(boolean truth);

    public final boolean isNegated() {
        return mModifierType == QueryParser.MINUS;
    }

    final String modToString() {
        switch(mModifierType) {
            case QueryParser.PLUS:
                return "+";
            case QueryParser.MINUS:
                return "-";
            default:
                return "";
        }
    }

    final boolean calcTruth(boolean truth) {
        if (isNegated()) {
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
    private static Map<String, Integer> sTokenImageMap;

    static {
        sTokenImageMap = new HashMap<String,Integer>();

        unquotedTokenImage = new String[QueryParser.tokenImage.length];
        for (int i = 0; i < QueryParser.tokenImage.length; i++) {
            String str = QueryParser.tokenImage[i].substring(1, QueryParser.tokenImage[i].length() - 1);
            if ("FIELD".equals(str)) {
                unquotedTokenImage[i] = "#"; // bug 22969 -- problem with proxying field queries
            } else {
                unquotedTokenImage[i] = str;
            }
            sTokenImageMap.put(str, i);
        }
    }

    public static int lookupQueryTypeFromString(String str) throws ServiceException {
        Integer toRet = sTokenImageMap.get(str);
        if (toRet == null)
            throw MailServiceException.QUERY_PARSE_ERROR(str, null, str, -1, "UNKNOWN_QUERY_TYPE");
        return toRet.intValue();
    }

}
