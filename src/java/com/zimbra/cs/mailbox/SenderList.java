/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mime.ParsedAddress;

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
        String sender = msg.getSender();
        if (sender != null && !sender.trim().equals("")) {
            mFirst = new ParsedAddress(sender).parse();
            mLastDate = msg.getDate();
        }
        mSize = 1;
    }

    public SenderList(List<Message> msgs) {
        this(msgs == null ? null : msgs.toArray(new Message[msgs.size()]));
    }

    public SenderList(Message[] msgs) {
        if (msgs == null || msgs.length == 0)
            return;
        Arrays.sort(msgs, new MailItem.SortDateAscending());

        mLastDate = msgs[msgs.length - 1].getDate();
        mSize = msgs.length;

        int first = 0;
        do {
            String sender = msgs[first].getSender();
            if (sender != null && !sender.trim().equals(""))
                mFirst = new ParsedAddress(sender).parse();
        } while (mFirst == null && ++first < mSize);

        for (int i = msgs.length - 1; i >= first && !mIsElided; i--) {
            String sender = msgs[i].getSender();
            if (sender == null || sender.trim().equals(""))
                continue;
            ParsedAddress pa = new ParsedAddress(sender).parse();
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
        String sender = msg.getSender();
        if (sender == null || sender.trim().equals("")) {
            mSize++;
            return this;
        }

        long date = msg.getDate();
        if (date < mLastDate)
            throw new RefreshException("appended message predates existing last message");

        mLastDate = date;
        mSize++;

        ParsedAddress pa = new ParsedAddress(sender).parse();
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
        sl.mLastDate = meta.getLong(Metadata.FN_LAST_DATE, 0);
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
        meta.put(Metadata.FN_LAST_DATE, mLastDate);
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
