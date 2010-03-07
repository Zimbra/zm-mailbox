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

/*
 * Created on May 26, 2004
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
import com.zimbra.cs.index.*;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.mailbox.calendar.cache.CacheToXML;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.service.mail.GetCalendarItemSummaries.EncodeCalendarItemResult;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.ZimbraSoapContext;

public class Search extends MailDocumentHandler  {
    protected static Log mLog = LogFactory.getLog(Search.class);

    public static final String DEFAULT_SEARCH_TYPES = MailboxIndex.GROUP_BY_CONVERSATION;

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
        
        
//        params.setMode(SearchResultMode.IDS); // HACK HACK TESTING DO NOT CHECK IN!

        ZimbraQueryResults results = null;
        try {
            results = doSearch(zsc, octxt, mbox, params);
            
            // create the XML response Element
            Element response = zsc.createElement(MailConstants.SEARCH_RESPONSE);

            // must use results.getSortBy() because the results might have ignored our sortBy
            // request and used something else...
            SortBy sb = results.getSortBy();
            response.addAttribute(MailConstants.A_SORTBY, sb.toString());

            ResultsPager pager = ResultsPager.create(results, params);
            response.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());

            putHits(zsc, octxt, response, pager, DONT_INCLUDE_MAILBOX_INFO, params);
            
            // call me AFTER putHits since some of the <info> is generated by the getting of the hits!
            putInfo(response, params, results);
            
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
        } catch (ParseException e) {
            MailServiceException me = null;
            String message = e.getMessage();
            if (e.code != null)
                message = e.code;
            if (e.expectedTokenSequences != null) {
                // this is a direct ParseException from JavaCC - don't return their long message as the code
                message = "PARSER_ERROR";
            }
            if (e.currentToken != null)
                me = MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(), e, e.currentToken.image, e.currentToken.beginColumn, message);
            else 
                me = MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(), e, "", -1, message);
            throw me;
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
            } catch (ServiceException ex) {}
            
            for (QueryInfo inf : qinfo) {
                inf.toXml(qinfoElt);
            }
        }
    }

    // just to make the argument lists a bit more readable:
    protected final boolean INCLUDE_MAILBOX_INFO = true;
    protected final boolean DONT_INCLUDE_MAILBOX_INFO = false;

    protected Element putHits(ZimbraSoapContext zsc, OperationContext octxt, Element response, ResultsPager pager, boolean includeMailbox, SearchParams params)
    throws ServiceException {
        int offset = params.getOffset();
        int limit  = params.getLimit();

        if (mLog.isDebugEnabled())
            mLog.debug("Search results beginning with offset " + offset);

        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        int totalNumHits = 0;
        ExpandResults expand = params.getInlineRule();
        if (expand == ExpandResults.HITS)
            expand = ExpandResults.NONE;      // "hits" is not a valid value for Search...
        
        while (pager.hasNext()) {
            ZimbraHit hit = pager.getNextHit();
            if (totalNumHits >= limit) {
                if (mLog.isDebugEnabled())
                    mLog.debug("Search results limited to " + limit + " hits.");
                break;
            }
            boolean inline = (totalNumHits == 0 && expand == ExpandResults.FIRST) || expand == ExpandResults.ALL || expand.matches(hit.getParsedItemID());
            boolean addSortField = true;
            
            Element e = null;
            if (params.getMode() == SearchResultMode.IDS) {
                if (hit instanceof ConversationHit) {
                    // need to expand the contained messages
                    e = response.addElement("hit");
                    e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(hit.getParsedItemID()));
                } else {
                    e = response.addElement("hit");
                    e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(hit.getParsedItemID()));
                }
            }  else {

            if (hit instanceof ConversationHit) {
                ConversationHit ch = (ConversationHit) hit;
                e = addConversationHit(ch, response, octxt, ifmt, params);
            } else if (hit instanceof MessageHit) {
                MessageHit mh = (MessageHit) hit;
                e = addMessageHit(mh, response, octxt, ifmt, inline, params);
            } else if (hit instanceof MessagePartHit) {
                MessagePartHit mph = (MessagePartHit) hit;
                e = addMessagePartHit(mph, response);                
            } else if (hit instanceof ContactHit) {
                ContactHit ch = (ContactHit) hit;
                e = ToXML.encodeContact(response, ifmt, ch.getContact(), true, null);
            } else if (hit instanceof NoteHit) {
                NoteHit nh = (NoteHit) hit;
                e = ToXML.encodeNote(response, ifmt, nh.getNote());
            } else if (hit instanceof ProxiedHit) {
                ProxiedHit ph = (ProxiedHit) hit;
                response.addElement(ph.getElement().detach());
                addSortField = false;
                totalNumHits++;
            } else if (hit instanceof CalendarItemHit) {
                CalendarItemHit ah = (CalendarItemHit) hit;
                e = addCalendarItemHit(ah, response, zsc, octxt, ifmt, inline, params);
            } else if (hit instanceof DocumentHit) {
                DocumentHit dh = (DocumentHit) hit;
                e = addDocumentHit(dh, response, octxt, ifmt);
            } else {
                mLog.error("Got an unknown hit type putting search hits: "+hit);
                continue;
            }
            
            }

            if (e != null && addSortField) {
                e.addAttribute(MailConstants.A_SORT_FIELD, hit.getSortField(pager.getSortOrder()).toString());
            }
            if (e != null && includeMailbox) {
                ItemId iid = new ItemId(hit.getAcctIdStr(), hit.getItemId());
                e.addAttribute(MailConstants.A_ID, iid.toString());
            }
            if (e != null)
                totalNumHits++;
            
            if (totalNumHits >= limit) {
                if (mLog.isDebugEnabled())
                    mLog.debug("Search results limited to " + limit + " hits.");
                break;
            }
        }

        response.addAttribute(MailConstants.A_QUERY_MORE, pager.hasNext());

        return response;
    }


    protected Element addConversationHit(ConversationHit ch, Element response, OperationContext octxt, ItemIdFormatter ifmt, SearchParams params)
    throws ServiceException {
        if (params.getMode() == SearchResultMode.IDS) {
            Element c = response.addElement(MailConstants.E_CONV);
            for (MessageHit mh : ch.getMessageHits())
                c.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, ifmt.formatItemId(mh.getItemId()));
            return c;
        } else {
            Conversation conv = ch.getConversation();
            MessageHit mh1 = ch.getFirstMessageHit();
            Element c = ToXML.encodeConversationSummary(response, ifmt, octxt, conv, mh1 == null ? null : mh1.getMessage(), params.getWantRecipients());
            if (ch.getScore() != 0)
                c.addAttribute(MailConstants.A_SCORE, ch.getScore());
            
            for (MessageHit mh : ch.getMessageHits())
                c.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, ifmt.formatItemId(mh.getMessage()));
            return c;
        }
    }

    /**
     * @param ah
     * @param response
     * @param zsc
     * @param inline
     * @param params
     * @return
     *           The encoded element OR NULL if the search params contained a calItemExpand range AND the 
     *           calendar item did not have any instances in the specified range
     * @throws ServiceException
     */
    protected Element addCalendarItemHit(CalendarItemHit ah, Element response, ZimbraSoapContext zsc, OperationContext octxt, ItemIdFormatter ifmt, boolean inline, SearchParams params)
    throws ServiceException {
        CalendarItem calItem = ah.getCalendarItem();
        
        Element calElement = null;
        int fields = PendingModifications.Change.ALL_FIELDS;
        
        Account acct = getRequestedAccount(zsc);
        EncodeCalendarItemResult encoded = 
            GetCalendarItemSummaries.encodeCalendarItemInstances(zsc, octxt, calItem, acct, params.getCalItemExpandStart(), params.getCalItemExpandEnd(), true);
        
        calElement = encoded.element;
        
        if (calElement != null) {
            response.addElement(encoded.element);
            ToXML.setCalendarItemFields(encoded.element, ifmt, octxt, calItem, fields, false, params.getNeuterImages());

            calElement.addAttribute(MailConstants.A_CONTENTMATCHED, true);
            if (ah.getScore() != 0)
                calElement.addAttribute(MailConstants.A_SCORE, ah.getScore());
        }
        
        return calElement;
    }


    protected Element addMessageHit(MessageHit mh, Element response, OperationContext octxt, ItemIdFormatter ifmt, boolean inline, SearchParams params)
    throws ServiceException {
        Message msg = mh.getMessage();

        // for bug 7568, mark-as-read must happen before the response is encoded.
        if (inline && msg.isUnread() && params.getMarkRead()) {
            // Mark the message as READ          
            try {
                msg.getMailbox().alterTag(octxt, msg.getId(), msg.getType(), Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException e) {
                if (e.getCode().equals(ServiceException.PERM_DENIED))
                    mLog.info("no permissions to mark message as read (ignored): " + msg.getId());
                else
                    mLog.warn("problem marking message as read (ignored): " + msg.getId(), e);
            }
        }

        Element m;
        if (inline)
            m = ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, params.getMaxInlinedLength(), params.getWantHtml(), params.getNeuterImages(), params.getInlinedHeaders(), true);
        else
            m = ToXML.encodeMessageSummary(response, ifmt, octxt, msg, params.getWantRecipients());

        if (mh.getScore() != 0)
            m.addAttribute(MailConstants.A_SCORE, mh.getScore());

        m.addAttribute(MailConstants.A_CONTENTMATCHED, true);

        List<MessagePartHit> parts = mh.getMatchedMimePartNames();
        if (parts != null) {
            for (MessagePartHit mph : parts) {
                String partNameStr = mph.getPartName();
                if (partNameStr.length() > 0)
                    m.addElement(MailConstants.E_HIT_MIMEPART).addAttribute(MailConstants.A_PART, partNameStr);
            }
        }
        
        return m;
    }

    protected Element addMessageMiss(Message msg, Element response, OperationContext octxt, ItemIdFormatter ifmt, boolean inline, SearchParams params)
    throws ServiceException {
        // for bug 7568, mark-as-read must happen before the response is encoded.
        if (inline && msg.isUnread() && params.getMarkRead()) {
            // Mark the message as READ          
            try {
                msg.getMailbox().alterTag(octxt, msg.getId(), msg.getType(), Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException e) {
                mLog.warn("problem marking message as read (ignored): " + msg.getId(), e);
            }
        }

        Element m;
        if (inline) {
            m = ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, params.getMaxInlinedLength(), params.getWantHtml(), params.getNeuterImages(), null, true);
            if (!msg.getFragment().equals(""))
                m.addAttribute(MailConstants.E_FRAG, msg.getFragment(), Element.Disposition.CONTENT);
        } else {
            m = ToXML.encodeMessageSummary(response, ifmt, octxt, msg, params.getWantRecipients());
        }
        return m;
    }

    protected Element addMessagePartHit(MessagePartHit mph, Element response) throws ServiceException {
        MessageHit mh = mph.getMessageResult();
        Message msg = mh.getMessage();
        Element mp = response.addElement(MailConstants.E_MIMEPART);

        mp.addAttribute(MailConstants.A_SIZE, msg.getSize());
        mp.addAttribute(MailConstants.A_DATE, msg.getDate());
        mp.addAttribute(MailConstants.A_CONV_ID, msg.getConversationId());
        mp.addAttribute(MailConstants.A_MESSAGE_ID, msg.getId());
        mp.addAttribute(MailConstants.A_CONTENT_TYPE, mph.getType());
        mp.addAttribute(MailConstants.A_CONTENT_NAME, mph.getFilename());
        mp.addAttribute(MailConstants.A_PART, mph.getPartName());
        if (mph.getScore() != 0)
            mp.addAttribute(MailConstants.A_SCORE, mph.getScore());

        ToXML.encodeEmail(mp, msg.getSender(), EmailType.FROM);
        String subject = msg.getSubject();
        if (subject != null)
            mp.addAttribute(MailConstants.E_SUBJECT, subject, Element.Disposition.CONTENT);

        return mp;
    }

    Element addContactHit(ItemIdFormatter ifmt, Element response, ContactHit ch) throws ServiceException {
        return ToXML.encodeContact(response, ifmt, ch.getContact(), true, null);
    }

    Element addDocumentHit(DocumentHit dh, Element response, OperationContext octxt, ItemIdFormatter ifmt) throws ServiceException {
        if (dh.getItemType() == MailItem.TYPE_DOCUMENT)
            return ToXML.encodeDocument(response, ifmt, octxt, dh.getDocument());
        else if (dh.getItemType() == MailItem.TYPE_WIKI)
            return ToXML.encodeWiki(response, ifmt, octxt, (WikiItem)dh.getDocument());
        throw ServiceException.UNKNOWN_DOCUMENT("invalid document type "+dh.getItemType(), null);
    }

    Element addNoteHit(ItemIdFormatter ifmt, Element response, NoteHit nh) throws ServiceException {
        // TODO - does this need to be a summary, instead of the whole note?
        return ToXML.encodeNote(response, ifmt, nh.getNote());
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
