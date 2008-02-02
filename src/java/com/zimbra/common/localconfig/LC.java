/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.localconfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Provides convenient means to get at local configuration - stuff that
 * we do not want in LDAP.
 */
public class LC {

    public static String get(String key) {
        String value;
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
    public static final KnownKey zimbra_extensions_common_directory;
    public static final KnownKey zimbra_mysql_user;
    public static final KnownKey zimbra_mysql_password;
    public static final KnownKey zimbra_ldap_userdn;
    public static final KnownKey zimbra_ldap_user;
    public static final KnownKey zimbra_ldap_password;
    public static final KnownKey zimbra_server_hostname;
    public static final KnownKey zimbra_attrs_directory;
    public static final KnownKey zimbra_user;
    public static final KnownKey zimbra_uid;
    public static final KnownKey zimbra_gid;
    public static final KnownKey zimbra_log4j_properties;
    public static final KnownKey zimbra_auth_always_send_refer;
    public static final KnownKey zimbra_admin_service_port;
    public static final KnownKey zimbra_admin_service_scheme;
    public static final KnownKey zimbra_zmprov_default_to_ldap;
    public static final KnownKey zimbra_zmprov_default_soap_server;
    public static final KnownKey localized_msgs_directory;
    public static final KnownKey localized_client_msgs_directory;
    public static final KnownKey skins_directory;

    public static final KnownKey zimbra_store_sweeper_max_age;

    public static final KnownKey zimbra_mailbox_purgeable;
    public static final KnownKey zimbra_mailbox_active_cache;
    public static final KnownKey zimbra_mailbox_inactive_cache;

    public static final KnownKey zimbra_index_max_uncommitted_operations;
    public static final KnownKey zimbra_index_lru_size;
    public static final KnownKey zimbra_index_idle_flush_time;
    public static final KnownKey zimbra_index_sweep_frequency;
    
    public static final KnownKey zimbra_index_reader_lru_size;
    public static final KnownKey zimbra_index_reader_idle_flush_time;
    public static final KnownKey zimbra_index_reader_idle_sweep_frequency;

    public static final KnownKey zimbra_spam_report_queue_size;

    public static final KnownKey zimbra_throttle_op_concurrency;
    
    public static final KnownKey zimbra_im_chat_flush_time;
    public static final KnownKey zimbra_im_chat_close_time;

    public static final KnownKey zimbra_session_max_pending_notifications;

    public static final KnownKey stats_img_folder;

    public static final KnownKey ldap_host;
    public static final KnownKey ldap_log_level;
    public static final KnownKey ldap_port;
    public static final KnownKey ldap_url;
    public static final KnownKey ldap_master_url;
    public static final KnownKey ldap_is_master;
    public static final KnownKey ldap_root_password;
    public static final KnownKey ldap_connect_timeout;
    public static final KnownKey ldap_read_timeout;
    public static final KnownKey ldap_connect_pool_master;
    public static final KnownKey ldap_connect_pool_debug;
    public static final KnownKey ldap_connect_pool_initsize;
    public static final KnownKey ldap_connect_pool_maxsize;
    public static final KnownKey ldap_connect_pool_prefsize;
    public static final KnownKey ldap_connect_pool_timeout;
    public static final KnownKey ldap_replication_password;
    public static final KnownKey ldap_postfix_password;
    public static final KnownKey ldap_amavis_password;
    public static final KnownKey ldap_require_tls;
    public static final KnownKey ldap_starttls_supported;
    

    public static final KnownKey ldap_cache_account_maxsize;
    public static final KnownKey ldap_cache_account_maxage;
    public static final KnownKey ldap_cache_cos_maxsize;
    public static final KnownKey ldap_cache_cos_maxage;
    public static final KnownKey ldap_cache_domain_maxsize;
    public static final KnownKey ldap_cache_domain_maxage;
    public static final KnownKey ldap_cache_server_maxsize;
    public static final KnownKey ldap_cache_server_maxage;
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

    public static final KnownKey mailboxd_directory;
    public static final KnownKey mailboxd_java_heap_memory_percent;
    public static final KnownKey mailboxd_java_options;
    public static final KnownKey mailboxd_java_home;
    public static final KnownKey mailboxd_pidfile;
    public static final KnownKey mailboxd_keystore;
    public static final KnownKey mailboxd_keystore_password;
    public static final KnownKey mailboxd_truststore;
    public static final KnownKey mailboxd_truststore_password;
    public static final KnownKey mailboxd_output_file;
    public static final KnownKey mailboxd_output_rotate_interval;
    public static final KnownKey mailboxd_thread_stack_size;
    
    public static final KnownKey ssl_allow_untrusted_certs;

    public static final KnownKey zimlet_directory;
    public static final KnownKey wiki_enabled;
    public static final KnownKey wiki_user;

    public static final KnownKey calendar_outlook_compatible_allday_events;
    public static final KnownKey calendar_entourage_compatible_timezones;
    public static final KnownKey calendar_ics_import_full_parse_max_size;

    public static final KnownKey nio_enabled;
    public static final KnownKey nio_debug_enabled;
    public static final KnownKey nio_imap_enabled;
    public static final KnownKey nio_imap_debug_logging;
    public static final KnownKey nio_pop3_enabled;
    public static final KnownKey nio_lmtp_enabled;

    public static final KnownKey krb5_keytab;
    
    public static final KnownKey zimbra_mtareport_max_recipients;
    public static final KnownKey zimbra_mtareport_max_senders;

    public static final KnownKey zimbra_mailbox_groups;
    
    public static final KnownKey debug_mailboxindex_use_new_locking;

    public static final KnownKey zimbra_class_provisioning;
    public static final KnownKey zimbra_class_accessmanager;
    public static final KnownKey zimbra_class_mboxmanager;
    public static final KnownKey zimbra_class_database;
    public static final KnownKey zimbra_class_application;
    
    public static final KnownKey data_source_trust_self_signed_certs;
    public static final KnownKey data_source_fast_fetch;
    public static final KnownKey data_source_fetch_size;

    public static final KnownKey timezone_file;

    public static final KnownKey search_disable_database_hints;
    public static final KnownKey search_dbfirst_term_percentage_cutoff;

    public static final KnownKey zmstat_log_directory;
    public static final KnownKey zmstat_interval;
    
    public static final KnownKey zimbra_noop_default_timeout;
    public static final KnownKey zimbra_noop_min_timeout;
    public static final KnownKey zimbra_noop_max_timeout;

    public static final KnownKey zimbra_waitset_default_request_timeout;
    public static final KnownKey zimbra_waitset_min_request_timeout;
    public static final KnownKey zimbra_waitset_max_request_timeout;
    public static final KnownKey zimbra_waitset_max_per_account;

    public static final KnownKey zimbra_admin_waitset_default_request_timeout;
    public static final KnownKey zimbra_admin_waitset_min_request_timeout;
    public static final KnownKey zimbra_admin_waitset_max_request_timeout;
    
    public static final KnownKey zimbra_waitset_initial_sleep_time;
    public static final KnownKey zimbra_waitset_nodata_sleep_time;
    
    public static final KnownKey zimbra_csv_mapping_file;
    
    public static final KnownKey debug_update_config_use_old_scheme;
    
    public static final KnownKey debug_batch_message_indexing;
    
    public static final KnownKey debug_xmpp_disable_client_tls;
    
    public static final KnownKey im_dnsutil_dnsoverride;
    
    public static final KnownKey javamail_pop3_debug;
    public static final KnownKey javamail_imap_debug;
    public static final KnownKey javamail_smtp_debug;
    
    public static final KnownKey javamail_pop3_timeout;
    public static final KnownKey javamail_imap_timeout;
    public static final KnownKey javamail_smtp_timeout;
    
    static {
        final String ZM_MYCNF_CAVEAT = "This value is stored here for use by zmmycnf program.  " +
                "Changing this setting does not immediately reflect in MySQL server.  " +
                "You will have to, with abundant precaution, re-generate my.cnf and " +
                "restart MySQL server for the change to take effect.";
        final String FS = "/";  // Use Unix-style file separator even on Windows.
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
        zimbra_home.setDoc("Zimbra installation root and home directory of `zimbra'" + 
                    " UNIX user. You can not relocate install root - do" +
                    " not change this setting.");

        zimbra_java_home = new KnownKey("zimbra_java_home");
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            zimbra_java_home.setDefault("/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home");
        } else {
            zimbra_java_home.setDefault("${zimbra_home}" + FS + "java");
        }
        zimbra_java_home.setDoc("Path to a JDK/J2SDK.");

        zimbra_log_directory = new KnownKey("zimbra_log_directory");
        zimbra_log_directory.setDefault("${zimbra_home}" + FS + "log");
        zimbra_log_directory.setDoc("Directory where log files are written.");

        zimbra_index_directory = new KnownKey("zimbra_index_directory");
        zimbra_index_directory.setDefault("${zimbra_home}" + FS + "index");
        zimbra_index_directory.setDoc("Directory for mailbox index files.");

        zimbra_store_directory = new KnownKey("zimbra_store_directory");
        zimbra_store_directory.setDefault("${zimbra_home}" + FS + "store");
        zimbra_store_directory.setDoc("Directory for mailbox messages.");

        zimbra_db_directory = new KnownKey("zimbra_db_directory");
        zimbra_db_directory.setDefault("${zimbra_home}" + FS + "db");
        zimbra_db_directory.setDoc("Directory for database files.");

        zimbra_tmp_directory = new KnownKey("zimbra_tmp_directory");
        zimbra_tmp_directory.setDefault(System.getProperty("java.io.tmpdir") + FS + "zimbra");
        zimbra_tmp_directory.setDoc("Directory for temporary files.");

        zimbra_extensions_directory = new KnownKey("zimbra_extension_directory");
        zimbra_extensions_directory.setDefault("${zimbra_home}" + FS + "lib" + FS + "ext");
        zimbra_extensions_directory.setDoc("Directory whose subdirs contain extensions.");

        zimbra_extensions_common_directory = new KnownKey("zimbra_extension_common_directory");
        zimbra_extensions_common_directory.setDefault("${zimbra_home}" + FS + "lib" + FS + "ext-common");
        zimbra_extensions_common_directory.setDoc("Directory with jar files that are common across all extensions.");

        zimbra_mysql_user = new KnownKey("zimbra_mysql_user");
        zimbra_mysql_user.setDefault("zimbra");
        zimbra_mysql_user.setDoc("MySQL username to use to create/access zimbra databases" +
                    " and tables. This is the value you would supply to" +
                    " the '-u' option of 'mysql' command line program.");

        zimbra_mysql_password = new KnownKey("zimbra_mysql_password");
        zimbra_mysql_password.setDefault("zimbra");
        zimbra_mysql_password.setForceToEdit(true);
        zimbra_mysql_password.setDoc("Password for " + zimbra_mysql_user.key() + ". Stored in" +
                    " local config for use by the store application to" +
                    " authenticate.  If you want to change this password," +
                    " please use the zmmypasswd program which will change the" +
                    " password in both MySQL and in local config.");

        zimbra_ldap_userdn = new KnownKey("zimbra_ldap_userdn");
        zimbra_ldap_userdn.setDefault("uid=zimbra,cn=admins,cn=zimbra");
        zimbra_ldap_userdn.setDoc("LDAP dn used to authenticate the store application with LDAP.");

        zimbra_ldap_user = new KnownKey("zimbra_ldap_user");
        zimbra_ldap_user.setDefault("zimbra");
        zimbra_ldap_user.setDoc("zimbra username used to authenticate with admin AuthRequest.");

        zimbra_ldap_password = new KnownKey("zimbra_ldap_password");
        zimbra_ldap_password.setDefault("zimbra");
        zimbra_ldap_password.setForceToEdit(true);
        zimbra_ldap_password.setDoc("Password for " + zimbra_ldap_userdn.key() + ". Stored in" +
                    " local config for use by the store application to" +
                    " authenticate.  If you want to change this password," +
                    " please use the zmldappasswd program which will  change the" +
                    " password in both LDAP and in local config.");

        zimbra_server_hostname = new KnownKey("zimbra_server_hostname");
        zimbra_server_hostname.setDefault(hostname);
        zimbra_server_hostname.setDoc("The provisioned name of this server. There should exist" +
                    " a corresponding `server' entry in LDAP - consult" +
                    " documentation for CreateServer command of the zmprov program.");

        zimbra_user = new KnownKey("zimbra_user");
        zimbra_user.setDefault("zimbra");
        zimbra_user.setDoc("The zimbra unix user to which the zimbra server process must" +
                    " switch privileges to, after binding privileged ports.");

        zimbra_uid = new KnownKey("zimbra_uid");
        zimbra_uid.setDefault("-1");
        zimbra_uid.setDoc("The zimbra unix uid to which the zimbra server process must" +
                    " switch privileges to, after binding privileged ports.");

        zimbra_gid = new KnownKey("zimbra_gid");
        zimbra_gid.setDefault("-1");
        zimbra_gid.setDoc("The zimbra unix gid to which the zimbra server process must" +
                    " switch privileges to, after binding privileged ports.");

        zimbra_log4j_properties = new KnownKey("zimbra_log4j_properties");
        zimbra_log4j_properties.setDefault("${zimbra_home}" + FS + "conf" + FS + "log4j.properties");
        zimbra_log4j_properties.setDoc("Path to log4j configuration properties file.");

        zimbra_attrs_directory = new KnownKey("zimbra_attrs_directory");
        zimbra_attrs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "attrs");
        zimbra_attrs_directory.setDoc("Directory that contains *.xml files which specify syntax for LDAP attributes used by the system");

        localized_msgs_directory = new KnownKey("localized_msgs_directory");
        localized_msgs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "msgs");
        localized_msgs_directory.setDoc("Directory for localized message files.");

        localized_client_msgs_directory = new KnownKey("localized_client_msgs_directory");
        localized_client_msgs_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "WEB-INF" + FS + "classes" + FS + "messages");
        localized_client_msgs_directory.setDoc("Directory for localized client message files.");

        skins_directory = new KnownKey("skins_directory");
        skins_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "skins");
        skins_directory.setDoc("Directory for skins.");
        
        zimbra_store_sweeper_max_age = new KnownKey("zimbra_store_sweeper_max_age");
        zimbra_store_sweeper_max_age.setDefault("480");  // 480 mins = 8 hours
        zimbra_store_sweeper_max_age.setDoc("Files older than this many minutes are auto-deleted from store incoming directory.");

        zimbra_mailbox_purgeable = new KnownKey("zimbra_mailbox_purgeable");
        zimbra_mailbox_purgeable.setDefault("true");
        zimbra_mailbox_purgeable.setDoc("Whether the mailbox manager should permit inactive mailboxes to be purged from its cache.");

        zimbra_mailbox_active_cache = new KnownKey("zimbra_mailbox_active_cache");
        zimbra_mailbox_active_cache.setDefault("500");
        zimbra_mailbox_active_cache.setDoc("The maximum size of a mailbox's internal LRU item cache when there are sessions active.");

        zimbra_mailbox_inactive_cache = new KnownKey("zimbra_mailbox_inactive_cache");
        zimbra_mailbox_inactive_cache.setDefault("30");
        zimbra_mailbox_inactive_cache.setDoc("The maximum size of a mailbox's internal LRU item cache when it has no active sessions.");

        zimbra_index_max_uncommitted_operations = new KnownKey("zimbra_index_max_uncommitted_operations");
        zimbra_index_max_uncommitted_operations.setDefault("200");
        zimbra_index_max_uncommitted_operations.setDoc("Maximum number of uncommitted indexing operations" +
                    " that may accumulate per mailbox before forcing a commit.");

        zimbra_index_lru_size = new KnownKey("zimbra_index_lru_size");
        zimbra_index_lru_size.setDefault("100");
        zimbra_index_lru_size.setDoc("Maximum number of open mailbox index writers in the LRU map.");

        zimbra_index_idle_flush_time = new KnownKey("zimbra_index_idle_flush_time");
        zimbra_index_idle_flush_time.setDefault("600");
        zimbra_index_idle_flush_time.setDoc("If idle for longer than this value (in seconds), flush" +
                    " uncommitted indexing ops in mailbox.");
        
        zimbra_index_sweep_frequency = new KnownKey("zimbra_index_sweep_frequency", "30", 
              "How often (seconds) do we sweep the Index Writer Cache looking for idle IndexWriters to close");
        
        zimbra_index_reader_lru_size  = new KnownKey("zimbra_index_reader_lru_size");
        zimbra_index_reader_lru_size.setDefault("20");
        zimbra_index_reader_lru_size.setDoc("Maximum number of IndexReaders cached open by the search subsystem");
        
        zimbra_index_reader_idle_flush_time = new KnownKey("zimbra_index_reader_idle_flush_time");
        zimbra_index_reader_idle_flush_time.setDefault("300");
        zimbra_index_reader_idle_flush_time.setDoc("If idle for longer than this value  (seconds) then close the index reader");
        
        zimbra_index_reader_idle_sweep_frequency = new KnownKey("zimbra_index_reader_idle_sweep_frequency");
        zimbra_index_reader_idle_sweep_frequency.setDefault("30");
        zimbra_index_reader_idle_sweep_frequency.setDoc("Frequency (seconds) the index reader LRU is swept for idle readers");

        zimbra_spam_report_queue_size = new KnownKey("zimbra_spam_report_queue_size");
        zimbra_spam_report_queue_size.setDefault("100");
        zimbra_spam_report_queue_size.setDoc("For Junk/Not Junk Msg/ConvActionRequests this queue size limits the" +
                    " the server workqueue for processing the forwards.");

        zimbra_throttle_op_concurrency = new KnownKey("zimbra_throttle_op_concurrency");
        zimbra_throttle_op_concurrency.setDefault("1000,1000,1000,1000,1000");
        zimbra_throttle_op_concurrency.setDoc("Comma-Separated list of concurrency values for each of the 5 priority levels " +
                    "in order from highest priority to lowest priority");
        
        zimbra_im_chat_flush_time = new KnownKey("zimbra_im_chat_flush_time", "300",
             "How frequently (seconds) are open IM chats written to the store");
        zimbra_im_chat_close_time = new KnownKey("zimbra_im_chat_close_time", "3600", 
             "How long (seconds) will the server wait to close idle IM chat sessions");

        zimbra_session_max_pending_notifications = new KnownKey("zimbra_session_max_pending_notifications");
        zimbra_session_max_pending_notifications.setDefault("400");
        zimbra_session_max_pending_notifications.setDoc("Maximum number of pending notifications that can be queued " +
                "in a SOAP session before the queue is purged and the client is told their state is invalid");

        stats_img_folder = new KnownKey("stats_img_folder");
        stats_img_folder.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "work");
        stats_img_folder.setDoc("Directory for storing generated statistics images.");

        ldap_host = new KnownKey("ldap_host");
        ldap_host.setDefault("");
        ldap_host.setDoc("LDAP host to use.  Deprecated - please use ldap_url instead.");

        ldap_log_level = new KnownKey("ldap_log_level");
        ldap_log_level.setDefault("32768");
        ldap_log_level.setDoc("LDAP logging level");

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
        ldap_root_password.setDoc("Password for LDAP slapd.conf rootdn.  As a convenience," +
                    " during LDAP initialization a random password is" +
                    " generated, saved in local config and in slapd.conf.  If you" +
                    " want to change this password, please use the zmldappasswd" +
                    " program which will change the password in both slapd.conf" +
                    " and in local config.");

        ldap_connect_timeout = new KnownKey("ldap_connect_timeout");
        ldap_connect_timeout.setDefault("30000");
        ldap_connect_timeout.setDoc("Milliseconds after which a connection attempt is aborted.");

        ldap_read_timeout = new KnownKey("ldap_read_timeout");
        ldap_read_timeout.setDefault("30000");
        ldap_read_timeout.setDoc("Milliseconds after which a read attempt is aborted.");
        
        ldap_connect_pool_master = new KnownKey("ldap_connect_pool_master");
        ldap_connect_pool_master.setDefault("false");
        ldap_connect_pool_master.setDoc("Whether to use JNDI connection pooling for LDAP master.");
        
        ldap_connect_pool_debug = new KnownKey("ldap_connect_pool_debug");
        ldap_connect_pool_debug.setDefault("false");
        ldap_connect_pool_debug.setDoc("Whether to debug LDAP connection pooling.");

        ldap_connect_pool_initsize = new KnownKey("ldap_connect_pool_initsize");
        ldap_connect_pool_initsize.setDefault("1");
        ldap_connect_pool_initsize.setDoc("Initial number of active LDAP connections to ramp up to.");

        ldap_connect_pool_maxsize = new KnownKey("ldap_connect_pool_maxsize");
        ldap_connect_pool_maxsize.setDefault("50");
        ldap_connect_pool_maxsize.setDoc("Maximum number of LDAP active and idle connections allowed.");

        ldap_connect_pool_prefsize = new KnownKey("ldap_connect_pool_prefsize");
        ldap_connect_pool_prefsize.setDefault("0");
        ldap_connect_pool_prefsize.setDoc("Preferred number  of LDAP connections - setting both maxsize" +
                    " and prefsize to the same value maintains the connection pool" +
                    " at a fixed size.");

        ldap_connect_pool_timeout = new KnownKey("ldap_connect_pool_timeout");
        ldap_connect_pool_timeout.setDefault("120000");
        ldap_connect_pool_timeout.setDoc("Milliseconds of idle time before an idle connection is bumped" +
                    " from the pool.");
        
        ldap_replication_password = new KnownKey("ldap_replication_password");
        ldap_replication_password.setDefault("zmreplica");
        ldap_replication_password.setDoc("Password used by the syncrepl user to authenticate to the ldap master.");
        
        ldap_postfix_password = new KnownKey("ldap_postfix_password");
        ldap_postfix_password.setDefault("zmpostfix");
        ldap_postfix_password.setDoc("Password used by postfix to authenticate to ldap.");

        ldap_amavis_password = new KnownKey("ldap_amavis_password");
        ldap_amavis_password.setDefault("zmamavis");
        ldap_amavis_password.setDoc("Password used by amavis to authenticate to ldap.");
       
	ldap_starttls_supported = new KnownKey("ldap_starttls_supported"); 
        ldap_starttls_supported.setDefault("0");
        ldap_starttls_supported.setDoc("Whether the LDAP server supports the startTLS operation.");

        ldap_require_tls = new KnownKey("ldap_require_tls");
        ldap_require_tls.setDefault("false");
        ldap_require_tls.setDoc("Whether TLS is required for LDAP clients.");

        ldap_cache_account_maxsize = 
            new KnownKey("ldap_cache_account_maxsize", "20000", "Maximum number of account objects to cache.");

        ldap_cache_account_maxage =
            new KnownKey("ldap_cache_account_maxage", "15", "Maximum age (in minutes) of account objects in cache.");

        ldap_cache_cos_maxsize = 
            new KnownKey("ldap_cache_cos_maxsize", "100", "Maximum number of cos objects to cache.");

        ldap_cache_cos_maxage = 
            new KnownKey("ldap_cache_cos_maxage", "15", "Maximum age (in minutes) of cos objects in cache.");        

        ldap_cache_domain_maxsize = 
            new KnownKey("ldap_cache_domain_maxsize", "100", "Maximum number of domain objects to cache");

        ldap_cache_domain_maxage = 
            new KnownKey("ldap_cache_domain_maxage", "15", "Maximum age (in minutes) of domain objects in cache.");        

        ldap_cache_server_maxsize = 
            new KnownKey("ldap_cache_server_maxsize", "100", "Maximum number of server objects to cache");

        ldap_cache_server_maxage =
            new KnownKey("ldap_cache_server_maxage", "15", "Maximum age (in minutes) of group objects in cache.");        

        ldap_cache_timezone_maxsize =
            new KnownKey("ldap_cache_timezone_maxsize", "100", "Maximum number of timezone objects to cache.");

        ldap_cache_zimlet_maxsize =
            new KnownKey("ldap_cache_zimlet_maxsize", "100", "Maximum number of zimlet objects to cache.");

        ldap_cache_zimlet_maxage = 
            new KnownKey("ldap_cache_zimlet_maxage", "15", "Maximum age (in minutes) of zimlet objects in cache.");        

        mysql_directory = new KnownKey("mysql_directory");
        mysql_directory.setDefault("${zimbra_home}" + FS + "mysql");
        mysql_directory.setDoc("Location of MySQL installation.");

        mysql_data_directory = new KnownKey("mysql_data_directory");
        mysql_data_directory.setDefault("${zimbra_db_directory}" + FS + "data");
        mysql_data_directory.setDoc("Directory in which MySQL data should reside.");

        mysql_socket = new KnownKey("mysql_socket");
        mysql_socket.setDefault("${zimbra_db_directory}" + FS + "mysql.sock");
        mysql_socket.setDoc("Path to MySQL socket for use by MySQL command line tools.");

        mysql_pidfile = new KnownKey("mysql_pidfile");
        mysql_pidfile.setDefault("${zimbra_db_directory}" + FS + "mysql.pid");
        mysql_pidfile.setDoc("File in which MySQL process id is stored.");

        mysql_mycnf = new KnownKey("mysql_mycnf");
        mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.cnf");
        mysql_mycnf.setDoc("Path to my.cnf, the MySQL config file.");

        mysql_bind_address = new KnownKey("mysql_bind_address");
        mysql_bind_address.setDefault("localhost");
        mysql_bind_address.setDoc("Interface on this host to which MySQL will bind.");

        mysql_port = new KnownKey("mysql_port");
        mysql_port.setDefault("7306");
        mysql_port.setDoc("Port number on which MySQL server should listen.");

        mysql_root_password = new KnownKey("mysql_root_password");
        mysql_root_password.setDefault("zimbra");
        mysql_root_password.setForceToEdit(true);
        mysql_root_password.setDoc("Password for MySQL's built-in `root' user, not to be" +
                    " confused with the UNIX root login.  As a convenience," +
                    " during database initialization a random password is" +
                    " generated, saved in local config and in MySQL.  If you" +
                    " want to change this password, please use the zmmypasswd" +
                    " program which will change the password in both MySQL" +
                    " and in local config.");

        mysql_memory_percent = new KnownKey("mysql_memory_percent");
        mysql_memory_percent.setDefault("40");
        mysql_memory_percent.setDoc("Percentage of system memory that mysql should use."
                    + ZM_MYCNF_CAVEAT);

        mysql_innodb_log_buffer_size = new KnownKey("mysql_innodb_log_buffer_size");
        mysql_innodb_log_buffer_size.setDefault("8388608");
        mysql_innodb_log_buffer_size.setDoc("Consult MySQL documentation for innodb_log_buffer_size." +
                    ZM_MYCNF_CAVEAT);

        mysql_innodb_log_file_size = new KnownKey("mysql_innodb_log_file_size");
        mysql_innodb_log_file_size.setDefault("104857600");
        mysql_innodb_log_file_size.setDoc("Consult MySQL documentation for innodb_log_file_size." +
                    ZM_MYCNF_CAVEAT);

        mysql_sort_buffer_size = new KnownKey("mysql_sort_buffer_size");
        mysql_sort_buffer_size.setDefault("1048576");
        mysql_sort_buffer_size.setDoc("Consult MySQL documentation for sort_buffer_size." +
                    ZM_MYCNF_CAVEAT);

        mysql_read_buffer_size = new KnownKey("mysql_read_buffer_size");
        mysql_read_buffer_size.setDefault("1048576");
        mysql_read_buffer_size.setDoc("Consult MySQL documentation for read_buffer_size." +
                    ZM_MYCNF_CAVEAT);

        mysql_table_cache = new KnownKey("mysql_table_cache");
        mysql_table_cache.setDefault("500");
        mysql_table_cache.setDoc("Consult MySQL documentation for table_cache. " + ZM_MYCNF_CAVEAT);

        zimbra_logger_mysql_password = new KnownKey("zimbra_logger_mysql_password");
        zimbra_logger_mysql_password.setDefault("zimbra");
        zimbra_logger_mysql_password.setForceToEdit(true);
        zimbra_logger_mysql_password.setDoc("Password for " + zimbra_mysql_user.key() + ". Stored in" +
                    " local config for use by the store application to" +
                    " authenticate.  If you want to change this password," +
                    " please use the zmmylogpasswd program which will change the" +
                    " password in both MySQL and in local config.");

        logger_mysql_directory = new KnownKey("logger_mysql_directory");
        logger_mysql_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "mysql");
        logger_mysql_directory.setDoc("Location of logger MySQL installation.");

        logger_mysql_data_directory = new KnownKey("logger_mysql_data_directory");
        logger_mysql_data_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "data");
        logger_mysql_data_directory.setDoc("Directory in which logger MySQL data should reside.");

        logger_mysql_socket = new KnownKey("logger_mysql_socket");
        logger_mysql_socket.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.sock");
        logger_mysql_socket.setDoc("Path to logger MySQL socket for use by logger MySQL command line tools.");

        logger_mysql_pidfile = new KnownKey("logger_mysql_pidfile");
        logger_mysql_pidfile.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.pid");
        logger_mysql_pidfile.setDoc("File in which logger MySQL process id is stored.");

        logger_mysql_mycnf = new KnownKey("logger_mysql_mycnf");
        logger_mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.logger.cnf");
        logger_mysql_mycnf.setDoc("Path to my.logger.cnf, the logger MySQL config file.");

        logger_mysql_bind_address = new KnownKey("logger_mysql_bind_address");
        logger_mysql_bind_address.setDefault("localhost");
        logger_mysql_bind_address.setDoc("Interface on this host to which logger MySQL will bind.");

        logger_mysql_port = new KnownKey("logger_mysql_port");
        logger_mysql_port.setDefault("7307");
        logger_mysql_port.setDoc("Port number on which logger MySQL server should listen.");

        postfix_alias_maps  = new KnownKey("postfix_alias_maps");
        postfix_alias_maps.setDefault("hash:" + FS + "etc" + FS + "aliases");
        postfix_alias_maps.setDoc("postfix_alias_maps");

        postfix_broken_sasl_auth_clients  = new KnownKey("postfix_broken_sasl_auth_clients");
        postfix_broken_sasl_auth_clients.setDefault("yes");
        postfix_broken_sasl_auth_clients.setDoc("postfix_broken_sasl_auth_clients");

        postfix_command_directory  = new KnownKey("postfix_command_directory");
        postfix_command_directory.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "sbin");
        postfix_command_directory.setDoc("postfix_command_directory");

        postfix_daemon_directory  = new KnownKey("postfix_daemon_directory");
        postfix_daemon_directory.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "libexec");
        postfix_daemon_directory.setDoc("postfix_daemon_directory");

        postfix_header_checks  = new KnownKey("postfix_header_checks");
        postfix_header_checks.setDefault("pcre:${zimbra_home}" + FS + "conf" + FS + "postfix_header_checks");
        postfix_header_checks.setDoc("postfix_header_checks");

        postfix_mailq_path  = new KnownKey("postfix_mailq_path");
        postfix_mailq_path.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "sbin" + FS + "mailq");
        postfix_mailq_path.setDoc("postfix_mailq_path");

        postfix_manpage_directory  = new KnownKey("postfix_manpage_directory");
        postfix_manpage_directory.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "man");
        postfix_manpage_directory.setDoc("postfix_manpage_directory");

        postfix_newaliases_path  = new KnownKey("postfix_newaliases_path");
        postfix_newaliases_path.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "sbin" + FS + "newaliases");
        postfix_newaliases_path.setDoc("postfix_newaliases_path");

        postfix_queue_directory  = new KnownKey("postfix_queue_directory");
        postfix_queue_directory.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "spool");
        postfix_queue_directory.setDoc("postfix_queue_directory");

        postfix_sender_canonical_maps  = new KnownKey("postfix_sender_canonical_maps");
        postfix_sender_canonical_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-scm.cf");
        postfix_sender_canonical_maps.setDoc("postfix_sender_canonical_maps");

        postfix_sendmail_path  = new KnownKey("postfix_sendmail_path");
        postfix_sendmail_path.setDefault("${zimbra_home}" + FS + "postfix-${postfix_version}" + FS + "sbin" + FS + "sendmail");
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
        postfix_smtpd_tls_loglevel.setDefault("1");
        postfix_smtpd_tls_loglevel.setDoc("postfix_smtpd_tls_loglevel");

        postfix_transport_maps  = new KnownKey("postfix_transport_maps");
        postfix_transport_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-transport.cf");
        postfix_transport_maps.setDoc("postfix_transport_maps");

        postfix_version  = new KnownKey("postfix_version");
        postfix_version.setDefault("2.4.3.4z");
        postfix_version.setDoc("postfix_version");

        postfix_virtual_alias_domains  = new KnownKey("postfix_virtual_alias_domains");
        postfix_virtual_alias_domains.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vad.cf");
        postfix_virtual_alias_domains.setDoc("postfix_virtual_alias_domains");

        postfix_virtual_alias_maps  = new KnownKey("postfix_virtual_alias_maps");
        postfix_virtual_alias_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vam.cf");
        postfix_virtual_alias_maps.setDoc("postfix_virtual_alias_maps");

        postfix_virtual_mailbox_domains  = new KnownKey("postfix_virtual_mailbox_domains");
        postfix_virtual_mailbox_domains.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmd.cf");
        postfix_virtual_mailbox_domains.setDoc("postfix_virtual_mailbox_domains");

        postfix_virtual_mailbox_maps  = new KnownKey("postfix_virtual_mailbox_maps");
        postfix_virtual_mailbox_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmm.cf");
        postfix_virtual_mailbox_maps.setDoc("postfix_virtual_mailbox_maps");

        postfix_virtual_transport  = new KnownKey("postfix_virtual_transport");
        postfix_virtual_transport.setDefault("error");
        postfix_virtual_transport.setDoc("postfix_virtual_transport");

        mailboxd_directory = new KnownKey("mailboxd_directory");
        mailboxd_directory.setDefault("${zimbra_home}" + FS + "mailboxd");
        mailboxd_directory.setDoc("Location of mailboxd installation.");

        mailboxd_java_heap_memory_percent = new KnownKey("mailboxd_java_heap_memory_percent");
        mailboxd_java_heap_memory_percent.setDefault("30");
        mailboxd_java_heap_memory_percent.setDoc("Percentage of system memory that will be used as the" +
                    " maximum Java heap size (-Xmx) of the JVM running Mailboxd.");

        mailboxd_java_options = new KnownKey("mailboxd_java_options");
        mailboxd_java_options.setDefault("-client -XX:NewRatio=2 -Djava.awt.headless=true -XX:MaxPermSize=128m -XX:SoftRefLRUPolicyMSPerMB=1");
        mailboxd_java_options.setDoc("JVM options to use when launching Mailboxd.");

        mailboxd_java_home = new KnownKey("mailboxd_java_home");
        mailboxd_java_home.setDefault("${zimbra_java_home}");
        mailboxd_java_home.setDoc("Path to JDK/JRE to use for running Mailboxd.");

        mailboxd_pidfile = new KnownKey("mailboxd_pidfile");
        mailboxd_pidfile.setDefault("${zimbra_log_directory}" + FS + "mailboxd.pid");
        mailboxd_pidfile.setDoc("File in which process id of Mailboxd JVM is stored.");

        mailboxd_keystore = new KnownKey("mailboxd_keystore");
        mailboxd_keystore.setDefault("${mailboxd_directory}" + FS + "etc" + FS + "keystore");
        mailboxd_keystore.setDoc("Location of keystore data file.");
        
        mailboxd_keystore_password = new KnownKey("mailboxd_keystore_password");
        mailboxd_keystore_password.setDefault("zimbra");
        mailboxd_keystore_password.setDoc("Password to be used with the KeyManager keystore.");
        
        mailboxd_truststore = new KnownKey("mailboxd_truststore");
        mailboxd_truststore.setDefault("${zimbra_java_home}" + FS + "lib" + FS + "security" + FS + "cacerts");
        mailboxd_truststore.setDoc("Location of truststore data file.");
        
        mailboxd_truststore_password = new KnownKey("mailboxd_truststore_password");
        mailboxd_truststore_password.setDefault("changeit");
        mailboxd_truststore_password.setDoc("Password to be used with the TrustManager keystore.");

        mailboxd_output_file = new KnownKey("mailboxd_output_file");
        mailboxd_output_file.setDefault("${zimbra_log_directory}" + FS + "zmmailboxd.out");
        mailboxd_output_file.setDoc("File to redirect stdout and stderr of server to.");
        
        mailboxd_output_rotate_interval = new KnownKey("mailboxd_output_rotate_interval");
        mailboxd_output_rotate_interval.setDefault("86400");
        mailboxd_output_rotate_interval.setDoc("Period, in seconds, at which mailboxd output file is rotated.  If <= 0, no rotation is performed.");
      
        mailboxd_thread_stack_size = new KnownKey("mailboxd_thread_stack_size");
        mailboxd_thread_stack_size.setDefault("256k");
        mailboxd_thread_stack_size.setDoc("Thread stack size for Mailboxd");

        ssl_allow_untrusted_certs = new KnownKey("ssl_allow_untrusted_certs");
        ssl_allow_untrusted_certs.setDefault("false");
        ssl_allow_untrusted_certs.setDoc("If true, allow self-signed SSL certificates.");

        zimlet_directory = new KnownKey("zimlet_directory");
        zimlet_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "service" + FS + "zimlet");
        zimlet_directory.setDoc("Path to installed Zimlets.");

        wiki_enabled = new KnownKey("wiki_enabled");
        wiki_enabled.setDefault("false");
        wiki_enabled.setDoc("Enable wiki app.");

        wiki_user = new KnownKey("wiki_user");
        wiki_user.setDefault("wiki");
        wiki_user.setDoc("Wiki user.");

        calendar_outlook_compatible_allday_events = new KnownKey("calendar_outlook_compatible_allday_events");
        calendar_outlook_compatible_allday_events.setDefault("true");
        calendar_outlook_compatible_allday_events.setDoc("Use Outlook-compatible all-day events.  True by default.");

        calendar_entourage_compatible_timezones = new KnownKey("calendar_entourage_compatible_timezones");
        calendar_entourage_compatible_timezones.setDefault("true");
        calendar_entourage_compatible_timezones.setDoc(
                "Quote TZID parameter in iCalendar properties, to workaround bug in MS Entourage.  True by default.");

        calendar_ics_import_full_parse_max_size = new KnownKey("calendar_ics_import_full_parse_max_size");
        calendar_ics_import_full_parse_max_size.setDefault("131072");  // 128KB
        calendar_ics_import_full_parse_max_size.setDoc(
                "During ics import use full parser if ics size is less than or equal to this; " +
                "larger ics files are parsed with callback parser which doesn't allow forward references to VTIMEZONE TZID");

        nio_imap_enabled = new KnownKey("nio_imap_enabled");
        nio_imap_enabled.setDefault("false");
        nio_imap_enabled.setDoc("Enable NIO based IMAP server.  If false, then the thread per connection IO framework is used.");

        nio_imap_debug_logging = new KnownKey("nio_imap_log_buffers");
        nio_imap_debug_logging.setDefault("false");
        nio_imap_debug_logging.setDoc("Log extremely large amounts of detail from the NIO IMAP server framework.  " +
                    "Useful only for debugging the IO framework.");

        nio_pop3_enabled = new KnownKey("nio_pop3_enabled");
        nio_pop3_enabled.setDefault("false");
        nio_pop3_enabled.setDoc("Enable NIO based POP3 server. If false, then the thread per connection IO framework is used.");

        nio_lmtp_enabled = new KnownKey("nio_lmtp_enabled");
        nio_lmtp_enabled.setDefault("false");
        nio_lmtp_enabled.setDoc("Enable NIO based LMTP server. If false, then the thread per connection IO framework is used.");

        nio_enabled = new KnownKey("nio_enabled");
        nio_enabled.setDefault("false");
        nio_enabled.setDoc("Enable NIO-based IMAP/POP3/LMTP servers. If false, then the thread per connection IO framework is used.");

        nio_debug_enabled = new KnownKey("nio_debug_enabled");
        nio_debug_enabled.setDefault("false");
        nio_debug_enabled.setDoc("Enable debug logging for NIO-based IMAP/POP3/LMTP servers.");

        krb5_keytab = new KnownKey("krb5_keytab");
        krb5_keytab.setDefault("${zimbra_home}" + FS + "conf" + FS + "krb5.keytab");
        krb5_keytab.setDoc("Zimbra Kerberos 5 keytab file location");

        zimbra_mtareport_max_recipients = new KnownKey("zimbra_mtareport_max_recipients");
        zimbra_mtareport_max_recipients.setDefault("50");
        zimbra_mtareport_max_recipients.setDoc("Number of recipients to list in daily mta reports.");

        zimbra_mtareport_max_senders = new KnownKey("zimbra_mtareport_max_senders");
        zimbra_mtareport_max_senders.setDefault("50");
        zimbra_mtareport_max_senders.setDoc("Number of senders to list in daily mta reports.");

        zimbra_auth_always_send_refer = new KnownKey("zimbra_auth_always_send_refer");
        zimbra_auth_always_send_refer.setDefault("false");
        zimbra_auth_always_send_refer.setDoc("Always send back a <refer> tag in an auth response to force a client redirect.");

        zimbra_admin_service_port = 
            new KnownKey("zimbra_admin_service_port", "7071", "Default/bootstrap admin port.");

        zimbra_admin_service_scheme = 
            new KnownKey("zimbra_admin_service_scheme", "https://", "Default/bootstrap admin port.");

        zimbra_zmprov_default_to_ldap =
            new KnownKey("zimbra_zmprov_default_to_ldap", "false", "Whether zmprov defaults to LDAP or SOAP.");            

        zimbra_zmprov_default_soap_server =
            new KnownKey("zimbra_zmprov_default_soap_server", "localhost", "Default soap server for zmprov to connect to");

        zimbra_mailbox_groups = new KnownKey("zimbra_mailbox_groups");
        zimbra_mailbox_groups.setDefault("100");
        zimbra_mailbox_groups.setDoc("Number of mailbox groups to distribute new mailboxes to.");
        
        debug_mailboxindex_use_new_locking = new KnownKey("debug_mailboxindex_use_new_locking");
        debug_mailboxindex_use_new_locking.setDefault("true");
        debug_mailboxindex_use_new_locking.setDoc("Use new-style locking for MailboxIndex");

        zimbra_class_provisioning =
            new KnownKey("zimbra_class_provisioning", "com.zimbra.cs.account.ldap.LdapProvisioning", "Provisioning interface class");
        zimbra_class_accessmanager =
            new KnownKey("zimbra_class_accessmanager", "com.zimbra.cs.account.DomainAccessManager", "Access manager interface class");
        zimbra_class_mboxmanager =
            new KnownKey("zimbra_class_mboxmanager", "com.zimbra.cs.mailbox.MailboxManager", "Mailbox manager interface class");
        zimbra_class_database =
            new KnownKey("zimbra_class_database", "com.zimbra.cs.db.MySQL", "Database configuration class");
        zimbra_class_application =
        	new KnownKey("zimbra_class_application", "com.zimbra.cs.util.ZimbraApplication", "Zimbra application interface class");
        
        
        data_source_trust_self_signed_certs =
            new KnownKey("data_source_trust_self_signed_certs", "false",
            "Allow self-signed certificates when connecting to a data source over SSL.");
        data_source_fast_fetch =
            new KnownKey("data_source_fast_fetch", "true", "Enable faster downloads in imap folder import (EXPERIMENTAL)");
        data_source_fetch_size =
            new KnownKey("data_source_fetch_size", "100", "maximum number of imap messages to FETCH in each request (EXPERIMENTAL)");
        
        timezone_file = new KnownKey("timezone_file");
        timezone_file.setDefault("${zimbra_home}" + FS + "conf" + FS + "timezones.ics");
        timezone_file.setDoc("iCalendar file listing well-known time zones");

        search_disable_database_hints = new KnownKey("search_disable_database_hints");
        search_disable_database_hints.setDefault("false");
        search_disable_database_hints.setDoc
        ("If true, do not use database hints in queries generated during search");
        
        search_dbfirst_term_percentage_cutoff = new KnownKey("search_dbfirst_term_percentage_cutoff");
        search_dbfirst_term_percentage_cutoff.setDefault("0.8");
        search_dbfirst_term_percentage_cutoff.setDoc("Internal Query Generation parameter");

        zmstat_log_directory = new KnownKey("zmstat_log_directory");
        zmstat_log_directory.setDefault("${zimbra_home}" + FS + "zmstat");
        zmstat_log_directory.setDoc("where zmstat csv files are saved");

        zmstat_interval = new KnownKey("zmstat_interval");
        zmstat_interval.setDefault("30");
        zmstat_interval.setDoc("how often samples are taken by zmstat (seconds)");
        
        zimbra_noop_default_timeout = new KnownKey("zimbra_noop_default_timeout", "300", 
        "Time (seconds) the server will allow a NoOpRequest to block if wait=1 is specified by the client");  
        zimbra_noop_min_timeout = new KnownKey("zimbra_noop_min_timeout", "30", 
        "Minimum allowable timeout (seconds) specified to NoOpRequest");  
        zimbra_noop_max_timeout = new KnownKey("zimbra_noop_max_timeout", "1200", 
        "Maximum allowable timeout (seconds) specified to NoOpRequest");
        
        zimbra_waitset_default_request_timeout = new KnownKey("zimbra_waitset_default_request_timeout", "300",
        "Default Timeout (seconds) a non-admin WaitSetRequest will block");
        zimbra_waitset_min_request_timeout = new KnownKey("zimbra_waitset_min_request_timeout", "30",
        "Minimum Timeout (seconds) a non-admin WaitSetRequest will block");
        zimbra_waitset_max_request_timeout = new KnownKey("zimbra_waitset_max_request_timeout", "1200",
        "Maximum Timeout (seconds) a non-admin WaitSetRequest will block");
        zimbra_waitset_max_per_account = new KnownKey("zimbra_waitset_max_per_account", "5",
        "Maximum number of non-admin WaitSets a single account may have open");
        
        zimbra_admin_waitset_default_request_timeout = new KnownKey("zimbra_admin_waitset_default_request_timeout", "300",
        "Default Timeout (seconds) an admin WaitSetRequest will block");
        zimbra_admin_waitset_min_request_timeout = new KnownKey("zimbra_admin_waitset_min_request_timeout", "0", 
        "Minimum Timeout (seconds) an admin WaitSetRequest will block");
        zimbra_admin_waitset_max_request_timeout = new KnownKey("zimbra_admin_waitset_max_request_timeout", "3600",
        "Maximum Timeout (seconds) an admin WaitSetRequest will block");
        
        zimbra_waitset_initial_sleep_time  = new KnownKey("zimbra_waitset_initial_sleep_time", "1", 
            "Initial timeout (seconds) to wait before processing any WaitSetRequest");
        zimbra_waitset_nodata_sleep_time = new KnownKey("zimbra_waitset_nodata_sleep_time", "3",
        "Time (seconds) to sleep handling a WaitSetRequest if there is no data after initial check");

        zimbra_csv_mapping_file = new KnownKey("zimbra_csv_mapping_file");
        zimbra_csv_mapping_file.setDefault("${zimbra_home}" + FS + "conf" + FS + "zimbra-contact-fields.xml");
        zimbra_csv_mapping_file.setDoc("Contact field mapping for CSV import and export");
        
        debug_update_config_use_old_scheme = new KnownKey("debug_update_config_use_old_scheme");
        debug_update_config_use_old_scheme.setDefault("false");
        debug_update_config_use_old_scheme.setDoc("If true, DbMailbox.updateConfig() will do DELETE/INSERT instead of UPDATE.");
        
        debug_batch_message_indexing = new KnownKey("debug_batch_message_indexing", "0", "debug only test code to batch index updates (don't index immediately)");
        
        debug_xmpp_disable_client_tls = new KnownKey("debug_xmpp_disable_client_tls", "0", "disable TLS for XMPP C2S protocol");
        
        im_dnsutil_dnsoverride = new KnownKey("im_dnsutil_dnsoverride", "", 
                  "DNS override settings for IM federation, in the format '{domain,host:port},{domain,host:port},...'");
        
        javamail_pop3_debug = new KnownKey("javamail_pop3_debug");
        javamail_pop3_debug.setDefault("false");
        javamail_pop3_debug.setDoc("Whether to enable javamail debug for POP3.");
        
        javamail_imap_debug = new KnownKey("javamail_imap_debug");
        javamail_imap_debug.setDefault("false");
        javamail_imap_debug.setDoc("Whether to enable javamail debug for IMAP.");
        
        javamail_smtp_debug = new KnownKey("javamail_smtp_debug");
        javamail_smtp_debug.setDefault("false");
        javamail_smtp_debug.setDoc("Whether to enable javamail debug for SMTP.");
        
        javamail_pop3_timeout = new KnownKey("javamail_pop3_timeout");
        javamail_pop3_timeout.setDefault("60");
        javamail_pop3_timeout.setDoc("POP3 timeout in seconds.");
        
        javamail_imap_timeout = new KnownKey("javamail_imap_timeout");
        javamail_imap_timeout.setDefault("60");
        javamail_imap_timeout.setDoc("IMAP timeout in seconds.");
        
        javamail_smtp_timeout = new KnownKey("javamail_smtp_timeout");
        javamail_smtp_timeout.setDefault("60");
        javamail_smtp_timeout.setDoc("SMTP timeout in seconds.");
    }
}
