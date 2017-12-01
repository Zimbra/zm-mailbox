/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Task;

/**
 * @since Oct 15, 2004
 */
abstract class ZimbraQueryResultsImpl implements ZimbraQueryResults {

    static final class LRUHashMap<T, U> extends LinkedHashMap<T, U> {
        private static final long serialVersionUID = -6181398012977532525L;

        private final int max;

        LRUHashMap(int max) {
            super(max, 0.75f, true);
            this.max = max;
        }

        LRUHashMap(int max, int size) {
            super(size, 0.75f, true);
            this.max = max;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<T, U> eldest) {
            return size() > max;
        }
    }

    private static final int MAX_LRU_ENTRIES = 2048;
    private static final int INITIAL_TABLE_SIZE = 100;

    private Map<Integer, ConversationHit> conversationHits;
    private Map<Integer, MessageHit> messageHits;
    private Map<String, MessagePartHit> partHits;
    private Map<Integer, ContactHit> contactHits;
    private Map<Integer, NoteHit>  noteHits;
    private Map<Integer, CalendarItemHit> calItemHits;

    private final Set<MailItem.Type> types;
    private final SortBy sortBy;
    private final SearchParams.Fetch fetch;

    ZimbraQueryResultsImpl(Set<MailItem.Type> types, SortBy sort, SearchParams.Fetch fetch) {
        this.types = types;
        this.fetch = fetch;
        this.sortBy = sort;

        conversationHits = new LRUHashMap<Integer, ConversationHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        messageHits = new LRUHashMap<Integer, MessageHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        partHits = new LRUHashMap<String, MessagePartHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        contactHits = new LRUHashMap<Integer, ContactHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        noteHits = new LRUHashMap<Integer, NoteHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        calItemHits = new LRUHashMap<Integer, CalendarItemHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
    };

    @Override
    public abstract ZimbraHit skipToHit(int hitNo) throws ServiceException;

    @Override
    public boolean hasNext() throws ServiceException {
        return (peekNext() != null);
    }

    @Override
    public SortBy getSortBy() {
        return sortBy;
    }

    Set<MailItem.Type> getTypes() {
        return types;
    }

    public SearchParams.Fetch getFetchMode() {
        return fetch;
    }

    protected ConversationHit getConversationHit(Mailbox mbx, int id, Object sortValue) {
        ConversationHit hit = conversationHits.get(id);
        if (hit == null) {
            hit = new ConversationHit(this, mbx, id, sortValue);
            conversationHits.put(id, hit);
        }
        return hit;
    }

    protected ContactHit getContactHit(Mailbox mbx, int id, Contact contact, Object sortValue) {
        ContactHit hit = contactHits.get(id);
        if (hit == null) {
            hit = new ContactHit(this, mbx, id, contact, sortValue);
            contactHits.put(id, hit);
        }
        return hit;
    }

    protected NoteHit getNoteHit(Mailbox mbx, int id, Note note, Object sortValue) {
        NoteHit hit = noteHits.get(id);
        if (hit == null) {
            hit = new NoteHit(this, mbx, id, note, sortValue);
            noteHits.put(id, hit);
        }
        return hit;
    }

    protected CalendarItemHit getAppointmentHit(Mailbox mbx, int id, CalendarItem cal, Object sortValue) {
        CalendarItemHit hit = calItemHits.get(id);
        if (hit == null) {
            hit = new CalendarItemHit(this, mbx, id, cal, sortValue);
            calItemHits.put(id, hit);
        }
        return hit;
    }

    protected CalendarItemHit getTaskHit(Mailbox mbx, int id, Task task, Object sortValue) {
        CalendarItemHit hit = calItemHits.get(id);
        if (hit == null) {
            hit = new TaskHit(this, mbx, id, task, sortValue);
            calItemHits.put(id, hit);
        }
        return hit;
    }

    protected MessageHit getMessageHit(Mailbox mbx, int id, Message msg, IndexDocument doc, Object sortValue) {
        MessageHit hit = messageHits.get(id);
        if (hit == null) {
            hit = new MessageHit(this, mbx, id, msg, doc, sortValue);
            messageHits.put(id, hit);
        }
        return hit;
    }

    protected MessagePartHit getMessagePartHit(Mailbox mbx, int id, Message msg, IndexDocument doc, Object sortValue) {
        String key = Integer.toString(id) + "-" + doc.get(LuceneFields.L_PARTNAME);
        MessagePartHit hit = partHits.get(key);
        if (hit == null) {
            hit = new MessagePartHit(this, mbx, id, msg, doc, sortValue);
            partHits.put(key, hit);
        }
        return hit;
    }

    protected DocumentHit getDocumentHit(Mailbox mbx, int id, com.zimbra.cs.mailbox.Document item,
            IndexDocument doc, Object sortValue) {
        return new DocumentHit(this, mbx, id, item, doc, sortValue);
    }

    /**
     * @param type
     * @return
     *          TRUE if this type of SearchResult should be added multiple times if there are multiple
     *          hits (e.g. if multiple document parts match) -- currently only true for MessageParts,
     *          false for all other kinds of result
     */
    static final boolean shouldAddDuplicateHits(MailItem.Type type) {
        return (type == MailItem.Type.CHAT || type == MailItem.Type.MESSAGE);
    }

    /**
     * We've got a {@link Mailbox}, a {@link DbSearch.Result} and (optionally) a Lucene Doc...
     * that's everything we need to build a real ZimbraHit.
     *
     * @param doc - Optional, only set if this search had a Lucene part
     */
    ZimbraHit getZimbraHit(Mailbox mbox, DbSearch.Result sr, IndexDocument doc, DbSearch.FetchMode fetch) {
        MailItem item = null;
        ImapMessage i4msg = null;
        int modseq = -1, parentId = 0;
        switch (fetch) {
            case MAIL_ITEM:
                item = sr.getItem();
                break;
            case IMAP_MSG:
                i4msg = sr.getImapMessage();
                break;
            case MODSEQ:
                modseq = sr.getModSeq();
                break;
            case PARENT:
                parentId = sr.getParentId();
                break;
        }

        ZimbraHit result = null;
        switch (sr.getType()) {
            case CHAT:
            case MESSAGE:
                if (doc != null) {
                    result = getMessagePartHit(mbox, sr.getId(), (Message) item, doc, sr.getSortValue());
                } else {
                    result = getMessageHit(mbox, sr.getId(), (Message) item, null, sr.getSortValue());
                }
                break;
            case CONTACT:
                result = getContactHit(mbox, sr.getId(), (Contact) item, sr.getSortValue());
                break;
            case NOTE:
                result = getNoteHit(mbox, sr.getId(), (Note) item, sr.getSortValue());
                break;
            case APPOINTMENT:
                result = getAppointmentHit(mbox, sr.getId(), (CalendarItem) item, sr.getSortValue());
                break;
            case TASK:
                result = getTaskHit(mbox, sr.getId(), (Task) item, sr.getSortValue());
                break;
            case DOCUMENT:
            case WIKI:
                result = getDocumentHit(mbox, sr.getId(), (com.zimbra.cs.mailbox.Document) item, doc, sr.getSortValue());
                break;
            default:
                assert(false);
        }

        if (i4msg != null) {
            result.cacheImapMessage(i4msg);
        }
        if (modseq > 0) {
            result.cacheModifiedSequence(modseq);
        }
        if (parentId != 0) {
            result.cacheParentId(parentId);
        }
        return result;
    }

    @Override
    public boolean isPreSorted() {
        return false;
    }
}
