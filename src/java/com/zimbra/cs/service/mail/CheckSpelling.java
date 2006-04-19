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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.RemoteServerRequest;
import com.zimbra.cs.util.ArrayUtil;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class CheckSpelling extends DocumentHandler  {
    
    private static Log sLog = LogFactory.getLog(CheckSpelling.class);
    
    public Element handle(Element request, Map context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);
        Element response = lc.createElement(MailService.CHECK_SPELLING_RESPONSE);

        // Make sure that the spell server URL is specified
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String[] urls = localServer.getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
        if (ArrayUtil.isEmpty(urls)) {
            sLog.debug(Provisioning.A_zimbraSpellCheckURL + " is not specified");
            return unavailable(response);
        }
        
        String text = request.getTextTrim();
        if (StringUtil.isNullOrEmpty(text)) {
            sLog.debug("<CheckSpellingRequest> was empty");
            response.addAttribute(MailService.A_AVAILABLE, true);
            return response;
        }
        
        // Issue the request
        Map params = null;
        for (int i = 0; i < urls.length; i++) {
            RemoteServerRequest req = new RemoteServerRequest();
            req.addParameter("text", text);
            String url = urls[i];
            try {
                sLog.debug("Attempting to check spelling at " + url);
                req.invoke(url);
                params = req.getResponseParameters();
                break; // Successful request.  No need to check the other servers.
            } catch (IOException ex) {
                sLog.warn("An error occurred while contacting " + url, ex);
            }
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
