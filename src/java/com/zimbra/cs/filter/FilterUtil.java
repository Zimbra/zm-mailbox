/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.DateParser;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class FilterUtil {

    public static final DateParser SIEVE_DATE_PARSER = new DateParser("yyyyMMdd");

    public enum Condition {
        allof, anyof;

        public static Condition fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Condition.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(Condition.values()), e);
            }
        }

    }

    public enum Flag {
        read, flagged;

        public static Flag fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Flag.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(Flag.values()), e);
            }
        }
    }

    public enum StringComparison {
        is, contains, matches;

        public static StringComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return StringComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    }

    public enum NumberComparison {
        over, under;

        public static NumberComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return NumberComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(NumberComparison.values()), e);
            }
        }
    }

    public enum DateComparison {
        before, after;

        public static DateComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return DateComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    }

    /**
     * Returns a Sieve-escaped version of the given string.  Replaces <tt>\</tt> with
     * <tt>\\</tt> and <tt>&quot;</tt> with <tt>\&quot;</tt>.
     */
    public static String escape(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }

    /**
     * Removes escape characters from a Sieve string literal.
     */
    public static String unescape(String s) {
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        return s;
    }

    /**
     * Adds a value to the given <tt>Map</tt>.  If <tt>initialKey</tt> already
     * exists in the map, uses the next available index instead.  This way we
     * guarantee that we don't lose data if the client sends two elements with
     * the same index, or doesn't specify the index at all.
     *
     * @return the index used to insert the value
     */
    public static <T> int addToMap(Map<Integer, T> map, int initialKey, T value) {
        int i = initialKey;
        while (true) {
            if (!map.containsKey(i)) {
                map.put(i, value);
                return i;
            }
            i++;
        }
    }

    public static int getIndex(Element actionElement) {
        String s = actionElement.getAttribute(MailConstants.A_INDEX, "0");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            ZimbraLog.soap.warn("Unable to parse index value %s for element %s.  Ignoring order.",
                s, actionElement.getName());
            return 0;
        }
    }

    /**
     * Parses a Sieve size string and returns the integer value.
     * The string may end with <tt>K</tt> (kilobytes), <tt>M</tt> (megabytes)
     * or <tt>G</tt> (gigabytes).  If the units are not specified, the
     * value is in bytes.
     *
     * @throws NumberFormatException if the value cannot be parsed
     */
    public static int parseSize(String sizeString) {
        if (sizeString == null || sizeString.length() == 0) {
            return 0;
        }
        sizeString = sizeString.toUpperCase();
        int multiplier = 1;
        if (sizeString.endsWith("K")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024;
        } else if (sizeString.endsWith("M")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024;
        } else if (sizeString.endsWith("G")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024 * 1024;
        }
        return Integer.parseInt(sizeString) * multiplier;
    }

    /**
     * Adds a message to the given folder.  Handles both local folders and mountpoints.
     * @return the id of the new message, or <tt>null</tt> if it was a duplicate
     */
    public static ItemId addMessage(DeliveryContext context, Mailbox mbox, ParsedMessage pm, String recipient,
                                    String folderPath, int flags, String tags)
    throws ServiceException {
        // Do initial lookup.
        Pair<Folder, String> folderAndPath = mbox.getFolderByPathLongestMatch(
            null, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
        Folder folder = folderAndPath.getFirst();
        String remainingPath = folderAndPath.getSecond();
        ZimbraLog.filter.debug("Attempting to file to %s, remainingPath=%s.", folder, remainingPath);

        if (folder instanceof Mountpoint) {
            Mountpoint mountpoint = (Mountpoint) folder;
            ZMailbox remoteMbox = getRemoteZMailbox(mbox, mountpoint);

            // Look up remote folder.
            String remoteAccountId = mountpoint.getOwnerId();
            ItemId id = mountpoint.getTarget();
            ZFolder remoteFolder = remoteMbox.getFolderById(id.toString());
            if (remoteFolder != null) {
                if (remainingPath != null) {
                    remoteFolder = remoteFolder.getSubFolderByPath(remainingPath);
                    if (remoteFolder == null) {
                        String msg = String.format("Subfolder %s of mountpoint %s does not exist.",
                            remainingPath, mountpoint.getName());
                        throw ServiceException.FAILURE(msg, null);
                    }
                }
            }

            // File to remote folder.
            if (remoteFolder != null) {
                byte[] content = null;
                try {
                    content = pm.getRawData();
                } catch (Exception e) {
                    throw ServiceException.FAILURE("Unable to get message content", e);
                }
                String msgId = remoteMbox.addMessage(remoteFolder.getId(),
                    com.zimbra.cs.mailbox.Flag.bitmaskToFlags(flags),
                    null, 0, content, false);
                return new ItemId(msgId, remoteAccountId);
            } else {
                String msg = String.format("Unable to find remote folder %s for mountpoint %s.",
                    remainingPath, mountpoint.getName());
                throw ServiceException.FAILURE(msg, null);
            }
        } else {
            if (!StringUtil.isNullOrEmpty(remainingPath)) {
                // Only part of the folder path matched.  Auto-create the remaining path.
                ZimbraLog.filter.info("Could not find folder %s.  Automatically creating it.",
                    folderPath);
                folder = mbox.createFolder(null, folderPath, (byte) 0, MailItem.TYPE_MESSAGE);
            }
            try {
                Message msg = mbox.addMessage(null, pm, folder.getId(), false, flags, tags, recipient, context);
                if (msg == null) {
                    return null;
                } else {
                    return new ItemId(msg);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("Unable to add message", e);
            }
        }
    }

    /**
     * Returns a <tt>ZMailbox</tt> for the remote mailbox referenced by the given
     * <tt>Mountpoint</tt>.
     */
    public static ZMailbox getRemoteZMailbox(Mailbox localMbox, Mountpoint mountpoint)
    throws ServiceException {
        // Get auth token
        AuthToken authToken = null;
        OperationContext opCtxt = localMbox.getOperationContext();
        if (opCtxt != null) {
            authToken = opCtxt.getAuthToken();
        }
        if (authToken == null) {
            authToken = AuthProvider.getAuthToken(localMbox.getAccount());
        }

        // Get ZMailbox
        Account account = Provisioning.getInstance().get(AccountBy.id, mountpoint.getOwnerId());
        ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(account));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(account.getId());
        zoptions.setTargetAccountBy(AccountBy.id);
        return ZMailbox.getMailbox(zoptions);
    }

    public static final String HEADER_FORWARDED = "X-Zimbra-Forwarded";

    public static void redirect(Mailbox sourceMbox, MimeMessage msg, String destinationAddress)
    throws ServiceException {
        MimeMessage outgoingMsg;

        try {
            if (!isMailLoop(sourceMbox, msg)) {
                outgoingMsg = new Mime.FixedMimeMessage(msg);
                outgoingMsg.setHeader(HEADER_FORWARDED, sourceMbox.getAccount().getName());
                outgoingMsg.saveChanges();
            } else {
                String error = String.format("Detected a mail loop for message %s.", Mime.getMessageID(msg));
                throw ServiceException.FAILURE(error, null);
            }
        } catch (MessagingException e) {
            try {
                // If MimeMessage.saveChanges fails, create a copy of the message
                // that doesn't recursively call updateHeaders() upon saveChanges().
                // This uses double the memory on malformed messages, but it should
                // avoid having a deep-buried misparse throw off the whole forward.
                // TODO: With JavaMail 1.4.3, this workaround might not be needed any more.
                outgoingMsg = new Mime.FixedMimeMessage(msg) {
                    @Override
                    protected void updateHeaders() throws MessagingException {
                        setHeader("MIME-Version", "1.0");
                        updateMessageID();
                    }
                };
                ZimbraLog.filter.info("Message format error detected.  Wrapper class in use.  %s", e.toString());
            } catch (MessagingException again) {
                throw ServiceException.FAILURE("Message format error detected.  Workaround failed.", again);
            }
        }

        MailSender sender = sourceMbox.getMailSender().setSaveToSent(false).setSkipSendAsCheck(true);
        if (Provisioning.getInstance().getLocalServer().isMailRedirectSetEnvelopeSender()) {
            // Set envelope sender to the account name (bug 31309).
            Account account = sourceMbox.getAccount();
            sender.setEnvelopeFrom(account.getName());
        } else {
            try {
                Address from = ArrayUtil.getFirstElement(outgoingMsg.getFrom());
                if (from != null) {
                    sender.setEnvelopeFrom(((InternetAddress) from).getAddress());
                }
            } catch (MessagingException e) {
                ZimbraLog.filter.warn("Unable to determine From header value.  " +
                    "Envelope sender will be set to the default value.", e);
            }
        }
        sender.setRecipients(destinationAddress);
        sender.sendMimeMessage(null, sourceMbox, outgoingMsg);
    }

    /**
     * Returns <tt>true</tt> if the current account's name is
     * specified in one of the X-Zimbra-Forwarded headers.
     */
    private static boolean isMailLoop(Mailbox sourceMbox, MimeMessage msg)
    throws ServiceException {
        String[] forwards = Mime.getHeaders(msg, HEADER_FORWARDED);
        String userName = sourceMbox.getAccount().getName();
        for (String forward : forwards) {
            if (StringUtil.equal(userName, forward)) {
                return true;
            }
        }
        return false;
    }
}

