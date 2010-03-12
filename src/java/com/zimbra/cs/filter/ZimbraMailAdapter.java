/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionRedirect;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.MailUtils;
import org.apache.jsieve.mail.SieveMailException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ActionTag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Sieve evaluation engine adds a list of {@link org.apache.jsieve.mail.Action}s 
 * that have matched the filter conditions to this object
 * and invokes its {@link #executeActions()} method.
 */
public class ZimbraMailAdapter implements MailAdapter
{
    private Mailbox mMailbox;
    private FilterHandler mHandler;
    private String mTags;
    private boolean mAllowFilterToMountpoint = true;
    
    /**
     * Keeps track of folders into which we filed messages, so we don't file twice
     * (RFC 3028 2.10.3).
     */
    private Set<String> mFiledIntoPaths = new HashSet<String>();
   
    /**
     * Set of address headers that need to be processed for IDN.
     */
    private static Set<String> sAddrHdrs;
    
    /**
     * List of Actions to perform.
     */ 
    private List<Action> mActions = new ArrayList<Action>();

    /**
     * Ids of messages that have been added.
     */
    protected List<ItemId> mAddedMessageIds = new ArrayList<ItemId>();

    static {
        sAddrHdrs = new HashSet<String>();
        sAddrHdrs.add("bcc");
        sAddrHdrs.add("cc");
        sAddrHdrs.add("from");
        sAddrHdrs.add("reply-to");
        sAddrHdrs.add("sender");
        sAddrHdrs.add("to");
    }
    
    public ZimbraMailAdapter(Mailbox mailbox, FilterHandler handler) {
        mMailbox = mailbox;
        mHandler = handler;
    }
    
    public void setAllowFilterToMountpoint(boolean allowFilterToMountpoint) {
        mAllowFilterToMountpoint = allowFilterToMountpoint;
    }

    /**
     * Returns the <tt>ParsedMessage</tt>, or <tt>null</tt> if it is not available.
     */
    public ParsedMessage getParsedMessage() {
        try {
            return mHandler.getParsedMessage();
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
            return mHandler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
        }
        return null;
    }
    
    /**
     * Returns the List of actions.
     * @return List
     */
    public List<Action> getActions() {
        return mActions;
    }
    
    public ListIterator<Action> getActionsIterator() {
        return mActions.listIterator();
    }

    /**
     * Adds an Action.
     * @param action The action to set
     */
    public void addAction(Action action) {
        mActions.add(action);
    }
    
    public void executeActions() throws SieveException {
        try {
            mHandler.beforeFiltering();
            
            String messageId = Mime.getMessageID(mHandler.getMimeMessage());

            // If the Sieve script has no actions, JSieve generates an implicit keep.  If
            // the script contains a single discard action, JSieve returns an empty list.
            if (getActions().size() == 0) {
                ZimbraLog.filter.info("Discarding message with Message-ID %s from %s",
                    messageId, Mime.getSender(mHandler.getMimeMessage()));
                mHandler.discard();
                return;
            }
            
            // If only tag/flag actions are specified, JSieve does not generate an
            // implicit keep.
            List<Action> deliveryActions = getDeliveryActions();
            
            if (deliveryActions.size() == 0) {
                doDefaultFiling();
            }
            
            // Handle explicit and implicit delivery actions
            for (Action action : deliveryActions) {
                if (action instanceof ActionKeep) {
                    if (((ActionKeep) action).isImplicit()) {
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
                        if (!mAllowFilterToMountpoint && isMountpoint(mMailbox, folderPath)) {
                            ZimbraLog.filter.info("Filing to mountpoint \"%s\" is not allowed.  Filing to the default folder instead.",
                                folderPath);
                            explicitKeep();
                        } else {
                            fileInto(folderPath);
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.filter.info("Unable to file message to %s.  Filing to %s instead.",
                            folderPath, mHandler.getDefaultFolderPath(), e);
                        explicitKeep();
                    }
                } else if (action instanceof ActionRedirect) {
                    // redirect mail to another address
                    ActionRedirect redirect = (ActionRedirect) action;
                    String addr = redirect.getAddress();
                    ZimbraLog.filter.info("Redirecting message to %s.", addr);
                    try {
                        mHandler.redirect(addr);
                    } catch (Exception e) {
                        ZimbraLog.filter.warn("Unable to redirect to %s.  Filing message to %s.",
                            addr, mHandler.getDefaultFolderPath(), e);
                        explicitKeep();
                    }
                } else {
                    throw new SieveException("unknown action " + action);
                }
            }
            mHandler.afterFiltering();
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
    }
    
    private static boolean isMountpoint(Mailbox mbox, String folderPath)
    throws ServiceException {
        Pair<Folder, String> pair = mbox.getFolderByPathLongestMatch(null, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
        Folder f = pair.getFirst();
        if (f != null && f instanceof Mountpoint) {
            return true;
        }
        return false;
    }
    
    private List<Action> getDeliveryActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : mActions) {
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
        for (Action action : mActions) {
            if (action instanceof ActionTag) {
                actions.add((ActionTag) action);
            }
        }
        return actions;
    }
    
    private List<ActionFlag> getFlagActions() {
        List<ActionFlag> actions = new ArrayList<ActionFlag>();
        for (Action action : mActions) {
            if (action instanceof ActionFlag) {
                actions.add((ActionFlag) action);
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
        String folderPath = mHandler.getDefaultFolderPath();
        Message msg = null;
        if (mFiledIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            msg = mHandler.implicitKeep(getFlagActions(), getTags());
            if (msg != null) {
                mFiledIntoPaths.add(folderPath);
                mAddedMessageIds.add(new ItemId(msg));
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
        String folderPath = mHandler.getDefaultFolderPath();
        Message msg = null;
        if (mFiledIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            msg = mHandler.explicitKeep(getFlagActions(), getTags());
            if (msg != null) {
                mFiledIntoPaths.add(folderPath);
                mAddedMessageIds.add(new ItemId(msg));
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
        if (mFiledIntoPaths.contains(folderPath)) {
            ZimbraLog.filter.info("Ignoring second attempt to file into %s.", folderPath);
        } else {
            ItemId id = mHandler.fileInto(folderPath, getFlagActions(), getTags());
            if (id != null) {
                mFiledIntoPaths.add(folderPath);
                mAddedMessageIds.add(id);
            }
        }
    }

    private String getTags()
    throws ServiceException {
        if (mTags == null) {
            StringBuilder tagsBuf = null;
            for (Action action : getTagActions()) {
                String tagName = ((ActionTag) action).getTagName();
                Tag tag = null;
                try {
                    tag = mMailbox.getTagByName(tagName);
                } catch (MailServiceException e) {
                    if (e.getCode().equals(MailServiceException.NO_SUCH_TAG)) {
                        ZimbraLog.filter.info("Could not find tag %s.  Automatically creating it.", tagName);
                        try {
                            tag = mMailbox.createTag(null, tagName, MailItem.DEFAULT_COLOR);
                        } catch (ServiceException e2) {
                            ZimbraLog.filter.warn("Could not create tag %s.  Not applying tag.", tagName, e2);
                        }
                    }
                }
                if (tag != null) {
                    if (tagsBuf == null) {
                        tagsBuf = new StringBuilder(Integer.toString(tag.getId()));
                    } else {
                        tagsBuf.append(",").append(tag.getId());
                    }
                }
            }
            if (tagsBuf == null) {
                mTags = "";
            } else {
                mTags = tagsBuf.toString();
            }
        }
        return mTags;
    }
    
    private List<String> handleIDN(String headerName, String[] headers) {

        List<String> hdrs = new ArrayList<String>();
        for (int i = 0; i < headers.length; i++) {
            boolean altered = false;
            
            if (headers[i].contains(IDNUtil.ACE_PREFIX)) {
                // handle multiple addresses in a header
                StringTokenizer st = new StringTokenizer(headers[i], ",;", true);
                StringBuffer addrs = new StringBuffer();
                while (st.hasMoreTokens()) {
                    String address = st.nextToken();
                    String delim = st.hasMoreTokens()?st.nextToken():"";
                    try {
                        InternetAddress inetAddr = new InternetAddress(address);
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
            hdrs.add(headers[i]);
            
        }
        
        return hdrs;
    }
    
    public List<String> getHeader(String name) {
        MimeMessage msg = null;
        try {
            msg = mHandler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
            return Collections.emptyList();
        }
        
        String[] headers = Mime.getHeaders(msg, name);
        if (headers == null) {
            return Collections.emptyList();
        }
        
        if (sAddrHdrs.contains(name.toLowerCase()))
            return handleIDN(name, headers);
        else
            return Arrays.asList(headers);
    }

    public List<String> getHeaderNames() throws SieveMailException {
        Set<String> headerNames = new HashSet<String>();
        MimeMessage msg = null;
        try {
            msg = mHandler.getMimeMessage();
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

    public List<String> getMatchingHeader(String name)
        throws SieveMailException {
        @SuppressWarnings("unchecked")
        List<String> result = MailUtils.getMatchingHeader(this, name);
        return result;
    }

    public int getSize() throws SieveMailException {
        try {
            return mHandler.getParsedMessage().getRawSize();
        } catch (Exception e) {
            throw new SieveMailException(e);
        }
    }
    
    /**
     * Returns the ids of messages that have been added by filter rules,
     * or an empty list.
     */
    public List<ItemId> getAddedMessageIds() {
        return Collections.unmodifiableList(mAddedMessageIds);
    }
    
    public Mailbox getMailbox() {
        return mMailbox;
    }
    
    public Object getContent() {
        return "";
    }
    
    public String getContentType() {
        return "text/plain";
    }

    public Address[] parseAddresses(String headerName) {
        MimeMessage msg = null;
        try {
            msg = mHandler.getMimeMessage();
        } catch (ServiceException e) {
            ZimbraLog.filter.warn("Unable to get MimeMessage.", e);
            return FilterAddress.EMPTY_ADDRESS_ARRAY;
        }
        
        String[] addresses = Mime.getHeaders(msg, headerName);
        if (addresses == null) {
            return FilterAddress.EMPTY_ADDRESS_ARRAY;
        }
        Address[] retVal = new Address[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            retVal[i] = new FilterAddress(addresses[i]);
        }
        return retVal;
    }

    // jSieve 0.4
    public boolean isInBodyText(String substring) {
        // No implementation.  We use our own body test.
        return false;
    }
}
