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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

/**
 * Query by conversation ID.
 *
 * @author tim
 * @author ysasaki
 */
public final class ConvQuery extends Query {
    private ItemId mConvId;
    private Mailbox mMailbox;

    private ConvQuery(Mailbox mbox, int mod, ItemId convId) throws ServiceException {
        super(mod, QueryParser.CONV);
        mMailbox = mbox;
        mConvId = convId;

        if (mConvId.getId() < 0) {
            // should never happen (make an ItemQuery instead
            throw ServiceException.FAILURE("Illegal Negative ConvID: " +
                    convId.toString() + ", use ItemQuery for virtual convs",
                    null);
        }
    }

    public static Query create(Mailbox mbox, int mod, String target)
        throws ServiceException {

        ItemId convId = new ItemId(target, mbox.getAccountId());
        if (convId.getId() < 0) {
            // ...convert negative convId to positive ItemId...
            convId = new ItemId(convId.getAccountId(), -1 * convId.getId());
            List<ItemId> iidList = new ArrayList<ItemId>(1);
            iidList.add(convId);
            return new ItemQuery(mbox, mod, false, false, iidList);
        } else {
            return new ConvQuery(mbox, mod, convId);
        }
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        DBQueryOperation op = new DBQueryOperation();
        op.addConvId(mMailbox, mConvId, calcTruth(truth));
        return op;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append(',');
        out.append(mConvId);
        return out.append(')');
    }

}
