/*
 * Created on Mar 28, 2005
 */
package com.zimbra.cs.index;

import java.util.Iterator;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.mailbox.MailItem;



/**
 * @author tim
 *
 * A LiquidHit which is being proxied from another server: ie we did a SOAP request
 * somewhere else and are now wrapping results we got from request.
 */
public class ProxiedHit extends LiquidHit 
{
    protected long mProxiedDate = -1;
    protected int mProxiedConvId = -1;
    protected int mProxiedMsgId = -1;
    protected byte mProxiedItemType = -1;
    protected String mProxiedSubject = null;
    protected String mProxiedName = null;
    protected String mMailboxIdStr = null;
    protected ParsedItemID itemID = null;
    
    protected Element mElement;
    
    private ParsedItemID getParsedItemID() throws ServiceException {
        if (itemID == null)
            itemID = ParsedItemID.Parse(mElement.getAttribute(MailService.A_ID));
        return itemID;
    }
    
    public String getMailboxIdStr() throws ServiceException { 
        ParsedItemID id = getParsedItemID();
        
        String server = id.getServerIDString();
        if (server == null) {
            server = getServer();
        }
        
        return "/"+server+"/"+id.getMailboxIDString();
    }

    public ProxiedHit(ProxiedQueryResults results, Element elt) {
        super(results, null, 0.0f);
        
        mMailboxIdStr = null;
        mElement = elt;
    }
    
    boolean inMailbox() throws ServiceException {
        return true; // hmmm....???
    }
    boolean inTrash() throws ServiceException {
        return true; // hmmm....???
    }
    boolean inSpam() throws ServiceException {
        return true; // hmmm....???
    }

    public int getSize() throws ServiceException {
        return (int)mElement.getAttributeLong(MailService.A_SIZE);
    }
    
    public long getDate() throws ServiceException {
        if (mProxiedDate < 0) {
            mProxiedDate = mElement.getAttributeLong(MailService.A_DATE);
        }
        return mProxiedDate;
    }

    public int getConversationId() throws ServiceException {
        if (mProxiedConvId <= 0) {
            mProxiedConvId = (int) mElement.getAttributeLong(MailService.A_CONV_ID);
        }
        return mProxiedConvId;
    }

    public int getItemId() throws ServiceException {
        if (mProxiedMsgId <= 0) {
            ParsedItemID id = getParsedItemID();
            mProxiedMsgId = id.getItemIDInt();
        }
        return mProxiedMsgId;
    }
    
    public byte getItemType() throws ServiceException {
        if (mProxiedItemType <= 0) {
            mProxiedItemType = (byte)mElement.getAttributeLong(MailService.A_ITEM_TYPE);
        }
        return mProxiedItemType;
    }
    
    void setItem(MailItem item) {
        assert(false); // can't preload a proxied hit!
    }

    boolean itemIsLoaded() {
        return true;
    }
    

    public String getSubject() throws ServiceException {
        if (mProxiedSubject == null) {
            mProxiedSubject = mElement.getAttribute(MailService.E_SUBJECT);
        }
        return mProxiedSubject;
    }
    
    public String getFragment() throws ServiceException {
        Element frag = mElement.getElement(MailService.E_FRAG);
        if (frag != null) {
            return frag.getText();
        }
        return "";
    }

    public String getName() throws ServiceException {
        StringBuffer toRet = new StringBuffer();
        for (Iterator iter = mElement.elementIterator(MailService.E_EMAIL); iter.hasNext(); ) 
        {
            Element cur = (Element)(iter.next());
            
            String type = cur.getAttribute(MailService.A_ADDRESS_TYPE);
            String typeStr = "";
            if (type.equals("f")) {
                typeStr = "from";
            } else if (type.equals("s")) {
                typeStr = "sender";
            } else if (type.equals("t")) {
                typeStr = "to";
            } else if (type.equals("r")) {
                typeStr = "reply-to";
            } else if (type.equals("c")) {
                typeStr = "cc";
            } else if (type.equals("b")) {
                typeStr = "bcc";
            }
   
            toRet.append(typeStr);
            toRet.append(":\"");
            
            boolean needSpace = false;

            String str = cur.getAttribute(MailService.A_PERSONAL, null);
            if (str != null) {
                toRet.append(str);
                needSpace = true;
            } else {
                str = cur.getAttribute(MailService.A_DISPLAY, null);
                if (str != null) {
                    if (needSpace) {
                        toRet.append(" ");
                    }
                    toRet.append(str);
                    needSpace = true;
                }
            }
            
            str = cur.getAttribute(MailService.A_ADDRESS, null);
            if (str != null) {
                if (needSpace) {
                    toRet.append(" ");
                }
                toRet.append(str);
            }
            
            toRet.append("\" ");
            
        }
        
        return toRet.toString();
    }
    
    public String toString() {
        return mElement.toString();
    }
    
    public String getServer() {
        ProxiedQueryResults res = (ProxiedQueryResults) getResults();
        return res.getServer();
    }
    
    public Element getElement() { 
        return mElement;
    }

}
