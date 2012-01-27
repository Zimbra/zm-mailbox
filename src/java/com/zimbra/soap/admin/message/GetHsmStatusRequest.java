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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.HsmConstants;

/**
 * @zm-api-command-description Queries the status of the most recent HSM session.  Status information for a given HSM
 * session is available until the next time HSM runs or until the server is restarted.
 * <br />
 * Notes:
 * <ul>
 * <li> If an HSM session is running, "endDate" is not specified in the response.
 * <li> As an HSM session runs, numMoved and numMailboxes increase with subsequent requests.
 * <li> A response sent while HSM is aborting returns aborted="0" and aborting="1".
 * <li> If HSM completed successfully, numMailboxes == totalMailboxes.
 * <li> If &lt;GetHsmStatusRequest> is sent after a server restart but before an HSM session, the response will contain
 *      running="0" and no additional information.
 * <li> Once HSM completes, the same &lt;GetHsmStatusResponse> will be returned until another HSM session or a server
 *      restart.
 * </ul>
 */
@XmlRootElement(name=HsmConstants.E_GET_HSM_STATUS_REQUEST)
public class GetHsmStatusRequest {
}
