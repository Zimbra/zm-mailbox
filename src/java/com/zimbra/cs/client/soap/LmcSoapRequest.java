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

package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import org.dom4j.Element;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AdminConstants;

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

    /*
      * If requestedAccountId is not null, the request is sent on behalf of
      * the account.
      */
    protected String mRequestedAccountId;

    public String getRequestedAccountId() {
        return mRequestedAccountId;
    }

    public void setRequestedAccountId(String id) {
        mRequestedAccountId = id;
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
                ZAuthToken zat = mSession.getAuthToken();
                trans.setAuthToken(zat.getType(), zat.getValue(), zat.getAttrs());
                curSessionID = mSession.getSessionID();
                trans.setSessionId(curSessionID);
            }

            // send it over
            Element requestXML = getRequestXML();
            if (sDumpXML) {
                sLog.info("Request:" + DomUtil.toString(requestXML, true));
            }
            com.zimbra.common.soap.Element requestElt = com.zimbra.common.soap.Element.convertDOM(requestXML);
            //System.out.println("Sending over request " + DomUtil.toString(requestXML, true));
            Element responseXML;
            if (mRequestedAccountId == null)
                responseXML = trans.invoke(requestElt).toXML();
            else
                responseXML = trans.invoke(requestElt, false, true, mRequestedAccountId).toXML();
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
        for (Iterator ait = parentElem.elementIterator(MailConstants.E_CONTACT); ait.hasNext(); ) {
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
        Element newAttr = DomUtil.add(parent, MailConstants.E_ATTRIBUTE,
                                      (attrValue == null) ? "" : attrValue);
        DomUtil.addAttr(newAttr, MailConstants.A_ATTRIBUTE_NAME, attr.getAttrName());
    }

    protected LmcConversation parseConversation(Element conv)
        throws LmcSoapClientException, ServiceException
    {
        LmcConversation result = new LmcConversation();

        // get the conversation attributes
        result.setID(conv.attributeValue(MailConstants.A_ID));
        result.setTags(conv.attributeValue(MailConstants.A_TAGS));
        String numMsgs = conv.attributeValue(MailConstants.A_NUM);
        if (numMsgs != null)
            result.setNumMessages(Integer.parseInt(numMsgs));
        result.setDate(conv.attributeValue(MailConstants.A_DATE));
        result.setFlags(conv.attributeValue(MailConstants.A_FLAGS));
        result.setFolder(conv.attributeValue(MailConstants.A_FOLDER));

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
            if (elementType.equals(MailConstants.E_FRAG)) {
                // fragment
                result.setFragment(e.getText());
            } else if (elementType.equals(MailConstants.E_EMAIL)) {
                // e-mail address
                LmcEmailAddress ea = parseEmailAddress(e);
                emailAddrs.add(ea);
            } else if (elementType.equals(MailConstants.E_SUBJECT)) {
                // subject
                result.setSubject(e.getText());
            } else if (elementType.equals(MailConstants.E_MSG)) {
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
        String attrName = DomUtil.getAttr(cna, MailConstants.A_ATTRIBUTE_NAME);
        String attrID = cna.attributeValue(MailConstants.A_ID);
        String ref = cna.attributeValue(MailConstants.A_REF);
        String attrData = cna.getText();

        LmcContactAttr lca = new LmcContactAttr(attrName, attrID, ref, attrData);
        return lca;
    }


    protected LmcContact parseContact(Element cn)
        throws ServiceException
    {
        LmcContact result = new LmcContact();

        // get the element's attributes
        result.setID(DomUtil.getAttr(cn, MailConstants.A_ID));
        result.setTags(cn.attributeValue(MailConstants.A_TAGS));
        result.setFlags(cn.attributeValue(MailConstants.A_FLAGS));
        result.setFolder(cn.attributeValue(MailConstants.A_FOLDER));

        // get the contact attributes (<a> elements)
        ArrayList cnAttrs = new ArrayList();
        for (Iterator ait = cn.elementIterator(MailConstants.E_ATTRIBUTE); ait.hasNext(); ) {
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
        result.setID(DomUtil.getAttr(msg, MailConstants.A_ID));
        result.setFlags(msg.attributeValue(MailConstants.A_FLAGS));
        String size = msg.attributeValue(MailConstants.A_SIZE);
        if (size != null)
            result.setSize(Integer.parseInt(size));
        result.setContentMatched(msg.attributeValue(MailConstants.A_CONTENTMATCHED));
        result.setDate(msg.attributeValue(MailConstants.A_DATE));
        result.setConvID(msg.attributeValue(MailConstants.A_CONV_ID));
        result.setFolder(msg.attributeValue(MailConstants.A_FOLDER));
        result.setOriginalID(msg.attributeValue(MailConstants.A_ORIG_ID));

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
            if (elementType.equals(MailConstants.E_FRAG)) {
                // fragment
                result.setFragment(e.getText());
            } else if (elementType.equals(MailConstants.E_EMAIL)) {
                // e-mail address
                LmcEmailAddress ea = parseEmailAddress(e);
                emailAddrs.add(ea);
            } else if (elementType.equals(MailConstants.E_SUBJECT)) {
                // subject
                result.setSubject(e.getText());
            } else if (elementType.equals(MailConstants.E_MIMEPART)) {
                // MIME part
                LmcMimePart mp = parseMimePart(e);
                result.addMimePart(mp);
            } else if (elementType.equals(MailConstants.E_MSG_ID_HDR)) {
                // message ID header
                result.setMsgIDHeader(e.getText());
            } else if (elementType.equals(MailConstants.E_INVITE)) {
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
        result.setPartName(DomUtil.getAttr(mp, MailConstants.A_PART));
        result.setIsBody(mp.attributeValue(MailConstants.A_BODY));
        result.setSize(mp.attributeValue(MailConstants.A_SIZE));
        result.setMessageID(mp.attributeValue(MailConstants.A_MESSAGE_ID));
        result.setConvID(mp.attributeValue(MailConstants.A_CONV_ID));
        result.setContentType(mp.attributeValue(MailConstants.A_CONTENT_TYPE));
        result.setContentTypeName(mp.attributeValue(MailConstants.A_CONTENT_NAME));
        result.setContentDisp(mp.attributeValue(MailConstants.A_CONTENT_DISPOSTION));
        result.setContentDispFilename(mp.attributeValue(MailConstants.A_CONTENT_FILENAME));
        // XXX assume that content description is an attr of <mp> and not <content>
        result.setContentDesc(mp.attributeValue(MailConstants.A_CONTENT_DESCRIPTION));

        // parse any interior elements (content or another MIME part)
        ArrayList subMimeParts = new ArrayList();
        for (Iterator it = mp.elementIterator(); it.hasNext();) {
            Element e = (Element) it.next();

            // find out what element it is and go process that
            String elementType = e.getQName().getName();
            if (elementType.equals(MailConstants.E_CONTENT)) {
                addContent(result, e);
            } else if (elementType.equals(MailConstants.E_MIMEPART)) {
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
        result.setType(ea.attributeValue(MailConstants.A_ADDRESS_TYPE));
        result.setEmailID(ea.attributeValue(MailConstants.A_ID));
        result.setReferencedID(ea.attributeValue(MailConstants.A_REF));
        result.setPersonalName(ea.attributeValue(MailConstants.A_PERSONAL));
        result.setEmailAddress(ea.attributeValue(MailConstants.A_ADDRESS));
        result.setDisplayName(ea.attributeValue(MailConstants.A_DISPLAY));

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
        response.setFolderID(DomUtil.getAttr(f, MailConstants.A_ID));
        response.setName(f.attributeValue(MailConstants.A_NAME));
        response.setParentID(f.attributeValue(MailConstants.A_FOLDER));
        response.setNumUnread(f.attributeValue(MailConstants.A_UNREAD));
        response.setView(f.attributeValue(MailConstants.A_DEFAULT_VIEW));

        // recurse if necessary
        ArrayList subFolders = new ArrayList();
        for (Iterator ait = f.elementIterator(MailConstants.E_FOLDER); ait.hasNext(); ) {
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
        String name = DomUtil.getAttr(e, AdminConstants.A_NAME);
        String value = e.getText();
        StringUtil.addToMultiMap(prefMap, name, value);
    }

    protected LmcNote parseNote(Element n)
        throws ServiceException
    {
        LmcNote result = new LmcNote();

        // grab all the attributes
        result.setID(DomUtil.getAttr(n, MailConstants.A_ID));
        result.setTags(n.attributeValue(MailConstants.A_TAGS));
        result.setDate(DomUtil.getAttr(n, MailConstants.A_DATE));
        result.setFolder(DomUtil.getAttr(n, MailConstants.A_FOLDER));
        result.setPosition(DomUtil.getAttr(n, MailConstants.A_BOUNDS));
        result.setColor(DomUtil.getAttr(n, MailConstants.A_COLOR));

        // get the content
        Element c = DomUtil.get(n, MailConstants.E_CONTENT);
        result.setContent(c.getText());

        return result;
    }

    protected LmcTag parseTag(Element t)
        throws ServiceException
    {
        String name = DomUtil.getAttr(t, MailConstants.A_NAME);
        String id = DomUtil.getAttr(t, MailConstants.A_ID);
        String color = DomUtil.getAttr(t, MailConstants.A_COLOR);
        long unreadCount = DomUtil.getAttrLong(t, MailConstants.A_UNREAD, -1);
        LmcTag newTag = (unreadCount == -1) ? new LmcTag(id, name, color)
                : new LmcTag(id, name, color, unreadCount);
        return newTag;
    }

    protected LmcDocument parseDocument(Element doc) {
        LmcDocument result = parseDocumentCommon(doc, new LmcDocument());
        return result;
    }

    protected LmcWiki parseWiki(Element wiki) {
        LmcWiki result = new LmcWiki();
        parseDocumentCommon(wiki, result);

        result.setWikiWord(wiki.attributeValue(MailConstants.A_NAME));

        try {
            Element c = DomUtil.get(wiki, MailConstants.A_BODY);
            result.setContents(c.getText());
        } catch (Exception e) {}

        return result;
    }

    private LmcDocument parseDocumentCommon(Element doc, LmcDocument result) {
        result.setID(doc.attributeValue(MailConstants.A_ID));
        result.setName(doc.attributeValue(MailConstants.A_NAME));
        result.setContentType(doc.attributeValue(MailConstants.A_CONTENT_TYPE));
        result.setFolder(doc.attributeValue(MailConstants.A_FOLDER));
        result.setRev(doc.attributeValue(MailConstants.A_VERSION));
        result.setLastModifiedDate(doc.attributeValue(MailConstants.A_DATE));
        result.setLastEditor(doc.attributeValue(MailConstants.A_LAST_EDITED_BY));
        result.setRestUrl(doc.attributeValue(MailConstants.A_REST_URL));
        result.setCreator(doc.attributeValue(MailConstants.A_CREATOR));
        result.setCreateDate(doc.attributeValue(MailConstants.A_CREATED_DATE));

        for (Iterator it = doc.elementIterator(); it.hasNext();) {
            Element e = (Element) it.next();

            String elementType = e.getQName().getName();
            if (elementType.equals(MailConstants.E_FRAG)) {
                // fragment
                result.setFragment(e.getText());
            }
        }
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
        Element m = DomUtil.add(e, MailConstants.E_MSG, "");

        // attributes on the message element
        addAttrNotNull(m, MailConstants.A_ORIG_ID, msg.getOriginalID());
        addAttrNotNull(m, MailConstants.A_FOLDER, msg.getFolder());
        addAttrNotNull(m, MailConstants.A_TAG, msg.getTag());

        // for all e-mail addresses, add them
        LmcEmailAddress addrs[] = msg.getEmailAddresses();
        for (int i = 0; addrs != null && i < addrs.length; i++)
            addEmailAddress(m, addrs[i]);

        // add subject
        DomUtil.add(m, MailConstants.E_SUBJECT, msg.getSubject());

        // content if present
        String content = msg.getContent();
        if (content != null)
            DomUtil.add(m, MailConstants.E_CONTENT, content);

        // In-Reply-To header if present
        if (inReplyTo != null)
            DomUtil.add(m, MailConstants.E_IN_REPLY_TO, inReplyTo);

        // message parts
        LmcMimePart mp = msg.getMimePart(0);
        if (mp != null)
            addMimePart(m, mp);

        // attachment ID's if present
        String attachmentIDs[] = msg.getAttachmentIDs();
        if (attachmentIDs != null) {
            for (int i = 0; i < attachmentIDs.length; i++) {
                Element aid = DomUtil.add(m, MailConstants.E_ATTACH, "");
                addAttrNotNull(aid, MailConstants.A_ATTACHMENT_ID, attachmentIDs[i]);
            }
        }

        // forwarding messages with attachments
        if (fwdPartNumbers != null) {
            Element attach = DomUtil.add(m, MailConstants.E_ATTACH, "");
            for (int i = 0; i < fwdPartNumbers.length; i++) {
                Element part = DomUtil.add(attach, MailConstants.E_MIMEPART, "");
                DomUtil.addAttr(part, MailConstants.A_MESSAGE_ID, fwdMsgID);
                DomUtil.addAttr(part, MailConstants.A_PART, fwdPartNumbers[i]);
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
        Element mpElem = DomUtil.add(m, MailConstants.E_MIMEPART, "");
        addAttrNotNull(mpElem, MailConstants.A_PART, mp.getPartName());
        addAttrNotNull(mpElem, MailConstants.A_BODY, mp.getIsBody());
        addAttrNotNull(mpElem, MailConstants.A_SIZE, mp.getSize());
        addAttrNotNull(mpElem, MailConstants.A_MESSAGE_ID, mp.getMessageID());
        addAttrNotNull(mpElem, MailConstants.A_CONV_ID, mp.getConvID());
        addAttrNotNull(mpElem, MailConstants.A_CONTENT_TYPE, mp.getContentType());
        addAttrNotNull(mpElem, MailConstants.A_CONTENT_NAME, mp.getContentTypeName());
        addAttrNotNull(mpElem, MailConstants.A_CONTENT_DISPOSTION, mp.getContentDisp());
        addAttrNotNull(mpElem, "filename", mp.getContentDispFilename());  // XXX: need constant

        // add the content element if present
        String content = mp.getContent();
        if (content != null) {
            Element cElem = DomUtil.add(mpElem, MailConstants.E_CONTENT, content);
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
        Element e = DomUtil.add(m, MailConstants.E_EMAIL,
                                (content == null) ? "" : content);
        addAttrNotNull(e, MailConstants.A_ADDRESS_TYPE, addr.getType());
        addAttrNotNull(e, MailConstants.A_ADDRESS, addr.getEmailAddress());
        addAttrNotNull(e, MailConstants.A_PERSONAL, addr.getPersonalName());
    }
}