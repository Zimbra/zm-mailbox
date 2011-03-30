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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Indirect {@link Conversation} result. Efficient Read-access to a {@link Conversation} object returned from a query.
 * <p>
 * This class may have a real {@link Conversation} under it, or it might just have a Lucene Document, or something else
 * -- the accessor APIs in this class will do the most efficient possible lookup and caching for read access to the data.
 *
 * @since Oct 15, 2004
 * @author tim
 */
public final class ConversationHit extends ZimbraHit {

    private final int conversationId;
    private Conversation conversation;
    private Map<Long, MessageHit> messageHits = new LinkedHashMap<Long, MessageHit>();
    private MessageHit lastMessageHitAdded;

    ConversationHit(ZimbraQueryResultsImpl results, Mailbox mbx, int convId) {
        super(results, mbx);
        conversationId = convId;
    }

    @Override
    public int getConversationId() {
        return getId();
    }

    public void addMessageHit(MessageHit hit) {
        lastMessageHitAdded = hit;
        messageHits.put(new Long(hit.getItemId()), hit);
    }

    public Collection<MessageHit> getMessageHits() {
        return messageHits.values();
    }

    public int getNumMessageHits() {
        return getMessageHits().size();
    }

    public int getNumMessages() throws ServiceException {
        return getConversation().getMessageCount();
    }

    public MessageHit getMessageHit(Long mailboxBlobId) {
        return messageHits.get(mailboxBlobId);
    }

    public MessageHit getFirstMessageHit() {
        Iterator<MessageHit> iter = getMessageHits().iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    @Override
    public int getItemId() {
        return conversationId;
    }

    @Override
    void setItem(MailItem item) {
        conversation = (Conversation) item;
    }

    @Override
    boolean itemIsLoaded() {
        return conversation != null;
    }

    public int getId() {
        return conversationId;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("item", getId()).addValue(super.toString()).toString();
    }

    /**
     * The subject returned here must be the SORTING subject, which is the subject of the most recent hit we found. (as
     * we iterate through results in order, the most recently added message is the order we want to track for sorting
     * purposes).
     */
    @Override
    public String getSubject() throws ServiceException {
        if (cachedSubj == null) {
            cachedSubj = lastMessageHitAdded.getSubject();
        }
        return cachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (cachedName == null) {
            MessageHit mh = getFirstMessageHit();
            cachedName = mh == null ? "" : mh.getName();
        }
        return cachedName;
    }

    @Override
    public String getRecipients() throws ServiceException {
        return Strings.nullToEmpty(lastMessageHitAdded.getRecipients());
    }

    public long getHitDate() throws ServiceException {
        MessageHit mh = getFirstMessageHit();
        return mh == null ? 0 : mh.getDate();
    }

    @Override
    public long getSize() throws ServiceException {
        return getConversation().getSize();
    }

    /**
     * Always use the hit date when sorting, otherwise confusion happens (since we are building the conv hit by
     * aggregating MessageHits....suddenly switching to a/ very different sort-field can cause sort order to be unstable.
     */
    @Override
    public long getDate() throws ServiceException {
        if (cachedDate == -1) {
            cachedDate = getHitDate();
            if (cachedDate == 0) {
                cachedDate = getConversation().getDate();
            }
        }
        return cachedDate;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getConversation();
    }

    /**
     * Returns the real {@link Conversation} object. Only use this if you need write access to the Conversation.
     *
     * Depending on the type of query that was executed, this may or may not result in a DB access
     *
     * @return real Conversation object
     */
    public Conversation getConversation() throws ServiceException {
        if (conversation == null) {
            conversation = getMailbox().getConversationById(null, conversationId);
        }
        return conversation;
    }
}
