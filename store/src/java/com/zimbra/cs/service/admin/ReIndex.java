/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.db.DbMailItem.QueryParams;
import com.zimbra.cs.index.queue.IndexingQueueAdapter;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ReIndexRequest;
import com.zimbra.soap.admin.message.ReIndexResponse;
import com.zimbra.soap.admin.type.ReindexMailboxInfo;
import com.zimbra.soap.admin.type.ReindexProgressInfo;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * Admin operation handler for {@code reIndexMailbox(rim)}.
 *
 * @author tim
 * @author ysasaki
 */
public final class ReIndex extends AdminDocumentHandler {

    private static final String ACTION_START = "start";
    private static final String ACTION_STATUS = "status";
    private static final String ACTION_ABORT = "cancel";
    /**
     * legacy (pre 9.x) SOAP clients expect String status ('started', 'idle', 'running' or 'cancelled')
     * /new (post 9.x) SOAP clients use integer status values from {@link ReIndexStatus}
     */
    public static final String STATUS_STARTED = "started";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_IDLE = "idle";
    public static final String STATUS_CANCELLED = "cancelled";

    private static final String[] TARGET_ACCOUNT_PATH = new String[] {
        AdminConstants.E_MAILBOX, AdminConstants.A_ACCOUNTID
    };

    @Override
    protected String[] getProxiedAccountPath() {
        return TARGET_ACCOUNT_PATH;
    }

    /**
     * must be careful and only allow access to domain if domain admin.
     */
    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ReIndexRequest req = JaxbUtil.elementToJaxb(request);
        String action = req.getAction();
        List<ReindexMailboxInfo> mailboxSelectors = req.getMbox();
        ServerSelector serverSelector = req.getServer();

        Provisioning prov = Provisioning.getInstance();
        List<String> accountIds = new ArrayList<String>();

        if(mailboxSelectors != null && mailboxSelectors.size() > 0) {
            for(ReindexMailboxInfo e : mailboxSelectors) {
                accountIds.add(e.getAccountId());
            }
        } else if(serverSelector != null) {
            //requesting re-indexing of all accounts on this server - this is likely an upgrade
            boolean isLocalServer = false;
            Key.ServerBy serverBy = Key.ServerBy.valueOf(serverSelector.getBy().toString());
            String serverKey = serverSelector.getKey();
            Server server = prov.get(serverBy, serverKey);
            if (server == null) {
                throw AccountServiceException.NO_SUCH_SERVER(serverKey);
            }
            isLocalServer = server.isLocalServer();
            if(!isLocalServer) {
                //if the server is not local, proxy the request to the target server
                return proxyRequest(request, context, server);
            }

            //get all accounts on this server
            SearchAccountsOptions searchOpts = new SearchAccountsOptions(new String[] { Provisioning.A_zimbraId });
            searchOpts.setMakeObjectOpt(MakeObjectOpt.NO_SECONDARY_DEFAULTS);
            searchOpts.setSortOpt(SortOpt.SORT_DESCENDING);
            List<NamedEntry> accts = prov.searchAccountsOnServer(server, searchOpts);
            for(NamedEntry entry : accts) {
                accountIds.add(entry.getId());
            }
        } else {
            throw ServiceException.INVALID_REQUEST("Either 'mbox' or 'server' element is required", null);
        }

        //right check. Do not proceed if the admin lacks permission for any of the requested accounts
        for(ReindexMailboxInfo e : mailboxSelectors) {
            Account account = prov.get(AccountBy.id, e.getAccountId(), zsc.getAuthToken());
            if (account == null) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(e.getAccountId());
            }

            defendAgainstAccountOrCalendarResourceHarvesting(account, AccountBy.id, e.getAccountId(), zsc,
                    Admin.R_reindexMailbox, Admin.R_reindexCalendarResourceMailbox);
        }
        OperationContext octxt = getOperationContext(zsc, context);
        Element response = zsc.createElement(AdminConstants.REINDEX_RESPONSE);
        if(ACTION_STATUS.equalsIgnoreCase(action)) {
            int totalSucceeded = 0;
            int totalFailed = 0;
            int totalRemaining = 0;
            //for legacy SOAP clients, unless any mailboxes are still re-indexing or all have aborted re-indexing - set overall status string to 'idle'
            String statusString = STATUS_IDLE;
            int statusCode = ReIndexStatus.STATUS_IDLE;
            for(ReindexMailboxInfo e : mailboxSelectors) {
                Account account = prov.getAccount(e.getAccountId());

                int accountStatusCode = ReIndexStatus.STATUS_IDLE;
                Element mboxStatus = response.addNonUniqueElement(AdminConstants.E_MAILBOX);
                mboxStatus.addAttribute(AdminConstants.A_ACCOUNTID, account.getId());

                //proxy requests for accounts that don't reside on this server
                if (!Provisioning.onLocalServer(account)) {
                    ReIndexResponse resp = proxyRequest(account.getId(), account.getMailHost(), action, context);
                    List<ReindexProgressInfo> mailboxProgress = resp.getMbox();
                    if(mailboxProgress != null && !mailboxProgress.isEmpty()) {
                        ReindexProgressInfo progressInfo = mailboxProgress.get(0);
                        totalSucceeded += progressInfo.getNumSucceeded();
                        totalFailed += progressInfo.getNumFailed();
                        totalRemaining += progressInfo.getNumRemaining();
                        accountStatusCode = progressInfo.getStatusCode();
                        addProgressInfo(mboxStatus,
                                new ReIndexStatus(totalSucceeded+totalFailed+totalRemaining,
                                        totalSucceeded, totalFailed, accountStatusCode));
                    }
                } else {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
                    if (mbox == null) {
                        throw ServiceException.FAILURE("mailbox not found for account " + account.getId(), null);
                    }
                    ReIndexStatus status = mbox.index.getReIndexStatus();
                    accountStatusCode = status.getStatus();
                    totalSucceeded = status.getSucceeded();
                    totalFailed = status.getFailed();
                    totalRemaining = (status.getTotal() - status.getProcessed());
                    addProgressInfo(mboxStatus, status);
                }
                //for legacy SOAP clients, if any mailboxes are still re-indexing - set overall status string to 'running'
                if(accountStatusCode == ReIndexStatus.STATUS_RUNNING) {
                    statusString = STATUS_RUNNING;
                }
                //for legacy SOAP clients, if all mailboxes aborted re-indexing - set overall status string to 'cancelled'
                if(!statusString.equalsIgnoreCase(STATUS_RUNNING) && accountStatusCode == ReIndexStatus.STATUS_ABORTED) {
                    statusString = STATUS_CANCELLED;
                }

                if(accountStatusCode == ReIndexStatus.STATUS_RUNNING) {
                    //code 1, tells SOAP client to keep polling for status updates
                    statusCode = ReIndexStatus.STATUS_RUNNING;
                } else if(statusCode == ReIndexStatus.STATUS_IDLE || statusCode == ReIndexStatus.STATUS_DONE) {
                    //overwrite done and idle statuses and retain first encountered negative status
                    statusCode = accountStatusCode;
                }
            }
            Element prog = response.addUniqueElement(AdminConstants.E_PROGRESS);
            response.addAttribute(AdminConstants.A_STATUS_CODE, statusCode);
            response.addAttribute(AdminConstants.A_STATUS, statusString);

            //totals for legacy SOAP clients
            prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED, totalSucceeded);
            prog.addAttribute(AdminConstants.A_NUM_FAILED, totalFailed);
            prog.addAttribute(AdminConstants.A_NUM_REMAINING, totalRemaining);
        } else if (ACTION_START.equalsIgnoreCase(action)) {
            int succeeded = 0;
            int failed = 0;
            int remaining = 0;
            //filter out remote accounts and proxy requests to their home servers
            List<Mailbox> mailboxesToReindexByTime = Lists.newArrayList();
            for(ReindexMailboxInfo reIndexMboxInfo : mailboxSelectors) {
                Account account = prov.getAccount(reIndexMboxInfo.getAccountId());
                Element mboxStatus = response.addNonUniqueElement(AdminConstants.E_MAILBOX);
                mboxStatus.addAttribute(AdminConstants.A_ACCOUNTID, account.getId());

                //proxy requests for accounts that don't reside on this server
                if (!Provisioning.onLocalServer(account)) {
                    ReIndexResponse resp = proxyRequest(account.getId(), account.getMailHost(), action, context);
                    List<ReindexProgressInfo> mailboxProgress = resp.getMbox();
                    if(mailboxProgress != null && !mailboxProgress.isEmpty()) {
                        ReindexProgressInfo progressInfo = mailboxProgress.get(0);
                        succeeded += progressInfo.getNumSucceeded();
                        failed += progressInfo.getNumFailed();
                        remaining += progressInfo.getNumRemaining();
                        addProgressInfo(mboxStatus,
                                new ReIndexStatus(succeeded+failed+remaining,
                                        succeeded, failed, progressInfo.getStatusCode()));
                    }
                } else {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
                    ReIndexStatus status;
                    if (mbox == null) {
                        throw ServiceException.FAILURE("mailbox not found for account " + account.getId(), null);
                    }
                    String typesStr = reIndexMboxInfo.getTypes();
                    String idsStr = reIndexMboxInfo.getIds();

                    if (typesStr != null && idsStr != null) {
                        ServiceException.INVALID_REQUEST("Can't specify both 'types' and 'ids'", null);
                    }

                    if (typesStr != null) {
                        Set<MailItem.Type> types;
                        try {
                            types = MailItem.Type.setOf(typesStr);
                        } catch (IllegalArgumentException e) {
                            throw MailServiceException.INVALID_TYPE(e.getMessage());
                        }
                        mbox.index.startReIndexByType(types, octxt);
                        status = mbox.index.getReIndexStatus();
                    } else if (idsStr != null) {
                        Set<Integer> ids = new HashSet<Integer>();
                        for (String id : Splitter.on(',').trimResults().split(idsStr)) {
                            try {
                                ids.add(Integer.parseInt(id));
                            } catch (NumberFormatException e) {
                                ServiceException.INVALID_REQUEST("invalid item ID: " + id, e);
                            }
                        }
                        mbox.index.startReIndexById(ids);
                        status = mbox.index.getReIndexStatus();
                    } else {
                        mailboxesToReindexByTime.add(mbox);
                        status = new ReIndexStatus(0,0,0,ReIndexStatus.STATUS_RUNNING);
                    }
                    addProgressInfo(mboxStatus, status);
                }
            }

            //start a separate thread that will queue up items for re-indexing
            Thread reindexer = new Thread() {
                @Override
                public void run() {
                    //create a map of oldest and newest dates, so we can avoid querying a mailbox for days that are outside of it's lifetime
                    HashMap<String,Long> oldestDates = Maps.newHashMap();
                    HashMap<String,Long> newestDates = Maps.newHashMap();
                    Long oldest = Long.MAX_VALUE;
                    Long mostRecent = 0L;
                    for(Mailbox mbox : mailboxesToReindexByTime) {
                        String accountId = mbox.getAccountId();
                        try {
                            Pair<Long,Long> dateBoundaries = mbox.getSearchableItemDateBoundaries(octxt);
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
                    params.setExcludedTypes(EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER, MailItem.Type.MOUNTPOINT,
                            MailItem.Type.TAG, MailItem.Type.CONVERSATION));
                    //slide day-long time window from most recent to oldest message
                    while(mostRecent >= oldest) {
                        Long dayBefore = mostRecent - Constants.MILLIS_PER_DAY;
                        long interval = ProvisioningUtil.getTimeIntervalServerAttribute(ZAttrProvisioning.A_zimbraIndexingQueuePollingInterval, 500L);
                        long maxWait = ProvisioningUtil.getTimeIntervalServerAttribute(ZAttrProvisioning.A_zimbraIndexingQueueTimeout, 10000L);
                        for(Mailbox mbox : mailboxesToReindexByTime) {
                            String accountId = mbox.getAccountId();
                            //check if current time window overlaps with the range of this account's mail items dates
                            if( !(oldestDates.get(accountId) > mostRecent || newestDates.get(accountId) < dayBefore) ) {
                                //generated SQL query has non-inclusive boundaries
                                params.setDateAfter((int) (dayBefore-1L));
                                params.setDateBefore((int) (mostRecent+1L));
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
            reindexer.setName("ReIndByDate-InitiatedBy-" + zsc.getAuthToken().getAccount().getName());
            reindexer.start();

            //legacy SOAP clients expect "started" in status string when sending ReIndexRequest with action='start'
            Element prog = response.addUniqueElement(AdminConstants.E_PROGRESS);
            prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED, 0);
            prog.addAttribute(AdminConstants.A_NUM_FAILED, 0);
            prog.addAttribute(AdminConstants.A_NUM_REMAINING, 0);
            response.addAttribute(AdminConstants.A_STATUS_CODE, ReIndexStatus.STATUS_RUNNING);
            response.addAttribute(AdminConstants.A_STATUS, STATUS_STARTED);
        } else if (ACTION_ABORT.equalsIgnoreCase(action)) {
            int totalSucceeded = 0;
            int totalFailed = 0;
            int totalRemaining = 0;
            for(ReindexMailboxInfo reIndexMboxInfo : mailboxSelectors) {
                Element mboxStatus = response.addNonUniqueElement(AdminConstants.E_MAILBOX);
                Account account = prov.getAccount(reIndexMboxInfo.getAccountId());
                mboxStatus.addAttribute(AdminConstants.A_ACCOUNTID, account.getId());
                if (!Provisioning.onLocalServer(account)) {
                    ReIndexResponse resp = proxyRequest(account.getId(), account.getMailHost(), action, context);
                    List<ReindexProgressInfo> mailboxProgress = resp.getMbox();
                    if(mailboxProgress != null && !mailboxProgress.isEmpty()) {
                        ReindexProgressInfo progressInfo = mailboxProgress.get(0);
                        int failed = progressInfo.getNumFailed();
                        int remaining = progressInfo.getNumRemaining();
                        int succeeded = progressInfo.getNumSucceeded();
                        int accountStatusCode = progressInfo.getStatusCode();
                        totalSucceeded += succeeded;
                        totalRemaining += remaining;
                        totalFailed += failed;
                        addProgressInfo(mboxStatus,
                                new ReIndexStatus(succeeded+failed+remaining,
                                        succeeded, failed, accountStatusCode));
                    }
                } else {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), false);
                    if (mbox == null) {
                        throw ServiceException.FAILURE("mailbox not found for account " + account.getId(), null);
                    }
                    ReIndexStatus status = mbox.index.abortReIndex();
                    addProgressInfo(mboxStatus, status);
                }
            }

            //legacy SOAP clients expect "cancelled" in status attribute of the response
            //when sending ReIndexRequest with action='cancel'
            response.addAttribute(AdminConstants.A_STATUS_CODE, ReIndexStatus.STATUS_ABORTED);
            response.addAttribute(AdminConstants.A_STATUS, STATUS_CANCELLED);

            //Adding aggregate progress for legacy SOAP clients
            Element prog = response.addUniqueElement(AdminConstants.E_PROGRESS);
            prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED, totalSucceeded);
            prog.addAttribute(AdminConstants.A_NUM_FAILED, totalFailed);
            prog.addAttribute(AdminConstants.A_NUM_REMAINING, totalRemaining);

        } else {
            throw ServiceException.INVALID_REQUEST("Unknown action: " + action, null);
        }
        return response;
    }

    private void addProgressInfo(Element mboxElement, ReIndexStatus status) {
        mboxElement.addAttribute(AdminConstants.A_NUM_SUCCEEDED, status.getSucceeded());
        mboxElement.addAttribute(AdminConstants.A_NUM_FAILED, status.getFailed());
        mboxElement.addAttribute(AdminConstants.A_NUM_REMAINING, status.getTotal() > 0 ? (status.getTotal() - status.getSucceeded() - status.getFailed()) : 0);
        mboxElement.addAttribute(AdminConstants.A_STATUS_CODE, status.getStatus());
    }

    private ReIndexResponse proxyRequest(String accountId, String host, String action, Map<String, Object> context) throws ServiceException {
        ReindexMailboxInfo mboxInfo = new ReindexMailboxInfo(accountId);
        ReIndexRequest r = new ReIndexRequest(action, Lists.newArrayList(mboxInfo));
        ReIndexResponse resp = (ReIndexResponse)JaxbUtil.elementToJaxb(
                proxyRequest(JaxbUtil.jaxbToElement(r), context, Provisioning.getInstance().getServerByName(host)));
        return resp;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_reindexMailbox);
        relatedRights.add(Admin.R_reindexCalendarResourceMailbox);
    }
}
