package com.zimbra.cs.account.ldap;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil.SearchLdapVisitor;


public class LdapSMIMEConfig {
    private static final char NAME_VALUE_DELIMITER = ':';
    
    Entry entry;
    private Map<String, SMIMEConfig> configs = new HashMap<String, SMIMEConfig>(); // config name is case-sensitive
    
    public static Set<String> getAllSMIMEAttributes() {
        return Field.getAllAttrNames();
    }
    
    // ctor for admin access (get/modify/remove SMIME configs).  Ignore any parsing error to let the admin command complete.
    LdapSMIMEConfig(Entry entry) throws ServiceException {
        this(entry, false);
    }
    
    // ctor for SMIME public key lookup.  Throws ServiceException if parseStrict is true
    // and any parsing error is detected.
    private LdapSMIMEConfig(Entry entry, boolean parseStrict) throws ServiceException {
        this.entry = entry;
        parse(parseStrict);
    }
    
    void remove(String configName) throws ServiceException {
        Map<String, Object> toModify = new HashMap<String, Object>();
        
        for (Field field : Field.values()) {
            String attrName = field.getAttrName();
            String[] attrValues = entry.getMultiAttr(attrName, false);
            
            for (String attrValue : attrValues) {
                if (attrValue.startsWith(configName)) {
                    StringUtil.addToMultiMap(toModify, "-" + attrName, attrValue);
                }
            }
        }
        
        Provisioning.getInstance().modifyAttrs(entry, toModify, false);
    }
    
    void modify(String configName, Map<String, Object> attrs) throws ServiceException {
        Map<String, Object> toModify = new HashMap<String, Object>();
        
        SMIMEConfig config = configs.get(configName);
        
        for (Map.Entry<String, Object> attr : attrs.entrySet()) {
            String attrName = attr.getKey();
            Object attrObject = attr.getValue();
            
            if (!(attrObject instanceof String)) {
                throw ServiceException.INVALID_REQUEST(attrName + " is not a String", null);
            }
            String value = (String)attrObject;
            
            Field field = Field.fromAttrName(attrName);
            if (field == null) {
                throw ServiceException.INVALID_REQUEST(attrName + " is not a SMIME attribute", null);
            }
            
            String curValue = null;
            if (config != null) {
                curValue = config.getConfigured(Field.fromAttrName(attrName));
            }
            
            if (value.isEmpty()) {
                // new value is empty
                // remove the cur value if it is present
                if (curValue != null) {
                    String curAttrValue = encodeAttrValue(configName, curValue);
                    StringUtil.addToMultiMap(toModify, "-" + attrName, curAttrValue);
                }
            } else {
                // new value is not empty
                // remove the cur value if it is present and is not the same as the new value
                if (curValue != null && !value.equals(curValue)) {
                    String curAttrValue = encodeAttrValue(configName, curValue);
                    StringUtil.addToMultiMap(toModify, "-" + attrName, curAttrValue);   
                }
                
                // add the new value if there is no cur value or if the cur value is not the same as the new value
                if (curValue == null || !value.equals(curValue)) {
                    String newAttrValue = encodeAttrValue(configName, value);
                    StringUtil.addToMultiMap(toModify, "+" + attrName, newAttrValue);
                }
            }
        }
        
        Provisioning.getInstance().modifyAttrs(entry, toModify, false);
    }
    
    Map<String, Map<String, Object>> get(String configName) throws ServiceException {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
        if (configName == null) {
            for (SMIMEConfig config : configs.values()) {
                result.put(config.getName(), get(config));
            }
        } else {
            SMIMEConfig config = configs.get(configName);
            if (config == null) {
                throw ServiceException.INVALID_REQUEST("No such SMIME config " + configName + " on " + entry.getLabel(), null);
            } else {
                result.put(config.getName(), get(config));
            }
        }
        return result;
    }

    Map<String, Object> get(SMIMEConfig config) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Field field : Field.values()) {
            String value = config.getConfigured(field);
            if (value != null) {
                result.put(field.getAttrName(), value);
            }
        }
        return result;
    }
    
    private static enum Field {
        LDAP_URL(Provisioning.A_zimbraSMIMELdapURL),
        STARTTLS_ENABLED(Provisioning.A_zimbraSMIMELdapStartTlsEnabled, LdapUtil.LDAP_FALSE),
        BIND_DN(Provisioning.A_zimbraSMIMELdapBindDn, null),             // allow null for anonymous bind
        BIND_PASSWORD(Provisioning.A_zimbraSMIMELdapBindPassword, null), // allow null for anonymous bind
        SEARCH_BASE(Provisioning.A_zimbraSMIMELdapSearchBase, ""),
        FILTER_TEMPLATE(Provisioning.A_zimbraSMIMELdapFilter),
        ATTRIBUTE(Provisioning.A_zimbraSMIMELdapAttribute);
        
        private String attrName;
        private boolean required;
        private String defaultValue;
        
        private static class AttrNameToFieldMap {
            private static Map<String, Field> attrNameToFieldMap = new HashMap<String, Field>();
        }
        
        private Field(String attrName) {
            this.attrName = attrName;
            this.required = true;
            
            AttrNameToFieldMap.attrNameToFieldMap.put(attrName, this);
        }
        
        private Field(String attrName, String defaultValue) {
            this.attrName = attrName;
            this.required = false;
            this.defaultValue = defaultValue;
            
            AttrNameToFieldMap.attrNameToFieldMap.put(attrName, this);
        }
        
        private static Set<String> getAllAttrNames() {
            return AttrNameToFieldMap.attrNameToFieldMap.keySet();
        }
        
        private static Field fromAttrName(String attrName) {
            return AttrNameToFieldMap.attrNameToFieldMap.get(attrName);
        }
        
        private String getAttrName() {
            return attrName;
        }
        
        private boolean isRequired() {
            return required;
        }
        
        private String getDefaultValue() throws ServiceException {
            if (required) {
                throw ServiceException.INVALID_REQUEST(attrName + " is required", null);
            }
            return defaultValue;
        }
    }
    
    private static class SMIMEConfig {
        private String configName;
        private Map<Field, String> fields = new HashMap<Field, String>();
        
        private SMIMEConfig(String configName) {
            this.configName = configName;
        }
        
        private String getName() {
            return configName;
        }
            
        private String getConfigured(Field field) {
            return fields.get(field);
        }
        
        private String get(Field field) throws ServiceException {
            String value = fields.get(field);
            if (value == null) {
                if (field.isRequired()) {
                    throw ServiceException.INVALID_REQUEST("missing " + field.getAttrName() + " for config " + configName, null);
                }
                value = field.getDefaultValue();
            }
            return value;
        }
        
        private boolean getBoolean(Field field) throws ServiceException {
            String value = get(field);
            return Boolean.valueOf(value);
        }
        
        private void set(Field field, String value) {
            fields.put(field, value);
        }
        
    }
    
    private Pair<String, String> parseAttrValue(String attrName, String attrValue) throws ServiceException {
        int delimiter = attrValue.indexOf(NAME_VALUE_DELIMITER);
        
        if (delimiter == -1 || delimiter == 0 || delimiter == attrValue.length() - 1) {
            throw ServiceException.INVALID_REQUEST("invalid " + attrName, null);
        }
        
        String name = attrValue.substring(0, delimiter).trim();
        String value = attrValue.substring(delimiter + 1).trim();
        
        return new Pair<String, String>(name, value);
    }
    
    private String encodeAttrValue(String configName, String value) {
        return configName + NAME_VALUE_DELIMITER + value;
    }
    
    private SMIMEConfig getConfigByName(String name, boolean createIfNotExist) {
        SMIMEConfig config = configs.get(name); 
        if (config == null && createIfNotExist) {
            config = new SMIMEConfig(name); 
            configs.put(name, config);
        }
        return config;
    }
    
    private boolean hasAnyConfig() {
        return !configs.isEmpty();
    }
    
    private void parse(boolean strict) throws ServiceException {
        for (Field field : Field.values()) {
            String attrName = field.getAttrName();
            String[] attrValues = entry.getMultiAttr(attrName, false);
            
            for (String attrValue : attrValues) {
                try {
                    Pair<String, String> nameValue = parseAttrValue(attrName, attrValue);
                    String configName = nameValue.getFirst();
                    String value = nameValue.getSecond();
                    
                    SMIMEConfig config = getConfigByName(configName, true);
                    
                    String fieldValue = config.getConfigured(field);
                    if (fieldValue != null) {
                        throw ServiceException.INVALID_REQUEST(attrName + " already has a value for config " + configName, null);
                    }
                    
                    if (!value.isEmpty()) {
                        config.set(field, value);
                    }
                } catch (ServiceException e) {
                    if (strict) {
                        throw e;
                    } else {
                        ZimbraLog.account.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    public static List<String> lookupPublicKeys(Account acct, String email) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.getDomain(acct);
        
        LdapSMIMEConfig smime = new LdapSMIMEConfig(domain, true);
        if (!smime.hasAnyConfig()) {
            smime = new LdapSMIMEConfig(prov.getConfig(), true);
        }
        
        if (!smime.hasAnyConfig()) {
            throw AccountServiceException.NO_SMIME_CONFIG("no SMIME config on domain " + domain.getName() + " or globalconfig");
        }
        
        return smime.lookup(email);
    }
    
    private List<String> lookup(String email) throws ServiceException {
        List<String> result = new ArrayList<String>();
        for (SMIMEConfig config : configs.values()) {
            lookup(config, email, result);
        }
        
        return result;
    }
    
    private void lookup(SMIMEConfig config, String email, List<String> result) throws ServiceException {
        
        String ldapUrl = config.get(Field.LDAP_URL);
        boolean startTLSEnabled = config.getBoolean(Field.STARTTLS_ENABLED);
        String bindDN = config.get(Field.BIND_DN);
        String bindPassword = config.get(Field.BIND_PASSWORD);
        String searchBase = config.get(Field.SEARCH_BASE);
        String filterTemplate = config.get(Field.FILTER_TEMPLATE);
        String attribute = config.get(Field.ATTRIBUTE);
        
        String filter = LdapUtil.computeAuthDn(email, filterTemplate); // TODO rename the method
        String[] attrs = attribute.split(",");
        Set<String> attrsSet = new HashSet<String>(Arrays.asList(attrs));
        
        SMIMELookupVisitor visitor = new SMIMELookupVisitor(result);
        
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(ldapUrl, startTLSEnabled, null, 
                bindDN, bindPassword, attrsSet, "SMIME public key lookup via external LDAP");
        } catch (NamingException e) {
            throw ServiceException.FAILURE("cannot establish LDAP connection", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("cannot establish LDAP connection", e);
        }
        
        LdapUtil.searchLdap(zlc, searchBase, filter, attrs, visitor);
    }
    
    private static class SMIMELookupVisitor implements SearchLdapVisitor {
        
        List<String> result;
        
        SMIMELookupVisitor(List<String> result) {
            this.result = result;
        }
        
        public void visit(String dn, Map<String, Object> attrsxx, Attributes ldapAttrs) {
            
            try {
                // all attrs are treated as binary data
                for (NamingEnumeration ne = ldapAttrs.getAll(); ne.hasMore(); ) {
                    Attribute attr = (Attribute) ne.next();
                    if (attr.size() == 1) {
                        Object o = attr.get();
                        result.add(ByteUtil.encodeLDAPBase64((byte[])o));
                    } else {
                        for (int i=0; i < attr.size(); i++) {
                            Object o = attr.get(i);
                            result.add(ByteUtil.encodeLDAPBase64((byte[])o));
                        }
                    }
                }
            } catch (NamingException e) {
                ZimbraLog.account.warn("caught NamingException", e);
            }
            
            /*
            for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                Object value = attr.getValue();
                if (value instanceof String) {
                    result.add((String)value);
                } else if (value instanceof String[]) {
                    for (String v : (String[])value) {
                        result.add(v);
                    }
                }
            }
            */
        }
    }
    
}
