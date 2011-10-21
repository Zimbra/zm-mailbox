/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap;

import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;

public abstract class LdapServerConfig {
    private static final String DEFALT_LDAP_PORT = "389";
    
    protected String ldapURL;  // space separated URLs
    protected String adminBindDN;
    protected String adminBindPassword;
    
    // whether startTLS is wanted
    // if ldapURL contains ldaps URL, then ldaps is honored not startTLS.
    protected boolean wantStartTLS; 
    
    // the actual connection type
    protected LdapConnType connType;
    
    // common settings from LC for both internal and external LDAP server
    // make them final - they are NOT taken into account for building 
    // connection pool keys.  If any of these becomes set-able, 
    // ExternalLdapConfig.getConnPoolKey() has to be modified accordingly.
    protected final  boolean sslAllowUntrustedCerts;
    
    protected final int connPoolMaxSize;
    protected final int connPoolTimeoutMillis;
    
    // Enable on-checkout health check.
    //     Health check is invoked whenever a connection is checked out from the pool
    //
    // Note: If on-checkout health check is enabled, background health check will be
    //       disabled.  If on-checkout health check is disabled, background health check 
    //       will be enabled.  
    //       Behavior difference in falling back to the next availble server in the sever set
    //       for the pool:
    //
    //       on-checkout: 
    //           No failure will be seen if one server in the server set goes down,
    //           because the connection pool always tests health of a connection 
    //           before handing it out to the application.
    //       background: 
    //           If a server goes down, LDAP op using the connection will fail, because 
    //           no health check is invoked prior to handing out the connection.
    //           *All* bad connections will be flushed out of the connection pool at the 
    //           next health check interval (specified by LC.ldap_connect_pool_health_check_background_interval_millis).
    //           Then good connections will be created (because there is no conn in the pool) 
    //           using the next server in the server set.  All LDAP ops - if they got a 
    //           connection pointing to the bad server - executed before the health check 
    //           will fail.   After the health check interval, LDAP ops should all works again 
    //           if there is a healthy server in the server set for the connection pool.
    //         
    protected final boolean connPoolHelathCheckOnCheckoutEnabled;
    
    // length of time in milliseconds between periodic background health checks against 
    // the available connections in connection pool
    //
    // Note: effective only when background health is enabled.
    protected final long connPoolHelathCheckBackgroundIntervalMillis;
    
    // The maximum length of time in milliseconds that should be allowed for the health check response 
    // to come back from the LDAP server 
    // If the provided value is less than or equal to zero, then the default value of 30000 milliseconds 
    // (30 seconds) will be used by unboundid SDK.
    protected final long connPoolHelathCheckMaxResponseTimeMillis;
    
    protected final int connectTimeoutMillis;
    protected final int readTimeoutMillis;
    
    
    public abstract int getConnPoolInitSize();
    
    
    private LdapServerConfig() {
        
        // load common settings, for both Zimbra LDAP and external LDAP
        
        //
        // SSL settings
        //
        this.sslAllowUntrustedCerts = LC.ssl_allow_untrusted_certs.booleanValue();
            
        //
        // connection pool settings
        //
        // System.setProperty("com.sun.jndi.ldap.connect.pool.debug", LC.ldap_connect_pool_debug.value());
        this.connPoolMaxSize = LC.ldap_connect_pool_maxsize.intValue();
        // System.setProperty("com.sun.jndi.ldap.connect.pool.prefsize", LC.ldap_connect_pool_prefsize.value());
        this.connPoolTimeoutMillis = LC.ldap_connect_pool_timeout.intValue();
        
        this.connPoolHelathCheckOnCheckoutEnabled = 
            LC.ldap_connect_pool_health_check_on_checkout_enabled.booleanValue();
        this.connPoolHelathCheckBackgroundIntervalMillis = 
            LC.ldap_connect_pool_health_check_background_interval_millis.longValue();
        this.connPoolHelathCheckMaxResponseTimeMillis = 
            LC.ldap_connect_pool_health_check_max_response_time_millis.longValue();
        
        // timeout setting
        this.connectTimeoutMillis = LC.ldap_connect_timeout.intValue();
        this.readTimeoutMillis = LC.ldap_read_timeout.intValue();

    }
    
    
    public static class ZimbraLdapConfig extends LdapServerConfig {
        private LdapServerType serverType;
        
        // This is a Zimbra LDAP setting only.
        private final int connPoolInitSize;
        
        public ZimbraLdapConfig(LdapServerType serverType) {
            super();
            
            this.serverType = serverType;
            this.connPoolInitSize = LC.ldap_connect_pool_initsize.intValue();
            
            if (LdapServerType.MASTER == this.serverType) {
                this.ldapURL = getMasterURL();
            } else {
                this.ldapURL = getReplicaURL();
            }
            
            /*
             * admin bind DN and bind password
             */
            this.adminBindDN = LC.zimbra_ldap_userdn.value();
            this.adminBindPassword = LC.zimbra_ldap_password.value();
            
            /*
             * startTLS settings
             */
            // Whether the LDAP server supports the startTLS operation.
            boolean ldap_starttls_supported = "1".equals(LC.ldap_starttls_supported.value());
            // Whether starttls is required for java ldap client when it establishes connections to the Zimbra ldap server
            boolean ldap_starttls_required = LC.ldap_starttls_required.booleanValue();
            boolean zimbra_require_interprocess_security = "1".equals(LC.zimbra_require_interprocess_security.value());
            
            this.wantStartTLS = (ldap_starttls_supported && ldap_starttls_required && zimbra_require_interprocess_security);
            
            this.connType = LdapConnType.getConnType(this.ldapURL, this.wantStartTLS);
        }
        
        @Override
        public int getConnPoolInitSize() {
            return connPoolInitSize;
        }
        
        private String getReplicaURL() {
            String replicaURL;
            
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
            
            return replicaURL;
        }
        
        private String getMasterURL() {
            String masterURL = LC.ldap_master_url.value().trim();
            if (masterURL.length() == 0) {
                masterURL = getReplicaURL();
            }
            
            return masterURL;
        }
    }
    
    public static class ExternalLdapConfig extends LdapServerConfig {
        
        // only in external LDAP settings, in ZimbraLDAP the deref policy is never
        protected String derefAliasPolicy;  
        
        private String authMech;
        private Set<String> binaryAttrs;  // not needed for unboundid
        private String notes;  // for debugging purpose

        /**
         * Instantiate an external LDAP config
         * 
         * @param urls          space separated URLs
         * @param wantStartTLS  whether startTLS is wanted (won't be honored if urls is ldaps)
         * @param authMech      // TODO: cleanup.  For now, if null: simple bind if binDN/password is not null, anon bind otherwise
         * @param bindDn
         * @param bindPassword
         * @param binaryAttrs
         * @param note
         */
        public ExternalLdapConfig(String urls, boolean wantStartTLS, String authMech, 
                String bindDn, String bindPassword, Set<String> binaryAttrs, String note) {
            super();
            
            this.ldapURL = urls;
            this.adminBindDN = bindDn;
            this.adminBindPassword = bindPassword;
            this.wantStartTLS = wantStartTLS;
            
            this.authMech = authMech;
            this.binaryAttrs = binaryAttrs;
            this.notes = notes;
            
            this.derefAliasPolicy = LC.ldap_deref_aliases.value();
            
            this.connType = LdapConnType.getConnType(this.ldapURL, this.wantStartTLS);
        }
        
        /**
         * Instantiate an external LDAP config. 
         * 
         * @param urls          array of URLs
         * @param wantStartTLS
         * @param authMech
         * @param bindDn
         * @param bindPassword
         * @param binaryAttrs
         * @param note
         */
        public ExternalLdapConfig(String[] urls, boolean wantStartTLS, String authMech, 
                String bindDn, String bindPassword, Set<String> binaryAttrs, String note) {
            this (LdapServerConfig.joinURLS(urls), wantStartTLS, authMech, 
                    bindDn, bindPassword, binaryAttrs,  note);
        }
        
        public static class ConnPoolKey {
            private static final char DELIMITER = ':';
            
            public static String getConnPoolKey(ExternalLdapConfig config) {
                StringBuilder key = new StringBuilder();
                key.append(config.ldapURL + DELIMITER);
                key.append(config.connType.toString() + DELIMITER);
                key.append((config.authMech == null ? "" : config.authMech) + DELIMITER);
                key.append((config.adminBindDN == null ? "" : config.adminBindDN) + DELIMITER);
                key.append((config.adminBindPassword == null ? "" :  config.adminBindPassword));
                
                // do not take into account common settings set in LdapConfig
                // they should be all the same.  
                
                return key.toString();
            }
            
            // given a key in the format of the String returned by getConnPoolKey,
            // return a display name for loggin purpose - basically just hide the password
            public static String getDisplayName(String key) {
                int offset = key.lastIndexOf(DELIMITER);
                return key.substring(0, offset);
            }
        }
        
        public String getAuthMech() {
            return authMech;
        }
        
        public Set<String> getBinaryAttrs() {
            return binaryAttrs;
        }
        
        public String getNotes() {
            return notes;
        }
        
        public String getDerefAliasPolicy() {
            return derefAliasPolicy;
        }

        @Override
        public int getConnPoolInitSize() {
            /*
             * ALWAYS return 0.
             * We don't want to create any connection during connection pool
             * creation, because ConnectionPool.getConnPoolByName() has to be 
             * guarded in a static synchronized block.  We do not want to block 
             * all threads needing this pool wait on this lock when the initial 
             * connection takes too long.
             */
            return 0;
        }
        
    }


    public static String joinURLS(String urls[]) {
        if (urls.length == 1) return urls[0];
        StringBuffer url = new StringBuffer();
        for (int i=0; i < urls.length; i++) {
            if (i > 0) url.append(' ');
            url.append(urls[i]);
        }
        return url.toString();
    }
    
    // return space separated URLs
    public String getLdapURL() {
        return ldapURL; 
    }
    
    public String getAdminBindDN() {
        return adminBindDN;
    }
    
    public String getAdminBindPassword() {
        return adminBindPassword;
    }
    
    public boolean getWantStartTLS() {
        return wantStartTLS;
    }
    
    public LdapConnType getConnType() {
        return connType;
    }
    
    public boolean sslAllowUntrustedCerts() {
        return sslAllowUntrustedCerts;
    }
    
    public int getConnPoolMaxSize() {
        return connPoolMaxSize;
    }
    
    public int getConnPoolTimeoutMillis() {
        return connPoolTimeoutMillis;
    }
    
    public boolean isConnPoolHelathCheckOnCheckoutEnabled() {
        return connPoolHelathCheckOnCheckoutEnabled;
    }
    
    public long getConnPoolHelathCheckBackgroundIntervalMillis() {
        return connPoolHelathCheckBackgroundIntervalMillis;
    }
    
    public long getConnPoolHelathCheckMaxResponseTimeMillis() {
        return connPoolHelathCheckMaxResponseTimeMillis;
    }
    
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }
    
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

}
