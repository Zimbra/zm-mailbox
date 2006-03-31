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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 28, 2005
 */
package com.zimbra.cs.index;

import java.util.Iterator;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.Element;



/**
 * @author tim
 *
 * A ZimbraHit which is being proxied from another server: ie we did a SOAP request
 * somewhere else and are now wrapping results we got from request.
 */
public class ProxiedHit extends ZimbraHit 
{
    protected long mProxiedDate = -1;
    protected int mProxiedConvId = -1;
    protected int mProxiedMsgId = -1;
    protected byte mProxiedItemType = -1;
    protected String mProxiedSubject = null;
    protected String mProxiedName = null;
    protected ItemId itemID = null;
    
    protected Element mElement;
    
    public ItemId getParsedItemID() throws ServiceException {
        if (itemID == null)
            itemID = new ItemId(mElement.getAttribute(MailService.A_ID), null);
        return itemID;
    }

    public ProxiedHit(ProxiedQueryResults results, Element elt) {
        super(results, null, 0.0f);
        mElement = elt;
    }
    
    int getSize() throws ServiceException {
        return (int) mElement.getAttributeLong(MailService.A_SIZE, 0);
    }
    
    long getDate() throws ServiceException {
        if (mProxiedDate < 0) {
            mProxiedDate = mElement.getAttributeLong(MailService.A_DATE, 0);
            if (mProxiedDate == 0) {
            	mProxiedDate = mElement.getAttributeLong(MailService.A_SORT_FIELD, 0);
            }
        }
        return mProxiedDate;
    }

    int getConversationId() throws ServiceException {
        if (mProxiedConvId <= 0) {
        	mProxiedConvId = (int) mElement.getAttributeLong(MailService.A_CONV_ID, 0);
        }
        return mProxiedConvId;
    }
    
    public int getItemId() throws ServiceException {
        if (mProxiedMsgId <= 0) {
            ItemId id = getParsedItemID();
            mProxiedMsgId = id.getId();
        }
        return mProxiedMsgId;
    }
    
    byte getItemType() throws ServiceException {
        if (mProxiedItemType <= 0) {
            mProxiedItemType = (byte) mElement.getAttributeLong(MailService.A_ITEM_TYPE);
        }
        return mProxiedItemType;
    }
    
    void setItem(MailItem item) {
        assert(false); // can't preload a proxied hit!
    }

    boolean itemIsLoaded() {
        return true;
    }
    

    String getSubject() throws ServiceException {
        if (mProxiedSubject == null) {
            mProxiedSubject = mElement.getAttribute(MailService.E_SUBJECT, null);
            if (mProxiedSubject == null) {
            	mProxiedSubject = mElement.getAttribute(MailService.A_SORT_FIELD);
            }
        }
        return mProxiedSubject;
    }
    
    String getFragment() throws ServiceException {
        Element frag = mElement.getOptionalElement(MailService.E_FRAG);
        if (frag != null) {
            return frag.getText();
        }
        return "";
    }

    String getName() throws ServiceException {
        if (mProxiedName == null) {
        	mProxiedName = mElement.getAttribute(MailService.A_SORT_FIELD);
        }
        return mProxiedName;
    	
//        StringBuffer toRet = new StringBuffer();
//        for (Iterator iter = mElement.elementIterator(MailService.E_EMAIL); iter.hasNext(); ) 
//        {
//            Element cur = (Element)(iter.next());
//            
//            String type = cur.getAttribute(MailService.A_ADDRESS_TYPE);
//            String typeStr = "";
//            if (type.equals("f")) {
//                typeStr = "from";
//            } else if (type.equals("s")) {
//                typeStr = "sender";
//            } else if (type.equals("t")) {
//                typeStr = "to";
//            } else if (type.equals("r")) {
//                typeStr = "reply-to";
//            } else if (type.equals("c")) {
//                typeStr = "cc";
//            } else if (type.equals("b")) {
//                typeStr = "bcc";
//            }
//   
//            toRet.append(typeStr);
//            toRet.append(":\"");
//            
//            boolean needSpace = false;
//
//            String str = cur.getAttribute(MailService.A_PERSONAL, null);
//            if (str != null) {
//                toRet.append(str);
//                needSpace = true;
//            } else {
//                str = cur.getAttribute(MailService.A_DISPLAY, null);
//                if (str != null) {
//                    if (needSpace) {
//                        toRet.append(" ");
//                    }
//                    toRet.append(str);
//                    needSpace = true;
//                }
//            }
//            
//            str = cur.getAttribute(MailService.A_ADDRESS, null);
//            if (str != null) {
//                if (needSpace) {
//                    toRet.append(" ");
//                }
//                toRet.append(str);
//            }
//            
//            toRet.append("\" ");
//            
//        }
//        
//        return toRet.toString();
    }
    
    public String toString() {
        return mElement.toString();
    }
    
    String getServer() {
        ProxiedQueryResults res = (ProxiedQueryResults) getResults();
        return res.getServer();
    }
    
    public Element getElement() { 
        return mElement;
    }

}
