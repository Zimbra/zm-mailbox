package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.ParseException;

import java.util.Iterator;
import java.util.List;

/**
 * Parses IMAP envelope:
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

    public Envelope(ImapData data) throws ParseException {
        parse(data);
    }

    private void parse(ImapData data) throws ParseException {
        if (!data.isList() || data.getListValue().size() != 10) {
            throw badEnvelope("not a list or incorrect list size");
        }
        Iterator<ImapData> it = data.getListValue().iterator();
        mDate = parseNString(it.next(), "date");
        mSubject = parseNString(it.next(), "subject");
        mFrom = parseAList(it.next(), "from");
        mSender = parseAList(it.next(), "sender");
        mReplyTo = parseAList(it.next(), "reply-to");
        mTo = parseAList(it.next(), "to");
        mCc = parseAList(it.next(), "cc");
        mBcc = parseAList(it.next(), "bcc");
        mInReplyTo = parseAList(it.next(), "in-reply-to");
        mMessageId = parseNString(it.next(), "message-id");
    }

    private String parseNString(ImapData data, String field)
            throws ParseException {
        if (!data.isNString()) throw invalidField(field);
        return data.isNil() ? null : data.getStringValue();
    }

    private Address[] parseAList(ImapData data, String field)
            throws ParseException {
        if (data.isNil()) return null;
        if (!data.isList() || data.getListValue().size() < 1) {
            throw invalidField(field);
        }
        List<ImapData> l = data.getListValue();
        Address[] addrs = new Address[l.size()];
        Iterator<ImapData> it = l.iterator();
        for (int i = 0; i < addrs.length; i++) {
            addrs[i] = parseAddress(it.next(), field);
        }
        return addrs;
    }

    private Address parseAddress(ImapData data, String field) throws ParseException {
        if (!data.isList() || data.getListValue().size() != 4) {
            throw invalidField(field);
        }
        Address addr = new Address();
        Iterator<ImapData> it = data.getListValue().iterator();
        addr.mName = parseNString(it.next(), field);
        addr.mAdl = parseNString(it.next(), field);
        addr.mHost = parseNString(it.next(), field);
        addr.mMailbox = parseNString(it.next(), field);
        return addr;
    }

    private static ParseException badEnvelope(String s) {
        return new ParseException("Bad envelope: " + s);
    }

    private static ParseException invalidField(String field) {
        return badEnvelope("invalid value for field '" + field + "'");
    }
    
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
