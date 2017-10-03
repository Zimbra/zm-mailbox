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
 * Query by subject.
 *
 * @author tim
 * @author ysasaki
 */
public final class SubjectQuery extends Query {
    private String subject;
    private boolean lt;
    private boolean inclusive;

    /**
     * This is only used for subject queries that start with {@code <} or {@code >}, otherwise we just use the normal
     * {@link TextQuery}.
     */
    private SubjectQuery(String text, Comparison comp) {
        subject = text;
        lt = comp.equals(Comparison.LE) || comp.equals(Comparison.LT);
        inclusive = comp.equals(Comparison.GE) || comp.equals(Comparison.LE);
    }

    public static Query create(String text, Comparison comp) {
        if (comp != null) {
            // real subject query!
            return new SubjectQuery(text, comp);
        } else {
            return new TextQuery(LuceneFields.L_H_SUBJECT, text);
        }
    }

    public static Query create(String text) {
        return create(text, null);
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        if (lt) {
            op.addSubjectRange(null, false, subject, inclusive, evalBool(bool));
        } else {
            op.addSubjectRange(subject, inclusive, null, false, evalBool(bool));
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SUBJECT:");
        out.append(lt ? '<' : '>');
        if (inclusive) {
            out.append('=');
        }
        out.append(subject);
    }

    @Override
    public void sanitizedDump(StringBuilder out) {
        out.append("SUBJECT:");
        out.append(lt ? '<' : '>');
        if (inclusive) {
            out.append('=');
        }
        out.append("$TEXT");
    }
}
