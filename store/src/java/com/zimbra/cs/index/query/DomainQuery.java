/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by email domain.
 *
 * @author tim
 * @author ysasaki
 */
public final class DomainQuery extends Query {
    private final String field;
    private final String term;
    private final Set<MailItem.Type> types;

    public DomainQuery(String field, String term, Set<MailItem.Type> types) {
        this.field = field;
        this.term = term;
        this.types = types;
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        LuceneQueryOperation op = new LuceneQueryOperation();
        op.addClause(toQueryString(field, term), new TermQuery(new Term(field, term)), evalBool(bool), getIndexTypes(types));
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("DOMAIN:");
        out.append(term);
    }

    @Override
    public void sanitizedDump(StringBuilder out) {
        out.append("DOMAIN:");
        out.append("$TEXT");
    }
}
