/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.doc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.service.ServiceException.InternalArgument;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.mail.UploadScanner;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class SaveDocument extends DocDocumentHandler {

    private static String[] TARGET_DOC_ID_PATH = new String[] { MailConstants.E_DOC, MailConstants.A_ID };
    private static String[] TARGET_DOC_FOLDER_PATH = new String[] { MailConstants.E_DOC, MailConstants.A_FOLDER };

    @Override protected String[] getProxiedIdPath(Element request) {
        String id = getXPath(request, TARGET_DOC_ID_PATH);
        return id == null ? TARGET_DOC_FOLDER_PATH : TARGET_DOC_ID_PATH;
    }

    private static final String DEFAULT_DOCUMENT_FOLDER = "" + Mailbox.ID_FOLDER_BRIEFCASE;

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        Element docElem = request.getElement(MailConstants.E_DOC);

        Doc doc = null;
        Element response = null;
        boolean success = false;
        Mailbox mbox = null;
        int folderId = 0;
        try {
            String explicitName = docElem.getAttribute(MailConstants.A_NAME, null);
            String explicitCtype = docElem.getAttribute(MailConstants.A_CONTENT_TYPE, null);

            //bug 37180, extract the filename from the path (for IE). IE sends the full path.
            if (explicitName != null) {
                try {
                    explicitName = explicitName.replaceAll("\\\\", "/");
                    explicitName = explicitName.substring(explicitName.lastIndexOf("/") + 1);
                } catch (Exception e) {
                    //Do nothing
                }
            }

            String description = docElem.getAttribute(MailConstants.A_DESC, null);
            ItemId fid = new ItemId(docElem.getAttribute(MailConstants.A_FOLDER, DEFAULT_DOCUMENT_FOLDER), zsc);
            folderId = fid.getId();

            String id = docElem.getAttribute(MailConstants.A_ID, null);
            int itemId = id == null ? 0 : new ItemId(id, zsc).getId();
            int ver = (int) docElem.getAttributeLong(MailConstants.A_VERSION, 0);

            mbox = getRequestedMailbox(zsc);
            Element attElem = docElem.getOptionalElement(MailConstants.E_UPLOAD);
            Element msgElem = docElem.getOptionalElement(MailConstants.E_MSG);
            Element docRevElem = docElem.getOptionalElement(MailConstants.E_DOC);
            if (attElem != null) {
                String aid = attElem.getAttribute(MailConstants.A_ID, null);
                doc = getUploadedDoc(aid, zsc, explicitName, explicitCtype, description);
            } else if (msgElem != null) {
                String part = msgElem.getAttribute(MailConstants.A_PART);
                ItemId iid = new ItemId(msgElem.getAttribute(MailConstants.A_ID), zsc);
                doc = fetchMimePart(octxt, zsc.getAuthToken(), iid, part, explicitName, explicitCtype, description);
            } else if (docRevElem != null) {
                ItemId iid = new ItemId(docRevElem.getAttribute(MailConstants.A_ID), zsc);
                int revSource = (int) docRevElem.getAttributeLong(MailConstants.A_VERSION, 0);
                Account sourceAccount = Provisioning.getInstance().getAccountById(iid.getAccountId());
                if (sourceAccount.getId().equals(zsc.getRequestedAccountId())) {
                    Document docRev;
                    if (revSource == 0) {
                        docRev = mbox.getDocumentById(octxt, iid.getId());
                    } else {
                        docRev = (Document) mbox.getItemRevision(octxt, iid.getId(), MailItem.Type.DOCUMENT, revSource);
                    }
                    doc = new Doc(docRev);
                } else {
                    doc = new Doc(zsc.getAuthToken(), sourceAccount, iid.getId(), revSource);
                }
                // preserve the old name when adding a new revision with
                // the content from another document
                if (ver != 0) {
                    doc.name = null;
                }
            } else {
                String inlineContent = docElem.getAttribute(MailConstants.E_CONTENT);
                doc = new Doc(inlineContent, explicitName, explicitCtype, description);
            }
            
            // set content-type based on file extension.
            setDocContentType(doc);

            Document docItem = null;
            InputStream is = null;
            try {
                is = doc.getInputStream();
            } catch (IOException e) {
                throw ServiceException.FAILURE("can't save document", e);
            }
            if (itemId == 0) {
                // create a new page
                docItem = createDocument(doc, zsc, octxt, mbox, docElem, is, folderId, MailItem.Type.DOCUMENT);
            } else {
                // add a new revision
                docItem = mbox.getDocumentById(octxt, itemId);
                if (docItem.getVersion() != ver) {
                    throw MailServiceException.MODIFY_CONFLICT(new Argument(MailConstants.A_NAME, doc.name, Argument.Type.STR), new Argument(MailConstants.A_ID, itemId,
                            Argument.Type.IID), new Argument(MailConstants.A_VERSION, docItem.getVersion(), Argument.Type.NUM));
                }
                String name = docItem.getName();
                if (doc.name != null) {
                    name = doc.name;
                }
                boolean descEnabled = docElem.getAttributeBool(MailConstants.A_DESC_ENABLED, docItem.isDescriptionEnabled());
                docItem = mbox.addDocumentRevision(octxt, itemId, getAuthor(zsc), name, doc.description, descEnabled, is);
            }

            response = zsc.createElement(MailConstants.SAVE_DOCUMENT_RESPONSE);
            Element m = response.addElement(MailConstants.E_DOC);
            m.addAttribute(MailConstants.A_ID, new ItemIdFormatter(zsc).formatItemId(docItem));
            m.addAttribute(MailConstants.A_VERSION, docItem.getVersion());
            m.addAttribute(MailConstants.A_NAME, docItem.getName());
            success = true;
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                MailItem item = null;
                if (mbox != null && folderId != 0) {
                    item = mbox.getItemByPath(octxt, doc.name, folderId);
                }
                if (item != null && item instanceof Document) {
                    // name clash with another Document
                    throw MailServiceException.ALREADY_EXISTS("name " + doc.name + " in folder " + folderId, doc.name, item.getId(), ((Document) item).getVersion());
                } else if (item != null) {
                    // name clash with a folder
                    throw MailServiceException.ALREADY_EXISTS("name " + doc.name + " in folder " + folderId, doc.name, item.getId());
                }
            }
            throw e;
        } finally {
            if (success && doc != null) {
                doc.cleanup();
            }
        }
        return response;
    }

    protected Doc getUploadedDoc(String uploadId, ZimbraSoapContext zsc, String name, String ct, String description) throws ServiceException {
        ZimbraLog.mailbox.info("uploadId=%s", uploadId);
        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), uploadId, zsc.getAuthToken());
        // scan upload for viruses
        //StringBuffer is used here rather than a StringBuilder as UploadScanner could potentially be multi-threaded.
        StringBuffer info = new StringBuffer();
        UploadScanner.Result result = UploadScanner.accept(up, info);
        if (result == UploadScanner.REJECT)
            throw MailServiceException.UPLOAD_REJECTED(up.getName(), info.toString());
        if (result == UploadScanner.ERROR)
            throw MailServiceException.SCAN_ERROR(up.getName());

        Doc doc = new Doc(up, name, ct, description);
        return doc;
    }

    protected void setDocContentType(Doc doc) {
        // set content-type based on file extension.
        if (doc.name != null) {
            String guess = MimeDetect.getMimeDetect().detect(doc.name);
            if (guess != null)
                doc.contentType = guess;
        }
    }

    protected Document createDocument(Doc doc, ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox,
            Element docElem, InputStream is, int folderId, MailItem.Type type) throws ServiceException {
        return createDocument(doc, zsc, octxt, mbox, docElem, is, folderId, type, null, null, true);
    }

    protected Document createDocument(Doc doc, ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox,
            Element docElem, InputStream is, int folderId, MailItem.Type type, MailItem parent, CustomMetadata custom, boolean index) throws ServiceException {
        Document docItem = null;
        if (doc.name == null || doc.name.trim().equals("")) {
            throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_NAME, null);
        } else if (doc.contentType == null || doc.contentType.trim().equals("")) {
            throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_CONTENT_TYPE, null);
        }
        boolean descEnabled = false;
        String flags = "";
        if (docElem != null) {
            descEnabled = docElem.getAttributeBool(MailConstants.A_DESC_ENABLED, true);
            flags = docElem.getAttribute(MailConstants.A_FLAGS, null);
        }

        try {
            ParsedDocument pd = new ParsedDocument(is, doc.name, doc.contentType, System.currentTimeMillis(),
                getAuthor(zsc), doc.description, descEnabled);

            docItem = mbox.createDocument(octxt, folderId, pd, type, Flag.toBitmask(flags), parent, custom, index);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to create document", e);
        }
        return docItem;
    }

    private Doc fetchMimePart(OperationContext octxt, AuthToken authtoken, ItemId itemId, String partId, String name, String ct, String description) throws ServiceException {
        String accountId = itemId.getAccountId();
        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (Provisioning.onLocalServer(acct)) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            Message msg = mbox.getMessageById(octxt, itemId.getId());
            try {
                return new Doc(Mime.getMimePart(msg.getMimeMessage(), partId), name, ct);
            } catch (MessagingException e) {
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch mime part msgId=" + itemId + ", partId=" + partId, e);
            } catch (IOException e) {
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch mime part msgId=" + itemId + ", partId=" + partId, e);
            }
        }

        String url = UserServlet.getRestUrl(acct) + "?auth=co&id=" + itemId + "&part=" + partId;
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient().build();
        HttpGet get = new HttpGet(url);
        authtoken.encode(client, get, false, acct.getAttr(ZAttrProvisioning.A_zimbraMailHost));
        try {
            HttpResponse httpResp = HttpClientUtil.executeMethod(client, get);
            int statusCode = httpResp.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote mime part", null, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
            }

            Header ctHeader = httpResp.getFirstHeader("Content-Type");
            ContentType contentType = new ContentType(ctHeader.getValue());

            return new Doc(httpResp.getEntity().getContent(), contentType, name, ct, description);
        } catch (HttpException e) {
            throw ServiceException.PROXY_ERROR(e, url);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote mime part", e, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
        }
    }

    protected static class Doc {
        String name;
        String contentType;
        String description;
        private Upload up;
        private MimePart mp;
        private String sp;
        private InputStream in;

        Doc(MimePart mpart, String filename, String ctype) {
            mp = mpart;
            name = Mime.getFilename(mpart);
            contentType = Mime.getContentType(mpart);
            overrideProperties(filename, ctype);
        }

        Doc(Upload upload, String filename, String ctype, String d) {
            up = upload;
            name = upload.getName();
            contentType = upload.getContentType();
            description = d;
            overrideProperties(filename, ctype);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        }

        Doc(String content, String filename, String ctype, String d) {
            sp = content;
            description = d;
            overrideProperties(filename, ctype);
            if (contentType != null) {
                contentType = new ContentType(contentType).setParameter("charset", "utf-8").toString();
            }
        }

        public Doc(InputStream in, ContentType ct, String filename, String ctype, String d) {
            this.in = in;
            description = d;
            name = ct == null ? null : ct.getParameter("name");
            if (name == null) {
                name = "New Document";
            }
            contentType = ct == null ? null : ct.getContentType();
            if (contentType == null) {
                contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            }
            overrideProperties(filename, ctype);
        }

        Doc (Document d) throws ServiceException {
            in = d.getContentStream();
            contentType = d.getContentType();
            name = d.getName();
            description = d.getDescription();
        }
        Doc(AuthToken auth, Account acct, int id, int ver) throws ServiceException {
            String url = UserServlet.getRestUrl(acct) + "?fmt=native&disp=attachment&id=" + id;
            if (ver > 0) {
                url += "&ver=" + ver;
            }
            Pair<Header[], byte[]> resource = UserServlet.getRemoteResource(auth.toZAuthToken(), url);
            int status = 0;
            for (Header h : resource.getFirst()) {
                if (h.getName().equalsIgnoreCase("X-Zimbra-Http-Status")) {
                    status = Integer.parseInt(h.getValue());
                } else if (h.getName().equalsIgnoreCase("X-Zimbra-ItemName")) {
                    name = h.getValue();
                } else if (h.getName().equalsIgnoreCase("Content-Type")) {
                    contentType = h.getValue();
                }
            }
            if (status != 200) {
                throw ServiceException.RESOURCE_UNREACHABLE("http error " + status, null);
            }
            in = new ByteArrayInputStream(resource.getSecond());
        }

        private void overrideProperties(String filename, String ctype) {
            if (filename != null && !filename.trim().equals("")) {
                name = filename;
            }
            if (ctype != null && !ctype.trim().equals("")) {
                contentType = ctype;
            }
        }

        public InputStream getInputStream() throws IOException {
            try {
                if (up != null) {
                    return up.getInputStream();
                } else if (mp != null) {
                    return mp.getInputStream();
                } else if (sp != null) {
                    return new ByteArrayInputStream(sp.getBytes("utf-8"));
                } else if (in != null) {
                    return in;
                } else {
                    throw new IOException("no contents");
                }
            } catch (MessagingException e) {
                throw new IOException(e.getMessage());
            }
        }

        public void cleanup() {
            if (up != null) {
               FileUploadServlet.deleteUpload(up);
            }
            ByteUtil.closeStream(in);
        }

        public String getName() {
            return name;
        }
    }

    @Override public boolean isReadOnly() {
        return false;
    }
}
