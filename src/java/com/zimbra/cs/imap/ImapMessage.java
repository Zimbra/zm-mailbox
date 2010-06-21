/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.ParseException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.BCodec;
import org.apache.commons.codec.net.QCodec;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeCompoundHeader;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.util.JMSession;

public class ImapMessage implements Comparable<ImapMessage>, java.io.Serializable {
    private static final long serialVersionUID = -1756550148606322493L;

    static class ImapMessageSet extends TreeSet<ImapMessage> {
        private static final long serialVersionUID = 4831178352505203361L;
        ImapMessageSet()                              { super(new SequenceComparator()); }
        ImapMessageSet(Collection<ImapMessage> msgs)  { this();  addAll(msgs); }
    }

    public static final List<Byte> SUPPORTED_TYPES = Arrays.asList(MailItem.TYPE_MESSAGE, MailItem.TYPE_CHAT, MailItem.TYPE_CONTACT);

    static final short FLAG_RECENT       = 0x0001;
    static final short FLAG_SPAM         = 0x0002;
    static final short FLAG_NONSPAM      = 0x0004;
    static final short FLAG_JUNKRECORDED = 0x0008;
    static final short FLAG_IS_CONTACT   = 0x0010;
    static final short FLAG_ADDED        = 0x0100;
    static final short FLAG_EXPUNGED     = 0x0200;

    static final int IMAP_FLAGS = Flag.BITMASK_UNREAD | Flag.BITMASK_FLAGGED | Flag.BITMASK_DELETED |
                                  Flag.BITMASK_DRAFT  | Flag.BITMASK_REPLIED | Flag.BITMASK_FORWARDED |
                                  Flag.BITMASK_NOTIFIED;
    static final short MUTABLE_SESSION_FLAGS = FLAG_SPAM | FLAG_NONSPAM | FLAG_JUNKRECORDED;
    static final short SESSION_FLAGS = FLAG_ADDED  | FLAG_EXPUNGED | FLAG_IS_CONTACT |
                                       FLAG_RECENT | MUTABLE_SESSION_FLAGS;

    int   sequence;
    int   msgId;
    int   imapUid;
    int   flags;
    long  tags;
    short sflags;

    public ImapMessage(int id, byte type, int imapId, int flag, long tag) {
        msgId   = id;
        imapUid = imapId;
        flags   = flag;
        tags    = tag;
        sflags  = (type == MailItem.TYPE_CONTACT ? FLAG_IS_CONTACT : 0);
    }

    public ImapMessage(MailItem item) {
        this(item.getId(), item.getType(), item.getImapUid(), item.getFlagBitmask(), item.getTagBitmask());
    }

    ImapMessage(ImapMessage i4msg) {
        msgId   = i4msg.msgId;
        imapUid = i4msg.imapUid;
        flags   = i4msg.flags;
        tags    = i4msg.tags;
        sflags  = (short) (i4msg.sflags & FLAG_IS_CONTACT);
    }

    ImapMessage reset() {
        sflags &= FLAG_IS_CONTACT;
        return this;
    }

    byte getType() {
        return (sflags & FLAG_IS_CONTACT) == 0 ? MailItem.TYPE_MESSAGE : MailItem.TYPE_CONTACT;
    }

    boolean isExpunged()  { return (sflags & FLAG_EXPUNGED) != 0; }
    boolean isAdded()     { return (sflags & FLAG_ADDED) != 0; }

    void setExpunged(boolean expunged)  { sflags = (short) (expunged ? sflags | FLAG_EXPUNGED : sflags & ~FLAG_EXPUNGED); }
    void setAdded(boolean added)        { sflags = (short) (added ? sflags | FLAG_ADDED : sflags & ~FLAG_ADDED); }

    long getSize(MailItem item) throws ServiceException {
        if (item instanceof Message)
            return item.getSize();
        // FIXME: need to generate the representation of the item to do this correctly...
        return getContent(item).getFirst();
    }


    public int compareTo(ImapMessage i4msg) {
        if (imapUid == i4msg.imapUid)  return 0;
        return (imapUid < i4msg.imapUid ? -1 : 1);
    }

    static class SequenceComparator implements Comparator<ImapMessage> {
        public int compare(ImapMessage o1, ImapMessage o2) {
            if (o1 == null)       return o2 == null ? 0 : -1;
            else if (o2 == null)  return 1;
            return (o1.sequence < o2.sequence ? -1 : (o1.sequence == o2.sequence ? 0 : 1));
        }
    }


    private static final DateFormat GMT_DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z (z)", Locale.US);
        static { GMT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT")); }

    static final Pair<Long, InputStream> EMPTY_CONTENT = new Pair<Long, InputStream>(0L, new ByteArrayInputStream(new byte[0]));

    static Pair<Long, InputStream> getContent(MailItem item) throws ServiceException {
        if (item instanceof Message) {
            return new Pair<Long, InputStream>(item.getSize(), item.getContentStream());
        } else if (item instanceof Contact) {
            try {
                VCard vcard = VCard.formatContact((Contact) item);
                QCodec qcodec = new QCodec();  qcodec.setEncodeBlanks(true);
                StringBuilder header = new StringBuilder();
                header.append("Subject: ").append(qcodec.encode(vcard.fn, MimeConstants.P_CHARSET_UTF8)).append(ImapHandler.LINE_SEPARATOR);
                synchronized (GMT_DATE_FORMAT) {
                    header.append("Date: ").append(GMT_DATE_FORMAT.format(new Date(item.getDate()))).append(ImapHandler.LINE_SEPARATOR);
                }
                header.append("Content-Type: text/x-vcard; charset=\"utf-8\"").append(ImapHandler.LINE_SEPARATOR);
                header.append("Content-Transfer-Encoding: 8bit").append(ImapHandler.LINE_SEPARATOR);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(header.toString().getBytes(MimeConstants.P_CHARSET_ASCII));
                baos.write(ImapHandler.LINE_SEPARATOR_BYTES);
                baos.write(vcard.formatted.getBytes(MimeConstants.P_CHARSET_UTF8));
                return new Pair<Long, InputStream>((long) baos.size(), new ByteArrayInputStream(baos.toByteArray()));
            } catch (Exception e) {
                throw ServiceException.FAILURE("problems serializing contact " + item.getId(), e);
            }
        } else {
            return EMPTY_CONTENT;
        }
    }

    static MimeMessage getMimeMessage(MailItem item) throws ServiceException {
        if (item instanceof Message)
            return ((Message) item).getMimeMessage(false);

        InputStream is = getContent(item).getSecond();
        try {
            return new Mime.FixedMimeMessage(JMSession.getSession(), is);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error creating MimeMessage for " + MailItem.getNameForType(item.getType()) + ' ' + item.getId(), e);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    int getModseq(MailItem item, ImapFlagCache i4cache) {
        return Math.max(item.getModifiedSequence(), getFlagModseq(i4cache));
    }

    int getFlagModseq(ImapFlagCache i4cache) {
        int modseq = 0;
        long tagBuffer = tags;
        for (int i = 0; tagBuffer != 0 && i < 64; i++) {
            long mask = 1L << i;
            if ((tagBuffer & mask) != 0) {
                ImapFlag i4flag = i4cache.getByMask(mask);
                if (i4flag != null)
                    modseq = Math.max(modseq, i4flag.mModseq);
                tagBuffer &= ~mask;
            }
        }
        return modseq;
    }

    void setPermanentFlags(int f, long t, int changeId, ImapFolder parent) {
        if (t == tags && (f & IMAP_FLAGS) == (flags & IMAP_FLAGS))
            return;
        flags = (f & IMAP_FLAGS) | (flags & ~IMAP_FLAGS);
        tags  = t;
        if (parent != null)
            parent.dirtyMessage(this, changeId);
    }

    void setSessionFlags(short s, ImapFolder parent) {
        if ((s & MUTABLE_SESSION_FLAGS) == (sflags & MUTABLE_SESSION_FLAGS))
            return;
        sflags = (short) ((s & MUTABLE_SESSION_FLAGS) | (sflags & ~MUTABLE_SESSION_FLAGS));
        if (parent != null)
            parent.dirtyMessage(this, -1);
    }

    private static final String NO_FLAGS = "FLAGS ()";

    String getFlags(ImapFolder i4folder) {
        if ((flags & IMAP_FLAGS) == Flag.BITMASK_UNREAD && tags == 0 && sflags == 0)
            return NO_FLAGS;
        StringBuilder result = new StringBuilder("FLAGS (");
        int empty = result.length();

        if ((flags & Flag.BITMASK_DELETED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Deleted");
        if ((flags & Flag.BITMASK_DRAFT) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Draft");
        if ((flags & Flag.BITMASK_FLAGGED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Flagged");
        if ((flags & Flag.BITMASK_REPLIED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Answered");
        if ((flags & Flag.BITMASK_NOTIFIED) != 0)
            result.append(result.length() == empty ? "" : " ").append("$MDNSent");
        if ((flags & Flag.BITMASK_FORWARDED) != 0)
            result.append(result.length() == empty ? "" : " ").append("$Forwarded Forwarded");
        // note: \Seen is the IMAP flag, but we store "unread", so the test here is == not !=
        if ((flags & Flag.BITMASK_UNREAD) == 0)
            result.append(result.length() == empty ? "" : " ").append("\\Seen");

        if ((sflags & FLAG_RECENT) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Recent");
        if ((sflags & FLAG_SPAM) != 0)
            result.append(result.length() == empty ? "" : " ").append("$Junk Junk");
        if ((sflags & FLAG_NONSPAM) != 0)
            result.append(result.length() == empty ? "" : " ").append("$NotJunk NotJunk NonJunk");
        if ((sflags & FLAG_JUNKRECORDED) != 0)
            result.append(result.length() == empty ? "" : " ").append("JunkRecorded");

        long tagBuffer = tags;
        for (int i = 0; tagBuffer != 0 && i < 64; i++) {
            long mask = 1L << i;
            if ((tagBuffer & mask) != 0) {
                ImapFlag i4flag = i4folder.getTagByMask(mask);
                if (i4flag != null) {
                    // make sure there's no naming conflict with a system flag like "Forwarded" or "NonJunk"
                    ImapFlag other = i4folder.getFlagByName(i4flag.mImapName);
                    if (other == null || other == i4flag)
                        result.append(result.length() == empty ? "" : " ").append(i4flag);
                }
                tagBuffer &= ~mask;
            }
        }
        return result.append(')').toString();
    }

    private static final byte[] NIL = { 'N', 'I', 'L' };

    private static void nstring(PrintStream ps, String value) { if (value == null)  ps.write(NIL, 0, 3);  else astring(ps, value, false); }
    private static void astring(PrintStream ps, String value) { if (value == null)  ps.print("\"\"");     else astring(ps, value, false); }
    private static void aSTRING(PrintStream ps, String value) { if (value == null)  ps.print("\"\"");     else astring(ps, value, true); }
    private static void astring(PrintStream ps, String value, boolean upcase) {
        boolean literal = false;
        StringBuilder nonulls = null;
        int i = 0, lastNull = -1;
        for (int length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            if (c == '\0') {
                if (nonulls == null)  nonulls = new StringBuilder();
                nonulls.append(value.substring(lastNull + 1, i));
                lastNull = i;
            } else if (c == '"' || c == '\\' || c >= 0x7f || c < 0x20) {
                literal = true;
            }
        }
        String content = (nonulls == null ? value : nonulls.append(value.substring(lastNull + 1, i)).toString());
        if (upcase)
            content = content.toUpperCase();
        if (!literal) {
            ps.write('"');  ps.print(content);  ps.write('"');
        } else {
            try {
                byte[] bytes = content.getBytes(MimeConstants.P_CHARSET_UTF8);
                ps.write('{');  ps.print(bytes.length);  ps.write('}');  ps.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, 2);
                ps.write(bytes, 0, bytes.length);
            } catch (UnsupportedEncodingException uee) {
                ps.write(NIL, 0, 3);
            }
        }
    }

    private static void nstring2047(PrintStream ps, String value) {
        if (value == null) {
            ps.write(NIL, 0, 3);  return;
        }

        boolean encoded = false;
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\' || c >= 0x7f || c < 0x20)
                encoded = true;
        }
        if (!encoded) {
            ps.write('"');  ps.print(value);  ps.write('"');
        } else {
            try {
                // can't use QCodec because it doesn't encode '"', which results in bad quoted-strings
                ps.write('"');  ps.print(new BCodec().encode(value, "utf-8"));  ps.write('"');
            } catch (EncoderException ee) {
                ps.write(NIL, 0, 3);
            }
        }
    }

    private static void address(PrintStream ps, InternetAddress addr) {
        String address = addr.getAddress().trim(), route = null;
        int colon;
        // handle obsolete route-addr
        if (address.startsWith("@") && (colon = address.indexOf(':')) != -1) {
            route = address.substring(0, colon);  address = address.substring(colon + 1);
        }
        String[] parts = address.split("@", 2);
        ps.write('(');  nstring2047(ps, addr.getPersonal());
        ps.write(' ');  nstring(ps, route);
        ps.write(' ');  nstring(ps, parts[0]);
        ps.write(' ');  nstring(ps, parts.length > 1 ? parts[1] : null);
        ps.write(')');
    }

    private static void naddresses(PrintStream ps, InternetAddress[] addrs) {
        int count = 0;
        if (addrs != null && addrs.length > 0) {
            for (InternetAddress addr : addrs) {
                if (addr.isGroup()) {
                    // 7.4.2: "[RFC-2822] group syntax is indicated by a special form of address
                    //         structure in which the host name field is NIL.  If the mailbox name
                    //         field is also NIL, this is an end of group marker (semi-colon in RFC
                    //         822 syntax).  If the mailbox name field is non-NIL, this is a start of
                    //         group marker, and the mailbox name field holds the group name phrase."
                    try {
                        String serialized = addr.getAddress();
                        int colon = serialized.indexOf(':');
                        String name = colon == -1 ? serialized : serialized.substring(0, colon);
                        InternetAddress[] members = addr.getGroup(false);

                        if (count++ == 0)  ps.write('(');
                        ps.print("(NIL NIL ");  nstring(ps, name);  ps.print(" NIL)");
                        if (members != null) {
                            for (InternetAddress member : members)
                                address(ps, member);
                        }
                        ps.print("(NIL NIL NIL NIL)");
                    } catch (ParseException e) { }
                } else if (addr.getAddress() == null) {
                    continue;
                } else {
                    // 7.4.2: "The fields of an address structure are in the following order: personal
                    //         name, [SMTP] at-domain-list (source route), mailbox name, and host name."
                    if (count++ == 0)  ps.write('(');
                    address(ps, addr);
                }
            }
        }
        if (count == 0)  ps.write(NIL, 0, 3);
        else             ps.write(')');
    }

    private static void nlist(PrintStream ps, String[] list) {
        if (list == null || list.length == 0) {
            ps.print("NIL");
        } else if (list.length == 1) {
            astring(ps, list[0]);
        } else {
            ps.write('(');
            for (int i = 0; i < list.length; i++) {
                if (i != 0)  ps.write(' ');
                astring(ps, list[i]);
            }
            ps.write(')');
        }
    }

    private static void nparams(PrintStream ps, MimeCompoundHeader header) {
        boolean first = true;
        for (Iterator<Map.Entry<String, String>> it = header.getParameterIterator(); it.hasNext(); first = false) {
            Map.Entry<String, String> param = it.next();
            ps.print(first ? '(' : ' ');  aSTRING(ps, param.getKey());  ps.write(' ');  nstring2047(ps, param.getValue());
        }
        ps.print(first ? "NIL" : ")");
    }

    private static void ndisposition(PrintStream ps, String disposition) {
        if (disposition == null) {
            ps.print("NIL");
        } else {
            ContentDisposition cdisp = new ContentDisposition(disposition);
            ps.write('(');  astring(ps, cdisp.getValue());
            ps.write(' ');  nparams(ps, cdisp);
            ps.write(')');
        }
    }

    static void serializeEnvelope(PrintStream ps, MimeMessage mm) throws MessagingException {
        // 7.4.2: "The fields of the envelope structure are in the following order: date, subject,
        //         from, sender, reply-to, to, cc, bcc, in-reply-to, and message-id.  The date,
        //         subject, in-reply-to, and message-id fields are strings.  The from, sender,
        //         reply-to, to, cc, and bcc fields are parenthesized lists of address structures."
        InternetAddress[] from = Mime.parseAddressHeader(mm, "From", false);
        InternetAddress[] sender = Mime.parseAddressHeader(mm, "Sender", false), replyTo = Mime.parseAddressHeader(mm, "Reply-To", false);
        ps.write('(');  nstring(ps, mm.getHeader("Date", ","));
        ps.write(' ');  nstring2047(ps, Mime.getSubject(mm));
        ps.write(' ');  naddresses(ps, from);
        ps.write(' ');  naddresses(ps, sender.length == 0 ? from : sender);
        ps.write(' ');  naddresses(ps, replyTo.length == 0 ? from : replyTo);
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "To", false));
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "CC", false));
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "BCC", false));
        ps.write(' ');  nstring(ps, mm.getHeader("In-Reply-To", " "));
        ps.write(' ');  nstring(ps, mm.getMessageID());
        ps.write(')');
    }

    private static String nATOM(String value) { return value == null ? "NIL" : '"' + value.toUpperCase() + '"'; }

    static void serializeStructure(PrintStream ps, MimeMessage root, boolean extensions) throws IOException, MessagingException {
        LinkedList<LinkedList<MPartInfo>> queue = new LinkedList<LinkedList<MPartInfo>>();
        LinkedList<MPartInfo> level = new LinkedList<MPartInfo>();
        level.add(Mime.getParts(root).get(0));
        queue.add(level);

        boolean pop = false;
        while (!queue.isEmpty()) {
            level = queue.getLast();
            if (level.isEmpty()) {
                queue.removeLast();  pop = true;  continue;
            }

            MPartInfo mpart = level.getFirst();
            MimePart mp = mpart.getMimePart();
            boolean hasChildren = mpart.getChildren() != null && !mpart.getChildren().isEmpty();

            // we used to force unset charsets on text/plain parts to US-ASCII, but that always seemed unwise...
            ContentType ctype = new ContentType(mp.getHeader("Content-Type", null)).setValue(mpart.getContentType());
            String primary = nATOM(ctype.getPrimaryType()), subtype = nATOM(ctype.getSubType());

            if (!pop)
                ps.write('(');
            if (primary.equals("\"MULTIPART\"")) {
                if (!pop) {
                    // 7.4.2: "Multiple parts are indicated by parenthesis nesting.  Instead of a body type
                    //         as the first element of the parenthesized list, there is a sequence of one
                    //         or more nested body structures.  The second element of the parenthesized
                    //         list is the multipart subtype (mixed, digest, parallel, alternative, etc.)."
                    if (!hasChildren) {
                        ps.print("NIL");
                    } else {
                        queue.addLast(new LinkedList<MPartInfo>(mpart.getChildren()));
                        continue;
                    }
                }
                ps.write(' ');  ps.print(subtype);
                if (extensions) {
                    // 7.4.2: "Extension data follows the multipart subtype.  Extension data is never
                    //         returned with the BODY fetch, but can be returned with a BODYSTRUCTURE
                    //         fetch.  Extension data, if present, MUST be in the defined order.  The
                    //         extension data of a multipart body part are in the following order:
                    //         body parameter parenthesized list, body disposition, body language,
                    //         body location"
                    ps.write(' ');  nparams(ps, ctype);
                    ps.write(' ');  ndisposition(ps, mp.getHeader("Content-Disposition", null));
                    ps.write(' ');  nlist(ps, mp.getContentLanguage());
                    ps.write(' ');  nstring(ps, mp.getHeader("Content-Location", null));
                }
            } else {
                if (!pop) {
                    // 7.4.2: "The basic fields of a non-multipart body part are in the following order:
                    //         body type, body subtype, body parameter parenthesized list, body id, body
                    //         description, body encoding, body size."
                    String cte = mp.getEncoding();
                    cte = (cte == null || cte.trim().equals("") ? "7bit" : cte);
                    aSTRING(ps, ctype.getPrimaryType());  ps.write(' ');  aSTRING(ps, ctype.getSubType());
                    ps.write(' ');  nparams(ps, ctype);
                    ps.write(' ');  nstring(ps, mp.getContentID());
                    ps.write(' ');  nstring2047(ps, mp.getDescription());
                    ps.write(' ');  aSTRING(ps, cte);
                    ps.write(' ');  ps.print(Math.max(mp.getSize(), 0));
                }
                boolean rfc822 = primary.equals("\"MESSAGE\"") && subtype.equals("\"RFC822\"");
                if (rfc822) {
                    // 7.4.2: "A body type of type MESSAGE and subtype RFC822 contains, immediately
                    //         after the basic fields, the envelope structure, body structure, and
                    //         size in text lines of the encapsulated message."
                    if (!pop) {
                        if (!hasChildren) {
                            ps.print(" NIL NIL");
                        } else {
                            MimeMessage mm = (MimeMessage) mpart.getChildren().get(0).getMimePart();
                            ps.write(' ');  serializeEnvelope(ps, mm);  ps.write(' ');
                            queue.addLast(new LinkedList<MPartInfo>(mpart.getChildren()));
                            continue;
                        }
                    }
                    ps.write(' ');  ps.print(getLineCount(mp));
                } else if (primary.equals("\"TEXT\"")) {
                    // 7.4.2: "A body type of type TEXT contains, immediately after the basic fields, the
                    //         size of the body in text lines.  Note that this size is the size in its
                    //         content transfer encoding and not the resulting size after any decoding."
                    ps.write(' ');  ps.print(getLineCount(mp));
                }
                if (extensions) {
                    // 7.4.2: "Extension data follows the basic fields and the type-specific fields
                    //         listed above.  Extension data is never returned with the BODY fetch,
                    //         but can be returned with a BODYSTRUCTURE fetch.  Extension data, if
                    //         present, MUST be in the defined order.  The extension data of a
                    //         non-multipart body part are in the following order: body MD5, body
                    //         disposition, body language, body location"
                    ps.write(' ');  nstring(ps, mp.getContentMD5());
                    ps.write(' ');  ndisposition(ps, mp.getHeader("Content-Disposition", null));
                    ps.write(' ');  nlist(ps, mp.getContentLanguage());
                    ps.write(' ');  nstring(ps, mp.getHeader("Content-Location", null));
                }
            }
            ps.write(')');

            level.removeFirst();
            pop = false;
        }
    }

    private static int getLineCount(MimePart mp) {
        InputStream is = null;
        try {
            if (mp instanceof MimeBodyPart)      is = ((MimeBodyPart) mp).getRawInputStream();
            else if (mp instanceof MimeMessage)  is = ((MimeMessage) mp).getRawInputStream();
            else                                 return 0;

            int lines = 0, c;
            boolean complete = false;
            while ((c = is.read()) != -1) {
                if ((complete = (c == '\n')) == true)
                    lines++;
            }
            return complete ? lines : lines + 1;
        } catch (MessagingException e) {
            return 0;
        } catch (IOException e) {
            return 0;
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    public static void main(String[] args) {
        PrintStream ps = new PrintStream(System.out);
        ps.print(ImapHandler.LINE_SEPARATOR_BYTES);
        String[] samples = new String[] { null, "test", "\u0442", "ha\nnd", "\"dog\"", "ca\"t", "\0fr\0og\0" };
        for (String s : samples) {
            nstring2047(ps, s);  ps.write(' ');  nstring(ps, s);  ps.write(' ');  astring(ps, s);  ps.write(' ');  aSTRING(ps, s);  ps.write('\n');
        }

//        java.io.File dir = new java.io.File("C:\\Temp\\mail");
//        for (java.io.File file : dir.listFiles()) {
//            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new java.io.FileInputStream(file));
//            ByteArrayOutputStream baosold = new ByteArrayOutputStream(), baosnew = new ByteArrayOutputStream();
//            serializeStructure(new PrintStream(baosold), mm, true);
//            iterativeSerializeStructure(new PrintStream(baosnew), mm, true);
//            if (!(new String(baosold.toByteArray())).equals(new String(baosnew.toByteArray())))
//                System.out.println("mismatch on file: " + file.getName());
//            else
//                System.out.println("success on file: " + file.getName());
//        }
    }
}
