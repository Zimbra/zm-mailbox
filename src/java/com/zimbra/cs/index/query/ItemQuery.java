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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.NoResultsQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

/**
 * Query by item ID.
 *
 * @author tim
 * @author ysasaki
 */
public final class ItemQuery extends Query {

    private final boolean isAllQuery;
    private final boolean isNoneQuery;
    private final List<ItemId> itemIds;

    public static Query create(Mailbox mbox, String str) throws ServiceException {
        boolean allQuery = false;
        boolean noneQuery = false;
        List<ItemId> itemIds = new ArrayList<ItemId>();

        if (str.equalsIgnoreCase("all")) {
            allQuery = true;
        } else if (str.equalsIgnoreCase("none")) {
            noneQuery = true;
        } else {
            String[] items = str.split(",");
            for (int i = 0; i < items.length; i++) {
                if (items[i].length() > 0) {
                    ItemId iid = new ItemId(items[i], mbox.getAccountId());
                    itemIds.add(iid);
                }
            }
            if (itemIds.size() == 0) {
                noneQuery = true;
            }
        }

        return new ItemQuery(allQuery, noneQuery, itemIds);
    }

    ItemQuery(boolean all, boolean none, List<ItemId> ids) {
        this.isAllQuery = all;
        this.isNoneQuery = none;
        this.itemIds = ids;
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        DBQueryOperation dbOp = new DBQueryOperation();
        bool = evalBool(bool);
        if (bool && isAllQuery || !bool && isNoneQuery) {
            // adding no constraints should match everything...
        } else if (bool && isNoneQuery || !bool && isAllQuery) {
            return new NoResultsQueryOperation();
        } else {
            for (ItemId iid : itemIds) {
                dbOp.addItemIdClause(mbox, iid, bool);
            }
        }
        return dbOp;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("ITEMID");
        if (isAllQuery) {
            out.append(",all");
        } else if (isNoneQuery) {
            out.append(",none");
        } else {
            for (ItemId id : itemIds) {
                out.append(',');
                out.append(id.toString());
            }
        }
    }
}
