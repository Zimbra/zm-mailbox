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
    public static final KnownKey zimbra_require_interprocess_security;
    public static final KnownKey localized_msgs_directory;
    public static final KnownKey localized_client_msgs_directory;
    public static final KnownKey skins_directory;

    public static final KnownKey zimbra_store_sweeper_max_age;
    public static final KnownKey zimbra_store_copy_buffer_size_kb;

    public static final KnownKey zimbra_mailbox_manager_hardref_cache;
    public static final KnownKey zimbra_mailbox_active_cache;
    public static final KnownKey zimbra_mailbox_inactive_cache;

    public static final KnownKey zimbra_index_max_uncommitted_operations;
    public static final KnownKey zimbra_index_lru_size;
    public static final KnownKey zimbra_index_idle_flush_time;
    public static final KnownKey zimbra_index_sweep_frequency;
    
    public static final KnownKey zimbra_index_reader_lru_size;
    public static final KnownKey zimbra_index_reader_idle_flush_time;
    public static final KnownKey zimbra_index_reader_idle_sweep_frequency;
    public static final KnownKey zimbra_index_deferred_items_delay;
    public static final KnownKey zimbra_index_deferred_items_failure_delay;

    public static final KnownKey zimbra_index_max_transaction_bytes;
    public static final KnownKey zimbra_index_max_transaction_items;
    public static final KnownKey zimbra_index_lucene_autocommit;
    public static final KnownKey zimbra_index_use_nfs_deletion_policy;
    public static final KnownKey zimbra_index_use_reader_reopen;
    
    public static final KnownKey zimbra_index_lucene_batch_use_doc_scheduler;
    public static final KnownKey zimbra_index_lucene_batch_min_merge;
    public static final KnownKey zimbra_index_lucene_batch_max_merge;
    public static final KnownKey zimbra_index_lucene_batch_merge_factor;
    public static final KnownKey zimbra_index_lucene_batch_use_compound_file;
    public static final KnownKey zimbra_index_lucene_batch_use_serial_merge_scheduler;
    public static final KnownKey zimbra_index_lucene_batch_max_buffered_docs;
    public static final KnownKey zimbra_index_lucene_batch_ram_buffer_size_kb;
    public static final KnownKey zimbra_index_lucene_batch_autocommit;
    
    public static final KnownKey zimbra_index_lucene_nobatch_use_doc_scheduler;
    public static final KnownKey zimbra_index_lucene_nobatch_min_merge;
    public static final KnownKey zimbra_index_lucene_nobatch_max_merge;
    public static final KnownKey zimbra_index_lucene_nobatch_merge_factor;
    public static final KnownKey zimbra_index_lucene_nobatch_use_compound_file;
    public static final KnownKey zimbra_index_lucene_nobatch_use_serial_merge_scheduler;
    public static final KnownKey zimbra_index_lucene_nobatch_max_buffered_docs;
    public static final KnownKey zimbra_index_lucene_nobatch_ram_buffer_size_kb;
    public static final KnownKey zimbra_index_lucene_nobatch_autocommit;
    
    public static final KnownKey zimbra_spam_report_queue_size;

    public static final KnownKey zimbra_throttle_op_concurrency;
    
    public static final KnownKey zimbra_im_chat_flush_time;
    public static final KnownKey zimbra_im_chat_close_time;

    public static final KnownKey zimbra_session_limit_imap;
    public static final KnownKey zimbra_session_timeout_soap;
    public static final KnownKey zimbra_session_max_pending_notifications;

    public static final KnownKey zimbra_converter_enabled_uuencode;
    public static final KnownKey zimbra_converter_enabled_tnef;

    public static final KnownKey zimbra_ssl_enabled;
    
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
    public static final KnownKey ldap_deref_aliases;
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
    public static final KnownKey postfix_in_flow_delay;
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
    public static final KnownKey calendar_canonical_tzid;

    public static final KnownKey calendar_cache_enabled;
    public static final KnownKey calendar_cache_directory;
    public static final KnownKey calendar_cache_lru_size;
    public static final KnownKey calendar_cache_range_month_from;
    public static final KnownKey calendar_cache_range_months;

    public static final KnownKey nio_enabled;
    public static final KnownKey nio_debug_enabled;
    public static final KnownKey nio_imap_enabled;
    public static final KnownKey nio_imap_debug_logging;
    public static final KnownKey nio_pop3_enabled;
    public static final KnownKey nio_lmtp_enabled;

    public static final KnownKey krb5_keytab;
    public static final KnownKey krb5_debug_enabled;
    
    public static final KnownKey zimbra_mtareport_max_recipients;
    public static final KnownKey zimbra_mtareport_max_senders;

    public static final KnownKey zimbra_mailbox_groups;
    
    public static final KnownKey debug_mailboxindex_use_new_locking;

    public static final KnownKey zimbra_class_provisioning;
    public static final KnownKey zimbra_class_accessmanager;
    public static final KnownKey zimbra_class_mboxmanager;
    public static final KnownKey zimbra_class_database;
    public static final KnownKey zimbra_class_application;
    public static final KnownKey zimbra_class_rulerewriterfactory;
    
    public static final KnownKey data_source_trust_self_signed_certs;
    public static final KnownKey data_source_fetch_size;
    public static final KnownKey data_source_max_message_memory_size;

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
    
    public static final KnownKey zimbra_auth_provider;
    
    public static final KnownKey debug_update_config_use_old_scheme;
    
    public static final KnownKey debug_xmpp_disable_client_tls;
    
    public static final KnownKey im_dnsutil_dnsoverride;
    
    public static final KnownKey javamail_pop3_debug;
    public static final KnownKey javamail_imap_debug;
    public static final KnownKey javamail_smtp_debug;
    
    public static final KnownKey javamail_pop3_timeout;
    public static final KnownKey javamail_imap_timeout;
    public static final KnownKey javamail_smtp_timeout;
    
    public static final KnownKey javamail_pop3_enable_starttls;
    public static final KnownKey javamail_imap_enable_starttls;
    public static final KnownKey javamail_smtp_enable_starttls;
    
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

        zimbra_java_home = new KnownKey("zimbra_java_home");
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            zimbra_java_home.setDefault("/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home");
        } else {
            zimbra_java_home.setDefault("${zimbra_home}" + FS + "java");
        }

        zimbra_log_directory = new KnownKey("zimbra_log_directory");
        zimbra_log_directory.setDefault("${zimbra_home}" + FS + "log");

        zimbra_index_directory = new KnownKey("zimbra_index_directory");
        zimbra_index_directory.setDefault("${zimbra_home}" + FS + "index");

        zimbra_store_directory = new KnownKey("zimbra_store_directory");
        zimbra_store_directory.setDefault("${zimbra_home}" + FS + "store");

        zimbra_db_directory = new KnownKey("zimbra_db_directory");
        zimbra_db_directory.setDefault("${zimbra_home}" + FS + "db");

        zimbra_tmp_directory = new KnownKey("zimbra_tmp_directory");
        zimbra_tmp_directory.setDefault(System.getProperty("java.io.tmpdir") + FS + "zimbra");

        zimbra_extensions_directory = new KnownKey("zimbra_extension_directory");
        zimbra_extensions_directory.setDefault("${zimbra_home}" + FS + "lib" + FS + "ext");

        zimbra_extensions_common_directory = new KnownKey("zimbra_extension_common_directory");
        zimbra_extensions_common_directory.setDefault("${zimbra_home}" + FS + "lib" + FS + "ext-common");

        zimbra_mysql_user = new KnownKey("zimbra_mysql_user");
        zimbra_mysql_user.setDefault("zimbra");

        zimbra_mysql_password = new KnownKey("zimbra_mysql_password");
        zimbra_mysql_password.setDefault("zimbra");
        zimbra_mysql_password.setForceToEdit(true);

        zimbra_ldap_userdn = new KnownKey("zimbra_ldap_userdn");
        zimbra_ldap_userdn.setDefault("uid=zimbra,cn=admins,cn=zimbra");

        zimbra_ldap_user = new KnownKey("zimbra_ldap_user");
        zimbra_ldap_user.setDefault("zimbra");

        zimbra_ldap_password = new KnownKey("zimbra_ldap_password");
        zimbra_ldap_password.setDefault("zimbra");
        zimbra_ldap_password.setForceToEdit(true);

        zimbra_server_hostname = new KnownKey("zimbra_server_hostname");
        zimbra_server_hostname.setDefault(hostname);

        zimbra_user = new KnownKey("zimbra_user");
        zimbra_user.setDefault("zimbra");

        zimbra_uid = new KnownKey("zimbra_uid");
        zimbra_uid.setDefault("-1");

        zimbra_gid = new KnownKey("zimbra_gid");
        zimbra_gid.setDefault("-1");

        zimbra_log4j_properties = new KnownKey("zimbra_log4j_properties");
        zimbra_log4j_properties.setDefault("${zimbra_home}" + FS + "conf" + FS + "log4j.properties");

        zimbra_attrs_directory = new KnownKey("zimbra_attrs_directory");
        zimbra_attrs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "attrs");

        localized_msgs_directory = new KnownKey("localized_msgs_directory");
        localized_msgs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "msgs");

        localized_client_msgs_directory = new KnownKey("localized_client_msgs_directory");
        localized_client_msgs_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "WEB-INF" + FS + "classes" + FS + "messages");

        skins_directory = new KnownKey("skins_directory");
        skins_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "skins");

        zimbra_store_sweeper_max_age = new KnownKey("zimbra_store_sweeper_max_age");
        zimbra_store_sweeper_max_age.setDefault("480");  // 480 mins = 8 hours

        zimbra_store_copy_buffer_size_kb = new KnownKey("zimbra_store_copy_buffer_size_kb");
        zimbra_store_copy_buffer_size_kb.setDefault("32");  // 32KB

        zimbra_mailbox_manager_hardref_cache = new KnownKey("zimbra_mailbox_manager_hardref_cache");
        zimbra_mailbox_manager_hardref_cache.setDefault("2500");

        zimbra_mailbox_active_cache = new KnownKey("zimbra_mailbox_active_cache");
        zimbra_mailbox_active_cache.setDefault("500");

        zimbra_mailbox_inactive_cache = new KnownKey("zimbra_mailbox_inactive_cache");
        zimbra_mailbox_inactive_cache.setDefault("30");

        zimbra_index_max_uncommitted_operations = new KnownKey("zimbra_index_max_uncommitted_operations");
        zimbra_index_max_uncommitted_operations.setDefault("200");

        zimbra_index_lru_size = new KnownKey("zimbra_index_lru_size");
        zimbra_index_lru_size.setDefault("100");

        zimbra_index_idle_flush_time = new KnownKey("zimbra_index_idle_flush_time");
        zimbra_index_idle_flush_time.setDefault("600");

        zimbra_index_sweep_frequency = new KnownKey("zimbra_index_sweep_frequency", "30");

        zimbra_index_reader_lru_size  = new KnownKey("zimbra_index_reader_lru_size");
        zimbra_index_reader_lru_size.setDefault("20");

        zimbra_index_reader_idle_flush_time = new KnownKey("zimbra_index_reader_idle_flush_time");
        zimbra_index_reader_idle_flush_time.setDefault("300");

        zimbra_index_reader_idle_sweep_frequency = new KnownKey("zimbra_index_reader_idle_sweep_frequency");
        zimbra_index_reader_idle_sweep_frequency.setDefault("30");

        zimbra_index_deferred_items_delay = new KnownKey("zimbra_index_deferred_items_delay", "10");
        zimbra_index_deferred_items_failure_delay = new KnownKey("zimbra_index_deferred_items_failure_delay", "300");
        
        zimbra_index_max_transaction_bytes = new KnownKey("zimbra_index_max_transaction_bytes", "1000000");
        zimbra_index_max_transaction_items = new KnownKey("zimbra_index_max_transaction_items", "100");
        
        zimbra_index_lucene_autocommit = new KnownKey("zimbra_index_lucene_autocommit", "false");

        zimbra_index_use_nfs_deletion_policy = new KnownKey("zimbra_index_use_nfs_deletion_policy", "false");
        
        zimbra_index_use_reader_reopen = new KnownKey("zimbra_index_use_reader_reopen", "false");

        
        zimbra_index_lucene_batch_use_doc_scheduler = new KnownKey("zimbra_index_lucene_batch_use_doc_scheduler", "true");
        zimbra_index_lucene_batch_min_merge = new KnownKey("zimbra_index_lucene_batch_min_merge", "1000");
        zimbra_index_lucene_batch_max_merge = new KnownKey("zimbra_index_lucene_batch_max_merge", Integer.toString(Integer.MAX_VALUE));
        zimbra_index_lucene_batch_merge_factor = new KnownKey("zimbra_index_lucene_batch_merge_factor", "10");
        zimbra_index_lucene_batch_use_compound_file = new KnownKey("zimbra_index_lucene_batch_use_compound_file", "true");
        zimbra_index_lucene_batch_use_serial_merge_scheduler = new KnownKey("zimbra_index_lucene_batch_use_serial_merge_scheduler", "true");
        zimbra_index_lucene_batch_max_buffered_docs = new KnownKey("zimbra_index_lucene_batch_max_buffered_docs", "200");
        zimbra_index_lucene_batch_ram_buffer_size_kb = new KnownKey("zimbra_index_lucene_batch_ram_buffer_size_kb", "10240");
        zimbra_index_lucene_batch_autocommit = new KnownKey("zimbra_index_lucene_batch_autocommit", "false");
            
        zimbra_index_lucene_nobatch_use_doc_scheduler = new KnownKey("zimbra_index_lucene_nobatch_use_doc_scheduler", "true");
        zimbra_index_lucene_nobatch_min_merge = new KnownKey("zimbra_index_lucene_nobatch_min_merge", "10");
        zimbra_index_lucene_nobatch_max_merge = new KnownKey("zimbra_index_lucene_nobatch_max_merge", Integer.toString(Integer.MAX_VALUE));
        zimbra_index_lucene_nobatch_merge_factor = new KnownKey("zimbra_index_lucene_nobatch_merge_factor", "3");
        zimbra_index_lucene_nobatch_use_compound_file = new KnownKey("zimbra_index_lucene_nobatch_use_compound_file", "true");
        zimbra_index_lucene_nobatch_use_serial_merge_scheduler = new KnownKey("zimbra_index_lucene_nobatch_use_serial_merge_scheduler", "true");
        zimbra_index_lucene_nobatch_max_buffered_docs = new KnownKey("zimbra_index_lucene_nobatch_max_buffered_docs", "200");
        zimbra_index_lucene_nobatch_ram_buffer_size_kb = new KnownKey("zimbra_index_lucene_nobatch_ram_buffer_size_kb", "10240");
        zimbra_index_lucene_nobatch_autocommit = new KnownKey("zimbra_index_lucene_nobatch_autocommit", "false");
        
        zimbra_spam_report_queue_size = new KnownKey("zimbra_spam_report_queue_size");
        zimbra_spam_report_queue_size.setDefault("100");

        zimbra_throttle_op_concurrency = new KnownKey("zimbra_throttle_op_concurrency");
        zimbra_throttle_op_concurrency.setDefault("1000,1000,1000,1000,1000");

        zimbra_im_chat_flush_time = new KnownKey("zimbra_im_chat_flush_time", "300");
        zimbra_im_chat_close_time = new KnownKey("zimbra_im_chat_close_time", "3600");

        zimbra_session_limit_imap = new KnownKey("zimbra_session_limit_imap");
        zimbra_session_limit_imap.setDefault("5");

        zimbra_session_timeout_soap = new KnownKey("zimbra_session_timeout_soap");
        zimbra_session_timeout_soap.setDefault("600");

        zimbra_session_max_pending_notifications = new KnownKey("zimbra_session_max_pending_notifications");
        zimbra_session_max_pending_notifications.setDefault("400");

        zimbra_converter_enabled_uuencode = new KnownKey("zimbra_converter_enabled_uuencode");
        zimbra_converter_enabled_uuencode.setDefault("true");

        zimbra_converter_enabled_tnef = new KnownKey("zimbra_converter_enabled_tnef");
        zimbra_converter_enabled_tnef.setDefault("true");

        
        zimbra_ssl_enabled = new KnownKey("zimbra_ssl_enabled", "true");
        stats_img_folder = new KnownKey("stats_img_folder");
        stats_img_folder.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "work");

        ldap_host = new KnownKey("ldap_host");
        ldap_host.setDefault("");

        ldap_log_level = new KnownKey("ldap_log_level");
        ldap_log_level.setDefault("32768");

        ldap_port = new KnownKey("ldap_port");
        ldap_port.setDefault("");

        ldap_url = new KnownKey("ldap_url");
        ldap_url.setDefault("");

        ldap_master_url = new KnownKey("ldap_master_url");
        ldap_master_url.setDefault("");

        ldap_is_master = new KnownKey("ldap_is_master");
        ldap_is_master.setDefault("false");

        ldap_root_password = new KnownKey("ldap_root_password");
        ldap_root_password.setDefault("zimbra");
        ldap_root_password.setForceToEdit(true);

        ldap_connect_timeout = new KnownKey("ldap_connect_timeout");
        ldap_connect_timeout.setDefault("30000");

        ldap_read_timeout = new KnownKey("ldap_read_timeout");
        ldap_read_timeout.setDefault("30000");

        ldap_deref_aliases = new KnownKey("ldap_deref_aliases");
        ldap_deref_aliases.setDefault("always");

        ldap_connect_pool_master = new KnownKey("ldap_connect_pool_master");
        ldap_connect_pool_master.setDefault("false");

        ldap_connect_pool_debug = new KnownKey("ldap_connect_pool_debug");
        ldap_connect_pool_debug.setDefault("false");

        ldap_connect_pool_initsize = new KnownKey("ldap_connect_pool_initsize");
        ldap_connect_pool_initsize.setDefault("1");

        ldap_connect_pool_maxsize = new KnownKey("ldap_connect_pool_maxsize");
        ldap_connect_pool_maxsize.setDefault("50");

        ldap_connect_pool_prefsize = new KnownKey("ldap_connect_pool_prefsize");
        ldap_connect_pool_prefsize.setDefault("0");

        ldap_connect_pool_timeout = new KnownKey("ldap_connect_pool_timeout");
        ldap_connect_pool_timeout.setDefault("120000");

        ldap_replication_password = new KnownKey("ldap_replication_password");
        ldap_replication_password.setDefault("zmreplica");

        ldap_postfix_password = new KnownKey("ldap_postfix_password");
        ldap_postfix_password.setDefault("zmpostfix");

        ldap_amavis_password = new KnownKey("ldap_amavis_password");
        ldap_amavis_password.setDefault("zmamavis");

	ldap_starttls_supported = new KnownKey("ldap_starttls_supported"); 
        ldap_starttls_supported.setDefault("0");

        ldap_require_tls = new KnownKey("ldap_require_tls");
        ldap_require_tls.setDefault("false");

        ldap_cache_account_maxsize = new KnownKey("ldap_cache_account_maxsize", "20000");

        ldap_cache_account_maxage = new KnownKey("ldap_cache_account_maxage", "15");

        ldap_cache_cos_maxsize = new KnownKey("ldap_cache_cos_maxsize", "100");

        ldap_cache_cos_maxage = new KnownKey("ldap_cache_cos_maxage", "15");

        ldap_cache_domain_maxsize = new KnownKey("ldap_cache_domain_maxsize", "100");

        ldap_cache_domain_maxage = new KnownKey("ldap_cache_domain_maxage", "15");

        ldap_cache_server_maxsize = new KnownKey("ldap_cache_server_maxsize", "100");

        ldap_cache_server_maxage = new KnownKey("ldap_cache_server_maxage", "15");

        ldap_cache_timezone_maxsize = new KnownKey("ldap_cache_timezone_maxsize", "100");

        ldap_cache_zimlet_maxsize = new KnownKey("ldap_cache_zimlet_maxsize", "100");

        ldap_cache_zimlet_maxage = new KnownKey("ldap_cache_zimlet_maxage", "15");

        mysql_directory = new KnownKey("mysql_directory");
        mysql_directory.setDefault("${zimbra_home}" + FS + "mysql");

        mysql_data_directory = new KnownKey("mysql_data_directory");
        mysql_data_directory.setDefault("${zimbra_db_directory}" + FS + "data");

        mysql_socket = new KnownKey("mysql_socket");
        mysql_socket.setDefault("${zimbra_db_directory}" + FS + "mysql.sock");

        mysql_pidfile = new KnownKey("mysql_pidfile");
        mysql_pidfile.setDefault("${zimbra_db_directory}" + FS + "mysql.pid");

        mysql_mycnf = new KnownKey("mysql_mycnf");
        mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.cnf");

        mysql_bind_address = new KnownKey("mysql_bind_address");
        mysql_bind_address.setDefault("localhost");

        mysql_port = new KnownKey("mysql_port");
        mysql_port.setDefault("7306");

        mysql_root_password = new KnownKey("mysql_root_password");
        mysql_root_password.setDefault("zimbra");
        mysql_root_password.setForceToEdit(true);

        mysql_memory_percent = new KnownKey("mysql_memory_percent");
        mysql_memory_percent.setDefault("40");

        mysql_innodb_log_buffer_size = new KnownKey("mysql_innodb_log_buffer_size");
        mysql_innodb_log_buffer_size.setDefault("8388608");

        mysql_innodb_log_file_size = new KnownKey("mysql_innodb_log_file_size");
        mysql_innodb_log_file_size.setDefault("104857600");

        mysql_sort_buffer_size = new KnownKey("mysql_sort_buffer_size");
        mysql_sort_buffer_size.setDefault("1048576");

        mysql_read_buffer_size = new KnownKey("mysql_read_buffer_size");
        mysql_read_buffer_size.setDefault("1048576");

        mysql_table_cache = new KnownKey("mysql_table_cache");
        mysql_table_cache.setDefault("500");

        zimbra_logger_mysql_password = new KnownKey("zimbra_logger_mysql_password");
        zimbra_logger_mysql_password.setDefault("zimbra");
        zimbra_logger_mysql_password.setForceToEdit(true);

        logger_mysql_directory = new KnownKey("logger_mysql_directory");
        logger_mysql_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "mysql");

        logger_mysql_data_directory = new KnownKey("logger_mysql_data_directory");
        logger_mysql_data_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "data");

        logger_mysql_socket = new KnownKey("logger_mysql_socket");
        logger_mysql_socket.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.sock");

        logger_mysql_pidfile = new KnownKey("logger_mysql_pidfile");
        logger_mysql_pidfile.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "mysql.pid");

        logger_mysql_mycnf = new KnownKey("logger_mysql_mycnf");
        logger_mysql_mycnf.setDefault("${zimbra_home}" + FS + "conf" + FS + "my.logger.cnf");

        logger_mysql_bind_address = new KnownKey("logger_mysql_bind_address");
        logger_mysql_bind_address.setDefault("localhost");

        logger_mysql_port = new KnownKey("logger_mysql_port");
        logger_mysql_port.setDefault("7307");

        postfix_alias_maps  = new KnownKey("postfix_alias_maps");
        postfix_alias_maps.setDefault("hash:" + FS + "etc" + FS + "aliases");

        postfix_broken_sasl_auth_clients  = new KnownKey("postfix_broken_sasl_auth_clients");
        postfix_broken_sasl_auth_clients.setDefault("yes");

        postfix_command_directory  = new KnownKey("postfix_command_directory");
        postfix_command_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin");

        postfix_daemon_directory  = new KnownKey("postfix_daemon_directory");
        postfix_daemon_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "libexec");

        postfix_header_checks  = new KnownKey("postfix_header_checks");
        postfix_header_checks.setDefault("pcre:${zimbra_home}" + FS + "conf" + FS + "postfix_header_checks");

        postfix_mailq_path  = new KnownKey("postfix_mailq_path");
        postfix_mailq_path.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin" + FS + "mailq");

        postfix_manpage_directory  = new KnownKey("postfix_manpage_directory");
        postfix_manpage_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "man");

        postfix_newaliases_path  = new KnownKey("postfix_newaliases_path");
        postfix_newaliases_path.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin" + FS + "newaliases");

        postfix_queue_directory  = new KnownKey("postfix_queue_directory");
        postfix_queue_directory.setDefault("${zimbra_home}" + FS + "data" + FS + "postfix" + FS + "spool");

        postfix_sender_canonical_maps  = new KnownKey("postfix_sender_canonical_maps");
        postfix_sender_canonical_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-scm.cf");

        postfix_sendmail_path  = new KnownKey("postfix_sendmail_path");
        postfix_sendmail_path.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin" + FS + "sendmail");

        postfix_smtpd_client_restrictions  = new KnownKey("postfix_smtpd_client_restrictions");
        postfix_smtpd_client_restrictions.setDefault("reject_unauth_pipelining");

        postfix_smtpd_data_restrictions  = new KnownKey("postfix_smtpd_data_restrictions");
        postfix_smtpd_data_restrictions.setDefault("reject_unauth_pipelining");

        postfix_smtpd_helo_required  = new KnownKey("postfix_smtpd_helo_required");
        postfix_smtpd_helo_required.setDefault("yes");

        postfix_smtpd_tls_cert_file  = new KnownKey("postfix_smtpd_tls_cert_file");
        postfix_smtpd_tls_cert_file.setDefault("${zimbra_home}"+FS+"conf"+FS+"smtpd.crt");

        postfix_smtpd_tls_key_file  = new KnownKey("postfix_smtpd_tls_key_file");
        postfix_smtpd_tls_key_file.setDefault("${zimbra_home}"+FS+"conf"+FS+"smtpd.key");

        postfix_smtpd_tls_loglevel  = new KnownKey("postfix_smtpd_tls_loglevel");
        postfix_smtpd_tls_loglevel.setDefault("1");

        postfix_in_flow_delay  = new KnownKey("postfix_in_flow_delay");
        postfix_in_flow_delay.setDefault("1s");

        postfix_transport_maps  = new KnownKey("postfix_transport_maps");
        postfix_transport_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-transport.cf");

        postfix_version  = new KnownKey("postfix_version");
        postfix_version.setDefault("2.4.7.5z");

        postfix_virtual_alias_domains  = new KnownKey("postfix_virtual_alias_domains");
        postfix_virtual_alias_domains.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vad.cf");

        postfix_virtual_alias_maps  = new KnownKey("postfix_virtual_alias_maps");
        postfix_virtual_alias_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vam.cf");

        postfix_virtual_mailbox_domains  = new KnownKey("postfix_virtual_mailbox_domains");
        postfix_virtual_mailbox_domains.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmd.cf");

        postfix_virtual_mailbox_maps  = new KnownKey("postfix_virtual_mailbox_maps");
        postfix_virtual_mailbox_maps.setDefault("ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmm.cf");

        postfix_virtual_transport  = new KnownKey("postfix_virtual_transport");
        postfix_virtual_transport.setDefault("error");

        mailboxd_directory = new KnownKey("mailboxd_directory");
        mailboxd_directory.setDefault("${zimbra_home}" + FS + "mailboxd");

        mailboxd_java_heap_memory_percent = new KnownKey("mailboxd_java_heap_memory_percent");
        mailboxd_java_heap_memory_percent.setDefault("30");

        mailboxd_java_options = new KnownKey("mailboxd_java_options");
        mailboxd_java_options.setDefault("-client -XX:NewRatio=2 -Djava.awt.headless=true -XX:MaxPermSize=128m -XX:SoftRefLRUPolicyMSPerMB=1");

        mailboxd_java_home = new KnownKey("mailboxd_java_home");
        mailboxd_java_home.setDefault("${zimbra_java_home}");

        mailboxd_pidfile = new KnownKey("mailboxd_pidfile");
        mailboxd_pidfile.setDefault("${zimbra_log_directory}" + FS + "mailboxd.pid");

        mailboxd_keystore = new KnownKey("mailboxd_keystore");
        mailboxd_keystore.setDefault("${mailboxd_directory}" + FS + "etc" + FS + "keystore");

        mailboxd_keystore_password = new KnownKey("mailboxd_keystore_password");
        mailboxd_keystore_password.setDefault("zimbra");

        mailboxd_truststore = new KnownKey("mailboxd_truststore");
        mailboxd_truststore.setDefault("${zimbra_java_home}" + FS + "lib" + FS + "security" + FS + "cacerts");

        mailboxd_truststore_password = new KnownKey("mailboxd_truststore_password");
        mailboxd_truststore_password.setDefault("changeit");

        mailboxd_output_file = new KnownKey("mailboxd_output_file");
        mailboxd_output_file.setDefault("${zimbra_log_directory}" + FS + "zmmailboxd.out");

        mailboxd_output_rotate_interval = new KnownKey("mailboxd_output_rotate_interval");
        mailboxd_output_rotate_interval.setDefault("86400");

        mailboxd_thread_stack_size = new KnownKey("mailboxd_thread_stack_size");
        mailboxd_thread_stack_size.setDefault("256k");

        ssl_allow_untrusted_certs = new KnownKey("ssl_allow_untrusted_certs");
        ssl_allow_untrusted_certs.setDefault("false");

        zimlet_directory = new KnownKey("zimlet_directory");
        zimlet_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "service" + FS + "zimlet");

        wiki_enabled = new KnownKey("wiki_enabled");
        wiki_enabled.setDefault("false");

        wiki_user = new KnownKey("wiki_user");
        wiki_user.setDefault("wiki");

        calendar_outlook_compatible_allday_events = new KnownKey("calendar_outlook_compatible_allday_events");
        calendar_outlook_compatible_allday_events.setDefault("true");

        calendar_entourage_compatible_timezones = new KnownKey("calendar_entourage_compatible_timezones");
        calendar_entourage_compatible_timezones.setDefault("true");

        calendar_ics_import_full_parse_max_size = new KnownKey("calendar_ics_import_full_parse_max_size");
        calendar_ics_import_full_parse_max_size.setDefault("131072");  // 128KB

        calendar_canonical_tzid = new KnownKey("calendar_canonical_tzid");
        calendar_canonical_tzid.setDefault("windows");

        calendar_cache_enabled = new KnownKey("calendar_cache_enabled");
        calendar_cache_enabled.setDefault("false");

        calendar_cache_directory = new KnownKey("calendar_cache_directory");
        calendar_cache_directory.setDefault("${zimbra_home}" + FS + "store" + FS + "calcache");

        calendar_cache_lru_size = new KnownKey("calendar_cache_lru_size");
        calendar_cache_lru_size.setDefault("1000");

        calendar_cache_range_month_from = new KnownKey("calendar_cache_range_month_from");
        calendar_cache_range_month_from.setDefault("0");

        calendar_cache_range_months = new KnownKey("calendar_cache_range_months");
        calendar_cache_range_months.setDefault("3");

        nio_imap_enabled = new KnownKey("nio_imap_enabled");
        nio_imap_enabled.setDefault("false");

        nio_imap_debug_logging = new KnownKey("nio_imap_log_buffers");
        nio_imap_debug_logging.setDefault("false");

        nio_pop3_enabled = new KnownKey("nio_pop3_enabled");
        nio_pop3_enabled.setDefault("false");

        nio_lmtp_enabled = new KnownKey("nio_lmtp_enabled");
        nio_lmtp_enabled.setDefault("false");

        nio_enabled = new KnownKey("nio_enabled");
        nio_enabled.setDefault("false");

        nio_debug_enabled = new KnownKey("nio_debug_enabled");
        nio_debug_enabled.setDefault("false");

        krb5_keytab = new KnownKey("krb5_keytab");
        krb5_keytab.setDefault("${zimbra_home}" + FS + "conf" + FS + "krb5.keytab");

        krb5_debug_enabled = new KnownKey("krb5_debug_enabled");
        krb5_debug_enabled.setDefault("false");

        zimbra_mtareport_max_recipients = new KnownKey("zimbra_mtareport_max_recipients");
        zimbra_mtareport_max_recipients.setDefault("50");

        zimbra_mtareport_max_senders = new KnownKey("zimbra_mtareport_max_senders");
        zimbra_mtareport_max_senders.setDefault("50");

        zimbra_auth_always_send_refer = new KnownKey("zimbra_auth_always_send_refer");
        zimbra_auth_always_send_refer.setDefault("false");

        zimbra_admin_service_port = new KnownKey("zimbra_admin_service_port", "7071");

        zimbra_admin_service_scheme = new KnownKey("zimbra_admin_service_scheme", "https://");

        zimbra_zmprov_default_to_ldap = new KnownKey("zimbra_zmprov_default_to_ldap", "false");

        zimbra_zmprov_default_soap_server = new KnownKey("zimbra_zmprov_default_soap_server", "localhost");

        zimbra_require_interprocess_security = new KnownKey("zimbra_require_interprocess_security");
        zimbra_require_interprocess_security.setDefault("1");

        zimbra_mailbox_groups = new KnownKey("zimbra_mailbox_groups");
        zimbra_mailbox_groups.setDefault("100");

        debug_mailboxindex_use_new_locking = new KnownKey("debug_mailboxindex_use_new_locking");
        debug_mailboxindex_use_new_locking.setDefault("true");

        zimbra_class_provisioning = new KnownKey("zimbra_class_provisioning", "com.zimbra.cs.account.ldap.LdapProvisioning");
        zimbra_class_accessmanager = new KnownKey("zimbra_class_accessmanager", "com.zimbra.cs.account.DomainAccessManager");
        zimbra_class_mboxmanager = new KnownKey("zimbra_class_mboxmanager", "com.zimbra.cs.mailbox.MailboxManager");
        zimbra_class_database = new KnownKey("zimbra_class_database", "com.zimbra.cs.db.MySQL");
        zimbra_class_application = new KnownKey("zimbra_class_application", "com.zimbra.cs.util.ZimbraApplication");
        zimbra_class_rulerewriterfactory = new KnownKey("zimbra_class_rulerewriterfactory", "com.zimbra.cs.filter.RuleRewriterFactory");
        
        data_source_trust_self_signed_certs = new KnownKey("data_source_trust_self_signed_certs", "false");
        data_source_fetch_size = new KnownKey("data_source_fetch_size", "5");
        data_source_max_message_memory_size = new KnownKey("data_source_max_message_memory_size", "2097152"); // 2 megabytes

        timezone_file = new KnownKey("timezone_file");
        timezone_file.setDefault("${zimbra_home}" + FS + "conf" + FS + "timezones.ics");

        search_disable_database_hints = new KnownKey("search_disable_database_hints");
        search_disable_database_hints.setDefault("false");

        search_dbfirst_term_percentage_cutoff = new KnownKey("search_dbfirst_term_percentage_cutoff");
        search_dbfirst_term_percentage_cutoff.setDefault("0.8");

        zmstat_log_directory = new KnownKey("zmstat_log_directory");
        zmstat_log_directory.setDefault("${zimbra_home}" + FS + "zmstat");

        zmstat_interval = new KnownKey("zmstat_interval");
        zmstat_interval.setDefault("30");

        zimbra_noop_default_timeout = new KnownKey("zimbra_noop_default_timeout", "300");
        zimbra_noop_min_timeout = new KnownKey("zimbra_noop_min_timeout", "30");
        zimbra_noop_max_timeout = new KnownKey("zimbra_noop_max_timeout", "1200");
        
        zimbra_waitset_default_request_timeout = new KnownKey("zimbra_waitset_default_request_timeout", "300");
        zimbra_waitset_min_request_timeout = new KnownKey("zimbra_waitset_min_request_timeout", "30");
        zimbra_waitset_max_request_timeout = new KnownKey("zimbra_waitset_max_request_timeout", "1200");
        zimbra_waitset_max_per_account = new KnownKey("zimbra_waitset_max_per_account", "5");
        
        zimbra_admin_waitset_default_request_timeout = new KnownKey("zimbra_admin_waitset_default_request_timeout", "300");
        zimbra_admin_waitset_min_request_timeout = new KnownKey("zimbra_admin_waitset_min_request_timeout", "0");
        zimbra_admin_waitset_max_request_timeout = new KnownKey("zimbra_admin_waitset_max_request_timeout", "3600");
        
        zimbra_waitset_initial_sleep_time  = new KnownKey("zimbra_waitset_initial_sleep_time", "1");
        zimbra_waitset_nodata_sleep_time = new KnownKey("zimbra_waitset_nodata_sleep_time", "3");

        zimbra_csv_mapping_file = new KnownKey("zimbra_csv_mapping_file");
        zimbra_csv_mapping_file.setDefault("${zimbra_home}" + FS + "conf" + FS + "zimbra-contact-fields.xml");

        zimbra_auth_provider = new KnownKey("zimbra_auth_provider");
        zimbra_auth_provider.setDefault("zimbra");

        debug_update_config_use_old_scheme = new KnownKey("debug_update_config_use_old_scheme");
        debug_update_config_use_old_scheme.setDefault("false");

        debug_xmpp_disable_client_tls = new KnownKey("debug_xmpp_disable_client_tls", "0");
        
        im_dnsutil_dnsoverride = new KnownKey("im_dnsutil_dnsoverride", "");
        
        javamail_pop3_debug = new KnownKey("javamail_pop3_debug");
        javamail_pop3_debug.setDefault("false");

        javamail_imap_debug = new KnownKey("javamail_imap_debug");
        javamail_imap_debug.setDefault("false");

        javamail_smtp_debug = new KnownKey("javamail_smtp_debug");
        javamail_smtp_debug.setDefault("false");

        javamail_pop3_timeout = new KnownKey("javamail_pop3_timeout");
        javamail_pop3_timeout.setDefault("60");
                                                 
        javamail_imap_timeout = new KnownKey("javamail_imap_timeout");
        javamail_imap_timeout.setDefault("60");
        
        javamail_smtp_timeout = new KnownKey("javamail_smtp_timeout");
        javamail_smtp_timeout.setDefault("60");

        javamail_pop3_enable_starttls = new KnownKey("javamail_pop3_enable_starttls");
        javamail_pop3_enable_starttls.setDefault("true");

        javamail_imap_enable_starttls = new KnownKey("javamail_imap_enable_starttls");
        javamail_imap_enable_starttls.setDefault("true");

        javamail_smtp_enable_starttls = new KnownKey("javamail_smtp_enable_starttls");
        javamail_smtp_enable_starttls.setDefault("true");

		// NOTE: When adding a new KnownKey, you do not need to call
		//       setDoc. The documentation string will come from the
		//       ZsMsg properties file, using the same key as the
		//       KnownKey.
		//       You can still use setDoc but it is NOT recommended
		//       because it will not be able to be translated.
	}
}
