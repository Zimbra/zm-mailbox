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

import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.LegacyNumericUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Structured field query.
 *
 * @see LuceneFields#L_FIELD
 * @author ysasaki
 */
public final class FieldQuery extends TextQuery {

    private static final Pattern NUMERIC_QUERY_REGEX = Pattern.compile("(<|>|<=|>=)?-?\\d+");

    private FieldQuery(String name, String value) {
        super(LuceneFields.L_FIELD, String.format("%s:%s", name, value == null ? "" : value));
    }

    /**
     * Factory to create a {@link FieldQuery}.
     * <p>
     * If it's a numeric range query, {@link NumericRangeFieldQuery} is created instead.
     *
     * @param mbox mailbox
     * @param name structured field name
     * @param value field value
     * @return new {@link FieldQuery} or {@link NumericRangeFieldQuery}
     * @throws ServiceException error on wildcard expansion
     */
    public static Query create(Mailbox mbox, String name, String value) throws ServiceException {
        if (NUMERIC_QUERY_REGEX.matcher(value).matches()) {
            try {
                return new NumericRangeFieldQuery(name, value);
            } catch (NumberFormatException e) { // fall back to text query
            }
        }
        return new FieldQuery(name, value);
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

        NumericRangeFieldQuery(String name, String value) throws NumberFormatException {
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
        }

        @Override
        public boolean hasTextOperation() {
            return true;
        }

        @SuppressWarnings("deprecation")
        @Override
        public QueryOperation compile(Mailbox mbox, boolean bool) {
            //TODO: convert to use PointValues
            org.apache.lucene.search.Query query = null;
        	BytesRefBuilder encodedNumBytes = new BytesRefBuilder();
        	BytesRefBuilder encodedMaxBytes;
        	BytesRefBuilder encodedMinBytes;
        	LegacyNumericUtils.intToPrefixCoded(number, 0, encodedNumBytes);

            switch (range) {
                case EQ:
                    query = new TermQuery(new Term(LuceneFields.L_FIELD,
                            name + "#:" + encodedNumBytes.get().utf8ToString()));
                    break;
                case GT:
                	encodedMaxBytes = new BytesRefBuilder();
                	LegacyNumericUtils.intToPrefixCoded(Integer.MAX_VALUE, 0, encodedMaxBytes);
                    query = new TermRangeQuery(LuceneFields.L_FIELD,
                            new BytesRef(name + "#:" + encodedNumBytes.get().utf8ToString()),
                            new BytesRef(name + "#:" + encodedMaxBytes.get().utf8ToString()),
                            false, true);
                    break;
                case GT_EQ:
                	encodedMaxBytes = new BytesRefBuilder();
                	LegacyNumericUtils.intToPrefixCoded(Integer.MAX_VALUE, 0, encodedMaxBytes);
                    query = new TermRangeQuery(LuceneFields.L_FIELD,
                            new BytesRef(name + "#:" + encodedNumBytes.get().utf8ToString()),
                            new BytesRef(name + "#:" + encodedMaxBytes.get().utf8ToString()),
                            true, true);
                    break;
                case LT:
                	encodedMinBytes = new BytesRefBuilder();
                	LegacyNumericUtils.intToPrefixCoded(Integer.MIN_VALUE, 0, encodedMinBytes);
                    query = new TermRangeQuery(LuceneFields.L_FIELD,
                            new BytesRef(name + "#:" + encodedMinBytes.get().utf8ToString()),
                            new BytesRef(name + "#:" + encodedNumBytes.get().utf8ToString()),
                            true, false);
                    break;
                case LT_EQ:
                	encodedMinBytes = new BytesRefBuilder();
                	LegacyNumericUtils.intToPrefixCoded(Integer.MIN_VALUE, 0, encodedMinBytes);
                    query = new TermRangeQuery(LuceneFields.L_FIELD,
                            new BytesRef(name + "#:" + encodedMinBytes.get().utf8ToString()),
                            new BytesRef(name + "#:" + encodedNumBytes.get().utf8ToString()),
                            true, true);
                    break;
                default:
                    assert false;
            }

            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(LuceneFields.L_FIELD, range.toString() + number), query, evalBool(bool));
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
