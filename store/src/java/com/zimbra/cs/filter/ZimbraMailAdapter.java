/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2017 Synacor, Inc.
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

package com.zimbra.cs.filter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionReject;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.MailUtils;
import org.apache.jsieve.mail.SieveMailException;
import org.apache.jsieve.mail.optional.EnvelopeAccessors;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.filter.jsieve.ActionEreject;
import com.zimbra.cs.filter.jsieve.ActionExplicitKeep;
import com.zimbra.cs.filter.jsieve.ActionFileInto;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ActionNotify;
import com.zimbra.cs.filter.jsieve.ActionNotifyMailto;
import com.zimbra.cs.filter.jsieve.ActionRedirect;
import com.zimbra.cs.filter.jsieve.ActionReply;
import com.zimbra.cs.filter.jsieve.ActionTag;
import com.zimbra.cs.filter.jsieve.ErejectException;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;


/**
 * Sieve evaluation engine adds a list of {@link org.apache.jsieve.mail.Action}s
 * that have matched the filter conditions to this object
 * and invokes its {@link #executeActions()} method.
 */
public class ZimbraMailAdapter implements MailAdapter, EnvelopeAccessors {
    private Mailbox mailbox;
    private Account account;
    private FilterHandler handler;
    private String[] tags;
    private boolean allowFilterToMountpoint = true;
    private Map<String, String> variables = new HashMap<String, String>();
    private List<String> matchedValues = new ArrayList<String>();
    private boolean parsedMessageCloned = false;

    public enum VARIABLEFEATURETYPE { UNKNOWN, OFF, AVAILABLE};
    private VARIABLEFEATURETYPE variablesExtAvailable = VARIABLEFEATURETYPE.UNKNOWN;

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

    private LmtpEnvelope envelope = null;

    /**
     * Parse result of the triggering message (editheader actions)
     */
    public enum PARSESTATUS { UNKNOWN, TOLERABLE, MIMEMALFORMED };
    private PARSESTATUS eheParseStatus = PARSESTATUS.UNKNOWN;

    /**
     * List of capability strings declared by "require" control.
     */
    private List<String> capabilities = new ArrayList<String>();
    
    private boolean isStop = false;

    private boolean fieldImplicitKeep = true;
    private boolean isAddHeaderPresent = false;
    private boolean isDeleteHeaderPresent = false;
    private boolean isReplaceHeaderPresent = false;
    private boolean isUserScriptExecuting = false;

    public ZimbraMailAdapter(Mailbox mailbox, FilterHandler handler) {
        this.mailbox = mailbox;
        this.handler = handler;

        try {
            setAccount(getMailbox().getAccount());
        } catch (ServiceException e) {
            ZimbraLog.filter.info("Error initializing the sieve variables extension.", e);
        }
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
        /*
         * [Keep action]
         * At the end of the evaluation for each script, if the implicit keep is
         * still in effect, SieveFactory class will call this method to add
         * a Keep action to the "actions" list. We won't/can't change such behaviour
         * in the SieveFactory class as it is in the apache jsieve library.
         * Instead, we add a (implicit) Keep action only if the implicit keep is
         * truly in effect.
         */
        if (actions.size() == 0) {
            // If the "actions" list is empty, add the action no matter what action is.
            actions.add(action);
        } else {
            if (actions.get(actions.size() - 1) instanceof ActionKeep) {
                // The recently added action must be an implicit keep.
                // Let's over-write this implicit keep with the new action.
                actions.remove(actions.size() - 1);
                actions.add(action);
            } else {
                // we don't have to add an implicit keep if there has been already
                // any action(s) in the "actions" list.
                if (!(action instanceof ActionKeep)) {
                    // Add non-keep action.
                    actions.add(action);
                }
            }
        }
    }

    @Override
    public void executeActions() throws SieveException {
        fieldImplicitKeep = fieldImplicitKeep & context.getCommandStateManager().isImplicitKeep();
    }

    /**
     * Execute the list of actions which have been stacked during the evaluation
     * of the each sieve script.
     * @throws SieveException
     */
    public void executeAllActions() throws SieveException {
        try {
            handler.beforeFiltering();

            String messageId = Mime.getMessageID(handler.getMimeMessage());

            // If the Sieve script has no actions, JSieve generates an implicit keep.  If
            // the script contains a single discard action, JSieve returns an empty list.
            if (this.discardActionPresent) {
                ZimbraLog.filter.info("Discarding message with Message-ID %s from %s",
                    messageId, Mime.getSender(handler.getMimeMessage()));
            }
            if (getActions().size() == 0) {
                handler.discard();
                return;
            }

            detectExplicitKeep();

            for (Action action : actions) {
                if (action instanceof ActionKeep) {
                    executeActionKeepInternal();
                } else if (action instanceof ActionExplicitKeep) {
                    keep(KeepType.EXPLICIT_KEEP);
                } else if (action instanceof ActionFileInto) {
                    executeActionFileIntoInternal(action);
                } else if (action instanceof ActionRedirect) {
                    executeActionRedirectInternal(action);
                } else if (action instanceof ActionReply) {
                    executeActionReplyInternal(action);
                } else if (action instanceof ActionNotify) {
                    executeActionNotifyInternal(action);
                } else if (action instanceof ActionReject) {
                    executeActionRejectInternal(action);
                } else if (action instanceof ActionEreject) {
                    executeActionErejectInternal(action);
                } else if (action instanceof ActionNotifyMailto) {
                    executeActionNotifyMailto(action);
                }
            }

            handler.afterFiltering();
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
    }

    private void detectExplicitKeep() throws ServiceException {
       List<Action> deliveryActions = getDeliveryActions();
       if (deliveryActions.isEmpty()) {
           // i.e. no keep/fileinto/redirect actions
           if (getReplyNotifyRejectActions().isEmpty()) {
               // if only flag/tag actions are present, we keep the message even if discard
               // action is present
               keep(KeepType.EXPLICIT_KEEP);
           } else if (!discardActionPresent) {
               // else if reply/notify/reject/ereject actions are present and there's no discard, do sort
               // of implicit keep
               keep(KeepType.EXPLICIT_KEEP);
           }
       } else {
           ListIterator<Action> li = deliveryActions.listIterator(deliveryActions.size());
           while (li.hasPrevious()) {
               Action lastDeliveryAction = li.previous();

               if (lastDeliveryAction instanceof ActionFileInto) {
                   ActionFileInto lastFileIntoAction = (ActionFileInto) lastDeliveryAction;
                   if (lastFileIntoAction.isCopy() && !discardActionPresent) {
                       keep(KeepType.EXPLICIT_KEEP);
                   }
                   break;
               } else if (lastDeliveryAction instanceof ActionRedirect) {
                   ActionRedirect lastRedirectAction = (ActionRedirect) lastDeliveryAction;
                   if (lastRedirectAction.isCopy() && !discardActionPresent) {
                       keep(KeepType.EXPLICIT_KEEP);
                   }
                   break;
               }
           }
       }
   }

   private void executeActionKeepInternal() throws ServiceException {
        if (isImplicitKeep()) {
            // implicit keep: this means that none of the user's rules have been matched
            // we need to check system spam filter to see if the mail is spam
            keep(KeepType.IMPLICIT_KEEP);
        } else if (!discardActionPresent) {
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionFileIntoInternal(Action action) throws ServiceException {
        ActionFileInto fileinto = (ActionFileInto) action;
        String folderPath = fileinto.getDestination();
        try {
            if (!allowFilterToMountpoint && isMountpoint(mailbox, folderPath)) {
                ZimbraLog.filter.info("Filing to mountpoint \"%s\" is not allowed.  Filing to the default folder instead.",
                                      folderPath);
                keep(KeepType.EXPLICIT_KEEP);
            } else {
                fileInto(folderPath);
            }
        } catch (ServiceException e) {
            ZimbraLog.filter.info("Unable to file message to %s.  Filing to %s instead.",
                                  folderPath, handler.getDefaultFolderPath(), e);
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionRedirectInternal(Action action) throws ServiceException {
        // redirect mail to another address
        ActionRedirect redirect = (ActionRedirect) action;
        String addr = redirect.getAddress();
        ZimbraLog.filter.info("Redirecting message to %s.", addr);
        try {
            handler.redirect(addr);
        } catch (Exception e) {
            ZimbraLog.filter.warn("Unable to redirect to %s.  Filing message to %s.",
                                  addr, handler.getDefaultFolderPath(), e);
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionReplyInternal(Action action) throws ServiceException {
        // reply to mail
        ActionReply reply = (ActionReply) action;
        ZimbraLog.filter.debug("Replying to message");
        try {
            String replyStrg = reply.getBodyTemplate();
            handler.reply(replyStrg);
        } catch (Exception e) {
            ZimbraLog.filter.warn("Unable to reply.", e);
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionNotifyInternal(Action action) throws ServiceException {
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
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionRejectInternal(Action action) throws ServiceException {
        ActionReject reject = (ActionReject) action;
        ZimbraLog.filter.debug("Refusing delivery of a message: %s", reject.getMessage());
        try {
            String msg = reject.getMessage();
            handler.reject(msg, envelope);
            handler.discard();
        } catch (Exception e) {
            ZimbraLog.filter.info("Unable to reject.", e);
            keep(KeepType.EXPLICIT_KEEP);
        }
    }

    private void executeActionErejectInternal(Action action) throws ErejectException {
        ActionEreject ereject = (ActionEreject) action;
        ZimbraLog.filter.debug("Refusing delivery of a message at the protocol level");
        try {
            handler.ereject(envelope);
        } catch (ErejectException e) {
            // 'ereject' action executed
            throw e;
        } catch (Exception e) {
            ZimbraLog.filter.warn("Unable to ereject.", e);
        }
    }

    private void executeActionNotifyMailto(Action action) throws ServiceException {
        ActionNotifyMailto notifyMailto = (ActionNotifyMailto) action;
        ZimbraLog.filter.debug("Sending RFC 5435/5436 compliant notification message to %s.", notifyMailto.getMailto());
        try {
            handler.notifyMailto(envelope,
                                 notifyMailto.getFrom(),
                                 notifyMailto.getImportance(),
                                 notifyMailto.getOptions(),
                                 notifyMailto.getMessage(),
                                 notifyMailto.getMailto(),
                                 notifyMailto.getMailtoParams());
        } catch (Exception e) {
            ZimbraLog.filter.warn("Unable to notify (mailto).", e);
            keep(KeepType.EXPLICIT_KEEP);
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
                action instanceof ActionExplicitKeep ||
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

    private List<Action> getReplyNotifyRejectActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : this.actions) {
            if (action instanceof ActionReply || action instanceof ActionNotify
               || action instanceof ActionReject || action instanceof ActionEreject) {
                actions.add(action);
            }
        }
        return actions;
    }

    /**
     * Files the message into the inbox or sent or spam  folder.
     * Keeps track of the folder path, to make sure we don't file to the same
     * folder twice.
     */
    public enum KeepType {IMPLICIT_KEEP, EXPLICIT_KEEP};
    public Message keep(KeepType type) throws ServiceException {
        String folderPath = handler.getDefaultFolderPath();
        folderPath = CharMatcher.is('/').trimFrom(folderPath); // trim leading and trailing '/'
        Message msg = null;
        ZimbraLog.filter.debug(type == KeepType.EXPLICIT_KEEP ? "Explicit - fileinto " : "Implicit - fileinto " +
            appendFlagTagActionsInfo(folderPath, getFlagActions(), getTagActions()));
        if (isPathContainedInFiledIntoPaths(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            if (type == KeepType.EXPLICIT_KEEP) {
                msg = handler.explicitKeep(getFlagActions(), getTags());
            } else {
                msg = handler.implicitKeep(getFlagActions(), getTags());
            }
            if (msg != null) {
                setTagsVisible(getTags());
                filedIntoPaths.add(folderPath);
                addedMessageIds.add(new ItemId(msg));
            }
        }
        return msg;
    }

    private boolean isPathContainedInFiledIntoPaths(String folderPath) {
        folderPath = StringUtil.trimTrailingSpaces(folderPath);

        // 1. Check folder name case-sensitively if it has already been registered in folderIntoPaths list
        if (filedIntoPaths.contains(folderPath)) {
            return true;
        }
        // 2. Check it case-insensitively
        for (String path : filedIntoPaths) {
            if (path.equalsIgnoreCase(folderPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Files the message into the given folder, as a result of an explicit
     * fileinto filter action.  Keeps track of the folder path, to make
     * sure we don't file to the same folder twice.
     */
    private void fileInto(String folderPath)
    throws ServiceException {
        folderPath = CharMatcher.is('/').trimFrom(folderPath); // trim leading and trailing '/'
        if (ZimbraLog.filter.isDebugEnabled()) {
            ZimbraLog.filter.debug(
                    appendFlagTagActionsInfo("fileinto " + folderPath, getFlagActions(), getTagActions()));
        }
        if (isPathContainedInFiledIntoPaths(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            ItemId id = handler.fileInto(folderPath, getFlagActions(), getTags());
            if (id != null) {
                setTagsVisible(getTags());
                filedIntoPaths.add(folderPath);
                addedMessageIds.add(id);
            }
        }
    }

    private void setTagsVisible(String[] tags) {
        for (String tagName : tags) {
            try {
                Tag tag;
                tag = mailbox.getTagByName(null, tagName);
                if (tag == null || !tag.isListed()) {
                    mailbox.createTag(null, tagName, (byte) 0);
                }
            } catch (ServiceException e) {
                ZimbraLog.filter.info("Failed to set tag visible.  \"" + tagName + "\" stays invisible on the tag list");
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
                emailAddr = StringEscapeUtils.unescapeJava(emailAddr);
                if (emailAddr != null && emailAddr.contains("@"))
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


    public void setEnvelope(LmtpEnvelope env) {
        this.envelope  = env;
    }

    @Override
    public List<String> getEnvelope(String name) throws SieveMailException {
        return getMatchingEnvelope(name);
    }

    @Override
    public List<String> getEnvelopeNames() throws SieveMailException {
        List<String> result = new ArrayList<String>();
        if (envelope.hasRecipients()) {
            result.add("to");
        }
        if (envelope.hasSender()) {
            result.add("from");
        }
        return result;
    }

    @Override
    public List<String> getMatchingEnvelope(String name)
        throws SieveMailException {
        List<String> result = Lists.newArrayListWithExpectedSize(2);
        if (envelope == null) {
            return result;
        }

        switch (name.toLowerCase()) {
        case "to":
            /* RFC 5228 5.4. Test envelope
             * ---
             * If the SMTP transaction involved several RCPT commands, only the data
             * from the RCPT command that caused delivery to this user is available
             * in the "to" part of the envelope.
             * ---
             * Return only the address (primary and alias) who is currently being processed.
             */
            List<LmtpAddress> recipients = envelope.getRecipients();
            try {
                String myaddress = mailbox.getAccount().getMail();
                if (null != myaddress && !myaddress.isEmpty()) {
                    for (LmtpAddress recipient: recipients) {
                        if (myaddress.toUpperCase().startsWith(recipient.getEmailAddress().toUpperCase())) {
                            result.add(recipient.getEmailAddress());
                        }
                    }
                }
                String[] myaliases = mailbox.getAccount().getMailAlias();
                if (myaliases.length > 0) {
                    for (String alias : myaliases) {
                        for (LmtpAddress recipient: recipients) {
                            if (alias.toUpperCase().startsWith(recipient.getEmailAddress().toUpperCase())) {
                                result.add(recipient.getEmailAddress());
                            }
                        }
                    }
                }
            } catch (ServiceException e) {
                // nothing to do with this exception. Just return an empty list
            }
            break;

        case "from":
            LmtpAddress sender = envelope.getSender();
            result.add(sender.getEmailAddress());
            break;

        }

        return result;
    }

    public String getVariable(String key) {
        return variables.get(key);
    }

    public void addVariable(String key, String value) {
        this.variables.put(key.toLowerCase(), value);
    }

    public List<String> getMatchedValues() {
        return matchedValues;
    }

    public void setMatchedValues(List<String> matchedValues) {
        this.matchedValues = matchedValues;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, String> getMimeVariables() {
        Map<String, String> mimeVars = null;
        try {
            mimeVars = FilterUtil.getVarsMap(mailbox,
                handler.getParsedMessage(), handler.getMimeMessage());
        } catch (MessagingException | ServiceException e) {
            ZimbraLog.filter.error("Unable to read mime variables.", e);
        }
        return mimeVars;
    }

    public void updateIncomingBlob() {
        DeliveryContext ctxt = handler.getDeliveryContext();
        if (ctxt != null) {
            StoreManager sm = StoreManager.getInstance();
            InputStream in = null;
            Blob blob = null;
            try {
                ParsedMessage pm = getParsedMessage();
                pm.updateMimeMessage();
                in = pm.getRawInputStream();
                blob = sm.storeIncoming(in);
            } catch (IOException | ServiceException | MessagingException e) {
                ZimbraLog.filter.error("Unable to update MimeMessage and incomimg blob.", e);
            } finally {
                ByteUtil.closeStream(in);
            }

            if (!parsedMessageCloned) {
                Blob prevBlob = ctxt.getIncomingBlob();
                if (prevBlob != null) {
                    sm.quietDelete(prevBlob);
                }
            } else {
                Blob prevBlob = ctxt.getMailBoxSpecificBlob(mailbox.getId());
                if (prevBlob != null) {
                    sm.quietDelete(prevBlob);
                    ctxt.clearMailBoxSpecificBlob(mailbox.getId());
                }
            }

            if (ctxt.getShared()) {
                ctxt.setMailBoxSpecificBlob(mailbox.getId(), blob);
                ZimbraLog.filter.debug("setting mailbox specific blob for mailbox %d", mailbox.getId());
            } else {
                try {
                    ctxt.deepsetIncomingBlob(blob);
                    ZimbraLog.filter.debug("Updated incoming blob");
                } catch (IOException e) {
                    ZimbraLog.filter.error("Unable to update incomimg blob.", e);
                    return;
                }
            }
        }
    }

    public void resetValues() {
        resetMatchedValues();
        resetVariables();
        resetEditHeaderFlags();
    }

    public void resetMatchedValues() { matchedValues.clear(); }
    public void resetVariables() { variables.clear(); }
    public void resetEditHeaderFlags() {
        isAddHeaderPresent = false;
        isDeleteHeaderPresent = false;
        isReplaceHeaderPresent = false;
        isUserScriptExecuting = false;
    }

    public VARIABLEFEATURETYPE getVariablesExtAvailable() { return variablesExtAvailable; }
    public void setVariablesExtAvailable(VARIABLEFEATURETYPE type) { this.variablesExtAvailable = type; }

    public boolean isStop() {
        return isStop;
    }

    public void setStop(boolean isStop) {
        this.isStop = isStop;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void addCapabilities(String capability) {
        this.capabilities.add(capability);
    }

    public void resetCapabilities() {
        this.capabilities.clear();
    }

    public boolean isCapable(String capability) {
        return this.capabilities.contains(capability);
    }

    public boolean isImplicitKeep () {
        return fieldImplicitKeep;
    }

    public PARSESTATUS getEditHeaderParseStatus() {
        return eheParseStatus;
    }

    public void setEditHeaderParseStatus(PARSESTATUS eheParseStatus) {
        this.eheParseStatus = eheParseStatus;
    }

    public boolean isAddHeaderPresent() {
        return isAddHeaderPresent;
    }

    public void setAddHeaderPresent(boolean isAddHeaderPresent) {
        this.isAddHeaderPresent = isAddHeaderPresent;
    }

    public boolean isDeleteHeaderPresent() {
        return isDeleteHeaderPresent;
    }

    public void setDeleteHeaderPresent(boolean isDeleteHeaderPresent) {
        this.isDeleteHeaderPresent = isDeleteHeaderPresent;
    }

    public boolean isReplaceHeaderPresent() {
        return isReplaceHeaderPresent;
    }

    public void setReplaceHeaderPresent(boolean isReplaceHeaderPresent) {
        this.isReplaceHeaderPresent = isReplaceHeaderPresent;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public boolean isUserScriptExecuting() {
        return isUserScriptExecuting;
    }

    public void setUserScriptExecuting(boolean isUserScriptExecuting) {
        this.isUserScriptExecuting = isUserScriptExecuting;
    }

    public boolean cloneParsedMessage() {
        boolean cloneFailure = false;
        DeliveryContext ctxt = handler.getDeliveryContext();
        if (ctxt != null && ctxt.getShared() && !parsedMessageCloned && handler instanceof IncomingMessageHandler) {
            try {
                ParsedMessage pm = handler.getParsedMessage();
                ParsedMessage clonePM = new ParsedMessage(pm.getRawData(), pm.isAttachmentIndexingEnabled());
                ((IncomingMessageHandler) handler).setParsedMessage(clonePM);
                parsedMessageCloned = true;
                ZimbraLog.filter.debug("cloned ParsedMessage");
            } catch (IOException | ServiceException e) {
                cloneFailure = true;
                ZimbraLog.filter.debug("failed to clone parsed message", e);
                ZimbraLog.filter.warn("ZimbraMailAdapter: failed to clone parsed message");
            }
        }
        return cloneFailure;
   }
}
