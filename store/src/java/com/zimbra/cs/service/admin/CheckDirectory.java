/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckDirectory extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(lc, context, localServer, Admin.R_checkDirectoryOnFileSystem);

        Element response = lc.createElement(AdminConstants.CHECK_DIRECTORY_RESPONSE);

        for (Iterator<Element> iter = request.elementIterator(AdminConstants.E_DIRECTORY);
             iter.hasNext(); ) {
            Element dirReq = iter.next();
            String path = dirReq.getAttribute(AdminConstants.A_PATH);
            boolean create = dirReq.getAttributeBool(AdminConstants.A_CREATE, false);
            File dir = new File(path);
            boolean exists = dir.exists();
            if (!exists && create) {
                dir.mkdirs();
                exists = dir.exists();
            }
            boolean isDirectory = false;
            boolean readable = false;
            boolean writable = false;
            if (exists) {
                isDirectory = dir.isDirectory();
                readable = dir.canRead();
                writable = dir.canWrite();
            }

            Element dirResp = response.addElement(AdminConstants.E_DIRECTORY);
            dirResp.addAttribute(AdminConstants.A_PATH, path);
            dirResp.addAttribute(AdminConstants.A_EXISTS, exists);
            dirResp.addAttribute(AdminConstants.A_IS_DIRECTORY, isDirectory);
            dirResp.addAttribute(AdminConstants.A_READABLE, readable);
            dirResp.addAttribute(AdminConstants.A_WRITABLE, writable);
        }

        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkDirectoryOnFileSystem);
    }
}
