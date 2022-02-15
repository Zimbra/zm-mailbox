/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareLocator;

public class SharedFileServletContext extends UserServletContext {

    public String shareUuid;
    public String itemUuid;

    public SharedFileServletContext(HttpServletRequest request,
            HttpServletResponse response, UserServlet srvlt)
            throws UserServletException, ServiceException {
        super(request, response, srvlt);
        wantCustomHeaders = false;  // suppress X-Zimbra-* headers in response
    }

    @Override
    protected void parseParams(HttpServletRequest request, AuthToken authToken)
            throws UserServletException, ServiceException {

        ZimbraLog.doc.debug("Parse params");
        String pathInfo = request.getPathInfo();
        if (StringUtil.isNullOrEmpty(pathInfo) || !pathInfo.startsWith("/")) {
            throw new UserServletException(HttpServletResponse.SC_NOT_FOUND, null);
        }

        String idstr;
        int end = pathInfo.indexOf('/', 1);
        if (end != -1) {
            idstr = pathInfo.substring(1, end);
        } else {
            idstr = pathInfo.substring(1);
        }
        EncodedId epath;
        try {
            epath = EncodedId.decode(idstr);
            ZimbraLog.doc.info("SHF : %s", epath);
        } catch (ServiceException e) {
            ZimbraLog.doc.debug("invalid path: " + idstr, e);
            throw new UserServletException(HttpServletResponse.SC_NOT_FOUND,null);
        }
        itemUuid = epath.getItemUuid();
        if (StringUtil.isNullOrEmpty(itemUuid)) {
            ZimbraLog.doc.debug("missing item uuid in path: " + idstr);
            throw new UserServletException(HttpServletResponse.SC_NOT_FOUND, null);
        }

        String accountId = null;
        if (epath.isShare() && (epath.getVersion() == null || !epath.getVersion().equals(SharedFileServlet.URL_VERSION))) {
            shareUuid = epath.getContainerUuid();
            ShareLocator shloc = Provisioning.getInstance().getShareLocatorById(shareUuid);
            if (shloc != null) {
                accountId = shloc.getShareOwnerAccountId();
            }
        } else {
            accountId = epath.getContainerUuid();
        }
        if (StringUtil.isNullOrEmpty(accountId)) {
            ZimbraLog.doc.debug("missing account id in path: " + idstr);
            throw new UserServletException(HttpServletResponse.SC_NOT_FOUND, null);
        }
        targetAccount = Provisioning.getInstance().get(AccountBy.id, accountId, authToken);
        fromDumpster = false;  // fetching dumpster'd file not supported
        ZimbraLog.doc.debug("Looking for item :%s, for account:%s", itemUuid, accountId);
    }

    public static class EncodedId {
        private String itemUuid;
        private String containerUuid;
        private boolean isShare;  // true = container is a share, false = container is an account
        //In the new version v2 , the url will contain the owner accountId in all cases
        // isShare will always be false
        //version will be null for old URL
        private String version;

        public EncodedId(String itemUuid, String containerUuid, boolean isShare, String version) {
            this.itemUuid = itemUuid;
            this.containerUuid = containerUuid;
            this.isShare = isShare;
            this.version = version;
        }

        public String getItemUuid() { return itemUuid; }
        public String getContainerUuid() { return containerUuid; }
        public boolean isShare() { return isShare; }
        public String getVersion() { return version; }

        public String encode() throws ServiceException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dos = null;
            try {
                dos = new DataOutputStream(buffer);
                UUID ui = UUID.fromString(itemUuid);
                dos.writeLong(ui.getMostSignificantBits());
                dos.writeLong(ui.getLeastSignificantBits());
                UUID uc = UUID.fromString(containerUuid);
                dos.writeLong(uc.getMostSignificantBits());
                dos.writeLong(uc.getLeastSignificantBits());
                dos.writeBoolean(isShare);
                dos.writeUTF(SharedFileServlet.URL_VERSION);
            } catch (IOException e) {
                throw ServiceException.FAILURE("can't encode", e);
            } catch (IllegalArgumentException e) {
                throw ServiceException.FAILURE("can't encode", e);
            } finally {
                ByteUtil.closeStream(dos);
            }
            return ByteUtil.encodeFSSafeBase64(buffer.toByteArray());
        }

        public static EncodedId decode(String encoded) throws ServiceException {
            String item, container;
            boolean isShare;
            String version = null;
            byte[] buffer = ByteUtil.decodeFSSafeBase64(encoded);
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new ByteArrayInputStream(buffer));
                long msb, lsb;
                msb = dis.readLong();
                lsb = dis.readLong();
                item = new UUID(msb, lsb).toString();
                msb = dis.readLong();
                lsb = dis.readLong();
                container = new UUID(msb, lsb).toString();
                isShare = dis.readBoolean();
                //in the old URL version param won't be there, hence it will throw an exception
                try {
                    version = dis.readUTF();
                } catch (EOFException e) {
                    //not throwing an exception as it is a valid scenario
                    //wherein version is not present for old URLS
                    ZimbraLog.doc.warn("Version Param missing. This is an old URL. %s",encoded);
                }
                return new EncodedId(item, container, isShare, version);
            } catch (IOException e) {
                throw ServiceException.FAILURE("can't decode " + encoded, e);
            } finally {
                ByteUtil.closeStream(dis);
            }
        }
    }

}