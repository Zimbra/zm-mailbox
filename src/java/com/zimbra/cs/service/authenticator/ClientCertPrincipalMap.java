package com.zimbra.cs.service.authenticator;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;

public class ClientCertPrincipalMap {
    static final String LOG_PREFIX = ClientCertAuthenticator.LOG_PREFIX;
    
    private static final String RULE_DELIMITER = ",";  // seperate each rule
    private static final char LDAP_FILTER_LEADING_CHAR = '('; 
    private static final String MAP_DELIMITER = "=";   // seperate cert filed and zimbra key
        
    static abstract class CertField {
        abstract String getName();
    }
    
    // a fixed, known certificate field 
    static class KnownCertField extends CertField {
        static enum Field {
            SUBJECT_DN,
            SUBJECTALTNAME_OTHERNAME_UPN,
            SUBJECTALTNAME_RFC822NAME;
            
            private KnownCertField knownCertField;
            private Field() {
                knownCertField = new KnownCertField(this);
            }
            
            private KnownCertField getKnownCertField() {
                return knownCertField;
            }
            
            private static String names() {
                StringBuilder str = new StringBuilder();
                int i = 0;
                for (Field type : Field.values()) {
                    if (i++ > 0) str.append('|');
                    str.append(type.name());
                }
                return str.toString();
            }
        };
        
        private Field field;
                
        private KnownCertField(Field field) {
            this.field = field;
        }
        
        private static KnownCertField parse(String fieldStr) {
            try {
                Field parsedField = Field.valueOf(fieldStr);
                return parsedField.getKnownCertField();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        static String names() {
            return Field.names();
        }
        
        Field getField() {
            return field;
        }
        
        @Override
        String getName() {
            return field.name();
        }
            
    }
        
    // a RDN in Subject
    static class SubjectCertField extends CertField {
        private static final String PREFIX = "SUBJECT_";
        private static final int PREFIX_LEN = PREFIX.length();
        
        // default if no mapping configured
        private static final SubjectCertField EMAILADDRESS = new SubjectCertField(CertUtil.ATTR_EMAILADDRESS);
        
        String rdnAttrType;
        
        private SubjectCertField(String rdnType) {
            this.rdnAttrType = rdnType;
        }
        
        static SubjectCertField parse(String fieldStr) {
            if (fieldStr.startsWith(PREFIX) && fieldStr.length() > PREFIX_LEN) {
                String rdnType = fieldStr.substring(PREFIX_LEN);
                return new SubjectCertField(rdnType);
            }
            return null;
        }
        
        static String names() {
            return PREFIX + "{an RDN attr, e.g. CN}";
        }
        
        String getRDNAttrType() {
            return rdnAttrType;
        }
        
        @Override
        String getName() {
            return PREFIX + rdnAttrType;
        }    
        
    }
    
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
    
    static abstract class Rule {
        abstract String getName();
        abstract ZimbraPrincipal apply(X509Certificate cert) throws ServiceException;
    }
    
    static class LdapFilterRule extends Rule {
        private static Pattern pattern = Pattern.compile("\\%\\{([^\\}]*)\\}");
        
        private String filter;
        
        private LdapFilterRule(String filter) {
            this.filter = filter;
        }
        
        String getFilter() {
            return filter;
        }

        @Override
        String getName() {
            return filter;
        }

        @Override
        ZimbraPrincipal apply(X509Certificate cert) throws ServiceException {
            String filter = expandFilter(cert);
            ZimbraLog.account.debug(LOG_PREFIX + 
                    "search account by expanded filter(prepended with account objectClass filter): " + filter);
            
            SearchAccountsOptions searchOpts = new SearchAccountsOptions();
            searchOpts.setMaxResults(1);
            searchOpts.setFilterString(FilterId.ACCOUNT_BY_SSL_CLENT_CERT_PRINCIPAL_MAP, filter);
            
            // should return at most one entry.  If more than one entries were matched,
            // TOO_MANY_SEARCH_RESULTS will be thrown
            List<NamedEntry> entries = Provisioning.getInstance().searchDirectory(searchOpts);
            
            if (entries.size() == 1) {
                Account acct = (Account) entries.get(0);
                return new ZimbraPrincipal(filter, acct);
            } else {
                return null;
            }
        }
        
        private String expandFilter(X509Certificate cert) throws ServiceException {
            CertUtil certUtil = new CertUtil(cert);
            
            Matcher matcher = pattern.matcher(getFilter());
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String rawCertField = matcher.group(1);
                CertField certField = parseCertField(rawCertField);
                String certFieldValue = certUtil.getCertField(certField);
                
                matcher.appendReplacement(sb, certFieldValue);
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }
    
    static class FieldMapRule extends Rule {
        private CertField certField;
        private ZimbraKey zimbraKey;
        
        private FieldMapRule(CertField certField, ZimbraKey zimbraKey) {
            this.certField = certField;
            this.zimbraKey = zimbraKey;
        }
        
        CertField getCertField() {
            return certField;
        }
        
        ZimbraKey getZimbraKey() {
            return zimbraKey;
        }

        @Override
        String getName() {
            return certField.getName() + MAP_DELIMITER + zimbraKey.name();
        }

        @Override
        ZimbraPrincipal apply(X509Certificate cert) throws ServiceException {
            CertUtil certUtil = new CertUtil(cert);
                        
            String certFieldValue = certUtil.getCertField(getCertField());
            if (certFieldValue != null) {
                Account acct = getZimbraAccount(getZimbraKey(), getCertField(), certFieldValue);
                if (acct != null) {
                    return new ZimbraPrincipal(certFieldValue, acct);
                }
            }
            
            return null;
        }
        
        private Account getZimbraAccount(ZimbraKey zimbraKey, CertField certField, String certFieldValue) {
            ZimbraLog.account.debug(LOG_PREFIX + "get account by " +
                    zimbraKey.name() + ", " + certField.getName() + "=" + certFieldValue);
            
            Provisioning prov = Provisioning.getInstance();
            Account acct = null;
            
            try {
                switch (zimbraKey) {
                    case name:
                        acct = prov.get(AccountBy.name, certFieldValue);
                        break;
                    case zimbraId:
                        acct = prov.get(AccountBy.id, certFieldValue);
                        break;
                    case zimbraForeignPrincipal:
                        String foreignPrincipal = 
                            String.format(Provisioning.FP_PREFIX_CERT, certField.getName(),certFieldValue);
                        acct = prov.get(AccountBy.foreignPrincipal, foreignPrincipal);
                        break;
                }
            } catch (ServiceException e) {
                ZimbraLog.account.debug(LOG_PREFIX + "no matching account by " +
                        zimbraKey.name() + ", " + certField.getName() + "=" + certFieldValue, e);
            }
            return acct;
        }
    }

    
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
            // default to SUBJECT_EMAILADDRESS=name
            Rule rule = new FieldMapRule(SubjectCertField.EMAILADDRESS, ZimbraKey.name);
            
            ZimbraLog.account.warn(LOG_PREFIX + "No " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap +
                    " configured, default to " + rule.getName());
            
            parsedRules.add(rule);
        } else {
            boolean ldapFilterRuleEnabled = 
                Provisioning.getInstance().getConfig().isMailSSLClientCertPrincipalMapLdapFilterEnabled();
            
            String[] rules = rawRules.split(RULE_DELIMITER);
            
            for (String rawRule : rules) {
                Rule rule = null;
                if (LDAP_FILTER_LEADING_CHAR == rawRule.charAt(0)) {
                    if (!ldapFilterRuleEnabled) {
                        throw ServiceException.FAILURE("LDAP filter is not allowed: " + rawRule, null);
                    }
                    rule = new LdapFilterRule(rawRule);
                } else {
                    rule = parseFieldMapRule(rawRule);
                }
                parsedRules.add(rule);
            }
        }
        
        return parsedRules;
    }
     
    private Rule parseFieldMapRule(String rawRule) throws ServiceException {
        
        String[] parts = rawRule.split(MAP_DELIMITER);
        if (parts.length != 2) {
            throw ServiceException.FAILURE("Invalid config:" + rawRule + 
                    " in " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap, null);
        }
        
        try {
            String certPart = parts[0].trim();
            String zimbraPart = parts[1].trim();
            
            CertField certField = parseCertField(certPart);
            ZimbraKey zimbraKey = ZimbraKey.valueOf(zimbraPart);
            
            Rule rule = new FieldMapRule(certField, zimbraKey);
            return rule;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Invalid config:" + rawRule + 
                    " in " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap, e);
        }
    }
    
    static CertField parseCertField(String rawCertField) throws ServiceException {
        // see if it is a KnownCertField
        CertField certField = KnownCertField.parse(rawCertField);
        if (certField == null) {
            // see if it is a SubjectCertField
            certField = SubjectCertField.parse(rawCertField);
        }
        
        if (certField == null) {
            throw ServiceException.FAILURE("Invalid cert field:" + rawCertField, null);
        }
        return certField;
    }
}
