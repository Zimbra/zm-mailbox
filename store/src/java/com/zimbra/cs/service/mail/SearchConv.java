/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SearchConvRequest;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @since Nov 30, 2004
 */
public final class SearchConv extends Search {

    private static final int CONVERSATION_FIELD_MASK = Change.SIZE | Change.TAGS | Change.FLAGS;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        SearchConvRequest req = zsc.elementToJaxb(request);
        boolean nest = ZmBoolean.toBool(req.getNestMessages(), false);
        Account acct = getRequestedAccount(zsc);
        SearchParams params = SearchParams.parse(req, zsc, acct.getPrefMailInitialSearch());

        // append (conv:(convid)) onto the beginning of the queryStr
        ItemId cid = new ItemId(req.getConversationId(), zsc);
        params.setQueryString("conv:\"" + cid.toString(ifmt) + "\" (" + params.getQueryString() + ')');
        // force to group-by-message
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));

        Element response = null;
        if (cid.belongsTo(mbox)) { // local
            try (ZimbraQueryResults results = mbox.index.search(zsc.getResponseProtocol(), octxt,
                params)) {
                response = zsc.createElement(MailConstants.SEARCH_CONV_RESPONSE);
                response.addAttribute(MailConstants.A_QUERY_OFFSET, Integer.toString(params.getOffset()));

                SortBy sort = results.getSortBy();
                response.addAttribute(MailConstants.A_SORTBY, sort.toString());

                List<Message> msgs = mbox.getMessagesByConversation(octxt, cid.getId(), sort, -1);
                if (msgs.isEmpty() && zsc.isDelegatedRequest()) {
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                }

                // filter out IMAP \Deleted messages from the message lists
                Conversation conv = mbox.getConversationById(octxt, cid.getId());
                if (conv.isTagged(Flag.FlagInfo.DELETED)) {
                    List<Message> raw = msgs;
                    msgs = new ArrayList<Message>();
                    for (Message msg : raw) {
                        if (!msg.isTagged(Flag.FlagInfo.DELETED)) {
                            msgs.add(msg);
                        }
                    }
                }

                Element container = nest ? ToXML.encodeConversationSummary(
                        response, ifmt, octxt, conv, CONVERSATION_FIELD_MASK): response;
                        SearchResponse builder = new SearchResponse(zsc, octxt, container, params);
                        builder.setAllRead(conv.getUnreadCount() == 0);
                        boolean more = putHits(octxt, ifmt, builder, msgs, results, params, conv);
                        response.addAttribute(MailConstants.A_QUERY_MORE, more);

                        // call me AFTER putHits since some of the <info> is generated by the getting of the hits!
                        builder.add(results.getResultInfo());
            } catch (IOException e) {
            } 

            return response;

        } else { // remote
            try {
                Element proxyRequest = zsc.createElement(MailConstants.SEARCH_CONV_REQUEST);
                Account target = Provisioning.getInstance().get(AccountBy.id, cid.getAccountId(), zsc.getAuthToken());

                if (target != null) {
                    params.setInlineRule(params.getInlineRule().toLegacyExpandResults(target.getServer()));
                }

                params.encodeParams(proxyRequest);
                proxyRequest.addAttribute(MailConstants.A_NEST_MESSAGES, nest);
                proxyRequest.addAttribute(MailConstants.A_CONV_ID, cid.toString());


                // okay, lets run the search through the query parser -- this has the side-effect of
                // re-writing the query in a format that is OK to proxy to the other server -- since the
                // query has an "AND conv:remote-conv-id" part, the query parser will figure out the right
                // format for us.  TODO somehow make this functionality a bit more exposed in the
                // ZimbraQuery APIs...
                String rewrittenQueryString = mbox.getRewrittenQueryString(octxt, params);
                proxyRequest.addAttribute(MailConstants.E_QUERY, rewrittenQueryString, Element.Disposition.CONTENT);

                // proxy to remote account
                response = proxyRequest(proxyRequest, context, target.getId());
                return response.detach();
            } catch (SoapFaultException e) {
                throw ServiceException.FAILURE("SoapFaultException: ", e);
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
    private boolean putHits(OperationContext octxt, ItemIdFormatter ifmt, SearchResponse resp, List<Message> msgs,
            ZimbraQueryResults results, SearchParams params, Conversation conv)
                    throws ServiceException {
        int offset = params.getOffset();
        int limit = params.getLimit();
        int size = msgs.size() <= limit + offset ? msgs.size() - offset : limit;

        if (size > 0) {
            // Array of ZimbraHit ptrs for matches, 1 entry for every message
            // we might return from conv. NULL means no ZimbraHit presumably b/c the message didn't match the search.
            // Note that the match for msgs[i] is matched[i-offset]!!!!
            ZimbraHit[] matched = new ZimbraHit[size];
            // For each hit, see if the hit message is in this conv (msgs).
            while (results.hasNext()) {
                ZimbraHit hit = results.getNext();
                // we only bother checking the messages between offset and offset + size,
                // since only they are getting returned.
                for (int i = offset; i < offset + size; i++) {
                    if (hit.getParsedItemID().equals(new ItemId(msgs.get(i)))) {
                        matched[i - offset] = hit;
                        break;
                    }
                }
            }
            ExpandResults expand = params.getInlineRule();

            /* Build a boolean array of which messages should be expanded.
             * This consolidates logic from SearchParams.isInlineExpand and the main message loop below
             */

            // Okay, we've built the matched[] array. Now iterate through all the messages, and put the message or
            // the MATCHED entry into the result
            boolean [] expandMsgs = determineExpandedMessages(expand, matched, msgs, conv, offset, size);
            for (int i = offset; i < offset + size; i++) {
                boolean expandMsg = expandMsgs[i];
                if (matched[i - offset] != null) {
                    resp.add(matched[i - offset],expandMsg);
                } else {
                    Message msg = msgs.get(i);
                    //boolean inline = expand == ExpandResults.ALL || expand.matches(msg);
                    addMessageMiss(msg, resp.toElement(), octxt, ifmt, expandMsg, params);
                }
            }
        }

        return offset + size < msgs.size();
    }

    /** Determine which messages in a conversation need to be expanded. This combines logic
     * that used to be in SearchResults.isInlineExpand and SearchConv.putHits.
     * Returns a boolean array with true/false corresponding to whether each message should
     * be expanded.
     */
    private boolean[] determineExpandedMessages(ExpandResults expand,
            ZimbraHit[] matched, List<Message> msgs, Conversation conv, int offset, int size) throws ServiceException {
        int numMatched = 0;
        boolean forceExpandFirstMsg;
        boolean[] expandMsgs = new boolean[size];
        if (expand == ExpandResults.FIRST_MSG ||
                expand == ExpandResults.HITS_OR_FIRST_MSG ||
                expand == ExpandResults.U_OR_FIRST_MSG ||
                expand == ExpandResults.U1_OR_FIRST_MSG ) {
            forceExpandFirstMsg = true;
        }
        else {forceExpandFirstMsg = false;}

        for (int i = offset; i < offset + size; i++) {
            boolean shouldExpand;
            if (matched[i - offset] != null) {
                numMatched++;
                MessageHit hit = (MessageHit) matched[i-offset];
                Message msg = hit.getMessage();
                if (expand == ExpandResults.FIRST) {
                    shouldExpand = numMatched == 1;
                } else if (expand == ExpandResults.ALL || expand == ExpandResults.HITS) {
                    shouldExpand = true;
                }
                else if (expand == ExpandResults.HITS_OR_FIRST_MSG) {
                    forceExpandFirstMsg = false;
                    shouldExpand = true;
                }
                else if (expand == ExpandResults.UNREAD) {
                    shouldExpand = msg.isUnread();
                }
                else if (expand == ExpandResults.U_OR_FIRST_MSG) {
                    shouldExpand = msg.isUnread();
                    if (forceExpandFirstMsg == true) {forceExpandFirstMsg = !shouldExpand;}
                }
                else if (expand == ExpandResults.UNREAD_FIRST) {
                    shouldExpand = conv.getUnreadCount() == 0 ? numMatched == 1  : msg.isUnread();
                }
                else if (expand == ExpandResults.U1_OR_FIRST_MSG) {
                    if  (conv.getUnreadCount() > 0) {forceExpandFirstMsg = false;}
                    shouldExpand = conv.getUnreadCount() == 0 ? numMatched == 1  : msg.isUnread();
                }
                else {
                    shouldExpand = expand.matches(hit.getParsedItemID());
                }
            }
            else {
                Message msg = msgs.get(i);
                shouldExpand = expand == ExpandResults.ALL || expand.matches(msg);
            }
            expandMsgs[i - offset] = shouldExpand;
        }

        if (forceExpandFirstMsg == true || expand == ExpandResults.FIRST_MSG) {expandMsgs[0] = true;}
        return expandMsgs;
    }

    private Element addMessageMiss(Message msg, Element response, OperationContext octxt, ItemIdFormatter ifmt,
            boolean inline, SearchParams params) throws ServiceException {
        //ZimbraLog.search.debug("query "+params.getQueryString()+" didn't match message "+Integer.toString(msg.getId()));

        // for bug 7568, mark-as-read must happen before the response is encoded.
        if (inline && msg.isUnread() && params.getMarkRead()) {
            // Mark the message as READ
            try {
                msg.getMailbox().alterTag(octxt, msg.getId(), msg.getType(), Flag.FlagInfo.UNREAD, false, null);
            } catch (ServiceException e) {
                ZimbraLog.search.warn("problem marking message as read (ignored): %d", msg.getId(), e);
            }
        }

        Element el;
        if (inline) {
            el = ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, params.getMaxInlinedLength(),
                    params.getWantHtml(), params.getNeuterImages(), null, true, params.getWantExpandGroupInfo(), LC.mime_encode_missing_blob.booleanValue(), params.getWantContent());
            if (!Strings.isNullOrEmpty(msg.getFragment())) {
                el.addAttribute(MailConstants.E_FRAG, msg.getFragment(), Element.Disposition.CONTENT);
            }
        } else {
            el = ToXML.encodeMessageSummary(response, ifmt, octxt, msg, params.getWantRecipients(), ToXML.NOTIFY_FIELDS);
        }
        return el;
    }

}
