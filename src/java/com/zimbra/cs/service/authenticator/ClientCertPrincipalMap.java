package com.zimbra.cs.service.authenticator;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class ClientCertPrincipalMap {
    
    static enum CertField {
        SUBJECT_DN,
        SUBJECT_CN,
        SUBJECT_EMAILADDRESS,
        SUBJECTALTNAME_OTHERNAME_UPN,
        SUBJECTALTNAME_RFC822NAME;
        
        static String names() {
            StringBuilder str = new StringBuilder();
            int i = 0;
            for (CertField type : CertField.values()) {
                if (i++ > 0) str.append('|');
                str.append(type.name());
            }
            return str.toString();
        }
    };
    
    static enum ZimbraKey {
        // Note: do NOT support search Zimbra account by DN because:
        // (1) DOS attack (non-existing DN will cause repeated LDAP search)
        // and
        // (2) Subject DN in the certificate mostly likely will not be an 
        //     exact match of a Zimbra account DN.
        // dn, 
        name,
        zimbraId,
        zimbraForeignPrincipal;
    }
    
    static class Rule {
        private CertField certField;
        private ZimbraKey zimbraKey;
        
        private Rule(CertField certField, ZimbraKey zimbraKey) {
            this.certField = certField;
            this.zimbraKey = zimbraKey;
        }
        
        CertField getCertFiled() {
            return certField;
        }
        
        ZimbraKey getZimbraKey() {
            return zimbraKey;
        }
        
        public String toString() {
            return certField.name() + MAP_DELIMITER + zimbraKey.name();
        }
    }

    private static final String RULE_DELIMITER = ",";  // seperate each rule
    private static final String MAP_DELIMITER = "=";   // seperate cert filed and zimbra key
    private List<Rule> rules;
    
    ClientCertPrincipalMap(HttpServletRequest req) throws ServiceException {
        String rawRules = getMappingConfig(req);
        rules = parse(rawRules);
    }
    
    List<Rule> getRules() {
        return rules;
    }
    
    private String getMappingConfig(HttpServletRequest req) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        String virtualHostName = HttpUtil.getVirtualHost(req);
        Entry entry = prov.get(DomainBy.virtualHostname, virtualHostName);
        if (entry == null) {
            entry = prov.getConfig();
        }
        
        return entry.getAttr(Provisioning.A_zimbraMailSSLClientCertPrincipalMap);
    }
    
    private List<Rule> parse(String rawRules) throws ServiceException {
        List<Rule> parsedRules = new ArrayList<Rule>();
        
        if (rawRules == null) {
            // default to Subject_emailAddress=name
            ZimbraLog.account.warn("No " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap +
                    " configured, default to " + 
                    CertField.SUBJECT_EMAILADDRESS.name() + MAP_DELIMITER + ZimbraKey.name.name());
            Rule rule = new Rule(CertField.SUBJECT_EMAILADDRESS, ZimbraKey.name);
            parsedRules.add(rule);
        } else {
            String[] rules = rawRules.split(RULE_DELIMITER);
            
            for (String rawRule : rules) {
                String[] parts = rawRule.split(MAP_DELIMITER);
                if (parts.length != 2) {
                    throw ServiceException.FAILURE("Invalid config:" + rawRule + 
                            " in " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap, null);
                }
                
                try {
                    CertField certField = CertField.valueOf(parts[0].trim());
                    ZimbraKey zimbraKey = ZimbraKey.valueOf(parts[1].trim());
                    
                    Rule rule = new Rule(certField, zimbraKey);
                    parsedRules.add(rule);
                } catch (IllegalArgumentException e) {
                    throw ServiceException.FAILURE("Invalid config:" + rawRule + 
                            " in " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap, e);
                }
            }
        }
        
        return parsedRules;
    }
     
}
