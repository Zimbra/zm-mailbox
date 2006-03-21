/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.*;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;


/**
 * @author schemers
 */
public class Search extends DocumentHandler  {
    protected static Log mLog = LogFactory.getLog(Search.class);
    
    protected static final boolean DONT_CACHE_RESULTS = true;
    
    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        SoapSession session = (SoapSession) lc.getSession(SessionCache.SESSION_SOAP);
        Mailbox mbox = getRequestedMailbox(lc);
        
        SearchParams params = parseCommonParameters(request, lc);
        ZimbraQueryResults results = getResults(mbox, params, lc, session);
        
        Element response = lc.createElement(MailService.SEARCH_RESPONSE);
        
        // must use results.getSortBy() because the results might have ignored our sortBy
        // request and used something else...
        SortBy sb = results.getSortBy();
        response.addAttribute(MailService.A_SORTBY, sb.getName());
        
        //
        // create a "pager" which generate one page's worth of data for the client (using
        // the cursor data, etc)
        //
        // If the pager detects "new results at head" -- ie new data came into our search, then
        // we'll skip back to the beginning of the search
        //
        ResultsPager pager;            
        try {
            pager = ResultsPager.create(results, params);
            response.addAttribute(MailService.A_QUERY_OFFSET, params.getOffset());
        } catch (ResultsPager.NewResultsAtHeadException e) {
            // NOTE: this branch is unused right now, TODO remove this depending on usability
            // decisions
            
            // FIXME!  workaround bug in resetIterator()
            results = getResults(mbox, params, lc, session);

            pager = new ResultsPager(results, sb, params.getLimit(), 0);
            response.addAttribute(MailService.A_QUERY_OFFSET, 0);
            response.addAttribute("newResults", true);
        }
        
        Element retVal = putHits(lc, response, pager, DONT_INCLUDE_MAILBOX_INFO, params);
        if (DONT_CACHE_RESULTS)
            results.doneWithSearchResults();
        
        return retVal;
    }
    
    protected SearchParams parseCommonParameters(Element request, ZimbraContext lc) throws ServiceException {
        String query = request.getAttribute(MailService.E_QUERY, null);
        if (query == null)
            query = getRequestedAccount(lc).getAttr(Provisioning.A_zimbraPrefMailInitialSearch);
        if (query == null)
            throw ServiceException.INVALID_REQUEST("no query submitted and no default query found", null);

        SearchParams params = new SearchParams();
        params.setOffset(getOffset(request));
        params.setLimit(getLimit(request));
        params.setQueryStr(query);
        String groupByStr = request.getAttribute(MailService.A_SEARCH_TYPES, null);
        if (groupByStr == null)
            groupByStr = request.getAttribute(MailService.A_GROUPBY, MailboxIndex.GROUP_BY_CONVERSATION);
        params.setTypesStr(groupByStr);
        params.setSortByStr(request.getAttribute(MailService.A_SORTBY, MailboxIndex.SortBy.DATE_DESCENDING.getName()));
        params.setFetchFirst(request.getAttributeBool(MailService.A_FETCH, false));
        if (params.getFetchFirst()) {
            params.setWantHtml(request.getAttributeBool(MailService.A_WANT_HTML, false));
            params.setMarkRead(request.getAttributeBool(MailService.A_MARK_READ, false));
        }
        params.setWantRecipients(request.getAttributeBool(MailService.A_RECIPIENTS, false));
        
        Element cursor = request.getOptionalElement("cursor");
        if (cursor != null) {
            int prevMailItemId = (int)cursor.getAttributeLong(MailService.A_ID);
//            int prevOffset = (int)cursor.getAttributeLong(MailService.A_QUERY_OFFSET);
            int prevOffset = 0;
            String sortVal = cursor.getAttribute("sortVal");
            params.setCursor(prevMailItemId, sortVal, prevOffset);
        }
        
        return params;
    }
    
    protected Mailbox getMailboxFromParameter(String id) throws ServiceException {
        Mailbox mbx = null;
        if (id.indexOf('@') >= 0) {
            // account
            Account acct = Provisioning.getInstance().getAccountByName(id);
            mbx = Mailbox.getMailboxByAccountId(acct.getId());
        } else {
            if (id.indexOf('-') >= 0) {
                mbx = Mailbox.getMailboxByAccountId(id);
            } else {
                int mailboxId = Integer.parseInt(id);
                mbx = Mailbox.getMailboxById(mailboxId);
            }
        }
        return mbx;
        
    }
    
    protected int getLimit(Element request) throws ServiceException {
        int limit = (int) request.getAttributeLong(MailService.A_QUERY_LIMIT, -1);
        if (limit <= 0 || limit > 1000)
            limit = 30; // default limit of...say..30...
        return limit;
    }
    
    protected int getOffset(Element request) throws ServiceException {
        // Lookup the offset= and limit= parameters in the soap request
        return (int) request.getAttributeLong(MailService.A_QUERY_OFFSET, 0);
    }

    // note that session may be null
    protected ZimbraQueryResults getResults(Mailbox mbox, SearchParams params, ZimbraContext lc, SoapSession session)
    throws ServiceException {
        ZimbraQueryResults results = null;
        boolean cacheResults = !DONT_CACHE_RESULTS && session != null;
        if (cacheResults) {
            session.getQueryResults(params.getQueryStr(), params.getTypesStr(), params.getSortByStr());
        }

        if (params.getOffset() > 0) {
            if (results == null) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Could not find cached results, re-running query.");
                    mLog.debug("Paging in cached result set for query="+params.getQueryStr());
                }
            } else {
            }
        } else {
            if (session != null) {
                session.clearCachedQueryResults();
            }
            results = null;
            if (mLog.isDebugEnabled()) {
                mLog.debug("Re-running query because offset at 0");
            }
        }
        
        if (mLog.isDebugEnabled()) {
            mLog.debug("Requesting results offset="+params.getOffset()+" limit="+params.getLimit());
        }
        
        if (results == null) {
            try {
                byte[] types = MailboxIndex.parseGroupByString(params.getTypesStr());
                
                results = mbox.search(lc.getOperationContext(), params.getQueryStr(), types, params.getSortBy(), params.getLimit() + params.getOffset());
                if (cacheResults) {
                    session.putQueryResults(params.getQueryStr(), params.getTypesStr(), params.getSortByStr(), results);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                throw ServiceException.FAILURE("IO error", e);
            } catch (ParseException e) {
                if (e.currentToken != null)
                    throw MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(), e, e.currentToken.image, e.currentToken.beginLine, e.currentToken.beginColumn);
                else 
                    throw MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(), e, "", -1, -1);
            }
        }
        return results;
    }
    
    protected final boolean INCLUDE_MAILBOX_INFO = true;
    protected final boolean DONT_INCLUDE_MAILBOX_INFO = false;
    
    protected Element putHits(ZimbraContext lc, Element response, ResultsPager pager, boolean includeMailbox, SearchParams params)
    throws ServiceException {
        int offset = params.getOffset();
        int limit  = params.getLimit();
        EmailElementCache eecache = new EmailElementCache();
            
        if (mLog.isDebugEnabled())
            mLog.debug("Search results beginning with offset "+offset);
        
        int totalNumHits = 0;
        
        for (Iterator iter = pager.getHits().iterator(); iter.hasNext();) {
            ZimbraHit hit = (ZimbraHit)iter.next();
            
//          for (ZimbraHit hit = results.skipToHit(offset); hit != null; hit = results.getNext()) {
            totalNumHits++;
            boolean inline = (totalNumHits == 1 && params.getFetchFirst());
            boolean addSortField = true;
            Element e = null;
            if (hit instanceof ConversationHit) {
                ConversationHit ch = (ConversationHit) hit;
                e = addConversationHit(lc, response, ch, eecache, params);
            } else if (hit instanceof MessageHit) {
                MessageHit mh = (MessageHit) hit;
                e = addMessageHit(lc, response, mh, eecache, inline, params);
            } else if (hit instanceof MessagePartHit) {
                MessagePartHit mph = (MessagePartHit) hit;
                e = addMessagePartHit(response, mph, eecache);                
            } else if (hit instanceof ContactHit) {
                ContactHit ch = (ContactHit) hit;
                e = ToXML.encodeContact(response, lc, ch.getContact(), null, true, null);
            } else if (hit instanceof NoteHit) {
                NoteHit nh = (NoteHit) hit;
                e = ToXML.encodeNote(response,lc, nh.getNote());
            } else if (hit instanceof ProxiedHit) {
                ProxiedHit ph = (ProxiedHit) hit;
                e = ph.getElement().detach();
                response.addElement(e);
                addSortField = false;
            } else if (hit instanceof AppointmentHit) {
                AppointmentHit ah = (AppointmentHit)hit;
                e = addAppointmentHit(lc, response, ah, inline, params);
            } else if (hit instanceof DocumentHit) {
            	DocumentHit dh = (DocumentHit)hit;
                e = addDocumentHit(lc, response, dh);
            } else {
                mLog.error("Got an unknown hit type putting search hits: "+hit);
            }
            if (e != null && addSortField) {
            	e.addAttribute(MailService.A_SORT_FIELD, hit.getSortField(pager.getSortOrder()).toString());
            }
            if (includeMailbox) {
//                String idStr = hit.getMailboxIdStr() + "/" + hit.getItemId();
            	ItemId iid = new ItemId(hit.getAcctIdStr(), hit.getItemId());
                e.addAttribute(MailService.A_ID, iid.toString());
            }
            if (totalNumHits >= limit) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Search results limited to " + limit + " hits.");
                }
                break;
            }
        }

        response.addAttribute(MailService.A_QUERY_MORE, pager.hasNext());
        
        return response;
    }
    
    
    protected Element addConversationHit(ZimbraContext lc, Element response, ConversationHit ch, EmailElementCache eecache, SearchParams params)
    throws ServiceException {
        Conversation conv = ch.getConversation();
        MessageHit mh = ch.getFirstMessageHit();
        Element c = ToXML.encodeConversationSummary(response, lc, conv, mh == null ? null : mh.getMessage(), eecache, params.getWantRecipients());
        if (ch.getScore() != 0)
            c.addAttribute(MailService.A_SCORE, ch.getScore());

        Collection s = ch.getMessageHits();
        if (s != null) {
            for (Iterator mit = s.iterator(); mit.hasNext(); ) {
                mh = (MessageHit) mit.next();
                Message msg = mh.getMessage();
                Element e = c.addElement(MailService.E_MSG);
                e.addAttribute(MailService.A_ID, msg.getId());
            }
        }
        return c;
    }

    protected Element addAppointmentHit(ZimbraContext lc, Element response, AppointmentHit ah, boolean inline, SearchParams params)
    throws ServiceException {
        Appointment appt = ah.getAppointment();
        Element m;
        if (inline) {
            m = ToXML.encodeApptSummary(response, lc, appt, PendingModifications.Change.ALL_FIELDS);
//            if (!msg.getFragment().equals(""))
//                m.addAttribute(MailService.E_FRAG, msg.getFragment(), Element.DISP_CONTENT);
        } else {
            m = ToXML.encodeApptSummary(response, lc, appt, PendingModifications.Change.ALL_FIELDS);
        }
        
        if (ah.getScore() != 0) {
            m.addAttribute(MailService.A_SCORE, ah.getScore());
        }
        
        m.addAttribute(MailService.A_CONTENTMATCHED, true);

        return m;
    }
    
    
    protected Element addMessageHit(ZimbraContext lc, Element response, MessageHit mh, EmailElementCache eecache, boolean inline, SearchParams params)
    throws ServiceException {
        Message msg = mh.getMessage();
        Element m;
        if (inline) {
            m = ToXML.encodeMessageAsMP(response, lc, msg, params.getWantHtml(), null);
            if (!msg.getFragment().equals(""))
                m.addAttribute(MailService.E_FRAG, msg.getFragment(), Element.DISP_CONTENT);
        } else
            m = ToXML.encodeMessageSummary(response, lc, msg, params.getWantRecipients());
        if (mh.getScore() != 0)
            m.addAttribute(MailService.A_SCORE, mh.getScore());
        
        m.addAttribute(MailService.A_CONTENTMATCHED, true);
        
        ArrayList parts = mh.getMatchedMimePartNames();
        if (parts != null) {
            for (Iterator mpit = parts.iterator(); mpit.hasNext(); ) {
                MessagePartHit mph = (MessagePartHit) mpit.next();
                String partNameStr = mph.getPartName();
                
                if (partNameStr.length() > 0) {
                    Element mp = m.addElement(MailService.E_HIT_MIMEPART);
                    mp.addAttribute(MailService.A_PART, partNameStr);
                }
            }
        }

        if (inline && msg.isUnread() && params.getMarkRead())
            try {
                Mailbox mbox = msg.getMailbox();
                mbox.alterTag(lc.getOperationContext(), msg.getId(), msg.getType(), Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException e) {
                mLog.warn("problem marking message as read (ignored): " + msg.getId(), e);
            }

        return m;
    }
    
    protected Element addMessageHit(ZimbraContext lc, Element response, Message msg, EmailElementCache eecache, SearchParams params) {
        Element m = ToXML.encodeMessageSummary(response, lc, msg, params.getWantRecipients());
        return m;
    }
    
    protected Element addMessagePartHit(Element response, MessagePartHit mph, EmailElementCache eecache) throws ServiceException {
        MessageHit mh = mph.getMessageResult();
        Message msg = mh.getMessage();
        Element mp = response.addElement(MailService.E_MIMEPART);
        
        mp.addAttribute(MailService.A_SIZE, msg.getSize());
        mp.addAttribute(MailService.A_DATE, msg.getDate());
        mp.addAttribute(MailService.A_CONV_ID, msg.getConversationId());
        mp.addAttribute(MailService.A_MESSAGE_ID, msg.getId());
        mp.addAttribute(MailService.A_CONTENT_TYPE, mph.getType());
        mp.addAttribute(MailService.A_CONTENT_NAME, mph.getFilename());
        mp.addAttribute(MailService.A_PART, mph.getPartName());        
        if (mph.getScore() != 0)
            mp.addAttribute(MailService.A_SCORE, mph.getScore());
        
        eecache.makeEmail(mp, msg.getSender(), EmailElementCache.EMAIL_TYPE_FROM, null);
        String subject = mph.getSubject();
        if (subject != null)
            mp.addAttribute(MailService.E_SUBJECT, subject, Element.DISP_CONTENT);
        
        return mp;
    }
    
    Element addContactHit(ZimbraContext lc, Element response, ContactHit ch, EmailElementCache eecache) throws ServiceException {
        return ToXML.encodeContact(response, lc, ch.getContact(), null, true, null);
    }
    
    Element addDocumentHit(ZimbraContext lc, Element response, DocumentHit dh) throws ServiceException {
    	if (dh.getItemType() == MailItem.TYPE_DOCUMENT)
    		return ToXML.encodeDocument(response, lc, (Document)dh.getDocument(), -1);
    	else if (dh.getItemType() == MailItem.TYPE_WIKI)
            return ToXML.encodeWiki(response, lc, (WikiItem)dh.getDocument(), -1);
    	throw ServiceException.UNKNOWN_DOCUMENT("invalid document type "+dh.getItemType(), null);
    }
    
    Element addNoteHit(ZimbraContext lc, Element response, NoteHit nh, EmailElementCache eecache) throws ServiceException {
        // TODO - does this need to be a summary, instead of the whole note?
        return ToXML.encodeNote(response, lc, nh.getNote());
    }
}
