/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.localconfig;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Provide convenient means to get at local configuration - stuff that
 * we do not want in LDAP.  Use one of these methods:
 * 
 *      String v = LC.get("my_config_param");
 * 
 * or 
 * 
 *      String v = LC.my_config_param.value();
 * 
 * The latter ofcourse is better.
 */
public class LC {
   
    public static String get(String key) {
        String value = null;
        try {
            value = LocalConfig.getInstance().get(key);
        } catch (ConfigException ce) {
            /*
             * We swallow the exception here because we know that
             * LocalConfig does a verify and the exception can never
             * occur here - we do not let that any sets on the
             * mLocalConfig field in this class - so no one can beat
             * the verification done in the constructor.
             */
            Logging.warn("LC.get(" + key + ") caused exception, returning empty string", ce);
            value = "";
        }
        if (value == null) {
            Logging.warn("LC.get(" + key + ") was null, returning empty string");
            value = "";
        }
        return value;
    }

    public static String[] getAllKeys() {
    	return LocalConfig.getInstance().allKeys();
    }

    static void init() {
        // This method is there to guarantee static initializer of this
        // class is run.
    }

    public static final KnownKey zimbra_home;
    public static final KnownKey zimbra_java_home;
    public static final KnownKey zimbra_log_directory;
    public static final KnownKey zimbra_index_directory;
    public static final KnownKey zimbra_store_directory;
    public static final KnownKey zimbra_db_directory;
    public static final KnownKey zimbra_tmp_directory;
    public static final KnownKey zimbra_extensions_directory;
    public static final KnownKey zimbra_mysql_user;
    public static final KnownKey zimbra_mysql_password;
    public static final KnownKey zimbra_ldap_userdn;
    public static final KnownKey zimbra_ldap_password;
    public static final KnownKey zimbra_server_hostname;
    public static final KnownKey zimbra_user;
    public static final KnownKey zimbra_uid;
    public static final KnownKey zimbra_gid;
    public static final KnownKey zimbra_log4j_properties;
    public static final KnownKey localized_msgs_directory;

    public static final KnownKey zimbra_store_sweeper_max_age;

    public static final KnownKey zimbra_index_max_uncommitted_operations;
    public static final KnownKey zimbra_index_lru_size;
    public static final KnownKey zimbra_index_idle_flush_time;
    
    public static final KnownKey zimbra_spam_report_queue_size;
    
    public static final KnownKey stats_img_folder;
    
    public static final KnownKey ldap_host;
    public static final KnownKey ldap_log_level;
    public static final KnownKey ldap_port;
    public static final KnownKey ldap_url;
    public static final KnownKey ldap_master_url;
    public static final KnownKey ldap_is_master;
    public static final KnownKey ldap_root_password;
    public static final KnownKey ldap_connect_timeout;
    public static final KnownKey ldap_connect_pool_debug;
    public static final KnownKey ldap_connect_pool_initsize;
    public static final KnownKey ldap_connect_pool_maxsize;
    public static final KnownKey ldap_connect_pool_prefsize;
    public static final KnownKey ldap_connect_pool_timeout;
    
    public static final KnownKey ldap_cache_account_maxsize;
    public static final KnownKey ldap_cache_account_maxage;
    public static final KnownKey ldap_cache_cos_maxsize;
    public static final KnownKey ldap_cache_cos_maxage;
    public static final KnownKey ldap_cache_domain_maxsize;
    public static final KnownKey ldap_cache_domain_maxage;
    public static final KnownKey ldap_cache_server_maxsize;
    public static final KnownKey ldap_cache_server_maxage;
    public static final KnownKey ldap_cache_group_maxsize;
    public static final KnownKey ldap_cache_group_maxage;
    public static final KnownKey ldap_cache_timezone_maxsize;
    public static final KnownKey ldap_cache_zimlet_maxsize;
    public static final KnownKey ldap_cache_zimlet_maxage;

    public static final KnownKey mysql_directory;
    public static final KnownKey mysql_data_directory;
    public static final KnownKey mysql_socket;
    public static final KnownKey mysql_pidfile;
    public static final KnownKey mysql_mycnf;
    public static final KnownKey mysql_bind_address;
    public static final KnownKey mysql_port;
    public static final KnownKey mysql_root_password;
    public static final KnownKey mysql_memory_percent;
    public static final KnownKey mysql_innodb_log_buffer_size;
    public static final KnownKey mysql_innodb_log_file_size;
    public static final KnownKey mysql_sort_buffer_size;
    public static final KnownKey mysql_read_buffer_size;
    public static final KnownKey mysql_table_cache;

    public static final KnownKey logger_mysql_directory;
    public static final KnownKey logger_mysql_data_directory;
    public static final KnownKey logger_mysql_socket;
    public static final KnownKey logger_mysql_pidfile;
    public static final KnownKey logger_mysql_mycnf;
    public static final KnownKey logger_mysql_bind_address;
    public static final KnownKey logger_mysql_port;
    public static final KnownKey zimbra_logger_mysql_password;

	public static final KnownKey postfix_alias_maps;
	public static final KnownKey postfix_broken_sasl_auth_clients;
	public static final KnownKey postfix_command_directory;
	public static final KnownKey postfix_daemon_directory;
	public static final KnownKey postfix_header_checks;
	public static final KnownKey postfix_mailq_path;
	public static final KnownKey postfix_manpage_directory;
	public static final KnownKey postfix_newaliases_path;
	public static final KnownKey postfix_queue_directory;
	public static final KnownKey postfix_sender_canonical_maps;
	public static final KnownKey postfix_sendmail_path;
	public static final KnownKey postfix_smtpd_client_restrictions;
	public static final KnownKey postfix_smtpd_data_restrictions;
	public static final KnownKey postfix_smtpd_helo_required;
	public static final KnownKey postfix_smtpd_tls_cert_file;
	public static final KnownKey postfix_smtpd_tls_key_file;
	public static final KnownKey postfix_smtpd_tls_loglevel;
	public static final KnownKey postfix_transport_maps;
	public static final KnownKey postfix_version;
	public static final KnownKey postfix_virtual_alias_domains;
	public static final KnownKey postfix_virtual_alias_maps;
	public static final KnownKey postfix_virtual_mailbox_domains;
	public static final KnownKey postfix_virtual_mailbox_maps;
	public static final KnownKey postfix_virtual_transport;

    public static final KnownKey tomcat_directory;
    public static final KnownKey tomcat_java_heap_memory_percent;
    public static final KnownKey tomcat_java_options;
    public static final KnownKey tomcat_java_home;
    public static final KnownKey tomcat_pidfile;
    public static final KnownKey tomcat_keystore;

    public static final KnownKey ssl_allow_untrusted_certs;

    public static final KnownKey zimlet_directory;
    public static final KnownKey wiki_enabled;
    public static final KnownKey wiki_user;
    
    public static final KnownKey nio_imap_enable;
    public static final KnownKey nio_imap_debug_logging;

    public static final KnownKey zimbra_mtareport_max_recipients;
    public static final KnownKey zimbra_mtareport_max_senders;

    static {
        final String ZM_MYCNF_CAVEAT = "This value is stored here for use by zmmycnf program.  Changing this setting does not immediately reflect in MySQL server.  You will have to, with abundant precaution, re-generate my.cnf and restart MySQL server for the change to take effect.";
        final String FS = File.separator;
        String hostname = "lookup.failed";
        try {
            InetAddress address = InetAddress.getLocalHost();
            String host = address.getCanonicalHostName();
            if (Character.isDigit(host.charAt(0)))
                host = address.getHostName();
            hostname = host.toLowerCase();
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        }

        zimbra_home = new KnownKey("zimbra_home");
        zimbra_home.setDefault(FS + "opt" + FS + "zimbra");
        zimbra_home.setForceToEdit(true);
        zimbra_home.setDoc
            ("Zimbra installation root and home directory of `zimbra'" + 
             " UNIX user. You can not relocate install root - do" +
             " not change this setting.");
        
        zimbra_java_home = new KnownKey("zimbra_java_home");
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            zimbra_java_home.setDefault("/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home");
        } else {
            zimbra_java_home.setDefault("${zimbra_home}" + FS + "java");
        }
        zimbra_java_home.setDoc
            ("Path to a JDK/J2SDK.");

        zimbra_log_directory = new KnownKey("zimbra_log_directory");
        zimbra_log_directory.setDefault("${zimbra_home}" + FS + "log");
        zimbra_log_directory.setDoc
            ("Directory where log files are written.");
        
        zimbra_index_directory = new KnownKey("zimbra_index_directory");
        zimbra_index_directory.setDefault("${zimbra_home}" + FS + "index");
        zimbra_index_directory.setDoc
            ("Directory for mailbox index files.");

        zimbra_store_directory = new KnownKey("zimbra_store_directory");
        zimbra_store_directory.setDefault("${zimbra_home}" + FS + "store");
        zimbra_store_directory.setDoc
            ("Directory for mailbox messages.");

        zimbra_db_directory = new KnownKey("zimbra_db_directory");
        zimbra_db_directory.setDefault("${zimbra_home}" + FS + "db");
        zimbra_db_directory.setDoc
            ("Directory for database files.");

        zimbra_tmp_directory = new KnownKey("zimbra_tmp_directory");
        zimbra_tmp_directory.setDefault(System.getProperty("java.io.tmpdir") + FS + "zimbra");
        zimbra_tmp_directory.setDoc
            ("Directory for temporary files.");

        zimbra_extensions_directory = new KnownKey("zimbra_extension_directory");
        zimbra_extensions_directory.setDefault("${zimbra_home}" + FS + "lib" + FS + "ext");
        zimbra_extensions_directory.setDoc
        	("Directory whose subdirs contain extensions");
        
        zimbra_mysql_user = new KnownKey("zimbra_mysql_user");
        zimbra_mysql_user.setDefault("zimbra");
        zimbra_mysql_user.setDoc
            ("MySQL username to use to create/access zimbra databases" +
             " and tables. This is the value you would supply to" +
             " the '-u' option of 'mysql' command line program.");

        zimbra_mysql_password = new KnownKey("zimbra_mysql_password");
        zimbra_mysql_password.setDefault("zimbra");
        zimbra_mysql_password.setForceToEdit(true);
        zimbra_mysql_password.setDoc
            ("Password for " + zimbra_mysql_user.key() + ". Stored in" +
             " local config for use by the store application to" +
             " authenticate.  If you want to change this password," +
             " please use the zmmypasswd program which will change the" +
             " password in both MySQL and in local config.");

        zimbra_ldap_userdn = new KnownKey("zimbra_ldap_userdn");
        zimbra_ldap_userdn.setDefault("uid=zimbra,cn=admins,cn=zimbra");
        zimbra_ldap_userdn.setDoc
            ("LDAP dn used to authenticate the store application with LDAP.");

        zimbra_ldap_password = new KnownKey("zimbra_ldap_password");
        zimbra_ldap_password.setDefault("zimbra");
        zimbra_ldap_password.setForceToEdit(true);
        zimbra_ldap_password.setDoc
            ("Password for " + zimbra_ldap_userdn.key() + ". Stored in" +
             " local config for use by the store application to" +
             " authenticate.  If you want to change this password," +
             " please use the zmldappasswd program which will  change the" +
             " password in both LDAP and in local config.");

        zimbra_server_hostname = new KnownKey("zimbra_server_hostname");
        zimbra_server_hostname.setDefault(hostname);
        zimbra_server_hostname.setDoc
            ("The provisioned name of this server. There should exist" +
             " a corresponding `server' entry in LDAP - consult" +
             " documentation for CreateServer command of the zmprov program.");

        zimbra_user = new KnownKey("zimbra_user");
        zimbra_user.setDefault("zimbra");
        zimbra_user.setDoc
            ("The zimbra unix user to which the zimbra server process must" +
             " switch privileges to, after binding privileged ports.");

        zimbra_uid = new KnownKey("zimbra_uid");
        zimbra_uid.setDefault("-1");
        zimbra_uid.setDoc
            ("The zimbra unix uid to which the zimbra server process must" +
             " switch privileges to, after binding privileged ports.");

        zimbra_gid = new KnownKey("zimbra_gid");
        zimbra_gid.setDefault("-1");
        zimbra_gid.setDoc
            ("The zimbra unix gid to which the zimbra server process must" +
             " switch privileges to, after binding privileged ports.");

        zimbra_log4j_properties = new KnownKey("zimbra_log4j_properties");
        zimbra_log4j_properties.setDefault("${zimbra_home}" + FS + "conf" + FS + "log4j.properties");
        zimbra_log4j_properties.setDoc
            ("Path to log4j configuration properties file.");

        localized_msgs_directory = new KnownKey("localized_msgs_directory");
        localized_msgs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "msgs");
        localized_msgs_directory.setDoc
            ("Directory for localized message files");

        zimbra_store_sweeper_max_age = new KnownKey("zimbra_store_sweeper_max_age");
        zimbra_store_sweeper_max_age.setDefault("480");  // 480 mins = 8 hours
        zimbra_store_sweeper_max_age.setDoc
            ("Files older than this many minutes are auto-deleted from store incoming directory.");

        zimbra_index_max_uncommitted_operations = new KnownKey("zimbra_index_max_uncommitted_operations");
        zimbra_index_max_uncommitted_operations.setDefault("200");
        zimbra_index_max_uncommitted_operations.setDoc
            ("Maximum number of uncommitted indexing operations" +
             " that may accumulate per mailbox before forcing a commit.");
        
        zimbra_index_lru_size = new KnownKey("zimbra_index_lru_size");
        zimbra_index_lru_size.setDefault("100");
        zimbra_index_lru_size.setDoc
            ("Maximum number of open mailbox index writers in the LRU map.");
        
        zimbra_index_idle_flush_time = new KnownKey("zimbra_index_idle_flush_time");
        zimbra_index_idle_flush_time.setDefault("600");
        zimbra_index_idle_flush_time.setDoc
            ("If idle for longer than this value (in seconds), flush" +
             " uncommitted indexing ops in mailbox.");

        zimbra_spam_report_queue_size = new KnownKey("zimbra_spam_report_queue_size");
        zimbra_spam_report_queue_size.setDefault("100");
        zimbra_spam_report_queue_size.setDoc
            ("For Junk/Not Junk Msg/ConvActionRequests this queue size limits the" +
             " the server workqueue for processing the forwards");

        stats_img_folder = new KnownKey("stats_img_folder");
        stats_img_folder.setDefault("${zimbra_home}" + FS + "zimbramon" + FS + "rrdtool" + FS + "work");
        stats_img_folder.setDoc
            ("Directory for storing generated statistics images.");

        ldap_host = new KnownKey("ldap_host");
        ldap_host.setDefault("");
        ldap_host.setDoc("LDAP host to use.  Deprecated - please use ldap_url instead.");

        ldap_log_level = new KnownKey("ldap_log_level");
        ldap_log_level.setDefault("0");
        ldap_log_level.setDoc
            ("LDAP logging level");

        ldap_port = new KnownKey("ldap_port");
        ldap_port.setDefault("");
        ldap_port.setDoc("LDAP port to use.  Deprecated - please use ldap_url instead.");

        ldap_url = new KnownKey("ldap_url");
        ldap_url.setDefault("");
        ldap_url.setDoc("List of LDAP servers for use by this server.");
        
        ldap_master_url = new KnownKey("ldap_master_url");
        ldap_master_url.setDefault("");
        ldap_master_url.setDoc("URL to the LDAP master.");

        ldap_is_master = new KnownKey("ldap_is_master");
        ldap_is_master.setDefault("false");
        ldap_is_master.setDoc("Is this host the master LDAP server?");

        ldap_root_password = new KnownKey("ldap_root_password");
        ldap_root_password.setDefault("zimbra");
        ldap_root_password.setForceToEdit(true);
        ldap_root_password.setDoc
            ("Password for LDAP slapd.conf rootdn.  As a convenience," +
             " during LDAP initialization a random password is" +
             " generated, saved in local config and in slapd.conf.  If you" +
             " want to change this password, please use the zmldappasswd" +
             " program which will change the password in both slapd.conf" +
             " and in local config.");

        ldap_connect_timeout = new KnownKey("ldap_connect_timeout");
        ldap_connect_timeout.setDefault("10000");
        ldap_connect_timeout.setDoc
            ("Milliseconds after which a connection attempt is aborted.");

        ldap_connect_pool_debug = new KnownKey("ldap_connect_pool_debug");
        ldap_connect_pool_debug.setDefault("false");
        ldap_connect_pool_debug.setDoc
            ("Whether to debug LDAP connection pooling.");
        
        ldap_connect_pool_initsize = new KnownKey("ldap_connect_pool_initsize");
        ldap_connect_pool_initsize.setDefault("1");
        ldap_connect_pool_initsize.setDoc
            ("Initial number of active LDAP connections to ramp up to.");
        
        ldap_connect_pool_maxsize = new KnownKey("ldap_connect_pool_maxsize");
        ldap_connect_pool_maxsize.setDefault("25");
        ldap_connect_pool_maxsize.setDoc
            ("Maximum number of LDAP active and idle connections allowed.");
        
        ldap_connect_pool_prefsize = new KnownKey("ldap_connect_pool_prefsize");
        ldap_connect_pool_prefsize.setDefault("0");
        ldap_connect_pool_prefsize.setDoc
            ("Preferred number  of LDAP connections - setting both maxsize" +
             " and prefsize to the same value maintains the connection pool" +
             " at a fixed size.");
        
        ldap_connect_pool_timeout = new KnownKey("ldap_connect_pool_timeout");
        ldap_connect_pool_timeout.setDefault("120000");
        ldap_connect_pool_timeout.setDoc
            ("Milliseconds of idle time before an idle connection is bumped" +
             " from the pool.");
        
        ldap_cache_account_maxsize = 
            new KnownKey("ldap_cache_account_maxsize", "5000", "Maximum number of account objects to cache");
        
        ldap_cache_account_maxage =
            new KnownKey("ldap_cache_account_maxage", "15", "maximum age (in minutes) of account object in cache");

        ldap_cache_cos_maxsize = 
            new KnownKey("ldap_cache_cos_maxsize", "100", "Maximum number of cos objects to cache");
        
        ldap_cache_cos_maxage = 
            new KnownKey("ldap_cache_cos_maxage", "15", "maximum age (in minutes) of cos object in cache");        

        ldap_cache_domain_maxsize = 
            new KnownKey("ldap_cache_domain_maxsize", "100", "Maximum number of domain objects to cache");
        
        ldap_cache_domain_maxage = 
            new KnownKey("ldap_cache_domain_maxage", "15", "maximum age (in minutes) of domain object in cache");        

        ldap_cache_server_maxsize = 
            new KnownKey("ldap_cache_server_maxsize", "100", "Maximum number of server objects to cache");

        ldap_cache_server_maxage =
            new KnownKey("ldap_cache_server_maxage", "15", "maximum age (in minutes) of group object in cache");        

        ldap_cache_group_maxsize = 
            new KnownKey("ldap_cache_group_maxsize", "500", "Maximum number of group objects to cache");
        
        ldap_cache_group_maxage =
            new KnownKey("ldap_cache_group_maxage", "15", "maximum age (in minutes) of group object in cache");        

        ldap_cache_timezone_maxsize =
            new KnownKey("ldap_cache_timezone_maxsize", "100", "Maximum number of timezone objects to cache");
        
        ldap_cache_zimlet_maxsize =
            new KnownKey("ldap_cache_zimlet_maxsize", "100", "Maximum number of zimlet objects to cache");
        
        ldap_cache_zimlet_maxage = 
            new KnownKey("ldap_cache_zimlet_maxage", "15", "maximum age (in minutes) of zimlet object in cache");        

        mysql_directory = new KnownKey("mysql_directory");
        mysql_directory.setDefault("${zimbra_home}" + FS + "mysql");
        mysql_directory.setDoc
            ("Location of MySQL installation.");

        mysql_data_directory = new KnownKey("mysql_data_directory");
        mysql_data_directory.setDefault("${zimbra_db_directory}" + FS + "data");
        mysql_data_directory.setDoc
            ("Directory in which MySQL data should reside.");

        mysql_socket = new KnownKey("mysql_socket");
        mysql_socket.setDefault("${zimbra_db_directory}" + FS + "mysql.sock");
        mysql_socket.setDoc
            ("Path to MySQL socket for use by MySQL command line tools.");

        mysql_pidfile = new KnownKey("mysql_pidfile");
        mysql_pidfile.setDefault("${zimbra_db_directory}" + FS + "mysql.pid");
        mysql_pidfile.setDoc
            ("File in which MySQL process id is stored.");

        mysql_mycnf = new KnownKey("mysql_mycnf");
        mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.cnf");
        mysql_mycnf.setDoc
            ("Path to my.cnf, the MySQL config file.");

        mysql_bind_address = new KnownKey("mysql_bind_address");
        mysql_bind_address.setDefault("localhost");
        mysql_bind_address.setDoc
            ("Interface on this host to which MySQL will bind.");

        mysql_port = new KnownKey("mysql_port");
        mysql_port.setDefault("7306");
        mysql_port.setDoc
            ("Port number on which MySQL server should listen.");

        mysql_root_password = new KnownKey("mysql_root_password");
        mysql_root_password.setDefault("zimbra");
        mysql_root_password.setForceToEdit(true);
        mysql_root_password.setDoc
            ("Password for MySQL's built-in `root' user, not to be" +
             " confused with the UNIX root login.  As a convenience," +
             " during database initialization a random password is" +
             " generated, saved in local config and in MySQL.  If you" +
             " want to change this password, please use the zmmypasswd" +
             " program which will change the password in both MySQL" +
             " and in local config.");

        mysql_memory_percent = new KnownKey("mysql_memory_percent");
        mysql_memory_percent.setDefault("40");
        mysql_memory_percent.setDoc
            ("Percentage of system memory that mysql should use."
             + ZM_MYCNF_CAVEAT);

        mysql_innodb_log_buffer_size = new KnownKey("mysql_innodb_log_buffer_size");
        mysql_innodb_log_buffer_size.setDefault("8388608");
        mysql_innodb_log_buffer_size.setDoc
            ("Consult MySQL documentation for innodb_log_buffer_size. " +
             ZM_MYCNF_CAVEAT);

        mysql_innodb_log_file_size = new KnownKey("mysql_innodb_log_file_size");
        mysql_innodb_log_file_size.setDefault("104857600");
        mysql_innodb_log_file_size.setDoc
            ("Consult MySQL documentation for innodb_log_file_size. " +
             ZM_MYCNF_CAVEAT);

        mysql_sort_buffer_size = new KnownKey("mysql_sort_buffer_size");
        mysql_sort_buffer_size.setDefault("1048576");
        mysql_sort_buffer_size.setDoc
            ("Consult MySQL documentation for sort_buffer_size. " +
             ZM_MYCNF_CAVEAT);
        
        mysql_read_buffer_size = new KnownKey("mysql_read_buffer_size");
        mysql_read_buffer_size.setDefault("1048576");
        mysql_read_buffer_size.setDoc
            ("Consult MySQL documentation for read_buffer_size. " +
             ZM_MYCNF_CAVEAT);
        
        mysql_table_cache = new KnownKey("mysql_table_cache");
        mysql_table_cache.setDefault("500");
        mysql_table_cache.setDoc
            ("Consult MySQL documentation for table_cache. " + ZM_MYCNF_CAVEAT);
        zimbra_logger_mysql_password = new KnownKey("zimbra_logger_mysql_password");
        zimbra_logger_mysql_password.setDefault("zimbra");
        zimbra_logger_mysql_password.setForceToEdit(true);
        zimbra_logger_mysql_password.setDoc
            ("Password for " + zimbra_mysql_user.key() + ". Stored in" +
             " local config for use by the store application to" +
             " authenticate.  If you want to change this password," +
             " please use the zmmylogpasswd program which will change the" +
             " password in both MySQL and in local config.");

        logger_mysql_directory = new KnownKey("logger_mysql_directory");
        logger_mysql_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "mysql");
        logger_mysql_directory.setDoc
            ("Location of logger MySQL installation.");

        logger_mysql_data_directory = new KnownKey("logger_mysql_data_directory");
        logger_mysql_data_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "data");
        logger_mysql_data_directory.setDoc
            ("Directory in which logger MySQL data should reside.");

        logger_mysql_socket = new KnownKey("logger_mysql_socket");
        logger_mysql_socket.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.sock");
        logger_mysql_socket.setDoc
            ("Path to logger MySQL socket for use by logger MySQL command line tools.");

        logger_mysql_pidfile = new KnownKey("logger_mysql_pidfile");
        logger_mysql_pidfile.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.pid");
        logger_mysql_pidfile.setDoc
            ("File in which logger MySQL process id is stored.");

        logger_mysql_mycnf = new KnownKey("logger_mysql_mycnf");
        logger_mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.logger.cnf");
        logger_mysql_mycnf.setDoc
            ("Path to my.logger.cnf, the logger MySQL config file.");

        logger_mysql_bind_address = new KnownKey("logger_mysql_bind_address");
        logger_mysql_bind_address.setDefault("localhost");
        logger_mysql_bind_address.setDoc
            ("Interface on this host to which logger MySQL will bind.");

        logger_mysql_port = new KnownKey("logger_mysql_port");
        logger_mysql_port.setDefault("7307");
        logger_mysql_port.setDoc
            ("Port number on which logger MySQL server should listen.");

		postfix_alias_maps  = new KnownKey("postfix_alias_maps");
		postfix_alias_maps.setDefault("hash:/etc/aliases");
		postfix_alias_maps.setDoc("postfix_alias_maps");

		postfix_broken_sasl_auth_clients  = new KnownKey("postfix_broken_sasl_auth_clients");
		postfix_broken_sasl_auth_clients.setDefault("yes");
		postfix_broken_sasl_auth_clients.setDoc("postfix_broken_sasl_auth_clients");

		postfix_command_directory  = new KnownKey("postfix_command_directory");
		postfix_command_directory.setDefault("/opt/zimbra/postfix-${postfix_version}/sbin");
		postfix_command_directory.setDoc("postfix_command_directory");

		postfix_daemon_directory  = new KnownKey("postfix_daemon_directory");
		postfix_daemon_directory.setDefault("/opt/zimbra/postfix-${postfix_version}/libexec");
		postfix_daemon_directory.setDoc("postfix_daemon_directory");

		postfix_header_checks  = new KnownKey("postfix_header_checks");
		postfix_header_checks.setDefault("pcre:/opt/zimbra/conf/postfix_header_checks");
		postfix_header_checks.setDoc("postfix_header_checks");

		postfix_mailq_path  = new KnownKey("postfix_mailq_path");
		postfix_mailq_path.setDefault("/opt/zimbra/postfix-${postfix_version}/sbin/mailq");
		postfix_mailq_path.setDoc("postfix_mailq_path");

		postfix_manpage_directory  = new KnownKey("postfix_manpage_directory");
		postfix_manpage_directory.setDefault("/opt/zimbra/postfix-${postfix_version}/man");
		postfix_manpage_directory.setDoc("postfix_manpage_directory");

		postfix_newaliases_path  = new KnownKey("postfix_newaliases_path");
		postfix_newaliases_path.setDefault("/opt/zimbra/postfix-${postfix_version}/sbin/newaliases");
		postfix_newaliases_path.setDoc("postfix_newaliases_path");

		postfix_queue_directory  = new KnownKey("postfix_queue_directory");
		postfix_queue_directory.setDefault("/opt/zimbra/postfix-${postfix_version}/spool");
		postfix_queue_directory.setDoc("postfix_queue_directory");

		postfix_sender_canonical_maps  = new KnownKey("postfix_sender_canonical_maps");
		postfix_sender_canonical_maps.setDefault("ldap:/opt/zimbra/conf/ldap-scm.cf");
		postfix_sender_canonical_maps.setDoc("postfix_sender_canonical_maps");

		postfix_sendmail_path  = new KnownKey("postfix_sendmail_path");
		postfix_sendmail_path.setDefault("/opt/zimbra/postfix-${postfix_version}/sbin/sendmail");
		postfix_sendmail_path.setDoc("postfix_sendmail_path");

		postfix_smtpd_client_restrictions  = new KnownKey("postfix_smtpd_client_restrictions");
		postfix_smtpd_client_restrictions.setDefault("reject_unauth_pipelining");
		postfix_smtpd_client_restrictions.setDoc("postfix_smtpd_client_restrictions");

		postfix_smtpd_data_restrictions  = new KnownKey("postfix_smtpd_data_restrictions");
		postfix_smtpd_data_restrictions.setDefault("reject_unauth_pipelining");
		postfix_smtpd_data_restrictions.setDoc("postfix_smtpd_data_restrictions");

		postfix_smtpd_helo_required  = new KnownKey("postfix_smtpd_helo_required");
		postfix_smtpd_helo_required.setDefault("yes");
		postfix_smtpd_helo_required.setDoc("postfix_smtpd_helo_required");

		postfix_smtpd_tls_cert_file  = new KnownKey("postfix_smtpd_tls_cert_file");
		postfix_smtpd_tls_cert_file.setDefault("${zimbra_home}"+FS+"conf"+FS+"smtpd.crt");
		postfix_smtpd_tls_cert_file.setDoc("postfix_smtpd_tls_cert_file");

		postfix_smtpd_tls_key_file  = new KnownKey("postfix_smtpd_tls_key_file");
		postfix_smtpd_tls_key_file.setDefault("${zimbra_home}"+FS+"conf"+FS+"smtpd.key");
		postfix_smtpd_tls_key_file.setDoc("postfix_smtpd_tls_key_file");

		postfix_smtpd_tls_loglevel  = new KnownKey("postfix_smtpd_tls_loglevel");
		postfix_smtpd_tls_loglevel.setDefault("3");
		postfix_smtpd_tls_loglevel.setDoc("postfix_smtpd_tls_loglevel");

		postfix_transport_maps  = new KnownKey("postfix_transport_maps");
		postfix_transport_maps.setDefault("ldap:/opt/zimbra/conf/ldap-transport.cf");
		postfix_transport_maps.setDoc("postfix_transport_maps");

		postfix_version  = new KnownKey("postfix_version");
		postfix_version.setDefault("2.2.9");
		postfix_version.setDoc("postfix_version");

		postfix_virtual_alias_domains  = new KnownKey("postfix_virtual_alias_domains");
		postfix_virtual_alias_domains.setDefault("ldap://opt/zimbra/conf/ldap-vad.cf");
		postfix_virtual_alias_domains.setDoc("postfix_virtual_alias_domains");

		postfix_virtual_alias_maps  = new KnownKey("postfix_virtual_alias_maps");
		postfix_virtual_alias_maps.setDefault("ldap:/opt/zimbra/conf/ldap-vam.cf");
		postfix_virtual_alias_maps.setDoc("postfix_virtual_alias_maps");

		postfix_virtual_mailbox_domains  = new KnownKey("postfix_virtual_mailbox_domains");
		postfix_virtual_mailbox_domains.setDefault("ldap:/opt/zimbra/conf/ldap-vmd.cf");
		postfix_virtual_mailbox_domains.setDoc("postfix_virtual_mailbox_domains");

		postfix_virtual_mailbox_maps  = new KnownKey("postfix_virtual_mailbox_maps");
		postfix_virtual_mailbox_maps.setDefault("ldap:/opt/zimbra/conf/ldap-vmm.cf");
		postfix_virtual_mailbox_maps.setDoc("postfix_virtual_mailbox_maps");

		postfix_virtual_transport  = new KnownKey("postfix_virtual_transport");
		postfix_virtual_transport.setDefault("error");
		postfix_virtual_transport.setDoc("postfix_virtual_transport");

        tomcat_directory = new KnownKey("tomcat_directory");
        tomcat_directory.setDefault("${zimbra_home}" + FS + "tomcat");
        tomcat_directory.setDoc("Location of tomcat installation.");

        tomcat_java_heap_memory_percent = new KnownKey("tomcat_java_heap_memory_percent");
        tomcat_java_heap_memory_percent.setDefault("30");
        tomcat_java_heap_memory_percent.setDoc
            ("Percentage of system memory that will be used as the" +
             " maximum Java heap size (-Xmx) of the JVM running Tomcat.");
        
        tomcat_java_options = new KnownKey("tomcat_java_options");
        tomcat_java_options.setDefault("-client -XX:NewRatio=2");
        tomcat_java_options.setDoc
            ("JVM options to use when launching Tomcat.");
        
        tomcat_java_home = new KnownKey("tomcat_java_home");
        tomcat_java_home.setDefault("${zimbra_java_home}");
        tomcat_java_home.setDoc
            ("Path to JDK/JRE to use for running Tomcat.");
        
        tomcat_pidfile = new KnownKey("tomcat_pidfile");
        tomcat_pidfile.setDefault("${zimbra_log_directory}" + FS + "tomcat.pid");
        tomcat_pidfile.setDoc
            ("File in which process id of Tomcat JVM is stored.");

        tomcat_keystore = new KnownKey("tomcat_keystore");
        tomcat_keystore.setDefault("${tomcat_directory}" + FS + "conf" + FS + "keystore");
        tomcat_keystore.setDoc
        	("Location of keystore data file.");
        
        ssl_allow_untrusted_certs = new KnownKey("ssl_allow_untrusted_certs");
        ssl_allow_untrusted_certs.setDefault("false");
        ssl_allow_untrusted_certs.setDoc
            ("If true, allow self-signed SSL certificates.");
        
        zimlet_directory = new KnownKey("zimlet_directory");
        zimlet_directory.setDefault("${tomcat_directory}" + FS + "webapps" + FS + "service" + FS + "zimlet");
        zimlet_directory.setDoc
        	("Path to installed Zimlets.");
        
        wiki_enabled = new KnownKey("wiki_enabled");
        wiki_enabled.setDefault("false");
        wiki_enabled.setDoc
            ("Enable wiki app.");
        
        wiki_user = new KnownKey("wiki_user");
        wiki_user.setDefault("wiki");
        wiki_user.setDoc
            ("Wiki user.");
        
        nio_imap_enable = new KnownKey("nio_imap_enable");
        nio_imap_enable.setDefault("false");
        nio_imap_enable.setDoc
            ("Enable NIO based IMAP server.  If false, then the thread per connection IO framework is used.");
        
        nio_imap_debug_logging = new KnownKey("nio_imap_log_buffers");
        nio_imap_debug_logging.setDefault("false");
        nio_imap_debug_logging.setDoc
            ("Log extremely large amounts of detail from the NIO IMAP server framework.  Useful only for debugging the IO framework.");


		zimbra_mtareport_max_recipients = new KnownKey("zimbra_mtareport_max_recipients");
		zimbra_mtareport_max_recipients.setDefault("50");
		zimbra_mtareport_max_recipients.setDoc
			("Number of recipients to list in daily mta reports");

		zimbra_mtareport_max_senders = new KnownKey("zimbra_mtareport_max_senders");
		zimbra_mtareport_max_senders.setDefault("50");
		zimbra_mtareport_max_senders.setDoc
			("Number of senders to list in daily mta reports");

    }

}
