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

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.document.IntPoint;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Structured field query.
 *
 * @see LuceneFields#L_FIELD
 * @author ysasaki
 */
public final class FieldQuery extends TextQuery {

    private static final Pattern NUMERIC_QUERY_REGEX = Pattern.compile("(<|>|<=|>=)?-?\\d+");

    private FieldQuery(String name, String value, boolean isPhraseQuery, Set<MailItem.Type> types) {
        super(LuceneFields.L_FIELD, String.format("%s:%s", name, value == null ? "" : value), isPhraseQuery, types);
    }

    public static Query create(Mailbox mbox, String name, String value) throws ServiceException {
        return create(mbox, name, value, EnumSet.noneOf(MailItem.Type.class));
    }

    public static Query create(Mailbox mbox, String name, String value, Set<MailItem.Type> types) throws ServiceException {
        return create(mbox, name, value, false, types);
    }

    /**
     * Factory to create a {@link FieldQuery}.
     * <p>
     * If it's a numeric range query, {@link NumericRangeFieldQuery} is created instead.
     *
     * @param mbox mailbox
     * @param name structured field name
     * @param value field value
     * @param isPhraseQuery whether the query text represents a phrase query
     * @return new {@link FieldQuery} or {@link NumericRangeFieldQuery}
     * @throws ServiceException error on wildcard expansion
     */
    public static Query create(Mailbox mbox, String name, String value, boolean isPhraseQuery, Set<MailItem.Type> types) throws ServiceException {
        if (NUMERIC_QUERY_REGEX.matcher(value).matches()) {
            try {
                return new NumericRangeFieldQuery(name, value, types);
            } catch (NumberFormatException e) { // fall back to text query
            }
        }
        return new FieldQuery(name, value, isPhraseQuery, types);
    }

    private static final class NumericRangeFieldQuery extends Query {
        private enum Range {
            EQ(""), GT(">"), GT_EQ(">="), LT("<"), LT_EQ("<=");

            private final String sign;

            private Range(String sign) {
                this.sign = sign;
            }

            @Override
            public String toString() {
                return sign;
            }
        }

        private final String name;
        private final int number;
        private final Range range;
        private final Set<MailItem.Type> types;

        NumericRangeFieldQuery(String name, String value, Set<MailItem.Type> types) throws NumberFormatException {
            this.name = name.toLowerCase();
            if (value.startsWith("<")) {
                if (value.startsWith("=", 1)) {
                    range = Range.LT_EQ;
                    number = Integer.parseInt(value.substring(2));
                } else {
                    range = Range.LT;
                    number = Integer.parseInt(value.substring(1));
                }
            } else if (value.startsWith(">")) {
                if (value.startsWith("=", 1)) {
                    range = Range.GT_EQ;
                    number = Integer.parseInt(value.substring(2));
                } else {
                    range = Range.GT;
                    number = Integer.parseInt(value.substring(1));
                }
            } else {
                range = Range.EQ;
                number = Integer.parseInt(value);
            }
            this.types = types;
        }

        @Override
        public boolean hasTextOperation() {
            return true;
        }

        @Override
        public QueryOperation compile(Mailbox mbox, boolean bool) {
            String fieldName = SolrUtils.getNumericHeaderFieldName(name);
            org.apache.lucene.search.Query query = null;
            switch (range) {
                case EQ:
                    query = IntPoint.newExactQuery(fieldName, number);
                    break;
                case GT:
                    query = IntPoint.newRangeQuery(fieldName, number+1, Integer.MAX_VALUE);
                    break;
                case GT_EQ:
                    query = IntPoint.newRangeQuery(fieldName, number, Integer.MAX_VALUE);
                    break;
                case LT:
                    query = IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, number-1);
                    break;
                case LT_EQ:
                    query = IntPoint.newRangeQuery(fieldName, Integer.MIN_VALUE, number);
                    break;
                default:
                    assert false;
            }

            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(fieldName, range.toString() + number), query, evalBool(bool), getIndexTypes(types));
            return op;
        }

        @Override
        void dump(StringBuilder out) {
            out.append('#');
            out.append(name);
            out.append("#:");
            out.append(range);
            out.append(number);
        }

        @Override
        void sanitizedDump(StringBuilder out) {
            out.append('#');
            out.append(name);
            out.append("#:");
            out.append(range);
            out.append("$NUM");
        }
    }

}
