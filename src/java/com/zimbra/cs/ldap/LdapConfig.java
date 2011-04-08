package com.zimbra.cs.ldap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;

public class LdapConfig {
    private static final String DEFALT_LDAP_PORT = "389";
    
    private String replicaURL;
    private String masterURL;
    private String adminBindDN;
    private String adminBindPassword;
    private boolean wantStartTLS;
    private boolean sslAllowUntrustedCerts;
    private int connPoolInitSize;
    private int connPoolMaxSize;
    
    public static LdapConfig loadZimbraConfig() {
        LdapConfig config = new LdapConfig();
        
        //
        // LDAP url for replica and master
        //
        String replicaURL;
        String masterURL;
        
        replicaURL = LC.ldap_url.value().trim();
        if (replicaURL.length() == 0) {
            String ldapHost = LC.ldap_host.value();
            String ldapPort = LC.ldap_port.value();
            
            if (StringUtil.isNullOrEmpty(ldapHost)) {
                ldapHost = "localhost";
            }
            if (StringUtil.isNullOrEmpty(ldapPort)) {
                ldapPort = DEFALT_LDAP_PORT;
            }
            replicaURL = "ldap://" + ldapHost + ":" + ldapPort + "/";
        }
        masterURL = LC.ldap_master_url.value().trim();
        if (masterURL.length() == 0) {
            masterURL = replicaURL;
        }
        
        config.replicaURL = replicaURL;
        config.masterURL = masterURL;
        
        //
        // admin bind DN and bind password
        //
        config.adminBindDN = LC.zimbra_ldap_userdn.value();
        config.adminBindPassword = LC.zimbra_ldap_password.value();
        
        //
        // startTLS settings
        //
        boolean ldap_starttls_supported = "1".equals(LC.ldap_starttls_supported.value());
        boolean ldap_starttls_required = LC.ldap_starttls_required.booleanValue();
        boolean zimbra_require_interprocess_security = "1".equals(LC.zimbra_require_interprocess_security.value());
        
        config.wantStartTLS = (ldap_starttls_supported && ldap_starttls_required && zimbra_require_interprocess_security);
        
        //
        // SSL settings
        //
        config.sslAllowUntrustedCerts = LC.ssl_allow_untrusted_certs.booleanValue();
            
        //
        // connection pool settings
        //
        // System.setProperty("com.sun.jndi.ldap.connect.pool.debug", LC.ldap_connect_pool_debug.value());
        config.connPoolInitSize = LC.ldap_connect_pool_initsize.intValue();
        config.connPoolMaxSize = LC.ldap_connect_pool_maxsize.intValue();
        // System.setProperty("com.sun.jndi.ldap.connect.pool.prefsize", LC.ldap_connect_pool_prefsize.value());
        // System.setProperty("com.sun.jndi.ldap.connect.pool.timeout", LC.ldap_connect_pool_timeout.value());
        
        
        return config;
    }
    
    public String getReplicaURL() {
        return replicaURL; 
    }
    
    public String getMasterURL() {
        return masterURL; 
    }
    
    public String getAdminBindDN() {
        return adminBindDN;
    }
    
    public String getAdminBindPassword() {
        return adminBindPassword;
    }
    
    public boolean wantStartTLS() {
        return wantStartTLS;
    }
    
    public boolean sslAllowUntrustedCerts() {
        return sslAllowUntrustedCerts;
    }
    
    public int getConnPoolInitSize() {
        return connPoolInitSize;
    }
    
    public int getConnPoolMaxSize() {
        return connPoolMaxSize;
    }
}
