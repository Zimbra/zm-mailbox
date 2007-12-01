/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.util.RemoteServerRequest;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class CheckSpelling extends MailDocumentHandler {
    
    private static Log sLog = LogFactory.getLog(CheckSpelling.class);
    
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        Element response = zc.createElement(MailConstants.CHECK_SPELLING_RESPONSE);

        // Make sure a spell check server is specified 
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String[] urls = localServer.getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
        if (ArrayUtil.isEmpty(urls)) {
            sLog.warn("No value specified for %s", Provisioning.A_zimbraSpellCheckURL);
            return unavailable(response);
        }

        // Handle no content
        String text = request.getTextTrim();
        if (StringUtil.isNullOrEmpty(text)) {
            sLog.debug("<CheckSpellingRequest> was empty");
            response.addAttribute(MailConstants.A_AVAILABLE, true);
            return response;
        }
        
        // Check spelling
        Map<String, String> params = null;        
        for (int i = 0; i < urls.length; i++) {
            RemoteServerRequest req = new RemoteServerRequest();
            req.addParameter("text", text);
            String url = urls[i];
            try {
                if (sLog.isDebugEnabled())
                    sLog.debug("Checking spelling: url=%s, text=%s", url, text);
                req.invoke(url);
                params = req.getResponseParameters();
                break; // Successful request.  No need to check the other servers.
            } catch (IOException ex) {
                ZimbraLog.mailbox.warn("An error occurred while contacting " + url, ex);
            }
        }

        // Check for errors
        if (params == null) {
            sLog.warn("Unable to check spelling.  No params returned.");
            return unavailable(response);
        }
        if (params.containsKey("error")) {
            throw ServiceException.FAILURE("Spell check failed: " + params.get("error"), null);
        }
        
        String misspelled = params.get("misspelled");
        if (misspelled == null) {
            sLog.warn("Misspelled data not found in spell server response.");
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
                    Element wordEl = response.addElement(MailConstants.E_MISSPELLED);
                    String word = line.substring(0, colonPos);
                    String suggestions = line.substring(colonPos + 1, line.length());
                    wordEl.addAttribute(MailConstants.A_WORD, word);
                    wordEl.addAttribute(MailConstants.A_SUGGESTIONS, suggestions);
                    numMisspelled++;
                }
            }
        } catch (IOException e) {
            sLog.warn(e);
            return unavailable(response);
        }
        
        response.addAttribute(MailConstants.A_AVAILABLE, true);
        sLog.debug(
            "CheckSpelling: found %d misspelled words in %d lines", numMisspelled, numLines);
        
        return response;
    }
    
    private Element unavailable(Element response) {
        response.addAttribute(MailConstants.A_AVAILABLE, false);
        return response;
    }
}
