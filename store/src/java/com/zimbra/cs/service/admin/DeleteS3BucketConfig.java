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
import com.zimbra.common.soap.GlobalExternalStoreConfigConstants;
import com.zimbra.cs.service.util.S3BucketGlobalConfigUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigRequest;
import com.zimbra.soap.admin.message.DeleteS3BucketConfigResponse;

/**
 * @author schemers
 */
public class DeleteS3BucketConfig extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Map<String, Object> attrs = AdminService.getAttrs(request, true);
        DeleteS3BucketConfigRequest req = zsc.elementToJaxb(request);
        String s3BucketConfigDeletedJson = null;

        S3BucketGlobalConfigUtil.validateReqBeforeDelete(req);
        String s3GlobalUUID = (String) attrs.get(GlobalExternalStoreConfigConstants.A_S3_GLOBAL_BUCKET_UUID);
        s3BucketConfigDeletedJson = S3BucketGlobalConfigUtil.deleteS3BucketConfig(s3GlobalUUID);

        DeleteS3BucketConfigResponse resp = new DeleteS3BucketConfigResponse();
        resp.setAttrs(S3BucketGlobalConfigUtil.getAttrs(s3BucketConfigDeletedJson));
        return zsc.jaxbToElement(resp);

    }

}