/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Jun 11, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.*;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.EmailElementCache;
import com.zimbra.cs.service.mail.EmailElementCache.CacheNode;


public class SenderList {
    public static class ParseException extends Exception {
        public ParseException(String message) { super(message); }
        public ParseException(Exception e)    { super(e); }
    }
    public static class RefreshException extends Exception {
        public RefreshException(String message) { super(message); }
    }

    private static class SenderCache extends EmailElementCache {
        private int mTouched = 0;
        
        private static final class SenderNode extends CacheNode {
            private SenderNode(String email, String personal, String display, int id) {
                super(email, personal, id);
                firstName = display;
            }
        }

        public CacheNode add(CacheNode cn)   { return add(cn, null, true); }
        public CacheNode add(String address) {
            CacheNode node = add(address, null, true);
            if (node == null)
                return null;
            node.parse();
            return node;
        }
        public void remove(CacheNode node) {
            for (Iterator it = mCache.values().iterator(); it.hasNext(); )
                if (it.next() == node)
                    it.remove();
        }

        Set prepare(Set set) {
            mTouched = 0;
            for (Iterator it = set.iterator(); it.hasNext(); )
                ((CacheNode) it.next()).first = false;
            return set;
        }

        void mark(CacheNode node) {
            if (node != null && !node.first) {
                node.first = true;
                mTouched++;
            }
        }
        boolean isMarked(CacheNode node)  { return node.first; }
        boolean stillMarking() { return mTouched < MAXIMUM_CACHED; }

        void sweep(Set set) {
            mId = 0;
            for (Iterator it = set.iterator(); it.hasNext(); ) {
                CacheNode node = (CacheNode) it.next();
                if (!node.first)
                    it.remove();
                else
                    node.id = Integer.toString(mId++);
            }
            for (Iterator it = mCache.values().iterator(); it.hasNext(); ) {
                CacheNode node = (CacheNode) it.next();
                if (!node.first)
                    node.id = Integer.toString(mId++);
            }
        }

        private static final String FN_ID       = "i";
        private static final String FN_EMAIL    = "a";
        private static final String FN_PERSONAL = "p";
        private static final String FN_DISPLAY  = "d";

        HashMap readNodes(MetadataList mlist) throws ServiceException {
            HashMap nodes = new HashMap();
            for (int i = 0; i < mlist.size(); i++) {
                Metadata meta = mlist.getMap(i);
                CacheNode node = add(new SenderNode(meta.get(FN_EMAIL, null), meta.get(FN_PERSONAL, null),
                                                    meta.get(FN_DISPLAY, null), mId++));
                if (node != null)
                    nodes.put(meta.get(FN_ID), node);
            }
            return nodes;
        }
        MetadataList exportNodes(Set set) {
            MetadataList nodes = new MetadataList();
            for (Iterator it = set.iterator(); it.hasNext(); ) {
                CacheNode node = (CacheNode) it.next();
                Metadata nmap = new Metadata();
                nmap.put(FN_ID, node.id);
                nmap.put(FN_EMAIL, node.emailPart);
                nmap.put(FN_PERSONAL, node.personalPart);
                nmap.put(FN_DISPLAY, node.firstName);
                nodes.add(nmap);
            }
            return nodes;
        }
    }

    final class ListEntry {
        public final int       messageId;
        public final int       date;
        public final CacheNode address;

        ListEntry(int id, int received, CacheNode sender) {
            messageId = id;
            date      = received;
            address   = sender;
        }
        ListEntry(int id, int received, String sender) {
            messageId = id;
            date      = received;
            address   = mCache.add(sender);
        }
        ListEntry(Message msg) {
            messageId = msg.getId();
            date      = (int) (msg.getDate() / 1000);
            address   = mCache.add(msg.getSender());
        }

        private static final String FN_MSG_ID  = "M";
        private static final String FN_NODE_ID = "N";
        private static final String FN_DATE    = "D";

        Metadata exportEntry() {
            Metadata meta = new Metadata();
            meta.put(FN_MSG_ID, messageId);
            meta.put(FN_DATE, date);
            meta.put(FN_NODE_ID, address == null ? null : address.id);
            return meta;
        }
    }

    private static final int MINIMUM_CACHED = 3;
    private static final int MAXIMUM_CACHED = 5;

    private ListEntry mFirst;
    private int       mElided = 0;
    private LinkedList /*<ListEntry>*/ mEntries = new LinkedList();

    SenderCache mCache = new SenderCache();

    SenderList()              { }
    SenderList(Message msg)   { add(msg); }
    SenderList(int id, int date, String sender)  { add(new ListEntry(id, date, sender)); }

    SenderList(String senders) throws ParseException, RefreshException {
        if (senders == null || senders.equals(""))
            return;

        HashSet removed = null;
        while (senders.length() > 0 && Character.isDigit(senders.charAt(0))) {
            int delimeter = senders.indexOf(':');
            if (delimeter == -1)
                break;
            if (removed == null)
                removed = new HashSet();
            removed.add(Integer.decode(senders.substring(0, delimeter)));
            senders = senders.substring(delimeter + 1);
        }

        try {
            Metadata meta = new Metadata(senders);
            Map nodes = mCache.readNodes(meta.getList(Metadata.FN_NODES));
            ListEntry le;

            mFirst = readEntry(meta.getMap(Metadata.FN_FIRST, true), nodes);
            if (mFirst != null) {
                mElided = (int) meta.getLong(Metadata.FN_ELIDED);
                if (removed != null && removed.contains(new Integer(mFirst.messageId))) {
                    if (mElided > 0)
                        throw new RefreshException("first message deleted");
                    else
                        mFirst = null;
                } else if (mElided == 0) {
                    mEntries.add(mFirst);
                    mFirst = null;
                } else if (nodes.size() < MINIMUM_CACHED)
                    throw new RefreshException("too few cached");
            }
            MetadataList entries = meta.getList(Metadata.FN_ENTRIES);
            for (int i = 0; i < entries.size() && (le = readEntry(entries.getMap(i), nodes)) != null; i++)
                if (removed == null || !removed.remove(new Integer(le.messageId)))
                    mEntries.add(le);
            if (removed != null)
                mElided -= removed.size();

            if (mElided < 0)
                throw new RefreshException("count incorrect");
            else if (mElided > 0 && getParticipants().size() < MINIMUM_CACHED)
                throw new RefreshException("too few cached");
            else if (mElided == 0 && mFirst != null) {
                mEntries.add(0, mFirst);
                mFirst = null;
            }
        } catch (ServiceException e) {
            throw new ParseException("malformed string");
        } catch (NumberFormatException e) {
            throw new ParseException("bad integer value");
        } catch (NullPointerException e) {
            throw new ParseException("missing field");
        }
    }

    private ListEntry readEntry(Metadata meta, Map nodes) {
        if (meta == null)
            return null;
        String msgId = meta.get(ListEntry.FN_MSG_ID, null);
        String date  = meta.get(ListEntry.FN_DATE, null);
        String nid = meta.get(ListEntry.FN_NODE_ID, null);
        if (msgId == null || date == null)
            return null;
        return new ListEntry(Integer.parseInt(msgId), Integer.parseInt(date), (CacheNode) nodes.get(nid));
    }

    void add(Message msg) {
        if (msg == null)
            return;
        add(new ListEntry(msg));
    }
    private void add(int msgId, int date, String addr) {
        add(new ListEntry(msgId, date, addr));
    }
    private void add(ListEntry le) {
        if (le == null)
            return;
        
        if (mFirst != null && le.date < mFirst.date) {
            mFirst = le;
            mElided++;
            return;
        }

        ListIterator lit = mEntries.listIterator();
        while (lit.hasNext()) {
            int index = lit.nextIndex();
            ListEntry nextEntry = (ListEntry) lit.next();
            if (le.date < nextEntry.date) {
                if (index > 0 || mElided == 0) {
                    lit.previous();
                    lit.add(le);
                } else
                    mElided++;
                return;
            }
            index++;
        }
        lit.add(le);
    }

    void remove(Message msg) throws RefreshException {
        if (msg != null)
            remove(msg.getId());
    }
    private void remove(int messageId) throws RefreshException {
        if (mEntries.size() == 0)
            return;

        if (mFirst != null && messageId == mFirst.messageId)
            throw new RefreshException("first message deleted");
        
        ListIterator lit = mEntries.listIterator();
        while (lit.hasNext()) {
            ListEntry le = (ListEntry) lit.next();
            if (messageId == le.messageId) {
                lit.remove();
                if (le.address != null) {
                    boolean removeNode = true;
                    if (mFirst != null && le.address == mFirst.address)
                        removeNode = false;
                    for (Iterator it = mEntries.iterator(); removeNode && it.hasNext(); )
                        if (((ListEntry) it.next()).address == le.address)
                            removeNode = false;
                    if (removeNode) {
                        mCache.remove(le.address);
                        // maybe a little too aggressive, but probably fine
                        if (mElided > 0 && getParticipants().size() <= MINIMUM_CACHED)
                            throw new RefreshException("too few cached");
                    }
                    return;
                }
            }
        }
        
        // that message wasn't cached, so we're hoping it's in that "..." of skipped mesasges
        // but we probably should have been doing some date checking in the mSenders iteration
        mElided--;
        if (mElided < 0)
            throw new RefreshException("count incorrect");
        else if (mElided == 0 && mFirst != null) {
            mEntries.addFirst(mFirst);
            mFirst = null;
        }
    }
    
    static SenderList merge(SenderList left, SenderList right) {
        // really want to return duplicates of the returned lists
        if (left == null || left.mEntries.size() == 0)
            return right;
        else if (right == null || right.mEntries.size() == 0)
            return left;
        
        SenderList result = new SenderList();
        
        result.mElided = left.mElided + right.mElided;

        if (left.mFirst == null && right.mFirst == null) {
            result.mergeLists(left.mEntries.listIterator(), right.mEntries.listIterator());
            return result;
        }

        ListIterator rightIter;

        if (left.mFirst != null && right.mFirst != null) {
            result.mFirst = result.copyNode(left.mFirst.date < right.mFirst.date ? left.mFirst : right.mFirst);
            result.mElided++;
            if (((ListEntry) left.mEntries.getFirst()).date > ((ListEntry) right.mEntries.getFirst()).date) {
                SenderList temp = left; left = right; right = temp;
            }
            rightIter = right.mEntries.listIterator(1);
            result.mElided++;
        } else {
            if (left.mFirst == null) {
                SenderList temp = left; left = right; right = temp;
            }
            if (left.mFirst.date < ((ListEntry) right.mEntries.getFirst()).date) {
                result.mFirst = left.mFirst;
                rightIter = right.mEntries.listIterator();
            } else {
                result.mFirst = (ListEntry) right.mEntries.getFirst();
                result.mElided++;
                rightIter = right.mEntries.listIterator(1);
            }
        }

        int earliestIncluded = ((ListEntry) left.mEntries.getFirst()).date;
        while (rightIter.hasNext()) {
            if (((ListEntry) rightIter.next()).date > earliestIncluded) {
                rightIter.previous();
                break;
            }
            result.mElided++;
        }
        result.mergeLists(left.mEntries.listIterator(), rightIter);
        return result;
    }
    
    private void mergeLists(ListIterator left, ListIterator right) {
        ListEntry nextLeft = left.hasNext() ? (ListEntry) left.next() : null;
        ListEntry nextRight = right.hasNext() ? (ListEntry) right.next() : null;
        
        while (nextLeft != null && nextRight != null) {
            if (nextLeft.date < nextRight.date) {
                mEntries.add(copyNode(nextLeft));
                nextLeft = left.hasNext() ? (ListEntry) left.next() : null;
            } else {
                mEntries.add(copyNode(nextRight));
                nextRight = right.hasNext() ? (ListEntry) right.next() : null;
            }
        }
        
        if (nextLeft != null) {
            mEntries.add(copyNode(nextLeft));
            while (left.hasNext())
                mEntries.add(copyNode((ListEntry) left.next()));
        } else if (nextRight != null) {
            mEntries.add(copyNode(nextRight));
            while (right.hasNext())
                mEntries.add(copyNode((ListEntry) right.next()));
        }
    }
    
    private ListEntry copyNode(ListEntry le) {
        return new ListEntry(le.messageId, le.date, mCache.add(le.address));
    }

    public int size() {
        return (mFirst == null ? 0 : 1) + mElided + mEntries.size(); 
    }

    private Set getParticipants() {
        LinkedHashSet set = new LinkedHashSet();
        if (mFirst != null && mFirst.address != null)
            set.add(mFirst.address);
        for (Iterator it = mEntries.iterator(); it.hasNext(); ) {
            CacheNode node = ((ListEntry) it.next()).address;
            if (node != null)
                set.add(node);
        }
        return set;
    }

    private ListEntry getEarliest() {
        if (mFirst != null)
            return mFirst;
        return (mEntries.size() == 0 ? null : (ListEntry) mEntries.getFirst());
    }

    public CacheNode getFirstAddress() {
        ListEntry earliest = getEarliest();
        return (earliest == null ? null : earliest.address);
    }

    public boolean isElided() {
        return (mElided > 0);
    }

    private static final CacheNode[] NO_ADDRESSES = new CacheNode[0];

    public CacheNode[] getLastAddresses() {
        CacheNode first = getFirstAddress();
        LinkedHashSet set = new LinkedHashSet();
        for (ListIterator lit = mEntries.listIterator(mEntries.size()); lit.hasPrevious(); ) {
            CacheNode node = ((ListEntry) lit.previous()).address;
            if (node != null && node != first && !set.contains(node))
                set.add(node);
        }
        if (set.size() == 0)
            return NO_ADDRESSES;
        CacheNode result[] = new CacheNode[set.size()];
        Iterator it = set.iterator();
        for (int i = set.size() - 1; it.hasNext(); i--)
            result[i] = (CacheNode) it.next();
        return result;
    }

    public ListEntry getLatest() {
        return (mEntries.size() == 0 ? null : (ListEntry) mEntries.getLast());
    }

    private Set trimParticipants() {
        Set nodes = mCache.prepare(getParticipants());
        mCache.mark(getFirstAddress());
        for (ListIterator lit = mEntries.listIterator(mEntries.size()); mCache.stillMarking() && lit.hasPrevious(); )
            mCache.mark(((ListEntry) lit.previous()).address);
        if (mCache.stillMarking())
            return nodes;
        mCache.sweep(nodes);
        return nodes;
    }

    public String toString() {
        if (mEntries.size() == 0)
            return "";
        Metadata meta = new Metadata();
        meta.put(Metadata.FN_NODES, mCache.exportNodes(trimParticipants()));

        boolean trimmed = false;
        ListIterator lit;
        for (lit = mEntries.listIterator(mEntries.size()); lit.hasPrevious(); ) {
            CacheNode node = ((ListEntry) lit.previous()).address;
            if (node != null && !node.first) {
                trimmed = true;
                break;
            }
        }
        if (trimmed)
            lit.next();

        if (trimmed || mFirst != null) {
            meta.put(Metadata.FN_FIRST, getEarliest().exportEntry());
            int elided = mElided + (trimmed ? lit.previousIndex() : 0);
            meta.put(Metadata.FN_ELIDED, elided);
        }
        MetadataList entries = new MetadataList();
        for (int index = 0; lit.hasNext(); index++)
            entries.add(((ListEntry) lit.next()).exportEntry());
        meta.put(Metadata.FN_ENTRIES, entries);
        return meta.toString();
    }

    public static void main(String args[]) {
        SenderList test = new SenderList();
        test.add(1, 100000, "a <a@b.com>");
        test.add(2, 100100, "b <a@b.com>");
        test.add(3, 100500, "a <c@b.com>");
        test.add(4, 100300, "a <d@b.com>");
        test.add(5, 100900, "a <e@b.com>");
        test.add(5, 104900, "a <f@b.com>");
        test.add(5, 102900, "a <g@b.com>");
        test.add(5, 103900, "a <h@b.com>");
        test.add(5, 101900, "a <i@b.com>");
        System.out.println(test.toString());
    }
}