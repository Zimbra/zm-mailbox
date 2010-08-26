/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.service.ServiceException.InternalArgument;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.soap.ZimbraSoapContext;

public class SaveDocument extends WikiDocumentHandler {

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
        try {
            String explicitName = docElem.getAttribute(MailConstants.A_NAME, null);
            String explicitCtype = docElem.getAttribute(MailConstants.A_CONTENT_TYPE, null);

            //bug 37180, extract the filename from the path (for IE). IE sends the full path.
            if (explicitName != null) {
                try {
                    explicitName = explicitName.replaceAll("\\\\","/");
                    explicitName = explicitName.substring(explicitName.lastIndexOf("/")+1);
                } catch (Exception e) {
                    //Do nothing
                }
            }

            Element attElem = docElem.getOptionalElement(MailConstants.E_UPLOAD);
            Element msgElem = docElem.getOptionalElement(MailConstants.E_MSG);
            if (attElem != null) {
                String aid = attElem.getAttribute(MailConstants.A_ID, null);
                Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), aid, zsc.getAuthToken());
                doc = new Doc(up, explicitName, explicitCtype);
            } else if (msgElem != null) {
                String part = msgElem.getAttribute(MailConstants.A_PART);
                ItemId iid = new ItemId(msgElem.getAttribute(MailConstants.A_ID), zsc);
                doc = fetchMimePart(octxt, iid, part, explicitName, explicitCtype, zsc.getAuthToken());
            } else {
                String inlineContent = docElem.getAttribute(MailConstants.E_CONTENT);
                doc = new Doc(inlineContent, explicitName, explicitCtype);
            }
            if (doc.name == null || doc.name.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_NAME, null);
            if (doc.contentType == null || doc.contentType.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_CONTENT_TYPE, null);

            String description = docElem.getAttribute(MailConstants.A_DESC, null);
            ItemId fid = new ItemId(docElem.getAttribute(MailConstants.A_FOLDER, DEFAULT_DOCUMENT_FOLDER), zsc);

            String id = docElem.getAttribute(MailConstants.A_ID, null);
            int itemId = (id == null ? 0 : new ItemId(id, zsc).getId());
            int ver = (int) docElem.getAttributeLong(MailConstants.A_VERSION, 0);

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(zsc.getRequestedAccountId());
            Document docItem = null;
            WikiPage.WikiContext ctxt = new WikiPage.WikiContext(octxt, zsc.getAuthToken());
            InputStream is = null;
            try {
                is = doc.getInputStream();
            } catch (IOException e) {
                throw ServiceException.FAILURE("can't save document", e);
            }
            if (itemId == 0) {
                // create a new page
                try {
                    docItem = mbox.createDocument(octxt, fid.getId(), doc.name, doc.contentType, getAuthor(zsc), description, is, MailItem.TYPE_DOCUMENT);
                } catch (ServiceException e) {
                    if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                        MailItem item = mbox.getItemByPath(octxt, doc.name, fid.getId());
                        if (item != null && item instanceof Document)
                            throw MailServiceException.ALREADY_EXISTS("name "+doc.name+" in folder "+fid.getId(),
                                    new Argument(MailConstants.A_NAME, doc.name, Argument.Type.STR),
                                    new Argument(MailConstants.A_ID, item.getId(), Argument.Type.IID),
                                    new Argument(MailConstants.A_VERSION, ((Document)item).getVersion(), Argument.Type.NUM));
                    }
                    throw e;
                }
            } else {
                // add a new revision
                WikiPage oldPage = WikiPage.findPage(ctxt, zsc.getRequestedAccountId(), itemId);
                if (oldPage == null)
                    throw new WikiServiceException.NoSuchWikiException("page id="+id+" not found");
                if (oldPage.getLastVersion() != ver) {
                    throw MailServiceException.MODIFY_CONFLICT(
                            new Argument(MailConstants.A_NAME, doc.name, Argument.Type.STR),
                            new Argument(MailConstants.A_ID, oldPage.getId(), Argument.Type.IID),
                            new Argument(MailConstants.A_VERSION, oldPage.getLastVersion(), Argument.Type.NUM));
                }
                docItem = mbox.addDocumentRevision(octxt, itemId, getAuthor(zsc), doc.name, description, is);
            }

            response = zsc.createElement(MailConstants.SAVE_DOCUMENT_RESPONSE);
            Element m = response.addElement(MailConstants.E_DOC);
            m.addAttribute(MailConstants.A_ID, new ItemIdFormatter(zsc).formatItemId(docItem));
            m.addAttribute(MailConstants.A_VERSION, docItem.getVersion());
            m.addAttribute(MailConstants.A_NAME, docItem.getName());
            success = true;
        } finally {
            if (success && doc != null)
                doc.cleanup();
        }
        return response;
    }

    private Doc fetchMimePart(OperationContext octxt, ItemId itemId, String partId, String name, String ct, AuthToken authtoken) throws ServiceException {
        String accountId = itemId.getAccountId();
        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (Provisioning.onLocalServer(acct)) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            Message msg = mbox.getMessageById(octxt, itemId.getId());
            try {
                return new Doc(Mime.getMimePart(msg.getMimeMessage(), partId), name, ct);
            } catch (MessagingException e) {
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch mime part msgId="+itemId+", partId="+partId, e);
            } catch (IOException e) {
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch mime part msgId="+itemId+", partId="+partId, e);
            }
        }

        String url = UserServlet.getRestUrl(acct) + "?auth=co&id=" + itemId + "&part=" + partId;
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(url);
        authtoken.encode(client, get, false, acct.getAttr(Provisioning.A_zimbraMailHost));
        try {
            int statusCode = HttpClientUtil.executeMethod(client, get);
            if (statusCode != HttpStatus.SC_OK)
                throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote mime part", null, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));

            Header ctHeader = get.getResponseHeader("Content-Type");
            ContentType contentType = new ContentType(ctHeader.getValue());

            return new Doc(get.getResponseBodyAsStream(), contentType, name, ct);
        } catch (HttpException e) {
            throw ServiceException.PROXY_ERROR(e, url);
        } catch (IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("can't fetch remote mime part", e, new InternalArgument(ServiceException.URL, url, Argument.Type.STR));
        }
    }

    private static class Doc {
        String name;
        String contentType;
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

        Doc(Upload upload, String filename, String ctype) {
            up = upload;
            name = upload.getName();
            contentType = upload.getContentType();
            overrideProperties(filename, ctype);
            if (contentType == null)
                contentType = "application/octet-stream";
        }

        Doc(String content, String filename, String ctype) {
            sp = content;
            overrideProperties(filename, ctype);
            if (contentType != null)
                contentType = new ContentType(contentType).setParameter("charset", "utf-8").toString();
        }

        Doc(InputStream in, ContentType ct, String filename, String ctype) {
            this.in = in;
            name = ct.getParameter("name");
            if (name == null)
                name = "New Document";
            contentType = ct.getValue();
            if (contentType == null)
                contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            overrideProperties(filename, ctype);
        }

        private void overrideProperties(String filename, String ctype) {
            if (filename != null && !filename.trim().equals(""))
                name = filename;
            if (ctype != null && !ctype.trim().equals(""))
                contentType = ctype;
        }

        public InputStream getInputStream() throws IOException {
            try {
                if (up != null)
                    return up.getInputStream();
                else if (mp != null)
                    return mp.getInputStream();
                else if (sp != null)
                    return new ByteArrayInputStream(sp.getBytes("utf-8"));
                else if (in != null)
                    return in;
                else
                    throw new IOException("no contents");
            } catch (MessagingException e) {
                throw new IOException(e.getMessage());
            }
        }
        public void cleanup() {
            if (up != null)
                FileUploadServlet.deleteUpload(up);
            if (in != null)
                try { in.close(); } catch (IOException e) {}
        }
    }

    @Override public boolean isReadOnly() {
        return false;
    }
}
