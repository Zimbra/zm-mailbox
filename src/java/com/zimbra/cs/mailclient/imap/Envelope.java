/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
    private String date;
    private String subject;
    private Address[] from;
    private Address[] sender;
    private Address[] replyTo;
    private Address[] to;
    private Address[] cc;
    private Address[] bcc;
    private String inReplyTo;
    private String messageId;

    public static Envelope read(ImapInputStream is) throws IOException {
        Envelope env = new Envelope();
        env.readEnvelope(is);
        return env;
    }
    
    private void readEnvelope(ImapInputStream is) throws IOException {
        is.skipSpaces();
        is.skipChar('(');
        date = is.readNString();
        is.skipChar(' ');
        subject = is.readNString();
        is.skipChar(' ');
        from = readAList(is);
        is.skipChar(' ');
        sender = readAList(is);
        is.skipChar(' ');
        replyTo = readAList(is);
        is.skipChar(' ');
        to = readAList(is);
        is.skipChar(' ');
        cc = readAList(is);
        is.skipChar(' ');
        bcc = readAList(is);
        is.skipChar(' ');
        inReplyTo = is.readNString();
        is.skipChar(' ');
        messageId = is.readNString();
        is.skipSpaces();
        is.skipChar(')');
    }

    private static Address[] readAList(ImapInputStream is) throws IOException {
        is.skipSpaces();
        if (is.match('(')) {
            List<Address> addrs = new ArrayList<Address>();
            is.skipSpaces();
            while (!is.match(')')) {
                addrs.add(readAddress(is));
                is.skipSpaces();
            }
            return addrs.toArray(new Address[addrs.size()]);
        } else {
            is.skipNil();
            return null;
        }
    }

    private static Address readAddress(ImapInputStream is) throws IOException {
        is.skipChar('(');
        Address addr = new Address();
        addr.name = is.readNString();
        is.skipChar(' ');
        addr.adl = is.readNString();
        is.skipChar(' ');
        addr.host = is.readNString();
        is.skipChar(' ');
        addr.mailbox = is.readNString();
        is.skipSpaces();
        is.skipChar(')');
        return addr;
    }

    public String getDate() {return date; }
    public String getSubject() { return subject; }
    public String getMessageId() { return messageId; }
    public Address[] getFrom() { return from; }
    public Address[] getSender() { return sender; }
    public Address[] getReplyTo() { return replyTo; }
    public Address[] getTo() { return to; }
    public Address[] getCc() { return cc; }
    public Address[] getBcc() { return bcc; }
    public String getInReplyTo() { return inReplyTo; }

    public static class Address {
        private String name;
        private String adl;
        private String host;
        private String mailbox;

        public String getName() { return name; }
        public String getAdl() { return adl; }
        public String getHost() { return host; }
        public String getMailbox() { return mailbox; }
    }
}
