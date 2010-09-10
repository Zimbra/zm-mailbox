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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.QueryOperation;

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

    private static final Map<String, String> LUCENE2QUERY =
        new ImmutableMap.Builder<String, String>()
        .put(LuceneFields.L_CONTACT_DATA, "contact:")
        .put(LuceneFields.L_CONTENT, "content:")
        .put(LuceneFields.L_H_MESSAGE_ID, "msgid:")
        .put(LuceneFields.L_H_X_ENV_FROM, "envfrom:")
        .put(LuceneFields.L_H_X_ENV_TO, "envto:")
        .put(LuceneFields.L_H_FROM, "from:")
        .put(LuceneFields.L_H_TO, "to:")
        .put(LuceneFields.L_H_CC, "cc:")
        .put(LuceneFields.L_H_SUBJECT, "subject:")
        .put(LuceneFields.L_FILENAME, "filename:")
        .put(LuceneFields.L_MIMETYPE, "type:")
        .put(LuceneFields.L_ATTACHMENTS, "attachment:")
        .put(LuceneFields.L_FIELD, "#")
        .build();

    private boolean bool = true;
    private Modifier modifier = Modifier.NONE;

    public final boolean getBool() {
        return bool;
    }

    final void setBool(boolean value) {
        bool = value;
    }

    final boolean evalBool(boolean value) {
        return modifier == Modifier.MINUS ? !value : value;
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
     * @param term parsed token value
     * @return query string which can be used for proxying
     */
    final String toQueryString(String luceneField, String term) {
        String field = LUCENE2QUERY.get(luceneField);
        if (LuceneFields.L_FIELD.equals(luceneField)) {
            int delim = term.indexOf(':');
            if (delim <= 0 || delim >= term.length() - 2) {
                return field + term;
            }
            StringBuilder buf = new StringBuilder(field);
            buf.append(term.subSequence(0, delim + 1));
            buf.append('"');
            for (int i = delim + 1; i < term.length(); i++) {
                char ch = term.charAt(i);
                if (ch == '"') {
                    buf.append("\\\"");
                } else {
                    buf.append(ch);
                }
            }
            buf.append('"');
            return buf.toString();
        } else {
            return field + term;
        }
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * {@link StringBuilder} version of {@link #toString()} to efficiently
     * concatenate multiple query objects.
     * <p>
     * Sub classes may override if a special handling is required, such as
     * conjunction.
     *
     * @param out output
     * @return the same {@link StringBuilder} as the parameter
     */
    public StringBuilder toString(StringBuilder out) {
        out.append(modifier);
        out.append("Q(");
        dump(out);
        return out.append(')');
    }

    /**
     * Sub classes must implement this method to dump the internal information
     * for debugging purpose.
     *
     * @param out output
     */
    abstract void dump(StringBuilder out);

    /**
     * Convenient method to dump a list of query clauses.
     *
     * @param clauses query clauses
     * @return string representation of the query clauses
     */
    public static String toString(List<Query> clauses) {
        StringBuilder out = new StringBuilder();
        for (Query clause : clauses) {
            clause.toString(out);
        }
        return out.toString();
    }

    /**
     * Called by the optimizer, returns an initialized {@link QueryOperation}
     * of the requested type.
     */
    public abstract QueryOperation getQueryOperation(boolean truth);

}
