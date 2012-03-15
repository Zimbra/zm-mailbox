/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-description Returns current import status for all data sources.  Status values for a data source
 * are reinitialized when either (a) another import process is started or (b) when the server is restarted.  If
 * import has not run yet, the success and error attributes are not specified in the response.
 */
@XmlRootElement(name=MailConstants.E_GET_IMPORT_STATUS_REQUEST)
public class GetImportStatusRequest {
}
