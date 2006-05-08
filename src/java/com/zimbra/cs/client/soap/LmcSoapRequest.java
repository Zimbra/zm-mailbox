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

package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.util.StringUtil;

public abstract class LmcSoapRequest {

    private static Log sLog = LogFactory.getLog(LmcSoapRequest.class);

    private static boolean sDumpXML = false;
    public static synchronized void setDumpXML(boolean b) { sDumpXML = b; }

    private static int sRetryCount = 3;
    public static synchronized void setRetryCount(int n) { sRetryCount = n; }

    private static int sTimeoutMillis = 30000;
    public static synchronized void setTimeout(int millis) { sTimeoutMillis = millis; }

    /*
	 * If session is null, no auth information will be sent.  Otherwise the 
	 * auth information in session will be sent.
	 */
	protected LmcSession mSession;

	public LmcSession getSession() {
		return mSession;
	}

	public void setSession(LmcSession l) {
		mSession = l;
	}

	/**
	 * A particular type of request must implement this to return the XML
	 * that should be sent in the soap:body element.  
	 * XXX: this will do no validation.  may want a boolean to do validation
	 * to make it more general purpose.  don't want validation as a test
	 * client since you want to be able to simulate errors.
	 */
	protected abstract Element getRequestXML() throws LmcSoapClientException;

	/**
	 * A particular type of request must implement this to take the XML
	 * returned from the server, parse it, and create a SoapResponse
	 * object from it.  
	 * @param responseXML - the element that is the root of the soap:Body
	 * element that contains the server's response.
	 * @exception SoapParseException if the parser cannot find elements
	 * that are required in the response.
	 */
	protected abstract LmcSoapResponse parseResponseXML(Element responseXML)
		throws SoapParseException, ServiceException, LmcSoapClientException;

	/**
	 * After setting up all the request parameters, send this request to
	 * the targetURL and return the server's response.
     * This method will side-effect the LmcSession object assigned with
     * setSession, changing the session ID if the server changed it.
	 * @param targetURL - the URL of the SOAP service to send the request to
	 * @exception lotsOfthem
	 */
	public LmcSoapResponse invoke(String targetURL)
		throws LmcSoapClientException, IOException, SoapFaultException,
			   ServiceException, SoapParseException 
    {
		LmcSoapResponse result = null;

		SoapHttpTransport trans = null;
		try {
			trans = new SoapHttpTransport(targetURL);
			trans.setTimeout(sTimeoutMillis);
            trans.setRetryCount(sRetryCount);
            trans.setUserAgent("lmc", null);

			// set the auth token and session id in the transport for this request to use
            String curSessionID = null;
			if (mSession != null) {
				trans.setAuthToken(mSession.getAuthToken());
                curSessionID = mSession.getSessionID();
				trans.setSessionId(curSessionID);
			}

            // send it over
			Element requestXML = getRequestXML();
            if (sDumpXML) {
            	sLog.info("Request:" + DomUtil.toString(requestXML, true));
            }
            com.zimbra.soap.Element requestElt = com.zimbra.soap.Element.convertDOM(requestXML);
			//System.out.println("Sending over request " + DomUtil.toString(requestXML, true));
			Element responseXML = trans.invoke(requestElt).toXML();
            if (sDumpXML) {
                sLog.info("Response:" + DomUtil.toString(responseXML, true) + "\n");
            }
            
            /*
             * check to see if the session ID changed.  this will not change 
             * the session ID if there was one and trans.getSessionId() now
             * returns null.
             */
            String newSessionID = trans.getSessionId();
            if (newSessionID != null && mSession != null) {
                if (curSessionID == null)
                    mSession.setSessionID(newSessionID);
                else if (!curSessionID.equals(newSessionID))
                	mSession.setSessionID(newSessionID);
            }
                
            // parse the response
            //System.out.println(DomUtil.toString(responseXML, true));
			result = parseResponseXML(responseXML);

		} finally {
			if (trans != null)
				trans.shutdown();
		}
		return result;
	}
    
    protected LmcContact[] parseContactArray(Element parentElem) 
        throws ServiceException
    {
        // iterate over all the <cn> elements in parentElem
        ArrayList contactArray = new ArrayList();
        for (Iterator ait = parentElem.elementIterator(MailService.E_CONTACT); ait.hasNext(); ) {
            Element a = (Element) ait.next();
            contactArray.add(parseContact(a));
        }

        if (contactArray.isEmpty()) {
            return null;
        } else {
            LmcContact contacts[] = new LmcContact[contactArray.size()]; 
            return (LmcContact []) contactArray.toArray(contacts);
        } 
    }

	protected static void addAttrNotNull(Element elem, 
                                         String attrName,
										 String attrValue) 
    {
		if (attrValue != null)
			DomUtil.addAttr(elem, attrName, attrValue);
	}
    
    protected void addContactAttr(Element parent,
                                  LmcContactAttr attr)
    {
        String attrValue = attr.getAttrData();
    	Element newAttr = DomUtil.add(parent, MailService.E_ATTRIBUTE, 
    			                      (attrValue == null) ? "" : attrValue);
    	DomUtil.addAttr(newAttr, MailService.A_ATTRIBUTE_NAME, attr.getAttrName());
    }

	protected LmcConversation parseConversation(Element conv)
	    throws LmcSoapClientException, ServiceException 
    {
		LmcConversation result = new LmcConversation();

		// get the conversation attributes
		result.setID(conv.attributeValue(MailService.A_ID));
		result.setTags(conv.attributeValue(MailService.A_TAGS));
        String numMsgs = conv.attributeValue(MailService.A_NUM);
        if (numMsgs != null)
        	result.setNumMessages(Integer.parseInt(numMsgs));
		result.setDate(conv.attributeValue(MailService.A_DATE));
		result.setFlags(conv.attributeValue(MailService.A_FLAGS));
		result.setFolder(conv.attributeValue(MailService.A_FOLDER));

		/*
		 * iterate over subelements. allowed subelements are e-mail addresses,
		 * subject, fragment, and messages. this assumes no particular order is
		 * required for correctness.
		 */
		ArrayList emailAddrs = new ArrayList();
		ArrayList msgs = new ArrayList();
		for (Iterator it = conv.elementIterator(); it.hasNext();) {
			Element e = (Element) it.next();

			// find out what element it is and go process that
			String elementType = e.getQName().getName();
			if (elementType.equals(MailService.E_FRAG)) {
				// fragment
				result.setFragment(e.getText());
			} else if (elementType.equals(MailService.E_EMAIL)) {
				// e-mail address
				LmcEmailAddress ea = parseEmailAddress(e);
				emailAddrs.add(ea);
			} else if (elementType.equals(MailService.E_SUBJECT)) {
				// subject
				result.setSubject(e.getText());
			} else if (elementType.equals(MailService.E_MSG)) {
				// message
				LmcMessage m = parseMessage(e);
				msgs.add(m);
			} else {
				// don't know what it is
				throw new LmcSoapClientException("unknown element "
						+ elementType + " within conversation");
			}
		}

		// set the arrays in the result object
		if (!emailAddrs.isEmpty()) {
            LmcEmailAddress a[] = new LmcEmailAddress[emailAddrs.size()];
            result.setParticipants((LmcEmailAddress[]) emailAddrs.toArray(a));
        }
		if (!msgs.isEmpty()) {
            LmcMessage m[] = new LmcMessage[msgs.size()];
			result.setMessages((LmcMessage[]) msgs.toArray(m));
		}
		return result;
	}
    
    protected LmcContactAttr parseContactAttr(Element cna)
        throws ServiceException
    {
        // get the attributes
        String attrName = DomUtil.getAttr(cna, MailService.A_ATTRIBUTE_NAME);
        String attrID = cna.attributeValue(MailService.A_ID);
        String ref = cna.attributeValue(MailService.A_REF);
        String attrData = cna.getText();
        
        LmcContactAttr lca = new LmcContactAttr(attrName, attrID, ref, attrData);
        return lca;
    }

    
    protected LmcContact parseContact(Element cn)
        throws ServiceException
    {
        LmcContact result = new LmcContact();
    
        // get the element's attributes
        result.setID(DomUtil.getAttr(cn, MailService.A_ID));
        result.setTags(cn.attributeValue(MailService.A_TAGS));
        result.setFlags(cn.attributeValue(MailService.A_FLAGS));
        result.setFolder(cn.attributeValue(MailService.A_FOLDER));
        
        // get the contact attributes (<a> elements)
        ArrayList cnAttrs = new ArrayList();
        for (Iterator ait = cn.elementIterator(MailService.E_ATTRIBUTE); ait.hasNext(); ) {
            Element cnAttrElem = (Element) ait.next();
            cnAttrs.add(parseContactAttr(cnAttrElem));
        }
        if (!cnAttrs.isEmpty()) {
            LmcContactAttr cnAttrArray[] = new LmcContactAttr[cnAttrs.size()];
            result.setAttrs((LmcContactAttr []) cnAttrs.toArray(cnAttrArray));
        }       
              
        // XXX: not clear from spec if the <mp> element is used -- assume not 
        return result;
    }
    
	protected LmcMessage parseMessage(Element msg) 
        throws ServiceException, LmcSoapClientException 
    {
		LmcMessage result = new LmcMessage();

		// get the message attributes
		result.setID(DomUtil.getAttr(msg, MailService.A_ID));
		result.setFlags(msg.attributeValue(MailService.A_FLAGS));
        String size = msg.attributeValue(MailService.A_SIZE);
        if (size != null)
        	result.setSize(Integer.parseInt(size));
        result.setContentMatched(msg.attributeValue(MailService.A_CONTENTMATCHED));
		result.setDate(msg.attributeValue(MailService.A_DATE));
		result.setConvID(msg.attributeValue(MailService.A_CONV_ID));
		result.setFolder(msg.attributeValue(MailService.A_FOLDER));
		result.setOriginalID(msg.attributeValue(MailService.A_ORIG_ID));

		/*
		 * iterate over subelements. allowed subelements are content (at most
		 * 1), subject, fragment, and msg parts. this assumes no particular
		 * order is required for correctness.
		 */
		ArrayList emailAddrs = new ArrayList();
		for (Iterator it = msg.elementIterator(); it.hasNext();) {
			Element e = (Element) it.next();

			// find out what element it is and go process that
			String elementType = e.getQName().getName();
			if (elementType.equals(MailService.E_FRAG)) {
				// fragment
				result.setFragment(e.getText());
			} else if (elementType.equals(MailService.E_EMAIL)) {
				// e-mail address
				LmcEmailAddress ea = parseEmailAddress(e);
				emailAddrs.add(ea);
			} else if (elementType.equals(MailService.E_SUBJECT)) {
				// subject
				result.setSubject(e.getText());
			} else if (elementType.equals(MailService.E_MIMEPART)) {
				// MIME part
				LmcMimePart mp = parseMimePart(e);
				result.addMimePart(mp);
            } else if (elementType.equals(MailService.E_MSG_ID_HDR)) {
                // message ID header
                result.setMsgIDHeader(e.getText());
            } else if (elementType.equals(MailService.E_INVITE)) {
                // ignore appointment invites for now
			} else {
				// don't know what it is
				throw new LmcSoapClientException("unknown element "
						+ elementType + " within message");
			}
		}

		if (!emailAddrs.isEmpty()) {
            LmcEmailAddress a[] = new LmcEmailAddress[emailAddrs.size()];
            result.setEmailAddresses((LmcEmailAddress[]) emailAddrs.toArray(a));
        }

		return result;
	}

	protected LmcMimePart parseMimePart(Element mp)
			throws LmcSoapClientException, ServiceException 
    {
		LmcMimePart result = new LmcMimePart();

		// get the attributes
		result.setPartName(DomUtil.getAttr(mp, MailService.A_PART));
		result.setIsBody(mp.attributeValue(MailService.A_BODY));
		result.setSize(mp.attributeValue(MailService.A_SIZE));
		result.setMessageID(mp.attributeValue(MailService.A_MESSAGE_ID));
		result.setConvID(mp.attributeValue(MailService.A_CONV_ID));
		result.setContentType(mp.attributeValue(MailService.A_CONTENT_TYPE));
		result.setContentTypeName(mp.attributeValue(MailService.A_CONTENT_NAME));
		result.setContentDisp(mp.attributeValue(MailService.A_CONTENT_DISPOSTION));
		result.setContentDispFilename(mp.attributeValue(MailService.A_CONTENT_FILENAME));
		// XXX assume that content description is an attr of <mp> and not <content>
		result.setContentDesc(mp.attributeValue(MailService.A_CONTENT_DESCRIPTION));

		// parse any interior elements (content or another MIME part)
		ArrayList subMimeParts = new ArrayList();
		for (Iterator it = mp.elementIterator(); it.hasNext();) {
			Element e = (Element) it.next();

			// find out what element it is and go process that
			String elementType = e.getQName().getName();
			if (elementType.equals(MailService.E_CONTENT)) {
				addContent(result, e);
			} else if (elementType.equals(MailService.E_MIMEPART)) {
				LmcMimePart nextPart = parseMimePart(e);
				subMimeParts.add(nextPart);
			} else {
				// unexpected element
				throw new LmcSoapClientException("unexpected element "
						+ elementType);
			}
		}

		if (!subMimeParts.isEmpty()) {
			LmcMimePart mpArr[] = new LmcMimePart[subMimeParts.size()];
			result.setSubParts((LmcMimePart[]) subMimeParts.toArray(mpArr));
        }
		return result;
	}

	/** 
	 * Add the information in the Element c to the MimePart mp. 
	 * @param mp
	 * @param c
	 */
	protected void addContent(LmcMimePart mp, Element c) {
		// XXX need constant
		mp.setContentEncoding(c.attributeValue("cte"));
		mp.setContent(c.getText());
	}

	protected LmcEmailAddress parseEmailAddress(Element ea) {
		LmcEmailAddress result = new LmcEmailAddress();

		// grab all the attributes
		result.setType(ea.attributeValue(MailService.A_ADDRESS_TYPE));
		result.setEmailID(ea.attributeValue(MailService.A_ID));
		result.setReferencedID(ea.attributeValue(MailService.A_REF));
		result.setPersonalName(ea.attributeValue(MailService.A_PERSONAL));
		result.setEmailAddress(ea.attributeValue(MailService.A_ADDRESS));
		result.setDisplayName(ea.attributeValue(MailService.A_DISPLAY));

		// get the content if any
		result.setContent(ea.getText());

		return result;
	}

    /**
     * This parses the <folder> element pointed to by p.
     * @param p - the folder element
     * @return an LmcFolder object that is populated with data from the XML
     * @throws ServiceException
     */
	protected LmcFolder parseFolder(Element f) 
        throws ServiceException 
    {
		LmcFolder response = new LmcFolder();
        
        /*
         * Get the attributes of this folder element.  The root folder
         * object does not have the name, parent, and numUnread attributes.
         * Hence this parses them as optional.
         */
		response.setFolderID(DomUtil.getAttr(f, MailService.A_ID));
		response.setName(f.attributeValue(MailService.A_NAME));
		response.setParentID(f.attributeValue(MailService.A_FOLDER));
		response.setNumUnread(f.attributeValue(MailService.A_UNREAD));
		response.setView(f.attributeValue(MailService.A_DEFAULT_VIEW));
        
        // recurse if necessary
        ArrayList subFolders = new ArrayList();
        for (Iterator ait = f.elementIterator(MailService.E_FOLDER); ait.hasNext(); ) {
            Element sub = (Element) ait.next();
            subFolders.add(parseFolder(sub));
        }
        if (!subFolders.isEmpty()) {
            LmcFolder fs[] = new LmcFolder[subFolders.size()];
            response.setSubFolders((LmcFolder []) subFolders.toArray(fs));
        }       
		return response;
	}

	protected void addPrefToMultiMap(HashMap prefMap, 
                                     Element e)
			throws ServiceException 
    {
		String name = DomUtil.getAttr(e, AdminService.A_NAME);
		String value = e.getText();
		StringUtil.addToMultiMap(prefMap, name, value);
	}

	protected LmcNote parseNote(Element n) 
        throws ServiceException 
    {
		LmcNote result = new LmcNote();

		// grab all the attributes
		result.setID(DomUtil.getAttr(n, MailService.A_ID));
		result.setTags(n.attributeValue(MailService.A_TAGS));
		result.setDate(DomUtil.getAttr(n, MailService.A_DATE));
		result.setFolder(DomUtil.getAttr(n, MailService.A_FOLDER));
		result.setPosition(DomUtil.getAttr(n, MailService.A_BOUNDS));
		result.setColor(DomUtil.getAttr(n, MailService.A_COLOR));

		// get the content 
		Element c = DomUtil.get(n, MailService.E_CONTENT);
		result.setContent(c.getText());

		return result;
	}

	protected LmcTag parseTag(Element t) 
        throws ServiceException 
    {
		String name = DomUtil.getAttr(t, MailService.A_NAME);
		String id = DomUtil.getAttr(t, MailService.A_ID);
		String color = DomUtil.getAttr(t, MailService.A_COLOR);
		long unreadCount = DomUtil.getAttrLong(t, MailService.A_UNREAD, -1);
		LmcTag newTag = (unreadCount == -1) ? new LmcTag(id, name, color)
				: new LmcTag(id, name, color, unreadCount);
		return newTag;
	}

	protected LmcDocument parseDocument(Element doc)
		throws ServiceException	{
		LmcDocument result = new LmcDocument();

		result.setID(doc.attributeValue(MailService.A_ID));
		result.setName(doc.attributeValue(MailService.A_NAME));
		result.setContentType(doc.attributeValue(MailService.A_CONTENT_TYPE));
		result.setFolder(doc.attributeValue(MailService.A_FOLDER));
		result.setRev(doc.attributeValue(MailService.A_VERSION));
		result.setLastModifiedDate(doc.attributeValue(MailService.A_DATE));
		result.setLastEditor(doc.attributeValue(MailService.A_LAST_EDITED_BY));
		
		return result;
	}
	
	protected LmcDocument parseWiki(Element wiki)
		throws ServiceException	{
		LmcWiki result = new LmcWiki();

		result.setID(wiki.attributeValue(MailService.A_ID));
		result.setWikiWord(wiki.attributeValue(MailService.A_NAME));
		result.setFolder(wiki.attributeValue(MailService.A_FOLDER));
		result.setRev(wiki.attributeValue(MailService.A_VERSION));
		result.setLastModifiedDate(wiki.attributeValue(MailService.A_DATE));
		result.setLastEditor(wiki.attributeValue(MailService.A_LAST_EDITED_BY));
		
		return result;
	}

	/**
	 * Add the XML representation of the message to the element.
	 * @param e - the element at the root
	 * @param msg - the msg to be represented as XML
	 */
	protected void addMsg(Element e, 
                          LmcMessage msg,
                          String inReplyTo,
                          String fwdMsgID,
                          String[] fwdPartNumbers) 
    {
		Element m = DomUtil.add(e, MailService.E_MSG, "");

		// attributes on the message element
		addAttrNotNull(m, MailService.A_ORIG_ID, msg.getOriginalID());
        addAttrNotNull(m, MailService.A_FOLDER, msg.getFolder());
        addAttrNotNull(m, MailService.A_TAG, msg.getTag());

		// for all e-mail addresses, add them
		LmcEmailAddress addrs[] = msg.getEmailAddresses();
		for (int i = 0; addrs != null && i < addrs.length; i++)
			addEmailAddress(m, addrs[i]);

		// add subject
		DomUtil.add(m, MailService.E_SUBJECT, msg.getSubject());
        
        // content if present
        String content = msg.getContent();
        if (content != null)
            DomUtil.add(m, MailService.E_CONTENT, content);

        // In-Reply-To header if present
        if (inReplyTo != null)
            DomUtil.add(m, MailService.E_IN_REPLY_TO, inReplyTo);

		// message parts
        LmcMimePart mp = msg.getMimePart(0);
        if (mp != null)
        	addMimePart(m, mp);
        
        // attachment ID's if present
        String attachmentIDs[] = msg.getAttachmentIDs();
        if (attachmentIDs != null) {
            for (int i = 0; i < attachmentIDs.length; i++) {
            	Element aid = DomUtil.add(m, MailService.E_ATTACH, "");
                addAttrNotNull(aid, MailService.A_ATTACHMENT_ID, attachmentIDs[i]);
            }
        }

        // forwarding messages with attachments
        if (fwdPartNumbers != null) {
            Element attach = DomUtil.add(m, MailService.E_ATTACH, "");
        	for (int i = 0; i < fwdPartNumbers.length; i++) {
                Element part = DomUtil.add(attach, MailService.E_MIMEPART, "");
                DomUtil.addAttr(part, MailService.A_MESSAGE_ID, fwdMsgID);
                DomUtil.addAttr(part, MailService.A_PART, fwdPartNumbers[i]);
            }
        }
    }

    /**
     * Add the XML representation of the MIME part to the element.
     * @param m - the element
     * @param mp - the MIME part to be added
     */
    protected void addMimePart(Element m,
                               LmcMimePart mp)
    {
        Element mpElem = DomUtil.add(m, MailService.E_MIMEPART, "");
        addAttrNotNull(mpElem, MailService.A_PART, mp.getPartName());
        addAttrNotNull(mpElem, MailService.A_BODY, mp.getIsBody());
        addAttrNotNull(mpElem, MailService.A_SIZE, mp.getSize());
        addAttrNotNull(mpElem, MailService.A_MESSAGE_ID, mp.getMessageID());
        addAttrNotNull(mpElem, MailService.A_CONV_ID, mp.getConvID());
        addAttrNotNull(mpElem, MailService.A_CONTENT_TYPE, mp.getContentType());
        addAttrNotNull(mpElem, MailService.A_CONTENT_NAME, mp.getContentTypeName());
        addAttrNotNull(mpElem, MailService.A_CONTENT_DISPOSTION, mp.getContentDisp());
        addAttrNotNull(mpElem, "filename", mp.getContentDispFilename());  // XXX: need constant

        // add the content element if present
        String content = mp.getContent();
        if (content != null) {
        	Element cElem = DomUtil.add(mpElem, MailService.E_CONTENT, content);
            addAttrNotNull(cElem, "cte", mp.getContentEncoding()); // XXX: need constant
        }
        
        // add all subparts 
        LmcMimePart subParts[] = mp.getSubParts();
        for (int i = 0; subParts != null && i < subParts.length; i++) 
            addMimePart(mpElem, subParts[i]);
    }
    
    
	/**
	 * Add the XML representation of addr on to the element m
	 */
	protected void addEmailAddress(Element m, 
                                   LmcEmailAddress addr) 
    {
		String content = addr.getContent();
		Element e = DomUtil.add(m, MailService.E_EMAIL, 
                                (content == null) ? "" : content);
		addAttrNotNull(e, MailService.A_ADDRESS_TYPE, addr.getType());
		addAttrNotNull(e, MailService.A_ADDRESS, addr.getEmailAddress());
		addAttrNotNull(e, MailService.A_PERSONAL, addr.getPersonalName());
	}
}