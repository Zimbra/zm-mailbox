/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016, 2017 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query messages "to me", "from me", "cc me" or any combination thereof.
 *
 * @author tim
 * @author ysasaki
 */
public final class MeQuery extends SubQuery {

    private MeQuery(List<Query> clauses) {
        super(clauses);
    }

    public static Query create(Mailbox mbox, Set<AddrQuery.Address> addrs) throws ServiceException {
        List<Query> clauses = new ArrayList<Query>();
        Account acct = mbox.getAccount();
        if (addrs.contains(AddrQuery.Address.FROM)) {
            clauses.add(new TextQuery(LuceneFields.L_H_FROM, acct.getName()));
        }
        if (addrs.contains(AddrQuery.Address.TO)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(LuceneFields.L_H_TO, acct.getName()));
        }
        if (addrs.contains(AddrQuery.Address.CC)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(LuceneFields.L_H_CC, acct.getName()));
        }

        for (String alias : acct.getMailAlias()) {
            if (addrs.contains(AddrQuery.Address.TO)) {
                if (!clauses.isEmpty()) {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TextQuery(LuceneFields.L_H_TO, alias));
            }
            if (addrs.contains(AddrQuery.Address.CC)) {
                if (!clauses.isEmpty()) {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new TextQuery(LuceneFields.L_H_CC, alias));
            }
        }
        return new MeQuery(clauses);
    }
}
