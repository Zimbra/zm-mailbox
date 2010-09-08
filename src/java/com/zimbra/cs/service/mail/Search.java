/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.wildfire.XMPPServer;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.im.provider.ZimbraRoutingTableImpl;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.QueryInfo;
import com.zimbra.cs.index.ResultsPager;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.cache.CacheToXML;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since May 26, 2004
 */
public class Search extends MailDocumentHandler  {
    protected static Log mLog = LogFactory.getLog(Search.class);

    public static final String DEFAULT_SEARCH_TYPES = MailboxIndex.GROUP_BY_CONVERSATION;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account acct = getRequestedAccount(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        {
            String query = request.getAttribute(MailConstants.E_QUERY, "");
            if (query.startsWith("$dump_routes")) {
                ZimbraLog.im.info("Routing Table: "+((ZimbraRoutingTableImpl)(XMPPServer.getInstance().getRoutingTable())).dumpRoutingTable());
                // create the XML response Element
                Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);
                return response;
            }
        }
        SearchParams params = SearchParams.parse(request, zsc, acct.getAttr(Provisioning.A_zimbraPrefMailInitialSearch));

        String query = params.getQueryStr();

        params.setQueryStr(query);
        if (LC.calendar_cache_enabled.booleanValue()) {
            List<String> apptFolderIds = getFolderIdListIfSimpleAppointmentsQuery(params);
            if (apptFolderIds != null) {
                Account authAcct = getAuthenticatedAccount(zsc);
                Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);
                runSimpleAppointmentQuery(response, params, octxt, zsc, authAcct, mbox, apptFolderIds);
                return response;
            }
        }

        ZimbraQueryResults results = null;
        try {
            results = doSearch(zsc, octxt, mbox, params);

            // create the XML response Element
            Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);

            // must use results.getSortBy() because the results might have ignored our sortBy
            // request and used something else...
            response.addAttribute(MailConstants.A_SORTBY, results.getSortBy().toString());
            response.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
            putHits(zsc, octxt, response, results, params);
            return response;
        } catch (ServiceException x) {
            ZimbraLog.misc.warn(x.getMessage(), x);
            throw x;
        } finally {
            if (results != null)
                results.doneWithSearchResults();
        }
    }

    protected ZimbraQueryResults doSearch(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox, SearchParams params) throws ServiceException {
        ZimbraQueryResults results;
        try {
            results = mbox.search(zsc.getResponseProtocol(), octxt, params);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IO error", e);
        }
        return results;
    }

    protected static void putInfo(Element response, SearchParams params, ZimbraQueryResults results) {
        List<QueryInfo> qinfo = results.getResultInfo();
        if ((qinfo.size() > 0) || params.getEstimateSize()) {
            Element qinfoElt = response.addElement(MailConstants.E_INFO);
            Element sizeEst = qinfoElt.addElement("sizeEstimate");
            try {
                sizeEst.addAttribute("value", results.estimateResultSize());
            } catch (ServiceException ex) {
            }

            for (QueryInfo inf : qinfo) {
                inf.toXml(qinfoElt);
            }
        }
    }

    private void putHits(ZimbraSoapContext zsc,
            OperationContext octxt, Element el, ZimbraQueryResults results,
            SearchParams params) throws ServiceException {

        if (params.getInlineRule() == ExpandResults.HITS) {
            // "hits" is not a valid value for Search...
            params.setInlineRule(ExpandResults.NONE);
        }

        ResultsPager pager = ResultsPager.create(results, params);

        SearchResponse resp = new SearchResponse(zsc, octxt, el, params);
        resp.setIncludeMailbox(false);
        resp.setSortOrder(pager.getSortOrder());

        while (pager.hasNext() && resp.size() < params.getLimit()) {
            ZimbraHit hit = pager.getNextHit();
            resp.add(hit);
        }

        resp.addHasMore(pager.hasNext());
        resp.add(results.getResultInfo(), results.estimateResultSize());
    }

    // Calendar summary cache stuff

    // Returns list of folder id string if the query is a simple appointments query.
    // Otherwise returns null.
    private static List<String> getFolderIdListIfSimpleAppointmentsQuery(SearchParams params) {
        // types = "appointment"
        byte[] types = params.getTypes();
        if (types == null || types.length != 1 ||
            (types[0] != MailItem.TYPE_APPOINTMENT && types[0] != MailItem.TYPE_TASK))
            return null;
        // has time range
        if (params.getCalItemExpandStart() == -1 || params.getCalItemExpandEnd() == -1)
            return null;
        // offset = 0
        if (params.getOffset() != 0)
            return null;
        // sortBy = "none"
        SortBy sortBy = params.getSortBy();
        if (sortBy != null && !sortBy.equals(SortBy.NONE))
            return null;

        // query string is "inid:<folder> [OR inid:<folder>]*"
        String queryStr = params.getQueryStr();
        if (queryStr == null)
            queryStr = "";
        queryStr = queryStr.toLowerCase();
        queryStr = removeOuterParens(queryStr);
        // simple appointment query can't have any ANDed terms
        if (queryStr.contains("and"))
            return null;
        String[] terms = queryStr.split("\\s+or\\s+");
        List<String> folderIdStrs = new ArrayList<String>();
        for (String term : terms) {
            term = term.trim();
            // remove outermost parentheses (light client does this, e.g. "(inid:10)")
            term = removeOuterParens(term);
            if (!term.startsWith("inid:"))
                return null;
            String folderId = term.substring(5);  // everything after "inid:"
            folderId = unquote(folderId);  // e.g. if query is: inid:"account-id:num", we want just account-id:num
            if (folderId.length() > 0)
                folderIdStrs.add(folderId);
        }
        return folderIdStrs;
    }

    private static String removeOuterParens(String str) {
        int len = str.length();
        if (len > 2 && str.charAt(0) == '(' && str.charAt(len - 1) == ')')
            str = str.substring(1, len - 1);
        return str;
    }

    private static String unquote(String str) {
        int len = str.length();
        if (len > 2 && str.charAt(0) == '\'' && str.charAt(len - 1) == '\'')
            str = str.substring(1, len - 1);
        len = str.length();
        if (len > 2 && str.charAt(0) == '"' && str.charAt(len - 1) == '"')
            str = str.substring(1, len - 1);
        return str;
    }

    private static void runSimpleAppointmentQuery(Element parent, SearchParams params,
                                                  OperationContext octxt, ZimbraSoapContext zsc,
                                                  Account authAcct, Mailbox mbox,
                                                  List<String> folderIdStrs)
    throws ServiceException {
        byte itemType = MailItem.TYPE_APPOINTMENT;
        byte[] types = params.getTypes();
        if (types != null && types.length == 1)
            itemType = types[0];

        parent.addAttribute(MailConstants.A_SORTBY, params.getSortByStr());
        parent.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        parent.addAttribute(MailConstants.A_QUERY_MORE, false);

        List<ItemId> folderIids = new ArrayList<ItemId>(folderIdStrs.size());
        for (String folderIdStr : folderIdStrs) {
            folderIids.add(new ItemId(folderIdStr, zsc));
        }

        Provisioning prov = Provisioning.getInstance();
        MailboxManager mboxMgr = MailboxManager.getInstance();
        Server localServer = prov.getLocalServer();

        Map<Server, Map<String /* account id */, List<Integer> /* folder ids */>> groupedByServer =
            groupByServer(ItemId.groupFoldersByAccount(octxt, mbox, folderIids));

        // Look up in calendar cache first.
        if (LC.calendar_cache_enabled.booleanValue()) {
            CalSummaryCache calCache = CalendarCacheManager.getInstance().getSummaryCache();
            long rangeStart = params.getCalItemExpandStart();
            long rangeEnd = params.getCalItemExpandEnd();
            for (Iterator<Map.Entry<Server, Map<String, List<Integer>>>> serverIter = groupedByServer.entrySet().iterator();
                 serverIter.hasNext(); ) {
                Map.Entry<Server, Map<String, List<Integer>>> serverMapEntry = serverIter.next();
                Map<String, List<Integer>> accountFolders = serverMapEntry.getValue();
                // for each account
                for (Iterator<Map.Entry<String, List<Integer>>> acctIter = accountFolders.entrySet().iterator();
                     acctIter.hasNext(); ) {
                    Map.Entry<String, List<Integer>> acctEntry = acctIter.next();
                    String acctId = acctEntry.getKey();
                    List<Integer> folderIds = acctEntry.getValue();
                    ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), acctId, false);
                    // for each folder
                    for (Iterator<Integer> iterFolderId = folderIds.iterator(); iterFolderId.hasNext(); ) {
                        int folderId = iterFolderId.next();
                        try {
                            CalendarDataResult result = calCache.getCalendarSummary(octxt, acctId, folderId, itemType, rangeStart, rangeEnd, true);
                            if (result != null) {
                                // Found data in cache.
                                iterFolderId.remove();
                                addCalendarDataToResponse(parent, octxt, zsc, authAcct, ifmt, result.data, result.allowPrivateAccess);
                            }
                        } catch (ServiceException e) {
                            String ecode = e.getCode();
                            if (ecode.equals(ServiceException.PERM_DENIED)) {
                                // share permission was revoked
                                ZimbraLog.calendar.warn(
                                        "Ignoring permission error during calendar search of folder " + ifmt.formatItemId(folderId), e);
                            } else if (ecode.equals(MailServiceException.NO_SUCH_FOLDER)) {
                                // shared calendar folder was deleted by the owner
                                ZimbraLog.calendar.warn(
                                        "Ignoring deleted calendar folder " + ifmt.formatItemId(folderId));
                            } else {
                                throw e;
                            }
                            iterFolderId.remove();
                        }
                    }
                    if (folderIds.isEmpty())
                        acctIter.remove();
                }
                if (accountFolders.isEmpty())
                    serverIter.remove();
            }
        }

        // For any remaining calendars, we have to get the data the hard way.
        for (Map.Entry<Server, Map<String, List<Integer>>> serverMapEntry : groupedByServer.entrySet()) {
            Server server = serverMapEntry.getKey();
            Map<String, List<Integer>> accountFolders = serverMapEntry.getValue();
            if (server.equals(localServer)) {  // local server
                for (Map.Entry<String, List<Integer>> entry : accountFolders.entrySet()) {
                    String acctId = entry.getKey();
                    List<Integer> folderIds = entry.getValue();
                    if (folderIds.isEmpty())
                        continue;
                    Account targetAcct = prov.get(AccountBy.id, acctId);
                    if (targetAcct == null) {
                        ZimbraLog.calendar.warn("Skipping unknown account " + acctId + " during calendar search");
                        continue;
                    }
                    Mailbox targetMbox = mboxMgr.getMailboxByAccount(targetAcct);
                    searchLocalAccountCalendars(parent, params, octxt, zsc, authAcct, targetMbox, folderIds, itemType);
                }
            } else {  // remote server
                searchRemoteAccountCalendars(parent, params, zsc, authAcct, accountFolders);
            }
        }
    }

    private static void addCalendarDataToResponse(Element parent, OperationContext octxt, ZimbraSoapContext zsc,
                                                  Account authAcct, ItemIdFormatter ifmt,
                                                  CalendarData calData, boolean allowPrivateAccess)
    throws ServiceException {
        for (Iterator<CalendarItemData> itemIter = calData.calendarItemIterator(); itemIter.hasNext(); ) {
            CalendarItemData calItemData = itemIter.next();
            int numInstances = calItemData.getNumInstances();
            if (numInstances > 0) {
                Element calItemElem = CacheToXML.encodeCalendarItemData(
                        zsc, ifmt, calItemData, allowPrivateAccess, false);
                parent.addElement(calItemElem);
            }
        }
    }

    private static void searchLocalAccountCalendars(
            Element parent, SearchParams params, OperationContext octxt, ZimbraSoapContext zsc,
            Account authAcct, Mailbox targetMbox, List<Integer> folderIds, byte itemType)
    throws ServiceException {
        ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), targetMbox.getAccountId(), false);
        long rangeStart = params.getCalItemExpandStart();
        long rangeEnd = params.getCalItemExpandEnd();
        for (int folderId : folderIds) {
            try {
                CalendarDataResult result = targetMbox.getCalendarSummaryForRange(octxt, folderId, itemType, rangeStart, rangeEnd);
                if (result != null)
                    addCalendarDataToResponse(parent, octxt, zsc, authAcct, ifmt, result.data, result.allowPrivateAccess);
            } catch (ServiceException e) {
                String ecode = e.getCode();
                if (ecode.equals(ServiceException.PERM_DENIED)) {
                    // share permission was revoked
                    ZimbraLog.calendar.warn(
                            "Ignoring permission error during calendar search of folder " + ifmt.formatItemId(folderId), e);
                } else if (ecode.equals(MailServiceException.NO_SUCH_FOLDER)) {
                    // shared calendar folder was deleted by the owner
                    ZimbraLog.calendar.warn(
                            "Ignoring deleted calendar folder " + ifmt.formatItemId(folderId));
                } else {
                    throw e;
                }
            }
        }
    }

    private static void searchRemoteAccountCalendars(
            Element parent, SearchParams params, ZimbraSoapContext zsc,
            Account authAcct, Map<String, List<Integer>> accountFolders)
    throws ServiceException {
        String nominalTargetAcctId = null;  // mail service soap requests want to see a target account
        StringBuilder queryStr = new StringBuilder();
        for (Map.Entry<String, List<Integer>> entry : accountFolders.entrySet()) {
            String acctId = entry.getKey();
            if (nominalTargetAcctId == null)
                nominalTargetAcctId = acctId;
            ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), acctId, false);
            List<Integer> folderIds = entry.getValue();
            for (int folderId : folderIds) {
                if (queryStr.length() > 0)
                    queryStr.append(" OR ");
                // must quote the qualified folder id
                queryStr.append("inid:\"").append(ifmt.formatItemId(folderId)).append("\"");
            }
        }
        Element req = new JSONElement(MailConstants.SEARCH_REQUEST);
        req.addAttribute(MailConstants.A_SEARCH_TYPES, params.getTypesStr());
        req.addAttribute(MailConstants.A_SORTBY, params.getSortByStr());
        req.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        if (params.getLimit() != 0)
            req.addAttribute(MailConstants.A_QUERY_LIMIT, params.getLimit());
        req.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, params.getCalItemExpandStart());
        req.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, params.getCalItemExpandEnd());
        req.addAttribute(MailConstants.E_QUERY, queryStr.toString(), Element.Disposition.CONTENT);

        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, nominalTargetAcctId);
        String pxyAuthToken = zsc.getAuthToken().getProxyAuthToken();
        ZAuthToken zat = pxyAuthToken == null ? zsc.getRawAuthToken() : new ZAuthToken(pxyAuthToken);
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setTargetAccount(nominalTargetAcctId);
        zoptions.setTargetAccountBy(AccountBy.id);
        zoptions.setNoSession(true);
        zoptions.setRequestProtocol(SoapProtocol.SoapJS);
        zoptions.setResponseProtocol(SoapProtocol.SoapJS);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);

        Element resp = zmbx.invoke(req);
        for (Element hit : resp.listElements()) {
            hit.detach();
            parent.addElement(hit);
        }
    }

    static Map<Server, Map<String /* account id */, List<Integer> /* folder ids */>> groupByServer(
            Map<String /* account id */, List<Integer> /* folder ids */> acctFolders)
    throws ServiceException {
        Map<Server, Map<String /* account id */, List<Integer> /* folder ids */>> groupedByServer =
            new HashMap<Server, Map<String, List<Integer>>>();
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<String, List<Integer>> entry : acctFolders.entrySet()) {
            String acctId = entry.getKey();
            List<Integer> folderIds = entry.getValue();
            Account acct = prov.get(AccountBy.id, acctId);
            if (acct == null) {
                ZimbraLog.calendar.warn("Skipping unknown account " + acctId + " during calendar search");
                continue;
            }
            Server server = prov.getServer(acct);
            if (server == null) {
                ZimbraLog.calendar.warn("Skipping account " + acctId + " during calendar search because its home server is unknown");
                continue;
            }
            Map<String, List<Integer>> map = groupedByServer.get(server);
            if (map == null) {
                map = new HashMap<String, List<Integer>>();
                groupedByServer.put(server, map);
            }
            map.put(acctId, folderIds);
        }
        return groupedByServer;
    }
}
