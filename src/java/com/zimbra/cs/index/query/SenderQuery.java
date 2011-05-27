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
        if (text.startsWith(Comparison.LE.toString())) {
            comparison = Comparison.LE;
        } else if (text.startsWith(Comparison.LT.toString())) {
            comparison = Comparison.LT;
        } else if (text.startsWith(Comparison.GE.toString())) {
            comparison = Comparison.GE;
        } else if (text.startsWith(Comparison.GT.toString())) {
            comparison = Comparison.GT;
        } else {
            throw new IllegalArgumentException(text);
        }
        sender = text.substring(comparison.toString().length());
    }

    public static Query create(Analyzer analyzer, String text) {
        if (text.length() > 1 &&
                (text.startsWith(Comparison.LT.toString()) || text.startsWith(Comparison.GT.toString()))) {
            return new SenderQuery(text);
        } else {
            return new TextQuery(analyzer, LuceneFields.L_H_FROM, text);
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

}
