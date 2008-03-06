package com.zimbra.cs.account.gal;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;

public class GalUtil {
    
    public static String expandFilter(GalParams galParams, GalOp galOp, String filterTemplate, String key, String token, boolean internal) throws ServiceException {
        String query;
        
        /*
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("s", key);
        query = LdapProvisioning.expandStr(filterTemplate, vars);
        */
        
        query = expandKey(galParams, galOp, filterTemplate, key);
        
        if (token != null) {
            if (token.equals(""))
                query = query.replaceAll("\\*\\*", "*");
            else {
                String arg = LdapUtil.escapeSearchFilterArg(token);
                if (internal)
                    query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+"))"+query.replaceAll("\\*\\*", "*")+")";
                else
                    query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+")(whenModified>="+arg+")(whenCreated>="+arg+"))"+query.replaceAll("\\*\\*", "*")+")";                
            }
        }
        
        return query;
    }
    
    public static String tokenizeKey(GalParams galParams, GalOp galOp) {
        if (galOp == GalOp.autocomplete)
            return galParams.tokenizeAutoCompleteKey();
        else if (galOp == GalOp.search)
            return galParams.tokenizeSearchKey();
        
        return null;
    }
    
    private static String expandKey(GalParams galParams, GalOp galOp, String filterTemplate, String key) throws ServiceException {

        if (!filterTemplate.startsWith("(")) {
            if (filterTemplate.endsWith(")"))
                throw ServiceException.INVALID_REQUEST("Unbalanced parenthesis in filter:" + filterTemplate, null);
            
            filterTemplate = "(" + filterTemplate + ")";
        }
        
        String query = null;
        Map<String, String> vars = new HashMap<String, String>();
        
        String tokenize = tokenizeKey(galParams, galOp);
        
        if (tokenize != null) {
            String tokens[] = key.split("\\s+");
            if (tokens.length > 1) {
                String q;
                if (GalConstants.TOKENIZE_KEY_AND.equals(tokenize)) {
                    q = "(&";
                } else if (GalConstants.TOKENIZE_KEY_OR.equals(tokenize)) {
                    q = "(|";
                } else
                    throw ServiceException.FAILURE("invalid attribute value for tokenize key: " + tokenize, null);
                    
                for (String t : tokens) {
                    vars.clear();
                    vars.put("s", t);
                    q = q + LdapProvisioning.expandStr(filterTemplate, vars);
                }
                q = q + ")";
                query = q;
            }
        }
        
        if (query == null) {
            vars.put("s", key);
            query = LdapProvisioning.expandStr(filterTemplate, vars);
        }
        
        return query;
    }
   
}
