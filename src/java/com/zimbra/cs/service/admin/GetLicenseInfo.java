/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

import com.zimbra.cs.localconfig.LC;

public class GetLicenseInfo extends AdminDocumentHandler {

    static DateFormat _dateFormat = new SimpleDateFormat("yyyyMMdd");
    static final String TRIAL_EXPIRATION_DATE_KEY = "trial_expiration_date";
    
    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        String expirationDate = LC.get(TRIAL_EXPIRATION_DATE_KEY);
        Element response = lc.createElement(AdminService.GET_LICENSE_INFO_RESPONSE);
        Element el = response.addElement(AdminService.E_LICENSE_EXPIRATION);
        el.addAttribute(AdminService.A_LICENSE_EXPIRATION_DATE, expirationDate);
        //if -- date object ( transformed from string ) is before the
        //current time -- then set isExpired to true) - otherwise set
        //it to false
        Date now = new Date();
        boolean isExpired = false;
        try {
            Date exp = parseDateString(expirationDate);
            isExpired = (now.getTime() > exp.getTime());
        } catch (ParseException pe){
            // do nothing, just say it's not expired
            isExpired = false;
        }        
        el.addAttribute(AdminService.A_LICENSE_EXPIRATION_IS_EXPIRED, isExpired);

        return response;
    }

    public boolean needsAdminAuth(Map context) {
        return false;
    }

    public boolean needsAuth(Map context) {
        return false;
    }


    private Date parseDateString (String dateTimeStr) throws ParseException{
        return _dateFormat.parse(dateTimeStr);
    }    
}
