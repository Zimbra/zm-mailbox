/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class CheckSpelling extends MailDocumentHandler {
    
    private static Log sLog = LogFactory.getLog(CheckSpelling.class);
    
    private class ServerResponse {
        int statusCode;
        String content;
    }
    
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        Element response = zc.createElement(MailConstants.CHECK_SPELLING_RESPONSE);

        // Make sure a spell check server is specified 
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String[] urls = localServer.getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
        if (ArrayUtil.isEmpty(urls)) {
            sLog.info("Unable to check spelling.  No value specified for %s", Provisioning.A_zimbraSpellCheckURL);
            return unavailable(response);
        }

        // Handle no content
        String text = request.getTextTrim();
        if (StringUtil.isNullOrEmpty(text)) {
            sLog.debug("<CheckSpellingRequest> was empty");
            response.addAttribute(MailConstants.A_AVAILABLE, true);
            return response;
        }
        
        // Get the dictionary name from either (1) the request,
        // (2) zimbraPrefSpellDictionary or (3) the account's locale.
        String dictionary = request.getAttribute(MailConstants.A_DICTIONARY, null);
        Account account = getRequestedAccount(zc);
        if (dictionary == null) {
            dictionary = account.getPrefSpellDictionary();
            if (dictionary == null) {
                dictionary = account.getLocale().toString();
            }
        }
        
        // Assemble the list of words to ignore from the account, domain, and COS.
        List<String> ignoreWords = new ArrayList<String>();
        addToList(ignoreWords, account.getPrefSpellIgnoreWord());
        addToList(ignoreWords, prov.getDomain(account).getPrefSpellIgnoreWord());
        addToList(ignoreWords, prov.getCOS(account).getPrefSpellIgnoreWord());
    
		String ignore = request.getAttribute(MailConstants.A_IGNORE, null);
		if (ignore != null) {
			ignore = ignore.replaceAll(","," ").replaceAll("\\s{2,}"," ").trim();
			addToList(ignoreWords, ignore.split(" "));
		}
        
        // Get word list from one of the spell servers.
        ServerResponse spellResponse = null;
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            try {
                sLog.debug("Checking spelling: url=%s, dictionary=%s, text=%s, ignore=%s", url, dictionary, text, ignoreWords);
                spellResponse = checkSpelling(url, dictionary, ignoreWords, text);
                if (spellResponse.statusCode == 200) {
                    break; // Successful request.  No need to check the other servers.
                }
            } catch (IOException ex) {
                ZimbraLog.mailbox.warn("An error occurred while contacting " + url, ex);
            }
        }

        // Check for errors
        if (spellResponse == null) {
            return unavailable(response);
        }
        if (spellResponse.statusCode != 200) {
            throw ServiceException.FAILURE("Spell check failed: " + spellResponse.content, null);
        }
        
        // Parse spell server response, assemble SOAP response
        if (spellResponse.content != null) {
            BufferedReader reader =
                new BufferedReader(new StringReader(spellResponse.content));
            String line = null;
            int numLines = 0;
            int numMisspelled = 0;
            try {
                while ((line = reader.readLine()) != null) {
                    // Each line in the response has the format "werd:word,ward,weird"
                    line = line.trim();
                    numLines++;
                    int colonPos = line.indexOf(':');

                    if (colonPos > 0) {
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
            sLog.debug(
                "CheckSpelling: found %d misspelled words in %d lines", numMisspelled, numLines);
        }
        
        response.addAttribute(MailConstants.A_AVAILABLE, true);
        return response;
    }
    
    private void addToList(List<String> list, String[] elements) {
        if (elements == null) {
            return;
        }
        for (String element : elements) {
            list.add(element);
        }
    }
    
    private ServerResponse checkSpelling(String url, String dictionary, List<String> ignoreWords, String text)
    throws IOException {
        PostMethod post = new PostMethod(url);
        if (dictionary != null) {
            post.addParameter("dictionary", dictionary);
        }
        if (text != null) {
            post.addParameter("text", text);
        }
        if (!ListUtil.isEmpty(ignoreWords)) {
            post.addParameter("ignore", StringUtil.join(",", ignoreWords));
        }
        HttpClient http = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        ServerResponse response = new ServerResponse();
        try {
            response.statusCode = http.executeMethod(post);
            response.content = post.getResponseBodyAsString();
        } finally {
            post.releaseConnection();
        }
        return response;
    }
    
    private Element unavailable(Element response) {
        response.addAttribute(MailConstants.A_AVAILABLE, false);
        return response;
    }
}
