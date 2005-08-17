package com.zimbra.cs.filter;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.MessagingException;

import org.apache.jsieve.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
//import org.apache.jsieve.mail.ActionRedirect;
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
import com.zimbra.cs.util.LiquidLog;

/**
 * <p>Class LiquidMailAdapter implements a mock MailAdapter for testing purposes.</p>
 * 
 * <p>Being a mock object, Actions are not performed against a mail server, but in 
 * most other respects it behaves as would expect a MailAdapter wrapping a JavaMail 
 * message should. To this extent, it is a useful demonstration of how to create an
 * implementation of a MailAdapter.
 */
public class LiquidMailAdapter implements MailAdapter
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
            sSpamHeader = prov.getConfig().getAttr(Provisioning.A_liquidSpamHeader, null);
            String spamRegex = prov.getConfig().getAttr(Provisioning.A_liquidSpamHeaderValue, null);
            if (spamRegex != null)
                sSpamHeaderValue = Pattern.compile(spamRegex);
        } catch (Exception e) {
            LiquidLog.filter.fatal("Unable to get spam header from provisioning", e);
            throw new RuntimeException("Unable to get spam header from provisioning", e);
        }
    }
    
            
    /**
     * Constructor for LiquidMailAdapter.
     */
    private LiquidMailAdapter()
    {
        super();
        mMessages = new ArrayList(5);
    }
    
    /**
     * Constructor for LiquidMailAdapter.
     * @param pm
     * @throws MessagingException 
     */
    public LiquidMailAdapter(Mailbox mailbox, ParsedMessage pm,
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
            if (sSpamHeaderValue != null) {
                if (val != null) {
                    Matcher m = sSpamHeaderValue.matcher(val);
                    mSpam = m.matches();
                }
            } else {
                // no expected header value is specified; 
                // presence of the header (regardless of its value) indicates spam
                mSpam = true;
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
                    }
                    
                } else if (actionClass == ActionFileInto.class) {
                    
                    ActionFileInto fileinto = (ActionFileInto) action;
                    String folderName = fileinto.getDestination();
                    int folderId = Mailbox.ID_FOLDER_INBOX;
                    try {
                        folderId = mMailbox.getFolderByPath(folderName).getId();
                    } catch (MailServiceException.NoSuchItemException nsie) {
                        LiquidLog.filter.warn("Folder " + folderName + " not found; message saved to INBOX for " + mRecipient);
                    }
                    // save the message to the specified folder;
                    // The message will not be filed into the same folder multiple times because of
                    // jsieve FileInto validation ensures it; it is allowed to be filed into
                    // multiple different folders, however
                    Message msg = addMessage(folderId, nontermActions);
                    if (msg == null) {
                        dup = true;
                        break;
                    }
                    
                } else if (actionClass == ActionTag.class ||
                        actionClass == ActionFlag.class) {
                    
                    nontermActions.add(action);
                    
                } /* else if (actionClass == ActionRedirect.class) {

                    // redirect mail to another address
                    ActionRedirect redirect = (ActionRedirect) action;
                    
                } else if (actionClass == ActionReject.class) {

                    // reject mail back to sender
                    ActionReject reject = (ActionReject) action;
                    
                } */ 
                else {
                    throw new SieveException("unknown action " + action);
                }
            }
            if (dup) {
                LiquidLog.filter.debug("filter actions ignored for duplicate messages that are not delivered");
            } else {
                // there may be non-terminal actions left; file a message to INBOX and apply the non-terminal actions on that message
                if (!nontermActions.isEmpty()) {
                    addMessage(Mailbox.ID_FOLDER_INBOX, nontermActions);
                }
            }
        } catch (ServiceException e) {
            throw new LiquidSieveException(e);
        } catch (IOException e) {
            throw new LiquidSieveException(e);
        } catch (MessagingException e) {
            throw new LiquidSieveException(e);
        }

    }

    Message doDefaultFiling() throws MessagingException, IOException, ServiceException {
        int folderId = mSpam ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
        Message msg = addMessage(folderId, Collections.EMPTY_LIST);
        return msg;
    }
    
    private Message addMessage(int folderId, List nontermActions) throws IOException, ServiceException {
        
        StringBuffer tagsBuf = null;
        int flagBits = Flag.FLAG_UNREAD;
        for (Iterator it = nontermActions.listIterator(); it.hasNext(); ) {
            Action action = (Action) it.next();
            
            if (action.getClass() == ActionTag.class) {

                // tag mail
                String tagName = ((ActionTag) action).getTagName();
                try {
                    Tag tag = mMailbox.getTagByName(tagName);
                    if (tagsBuf == null) {
                        tagsBuf = new StringBuffer(String.valueOf(tag.getId()));
                    } else {
                        tagsBuf.append(",").append(tag.getId());
                    }
                } catch (MailServiceException.NoSuchItemException nsie) {
                    LiquidLog.filter.warn("Tag " + tagName + " does not exist; cannot tag message " +
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
                    LiquidLog.filter.warn("Unable to flag message", e);
                }
            }
        }
        nontermActions.clear();
        String tags = null;
        if (tagsBuf != null)
            tags = tagsBuf.toString();
        Message msg = mMailbox.addMessage(null, mParsedMessage, folderId, flagBits, tags, mRecipient, mSharedDeliveryCtxt);
        if (msg != null) {
            mMessages.add(msg);
            if (LiquidLog.filter.isDebugEnabled())
                LiquidLog.filter.debug("Saved message " + msg.getId() + " to mailbox: " + msg.getMailboxId() + " folder: " + folderId + 
                    " tags: " + tags + " flags: 0x" + Integer.toHexString(flagBits));
        }
        return msg;
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
