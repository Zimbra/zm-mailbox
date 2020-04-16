/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.client.soap;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.client.LmcMessage;
import com.zimbra.cs.client.LmcSession;

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

    @Override
    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.SEND_MSG_REQUEST);
        addMsg(request, mMsg, mInReplyTo, mFwdMsgID, mFwdPartNumbers);
        return request;
    }

    @Override
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
        throws LmcSoapClientException, IOException, HttpException
    {
        String aid = null;

        // set the cookie.
        if (session == null)
            System.err.println(System.currentTimeMillis() + " " + Thread.currentThread() + " LmcSendMsgRequest.postAttachment session=null");

        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpPost post = new HttpPost(uploadURL);
        ZAuthToken zat = session.getAuthToken();
        Map<String, String> cookieMap = zat.cookieMap(false);
        if (cookieMap != null) {
            BasicCookieStore initialState = new BasicCookieStore();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                cookie.setDomain(domain);
                cookie.setPath("/");
                cookie.setSecure(false);
                cookie.setExpiryDate(null);
                initialState.addCookie(cookie);
            }
            clientBuilder.setDefaultCookieStore(initialState);

            RequestConfig reqConfig = RequestConfig.copy(
                ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
                .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

            clientBuilder.setDefaultRequestConfig(reqConfig);
        }
        SocketConfig config = SocketConfig.custom().setSoTimeout(msTimeout).build();
        clientBuilder.setDefaultSocketConfig(config);
        int statusCode = -1;
        try {
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(f.getName());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("upfile", f, ContentType.create(contentType, "UTF-8"), f.getName());
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            HttpClient client = clientBuilder.build();
            HttpResponse httpResponse = HttpClientUtil.executeMethod(client, post);
            statusCode =  httpResponse.getStatusLine().getStatusCode();
            // parse the response
            if (statusCode == 200) {
                // paw through the returned HTML and get the attachment id
                String response = EntityUtils.toString( httpResponse.getEntity());
                //System.out.println("response is\n" + response);
                int lastQuote = response.lastIndexOf("'");
                int firstQuote = response.indexOf("','") + 3;
                if (lastQuote == -1 || firstQuote == -1)
                    throw new LmcSoapClientException("Attachment post failed, unexpected response: " + response);
                aid = response.substring(firstQuote, lastQuote);
            } else {
                throw new LmcSoapClientException("Attachment post failed, status=" + statusCode);
            }
        } catch (IOException | HttpException e) {
            System.err.println("Attachment post failed");
            e.printStackTrace();
            throw e;
        } finally {
            post.releaseConnection();
        }
        return aid;
    }
}
