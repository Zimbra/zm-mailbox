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

import org.apache.lucene.analysis.Analyzer;

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
    private boolean lessThan;
    private boolean equal;

    /**
     * This is only used for subject queries that start with {@code <} or {@code >}, otherwise we just use the normal
     * {@link TextQuery}.
     */
    private SubjectQuery(String text) {
        lessThan = (text.charAt(0) == '<');
        equal = false;
        subject = text.substring(1);

        if (subject.charAt(0) == '=') {
            equal = true;
            subject = subject.substring(1);
        }
    }

    public static Query create(Analyzer analyzer, String text) {
        if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
            // real subject query!
            return new SubjectQuery(text);
        } else {
            return new TextQuery(analyzer, LuceneFields.L_H_SUBJECT, text);
        }
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        if (lessThan) {
            op.addRelativeSubject(null, false, subject, equal, evalBool(bool));
        } else {
            op.addRelativeSubject(subject, equal, null, false, evalBool(bool));
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SUBJECT:");
        out.append(lessThan ? '<' : '>');
        if (equal) {
            out.append('=');
        }
        out.append(subject);
    }

}
