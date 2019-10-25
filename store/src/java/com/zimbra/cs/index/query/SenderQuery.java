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

import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by sender.
 *
 * @author tim
 * @author ysasaki
 */
public final class SenderQuery extends Query {
    private final String sender;
    private final Comparison comparison;

    /**
     * This is only used for subject queries that start with {@code <} or {@code >}, otherwise we just use the normal
     * {@link TextQuery}.
     */
    private SenderQuery(String text) {
        comparison = Comparison.fromPrefix(text);
        sender = text.substring(comparison.toString().length());
    }

    public static Query create(String text) {
        return create(text, false);
    }

    public static Query create(String text, boolean isPhraseQuery) {
        if (text.length() > 1 &&
                (text.startsWith(Comparison.LT.toString()) || text.startsWith(Comparison.GT.toString()))) {
            return new SenderQuery(text);
        } else {
            return new TextQuery(LuceneFields.L_H_FROM, text, isPhraseQuery);
        }
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        switch (comparison) {
            case LE:
                op.addSenderRange(null, false, sender, true, evalBool(bool));
                break;
            case LT:
                op.addSenderRange(null, false, sender, false, evalBool(bool));
                break;
            case GE:
                op.addSenderRange(sender, true, null, false, evalBool(bool));
                break;
            case GT:
                op.addSenderRange(sender, false, null, false, evalBool(bool));
                break;
            default:
                assert false : comparison;
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SENDER:").append(comparison).append(sender);
    }

    @Override
    public void sanitizedDump(StringBuilder out) {
        out.append("SENDER:").append(comparison).append("$TEXT");
    }
}
