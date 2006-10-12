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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 30, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.operation.SearchOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 */
public class SearchConv extends Search {
    private static Log sLog = LogFactory.getLog(Search.class);

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        if (sLog.isDebugEnabled())
            sLog.debug("**Start SearchConv");

        ZimbraSoapContext zc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zc);
        Mailbox.OperationContext octxt = zc.getOperationContext();
        Session session = getSession(context);

        // FIXME: should proxy if conversation is a qualified ID in another mailbox
        int cid = (int) request.getAttributeLong(MailService.A_CONV_ID);
        SearchParams params = parseCommonParameters(request, zc);

        //
        // append (conv:(convid)) onto the beginning of the queryStr
        StringBuffer queryBuffer = new StringBuffer("conv:\"");
        queryBuffer.append(cid);
        queryBuffer.append("\" (");
        queryBuffer.append(params.getQueryStr());
        queryBuffer.append(")");
        params.setQueryStr(queryBuffer.toString());

        // 
        // force to group-by-message
        params.setTypesStr(MailboxIndex.GROUP_BY_MESSAGE);

        //ZimbraQueryResults results = this.getResults(mbox, params, lc, session);
        SearchOperation op = new SearchOperation(session, zc, zc.getOperationContext(), mbox, Requester.SOAP,  params);
        op.schedule();
        ZimbraQueryResults results = op.getResults();        

        try {
            Element response = zc.createElement(MailService.SEARCH_CONV_RESPONSE);
            response.addAttribute(MailService.A_QUERY_OFFSET, Integer.toString(params.getOffset()));

            SortBy sb = results.getSortBy();
            response.addAttribute(MailService.A_SORTBY, sb.toString());

            List<Message> msgs = mbox.getMessagesByConversation(octxt, cid, sb.getDbMailItemSortByte());

            // filter out IMAP \Deleted messages from the message lists
            Conversation conv = mbox.getConversationById(octxt, cid);
            if (conv.isTagged(mbox.mDeletedFlag)) {
                List<Message> raw = msgs;
                msgs = new ArrayList<Message>();
                for (Message msg : raw) {
                    if (!msg.isTagged(mbox.mDeletedFlag))
                        msgs.add(msg);
                }
            }

            Element retVal = putHits(zc, response, msgs, results, params);

            return retVal;
        } finally {
            results.doneWithSearchResults();
        }
    }

    /**
     * NOTE - this version will only work for messages.  That's OK since we force GROUP_BY_MESSAGE here
     * 
     * @param response - soap container to put response data in
     * @param msgs - list of messages in this conversation
     * @param results - set of HITS for messages in this conv which match the search
     * @param offset - offset in conv to start at 
     * @param limit - number to return
     * @return
     * @throws ServiceException
     */
    Element putHits(ZimbraSoapContext zc, Element response, List<Message> msgs, ZimbraQueryResults results, SearchParams params)
    throws ServiceException {
        int offset = params.getOffset();
        int limit  = params.getLimit();
        EmailElementCache eecache = new EmailElementCache();

        if (sLog.isDebugEnabled()) {
            sLog.debug("SearchConv beginning with offset "+offset);
        }

        int iterLen = limit;
//      boolean hasMoreHits = false;

        if (msgs.size() > iterLen + offset) {
//          hasMoreHits = true;
        } else {
            // iterLen+offset <= msgs.length
            iterLen = msgs.size() - offset;
        }

        if (iterLen > 0) {
            //
            // Array of ZimbraHit ptrs for matches, 1 entry for every message we might return from conv.
            // NULL means no ZimbraHit presumably b/c the message didn't match the search
            //
            // ***Note that the match for msgs[i] is matched[i-offset]!!!!
            //
            ZimbraHit matched[] = new ZimbraHit[iterLen];

            //
            // Foreach hit, see if the hit message is in msgs[] (list of msgs in this conv), and if so 
            //
            HitIter: 
                for (ZimbraHit curHit = results.getFirstHit(); curHit != null; curHit = results.getNext()) {
                    // we only bother checking the messages between offset and offset+iterLen, since only they
                    // are getting returned.
                    for (int i = offset; i < offset + iterLen; i++) {
                        if (curHit.getItemId() == msgs.get(i).getId()) {
                            matched[i-offset] = curHit;
                            continue HitIter; 
                        }
                    }
                }

            //
            // Okay, we've built the matched[] array.  Now iterate through all the messages, and put the message
            // or the MATCHED entry into the result
            //
            ExpandResults expand = params.getFetchFirst();
            for (int i = offset; i < offset + iterLen; i++) {
                if (matched[i-offset] != null) {
                    addMessageHit(zc, response, (MessageHit) matched[i-offset], eecache, expand != ExpandResults.NONE, params);
                    if (expand == ExpandResults.FIRST)
                        expand = ExpandResults.NONE;
                } else {
                    addMessageMiss(zc, response, msgs.get(i), eecache, expand == ExpandResults.ALL, params);
                }
            }
        }

        response.addAttribute(MailService.A_QUERY_MORE, offset + iterLen < msgs.size());

        return response;
    }
}
