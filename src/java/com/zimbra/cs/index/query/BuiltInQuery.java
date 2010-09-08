package com.zimbra.cs.index.query;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
import static com.zimbra.cs.index.ZimbraQuery.*;

import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Built-in queries.
 * <p>
 * Use {@link #getQuery(String, Mailbox, Analyzer, int)} to create an instance.
 *
 * @author ysasaki
 */
public abstract class BuiltInQuery {

    private BuiltInQuery() {
    }

    abstract BaseQuery create(Mailbox mailbox, Analyzer analyzer,
            int mod) throws ServiceException;

    public static BaseQuery getQuery(String name, Mailbox mailbox,
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
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ReadQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unread", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ReadQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("flagged", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new FlaggedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unflagged", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new FlaggedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("draft", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new DraftQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("received", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("replied", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new RepliedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unreplied", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new RepliedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("forwarded", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ForwardedQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("unforwarded", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new ForwardedQuery(mbox, mod, false);
            }
        });
        builtInQueries.put("invite", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new IsInviteQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("anywhere", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_ANY_FOLDER, false);
            }
        });
        builtInQueries.put("local", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_LOCAL_FOLDER, false);
            }
        });
        builtInQueries.put("remote", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return InQuery.Create(mbox, mod, InQuery.IN_REMOTE_FOLDER, true);
            }
        });
        builtInQueries.put("solo", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                //TODO: don't refer a constant
                return ConvCountQuery.create(mod, QueryParser.CONV_COUNT, "1");
            }
        });
        // send by me
        builtInQueries.put("sent", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("tome", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod, ADDR_BITMASK_TO);
            }
        });
        // sent by me
        builtInQueries.put("fromme", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return new SentQuery(mbox, mod, true);
            }
        });
        builtInQueries.put("ccme",  new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod, ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("tofromme", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        ADDR_BITMASK_TO | ADDR_BITMASK_FROM);
            }
        });
        builtInQueries.put("toccme", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        ADDR_BITMASK_TO | ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("fromccme", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        ADDR_BITMASK_FROM | ADDR_BITMASK_CC);
            }
        });
        builtInQueries.put("tofromccme", new BuiltInQuery() {
            @Override
            BaseQuery create(Mailbox mbox, Analyzer analyze, int mod) throws ServiceException {
                return MeQuery.create(mbox, analyze, mod,
                        ADDR_BITMASK_TO | ADDR_BITMASK_FROM | ADDR_BITMASK_CC);
            }
        });
    }

}
