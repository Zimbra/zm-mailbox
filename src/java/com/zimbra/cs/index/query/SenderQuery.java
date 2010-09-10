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

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
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
    private String mStr;
    private boolean mLt;
    private boolean mEq;

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        if (mLt) {
            op.addRelativeSender(null, false, mStr, mEq, evalBool(bool));
        } else {
            op.addRelativeSender(mStr, mEq, null, false, evalBool(bool));
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("SENDER");
        out.append(mLt ? '<' : '>');
        if (mEq) {
            out.append('=');
        }
        out.append(mStr);
    }

    /**
     * Don't call directly -- use SubjectQuery.create()
     *
     * This is only invoked for subject queries that start with < or > -- otherwise we just
     * use the normal TextQuery class
     */
    private SenderQuery(String text) {
        mLt = (text.charAt(0) == '<');
        mEq = false;
        mStr = text.substring(1);

        if (mStr.charAt(0) == '=') {
            mEq = true;
            mStr= mStr.substring(1);
        }

        // bug: 27976 -- we have to allow >"" for cursors to work as expected
        //            if (mStr.length() == 0)
        //                throw MailServiceException.PARSE_ERROR("Invalid sender string: "+text, null);
    }

    public static Query create(Mailbox mbox, Analyzer analyzer, String text)
        throws ServiceException {

        if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
            return new SenderQuery(text);
        } else {
            return new TextQuery(mbox, analyzer, LuceneFields.L_H_FROM, text);
        }
    }
}
