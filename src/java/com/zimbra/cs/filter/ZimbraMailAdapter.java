/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.jsieve.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionRedirect;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.MailUtils;
import org.apache.jsieve.mail.SieveMailException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ActionTag;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Sieve evaluation engine adds a list of {@link org.apache.jsieve.mail.Action}s 
 * that have matched the filter conditions to this object
 * and invokes its {@link #executeActions()} method.
 */
public class ZimbraMailAdapter implements MailAdapter
{
    private Mailbox mMailbox;
    private String mRecipient;

    private static String sSpamHeader;
    private static Pattern sSpamHeaderValue;
    protected SharedDeliveryContext mSharedDeliveryCtxt;
    
    /**
     * The message being adapted.
     */ 
    private ParsedMessage mParsedMessage;
    
    /**
     * List of Actions to perform.
     */ 
    private List<Action> mActions;

    /**
     * List of processed messages that have been filed into appropriate folders.
     */
    protected List<Message> mMessages;

    /**
     * true if the system spam detector finds this mail to be spam
     * false otherwise
     */
    private boolean mSpam;
    
    static {
        try {
            Provisioning prov = Provisioning.getInstance();
            sSpamHeader = prov.getConfig().getAttr(Provisioning.A_zimbraSpamHeader, null);
            String spamRegex = prov.getConfig().getAttr(Provisioning.A_zimbraSpamHeaderValue, null);
            if (spamRegex != null)
                sSpamHeaderValue = Pattern.compile(spamRegex);
        } catch (Exception e) {
            ZimbraLog.filter.fatal("Unable to get spam header from provisioning", e);
            throw new RuntimeException("Unable to get spam header from provisioning", e);
        }
    }
    
            
    /**
     * Constructor for ZimbraMailAdapter.
     */
    private ZimbraMailAdapter()
    {
        super();
        mMessages = new ArrayList<Message>(5);
    }
    
    /**
     * Constructor for ZimbraMailAdapter.
     * @param pm
     * @throws MessagingException 
     */
    public ZimbraMailAdapter(Mailbox mailbox, ParsedMessage pm,
                             String recipient, SharedDeliveryContext sharedDeliveryCtxt) throws MessagingException
    {
        this();
        mMailbox = mailbox;
        mRecipient = recipient;
        mSharedDeliveryCtxt = sharedDeliveryCtxt;
        setParsedMessage(pm);
        
        // check spam headers set by system spam detector
        if (sSpamHeader != null) {
            String val = pm.getMimeMessage().getHeader(sSpamHeader, null);
            if (val != null) {
                if (sSpamHeaderValue != null) {
                    Matcher m = sSpamHeaderValue.matcher(val);
                    mSpam = m.matches();
                } else {
                    // no expected header value is configured; 
                    // presence of the header (regardless of its value) indicates spam
                    mSpam = true;
                }
            }
        }
    }    

    public ParsedMessage getParsedMessage() {
        return mParsedMessage;
    }
    
    /**
     * Sets the message.
     * @param pm The message to set
     */
    protected void setParsedMessage(ParsedMessage pm)
    {
        mParsedMessage = pm;
    }

    /**
     * Returns the List of actions.
     * @return List
     */
    public List<Action> getActions()
    {
        List<Action> actions = null;
        if (null == (actions = getActionsBasic()))
        {
            updateActions();
            return getActions();
        }    
        return actions;
    }
    
    /**
     * Returns a new List of actions.
     * @return List
     */
    protected List<Action> computeActions()
    {
        return new ArrayList<Action>();
    }    
    
    /**
     * Returns the List of actions.
     * @return List
     */
    private List<Action> getActionsBasic()
    {
        return mActions;
    }    

    /**
     * Adds an Action.
     * @param action The action to set
     */
    public void addAction(Action action)
    {
        getActions().add(action);
    }
    
    public void executeActions() throws SieveException {
        try {
            boolean dup = false;
            
            // If the Sieve script has no actions, JSieve generates an implicit keep.  If
            // the script contains a single discard action, JSieve returns an empty list.
            if (getActions().size() == 0) {
                ZimbraLog.filter.info("Discarding message with Message-ID %s from %s",
                    mParsedMessage.getMessageID(), mParsedMessage.getSender());
                return;
            }
            
            // If only tag/flag actions are specified, JSieve does not generate an
            // implicit keep.
            List<Action> deliveryActions = getDeliveryActions();
            if (deliveryActions.size() == 0) {
                Message msg = doDefaultFiling();
                if (msg == null) {
                    dup = true;
                }
            }

            // Handle explicit and implicit delivery actions
            for (Action action : deliveryActions) {
                if (action instanceof ActionKeep) {
                    ActionKeep keep = (ActionKeep) action;
                    Message msg = null;
                    if (keep.isImplicit()) {
                        // implicit keep: this means that none of the user's rules have been matched
                        // we need to check system spam filter to see if the mail is spam
                        msg = doDefaultFiling();
                    } else {
                        // if explicit keep is specified, keep in INBOX regardless of spam
                        // save the message to INBOX by explicit user request in the filter
                        msg = addMessage(Mailbox.ID_FOLDER_INBOX);
                    }
                    if (msg == null) {
                        dup = true;
                        break;
                    }
                } else if (action instanceof ActionFileInto) {
                    ActionFileInto fileinto = (ActionFileInto) action;
                    String folderName = fileinto.getDestination();
                    int folderId = Mailbox.ID_FOLDER_INBOX;
                    try {
                        folderId = mMailbox.getFolderByPath(null, folderName).getId();
                    } catch (MailServiceException.NoSuchItemException nsie) {
                        ZimbraLog.filter.warn("Folder " + folderName + " not found; message saved to INBOX for " + mRecipient);
                    }
                    // save the message to the specified folder;
                    // The message will not be filed into the same folder multiple times because of
                    // jsieve FileInto validation ensures it; it is allowed to be filed into
                    // multiple different folders, however
                    Message msg = addMessage(folderId);
                    if (msg == null) {
                        dup = true;
                        break;
                    }
                } else if (action instanceof ActionRedirect) {
                    // redirect mail to another address
                    ActionRedirect redirect = (ActionRedirect) action;
                    String addr = redirect.getAddress();
                    ZimbraLog.filter.info("Redirecting message to " + addr);
                    MimeMessage mm = mParsedMessage.getMimeMessage();
                    try {
                        mm.saveChanges();
                    } catch (MessagingException e) {
                        try {
                            mm = new MimeMessage(mm) {
                                @Override protected void updateHeaders() throws MessagingException {
                                    setHeader("MIME-Version", "1.0");  if (getMessageID() == null) updateMessageID();
                                }
                            };
                            ZimbraLog.filter.info("Message format error detected; wrapper class in use");
                        } catch (MessagingException e2) {
                            ZimbraLog.filter.warn("Message format error detected; workaround failed");
                        }
                    }
                    try {
                        Transport.send(mm, new Address[] { new InternetAddress(addr) });
                    } catch (MessagingException e) {
                        ZimbraLog.filter.warn("Redirect to " + addr + " failed.  Saving message to INBOX.  " + e.toString());
                        addMessage(Mailbox.ID_FOLDER_INBOX);
                    }
                } else {
                    throw new SieveException("unknown action " + action);
                }
            }
            if (dup) {
                ZimbraLog.filter.debug("filter actions ignored for duplicate messages that are not delivered");
            }
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        } catch (IOException e) {
            throw new ZimbraSieveException(e);
        }
    }

    private List<Action> getDeliveryActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : getActions()) {
            if (action instanceof ActionKeep ||
                action instanceof ActionFileInto ||
                action instanceof ActionRedirect) {
                actions.add(action);
            }
        }
        return actions;
    }
    
    private List<Action> getTagFlagActions() {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : getActions()) {
            if (action instanceof ActionTag ||
                action instanceof ActionFlag) {
                actions.add(action);
            }
        }
        return actions;
    }

    Message doDefaultFiling() throws IOException, ServiceException {
        int folderId = mSpam ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
        return addMessage(folderId);
    }

    private Message addMessage(int folderId) throws IOException, ServiceException {
        int flags = getFlagBitmask();
        String tags = getTags();
        Message msg = mMailbox.addMessage(null, mParsedMessage, folderId,
            false, flags, tags, mRecipient, mSharedDeliveryCtxt);
        if (msg != null) {
            mMessages.add(msg);
        }
        return msg;
    }

    private String getTags() {
        StringBuilder tagsBuf = null;
        for (Action action : getTagFlagActions()) {
            if (action instanceof ActionTag) {
                String tagName = ((ActionTag) action).getTagName();
                try {
                    Tag tag = mMailbox.getTagByName(tagName);
                    if (tagsBuf == null) {
                        tagsBuf = new StringBuilder(String.valueOf(tag.getId()));
                    } else {
                        tagsBuf.append(",").append(tag.getId());
                    }
                } catch (MailServiceException.NoSuchItemException nsie) {
                    ZimbraLog.filter.warn("Tag " + tagName + " does not exist; cannot tag message " +
                            " for " + mRecipient);
                } catch (ServiceException e) {
                    ZimbraLog.filter.warn("Unable to determine tags");
                }
            }            
        }
        return tagsBuf == null ? "" : tagsBuf.toString();
    }
    
    private int getFlagBitmask() {
        int flagBits = Flag.BITMASK_UNREAD;
        for (Action action : getTagFlagActions()) {
            if (action instanceof ActionFlag) {
                ActionFlag flagAction = (ActionFlag) action;
                int flagId = flagAction.getFlagId();
                try {
                    Flag flag = mMailbox.getFlagById(flagId);
                    if (flagAction.isSetFlag())
                        flagBits |= flag.getBitmask();
                    else
                        flagBits &= (~flag.getBitmask());
                } catch (ServiceException e) {
                    ZimbraLog.filter.warn("Unable to flag message", e);
                }
            }
        }
        return flagBits;
    }
    
    /**
     * Sets the actions.
     * @param actions The actions to set
     */
    protected void setActions(List<Action> actions)
    {
        mActions = actions;
    }
    
    /**
     * Updates the actions.
     */
    protected void updateActions()
    {
        setActions(computeActions());
    }    

    public ListIterator getActionsIterator()
    {
        return getActions().listIterator();
    }

    public List<String> getHeader(String name)
    {
        String[] headers = mParsedMessage.getHeaders(name);
        return (headers == null ? new ArrayList(0) : Arrays.asList(headers));
    }

    public List<String> getHeaderNames() throws SieveMailException
    {
        Set<String> headerNames = new HashSet<String>();
        try
        {
            Enumeration allHeaders = mParsedMessage.getMimeMessage().getAllHeaders();
            while (allHeaders.hasMoreElements())
            {
                headerNames.add(((Header) allHeaders.nextElement()).getName());
            }
            return new ArrayList<String>(headerNames);
        }
        catch (MessagingException ex)
        {
            throw new SieveMailException(ex);
        }
    }

    public List getMatchingHeader(String name)
        throws SieveMailException
    {
        List result = MailUtils.getMatchingHeader(this, name);
        return result;
    }

    public int getSize() throws SieveMailException
    {
        int size;
        try {
            size = mParsedMessage.getRawSize();
            return size;
        } catch (IOException ioe) {
            throw new SieveMailException(ioe);
        } catch (MessagingException me) {
            throw new SieveMailException(me);
        } 
    }
    
    /**
     * Gets the processed messages. Multiple fileinto actions may be specified.
     * In that case, multiple copies of the message appear in different folders.
     * 
     * @return
     */
    public Message[] getProcessedMessages() {
        return mMessages.toArray(new Message[0]);
    }
    
    public Mailbox getMailbox() {
        return mMailbox;
    }
    
    protected String getRecipient() {
        return mRecipient;
    }

}
