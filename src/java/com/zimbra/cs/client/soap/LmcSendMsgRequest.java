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

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.*;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.client.*;

public class LmcSendMsgRequest extends LmcSoapRequest {

    private LmcMessage mMsg;
    private String mInReplyTo;
    private String mFwdMsgID;
    private String[] mFwdPartNumbers;

    /**
     * Set the message that will be sent
     * @param m - the message to be sent
     */
    public void setMsg(LmcMessage m) { mMsg = m; }

    public LmcMessage getMsg() { return mMsg; }

    public void setReplyInfo(String inReplyTo) {
        mInReplyTo = inReplyTo;
    }

    public void setFwdInfo(String inReplyTo, String fwdMsgID, String[] fwdPartNumbers) {
        mInReplyTo = inReplyTo;
        mFwdMsgID = fwdMsgID;
        mFwdPartNumbers = fwdPartNumbers;
    }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.SEND_MSG_REQUEST);
        addMsg(request, mMsg, mInReplyTo, mFwdMsgID, mFwdPartNumbers);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        // this assumes, per soap.txt, that only the ID attribute is needed
        Element m = DomUtil.get(responseXML, MailConstants.E_MSG);
        LmcSendMsgResponse response = new LmcSendMsgResponse();
        response.setID(m.attributeValue(MailConstants.A_ID));
        return response;
    }

    /*
    * Post the attachment represented by File f and return the attachment ID
    */
    public String postAttachment(String uploadURL,
                                 LmcSession session,
                                 File f,
                                 String domain,  // cookie domain e.g. ".example.zimbra.com"
                                 int msTimeout)
        throws LmcSoapClientException, IOException
    {
        String aid = null;

        // set the cookie.
        if (session == null)
            System.err.println(System.currentTimeMillis() + " " + Thread.currentThread() + " LmcSendMsgRequest.postAttachment session=null");
        
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(uploadURL);
        ZAuthToken zat = session.getAuthToken();
        Map<String, String> cookieMap = zat.cookieMap(false);
        if (cookieMap != null) {
            HttpState initialState = new HttpState();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                Cookie cookie = new Cookie(domain, ck.getKey(), ck.getValue(), "/", -1, false);
                initialState.addCookie(cookie);
            }
            client.setState(initialState);
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        }
        client.getHttpConnectionManager().getParams().setConnectionTimeout(msTimeout);
        int statusCode = -1;
        try {
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(f.getName());
            Part[] parts = { new FilePart(f.getName(), f, contentType, "UTF-8") };
            post.setRequestEntity( new MultipartRequestEntity(parts, post.getParams()) );
            statusCode = client.executeMethod(post);

            // parse the response
            if (statusCode == 200) {
                // paw through the returned HTML and get the attachment id
                String response = post.getResponseBodyAsString();
                //System.out.println("response is\n" + response);
                int lastQuote = response.lastIndexOf("'");
                int firstQuote = response.indexOf("','") + 3;
                if (lastQuote == -1 || firstQuote == -1)
                    throw new LmcSoapClientException("Attachment post failed, unexpected response: " + response);
                aid = response.substring(firstQuote, lastQuote);
            } else {
                throw new LmcSoapClientException("Attachment post failed, status=" + statusCode);
            }
        } catch (IOException e) {
            System.err.println("Attachment post failed");
            e.printStackTrace();
            throw e;
        } finally {
            post.releaseConnection();
        }

        return aid;
    }
}
