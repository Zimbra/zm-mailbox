/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.wiki;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;
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

            Element attElem = docElem.getOptionalElement(MailConstants.E_UPLOAD);
            Element msgElem = docElem.getOptionalElement(MailConstants.E_MSG);
            if (attElem != null) {
                String aid = attElem.getAttribute(MailConstants.A_ID, null);
                Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), aid, zsc.getAuthToken());
                doc = new Doc(up, explicitName, explicitCtype);
            } else if (msgElem != null) {
                String msgid = msgElem.getAttribute(MailConstants.A_ID);
                String part = msgElem.getAttribute(MailConstants.A_PART);
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(zsc.getRequestedAccountId());
                Message msg = mbox.getMessageById(octxt, Integer.parseInt(msgid));
                try {
                    doc = new Doc(Mime.getMimePart(msg.getMimeMessage(), part), explicitName, explicitCtype);
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("cannot get part " + part + " from message " + msgid, e);
                }
            } else {
                String inlineContent = docElem.getAttribute(MailConstants.E_CONTENT);
                doc = new Doc(inlineContent, explicitName, explicitCtype);
            }
            if (doc.name == null || doc.name.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_NAME, null);
            if (doc.contentType == null || doc.contentType.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("missing required attribute: " + MailConstants.A_CONTENT_TYPE, null);

            ItemId fid = new ItemId(docElem.getAttribute(MailConstants.A_ID, DEFAULT_DOCUMENT_FOLDER), zsc);

            String id = docElem.getAttribute(MailConstants.A_ID, null);
            int itemId = (id == null ? 0 : new ItemId(id, zsc).getId());
            int ver = (int) docElem.getAttributeLong(MailConstants.A_VERSION, 0);

            WikiContext ctxt = new WikiContext(octxt, zsc.getAuthToken());
            WikiPage page = WikiPage.create(doc.name, getAuthor(zsc), doc.contentType, doc.getInputStream());
            Wiki.addPage(ctxt, page, itemId, ver, fid);
            Document docItem = page.getWikiItem(ctxt);

            response = zsc.createElement(MailConstants.SAVE_DOCUMENT_RESPONSE);
            Element m = response.addElement(MailConstants.E_DOC);
            m.addAttribute(MailConstants.A_ID, new ItemIdFormatter(zsc).formatItemId(docItem));
            m.addAttribute(MailConstants.A_VERSION, docItem.getVersion());
            success = true;
        } catch (IOException e) {
            throw ServiceException.FAILURE("can't save document", e);
        } finally {
            if (success && doc != null)
                doc.cleanup();
        }
        return response;
    }

	private static class Doc {
		String name;
		String contentType;
		private Upload up;
		private MimePart mp;
		private String sp;

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
    			else
    				throw new IOException("no contents");
			} catch (MessagingException e) {
				throw new IOException(e.getMessage());
			}
		}
		public void cleanup() {
			if (up != null)
                FileUploadServlet.deleteUpload(up);
		}
	}

	@Override public boolean isReadOnly() {
        return false;
    }
}
