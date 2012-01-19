/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.mail.internet.MimeMessage;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mime.HeaderUtils;
import com.zimbra.common.mime.MimeHeader;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;

/** Manages message threading into {@link Conversation}s based on the {@link
 *  Account} attribute {@link Provisioning#A_zimbraMailThreadingAlgorithm}.
 *  <p>
 *  If their threading mode is {@link MailThreadingAlgorithm#none}, no
 *  conversation threading is performed.
 *  <p>
 *  If their threading mode is {@link MailThreadingAlgorithm#subject}, the
 *  message will be threaded based solely on its normalized subject.
 *  <p>
 *  If their threading mode is {@link MailThreadingAlgorithm#strict}, only the
 *  threading message headers ({@code References}, {@code In-Reply-To}, {@code
 *  Message-ID}, and {@code Resent-Message-ID}) are used to correlate messages.
 *  No checking of normalized subjects is performed.
 *  <p>
 *  If their threading mode is {@link MailThreadingAlgorithm#references}, the
 *  same logic as {@link MailThreadingAlgorithm#strict} applies with the
 *  constraints slightly loosened so that the non-standard {@code Thread-Index}
 *  header is considered when threading messages and that a reply message
 *  lacking {@code References} and {@code In-Reply-To} headers will fall back
 *  to using subject-based threading.
 *  <p>
 *  If their threading mode is {@link MailThreadingAlgorithm#subjrefs}, the
 *  same logic as {@link MailThreadingAlgorithm#references} applies with the
 *  caveat that modifications to the subject will break a thread in two. */
public final class Threader {
    // references-based threading:
    //   - check OPEN_CONVERSATION for Message-Id, In-Reply-To, References
    //     - 0 matches: fall back to normal threading
    //     - 1 match: target conversation (also check subject?)
    //     - >1 matches: merge other convs into largest match
    //   - if vconv -> conv, shift open rows
    //   - once done, add all ids to OPEN_CONV pointing at target conv (may be vconv)

    private final MailThreadingAlgorithm mode;
    private final Mailbox mbox;
    private final ParsedMessage pm;
    private final String subjHash;
    private List<String> refHashes;
    private List<Conversation> matchedConversations;

    public Threader(Mailbox mbox, ParsedMessage pm) throws ServiceException {
        this.mbox = mbox;
        this.pm = pm;
        this.mode = getThreadingAlgorithm(mbox.getAccount());
        this.subjHash = isEnabled() ? Mailbox.getHash(pm.getNormalizedSubject()) : null;
        this.refHashes = isEnabled() && !mode.isSubject() ? getReferenceHashes(true) : null;
    }

    /** Retrieves the current algorithm used for threading new messages.
     *  Threading can be disabled on a system-wide basis via the localconfig
     *  key {@code debug_disable_conversation}.  If threading has not been
     *  disabled, the threading mode is retrieved from the {@link Account}
     *  attribute {@link Provisioning#A_zimbraMailThreadingAlgorithm}.
     *  If this attribute is unset, we default to {@link
     *  MailThreadingAlgorithm#references}.
     * @see DebugConfig#disableConversation */
    private static MailThreadingAlgorithm getThreadingAlgorithm(Account acct) {
        if (DebugConfig.disableConversation) {
            return MailThreadingAlgorithm.none;
        }

        MailThreadingAlgorithm tmode = acct.getMailThreadingAlgorithm();
        return tmode == null ? MailThreadingAlgorithm.references : tmode;
    }

    /** Returns the SHA-1 hashes for all of the {@code Threader}'s message's
     *  threading header values.  This always includes the contents of its
     *  standard threading headers (the message-ids from {@code Messsage-ID},
     *  {@code In-Reply-To}, and {@code References}).  If the threading mode
     *  is not {@link MailThreadingAlgorithm#strict}, we also include hashes
     *  based on the non-standard {@code Thread-Index} header, useful for
     *  interoperability with Microsoft Outlook and siblings.  We prepend two
     *  control characters to these values before generating the hashes to
     *  avoid OPEN_CONVERSATION conflicts with hashed normalized subjects.
     * @param includeParents  If {@code true}, includes the hashes for all
     *                        threading headers in the returned {@code List}.
     *                        If {@code false}, hashes only the {@code
     *                        Message-ID} header and (optionally) {@code
     *                        Thread-Index}.
     * @see Mailbox#getHash(String)
     * @see #getThreadIndexHashes(boolean)
     * @return a mutable {@code List} containing the base64-encoded hashes. */
    private List<String> getReferenceHashes(boolean includeParents) {
        List<String> hashes = new ArrayList<String>();

        Collection<String> references = includeParents ? pm.getAllReferences() : Mime.getReferences(pm.getMimeMessage(), "Message-ID");
        for (String reference : references) {
            hashes.add(Mailbox.getHash("\u0001\u0002" + Strings.nullToEmpty(reference)));
        }

        // in "strict" mode, we ignore the Thread-Index header
        if (!mode.isStrict()) {
            hashes.addAll(getThreadIndexHashes(true));
        }

        return hashes;
    }

    /** Returns the SHA-1 hashes for the {@code Threader}'s message's {@code
     *  Thread-Index} header and those derived from it for all its parents.
     *  We prepend two control characters to these values before generating
     *  the hashes to avoid OPEN_CONVERSATION conflicts with hashed normalized
     *  subjects.
     * @param includeParents  If {@code true}, includes the hashes for parent
     *                        messages in the returned {@code List}.  If {@code
     *                        false}, returns only this message's hash.
     * @see ThreadIndex */
    private List<String> getThreadIndexHashes(boolean includeParents) {
        byte[] tindex = null;
        try {
            tindex = ThreadIndex.parseHeader(pm.getMimeMessage().getHeader("Thread-Index", null));
        } catch (Exception e) { }
        if (tindex == null)
            return Collections.emptyList();

        List<String> hashes = new ArrayList<String>(3);
        // technically shouldn't be shorter than 22 bytes, but always take 1 pass just in case
        int length = tindex.length;
        do {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update((byte) 1);  md.update((byte) 3);
                md.update(tindex, 0, length);
                hashes.add(ByteUtil.encodeFSSafeBase64(md.digest()));
            } catch (NoSuchAlgorithmException e) {
                return Collections.emptyList();
            }
            length -= 5;
        } while (length >= 22 && includeParents);
        return hashes;
    }

    public static class ThreadIndex {
        /** Generates a new {@code Thread-Index} value suitable for adding as
         *  a header for a message that is not a reply to a parent containing
         *  a {@code Thread-Index} header.  This value is a base64-encoded
         *  22-byte random value and has no relation to the message. */
        public static String newThreadIndex() {
            byte[] random = new byte[22];
            new Random().nextBytes(random);
            return HeaderUtils.encodeB2047(random).trim();
        }

        /** Generates a new {@code Thread-Index} value suitable for adding as
         *  a header for a reply to a parent containing a {@code Thread-Index}
         *  header.  This value is created by appending 5 random bytes to the
         *  end of the parent's decoded {@code Thread-Index} value. */
        public static String addChild(byte[] oldTIndex) {
            if (oldTIndex == null) {
                return null;
            }

            int oldLength = oldTIndex.length;
            byte[] tindex = new byte[oldLength + 5], random = new byte[5];
            new Random().nextBytes(random);
            System.arraycopy(oldTIndex, 0, tindex, 0, oldLength);
            System.arraycopy(random, 0, tindex, oldLength, 5);
            return HeaderUtils.encodeB2047(tindex).trim();
        }

        /** Parses the contents of a {@code Thread-Index} header and returns
         *  the base64-decoded thread-index {@code byte} array.  Such arrays
         *  must be of length 22, 27, 32, 37, 42, etc.; if the decoded array
         *  does not match the pattern, {@code null} is returned instead. */
        public static byte[] parseHeader(String tidxHdr) {
            if (tidxHdr == null || tidxHdr.trim().isEmpty()) {
                return null;
            }

            byte[] tindex = HeaderUtils.decodeB2047(tidxHdr.trim());
            if (tindex.length % 5 != 2) {
                ZimbraLog.mailbox.debug("  ignoring Thread-Index of decoded length %d", tindex.length);
                return null;
            }
            return tindex;
        }

        /** Generates a new {@code Thread-Topic} value suitable for adding as
         *  a header for a message that is not a reply to a parent containing
         *  a {@code Thread-Topic} header.  This value is the normalized
         *  version of the message's {@code Subject} header (i.e. with all
         *  prefixes and trailers stripped).
         * @see ParsedMessage#normalize(String) */
        public static String newThreadTopic(String subject) {
            String ttopic = ParsedMessage.normalize(subject);
            return ttopic == null ? null : MimeHeader.escape(ttopic, null, false);
        }
    }


    /** Returns whether threading is enabled for this {@code Mailbox}. */
    boolean isEnabled() {
        return !mode.isNone();
    }

    /** Returns whether the regular old-message mailbox purge task should also
     *  trim old entries from the OPEN_CONVERSATION table for the given {@code
     *  Mailbox}.  In general, when we thread using references as a criterion,
     *  we don't want to remove older entries. */
    static boolean isHashPurgeAllowed(Account acct) {
        MailThreadingAlgorithm tmode = getThreadingAlgorithm(acct);
        return tmode.isNone() || tmode.isSubject();
    }

    /** Searches for an existing {@code Conversation} matching the message
     *  being threaded.  If the user's threading mode is {@code subject}, this
     *  method always returns an empty list.  If the user's threading mode is
     *  {@code subject}, only the normalized subject is used for threading and
     *  only one {@code Conversation} will ever be returned.  For all other
     *  threading modes, 0, 1 or multiple {@code Conversation}s may be returned;
     *  in the multi-{@code Conversation} case, the caller is responsible for
     *  choosing one or merging them all. */
    List<Conversation> lookupConversation() throws ServiceException {
        if (matchedConversations == null) {
            if (mode.isNone()) {
                return Collections.emptyList();
            }
            ZimbraLog.mailbox.debug("  threading message \"%s\" (%s)", pm.getSubject(), pm.getMessageID());

            List<Conversation> matches = Collections.emptyList();
            if (!mode.isSubject()) {
                // check to see if there's a conversation matching this message's references
                matches = lookupByReference();
            }
            if (matches.isEmpty() && (mode.isSubject() || (!mode.isStrict() && isReplyWithoutReferences()))) {
                // check for an existing open conversation with the same normalized subject
                matches = lookupBySubject();
            }
            matchedConversations = matches;
        }
        return new ArrayList<Conversation>(matchedConversations);
    }

    /** Clears cached threader results.  Matching conversations will be
     *  recalculated the next time {@link #lookupConversation()} is called. */
    void reset() {
        matchedConversations = null;
    }

    /** Returns whether the message being threaded has a subject that looks
     *  like a reply but has no {@code In-Reply-To} or {@code References}
     *  header.  Several popular mailers do this, including at least some
     *  versions of Outlook and Lotus Notes. */
    private boolean isReplyWithoutReferences() {
        if (!pm.isReply()) {
            return false;
        }

        MimeMessage mm = pm.getMimeMessage();
        return Mime.getReferences(mm, "In-Reply-To").isEmpty() && Mime.getReferences(mm, "References").isEmpty();
    }

    /** Searches the {@code OPEN_CONVERSATION} table for existing {@code
     *  Conversation}s matching this new message's threading headers.  We
     *  thread on {@code References}, {@code In-Reply-To}, {@code Message-ID}
     *  (which also correlates mailing list duplicates), and {@code
     *  Resent-Message-ID}.  If the user's threading algorithm is not
     *  {@link MailThreadingAlgorithm#strict}, we also thread on {@code
     *  Thread-Index}.
     * @return a list of matching {@code Conversation}s
     */
    private List<Conversation> lookupByReference() throws ServiceException {
        if (refHashes == null || refHashes.isEmpty()) {
            return Collections.emptyList();
        }
        ZimbraLog.mailbox.debug("  lookup by references (%s): %s", mode, refHashes);
        List<MailItem.UnderlyingData> dlist = DbMailItem.getByHashes(mbox, refHashes);
        if (dlist == null || dlist.isEmpty()) {
            ZimbraLog.mailbox.debug("  no reference matches found");
            return Collections.emptyList();
        }

        List<Conversation> matches = new ArrayList<Conversation>(dlist.size());
        for (MailItem.UnderlyingData data : dlist) {
            if (data.type == MailItem.Type.CONVERSATION.toByte()) {
                matches.add(mbox.getConversation(data));
            } else {
                matches.add((Conversation) mbox.getMessage(data).getParent());
            }
        }
        ZimbraLog.mailbox.debug("  found %d reference match(es)", matches.size());

        if (mode.isSubjrefs()) {
            // constrain the matches to those with the same normalized subject
            for (int i = matches.size() - 1; i >= 0; i--) {
                Conversation hit = matches.get(i);
                if (!pm.getNormalizedSubject().equals(hit.getNormalizedSubject())) {
                    ZimbraLog.mailbox.debug("  dropping one reference match due to non-matching subjects");
                    matches.remove(i);
                    // need to trim refHashes so as not to overwrite the hashes that mapped to the other subject's thread
                    // FIXME: this trimming may be a little too aggressive, may miss some out-of-order delivery cases
                    refHashes = getReferenceHashes(false);
                }
            }
            if (matches.isEmpty()) {
                ZimbraLog.mailbox.debug("  no valid reference matches found");
            }
        }
        assert matches != null;
        return matches;
    }

    /** The number of milliseconds of inactivity (i.e. time since last message
     *  receipt) after which a conversation is considered "closed". */
    private static final long CONVERSATION_REPLY_WINDOW = Constants.MILLIS_PER_MONTH;

    /** The number of milliseconds of inactivity (i.e. time since last message
     *  receipt) after which a non-reply is not grouped with an existing
     *  conversation with the same subject. */
    private static final long CONVERSATION_NONREPLY_WINDOW = 2 * Constants.MILLIS_PER_DAY;

    /** The maximum size for a conversation beyond which non-reply messages
     *  are not grouped with it, even if their delivery time is within
     *  {@link #CONVERSATION_NONREPLY_WINDOW}. */
    private static final int CONVERSATION_NONREPLY_SIZE_LIMIT = 50;

    /** Searches the {@code OPEN_CONVERSATION} table for existing {@code
     *  Conversation}s matching this new message's normalized subject.
     *  A potential match may be discarded as stale if its latest message was
     *  added too long ago, or may be discarded if the new message is not a
     *  reply and the match is already too large.
     * @return a list containing one matching {@code Conversation}s
     * @see #CONVERSATION_REPLY_WINDOW
     * @see #CONVERSATION_NONREPLY_WINDOW
     * @see #CONVERSATION_NONREPLY_SIZE_LIMIT */
    private List<Conversation> lookupBySubject() throws ServiceException {
        if (subjHash == null) {
            return null;
        }

        ZimbraLog.mailbox.debug("  lookup by subject (%s): %s", mode, pm.getNormalizedSubject());
        Conversation conv = mbox.getConversationByHash(subjHash);
        if (conv == null) {
            ZimbraLog.mailbox.debug("  no subject matches found");
            return Collections.emptyList();
        }

        ZimbraLog.mailbox.debug("  found conversation %d for subject hash: %s", conv.getId(), subjHash);

        // the caller can specify the received date via ParsedMessge constructor or X-Zimbra-Received header
        long window = pm.isReply() ? CONVERSATION_REPLY_WINDOW : CONVERSATION_NONREPLY_WINDOW;
        if (pm.getReceivedDate() > conv.getDate() + window) {
            // if the last message in the conv was more than 1 month ago, it's probably not related...
            ZimbraLog.mailbox.debug("  but rejected it because it's too old");
            return Collections.emptyList();
        }

        if (!pm.isReply() && conv.getSize() > CONVERSATION_NONREPLY_SIZE_LIMIT) {
            // put a cap on the number of non-reply messages accumulating in a conversation
            ZimbraLog.mailbox.debug("  but rejected it because it's too big to add a non-reply");
            return Collections.emptyList();
        }

        return Lists.newArrayList(conv);
    }

    /** Updates the {@code OPEN_CONVERSATION} database table to force all of
     *  the new message's hashes to point at its {@code Conversation}.  This
     *  includes the subject hash (if the user's threading algorithm is not
     *  {@link MailThreadingAlgorithm#strict}) and the reference hashes (if
     *  the threading algorithm isn't {@link MailThreadingAlgorithm#subject}).
     *  If threading is disabled for the {@code Mailbox}, this method does
     *  nothing.
     * @param conv  The threaded message's target {@code Conversation}.
     * @see #getReferenceHashes(boolean) */
    void recordAddedMessage(Conversation conv) throws ServiceException {
        if (conv == null)
            return;

        if (subjHash != null) {
            // use the Mailbox version in order to update Mailbox.mConvHashes
            mbox.openConversation(conv, subjHash);
        }

        if (refHashes != null) {
            for (String refHash : refHashes) {
                conv.open(refHash);
            }
        }
    }

    /** Updates the {@code OPEN_CONVERSATION} database table to redirect any
     *  threading hashes currently pointing at the {@code Message} to instead
     *  refer to the given {@code Conversation}. */
    void changeThreadingTargets(Message msg, Conversation conv) throws ServiceException {
        if (conv != null && msg != null && isEnabled() && !mode.isSubject()) {
            DbMailItem.changeOpenTargets(msg, conv.getId());
            ZimbraLog.mailbox.debug("  transferred hashes from message %d to conv %d", msg.getId(), conv.getId());
        }
    }

    @Override
    public String toString() {
        Objects.ToStringHelper tostr = Objects.toStringHelper(this);
        tostr.add("mode", mode);
        if (subjHash != null) {
            tostr.add("subjHash", subjHash);
        }
        if (refHashes != null) {
            tostr.add("refHashes", refHashes);
        }
        return tostr.toString();
    }
}
