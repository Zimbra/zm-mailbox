/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * IMAP response ENVELOPE data:
 * 
 * envelope        = "(" env-date SP env-subject SP env-from SP
 *                 env-sender SP env-reply-to SP env-to SP env-cc SP
 *                 env-bcc SP env-in-reply-to SP env-message-id ")"
 * env-bcc         = "(" 1*address ")" / nil
 * env-cc          = "(" 1*address ")" / nil
 * env-date        = nstring
 * env-from        = "(" 1*address ")" / nil
 * env-in-reply-to = nstring
 * env-message-id  = nstring
 * env-reply-to    = "(" 1*address ")" / nil
 * env-sender      = "(" 1*address ")" / nil
 * env-subject     = nstring
 * env-to          = "(" 1*address ")" / nil
 * 
 * address         = "(" addr-name SP addr-adl SP addr-mailbox SP
 *                       addr-host ")"
 * addr-adl        = nstring
 * addr-host       = nstring
 * addr-mailbox    = nstring
 * addr-name       = nstring
 */
public class Envelope {
    private String mDate;
    private String mSubject;
    private Address[] mFrom;
    private Address[] mSender;
    private Address[] mReplyTo;
    private Address[] mTo;
    private Address[] mCc;
    private Address[] mBcc;
    private Address[] mInReplyTo;
    private String mMessageId;

    public static Envelope read(ImapInputStream is) throws IOException {
        Envelope env = new Envelope();
        env.readEnvelope(is);
        return env;
    }
    
    private void readEnvelope(ImapInputStream is) throws IOException {
        is.skipChar('(');
        mDate = is.readNString();
        is.skipChar(' ');
        mSubject = is.readNString();
        is.skipChar(' ');
        mFrom = readAList(is);
        is.skipChar(' ');
        mSender = readAList(is);
        is.skipChar(' ');
        mReplyTo = readAList(is);
        is.skipChar(' ');
        mTo = readAList(is);
        is.skipChar(' ');
        mCc = readAList(is);
        is.skipChar(' ');
        mBcc = readAList(is);
        is.skipChar(' ');
        mInReplyTo = readAList(is);
        is.skipChar(' ');
        mMessageId = is.readNString();
        is.skipChar(')');
    }

    private static Address[] readAList(ImapInputStream is) throws IOException {
        if (is.match('(')) {
            List<Address> addrs = new ArrayList<Address>();
            do {
                addrs.add(readAddress(is));
            } while (!is.match(')'));
            return addrs.toArray(new Address[addrs.size()]);
        } else {
            is.skipNil();
            return null;
        }
    }

    private static Address readAddress(ImapInputStream is) throws IOException {
        is.skipChar('(');
        Address addr = new Address();
        addr.mName = is.readNString();
        is.skipChar(' ');
        addr.mAdl = is.readNString();
        is.skipChar(' ');
        addr.mHost = is.readNString();
        is.skipChar(' ');
        addr.mMailbox = is.readNString();
        is.skipChar(')');
        return addr;
    }

    public String getDate() {return mDate; }
    public String getSubject() { return mSubject; }
    public String getMessageId() { return mMessageId; }
    public Address[] getFrom() { return mFrom; }
    public Address[] getSender() { return mSender; }
    public Address[] getReplyTo() { return mReplyTo; }
    public Address[] getTo() { return mTo; }
    public Address[] getCc() { return mCc; }
    public Address[] getBcc() { return mBcc; }
    public Address[] getInReplyTo() { return mInReplyTo; }

    public static class Address {
        private String mName;
        private String mAdl;
        private String mHost;
        private String mMailbox;

        public String getName() { return mName; }
        public String getAdl() { return mAdl; }
        public String getHost() { return mHost; }
        public String getMailbox() { return mMailbox; }
    }
}
