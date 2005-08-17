/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.index.ContactHit;
import com.liquidsys.coco.index.ConversationHit;
import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.index.MessageHit;
import com.liquidsys.coco.index.MessagePartHit;
import com.liquidsys.coco.index.NoteHit;
import com.liquidsys.coco.index.ProxiedHit;
import com.liquidsys.coco.index.SearchParams;
import com.liquidsys.coco.index.queryparser.ParseException;
import com.liquidsys.coco.mailbox.Conversation;
import com.liquidsys.coco.mailbox.Flag;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.SoapSession;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;


/**
 * @author schemers
 */
public class Search extends DocumentHandler  {
    protected static Log mLog = LogFactory.getLog(Search.class);
    
    protected static StopWatch sWatch = StopWatch.getInstance("Search");
    
    protected static final boolean DONT_CACHE_RESULTS = true;
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime =  sWatch.start();
        
        try {
            LiquidContext lc = getLiquidContext(context);
            SoapSession session = lc.getSession();
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            SearchParams params = parseCommonParameters(request, lc);
            LiquidQueryResults results = this.getResults(mbox, session, params);

            Element response = lc.createElement(MailService.SEARCH_RESPONSE);
            response.addAttribute(MailService.A_QUERY_OFFSET, params.getOffset());

            Element retVal = putHits(octxt, response, results, DONT_INCLUDE_MAILBOX_INFO, params);
            if (DONT_CACHE_RESULTS)
                results.doneWithSearchResults();
            if (mLog.isDebugEnabled())
                mLog.debug("Search Element Handler Finished in "+(System.currentTimeMillis()-startTime)+"ms");
            return retVal;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    protected SearchParams parseCommonParameters(Element request, LiquidContext lc) throws ServiceException {
        String query = request.getAttribute(MailService.E_QUERY, null);
        if (query == null)
            query = getRequestedAccount(lc).getAttr(Provisioning.A_liquidPrefMailInitialSearch);
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
        params.setSortByStr(request.getAttribute(MailService.A_SORTBY, MailboxIndex.SORT_BY_DATE_DESCENDING));
        params.setFetchFirst(request.getAttributeBool(MailService.A_FETCH, false));
        if (params.getFetchFirst()) {
            params.setWantHtml(request.getAttributeBool(MailService.A_WANT_HTML, false));
            params.setMarkRead(request.getAttributeBool(MailService.A_MARK_READ, false));
        }
        params.setWantRecipients(request.getAttributeBool(MailService.A_RECIPIENTS, false));

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
    
    protected LiquidQueryResults getResults(Mailbox mbox, SoapSession session, SearchParams params) throws ServiceException
    {
        LiquidQueryResults results = null;
        if (!DONT_CACHE_RESULTS)
            session.getQueryResults(params.getQueryStr(), params.getTypesStr(), params.getSortByStr());

        if (params.getOffset() > 0) {
            if (results == null) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Could not find cached results, re-running query.");
                    mLog.debug("Paging in cached result set for query="+params.getQueryStr());
                }
            } else {
            }
        } else {
            session.clearCachedQueryResults();
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
                results = mbox.search(params.getQueryStr(), params.getTypesStr(), params.getSortByStr());
                if (!DONT_CACHE_RESULTS) {
                    session.putQueryResults(params.getQueryStr(), params.getTypesStr(), params.getSortByStr(), results);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                throw ServiceException.FAILURE("IO error", e);
            } catch (ParseException e) {
                throw MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(), e);
            }
        }
        return results;
    }
    
    protected final boolean INCLUDE_MAILBOX_INFO = true;
    protected final boolean DONT_INCLUDE_MAILBOX_INFO = false;
    
    protected Element putHits(OperationContext octxt, Element response, LiquidQueryResults results, boolean includeMailbox, SearchParams params)
    throws ServiceException {
        int offset = params.getOffset();
        int limit  = params.getLimit();
        EmailElementCache eecache = new EmailElementCache();
            
        if (mLog.isDebugEnabled())
            mLog.debug("Search results beginning with offset "+offset);

        int totalNumHits = 0;
        if (null == results) {
            mLog.info("Got NULL search results object.  This is a bug, report to Tim");
        } else {
            for (LiquidHit hit = results.skipToHit(offset); hit != null; hit = results.getNext()) {
                totalNumHits++;
                boolean inline = totalNumHits == 1 && params.getFetchFirst();
                Element e = null;
                if (hit instanceof ConversationHit) {
                    ConversationHit ch = (ConversationHit) hit;
                    e = addConversationHit(response, ch, eecache);
                } else if (hit instanceof MessageHit) {
                    MessageHit mh = (MessageHit) hit;
                    e = addMessageHit(octxt, response, mh, eecache, inline, params);
                } else if (hit instanceof MessagePartHit) {
                    MessagePartHit mph = (MessagePartHit) hit;
                    e = addMessagePartHit(response, mph, eecache);                
                } else if (hit instanceof ContactHit) {
                    ContactHit ch = (ContactHit) hit;
                    ToXML.encodeContact(response, ch.getContact(), null, true, null);
                } else if (hit instanceof NoteHit) {
                    NoteHit nh = (NoteHit) hit;
                    e = ToXML.encodeNote(response,nh.getNote());
                } else if (hit instanceof ProxiedHit) {
                    ProxiedHit ph = (ProxiedHit) hit;
                    e = ph.getElement().detach();
                    response.addElement(e);
                }
                if (includeMailbox) {
                    String idStr = hit.getMailboxIdStr() + "/" + hit.getItemId();
                    e.addAttribute(MailService.A_ID, idStr);
                }
                if (totalNumHits >= limit) {
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Search results limited to " + limit + " hits.");
                    }
                    break;
                }
            }
        }

        response.addAttribute(MailService.A_QUERY_MORE, results.hasNext());
        
        return response;
    }
    
    
    protected Element addConversationHit(Element response, ConversationHit ch, EmailElementCache eecache) throws ServiceException {
        Conversation conv = ch.getConversation();
        Element c = ToXML.encodeConversationSummary(response, conv, ch.getHitDate(), ch.getHitFragment(), eecache);
        if (ch.getScore() != 0)
            c.addAttribute(MailService.A_SCORE, ch.getScore());
        Collection s = ch.getMessageHits();
        if (s != null) {
            for (Iterator mit = s.iterator(); mit.hasNext(); ) {
                MessageHit mh = (MessageHit) mit.next();
                Message msg = mh.getMessage();
                Element e = c.addElement(MailService.E_MSG);
                e.addAttribute(MailService.A_ID, msg.getId());
                ArrayList parts = mh.getMatchedMimePartNames();
                if (parts != null) {
                    for (Iterator mpit = parts.iterator(); mpit.hasNext();) {
                        Element mp = e.addElement(MailService.E_MIMEPART);
                        MessagePartHit mph = (MessagePartHit) mpit.next();
                        mp.addAttribute(MailService.A_PART, mph.getPartName());
                    }
                }
            }
        }
        return c;
    }
    
    protected Element addMessageHit(OperationContext octxt, Element response, MessageHit mh, EmailElementCache eecache, boolean inline, SearchParams params)
    throws ServiceException {
        Message msg = mh.getMessage();
        Element m;
        if (inline) {
            m = ToXML.encodeMessageAsMP(response, msg, params.getWantHtml(), null);
            if (!msg.getFragment().equals(""))
                m.addAttribute(MailService.E_FRAG, msg.getFragment(), Element.DISP_CONTENT);
        } else
            m = ToXML.encodeMessageSummary(response, msg, params.getWantRecipients());
        if (mh.getScore() != 0)
            m.addAttribute(MailService.A_SCORE, mh.getScore());
        
        m.addAttribute(MailService.A_CONTENTMATCHED, true);
        
        ArrayList parts = mh.getMatchedMimePartNames();
        if (parts != null) {
            for (Iterator mpit = parts.iterator(); mpit.hasNext(); ) {
                Element mp = m.addElement(MailService.E_MIMEPART);
                MessagePartHit mph = (MessagePartHit) mpit.next();
                mp.addAttribute(MailService.A_PART, mph.getPartName());
            }
        }

        if (inline && msg.isUnread() && params.getMarkRead())
            try {
                Mailbox mbox = msg.getMailbox();
                mbox.alterTag(octxt, msg.getId(), msg.getType(), Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException e) {
                mLog.warn("problem marking message as read (ignored): " + msg.getId(), e);
            }

        return m;
    }
    
    protected Element addMessageHit(Element response, Message msg, EmailElementCache eecache, SearchParams params) {
        Element m = ToXML.encodeMessageSummary(response, msg, params.getWantRecipients());
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
    
    Element addContactHit(Element response, ContactHit ch, EmailElementCache eecache) throws ServiceException {
        return ToXML.encodeContact(response, ch.getContact(), null, true, null);
    }
    
    Element addNoteHit(Element response, NoteHit nh, EmailElementCache eecache) throws ServiceException {
        // TODO - does this need to be a summary, instead of the whole note?
        return ToXML.encodeNote(response, nh.getNote());
    }
}
