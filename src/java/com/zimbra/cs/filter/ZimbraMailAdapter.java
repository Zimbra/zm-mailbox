/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.cs.filter.jsieve.ActionNotify;
import com.zimbra.cs.filter.jsieve.ActionReply;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionRedirect;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.MailUtils;
import org.apache.jsieve.mail.SieveMailException;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ActionTag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Sieve evaluation engine adds a list of {@link org.apache.jsieve.mail.Action}s
 * that have matched the filter conditions to this object
 * and invokes its {@link #executeActions()} method.
 */
public class ZimbraMailAdapter implements MailAdapter {
    private Mailbox mailbox;
    private FilterHandler handler;
    private String[] tags;
    private boolean allowFilterToMountpoint = true;

    /**
     * Keeps track of folders into which we filed messages, so we don't file twice
     * (RFC 3028 2.10.3).
     */
    private Set<String> filedIntoPaths = new HashSet<String>();

    /**
     * Set of address headers that need to be processed for IDN.
     */
    private static Set<String> addrHdrs = ImmutableSet.of("from", "sender", "to", "bcc", "cc", "reply-to");

    /**
     * List of Actions to perform.
     */
    private List<Action> actions = new ArrayList<Action>();

    /**
     * Ids of messages that have been added.
     */
    protected List<ItemId> addedMessageIds = new ArrayList<ItemId>();

    private SieveContext context;

    private boolean discardActionPresent = false;

    public ZimbraMailAdapter(Mailbox mailbox, FilterHandler handler) {
        this.mailbox = mailbox;
        this.handler = handler;
    }

    public void setAllowFilterToMountpoint(boolean allowFilterToMountpoint) {
        this.allowFilterToMountpoint = allowFilterToMountpoint;
    }

    /**
     * Returns the {@link Message} we are filtering, or {@code null} if not available.
     */
    public Message getMessage() {
        try {
            return handler.getMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get Message", e);
        }
        return null;
    }

    /**
     * Returns the <tt>ParsedMessage</tt>, or <tt>null</tt> if it is not available.
     */
    public ParsedMessage getParsedMessage() {
        try {
            return handler.getParsedMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get ParsedMessage.", e);
        }
        return null;
    }

    /**
     * Returns the <tt>MimeMessage</tt>, or <tt>null</tt> if it is not available.
     */
    public MimeMessage getMimeMessage() {
        try {
            return handler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
        }
        return null;
    }

    /**
     * <p>Sets the context for the current sieve script execution.</p>
     * <p>Sieve engines <code>MUST</code> set this property before any calls
     * related to the execution of a script are made.</p>
     * <p>Implementations intended to be shared between separate threads of
     * execution <code>MUST</code> ensure that they manage concurrency contexts,
     * for example by storage in a thread local variable. Engines <code>MUST</code>
     * - for a script execution - ensure that all calls are made within the
     * same thread of execution.</p>
     *
     * @param context the current context,
     *                or null to clear the contest once the execution of a script has completed.
     */
    @Override
    public void setContext(SieveContext context) {
        this.context = context;
    }

    /**
     * Returns the List of actions.
     */
    @Override
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Adds an Action.
     * @param action The action to set
     */
    @Override
    public void addAction(Action action) {
        actions.add(action);
    }

    @Override
    public void executeActions() throws SieveException {
        try {
            handler.beforeFiltering();

            String messageId = Mime.getMessageID(handler.getMimeMessage());

            // If the Sieve script has no actions, JSieve generates an implicit keep.  If
            // the script contains a single discard action, JSieve returns an empty list.
            if (getActions().size() == 0) {
                ZimbraLog.filter.info("Discarding message with Message-ID %s from %s",
                    messageId, Mime.getSender(handler.getMimeMessage()));
                handler.discard();
                return;
            }

            if (getDeliveryActions().isEmpty()) {
                // i.e. no keep/fileinto/redirect actions
                if (getReplyNotifyActions().isEmpty()) {
                    // if only flag/tag actions are present, we keep the message even if discard
                    // action is present
                    explicitKeep();
                } else if (!discardActionPresent) {
                    // else if reply/notify actions are present and there's no discard, do sort
                    // of implicit keep
                    explicitKeep();
                }
            }

            for (Action action : actions) {
                if (action instanceof ActionKeep) {
                    if (context == null) {
                        ZimbraLog.filter.warn("SieveContext has unexpectedly not been set");
                        doDefaultFiling();
                    } else if (context.getCommandStateManager().isImplicitKeep()) {
                        // implicit keep: this means that none of the user's rules have been matched
                        // we need to check system spam filter to see if the mail is spam
                        doDefaultFiling();
                    } else {
                        explicitKeep();
                    }
                } else if (action instanceof ActionFileInto) {
                    ActionFileInto fileinto = (ActionFileInto) action;
                    String folderPath = fileinto.getDestination();
                    try {
                        if (!allowFilterToMountpoint && isMountpoint(mailbox, folderPath)) {
                            ZimbraLog.filter.info("Filing to mountpoint \"%s\" is not allowed.  Filing to the default folder instead.",
                                                  folderPath);
                            explicitKeep();
                        } else {
                            fileInto(folderPath);
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.filter.info("Unable to file message to %s.  Filing to %s instead.",
                                              folderPath, handler.getDefaultFolderPath(), e);
                        explicitKeep();
                    }
                } else if (action instanceof ActionRedirect) {
                    // redirect mail to another address
                    ActionRedirect redirect = (ActionRedirect) action;
                    String addr = redirect.getAddress();
                    ZimbraLog.filter.info("Redirecting message to %s.", addr);
                    try {
                        handler.redirect(addr);
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to redirect to %s.  Filing message to %s.",
                                              addr, handler.getDefaultFolderPath(), e);
                        explicitKeep();
                    }
                } else if (action instanceof ActionReply) {
                    // reply to mail
                    ActionReply reply = (ActionReply) action;
                    ZimbraLog.filter.debug("Replying to message");
                    try {
                        handler.reply(reply.getBodyTemplate());
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to reply.", e);
                        explicitKeep();
                    }
                } else if (action instanceof ActionNotify) {
                    ActionNotify notify = (ActionNotify) action;
                    ZimbraLog.filter.debug("Sending notification message to %s.", notify.getEmailAddr());
                    try {
                        handler.notify(notify.getEmailAddr(),
                                       notify.getSubjectTemplate(),
                                       notify.getBodyTemplate(),
                                       notify.getMaxBodyBytes(),
                                       notify.getOrigHeaders());
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to notify.", e);
                        explicitKeep();
                    }
                }
            }

            handler.afterFiltering();
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
    }

    private static boolean isMountpoint(Mailbox mbox, String folderPath)
    throws ServiceException {
        Pair<Folder, String> pair = mbox.getFolderByPathLongestMatch(null, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
        Folder f = pair.getFirst();
        return f != null && f instanceof Mountpoint;
    }

    private List<Action> getDeliveryActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : this.actions) {
            if (action instanceof ActionKeep ||
                action instanceof ActionFileInto ||
                action instanceof ActionRedirect) {
                actions.add(action);
            }
        }
        return actions;
    }

    private List<ActionTag> getTagActions() {
        List<ActionTag> actions = new ArrayList<ActionTag>();
        for (Action action : this.actions) {
            if (action instanceof ActionTag) {
                actions.add((ActionTag) action);
            }
        }
        return actions;
    }

    private List<ActionFlag> getFlagActions() {
        List<ActionFlag> actions = new ArrayList<ActionFlag>();
        for (Action action : this.actions) {
            if (action instanceof ActionFlag) {
                actions.add((ActionFlag) action);
            }
        }
        return actions;
    }

    private List<Action> getReplyNotifyActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : this.actions) {
            if (action instanceof ActionReply || action instanceof ActionNotify) {
                actions.add(action);
            }
        }
        return actions;
    }

    /**
     * Files the message into the inbox or spam folder.  Keeps track
     * of the folder path, to make sure we don't file to the same
     * folder twice.
     */
    public Message doDefaultFiling()
    throws ServiceException {
        String folderPath = handler.getDefaultFolderPath();
        Message msg = null;
        if (filedIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            msg = handler.implicitKeep(getFlagActions(), getTags());
            if (msg != null) {
                filedIntoPaths.add(folderPath);
                addedMessageIds.add(new ItemId(msg));
            }
        }
        return msg;
    }

    /**
     * Files the message into the inbox folder.  Keeps track
     * of the folder path, to make sure we don't file to the same
     * folder twice.
     */
    private Message explicitKeep()
    throws ServiceException {
        String folderPath = handler.getDefaultFolderPath();
        if (ZimbraLog.filter.isDebugEnabled()) {
            ZimbraLog.filter.debug(
                    appendFlagTagActionsInfo(
                            "Explicit keep - fileinto " + folderPath, getFlagActions(), getTagActions()));
        }
        Message msg = null;
        if (filedIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            msg = handler.explicitKeep(getFlagActions(), getTags());
            if (msg != null) {
                filedIntoPaths.add(folderPath);
                addedMessageIds.add(new ItemId(msg));
            }
        }
        return msg;
    }

    /**
     * Files the message into the given folder, as a result of an explicit
     * fileinto filter action.  Keeps track of the folder path, to make
     * sure we don't file to the same folder twice.
     */
    private void fileInto(String folderPath)
    throws ServiceException {
        if (ZimbraLog.filter.isDebugEnabled()) {
            ZimbraLog.filter.debug(
                    appendFlagTagActionsInfo("fileinto " + folderPath, getFlagActions(), getTagActions()));
        }
        if (filedIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            ItemId id = handler.fileInto(folderPath, getFlagActions(), getTags());
            if (id != null) {
                filedIntoPaths.add(folderPath);
                addedMessageIds.add(id);
            }
        }
    }

    private static String appendFlagTagActionsInfo(
            String deliveryActionInfo, Collection<ActionFlag> flagActions,  Collection<ActionTag> tagActions) {
        StringBuilder builder = new StringBuilder(deliveryActionInfo);
        for (ActionFlag flagAction : flagActions) {
            builder.append(",Flag ").append(flagAction.getName());
        }
        for (ActionTag tagAction : tagActions) {
            builder.append(",Tag ").append(tagAction.getTagName());
        }
        return builder.toString();
    }

    private String[] getTags() {
        if (tags == null) {
            List<String> taglist = Lists.newArrayList();
            for (Action action : getTagActions()) {
                taglist.add(((ActionTag) action).getTagName());
            }
            tags = taglist.toArray(new String[taglist.size()]);
        }
        return tags;
    }

    private List<String> handleIDN(String headerName, String[] headers) {

        List<String> hdrs = new ArrayList<String>();
        for (String header : headers) {
            boolean altered = false;

            if (header.contains(IDNUtil.ACE_PREFIX)) {
                // handle multiple addresses in a header
                StringTokenizer st = new StringTokenizer(header, ",;", true);
                StringBuffer addrs = new StringBuffer();
                while (st.hasMoreTokens()) {
                    String address = st.nextToken();
                    String delim = st.hasMoreTokens() ? st.nextToken() : "";
                    try {
                        javax.mail.internet.InternetAddress inetAddr = new JavaMailInternetAddress(address);
                        String addr = inetAddr.getAddress();
                        String unicodeAddr = IDNUtil.toUnicode(addr);
                        if (unicodeAddr.equalsIgnoreCase(addr)) {
                            addrs.append(address).append(delim);
                        } else {
                            altered = true;
                            // put the unicode addr back to the address
                            inetAddr.setAddress(unicodeAddr);
                            addrs.append(inetAddr.toString()).append(delim);
                        }
                    } catch (AddressException e) {
                        ZimbraLog.filter.warn("handleIDN encountered invalid address " + address + "in header " + headerName);
                        addrs.append(address).append(delim);  // put back the orig address
                    }
                }

                // if altered, add the altered value
                if (altered) {
                    String unicodeAddrs = addrs.toString();
                    ZimbraLog.filter.debug("handleIDN added value " + unicodeAddrs + " for header " + headerName);
                    hdrs.add(unicodeAddrs);
                }
            }

            // always put back the orig value
            hdrs.add(header);

        }

        return hdrs;
    }

    @Override
    public List<String> getHeader(String name) {
        MimeMessage msg;
        try {
            msg = handler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
            return Collections.emptyList();
        }

        String[] headers = Mime.getHeaders(msg, name);
        if (headers == null) {
            return Collections.emptyList();
        }

        if (addrHdrs.contains(name.toLowerCase()))
            return handleIDN(name, headers);
        else
            return Arrays.asList(headers);
    }

    @Override
    public List<String> getHeaderNames() throws SieveMailException {
        Set<String> headerNames = new HashSet<String>();
        MimeMessage msg;
        try {
            msg = handler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            Enumeration<Header> allHeaders = msg.getAllHeaders();
            while (allHeaders.hasMoreElements()) {
                headerNames.add(allHeaders.nextElement().getName());
            }
            return new ArrayList<String>(headerNames);
        } catch (MessagingException ex) {
            throw new SieveMailException(ex);
        }
    }

    @Override
    public List<String> getMatchingHeader(String name) throws SieveMailException {
        return MailUtils.getMatchingHeader(this, name);
    }

    /**
     * Scans all MIME parts and returns the values of any headers that
     * match the given name.
     */
    public Set<String> getMatchingHeaderFromAllParts(String name)
    throws SieveMailException {
        MimeMessage msg;
        Set<String> values = new HashSet<String>();

        try {
            msg = handler.getMimeMessage();
            for (MPartInfo partInfo : Mime.getParts(msg)) {
                MimePart part = partInfo.getMimePart();
                values.addAll(Arrays.asList(Mime.getHeaders(part, name)));
            }
        } catch (Exception e) {
            throw new SieveMailException("Unable to match attachment headers.", e);
        }

        return values;
    }

    @Override
    public int getSize() {
        return handler.getMessageSize();
    }

    /**
     * Returns the ids of messages that have been added by filter rules,
     * or an empty list.
     */
    public List<ItemId> getAddedMessageIds() {
        return Collections.unmodifiableList(addedMessageIds);
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public Object getContent() {
        return "";
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public Address[] parseAddresses(String headerName) {
        MimeMessage msg;
        try {
            msg = handler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
            return FilterAddress.EMPTY_ADDRESS_ARRAY;
        }

        String[] hdrValues = null;
        try {
            hdrValues = msg.getHeader(headerName);
        } catch (MessagingException e) {
            ZimbraLog.filter.warn("Unable to get headers named '%s'", headerName, e);
        }
        if (hdrValues == null) {
            return FilterAddress.EMPTY_ADDRESS_ARRAY;
        }

        List<Address> retVal = new LinkedList<Address>();
        for (String hdrValue : hdrValues) {
            for (InternetAddress addr : InternetAddress.parseHeader(hdrValue)) {
                String emailAddr = addr.getAddress();
                if (emailAddr != null)
                    retVal.add(new FilterAddress(emailAddr));
            }
        }
        return retVal.toArray(new Address[retVal.size()]);
    }

    // jSieve 0.4
    @Override
    public boolean isInBodyText(String substring) {
        // No implementation.  We use our own body test.
        return false;
    }

    public void setDiscardActionPresent() {
        discardActionPresent = true;
    }
}
