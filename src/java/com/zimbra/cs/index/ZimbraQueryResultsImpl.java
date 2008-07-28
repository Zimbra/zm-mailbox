/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;


import java.util.*;
//import java.io.IOException;

//import com.zimbra.common.util.Log;
//import com.zimbra.common.util.LogFactory;

//import org.apache.lucene.search.*;
import org.apache.lucene.document.*;

/**
 */
abstract class ZimbraQueryResultsImpl implements ZimbraQueryResults
{
    static final class LRUHashMap<T, U> extends LinkedHashMap<T, U> {
        private final int mMaxSize;
        LRUHashMap(int maxSize) {
            super(maxSize);
            mMaxSize = maxSize;
        }
        LRUHashMap(int maxSize, int tableSize) {
            super(tableSize);
            mMaxSize = maxSize;
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {  
            return size() > mMaxSize;
          }          
    }
    
    /////////////////////////
    //
    // These come from the ZimbraQueryResults interface:
    //
    // void resetIterator() throws ServiceException;
    // ZimbraHit getNext() throws ServiceException;
    // ZimbraHit peekNext() throws ServiceException;
    //

    public abstract void doneWithSearchResults() throws ServiceException;
    public abstract ZimbraHit skipToHit(int hitNo) throws ServiceException;

    public boolean hasNext() throws ServiceException {
        return (peekNext() != null);
    }
    
    private static final int MAX_LRU_ENTRIES = 2048;
    private static final int INITIAL_TABLE_SIZE = 100; 

    private HashMap<Integer, ConversationHit> mConversationHits;
    private HashMap<Integer, MessageHit> mMessageHits;
    private HashMap<String, MessagePartHit> mPartHits;
    private HashMap<Integer, ContactHit> mContactHits;
    private HashMap<Integer, NoteHit>  mNoteHits;
    private HashMap<Integer, CalendarItemHit> mCalItemHits;
    
    protected MessageHit getCachedMessageHit(int messageId) {
        return null;
    }

    ZimbraQueryResultsImpl(byte[] types, SortBy searchOrder, Mailbox.SearchResultMode mode) { 
        mTypes = types;
        mMode = mode;

        mSearchOrder = searchOrder;

        mConversationHits = new LRUHashMap<Integer, ConversationHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        mMessageHits      = new LRUHashMap<Integer, MessageHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        mPartHits         = new LRUHashMap<String, MessagePartHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        mContactHits      = new LRUHashMap<Integer, ContactHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        mNoteHits         = new LRUHashMap<Integer, NoteHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
        mCalItemHits      = new LRUHashMap<Integer, CalendarItemHit>(MAX_LRU_ENTRIES, INITIAL_TABLE_SIZE);
    };

    public ZimbraHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }

    private byte[] mTypes;
    private SortBy mSearchOrder;
    private Mailbox.SearchResultMode mMode;

    public SortBy getSortBy() {
        return mSearchOrder;
    }

    byte[] getTypes() { 
        return mTypes;
    }

    public Mailbox.SearchResultMode getSearchMode() { return mMode; }

    protected ConversationHit getConversationHit(Mailbox mbx, int convId, float score) {
        ConversationHit ch = mConversationHits.get(convId);
        if (ch == null) {
            ch = new ConversationHit(this, mbx, convId, score);
            mConversationHits.put(convId, ch);
        } else {
            ch.updateScore(score);
        }
        return ch;
    }

    protected ContactHit getContactHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        ContactHit hit = mContactHits.get(mailItemId);
        if (hit == null) {
            hit = new ContactHit(this, mbx, mailItemId, d, score, ud);
            mContactHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected NoteHit getNoteHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        NoteHit hit = mNoteHits.get(mailItemId);
        if (hit == null) {
            hit = new NoteHit(this, mbx, mailItemId, d, score, ud);
            mNoteHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected CalendarItemHit getAppointmentHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        CalendarItemHit hit = mCalItemHits.get(mailItemId);
        if (hit == null) {
            if (d != null) {
                hit = new CalendarItemHit(this, mbx, mailItemId, d, score, ud);
            } else {
                hit = new CalendarItemHit(this, mbx, mailItemId, score, ud);
            }
            mCalItemHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected CalendarItemHit getTaskHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        CalendarItemHit hit = mCalItemHits.get(mailItemId);
        if (hit == null) {
            hit = TaskHit.create(this, mbx, mailItemId, d, score, ud);
            mCalItemHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }
    
    protected MessageHit getMessageHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData underlyingData) throws ServiceException {
        MessageHit hit = mMessageHits.get(mailItemId);
        if (hit == null) {
            if (d != null) {
                hit = new MessageHit(this, mbx, mailItemId, d, score, underlyingData);
            } else {
                hit = new MessageHit(this, mbx, mailItemId, score, underlyingData);
            }
            mMessageHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected MessagePartHit getMessagePartHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData underlyingData) throws ServiceException 
    {
        String partKey = Integer.toString(mailItemId) + "-" + d.get(LuceneFields.L_PARTNAME);
        MessagePartHit hit = mPartHits.get(partKey);
        if (hit == null) {
            hit = new MessagePartHit(this, mbx, mailItemId, d, score, underlyingData);
            mPartHits.put(partKey, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }
    
    /**
     * @param type
     * @return
     *          TRUE if this type of SearchResult should be added multiple times if there are multiple 
     *          hits (e.g. if multiple document parts match) -- currently only true for MessageParts,
     *          false for all other kinds of result
     */
    static final boolean shouldAddDuplicateHits(byte type) {
        return (type == MailItem.TYPE_CHAT || type == MailItem.TYPE_MESSAGE);
    }
    
    /**
     * We've got a mailbox, a score a DBMailItem.SearchResult and (optionally) a Lucene Doc...
     * that's everything we need to build a real ZimbraHit.
     * 
     * @param mbox
     * @param score
     * @param sr
     * @param doc - Optional, only set if this search had a Lucene part
     * @return
     * @throws ServiceException
     */
    ZimbraHit getZimbraHit(Mailbox mbox, float score, SearchResult sr, Document doc, SearchResult.ExtraData extra) throws ServiceException {
        MailItem.UnderlyingData ud = null;
        ImapMessage i4msg = null;
        int modseq = -1, parentId = 0;
        switch (extra) {
            case MAIL_ITEM:  ud = (MailItem.UnderlyingData) sr.extraData;                   break;
            case IMAP_MSG:   i4msg = (ImapMessage) sr.extraData;                            break;
            case MODSEQ:     modseq = sr.extraData != null ? (Integer) sr.extraData : -1;   break;
            case PARENT:     parentId = sr.extraData != null ? (Integer) sr.extraData : 0;  break;
        }

        ZimbraHit toRet = null;
        switch (sr.type) {
            case MailItem.TYPE_CHAT:
            case MailItem.TYPE_MESSAGE:
                if (doc != null) {
                    toRet = getMessagePartHit(mbox, sr.id, doc, score, ud);
                    toRet.cacheSortField(getSortBy(), sr.sortkey);
                } else {
                    toRet = getMessageHit(mbox, sr.id, null, score, ud);
                    toRet.cacheSortField(getSortBy(), sr.sortkey);
                }
                break;
            case MailItem.TYPE_CONTACT:
                toRet = getContactHit(mbox, sr.id, null, score, ud);
                break;
            case MailItem.TYPE_NOTE:
                toRet = getNoteHit(mbox, sr.id, null, score, ud);
                break;
            case MailItem.TYPE_APPOINTMENT:
                toRet = getAppointmentHit(mbox, sr.id, null, score, ud);
                break;
            case MailItem.TYPE_TASK:
                toRet = getTaskHit(mbox, sr.id, null, score, ud);
                break;
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                toRet = getDocumentHit(mbox, sr.id, doc, score, ud);
                break;          
            default:
                assert(false);
        }

        if (i4msg != null)
            toRet.cacheImapMessage(i4msg);
        if (modseq > 0)
            toRet.cacheModifiedSequence(modseq);
        if (parentId != 0)
            toRet.cacheParentId(parentId);

        return toRet;
    }

    protected DocumentHit getDocumentHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData underlyingData) throws ServiceException {
        DocumentHit hit;
        if (d != null) {
            hit = new DocumentHit(this, mbx, score, mailItemId, underlyingData, d);
        } else {
            hit = new DocumentHit(this, mbx, score, mailItemId, underlyingData);
        }
        return hit;
    }
}
