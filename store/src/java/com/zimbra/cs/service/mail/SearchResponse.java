/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ZimbraFetchMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.CalendarItemHit;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.ConversationHit;
import com.zimbra.cs.index.DocumentHit;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.MessagePartHit;
import com.zimbra.cs.index.NoteHit;
import com.zimbra.cs.index.ProxiedHit;
import com.zimbra.cs.index.QueryInfo;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.ContactMemberOfMap;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.mail.GetCalendarItemSummaries.EncodeCalendarItemResult;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.ConversationMsgHitInfo;
import com.zimbra.soap.mail.type.MessageHitInfo;
import com.zimbra.soap.util.JaxbInfo;

/**
 * A helper class to build a search SOAP response.
 *
 * @author ysasaki
 */
final class SearchResponse {
    protected static final Log LOG = LogFactory.getLog(SearchResponse.class);

    private final ZimbraSoapContext zsc;
    private final ItemIdFormatter ifmt;
    private final SearchParams params;
    private final Element element;
    private final OperationContext octxt;
    private boolean includeMailbox = false;
    private int size = 0;
    private final ExpandResults expand;
    private SortBy sortOrder = SortBy.NONE;;
    private boolean allRead = false;
    private final Map<String,Set<String>> memberOfMap;

    protected SearchResponse(ZimbraSoapContext zsc, OperationContext octxt, Element el, SearchParams params) {
        this(zsc, octxt, el, params, (Map<String,Set<String>>) null);
    }

    protected SearchResponse(ZimbraSoapContext zsc, OperationContext octxt, Element el, SearchParams params,
            Map<String,Set<String>> memberOf) {
        this.zsc = zsc;
        this.params = params;
        this.octxt = octxt;
        this.element = el;
        ifmt = new ItemIdFormatter(zsc);
        expand = params.getInlineRule();
        memberOfMap = memberOf;
    }

    void setAllRead(boolean value) {
        allRead = value;
    }

    /**
     * Set whether the response includes mailbox IDs or not.
     *
     * @param value true to include, otherwise false
     */
    void setIncludeMailbox(boolean value) {
        includeMailbox = value;
    }

    void setSortOrder(SortBy value) {
        sortOrder = value;
    }

    /**
     * Append a paging flag to the response.
     *
     * @param hasMore true if the search result has more pages, otherwise false
     */
    void addHasMore(boolean hasMore) {
        element.addAttribute(MailConstants.A_QUERY_MORE, hasMore);
    }

    /**
     * Once you are done, call this method to get the result.
     *
     * @return result
     */
    Element toElement() {
        return element;
    }

    /**
     * Returns the number of hits.
     *
     * @return number of hits
     */
    int size() {
        return size;
    }

    /**
     * Append the hit to this response.
     *
     * @param hit hit to append
     * @throws ServiceException error
     */
    void add(ZimbraHit zimbraHit) throws ServiceException{
		add(zimbraHit,false);

	}
    /* We need to pass in a boolean signifying whether to expand the message or not (bug 75990)
    */
    void add(ZimbraHit hit, boolean expandMsg) throws ServiceException {
        Element el = null;
        if (params.getFetchMode() == SearchParams.Fetch.IDS) {
            if (hit instanceof ConversationHit) {
                // need to expand the contained messages
                el = element.addNonUniqueElement(MailConstants.E_HIT);
                el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(hit.getParsedItemID()));
            } else {
                el = element.addNonUniqueElement(MailConstants.E_HIT);
                el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(hit.getParsedItemID()));
            }
        } else if (hit instanceof ProxiedHit) {
            element.addNonUniqueElement(((ProxiedHit) hit).getElement().detach());
            size++;
            return;
        } else {
            if (hit instanceof ConversationHit) {
                el = add((ConversationHit) hit);
            } else if (hit instanceof MessageHit) {
                el = add((MessageHit) hit,expandMsg);
            } else if (hit instanceof MessagePartHit) {
                el = add((MessagePartHit) hit);
            } else if (hit instanceof ContactHit) {
                el = add((ContactHit) hit);
            } else if (hit instanceof NoteHit) {
                el = add((NoteHit) hit);
            } else if (hit instanceof CalendarItemHit) {
                el = add((CalendarItemHit) hit); // el could be null
            } else if (hit instanceof DocumentHit) {
                el = add((DocumentHit) hit);
            } else {
                LOG.error("Got an unknown hit type putting search hits: " + hit);
                return;
            }
        }

        if (el != null) {
            size++;
            el.addAttribute(MailConstants.A_SORT_FIELD, hit.getSortField(sortOrder).toString());
            if (includeMailbox) {
                el.addAttribute(MailConstants.A_ID, new ItemId(hit.getAcctIdStr(), hit.getItemId()).toString());
            }
        }
    }

    private Element add(ConversationHit hit) throws ServiceException {
        if (params.getFetchMode() == SearchParams.Fetch.IDS) {
            Element el = element.addNonUniqueElement(MailConstants.E_CONV);
            for (MessageHit mhit : hit.getMessageHits()) {
                ConversationMsgHitInfo cMsgHit = new ConversationMsgHitInfo(ifmt.formatItemId(mhit.getItemId()));
                cMsgHit.toElement(el);
            }
            return el;
        } else {
            Conversation conv = hit.getConversation();
            MessageHit mhit = hit.getFirstMessageHit();
            Element el = ToXML.encodeConversationSummary(element, ifmt, octxt, conv,
                    mhit == null ? null : mhit.getMessage(), params.getWantRecipients());

            Collection<MessageHit> msgHits = hit.getMessageHits();
            long numMsgs = el.getAttributeLong(MailConstants.A_NUM, 0);
            if (!params.fullConversation() || numMsgs == msgHits.size()) {
                for (MessageHit mh : msgHits) {
                    Message msg = mh.getMessage();
                    doConvMsgHit(el, msg, numMsgs);
                }
            } else {
                for (Message msg : conv.getMailbox().getMessagesByConversation(octxt, conv, SortBy.DATE_DESC,
                        -1 /* limit */, false /* excludeSpamAndTrash */)) {
                    doConvMsgHit(el, msg, numMsgs);
                }
            }
            return el;
        }
    }

    private Element doConvMsgHit(Element el, Message msg, long numMsgsInConv) {
        // Folder ID useful when undoing a move to different folder, also determining whether in junk/trash
        ConversationMsgHitInfo cMsgHit =
                ConversationMsgHitInfo.fromIdAndFolderId(ifmt.formatItemId(msg),
                    ifmt.formatItemId(new ItemId(msg.getAccountId(), msg.getFolderId())));
        // if it's a 1-message conversation, hand back size for the lone message
        if (numMsgsInConv == 1) {
            cMsgHit.setSize(msg.getSize());
        }
        if (msg.isDraft() && msg.getDraftAutoSendTime() != 0) {
            cMsgHit.setAutoSendTime(msg.getDraftAutoSendTime());
        }
        if(!msg.getFlagString().equalsIgnoreCase("")) {
            cMsgHit.setFlags(msg.getFlagString());
        }
        cMsgHit.setDate(msg.getDate());
        cMsgHit.toElement(el);
        return el;
    }

    //for bug 75990, we are now passing an expandMsg boolean instead of calculating in isInLineExpand
    private Element add(MessageHit hit, boolean expandMsg) throws ServiceException {
        Message msg = hit.getMessage();
        // for bug 7568, mark-as-read must happen before the response is encoded.
        if (expandMsg && msg.isUnread() && params.getMarkRead()) {
            // Mark the message as READ
            try {
                msg.getMailbox().alterTag(octxt, msg.getId(), msg.getType(), Flag.FlagInfo.UNREAD, false, null);
            } catch (ServiceException e) {
                if (e.getCode().equals(ServiceException.PERM_DENIED)) {
                    LOG.info("no permissions to mark message as read (ignored): %d", msg.getId());
                } else {
                    LOG.warn("problem marking message as read (ignored): %d", msg.getId(), e);
                }
            }
        }

        Element el;
        int fields;
        if (params.isQuick()) {
            fields = PendingModifications.Change.CONTENT;
        } else {
            fields = getFieldBitmask();
        }
        if (expandMsg) {
            el = ToXML.encodeMessageAsMP(element, ifmt, octxt, msg, null, params.getMaxInlinedLength(),
                    params.getWantHtml(), params.getNeuterImages(), params.getInlinedHeaders(), true,
                    params.getWantExpandGroupInfo(), LC.mime_encode_missing_blob.booleanValue(),
                    params.getWantContent(), fields);
        } else {
            el = ToXML.encodeMessageSummary(element, ifmt, octxt, msg, params.getWantRecipients(), fields);
        }

        el.addAttribute(MailConstants.A_CONTENTMATCHED, true);

        List<MessagePartHit> parts = hit.getMatchedMimePartNames();
        if (parts != null) {
            for (MessagePartHit mph : parts) {
                String partNameStr = mph.getPartName();
                if (partNameStr.length() > 0) {
                    el.addNonUniqueElement(MailConstants.E_HIT_MIMEPART).addAttribute(MailConstants.A_PART, partNameStr);
                }
            }
        }
        /* Different SOAP interfaces assemble elements in different orders.  WSDL/JAXB require a specific order,
         * so force them into the correct order here.  When/if we change to using the JAXB objects, this problem
         * will go away.
         */
        JaxbInfo jaxbInfo = JaxbInfo.getFromCache(MessageHitInfo.class);
        List<List <org.dom4j.QName>> nameOrder = jaxbInfo.getElementNameOrder();
        return Element.reorderChildElements(el, nameOrder);
    }

    private Element add(MessagePartHit hit) throws ServiceException {
        Message msg = hit.getMessageResult().getMessage();
        Element el = element.addNonUniqueElement(MailConstants.E_MIMEPART);
        el.addAttribute(MailConstants.A_SIZE, msg.getSize());
        el.addAttribute(MailConstants.A_DATE, msg.getDate());
        el.addAttribute(MailConstants.A_CONV_ID, msg.getConversationId());
        el.addAttribute(MailConstants.A_MESSAGE_ID, msg.getId());
        el.addAttribute(MailConstants.A_CONTENT_TYPE, hit.getType());
        el.addAttribute(MailConstants.A_CONTENT_NAME, hit.getFilename());
        el.addAttribute(MailConstants.A_PART, hit.getPartName());

        ToXML.encodeEmail(el, msg.getSender(), EmailType.FROM);
        String subject = msg.getSubject();
        if (subject != null) {
            el.addAttribute(MailConstants.E_SUBJECT, subject, Element.Disposition.CONTENT);
        }

        return el;
    }

    private int getFieldBitmask() {
        int fields = ToXML.NOTIFY_FIELDS;
        ZimbraFetchMode fetchMode = params.getZimbraFetchMode();
        if (fetchMode == ZimbraFetchMode.MODSEQ) {
            fields |= Change.MODSEQ;
        } else if (fetchMode == ZimbraFetchMode.IMAP) {
            fields |= (Change.MODSEQ | Change.IMAP_UID);
        }
        return fields;
    }

    private Element add(ContactHit hit) throws ServiceException {
        return ToXML.encodeContact(element, ifmt, octxt, hit.getContact(), (ContactGroup)null,
                (Collection<String>)null /* memberAttrFilter */, true /* summary */,
                (Collection<String>)null /* attrFilter */, getFieldBitmask(),
                (String)null /* migratedDlist */, false /* returnHiddenAttrs */,
                GetContacts.NO_LIMIT_MAX_MEMBERS, true /* returnCertInfo */,
                ContactMemberOfMap.setOfMemberOf(zsc.getRequestedAccountId(), hit.getItemId(), memberOfMap));
    }

    private Element add(NoteHit hit) throws ServiceException {
        return ToXML.encodeNote(element, ifmt, octxt, hit.getNote());
    }

    private Element add(DocumentHit hit) throws ServiceException {
        if (hit.getItemType() == MailItem.Type.DOCUMENT) {
            return ToXML.encodeDocument(element, ifmt, octxt, hit.getDocument());
        } else if (hit.getItemType() == MailItem.Type.WIKI) {
            return ToXML.encodeWiki(element, ifmt, octxt, (WikiItem) hit.getDocument());
        } else {
            throw ServiceException.UNKNOWN_DOCUMENT("invalid document type " + hit.getItemType(), null);
        }
    }

    /**
     * The encoded element OR NULL if the search params contained a calItemExpand
     * range AND the calendar item did not have any instances in the specified range.
     *
     * @return could be NULL
     */
    private Element add(CalendarItemHit hit) throws ServiceException {
        CalendarItem item = hit.getCalendarItem();
        Account acct = DocumentHandler.getRequestedAccount(zsc);
        long rangeStart = params.getCalItemExpandStart();
        long rangeEnd = params.getCalItemExpandEnd();
        if (rangeStart == -1 && rangeEnd == -1 && (item instanceof Appointment)) {
            // If no time range was given, force first instance only. (bug 51267)
            rangeStart = item.getStartTime();
            rangeEnd = rangeStart + 86400000;
        }
        EncodeCalendarItemResult encoded =
            GetCalendarItemSummaries.encodeCalendarItemInstances(zsc, octxt, item, acct, rangeStart, rangeEnd, true);

        Element el = encoded.element;
        if (el != null) {
            element.addNonUniqueElement(el);
            ToXML.setCalendarItemFields(el, ifmt, octxt, item, PendingModifications.Change.ALL_FIELDS, false,
                    params.getNeuterImages());
            el.addAttribute(MailConstants.A_CONTENTMATCHED, true);
        }
        return el;
    }

    /**
     * Append the query information to this response.
     *
     * @param qinfo query information
     */
    public void add(List<QueryInfo> qinfo) {
        if (qinfo.size() > 0) {
            Element el = element.addNonUniqueElement(MailConstants.E_INFO);
            for (QueryInfo inf : qinfo) {
                inf.toXml(el);
            }
        }
    }
}
