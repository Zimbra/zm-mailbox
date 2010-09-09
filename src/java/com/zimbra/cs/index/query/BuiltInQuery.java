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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.index.query.parser.QueryParser;
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

    abstract Query create(Mailbox mailbox, Analyzer analyzer,
            int mod) throws ServiceException;

    public static Query getQuery(String name, Mailbox mailbox,
            Analyzer analyzer, int mod) throws ServiceException {
        BuiltInQuery query = builtInQueries.get(name);
        if (query != null) {
            return query.create(mailbox, analyzer, mod);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static final Map<String, BuiltInQuery> builtInQueries =
        new HashMap<String, BuiltInQuery>();
    static {
        builtInQueries.put("read", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ReadQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unread", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ReadQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("flagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new FlaggedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unflagged", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new FlaggedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("draft", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new DraftQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("received", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("replied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new RepliedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unreplied", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new RepliedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("forwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ForwardedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unforwarded", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ForwardedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("invite", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new InviteQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("anywhere", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_ANY_FOLDER, false);
            }
        });
        builtInQueries.put("local", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_LOCAL_FOLDER, false);
            }
        });
        builtInQueries.put("remote", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_REMOTE_FOLDER, true);
            }
        });
        builtInQueries.put("solo", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                //TODO: don't refer a constant
                return ConvCountQuery.create(mod, QueryParser.CONV_COUNT, "1");
            }
        });
        // send by me
        builtInQueries.put("sent", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("tome", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod, AddrQuery.ADDR_BITMASK_TO);
            }
        });
        // sent by me
        builtInQueries.put("fromme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("ccme",  new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod, AddrQuery.ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("tofromme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        AddrQuery.ADDR_BITMASK_TO | AddrQuery.ADDR_BITMASK_FROM);
            }
        });
        builtInQueries.put("toccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        AddrQuery.ADDR_BITMASK_TO | AddrQuery.ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("fromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        AddrQuery.ADDR_BITMASK_FROM | AddrQuery.ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("tofromccme", new BuiltInQuery() {
            @Override
            Query create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        AddrQuery.ADDR_BITMASK_TO | AddrQuery.ADDR_BITMASK_FROM | AddrQuery.ADDR_BITMASK_CC);
            }
        });
    }

}
