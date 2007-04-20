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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.cs.session.WaitSet.WaitSetAccount;
import com.zimbra.cs.session.WaitSet.WaitSetError;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class CreateWaitSet extends AdminDocumentHandler {
    /*
     <!--*************************************
          CreateWaitSet: must be called once to initialize the WaitSet
          and to set its "default interest types"
         ************************************* -->
        <CreateWaitSetRequest defTypes="DEFAULT_INTEREST_TYPES" [all="1"]>
          [ <add>
            [<a id="ACCTID" [token="lastKnownSyncToken"] [types="if_not_default"]/>]+
            </add> ]
        </CreateWaitSetRequest>

        <CreateWaitSetResponse waitSet="setId" defTypes="types" seq="0">
          [ <error ...something.../>]*
        </CreateWaitSetResponse>  
     */

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        String defInterestStr = request.getAttribute(AdminConstants.A_DEFTYPES);
        int defaultInterests = WaitMultipleAccounts.parseInterestStr(defInterestStr, 0);
        
        boolean allAccts = request.getAttributeBool("allAccounts", false);
        
        List<WaitSetAccount> add = WaitMultipleAccounts.parseAddUpdateAccounts(
            request.getOptionalElement(AdminConstants.E_WAITSET_ADD), defaultInterests);
        
        Pair<String, List<WaitSetError>> result = WaitSetMgr.create(defaultInterests, allAccts, add);
        String wsId = result.getFirst();
        List<WaitSetError> errors = result.getSecond();
        
        Element response = zsc.createElement(AdminConstants.CREATE_WAIT_SET_RESPONSE);
        response.addAttribute(AdminConstants.A_WAITSET_ID, wsId);
        response.addAttribute(AdminConstants.A_DEFTYPES, WaitMultipleAccounts.interestToStr(defaultInterests));
        response.addAttribute(AdminConstants.A_SEQ, 0);
        
        WaitMultipleAccounts.encodeErrors(response, errors);
        
        return response;
    }

}
