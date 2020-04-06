/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.unboundid;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFWriter;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;

public class InMemoryLdapServer {
    public static final String ZIMBRA_LDAP_SERVER = "ZIMBRA_LDAP_SERVER";
    
    public static final String UNITTEST_BASE_DOMAIN_SEGMENT = "unittest";
    
    private static final String UNITTEST_DATA_PATH = LC.zimbra_home.value() + 
            "/unittest/ldap/InMemoryLdapServer";
    
    private static final String SCHEMA_FILE_NAME = "zimbra_schema.ldif";
    private static final String DIT_FILE_NAME = "zimbra_dit.ldif";
    
    private static Map<String, Server> servers = Maps.newHashMap();
    
    public static boolean isOn() {
        return DebugConfig.useInMemoryLdapServer;
    }
    
    private static String getSchemaFileName(String path) {
        return path + "/" + SCHEMA_FILE_NAME;
    }
    
    private static String getDITFileName(String path) {
        return path + "/" + DIT_FILE_NAME;
    }
    
    private static synchronized Server getServer(String serverName) {
        return servers.get(serverName);
    }
    
    private static synchronized void addServer(String serverName, Server server) {
        servers.put(serverName, server);
    }
    
    private static synchronized void removeServer(String serverName) {
        servers.remove(serverName);
    }
    
    public static synchronized void start(String serverName, ServerConfig serverConfig) 
    throws LdapException {
        Server server = getServer(serverName);
        if (server != null) {
            throw LdapException.LDAP_ERROR("in memory LDAP server is already started", null);
        }
        
        server = new Server(serverConfig);
        server.start();
        addServer(serverName, server);
    }
    
    public static synchronized void stop(String serverName) throws LdapException {
        Server server = getServer(serverName);
        if (server == null) {
            throw LdapException.LDAP_ERROR("in memory LDAP server was not started", null);
        }
        
        server.stop();
        removeServer(serverName);
    }
    
    static synchronized LDAPConnectionPool createConnPool(String serverName, LdapServerConfig config) 
    throws LdapException {
        Server server = getServer(serverName);
        if (server == null) {
            throw LdapException.LDAP_ERROR("in memory LDAP server was not started", null);
        }
        
        return server.getConnectionPool(config);
    }
    
    // only used for external LDAP auth, just use the default Zimbra server, as all
    // unit tests are using the Zimbra LDAP server for external LDAP server
    static LDAPConnection getConnection() throws LdapException {
        Server server = getServer(ZIMBRA_LDAP_SERVER);
        return server.getConnection();
    }
    
    static Schema getSchema(String serverName) throws LdapException {
        Server server = getServer(serverName);
        if (server == null) {
            throw LdapException.LDAP_ERROR("in memory LDAP server was not started", null);
        }
        
        return server.getSchema();
    }
    
    public static void export() throws LdapException {
        String path = LC.zimbra_tmp_directory.value() + "/inmem_ldap_%s.ldif";
        
        for (Map.Entry<String, Server> entry : servers.entrySet()) {
            String serverName = entry.getKey();
            Server server = entry.getValue();
            String filePath = String.format(path, serverName);
            server.export(filePath);
        }
    }
        
    /*
     * unboundid InMemoryDirectoryServer only supports OctetStringMatchingRule
     * for comparing the password.  Convert the password to it's SSHA 
     * encryption so it will compared equal in InMemoryDirectoryServer's bind 
     * handler.
     * 
     * Tesing hack for non-SSHA password: 
     * check prefix of the password, if it matches the prefix then don't encode it to SSHA.
     * Unit test code must use Password.genNonSSHAPassword() to get a non-SSHA password.
     * 
     */
    public static class Password {
        private static String NON_SSHA_PASSWORD_PREFIX = "__non_ssha__";
        
        public static String genNonSSHAPassword(String password) {
            return NON_SSHA_PASSWORD_PREFIX + password;
        }
        
        public static String treatPassword(String password) {
            if (password.startsWith(NON_SSHA_PASSWORD_PREFIX)) {
                return password;
            } else {
                return PasswordUtil.Scheme.SSHA.generate(password);
            }
        }
    }

    public static class ServerConfig {
        private String schemaLDIFFile;
        private String ditLDIFFile;
        private List<String> extraBaseDNs;
        
        public ServerConfig() {
            this(UNITTEST_DATA_PATH, null);
        }
        
        public ServerConfig(List<String> extraBaseDNs) {
            this(UNITTEST_DATA_PATH, extraBaseDNs);
        }
        
        public ServerConfig(String path, List<String> extraBaseDNs) {
            this(getSchemaFileName(path), getDITFileName(path), extraBaseDNs);
        }
        
        public ServerConfig(String schemaLDIFFile, String ditLDIFFile, 
                List<String> extraBaseDNs) {
            this.schemaLDIFFile = schemaLDIFFile;
            this.ditLDIFFile = ditLDIFFile;
            this.extraBaseDNs = extraBaseDNs;
        }
        
        private String schemaLDIFFile() {
            return schemaLDIFFile;
        }
        
        private String ditLDIFFile() {
            return ditLDIFFile;
        }
        
        private List<String> extraBaseDNs() {
            return extraBaseDNs;
        }
    }
    
    private static class Server {
        // path to the directory where SCHEMA_FILE_NAME and DIT_FILE_NAME are 
        private ServerConfig serverConfig;
        
        private InMemoryDirectoryServer server;
        
        private Server(ServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }
        
        private boolean started() {
            return server != null;
        }
        
        private void start() throws LdapException {
            if (started()) {
                throw LdapException.LDAP_ERROR("in memory LDAP server is already started", null);
            }
            
            boolean started = false;
            try {
                Schema schema = Schema.getSchema(serverConfig.schemaLDIFFile());
                
                // get top level domain name setup in reset-all.
                // In reset-all, the default domain name is the same as the local server name
                String domainName = LC.zimbra_server_hostname.value();
                String topLevelDomainDN = LdapUtil.domainToTopLevelDN(domainName);
                
                List<String> baseDNs = Lists.newArrayList("cn=zimbra", topLevelDomainDN, "dc=com");
                List<String> extraBaseDNs = serverConfig.extraBaseDNs();
                if (extraBaseDNs != null) {
                    baseDNs.addAll(extraBaseDNs);
                }
                
                // add list of known DNs 
                InMemoryDirectoryServerConfig config = 
                    new InMemoryDirectoryServerConfig(baseDNs.toArray(new String[baseDNs.size()]));

                config.addAdditionalBindCredentials("cn=config", LC.ldap_root_password.value());
                config.setSchema(schema);
                config.setGenerateOperationalAttributes(true);
                
                server = new InMemoryDirectoryServer(config);
                server.importFromLDIF(true, serverConfig.ditLDIFFile());
                server.startListening();
                started = true;
                
            } catch (LDIFException e) {
                throw LdapException.LDAP_ERROR("failed to start in memory ldap server", e);
            } catch (LDAPException e) {
                throw LdapException.LDAP_ERROR("failed to start in memory ldap server", e);
            } catch (IOException e) {
                throw LdapException.LDAP_ERROR("failed to start in memory ldap server", e);
            } finally {
                if (!started) {
                    server = null;
                }
            }
        }
        
        private void stop() throws LdapException {
            if (!started()) {
                throw LdapException.LDAP_ERROR("in memory LDAP server was not started", null);
            }
            
            server.shutDown(true);
        }
        
        private Schema getSchema() throws LdapException {
            try {
                return server.getSchema();
            } catch (LDAPException e) {
                throw UBIDLdapException.mapToLdapException(e);
            }
        }
        
        private void export(String path) throws LdapException {
            try {
                boolean excludeGeneratedAttrs = false;
                boolean excludeChangeLog = true;
                server.exportToLDIF(path, excludeGeneratedAttrs, excludeChangeLog);
            } catch (LDAPException e) {
                throw UBIDLdapException.mapToLdapException(e);
            }
        }
        
        private LDAPConnectionPool getConnectionPool(LdapServerConfig config) 
        throws LdapException {
            try {
                /*
                 * For external LDAP connections, config.getConnPoolInitSize() 
                 * always returns 0 (see comments in ExternalLdapConfig.getConnPoolInitSize()).
                 * 
                 * But for InMemoryDirectoryServer.getConnectionPool(), it internally 
                 * uses a sample connection as a template for connections in the the pool 
                 * and puts the sample connection in the pool, and therefore requires the 
                 * init size must be >= 1. 
                 */
                int initPoolSize = config.getConnPoolInitSize();
                if (config instanceof LdapServerConfig.ExternalLdapConfig) {
                    if (initPoolSize < 1) {
                        initPoolSize = 1;
                    }
                }
                assert(initPoolSize >= 1);
                
                // create a connection pool
                return server.getConnectionPool(null, null, 
                        initPoolSize, config.getConnPoolMaxSize());
            } catch (LDAPException e) {
                throw UBIDLdapException.mapToLdapException(e);
            }
        }
        
        private LDAPConnection getConnection() throws LdapException {
            try {
                return server.getConnection();
            } catch (LDAPException e) {
                throw UBIDLdapException.mapToLdapException(e);
            }
        }

    }
    
    
    /*
     * Export from LDAP server to LDIF
     */
    private static class Exporter {
        
        private PrintStream logger;
        private String path;
        
        private Exporter(String path) {
            this(path, System.out);
        }
        
        private Exporter(String path, PrintStream logger) {
            this.logger = logger;
            this.path = path;
        }
        
        /**
         * Generate LDIF files for Zimbra schema and the entire DIT from the 
         * current image of LDAP database.
         */
        private void generateLDIF() {
            
            UBIDLdapContext zlc = null;
            
            try {
                zlc = (UBIDLdapContext) LdapClient.getContext(LdapUsage.UNITTEST);
                LDAPConnection conn = zlc.getNative();
                
                /*
                 * generate LDIF for schema
                 */
                String filePath = getSchemaFileName(path);
                logger.println("============================================");
                logger.println("    Generating " + filePath);
                logger.println("============================================");
                Schema schema = conn.getSchema();
                LDIFWriter schemaWriter = new LDIFWriter(filePath);
                schemaWriter.writeEntry(schema.getSchemaEntry());
                schemaWriter.close();
                
                logger.println();
                
                /*
                 * generate LDIF for DIT objects
                 */
                filePath = getDITFileName(path);
                logger.println("============================================");
                logger.println("    Generating " + filePath);
                logger.println("============================================");
                LDIFWriter ditWriter = new LDIFWriter(filePath);
                SearchResult searchResult =
                    conn.search(LdapConstants.DN_ROOT_DSE, SearchScope.SUB, "(objectClass=*)");
                for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                    ditWriter.writeEntry(entry);
                }
                ditWriter.close();
                
            } catch (ServiceException e) {
                logger.println("Failed to generate LDIF files for InMemoryLdapSerer");
                e.printStackTrace(logger);
            } catch (LDAPException e) {
                logger.println("Failed to generate LDIF files for InMemoryLdapSerer");
                e.printStackTrace(logger);
            } catch (IOException e) {
                logger.println("Failed to generate LDIF files for InMemoryLdapSerer");
                e.printStackTrace(logger);
            } finally {
                LdapClient.closeContext(zlc);
            }
        }
        
        private Schema getSchema() throws ServiceException {
            UBIDLdapContext zlc = null;
            
            try {
                zlc = (UBIDLdapContext) LdapClient.getContext(LdapUsage.UNITTEST);
                LDAPConnection conn = zlc.getNative();
                Schema schema = conn.getSchema();
                return schema;
            } catch (LDAPException e) {
                throw ServiceException.FAILURE("unable to extract schema", e);
            } finally {
                LdapClient.closeContext(zlc);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        String path = args[0];
        
        Exporter exporter = new Exporter(path);
        exporter.generateLDIF();
    }
}
