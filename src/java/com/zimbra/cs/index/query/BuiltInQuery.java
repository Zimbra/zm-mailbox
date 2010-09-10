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

    abstract Query create(Mailbox mailbox, Analyzer analyzer) throws ServiceException;

    public static Query getQuery(String name, Mailbox mailbox,
            Analyzer analyzer) throws ServiceException {
        BuiltInQuery query = builtInQueries.get(name);
        if (query != null) {
            return query.create(mailbox, analyzer);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final Map<String, BuiltInQuery> builtInQueries =
        new ImmutableMap.Builder<String, BuiltInQuery>()
        .put("read", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new ReadQuery(mbox, true);
            }
        })
        .put("unread", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new ReadQuery(mbox, false);
            }
        })
        .put("flagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new FlaggedQuery(mbox, true);
            }
        })
        .put("unflagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new FlaggedQuery(mbox, false);
            }
        })
        .put("draft", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new DraftQuery(mbox, true);
            }
        })
        .put("received", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new SentQuery(mbox, false);
            }
        })
        .put("replied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new RepliedQuery(mbox, true);
            }
        })
        .put("unreplied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new RepliedQuery(mbox, false);
            }
        })
        .put("forwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new ForwardedQuery(mbox, true);
            }
        })
        .put("unforwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new ForwardedQuery(mbox, false);
            }
        })
        .put("invite", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new InviteQuery(mbox, true);
            }
        })
        .put("anywhere", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return InQuery.create(InQuery.In.ANY, false);
            }
        })
        .put("local", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return InQuery.create(InQuery.In.LOCAL, false);
            }
        })
        .put("remote", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return InQuery.create(InQuery.In.REMOTE, true);
            }
        })
        .put("solo", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return ConvCountQuery.create("1");
            }
        })
        .put("sent", new BuiltInQuery() { // send by me
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new SentQuery(mbox, true);
            }
        })
        .put("tome", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.TO));
            }
        })
        .put("fromme", new BuiltInQuery() { // sent by me
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return new SentQuery(mbox, true);
            }
        })
        .put("ccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.CC));
            }
        })
        .put("tofromme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.FROM));
            }
        })
        .put("toccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.CC));
            }
        })
        .put("fromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.FROM, AddrQuery.Address.CC));
            }
        })
        .put("tofromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze) throws ServiceException {
                return MeQuery.create(mbox, analyze,
                        EnumSet.of(AddrQuery.Address.TO, AddrQuery.Address.FROM,
                                AddrQuery.Address.CC));
            }
        })
        .build();
}
