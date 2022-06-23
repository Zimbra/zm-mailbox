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
import com.zimbra.cs.service.util.S3BucketGlobalConfigUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CreateS3BucketConfigRequest;
import com.zimbra.soap.admin.message.CreateS3BucketConfigResponse;

/**
 * @author schemers
 */
public class CreateS3BucketConfig extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        CreateS3BucketConfigRequest req = zsc.elementToJaxb(request);
        String s3BucketConfigCreatedJson = null;

        S3BucketGlobalConfigUtil.validateReqBeforeCreate(req);
        s3BucketConfigCreatedJson = S3BucketGlobalConfigUtil.createS3BucketConfig(req);

        CreateS3BucketConfigResponse resp = new CreateS3BucketConfigResponse();
        resp.setAttrs(S3BucketGlobalConfigUtil.getAttrs(s3BucketConfigCreatedJson));
        return zsc.jaxbToElement(resp);

    }

}