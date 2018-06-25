/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.jetty.http.HttpHeader;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;

public class DavRequest {

    public DavRequest(String uri, String method) {
        mUri = uri;
        mMethod = method;
        mDepth = Depth.zero;
        mHeaders = new ArrayList<Pair<String,String>>();
    }

    public Document getRequestMessage() {
        return mDoc;
    }

    public void setRedirectUrl(String url) {
        mRedirectUrl = url;
    }

    public void setRequestMessage(Element root) {
        if (mDoc == null) {
            mDoc = DocumentHelper.createDocument();
        }
        mDoc.setRootElement(root);
    }

    public void addRequestProp(QName p) {
        if (mDoc == null) {
            return;
        }
        Element prop = mDoc.getRootElement().element(DavElements.E_PROP);
        if (prop == null) {
            return;
        }
        prop.addElement(p);
    }

    public void addHref(String href) {
        if (mDoc == null) {
            return;
        }
        Element el = mDoc.getRootElement().addElement(DavElements.E_HREF);
        el.setText(href);
    }

    public void addRequestElement(Element e) {
        if (mDoc == null) {
            return;
        }
        mDoc.getRootElement().add(e);
    }

    public void addRequestHeader(String name, String value) {
        mHeaders.add(new Pair<String,String>(name, value));
    }

    public ArrayList<Pair<String,String>> getRequestHeaders() {
        return mHeaders;
    }

    public void setDepth(Depth d) {
        mDepth = d;
    }

    public Depth getDepth() {
        return mDepth;
    }

    public String getUri() {
        return mUri;
    }
    public String getMethod() {
        return mMethod;
    }
    public String getRequestMessageString() throws IOException {
        if (mDoc != null) {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setTrimText(false);
            format.setOmitEncoding(false);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(mDoc);
            return new String(out.toByteArray(), "UTF-8");
        }
        return "";
    }

    public static class DocumentRequestEntity implements HttpEntity {
        private final Document doc;
        private byte[] buffer;
        public DocumentRequestEntity(Document d) { doc = d; buffer = null; }
        @Override
        public boolean isRepeatable() { return true; }
        @Override
        public long getContentLength() {
            if (buffer == null)
                try {
                    getContents();
                } catch (Exception e) {
                }
            if (buffer == null) {
                return -1;
            }
            return buffer.length;
        }

        public Header getContentType() { return new BasicHeader(HttpHeader.CONTENT_TYPE.name(), "text/xml"); }
  
        public void writeRequest(OutputStream out) throws IOException {
            if (buffer != null) {
                out.write(buffer);
                return;
            }
            OutputFormat format = OutputFormat.createCompactFormat();
            format.setTrimText(false);
            format.setOmitEncoding(false);
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(doc);
        }
        private void getContents() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeRequest(out);
            buffer = out.toByteArray();
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#consumeContent()
         */
        @Override
        public void consumeContent() throws IOException {
            // TODO Auto-generated method stub
            
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#getContent()
         */
        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            // TODO Auto-generated method stub
            return null;
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#getContentEncoding()
         */
        @Override
        public Header getContentEncoding() {
            // TODO Auto-generated method stub
            return null;
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#isChunked()
         */
        @Override
        public boolean isChunked() {
            // TODO Auto-generated method stub
            return false;
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#isStreaming()
         */
        @Override
        public boolean isStreaming() {
            // TODO Auto-generated method stub
            return false;
        }
        /* (non-Javadoc)
         * @see org.apache.http.HttpEntity#writeTo(java.io.OutputStream)
         */
        @Override
        public void writeTo(OutputStream arg0) throws IOException {
            // TODO Auto-generated method stub
            
        }
        
        
    }

    public HttpRequestBase getHttpMethod(String baseUrl) {
        String url = mRedirectUrl;
        if (url == null) {
            // Normally, mUri is a relative URL but sometimes it is a full URL.
            // For instance iCloud currently specifies a full URL instead of a relative one for calendar-home-set
            // in a PROPFIND result, even though it specifies relative ones for schedule-inbox-URL and
            // schedule-outbox-URL in the same report.
            try {
                new URL(mUri);  // This will throw an exception when the URL is a relative one.
                url = mUri;
            } catch (MalformedURLException e) {
                url = baseUrl + mUri;
            }
        }

        if (mDoc == null)
            return new HttpGet(url) {
                @Override
                public String getMethod() {
                    return mMethod;
                }
            };

        HttpPut m = new HttpPut(url) {
            HttpEntity re;
            @Override
            public String getMethod() {
                return mMethod;
            }
            @Override
            public HttpEntity getEntity() {
                return re;
            }
            @Override
            public void setEntity(HttpEntity requestEntity) {
                re = requestEntity;
                super.setEntity(requestEntity);
            }
        };
        DocumentRequestEntity re = new DocumentRequestEntity(mDoc);
        m.setEntity(re);
        return m;
    }

    private Document mDoc;
    private final String mUri;
    private String mRedirectUrl;
    private final String mMethod;
    private Depth mDepth;
    private final ArrayList<Pair<String,String>> mHeaders;

    private static final String PROPFIND = "PROPFIND";
    private static final String REPORT = "REPORT";
    private static final String DELETE = "DELETE";
    private static final String MKCOL = "MKCOL";
    private static final String MKCALENDAR = "MKCALENDAR";
    private static final String PROPPATCH = "PROPPATCH";
    private static final String OPTION = "OPTION";

    public static DavRequest PROPFIND(String uri) {
        DavRequest req = new DavRequest(uri, PROPFIND);
        Element root = DocumentHelper.createElement(DavElements.E_PROPFIND);
        root.addElement(DavElements.E_PROP);
        req.setRequestMessage(root);
        return req;
    }

    public static DavRequest CALENDARMULTIGET(String uri) {
        DavRequest req = new DavRequest(uri, REPORT);
        Element root = DocumentHelper.createElement(DavElements.E_CALENDAR_MULTIGET);
        root.addElement(DavElements.E_PROP);
        req.setRequestMessage(root);
        return req;
    }

    public static DavRequest EXPAND(String uri) {
        DavRequest req = new DavRequest(uri, REPORT);
        Element root = DocumentHelper.createElement(DavElements.E_EXPAND_PROPERTY);
        req.setRequestMessage(root);
        return req;
    }

    public static DavRequest CALENDARQUERY(String uri) {
        DavRequest req = new DavRequest(uri, REPORT);
        Element root = DocumentHelper.createElement(DavElements.E_CALENDAR_QUERY);
        root.addElement(DavElements.E_PROP);
        root.addElement(DavElements.E_FILTER).addElement(DavElements.E_COMP_FILTER).addAttribute(DavElements.P_NAME, "VCALENDAR");
        req.setRequestMessage(root);
        return req;
    }

    public static DavRequest DELETE(String uri) {
        DavRequest req = new DavRequest(uri, DELETE);
        return req;
    }

    public static DavRequest MKCOL(String uri) {
        DavRequest req = new DavRequest(uri, MKCOL);
        return req;
    }

    public static DavRequest MKCALENDAR(String uri) {
        DavRequest req = new DavRequest(uri, MKCALENDAR);
        return req;
    }

    public static DavRequest PROPPATCH(String uri) {
        DavRequest req = new DavRequest(uri, PROPPATCH);
        Element root = DocumentHelper.createElement(DavElements.E_PROPERTYUPDATE);
        root.addElement(DavElements.E_SET).addElement(DavElements.E_PROP);
        req.setRequestMessage(root);
        return req;
    }

    public static DavRequest OPTION(String uri) {
        DavRequest req = new DavRequest(uri, OPTION);
        return req;
    }
}
