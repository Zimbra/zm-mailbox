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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;

/**
 * Query by tag.
 *
 * @author tim
 * @author ysasaki
 */
public class TagQuery extends Query {

    private final Tag mTag;

    public TagQuery(Mailbox mailbox, String name, boolean truth)
        throws ServiceException {
        mTag = mailbox.getTagByName(name);
        setBool(truth);
    }

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        DBQueryOperation dbOp = new DBQueryOperation();
        dbOp.addTagClause(mTag, evalBool(bool));
        return dbOp;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("TAG,");
        out.append(mTag.getName());
    }

}
