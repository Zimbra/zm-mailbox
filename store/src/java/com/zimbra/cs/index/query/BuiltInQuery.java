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

import java.util.EnumSet;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Built-in queries.
 * <p>
 * Use {@link #getQuery(String, Mailbox, Analyzer, int)} to create an instance.
 *
 * @author tim
 * @author ysasaki
 */
public abstract class BuiltInQuery {

    private BuiltInQuery() {
    }

    abstract Query create(Mailbox mailbox) throws ServiceException;

    public static Query getQuery(String name, Mailbox mailbox) throws ServiceException {
        BuiltInQuery query = BUILTIN_QUERIES.get(name);
        if (query != null) {
            return query.create(mailbox);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final Map<String, BuiltInQuery> BUILTIN_QUERIES = ImmutableMap.<String, BuiltInQuery>builder()
        .put("read", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new ReadQuery(true);
            }
        })
        .put("unread", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new ReadQuery(false);
            }
        })
        .put("flagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new FlaggedQuery(true);
            }
        })
        .put("unflagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new FlaggedQuery(false);
            }
        })
        .put("draft", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new DraftQuery(true);
            }
        })
        .put("received", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new SentQuery(false);
            }
        })
        .put("replied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new RepliedQuery(true);
            }
        })
        .put("unreplied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new RepliedQuery(false);
            }
        })
        .put("forwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new ForwardedQuery(true);
            }
        })
        .put("unforwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox)  {
                return new ForwardedQuery(false);
            }
        })
        .put("invite", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) {
                return new InviteQuery(true);
            }
        })
        .put("anywhere", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return InQuery.create(InQuery.In.ANY, false);
            }
        })
        .put("local", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return InQuery.create(InQuery.In.LOCAL, false);
            }
        })
        .put("remote", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return InQuery.create(InQuery.In.REMOTE, true);
            }
        })
        .put("solo", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return ConvCountQuery.create("1");
            }
        })
        .put("sent", new BuiltInQuery() { // send by me
            @Override
            Query create(Mailbox mbox) {
                return new SentQuery(true);
            }
        })
        .put("tome", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.TO));
            }
        })
        .put("fromme", new BuiltInQuery() { // sent by me
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.FROM));
            }
        })
        .put("ccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.CC));
            }
        })
        .put("tofromme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.FROM));
            }
        })
        .put("toccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.CC));
            }
        })
        .put("fromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox, EnumSet.of(AddrQuery.Address.FROM, AddrQuery.Address.CC));
            }
        })
        .put("tofromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox) throws ServiceException {
                return MeQuery.create(mbox,
                        EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.FROM, AddrQuery.Address.CC));
            }
        })
        .build();
}
