/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

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

    public enum Comparison {
        GT(">"), GE(">="), LT("<"), LE("<=");

        private final String symbol;

        private Comparison(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public static Comparison fromString(String string) {
            for (Comparison c: Comparison.values()) {
                if (string.equals(c.symbol)) {
                    return c;
                }
            }
        throw new IllegalArgumentException(string + " is not a valid comparison");
        }

        public static Comparison fromPrefix(String text) {
            if (text.startsWith(Comparison.LE.symbol)) {
                return Comparison.LE;
            } else if (text.startsWith(Comparison.LT.symbol)) {
                return Comparison.LT;
            } else if (text.startsWith(Comparison.GE.symbol)) {
                return Comparison.GE;
            } else if (text.startsWith(Comparison.GT.symbol)) {
                return Comparison.GT;
            } else {
                return null;
            }
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

    public void setModifier(Modifier mod) {
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
        } else if (luceneField.startsWith("header_")) {
            return String.format("#%s:%s", luceneField.substring("header_".length()), term);
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

    public StringBuilder toSanitizedString(StringBuilder out) {
        out.append(modifier);
        out.append("Q(");
        sanitizedDump(out);
        return out.append(')');
    }

    /**
     * Sub classes must implement this method to dump the internal information
     * for debugging purpose.
     *
     * @param out output
     */
    abstract void dump(StringBuilder out);

    void sanitizedDump(StringBuilder out) {
        dump(out);
    }

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
     * Compiles this query into a {@link QueryOperation}.
     */
    public abstract QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException;

    /**
     * Returns true if this query has at least one text query, false if it's entirely DB query.
     */
    public abstract boolean hasTextOperation();
}
