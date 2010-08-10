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
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since Nov 30, 2004
 */
public class SearchConv extends Search {
    private static Log sLog = LogFactory.getLog(Search.class);

    private static final int CONVERSATION_FIELD_MASK =
        Change.MODIFIED_SIZE | Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("**Start SearchConv");
        }

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean nest = request.getAttributeBool(MailConstants.A_NEST_MESSAGES, false);

        Account acct = getRequestedAccount(zsc);
        SearchParams params = SearchParams.parse(request, zsc,
                acct.getAttr(Provisioning.A_zimbraPrefMailInitialSearch));

        // append (conv:(convid)) onto the beginning of the queryStr
        ItemId cid = new ItemId(request.getAttribute(MailConstants.A_CONV_ID), zsc);
        StringBuilder queryBuffer = new StringBuilder("conv:\"");
        queryBuffer.append(cid.toString(ifmt));
        queryBuffer.append("\" (");
        queryBuffer.append(params.getQueryStr());
        queryBuffer.append(")");
        params.setQueryStr(queryBuffer.toString());

        // force to group-by-message
        params.setTypesStr(MailboxIndex.GROUP_BY_MESSAGE);

        if (cid.belongsTo(mbox)) {
            // LOCAL!
            ZimbraQueryResults results = this.doSearch(zsc, octxt, mbox, params);

            try {
                Element response = zsc.createElement(MailConstants.SEARCH_CONV_RESPONSE);
                response.addAttribute(MailConstants.A_QUERY_OFFSET, Integer.toString(params.getOffset()));

                SortBy sort = results.getSortBy();
                response.addAttribute(MailConstants.A_SORTBY, sort.toString());

                List<Message> msgs = mbox.getMessagesByConversation(octxt, cid.getId(), sort);
                if (msgs.isEmpty() && zsc.isDelegatedRequest()) {
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                }

                // filter out IMAP \Deleted messages from the message lists
                Conversation conv = mbox.getConversationById(octxt, cid.getId());
                if (conv.isTagged(Flag.ID_FLAG_DELETED)) {
                    List<Message> raw = msgs;
                    msgs = new ArrayList<Message>();
                    for (Message msg : raw) {
                        if (!msg.isTagged(Flag.ID_FLAG_DELETED)) {
                            msgs.add(msg);
                        }
                    }
                }

                Element container = nest ? ToXML.encodeConversationSummary(
                        response, ifmt, octxt, conv, CONVERSATION_FIELD_MASK): response;
                SearchResponse builder = new SearchResponse(zsc, octxt, container, params);

                boolean more = putHits(octxt, ifmt, builder, msgs, results, params);
                response.addAttribute(MailConstants.A_QUERY_MORE, more);

                // call me AFTER putHits since some of the <info> is generated
                // by the getting of the hits!
                builder.add(results.getResultInfo(), results.estimateResultSize());
                return response;
            } finally {
                results.doneWithSearchResults();
            }
        } else {
            try {
                Element proxyRequest = zsc.createElement(MailConstants.SEARCH_CONV_REQUEST);

                params.encodeParams(proxyRequest);
                proxyRequest.addAttribute(MailConstants.A_NEST_MESSAGES, nest);
                proxyRequest.addAttribute(MailConstants.A_CONV_ID, cid.toString());

                try {
                    // okay, lets run the search through the query parser -- this has the side-effect of
                    // re-writing the query in a format that is OK to proxy to the other server -- since the
                    // query has an "AND conv:remote-conv-id" part, the query parser will figure out the right
                    // format for us.  TODO somehow make this functionality a bit more exposed in the
                    // ZimbraQuery APIs...
                    String rewrittenQueryString = mbox.getRewrittenQueryString(octxt, params);
                    proxyRequest.addAttribute(MailConstants.E_QUERY, rewrittenQueryString, Element.Disposition.CONTENT);

                    // now create a soap transport to talk to the remote account
                    Account target = Provisioning.getInstance().get(AccountBy.id, cid.getAccountId(), zsc.getAuthToken());
                    SoapHttpTransport soapTransp = new SoapHttpTransport(AccountUtil.getSoapUri(target));
                    String pxyAuthToken = Provisioning.onLocalServer(target) ? null : zsc.getAuthToken().getProxyAuthToken();
                    soapTransp.setAuthToken(pxyAuthToken == null ? AuthProvider.getAuthToken(acct).getEncoded() : pxyAuthToken);
                    soapTransp.setTargetAcctId(target.getId());
                    soapTransp.setRequestProtocol(zsc.getResponseProtocol());

                    // and just pass the response on through!
                    Element response = soapTransp.invokeWithoutSession(proxyRequest);
                    return response.detach();
                } catch (ParseException e) {
                    MailServiceException me = null;
                    String message = e.getMessage();
                    if (e.code != null)
                        message = e.code;
                    if (e.expectedTokenSequences != null) {
                        // this is a direct ParseException from JavaCC - don't return their long message as the code
                        message = "PARSER_ERROR";
                    }
                    if (e.currentToken != null) {
                        me = MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(),
                                e, e.currentToken.image, e.currentToken.beginColumn, message);
                    } else {
                        me = MailServiceException.QUERY_PARSE_ERROR(params.getQueryStr(),
                                e, "", -1, message);
                    }
                    throw me;
                } catch (IOException e) {
                    throw ServiceException.FAILURE("IOException: ", e);
                } catch (SoapFaultException e) {
                    throw ServiceException.FAILURE("SoapFaultException: ", e);
                }
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("AuthTokenException: ", e);
            }
        }
    }

    /**
     * This will only work for messages. That's OK since we force
     * GROUP_BY_MESSAGE here.
     *
     * @param octxt operation context
     * @param el SOAP container to put response data in
     * @param msgs list of messages in this conversation
     * @param results set of HITS for messages in this conversation which
     *  matches the search
     * @param offset offset in conversation to start at
     * @param limit number to return
     * @return whether there are more messages in the conversation past
     *  the specified limit
     * @throws ServiceException
     */
    private boolean putHits(OperationContext octxt, ItemIdFormatter ifmt,
            SearchResponse resp, List<Message> msgs, ZimbraQueryResults results,
            SearchParams params) throws ServiceException {

        int offset = params.getOffset();
        int limit  = params.getLimit();

        if (sLog.isDebugEnabled()) {
            sLog.debug("SearchConv beginning with offset " + offset);
        }

        int iterLen = limit;

        if (msgs.size() <= iterLen + offset) {
            iterLen = msgs.size() - offset;
        }

        if (iterLen > 0) {
            // Array of ZimbraHit ptrs for matches, 1 entry for every message
            // we might return from conv. NULL means no ZimbraHit presumably b/c
            // the message didn't match the search.
            //
            // Note that the match for msgs[i] is matched[i-offset]!!!!
            ZimbraHit matched[] = new ZimbraHit[iterLen];

            // For each hit, see if the hit message is in this conv (msgs).
            while (results.hasNext()) {
                ZimbraHit hit = results.getNext();
                // we only bother checking the messages between offset and
                // offset + iterLen, since only they are getting returned.
                for (int i = offset; i < offset + iterLen; i++) {
                    if (hit.getItemId() == msgs.get(i).getId()) {
                        matched[i - offset] = hit;
                        break;
                    }
                }
            }

            // Okay, we've built the matched[] array. Now iterate through all
            // the messages, and put the message or the MATCHED entry
            // into the result
            for (int i = offset; i < offset + iterLen; i++) {
                if (matched[i - offset] != null) {
                    resp.add(matched[i - offset]);
                } else {
                    Message msg = msgs.get(i);
                    ExpandResults expand = params.getInlineRule();
                    boolean inline = expand == ExpandResults.ALL || expand.matches(msg);
                    addMessageMiss(msg, resp.toElement(), octxt, ifmt, inline, params);
                }
            }
        }

        return offset + iterLen < msgs.size();
    }

    private Element addMessageMiss(Message msg, Element response,
            OperationContext octxt, ItemIdFormatter ifmt, boolean inline,
            SearchParams params) throws ServiceException {

        // for bug 7568, mark-as-read must happen before the response is encoded.
        if (inline && msg.isUnread() && params.getMarkRead()) {
            // Mark the message as READ
            try {
                msg.getMailbox().alterTag(octxt, msg.getId(), msg.getType(),
                        Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException e) {
                mLog.warn("problem marking message as read (ignored): " +
                        msg.getId(), e);
            }
        }

        Element el;
        if (inline) {
            el = ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null,
                    params.getMaxInlinedLength(), params.getWantHtml(),
                    params.getNeuterImages(), null, true);
            if (!msg.getFragment().equals("")) {
                el.addAttribute(MailConstants.E_FRAG, msg.getFragment(),
                        Element.Disposition.CONTENT);
            }
        } else {
            el = ToXML.encodeMessageSummary(response, ifmt, octxt, msg,
                    params.getWantRecipients());
        }
        return el;
    }

}
