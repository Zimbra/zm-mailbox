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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.service.ServiceException;

public class SenderList {
    public static class RefreshException extends Exception {
        private static final long serialVersionUID = 7137768259442997280L;
        public RefreshException(String message) { super(message); }
    }

    private static final int MAX_PARTICIPANT_COUNT = 5;

    private ParsedAddress mFirst;
    private List<ParsedAddress> mParticipants;  // ordered latest to earliest, doesn't include mFirst
    private boolean mIsElided;
    private long mLastDate;
    private int mSize;

    public SenderList()  {}

    public SenderList(Message msg) {
        mFirst = new ParsedAddress(msg.getSender()).parse();
        mLastDate = msg.getDate();
        mSize = 1;
    }

    public SenderList(List<Message> msgs) {
        if (msgs == null || msgs.isEmpty())
            return;
        Collections.sort(msgs, new MailItem.SortDateAscending());

        mFirst = new ParsedAddress(msgs.get(0).getSender()).parse();
        mLastDate = msgs.get(msgs.size() - 1).getDate();
        mSize = msgs.size();

        for (int i = msgs.size() - 1; i >= 1 && !mIsElided; i--) {
            ParsedAddress pa = new ParsedAddress(msgs.get(i).getSender()).parse();
            if (pa.equals(mFirst) || (mParticipants != null && mParticipants.contains(pa)))
                continue;
            else if (mParticipants == null)
                (mParticipants = new ArrayList<ParsedAddress>(MAX_PARTICIPANT_COUNT)).add(pa);
            else if (mParticipants.size() >= MAX_PARTICIPANT_COUNT)
                mIsElided = true;
            else
                mParticipants.add(pa);
        }
    }

    public SenderList(Message[] msgs) {
        if (msgs == null || msgs.length == 0)
            return;
        Arrays.sort(msgs, new MailItem.SortDateAscending());

        mFirst = new ParsedAddress(msgs[0].getSender()).parse();
        mLastDate = msgs[msgs.length - 1].getDate();
        mSize = msgs.length;

        for (int i = msgs.length - 1; i >= 1 && !mIsElided; i--) {
            ParsedAddress pa = new ParsedAddress(msgs[i].getSender()).parse();
            if (pa.equals(mFirst) || (mParticipants != null && mParticipants.contains(pa)))
                continue;
            else if (mParticipants == null)
                (mParticipants = new ArrayList<ParsedAddress>(MAX_PARTICIPANT_COUNT)).add(pa);
            else if (mParticipants.size() >= MAX_PARTICIPANT_COUNT)
                mIsElided = true;
            else
                mParticipants.add(pa);
        }
    }


    public SenderList add(Message msg) throws RefreshException {
        if (msg.getDate() < mLastDate)
            throw new RefreshException("appended message predates existing last message");

        mLastDate = msg.getDate();
        mSize++;
        ParsedAddress pa = new ParsedAddress(msg.getSender()).parse();
        
        if (mFirst == null) {
            mFirst = pa;
        } else if (pa.equals(mFirst)) {
            return this;
        } else if (mParticipants == null) {
            (mParticipants = new ArrayList<ParsedAddress>(MAX_PARTICIPANT_COUNT)).add(pa);
        } else {
            mParticipants.remove(pa);
            mParticipants.add(0, pa);
            while (mParticipants.size() > MAX_PARTICIPANT_COUNT) {
                mParticipants.remove(MAX_PARTICIPANT_COUNT);
                mIsElided = true;
            }
        }

        return this;
    }


    public int size()                       { return mSize; }
    public boolean isElided()               { return mIsElided; }
    public ParsedAddress getFirstAddress()  { return mFirst; }
    public List<ParsedAddress> getLastAddresses() {
        if (mParticipants == null || mParticipants.isEmpty())
            return Collections.emptyList();
        List<ParsedAddress> addrs = new ArrayList<ParsedAddress>(mParticipants);
        if (addrs.size() > 1)
            Collections.reverse(addrs);
        return addrs;
    }


    private static final String FN_EMAIL    = "a";
    private static final String FN_PERSONAL = "p";
    private static final String FN_DISPLAY  = "d";

    private static ParsedAddress importAddress(Metadata meta) {
        if (meta == null)
            return null;
        ParsedAddress pa = new ParsedAddress(meta.get(FN_EMAIL, null), meta.get(FN_PERSONAL, null));
        pa.firstName = meta.get(FN_DISPLAY, null);
        return pa;
    }

    public static SenderList parse(String encoded) throws ServiceException {
        return parse(new Metadata(encoded));
    }

    public static SenderList parse(Metadata meta) throws ServiceException {
        SenderList sl = new SenderList();
        sl.mSize = (int) meta.getLong(Metadata.FN_NODES);
        sl.mFirst = importAddress(meta.getMap(Metadata.FN_FIRST, true));
        sl.mIsElided = meta.getBool(Metadata.FN_ELIDED);
        MetadataList entries = meta.getList(Metadata.FN_ENTRIES, true);
        if (entries != null && !entries.isEmpty()) {
            sl.mParticipants = new ArrayList<ParsedAddress>(entries.size());
            for (int i = 0; i < entries.size(); i++)
                sl.mParticipants.add(importAddress(entries.getMap(i)));
        }
        return sl;
    }

    private static Metadata exportAddress(ParsedAddress pa) {
        if (pa == null)
            return null;
        Metadata meta = new Metadata();
        meta.put(FN_EMAIL, pa.emailPart);
        meta.put(FN_PERSONAL, pa.personalPart);
        meta.put(FN_DISPLAY, pa.firstName);
        return meta;
    }

    @Override
    public String toString() {
        Metadata meta = new Metadata();
        meta.put(Metadata.FN_NODES, mSize);
        meta.put(Metadata.FN_FIRST, exportAddress(mFirst));
        meta.put(Metadata.FN_ELIDED, mIsElided);
        if (mParticipants != null && !mParticipants.isEmpty()) {
            MetadataList entries = new MetadataList();
            for (ParsedAddress pa : mParticipants)
                entries.add(exportAddress(pa));
            meta.put(Metadata.FN_ENTRIES, entries);
        }
        return meta.toString();
    }
}
