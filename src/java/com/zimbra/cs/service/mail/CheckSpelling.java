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

package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.operation.CheckSpellingOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class CheckSpelling extends DocumentHandler  {
    
    private static Log sLog = LogFactory.getLog(CheckSpelling.class);
    
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        Element response = zc.createElement(MailService.CHECK_SPELLING_RESPONSE);
        SoapSession session = (SoapSession) zc.getSession(SessionCache.SESSION_SOAP);
        
        String text = request.getTextTrim();
        if (StringUtil.isNullOrEmpty(text)) {
            sLog.debug("<CheckSpellingRequest> was empty");
            response.addAttribute(MailService.A_AVAILABLE, true);
            return response;
        }
        
        Map params = null;        
        try {
        	params = new CheckSpellingOperation(session, zc.getOperationContext(), null, Requester.SOAP, text).getResult();
        } catch (ServiceException e) {
        	if (e.getCause().equals(ServiceException.NO_SPELL_CHECK_URL)) {
        		return unavailable(response);
        	} else throw e;
        }

        if (params == null) {
            sLog.warn("Unable to check spelling.");
            return unavailable(response);
        }
        
        String misspelled = (String) params.get("misspelled");
        if (misspelled == null) {
            sLog.warn("misspelled data not found in spell server response");
            return unavailable(response);
        }
        
        // Parse spell server response, assemble SOAP response
        BufferedReader reader =
            new BufferedReader(new StringReader(misspelled));
        String line = null;
        int numLines = 0;
        int numMisspelled = 0;
        try {
            while ((line = reader.readLine()) != null) {
                // Each line in the response has the format "werd:word,ward,weird"
                line = line.trim();
                numLines++;
                int colonPos = line.indexOf(':');
                
                if (colonPos >= 0) {
                    Element wordEl = response.addElement(MailService.E_MISSPELLED);
                    String word = line.substring(0, colonPos);
                    String suggestions = line.substring(colonPos + 1, line.length());
                    wordEl.addAttribute(MailService.A_WORD, word);
                    wordEl.addAttribute(MailService.A_SUGGESTIONS, suggestions);
                    numMisspelled++;
                }
            }
        } catch (IOException e) {
            sLog.warn(e);
            return unavailable(response);
        }
        
        response.addAttribute(MailService.A_AVAILABLE, true);
        sLog.debug(
            "CheckSpelling: found " + numMisspelled + " misspelled words in " + numLines + " lines");
        
        return response;
    }
    
    private Element unavailable(Element response) {
        response.addAttribute(MailService.A_AVAILABLE, false);
        return response;
    }
}
