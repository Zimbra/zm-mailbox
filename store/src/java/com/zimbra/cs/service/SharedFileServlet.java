/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;

@SuppressWarnings("serial")
public class SharedFileServlet extends UserServlet {

    private static final String SERVLET_PATH = "/shf";
    private static final String DOC_EXCHANGE_FORWARD_URL_FOR_EDIT= "/extension/doc/";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected UserServletContext createContext(HttpServletRequest req, HttpServletResponse resp, UserServlet servlet)
            throws UserServletException, ServiceException {
        return new SharedFileServletContext(req, resp, servlet);
    }

    private String getProxyPath(String itemUuid, String accountId) throws ServiceException {
        // Proxy URI removes the share uuid to prevent proxy loop.
        return getSharedFileURLPath(itemUuid, accountId, null);
    }

    public static String getSharedFileURLPath(String itemUuid, String accountId, String shareUuid) throws ServiceException {
        boolean isShare;
        String container;
        if (!StringUtil.isNullOrEmpty(shareUuid)) {
            isShare = true;
            container = shareUuid;
        } else {
            isShare = false;
            container = accountId;
        }
        String encoded = new SharedFileServletContext.EncodedId(itemUuid, container, isShare).encode();
        return String.format("%s/%s", SERVLET_PATH, encoded);
    }

    @Override
    protected boolean proxyIfRemoteTargetAccount(HttpServletRequest req, HttpServletResponse resp, UserServletContext ct)
            throws IOException, ServiceException {
        SharedFileServletContext context = (SharedFileServletContext) ct;
        if (context.targetAccount != null && !Provisioning.onLocalServer(context.targetAccount)) {
            try {
                String uri = getProxyPath(context.itemUuid, context.targetAccount.getId());
                proxyServletRequest(req, resp, Provisioning.affinityServer(context.targetAccount), uri, getProxyAuthToken(context));
            } catch (HttpException e) {
                throw new IOException("Unknown error", e);
            }
            return true;
        }

        return false;
    }

    @Override
    protected void resolveItems(UserServletContext context) throws ServiceException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected MailItem resolveItem(UserServletContext ct) throws ServiceException, IOException {
        SharedFileServletContext context = (SharedFileServletContext) ct;
        MailItem item = context.targetMailbox.getItemByUuid(context.opContext, context.itemUuid, MailItem.Type.UNKNOWN, context.fromDumpster);
        if (item instanceof Document) {
            context.target = item;
            return context.target;
        } else {
            // This servlet is for files/documents only.
            throw MailServiceException.NO_SUCH_ITEM_UUID(context.itemUuid);
        }
    }

    @Override
    protected boolean proxyIfMountpoint(HttpServletRequest req, HttpServletResponse resp, UserServletContext ct, MailItem item)
    throws IOException, ServiceException, UserServletException {
        if (isEditEnabled(item, ct)) {
            // redirect to /service/extension/doc/{briefcase-doc-id}
            // item.getDigest() is the docId
            try {
                log.debug("Redirecting to Doc Extension Server %s", getSharedDocId(ct.req.getRequestURL().toString()));
                RequestDispatcher dispatcher = getServletContext()
                        .getRequestDispatcher(DOC_EXCHANGE_FORWARD_URL_FOR_EDIT + getSharedDocId(ct.req.getRequestURL().toString()));
                dispatcher.forward(req, resp);
            } catch (ServletException e) {
                throw ServiceException.PROXY_ERROR(e, DOC_EXCHANGE_FORWARD_URL_FOR_EDIT);
            }
        }
        return false;
    }

    /**
     * In case of download of a file from Briefcase, if the following conditions are met
     * redirect to /service/extension/doc/{briefcase-doc-id}
     * checks the following conditions
     * 1. extension available
     * 2. document supported type
     * */
    private boolean isEditEnabled(MailItem item,UserServletContext context) {

        boolean zimbraFeatureDocumentEditingEnabled = false;
        Account account  = context.targetAccount;
        if (account != null) {
            zimbraFeatureDocumentEditingEnabled = account.isFeatureDocumentEditingEnabled();
            log.debug("Document editing account = %s, enabled = %s ", account.getName(), zimbraFeatureDocumentEditingEnabled);
        }
        log.debug("Document supported for editing = %s" , isAllowedDocType(item));
        return (zimbraFeatureDocumentEditingEnabled && isAllowedDocType(item));
    }

    // check if it is one of the allowed file extensions
    private boolean isAllowedDocType(MailItem item) {
        // Todo: Initialize only once. This will create lot of strings on every document edit.
        HashSet<String> allowedTypes = new HashSet<String>();
        allowedTypes.addAll(Arrays.asList(LC.doc_editing_supported_document_formats.value().toString().split(",")));
        allowedTypes.addAll(Arrays.asList(LC.doc_editing_supported_spreadsheet_formats.value().toString().split(",")));
        allowedTypes.addAll(Arrays.asList(LC.doc_editing_supported_presentation_formats.value().toString().split(",")));
        return allowedTypes.contains(Files.getFileExtension(item.getName()));
    }

    /**
     * Extract the shared doc id passed in the URL
     *
     * @param url
     * @return
     */
    private String getSharedDocId(final String url) {
        String[] urlParts = url.split("/");
        return urlParts[urlParts.length - 1];
    }

    @Override
    public void init() throws ServletException {
        ZimbraLog.doc.info("Starting up SHF");
        super.init();
    }

    @Override
    public void destroy() {
        ZimbraLog.doc.info("Shutting down SHF");
        super.destroy();
    }
}
