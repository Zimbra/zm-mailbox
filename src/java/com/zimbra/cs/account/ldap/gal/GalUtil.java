package com.zimbra.cs.account.ldap.gal;

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
    
    private static String expandKey(GalParams galParams, GalOp galOp, String filterTemplate, String key) throws ServiceException {
        String query = null;
        
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("s", key);
        
        String tokenize = null;
        if (galOp == GalOp.autocomplete)
            tokenize = galParams.tokenizeAutoCompleteKey();
        else if (galOp == GalOp.search)
            tokenize = galParams.tokenizeSearchKey();
        
        if (tokenize != null) {
            String tokens[] = key.split("\\s+");
            if (tokens.length > 1) {
                String q;
                if (Gal.TOKENIZE_KEY_AND.equals(tokenize)) {
                    q = "(&";
                } else if (Gal.TOKENIZE_KEY_OR.equals(tokenize)) {
                    q = "(|";
                } else
                    throw ServiceException.FAILURE("invalid attribute value for tokenize key: " + tokenize, null);
                    
                for (String t : tokens) {
                    q = q + LdapProvisioning.expandStr(filterTemplate, vars);
                }
                q = q + ")";
                query = q;
            }
        }
        
        if (query == null)
            query = LdapProvisioning.expandStr(filterTemplate, vars);
        
        return query;
    }
   
}
