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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;

@SuppressWarnings("serial")
public class SharedFileServlet extends UserServlet {

    private static final String SERVLET_PATH = "/shf";

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

        throw MailServiceException.NO_SUCH_ITEM_UUID(context.itemUuid);
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
        return false;
    }
}
