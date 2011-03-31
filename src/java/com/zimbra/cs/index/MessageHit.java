/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedAddress;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * Efficient Read-access to a {@link Message} returned from a query. APIs mirror
 * the APIs on {@link Message}, but are read-only. The real archive.mailbox.Message
 * can be retrieved, but this should only be done if write-access is necessary.
 *
 * @since Oct 15, 2004
 * @author tim
 */
public final class MessageHit extends ZimbraHit {

    private static final Log LOG = LogFactory.getLog(MessageHit.class);

    private Document document = null;
    private Message message = null;
    private List<MessagePartHit> matchedParts = null;
    private int conversationId = 0;
    private int messageId = 0;
    private ConversationHit conversationHit = null;

    MessageHit(ZimbraQueryResultsImpl results, Mailbox mbx, int itemId, Document doc, Message msg) {
        super(results, mbx);
        assert(itemId != 0);
        messageId = itemId;
        document = doc;
        message = msg;
    }

    int getFolderId() throws ServiceException {
        return getMessage().getFolderId();
    }

    @Override
    public int getConversationId() throws ServiceException {
        if (conversationId == 0) {
            conversationId = getMessage().getConversationId();
        }
        return conversationId;
    }

    @Override
    public long getDate() throws ServiceException {
        if (cachedDate == -1) {
            if (message == null && document != null) {
                String dateStr = document.get(LuceneFields.L_SORT_DATE);
                if (dateStr != null) {
                    try {
                        return cachedDate = DateTools.stringToTime(dateStr);
                    } catch (ParseException e) {
                        return 0;
                    }
                }
            }
            cachedDate = getMessage().getDate();
        }
        return cachedDate;
    }

    public void addPart(MessagePartHit part) {
        if (matchedParts == null) {
            matchedParts = new ArrayList<MessagePartHit>();
        }
        if (!matchedParts.contains(part)) {
            matchedParts.add(part);
        }
    }

    public List<MessagePartHit> getMatchedMimePartNames() {
        return matchedParts;
    }

    @Override
    public int getItemId() {
        return messageId;
    }

    @Override
    public String toString() {
        try {
            return Objects.toStringHelper(this)
                .add("id", getItemId())
                .add("conv", getConversationId())
                .add("size", getSize())
                .addValue(super.toString())
                .toString();
        } catch (ServiceException e) {
            return e.toString();
        }
    }

    @Override
    public long getSize() throws ServiceException {
        if (cachedSize == -1) {
            if (message == null && document != null) {
                String sizeStr = document.get(LuceneFields.L_SORT_SIZE);
                if (sizeStr != null) {
                    cachedSize = Long.parseLong(sizeStr);
                    return cachedSize;
                }
            }
            cachedSize = getMessage().getSize();
        }
        return cachedSize;
    }

    public boolean isTagged(Tag tag) throws ServiceException {
        return getMessage().isTagged(tag);
    }

    @Override
    void setItem(MailItem item) {
        message = (Message) item;
    }

    @Override
    boolean itemIsLoaded() {
        return message != null;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getMessage();
    }

    public Message getMessage() throws ServiceException {
        if (message == null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(
                    getMailbox().getId());
            int messageId = getItemId();
            try {
                message = mbox.getMessageById(null, messageId);
            } catch (ServiceException e) {
                LOG.error("Failed to get message mbox=%d,id=%d", mbox.getId(), messageId, e);
                throw e;
            }
        }
        return message;
    }

    @Override
    public String getSubject() throws ServiceException {
        if (cachedSubj == null) {
            cachedSubj = getMessage().getSortSubject();
        }
        return cachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (cachedName == null) {
            cachedName = getSender();
        }
        return cachedName;
    }

    @Override
    public String getRecipients() throws ServiceException {
        if (cachedRecipients == null) {
            cachedRecipients = Strings.nullToEmpty(getMessage().getSortRecipients());
        }
        return cachedRecipients;
    }

    public long getDateHeader() throws ServiceException {
        if (message == null && document != null) {
            String dateStr = document.get(LuceneFields.L_SORT_DATE);
            if (dateStr != null) {
                try {
                    return DateTools.stringToTime(dateStr);
                } catch (ParseException e) {
                    return 0;
                }
            } else {
                return 0;
            }
        }
        return getMessage().getDate();
    }

    public String getSender() throws ServiceException {
        return new ParsedAddress(getMessage().getSender()).getSortString();
    }

    /**
     * Returns a {@link ConversationHit} corresponding to this message's conversation.
     */
    public ConversationHit getConversationResult() throws ServiceException {
        if (conversationHit == null) {
            Integer cid = new Integer(getConversationId());
            conversationHit = getResults().getConversationHit(getMailbox(), cid);
            conversationHit.addMessageHit(this);
        }
        return conversationHit;
    }
}
