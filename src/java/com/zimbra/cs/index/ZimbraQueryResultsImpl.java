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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailItem.SearchResult;
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
 * @author tim
 * 
 * Really, this class should be renamed to ZimbraQueryResultsImpl, and the 
 * SuperInterface should be ZimbraQueryResultsImpl -- will fix this as soon as I 
 * get some free time.
 */
abstract class ZimbraQueryResultsImpl implements ZimbraQueryResults
{
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

    private HashMap<Integer, ConversationHit> mConversationHits;
    private HashMap<Integer, MessageHit> mMessageHits;
    private HashMap<String, MessagePartHit> mPartHits;
    private HashMap<Integer, ContactHit> mContactHits;
    private HashMap<Integer, NoteHit>  mNoteHits;
    private HashMap<Integer, CalendarItemHit> mCalItemHits;

    ZimbraQueryResultsImpl(byte[] types, SortBy searchOrder, Mailbox.SearchResultMode mode) { 
        mTypes = types;
        mMode = mode;

        mSearchOrder = searchOrder;

        mConversationHits = new LinkedHashMap<Integer, ConversationHit>();
        mMessageHits = new LinkedHashMap<Integer, MessageHit>();
        mPartHits = new LinkedHashMap<String, MessagePartHit>();
        mContactHits = new LinkedHashMap<Integer, ContactHit>();
        mNoteHits = new LinkedHashMap<Integer, NoteHit>();
        mCalItemHits = new LinkedHashMap<Integer, CalendarItemHit>();
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
        ConversationHit ch = (ConversationHit) mConversationHits.get(convId);
        if (ch == null) {
            ch = new ConversationHit(this, mbx, convId, score);
            mConversationHits.put(convId, ch);
        } else {
            ch.updateScore(score);
        }
        return ch;
    }

    protected ContactHit getContactHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        ContactHit hit = (ContactHit) mContactHits.get(mailItemId);
        if (hit == null) {
            hit = new ContactHit(this, mbx, mailItemId, d, score, ud);
            mContactHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected NoteHit getNoteHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        NoteHit hit = (NoteHit) mNoteHits.get(mailItemId);
        if (hit == null) {
            hit = new NoteHit(this, mbx, mailItemId, d, score, ud);
            mNoteHits.put(mailItemId, hit);
        } else {
            hit.updateScore(score);
        }
        return hit;
    }

    protected CalendarItemHit getCalendarItemHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        CalendarItemHit hit = (CalendarItemHit) mCalItemHits.get(mailItemId);
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

    protected MessageHit getMessageHit(Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData underlyingData) throws ServiceException {
        MessageHit hit = (MessageHit) mMessageHits.get(mailItemId);
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
        MessagePartHit hit = (MessagePartHit) mPartHits.get(partKey);
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
    ZimbraHit getZimbraHit(Mailbox mbox, float score, SearchResult sr, Document doc) throws ServiceException {
        ZimbraHit toRet = null;
        switch(sr.type) {
            case MailItem.TYPE_CHAT:
            case MailItem.TYPE_MESSAGE:
                if (doc != null) {
                    toRet = getMessagePartHit(mbox, sr.id, doc, score, sr.data);
                    toRet.cacheSortField(getSortBy(), sr.sortkey);
                } else {
                    toRet = getMessageHit(mbox, sr.id, null, score, sr.data);
                    toRet.cacheSortField(getSortBy(), sr.sortkey);
                }
                break;
            case MailItem.TYPE_CONTACT:
                toRet = getContactHit(mbox, sr.id, null, score, sr.data);
                break;
            case MailItem.TYPE_NOTE:
                toRet = getNoteHit(mbox, sr.id, null, score, sr.data);
                break;
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                toRet = getCalendarItemHit(mbox, sr.id, null, score, sr.data);
                break;
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                toRet = getDocumentHit(mbox, sr.id, doc, score, sr.data);
                break;          
            default:
                assert(false);
        }

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
