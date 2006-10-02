/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.apache.jsieve.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionRedirect;
//import org.apache.jsieve.mail.ActionReject;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.MailUtils;
import org.apache.jsieve.mail.SieveMailException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.ActionTag;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

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
    private List fieldActions;

    /**
     * List of processed messages that have been filed into appropriate folders.
     */
    protected List /*<Message>*/ mMessages;

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
        mMessages = new ArrayList(5);
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
    public List getActions()
    {
        List actions = null;
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
    protected List computeActions()
    {
        return new ArrayList();
    }    
    
    /**
     * Returns the List of actions.
     * @return List
     */
    private List getActionsBasic()
    {
        return fieldActions;
    }    

    /**
     * Adds an Action.
     * @param action The action to set
     */
    public void addAction(Action action)
    {
        getActions().add(action);
    }
    
    /**
     * @see org.apache.jsieve.mail.MailAdapter#executeActions()
     */
    public void executeActions() throws SieveException
    {
        try {
            ListIterator actionsIter = getActionsIterator();
            boolean dup = false;
            List nontermActions = new ArrayList(5);
            
            Message lastMsgByTermAction = null;
            while (actionsIter.hasNext()) {
                Action action = (Action) actionsIter.next();
                
                Class actionClass = action.getClass();
                if (actionClass == ActionKeep.class) {
                    
                    ActionKeep keep = (ActionKeep) action;
                    Message msg = null;
                    if (keep.isImplicit()) {
                        // implicit keep: this means that none of the user's rules have been matched
                        // we need to check system spam filter to see if the mail is spam
                        msg = doDefaultFiling();
                    } else {
                        // if explicit keep is specified, keep in INBOX regardless of spam
                        // save the message to INBOX by explicit user request in the filter
                        msg = addMessage(Mailbox.ID_FOLDER_INBOX, nontermActions);
                    }
                    if (msg == null) {
                        dup = true;
                        break;
                    } else {
                        lastMsgByTermAction = msg;
                    }
                    
                } else if (actionClass == ActionFileInto.class) {
                    
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
                    Message msg = addMessage(folderId, nontermActions);
                    if (msg == null) {
                        dup = true;
                        break;
                    } else {
                        lastMsgByTermAction = msg;
                    }
                    
                } else if (actionClass == ActionTag.class ||
                        actionClass == ActionFlag.class) {
                    
                    nontermActions.add(action);
                    
                }  else if (actionClass == ActionRedirect.class) {

                    // redirect mail to another address
                    ActionRedirect redirect = (ActionRedirect) action;
                    String addr = redirect.getAddress();
                    ZimbraLog.filter.info("redirecting to " + addr);
                    MimeMessage mm = mParsedMessage.getMimeMessage();
                    try {
                        mm.setRecipients(javax.mail.Message.RecipientType.TO, addr);
                        // Received header will be automatically added by JavaMail
                        // No Resent-* headers are added
                        Transport.send(mm);
                    } catch (AddressException e) {
                        throw MailServiceException.PARSE_ERROR("wrongly formatted address: " + addr, e);
                    } catch (SendFailedException e) {
                        throw MailServiceException.SEND_FAILURE("redirect to " + addr + " failed", e, e.getInvalidAddresses(), e.getValidUnsentAddresses());
                    }
                    
                } 
                /* else if (actionClass == ActionReject.class) {

                    // reject mail back to sender
                    ActionReject reject = (ActionReject) action;
                    
                } */ 
                else {
                    throw new SieveException("unknown action " + action);
                }
            }
            if (dup) {
                ZimbraLog.filter.debug("filter actions ignored for duplicate messages that are not delivered");
            } else {
                // there may be non-terminal actions left; 
                // apply to the message by the last terminal action
                // or if no execution of terminal action is found, 
                // file a message to INBOX and apply the non-terminal actions on that message
                if (!nontermActions.isEmpty()) {
                    if (lastMsgByTermAction != null) {
                        alterMessage(lastMsgByTermAction, nontermActions);
                    } else {
                        addMessage(Mailbox.ID_FOLDER_INBOX, nontermActions);
                    }
                }
            }
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        } catch (IOException e) {
            throw new ZimbraSieveException(e);
        } catch (MessagingException e) {
            throw new ZimbraSieveException(e);
        }

    }

    Message doDefaultFiling() throws MessagingException, IOException, ServiceException {
        int folderId = mSpam ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
        Message msg = addMessage(folderId, Collections.EMPTY_LIST);
        return msg;
    }
    
    private Message addMessage(int folderId, List nontermActions) throws IOException, ServiceException {
        TagAndFlag tf = getTagAndFlag(nontermActions);
        Message msg = mMailbox.addMessage(null, mParsedMessage, folderId, false, tf.flagBits, tf.tags, mRecipient, mSharedDeliveryCtxt);
        if (msg != null) {
            mMessages.add(msg);
            if (ZimbraLog.filter.isDebugEnabled())
                ZimbraLog.filter.debug("Saved message " + msg.getId() + " to mailbox: " + msg.getMailboxId() + " folder: " + folderId + 
                    " tags: " + tf.tags + " flags: 0x" + Integer.toHexString(tf.flagBits));
        }
        return msg;
    }
    
    private void alterMessage(Message msg, List nontermActions) throws IOException, ServiceException {
        long oldTags  = msg.getTagBitmask();
        int  oldFlags = msg.getFlagBitmask();
        TagAndFlag tf = getTagAndFlag(nontermActions);
        long tags = (tf.tags == null ? MailItem.TAG_UNCHANGED : Tag.tagsToBitmask(tf.tags));
        tags |= oldTags;
        int flags = tf.flagBits | oldFlags;
        mMailbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE, flags, tags, null);
    }
    
    private TagAndFlag getTagAndFlag(List nontermActions) throws ServiceException {
        StringBuilder tagsBuf = null;
        int flagBits = Flag.BITMASK_UNREAD;
        for (Iterator it = nontermActions.listIterator(); it.hasNext(); ) {
            Action action = (Action) it.next();
            
            if (action.getClass() == ActionTag.class) {

                // tag mail
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
                }                
            } else if (action.getClass() == ActionFlag.class) {
                
                // flag mail
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
        nontermActions.clear();
        String tags = null;
        if (tagsBuf != null)
            tags = tagsBuf.toString();
        return new TagAndFlag(tags, flagBits);
    }
    
    private static class TagAndFlag {
        private TagAndFlag(String t, int f) { tags = t; flagBits = f; }
        
        private String tags;
        private int flagBits;
    }
    
    /**
     * Sets the actions.
     * @param actions The actions to set
     */
    protected void setActions(List actions)
    {
        fieldActions = actions;
    }
    
    /**
     * Updates the actions.
     */
    protected void updateActions()
    {
        setActions(computeActions());
    }    

    /**
     * @see org.apache.jsieve.mail.MailAdapter#getActionsIterator()
     */
    public ListIterator getActionsIterator()
    {
        return getActions().listIterator();
    }

    /**
     * @see org.apache.jsieve.mail.MailAdapter#getHeader(String)
     */
    public List getHeader(String name) throws SieveMailException
    {
        try
        {
            String[] headers = mParsedMessage.getMimeMessage().getHeader(name);            
            return (headers == null ? new ArrayList(0) : Arrays.asList(headers));
        }
        catch (MessagingException ex)
        {
            throw new SieveMailException(ex);
        }
    }

    /**
     * @see org.apache.jsieve.mail.MailAdapter#getHeaderNames()
     */
    public List getHeaderNames() throws SieveMailException
    {
        Set headerNames = new HashSet();
        try
        {
            Enumeration allHeaders = mParsedMessage.getMimeMessage().getAllHeaders();
            while (allHeaders.hasMoreElements())
            {
                headerNames.add(((Header) allHeaders.nextElement()).getName());
            }
            return new ArrayList(headerNames);
        }
        catch (MessagingException ex)
        {
            throw new SieveMailException(ex);
        }
    }

    /**
     * @see org.apache.jsieve.mail.MailAdapter#getMatchingHeader(String)
     */
    public List getMatchingHeader(String name)
        throws SieveMailException
    {
        List result = MailUtils.getMatchingHeader(this, name);
        return result;
    }

    /**
     * @see org.apache.jsieve.mail.MailAdapter#getSize()
     */
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
        return (Message[]) mMessages.toArray(new Message[0]);
    }
    
    public Mailbox getMailbox() {
        return mMailbox;
    }
    
    protected String getRecipient() {
        return mRecipient;
    }

}
