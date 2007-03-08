/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.session.WaitSet;
import com.zimbra.soap.ZimbraSoapContext;

/*
  *************************************
  DestroyWaitSet: Use this to close out the wait set.  Note that the
  server will automatically time out a wait set if there is no reference
  to it for (default of) 10 minutes.
  *************************************
  <DestroyWaitSetRequest waitSet="setId"/>

  <DestroyWaitSetResponse waitSet="setId"/>
*/

/**
 * 
 */
public class DestroyWaitSet extends AdminDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String waitSetId = request.getAttribute(AdminConstants.A_WAITSET_ID);
        WaitSet.destroy(waitSetId);
        
        Element response = zsc.createElement(AdminConstants.DESTROY_WAIT_SET_RESPONSE);
        response.addAttribute(AdminConstants.A_WAITSET_ID, waitSetId);
        return response;
    }
}
