/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.QueryInfo;
import com.zimbra.cs.index.ResultsPager;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.ContactMemberOfMap;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.cache.CacheToXML;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SearchRequest;

/**
 * @since May 26, 2004
 */
public class Search extends MailDocumentHandler  {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account account = getRequestedAccount(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        fixBooleanRecipients(request);
        SearchRequest req = zsc.elementToJaxb(request);
        
        // ZBUG-1765 ... task selection time date validation is not required
        if ( req.getSearchTypes().equalsIgnoreCase("task") && req.getCursor() != null) {
        	req.setCursor(null);
        }
        
        
        if (MoreObjects.firstNonNull(req.getWarmup(), false)) {
            mbox.index.getIndexStore().warmup();
            return zsc.createElement(MailConstants.SEARCH_RESPONSE);
        }

        SearchParams params = SearchParams.parse(req, zsc, account.getPrefMailInitialSearch());
        if (params.getLocale() == null) {
            params.setLocale(mbox.getAccount().getLocale());
        }
        if (params.inDumpster() && params.getTypes().contains(MailItem.Type.CONVERSATION)) {
            throw ServiceException.INVALID_REQUEST("cannot search for conversations in dumpster", null);
        }

        if (LC.calendar_cache_enabled.booleanValue()) {
            List<String> apptFolderIds = getFolderIdListIfSimpleAppointmentsQuery(params, zsc);
            if (apptFolderIds != null) {
                Account authAcct = getAuthenticatedAccount(zsc);
                Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);
                runSimpleAppointmentQuery(response, params, octxt, zsc, authAcct, mbox, apptFolderIds);
                return response;
            }
        }
        Map<String,Set<String>> memberOfMap = null;
        if (req.getIncludeMemberOf()) {
            memberOfMap = ContactMemberOfMap.getMemberOfMap(mbox, octxt);
        }

        // create the XML response Element
        Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);
        try (ZimbraQueryResults results = mbox.index.search(zsc.getResponseProtocol(), octxt,
            params)) {
            // must use results.getSortBy() because the results might have ignored our sortBy
            // request and used something else...
            response.addAttribute(MailConstants.A_SORTBY, results.getSortBy().toString());
            putHits(zsc, octxt, response, results, params, memberOfMap);
        } catch (IOException e) {
        } 
        return response;
    }

    protected static void putInfo(Element response, ZimbraQueryResults results) {
        List<QueryInfo> qinfo = results.getResultInfo();
        if (qinfo.size() > 0) {
            Element qinfoElt = response.addNonUniqueElement(MailConstants.E_INFO);
            for (QueryInfo inf : qinfo) {
                inf.toXml(qinfoElt);
            }
        }
    }

    private void putHits(ZimbraSoapContext zsc, OperationContext octxt, Element el, ZimbraQueryResults results,
            SearchParams params, Map<String,Set<String>> memberOfMap) throws ServiceException {

        if (params.getInlineRule() == ExpandResults.HITS ||
            params.getInlineRule() == ExpandResults.FIRST_MSG ||
            params.getInlineRule() == ExpandResults.HITS_OR_FIRST_MSG ||
            params.getInlineRule() == ExpandResults.UNREAD ||
            params.getInlineRule() == ExpandResults.UNREAD_FIRST ||
            params.getInlineRule() == ExpandResults.U_OR_FIRST_MSG ||
            params.getInlineRule() == ExpandResults.U1_OR_FIRST_MSG) {
            // these are not valid values for Search (according to soap.txt)
            params.setInlineRule(ExpandResults.NONE);
        }

        ResultsPager pager = ResultsPager.create(results, params);
        if (params.getCursor() != null) {
            if (params.getCursor().isIncludeOffset()) {
                long offset = pager.getCursorOffset();
                if (offset >= 0) {
                    el.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
                }
            }
        } else {
            el.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        }

        SearchResponse resp = new SearchResponse(zsc, octxt, el, params, memberOfMap);
        resp.setIncludeMailbox(false);
        resp.setSortOrder(pager.getSortOrder());
        boolean expand;
        ExpandResults expandValue = params.getInlineRule();
        int hitNum = 0;
        while (pager.hasNext() && resp.size() < params.getLimit()) {
            hitNum ++;
            ZimbraHit hit = pager.getNextHit();
            if (hit instanceof MessageHit) {
                /*
                 * Determine whether or not to expand MessageHits.
                 * This logic used to be in SearchResponse.isInlineExpand, but was moved
                 * to the handler classes because in some cases
                 * the decision to expand any particular hit is dependent on
                 * other hits (see SearchConv)
                 */
                if (expandValue == ExpandResults.NONE) {
                    expand = false;
                } else if (expandValue == ExpandResults.ALL) {
                    expand = true;
                } else if (expandValue == ExpandResults.FIRST) {
                    expand = params.getOffset() > 0 ? false : hitNum == 1;
                } else {
                    expand = expandValue.matches(hit.getParsedItemID());
                }
                resp.add(hit, expand);
            } else {
                resp.add(hit);
            }
        }
        resp.addHasMore(pager.hasNext());
        resp.add(results.getResultInfo());
    }
    // Calendar summary cache stuff

    /**
     * Returns list of folder id string if the query is a simple appointments query. Otherwise returns null.
     *
     * @param params search parameters
     * @param zsc not used, may be used in subclass
     * @throws ServiceException subclass may throw
     */
    protected List<String> getFolderIdListIfSimpleAppointmentsQuery(SearchParams params, ZimbraSoapContext zsc)
            throws ServiceException {
        // types = "appointment"
        Set<MailItem.Type> types = params.getTypes();
        if (types.size() != 1) {
            return null;
        }
        MailItem.Type type = Iterables.getOnlyElement(types);
        if (type != MailItem.Type.APPOINTMENT && type != MailItem.Type.TASK) {
            return null;
        }
        // has time range
        if (params.getCalItemExpandStart() == -1 || params.getCalItemExpandEnd() == -1) {
            return null;
        }
        // offset = 0
        if (params.getOffset() != 0) {
            return null;
        }
        // sortBy = "none"
        SortBy sortBy = params.getSortBy();
        if (sortBy != null && !sortBy.equals(SortBy.NONE)) {
            return null;
        }
        // query string is "inid:<folder> [OR inid:<folder>]*"
        String queryString = Strings.nullToEmpty(params.getQueryString());
        queryString = queryString.toLowerCase();
        queryString = removeOuterParens(queryString);
        // simple appointment query can't have any ANDed terms
        if (queryString.contains("and")) {
            return null;
        }
        String[] terms = queryString.split("\\s+or\\s+");
        List<String> folderIdStrs = new ArrayList<String>();
        for (String term : terms) {
            term = term.trim();
            // remove outermost parentheses (light client does this, e.g. "(inid:10)")
            term = removeOuterParens(term);
            if (!term.startsWith("inid:")) {
                return null;
            }
            String folderId = term.substring(5);  // everything after "inid:"
            folderId = unquote(folderId);  // e.g. if query is: inid:"account-id:num", we want just account-id:num
            if (folderId.length() > 0) {
                folderIdStrs.add(folderId);
            }
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

    private static void runSimpleAppointmentQuery(Element parent, SearchParams params, OperationContext octxt,
            ZimbraSoapContext zsc, Account authAcct, Mailbox mbox, List<String> folderIdStrs) throws ServiceException {
        Set<MailItem.Type> types = params.getTypes();
        MailItem.Type type = types.size() == 1 ? Iterables.getOnlyElement(types) : MailItem.Type.APPOINTMENT;

        if (params.getSortBy() != null) {
            parent.addAttribute(MailConstants.A_SORTBY, params.getSortBy().toString());
        }
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
                    // Setup ItemIdFormatter appropriate for this folder which might not be in the authed account
                    // but also take note of presense of <noqualify/> in SOAP context
                    ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), acctId, zsc.wantsUnqualifiedIds());
                    // for each folder
                    for (Iterator<Integer> iterFolderId = folderIds.iterator(); iterFolderId.hasNext(); ) {
                        int folderId = iterFolderId.next();
                        try {
                            CalendarDataResult result = calCache.getCalendarSummary(octxt, acctId, folderId, type,
                                    rangeStart, rangeEnd, true);
                            if (result != null) {
                                // Found data in cache.
                                iterFolderId.remove();
                                addCalendarDataToResponse(parent, zsc, ifmt, result.data, result.allowPrivateAccess);
                            }
                        } catch (ServiceException e) {
                            String ecode = e.getCode();
                            if (ecode.equals(ServiceException.PERM_DENIED)) {
                                // share permission was revoked
                                ZimbraLog.calendar.warn("Ignoring permission error during calendar search of folder %s",
                                        ifmt.formatItemId(folderId), e);
                            } else if (ecode.equals(MailServiceException.NO_SUCH_FOLDER)) {
                                // shared calendar folder was deleted by the owner
                                ZimbraLog.calendar.warn("Ignoring deleted calendar folder %s",
                                        ifmt.formatItemId(folderId));
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
                    searchLocalAccountCalendars(parent, params, octxt, zsc, authAcct, targetMbox, folderIds, type);
                }
            } else {  // remote server
                searchRemoteAccountCalendars(parent, params, zsc, authAcct, accountFolders);
            }
        }
    }

    private static void addCalendarDataToResponse(Element parent, ZimbraSoapContext zsc, ItemIdFormatter ifmt,
            CalendarData calData, boolean allowPrivateAccess) throws ServiceException {
        for (Iterator<CalendarItemData> itemIter = calData.calendarItemIterator(); itemIter.hasNext(); ) {
            CalendarItemData calItemData = itemIter.next();
            int numInstances = calItemData.getNumInstances();
            if (numInstances > 0) {
                Element calItemElem = CacheToXML.encodeCalendarItemData(
                        zsc, ifmt, calItemData, allowPrivateAccess, false);
                parent.addNonUniqueElement(calItemElem);
            }
        }
    }

    private static void searchLocalAccountCalendars(Element parent, SearchParams params, OperationContext octxt,
            ZimbraSoapContext zsc, Account authAcct, Mailbox targetMbox, List<Integer> folderIds, MailItem.Type type)
            throws ServiceException {
        ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), targetMbox.getAccountId(), false);
        long rangeStart = params.getCalItemExpandStart();
        long rangeEnd = params.getCalItemExpandEnd();
        for (int folderId : folderIds) {
            try {
                CalendarDataResult result = targetMbox.getCalendarSummaryForRange(octxt, folderId, type,
                        rangeStart, rangeEnd);
                if (result != null) {
                    addCalendarDataToResponse(parent, zsc, ifmt, result.data, result.allowPrivateAccess);
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
        Element req = zsc.createElement(MailConstants.SEARCH_REQUEST);
        req.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.toString(params.getTypes()));
        if (params.getSortBy() != null) {
            req.addAttribute(MailConstants.A_SORTBY, params.getSortBy().toString());
        }
        req.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        if (params.getLimit() != 0)
            req.addAttribute(MailConstants.A_QUERY_LIMIT, params.getLimit());
        req.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, params.getCalItemExpandStart());
        req.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, params.getCalItemExpandEnd());
        req.addAttribute(MailConstants.E_QUERY, queryStr.toString(), Element.Disposition.CONTENT);

        Account target = Provisioning.getInstance().get(Key.AccountBy.id, nominalTargetAcctId);
        AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(zsc.getAuthToken());
        String pxyAuthToken = authToken.getProxyAuthToken();
        ZAuthToken zat = pxyAuthToken == null ? authToken.toZAuthToken() : new ZAuthToken(pxyAuthToken);
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setTargetAccount(nominalTargetAcctId);
        zoptions.setTargetAccountBy(AccountBy.id);
        zoptions.setNoSession(true);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        zmbx.setName(target.getName()); /* need this when logging in using another user's auth */

        Element resp = zmbx.invoke(req);
        for (Element hit : resp.listElements()) {
            hit.detach();
            parent.addNonUniqueElement(hit);
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

    private static void fixBooleanRecipients(Element request) {
        String recipField = MailConstants.A_RECIPIENTS;
        String recip;
        try {
            recip = request.getAttribute(recipField);
        }  catch (ServiceException e) {
            // request doesn't have a "recip" field
            return;
        }
        if (recip.equals("true")) {
            request.addAttribute(recipField, "1");
        } else if (recip.equals("false")) {
            request.addAttribute(recipField, "0");
        }
    }
}

