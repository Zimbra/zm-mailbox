/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.gal;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

public class GalUtil {
    
    public static String expandFilter(String tokenize, String filterTemplate, String key, String token) 
    throws ServiceException {
        return expandFilter(tokenize, filterTemplate, key, token, null);
    }
    
    public static String expandFilter(String tokenize, String filterTemplate, String key, String token, String extraQuery) 
    throws ServiceException {
        String query;
        
        if (key != null) {
            while (key.startsWith("*")) {
                key = key.substring(1);
            }
            while (key.endsWith("*")) {
                key = key.substring(0,key.length()-1);
            }
        }
        query = expandKey(tokenize, filterTemplate, key);

        if (query.indexOf("**") > 0) {
        	query = query.replaceAll("\\*\\*", "*");
        }
        
        if (token != null && token.length() > 0) {
        	String arg = LdapUtil.escapeSearchFilterArg(token);
        	query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+"))"+query+")";
        }
        
        if (extraQuery != null) {
            query = "(&" + query + extraQuery + ")";
        }
        
        return query;
    }
    
    public static String tokenizeKey(GalParams galParams, GalOp galOp) {
    	if (galParams == null)
    		return null;
        if (galOp == GalOp.autocomplete)
            return galParams.tokenizeAutoCompleteKey();
        else if (galOp == GalOp.search)
            return galParams.tokenizeSearchKey();
        
        return null;
    }
    
    private static String expandKey(String tokenize, String filterTemplate, String key) throws ServiceException {

        if (!filterTemplate.startsWith("(")) {
            if (filterTemplate.endsWith(")"))
                throw ServiceException.INVALID_REQUEST("Unbalanced parenthesis in filter:" + filterTemplate, null);
            
            filterTemplate = "(" + filterTemplate + ")";
        }
        
        String query = null;
        Map<String, String> vars = new HashMap<String, String>();
        
        ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
        
        if (tokenize != null) {
            String tokens[] = key.split("\\s+");
            if (tokens.length > 1) {
                String q;
                if (GalConstants.TOKENIZE_KEY_AND.equals(tokenize)) {
                    q = "(&";
                } else if (GalConstants.TOKENIZE_KEY_OR.equals(tokenize)) {
                    q = "(|";
                } else {
                    throw ServiceException.FAILURE("invalid attribute value for tokenize key: " + tokenize, null);
                }
                
                for (String t : tokens) {
                    vars.clear();
                    vars.put("s", filterFactory.encodeValue(t));
                    q = q + LdapUtil.expandStr(filterTemplate, vars);
                }
                q = q + ")";
                query = q;
            }
        }
        
        if (query == null) {
            vars.put("s", filterFactory.encodeValue(key));
            query = LdapUtil.expandStr(filterTemplate, vars);
        }
        
        return query;
    }
   
}
