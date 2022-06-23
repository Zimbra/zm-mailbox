/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

/*
 * Created on Jun, 2022
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.util.S3BucketGlobalConfigUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetS3BucketConfigResponse;
import com.zimbra.soap.type.GlobalExternalStoreConfig;

/**
 * @author schemers
 */
public class GetS3BucketConfig extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String gescLdapJson = prov.getConfig().getGlobalExternalStoreConfig();
        GlobalExternalStoreConfig gesc = S3BucketGlobalConfigUtil.parseJsonToGesc(gescLdapJson);

        GetS3BucketConfigResponse resp = new GetS3BucketConfigResponse();
        resp.setGlobalExternalStoreConfig(gesc);

        return zsc.jaxbToElement(resp);

    }

}