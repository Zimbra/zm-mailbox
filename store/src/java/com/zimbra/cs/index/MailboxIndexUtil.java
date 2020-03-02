package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem.QueryParams;
import com.zimbra.cs.index.queue.IndexingQueueAdapter;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.util.ProvisioningUtil;

public class MailboxIndexUtil {

    public static void asyncDeleteIndex(Mailbox mbox, OperationContext octxt) {
        Thread deleteIndex = new Thread() {
            @Override
            public void run() {
                try {
                    mbox.index.deleteIndex();
                    if (mbox.getContactCount() > 0) {
                        // Rebuild contact index for sieve rules.
                        mbox.index.startReIndexByType(EnumSet.of(MailItem.Type.CONTACT), octxt, true);
                    }
                } catch (IOException | ServiceException e) {
                    ZimbraLog.index.error("error deleting index");
                }
            }

        };
        deleteIndex.start();
    }

    public static void asyncReIndexMailbox(Mailbox mbox, OperationContext octxt) {
        asyncReIndexMailbox(mbox, null, octxt);
    }

    public static void asyncReIndexMailbox(Mailbox mbox, Set<MailItem.Type> types, OperationContext octxt) {
        asyncReIndexMailboxes(Collections.singletonList(new MailboxReIndexSpec(mbox, types)), octxt);

    }

    public static void asyncReIndexMailboxes(List<MailboxReIndexSpec> mailboxes, OperationContext octxt) {
        Thread reindexer = new Thread() {
            @Override
            public void run() {
                //create a map of oldest and newest dates, so we can avoid querying a mailbox for days that are outside of it's lifetime
                HashMap<String,Long> oldestDates = Maps.newHashMap();
                HashMap<String,Long> newestDates = Maps.newHashMap();
                Long oldest = Long.MAX_VALUE;
                Long mostRecent = 0L;
                for(MailboxReIndexSpec mboxSpec : mailboxes) {
                    Mailbox mbox = mboxSpec.getMailbox();
                    String accountId = mbox.getAccountId();
                    try {
                        Pair<Long,Long> dateBoundaries = mbox.getSearchableItemDateBoundaries(octxt, mboxSpec.getTypes());
                        Long oldDate = dateBoundaries.getFirst();
                        Long recentDate = dateBoundaries.getSecond();

                        if(recentDate < oldDate) {
                            //this mailbox is empty
                            IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
                            if(queueAdapter == null) {
                                throw ServiceException.FAILURE("Indexing Queue Adapter is not properly configured", null);
                            }
                            queueAdapter.deleteMailboxTaskCounts(accountId);
                            queueAdapter.setTaskStatus(accountId, ReIndexStatus.STATUS_DONE);
                        } else {
                            newestDates.put(accountId, recentDate);
                            oldestDates.put(accountId, oldDate);
                            if(oldDate < oldest) {
                                oldest = oldDate;
                            }
                            if(recentDate > mostRecent) {
                                mostRecent = recentDate;
                            }
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.index.error("Failed to queue items for re-indexing by date for account %s", accountId, e);
                    }
                }

                //within each day, index newest messages first
                QueryParams params = new QueryParams();
                params.setOrderBy(Arrays.asList(new String[]{"date desc"}));
                //slide day-long time window from most recent to oldest message
                while(mostRecent >= oldest) {
                    Long dayBefore = mostRecent - Constants.MILLIS_PER_DAY;
                    long interval = ProvisioningUtil.getTimeIntervalServerAttribute(ZAttrProvisioning.A_zimbraIndexingQueuePollingInterval, 500L);
                    long maxWait = ProvisioningUtil.getTimeIntervalServerAttribute(ZAttrProvisioning.A_zimbraIndexingQueueTimeout, 10000L);
                    for(MailboxReIndexSpec mboxSpec : mailboxes) {
                        Mailbox mbox = mboxSpec.getMailbox();
                        String accountId = mbox.getAccountId();
                        //check if current time window overlaps with the range of this account's mail items dates
                        if( !(oldestDates.get(accountId) > mostRecent || newestDates.get(accountId) < dayBefore) ) {
                            //generated SQL query has non-inclusive boundaries
                            params.setDateAfter((int) (dayBefore-1L));
                            params.setDateBefore((int) (mostRecent+1L));

                            params.clearTypes();
                            Set<Type> types = mboxSpec.getTypes();
                            if (types == null) {
                                params.setExcludedTypes(EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER, MailItem.Type.MOUNTPOINT,
                                        MailItem.Type.TAG, MailItem.Type.CONVERSATION));
                            } else {
                                params.setIncludedTypes(types);
                            }
                            try {
                                while(maxWait > 0) {
                                    List<Integer> ids = mbox.getItemIdList(octxt, params);
                                    if(ids.isEmpty()) {
                                        break;
                                    }
                                    if(mbox.index.startReIndexById(ids)) {
                                        ZimbraLog.index.debug("Queued %d items for re-indexing for account %s for date range %tD - %tD", ids.size(), accountId, new Date(dayBefore), new Date(mostRecent));
                                        break;
                                    } else {
                                        //queue is full. Wait for space to free up
                                        try {
                                            Thread.sleep(interval);
                                        } catch (InterruptedException e) {
                                            break;
                                        }
                                        maxWait-=interval;
                                        continue;
                                    }
                                }
                            } catch (ServiceException e) {
                                ZimbraLog.index.error("Failed to queue items for re-indexing by date. Start date: %d, end date: %d, accountId: %s", dayBefore, mostRecent, accountId, e);
                            }
                        }
                    }
                    //move the window down by one day
                    mostRecent = dayBefore;
                }
            }
        };
        reindexer.setName("ReIndByDate-InitiatedBy-" + octxt.getmAuthTokenAccountId());
        reindexer.start();
    }

    public static class MailboxReIndexSpec extends Pair<Mailbox, Set<MailItem.Type>>{

        public MailboxReIndexSpec(Mailbox mbox) {
            this(mbox, null);
        }

        public MailboxReIndexSpec(Mailbox mbox, Set<Type> types) {
            super(mbox, types);
        }

        public Mailbox getMailbox() {
            return getFirst();
        }

        public Set<MailItem.Type> getTypes() {
            return getSecond();
        }
    }
}
