/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.localconfig;

import com.zimbra.common.util.Constants;

import java.io.File;

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
//            Logging.warn("LC.get(" + key + ") was null, returning empty string");
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

    public static final KnownKey zimbra_minimize_resources;

    public static final KnownKey zimbra_home;
    public static final KnownKey zimbra_java_path;
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
    public static final KnownKey zimbra_rights_directory;
    public static final KnownKey zimbra_user;
    public static final KnownKey zimbra_uid;
    public static final KnownKey zimbra_gid;
    public static final KnownKey zimbra_log4j_properties;
    public static final KnownKey zimbra_log4j_properties_watch;
    public static final KnownKey zimbra_auth_always_send_refer;
    public static final KnownKey zimbra_admin_service_port;
    public static final KnownKey zimbra_admin_service_scheme;
    public static final KnownKey zimbra_zmprov_default_to_ldap;
    public static final KnownKey zimbra_zmprov_default_soap_server;
    public static final KnownKey zimbra_require_interprocess_security;
    public static final KnownKey zimbra_relative_volume_path;
    public static final KnownKey localized_msgs_directory;
    public static final KnownKey localized_client_msgs_directory;
    public static final KnownKey skins_directory;

    public static final KnownKey zimbra_disk_cache_servlet_flush;
    public static final KnownKey zimbra_disk_cache_servlet_size;

    public static final KnownKey zimbra_store_sweeper_max_age;
    public static final KnownKey zimbra_store_copy_buffer_size_kb;
    public static final KnownKey zimbra_nio_file_copy_chunk_size_kb;
    public static final KnownKey zimbra_blob_input_stream_buffer_size_kb;

    public static final KnownKey zimbra_mailbox_manager_hardref_cache;
    public static final KnownKey zimbra_mailbox_active_cache;
    public static final KnownKey zimbra_mailbox_inactive_cache;
    public static final KnownKey zimbra_mailbox_change_checkpoint_frequency;

    public static final KnownKey zimbra_index_factory_classname;
    public static final KnownKey zimbra_index_max_uncommitted_operations;
    public static final KnownKey zimbra_index_lru_size;
    public static final KnownKey zimbra_index_idle_flush_time;
    public static final KnownKey zimbra_index_sweep_frequency;
    public static final KnownKey zimbra_index_completed_pool_size;
    public static final KnownKey zimbra_index_flush_pool_size;
    public static final KnownKey zimbra_index_reindex_pool_size;
    
    public static final KnownKey zimbra_index_reader_lru_size;
    public static final KnownKey zimbra_index_reader_idle_flush_time;
    public static final KnownKey zimbra_index_reader_idle_sweep_frequency;
    public static final KnownKey zimbra_index_deferred_items_delay;
    public static final KnownKey zimbra_index_deferred_items_failure_delay;

    public static final KnownKey zimbra_index_max_transaction_bytes;
    public static final KnownKey zimbra_index_max_transaction_items;
    public static final KnownKey zimbra_index_lucene_autocommit;
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
    public static final KnownKey zimbra_index_lucene_max_terms_per_query;
    public static final KnownKey zimbra_index_wildcard_max_terms_expanded;
    
    public static final KnownKey zimbra_index_lucene_nobatch_use_doc_scheduler;
    public static final KnownKey zimbra_index_lucene_nobatch_min_merge;
    public static final KnownKey zimbra_index_lucene_nobatch_max_merge;
    public static final KnownKey zimbra_index_lucene_nobatch_merge_factor;
    public static final KnownKey zimbra_index_lucene_nobatch_use_compound_file;
    public static final KnownKey zimbra_index_lucene_nobatch_use_serial_merge_scheduler;
    public static final KnownKey zimbra_index_lucene_nobatch_max_buffered_docs;
    public static final KnownKey zimbra_index_lucene_nobatch_ram_buffer_size_kb;
    public static final KnownKey zimbra_index_lucene_nobatch_autocommit;
    
    public static final KnownKey zimbra_rights_delegated_admin_supported;

    public static final KnownKey zimbra_spam_report_queue_size;

    public static final KnownKey zimbra_throttle_op_concurrency;

    public static final KnownKey zimbra_web_generate_gzip;

    public static final KnownKey zimbra_im_chat_flush_time;
    public static final KnownKey zimbra_im_chat_close_time;

    public static final KnownKey zimbra_http_originating_ip_header;

    public static final KnownKey zimbra_session_limit_imap;
    public static final KnownKey zimbra_session_timeout_soap;
    public static final KnownKey zimbra_session_max_pending_notifications;

    public static final KnownKey zimbra_converter_enabled_uuencode;
    public static final KnownKey zimbra_converter_enabled_tnef;
    public static final KnownKey zimbra_converter_depth_max;

    public static final KnownKey zimbra_ssl_enabled;
    
    public static final KnownKey stats_img_folder;
    
    public static final KnownKey soap_fault_include_stack_trace;
    public static final KnownKey soap_response_buffer_size;
    public static final KnownKey soap_response_chunked_transfer_encoding_disabled;
    public static final KnownKey zimbra_servlet_output_stream_buffer_size;

    public static final KnownKey ldap_host;
    public static final KnownKey ldap_port;
    public static final KnownKey ldap_url;
    public static final KnownKey ldap_master_url;
    public static final KnownKey ldap_bind_url;
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
    public static final KnownKey ldap_nginx_password;
    public static final KnownKey ldap_starttls_supported;
    public static final KnownKey ldap_common_loglevel;
    public static final KnownKey ldap_common_require_tls;
    public static final KnownKey ldap_common_threads;
    public static final KnownKey ldap_common_toolthreads;
    public static final KnownKey ldap_common_writetimeout;
    public static final KnownKey ldap_db_cachefree;
    public static final KnownKey ldap_db_cachesize;
    public static final KnownKey ldap_db_checkpoint;
    public static final KnownKey ldap_db_dncachesize;
    public static final KnownKey ldap_db_idlcachesize;
    public static final KnownKey ldap_db_shmkey;
    public static final KnownKey ldap_accesslog_cachefree;
    public static final KnownKey ldap_accesslog_cachesize;
    public static final KnownKey ldap_accesslog_checkpoint;
    public static final KnownKey ldap_accesslog_dncachesize;
    public static final KnownKey ldap_accesslog_idlcachesize;
    public static final KnownKey ldap_accesslog_shmkey;
    public static final KnownKey ldap_overlay_syncprov_checkpoint;
    public static final KnownKey ldap_overlay_syncprov_sessionlog;
    public static final KnownKey ldap_overlay_accesslog_logpurge;


    public static final KnownKey ldap_cache_account_maxsize;
    public static final KnownKey ldap_cache_account_maxage;
    public static final KnownKey ldap_cache_cos_maxsize;
    public static final KnownKey ldap_cache_cos_maxage;
    public static final KnownKey ldap_cache_domain_maxsize;
    public static final KnownKey ldap_cache_domain_maxage;
    public static final KnownKey ldap_cache_external_domain_maxsize;
    public static final KnownKey ldap_cache_external_domain_maxage;
    public static final KnownKey ldap_cache_group_maxsize;
    public static final KnownKey ldap_cache_group_maxage;
    public static final KnownKey ldap_cache_right_maxsize;
    public static final KnownKey ldap_cache_right_maxage;
    public static final KnownKey ldap_cache_server_maxsize;
    public static final KnownKey ldap_cache_server_maxage;
    public static final KnownKey ldap_cache_timezone_maxsize;
    public static final KnownKey ldap_cache_xmppcomponent_maxsize;
    public static final KnownKey ldap_cache_xmppcomponent_maxage;
    public static final KnownKey ldap_cache_zimlet_maxsize;
    public static final KnownKey ldap_cache_zimlet_maxage;
    
    public static final KnownKey ldap_cache_reverseproxylookup_domain_maxsize;
    public static final KnownKey ldap_cache_reverseproxylookup_domain_maxage;
    public static final KnownKey ldap_cache_reverseproxylookup_server_maxsize;
    public static final KnownKey ldap_cache_reverseproxylookup_server_maxage;
    
    public static final KnownKey mysql_directory;
    public static final KnownKey mysql_data_directory;
    public static final KnownKey mysql_socket;
    public static final KnownKey mysql_pidfile;
    public static final KnownKey mysql_mycnf;
    public static final KnownKey mysql_errlogfile;
    public static final KnownKey mysql_bind_address;
    public static final KnownKey mysql_port;
    public static final KnownKey mysql_root_password;

    public static final KnownKey derby_properties;

    public final static KnownKey logger_data_directory;
    public final static KnownKey logger_zmrrdfetch_port;
    public static final KnownKey logger_mysql_directory;
    public static final KnownKey logger_mysql_data_directory;
    public static final KnownKey logger_mysql_socket;
    public static final KnownKey logger_mysql_pidfile;
    public static final KnownKey logger_mysql_mycnf;
    public static final KnownKey logger_mysql_errlogfile;
    public static final KnownKey logger_mysql_bind_address;
    public static final KnownKey logger_mysql_port;
    public static final KnownKey zimbra_logger_mysql_password;

    public static final KnownKey postfix_alias_maps;
    public static final KnownKey postfix_broken_sasl_auth_clients;
    public static final KnownKey postfix_bounce_queue_lifetime;
    public static final KnownKey postfix_command_directory;
    public static final KnownKey postfix_daemon_directory;
    public static final KnownKey postfix_enable_smtpd_policyd;
    public static final KnownKey postfix_header_checks;
    public static final KnownKey postfix_in_flow_delay;
    public static final KnownKey postfix_lmtp_connection_cache_destinations;
    public static final KnownKey postfix_lmtp_connection_cache_time_limit;
    public static final KnownKey postfix_lmtp_host_lookup;
    public static final KnownKey postfix_mailq_path;
    public static final KnownKey postfix_manpage_directory;
    public static final KnownKey postfix_maximal_backoff_time;
    public static final KnownKey postfix_minimal_backoff_time;
    public static final KnownKey postfix_newaliases_path;
    public static final KnownKey postfix_policy_time_limit;
    public static final KnownKey postfix_queue_directory;
    public static final KnownKey postfix_smtpd_sasl_authenticated_header;
    public static final KnownKey postfix_sender_canonical_maps;
    public static final KnownKey postfix_sendmail_path;
    public static final KnownKey postfix_smtpd_client_restrictions;
    public static final KnownKey postfix_smtpd_data_restrictions;
    public static final KnownKey postfix_smtpd_helo_required;
    public static final KnownKey postfix_smtpd_tls_cert_file;
    public static final KnownKey postfix_smtpd_tls_key_file;
    public static final KnownKey postfix_smtpd_tls_loglevel;
    public static final KnownKey postfix_queue_run_delay;
    public static final KnownKey postfix_transport_maps;
    public static final KnownKey postfix_propagate_unmatched_extensions;
    public static final KnownKey postfix_virtual_alias_domains;
    public static final KnownKey postfix_virtual_alias_maps;
    public static final KnownKey postfix_virtual_mailbox_domains;
    public static final KnownKey postfix_virtual_mailbox_maps;
    public static final KnownKey postfix_virtual_transport;

    public static final KnownKey sqlite_cache_size;
    public static final KnownKey sqlite_journal_mode;
    public static final KnownKey sqlite_page_size;
    public static final KnownKey sqlite_sync_mode;
                                                                  
    public static final KnownKey mailboxd_directory;
    public static final KnownKey mailboxd_java_heap_memory_percent;
    public static final KnownKey mailboxd_java_heap_new_size_percent;
    public static final KnownKey mailboxd_thread_stack_size;
    public static final KnownKey mailboxd_java_options;
    public static final KnownKey mailboxd_pidfile;
    public static final KnownKey mailboxd_keystore;
    public static final KnownKey mailboxd_keystore_password;
    public static final KnownKey mailboxd_keystore_base;
    public static final KnownKey mailboxd_keystore_base_password;
    public static final KnownKey mailboxd_truststore;
    public static final KnownKey mailboxd_truststore_password;
    public static final KnownKey mailboxd_output_filename;
    public static final KnownKey mailboxd_output_file;
    public static final KnownKey mailboxd_output_rotate_interval;
    
    public static final KnownKey ssl_allow_untrusted_certs;
    public static final KnownKey ssl_allow_mismatched_certs;
    public static final KnownKey ssl_allow_accept_untrusted_certs;

    public static final KnownKey zimlet_directory;
    public static final KnownKey wiki_enabled;
    public static final KnownKey wiki_user;

    public static final KnownKey calendar_outlook_compatible_allday_events;
    public static final KnownKey calendar_entourage_compatible_timezones;
    public static final KnownKey calendar_apple_ical_compatible_canceled_instances;
    public static final KnownKey calendar_ics_import_full_parse_max_size;
    public static final KnownKey calendar_ics_export_buffer_size;
    public static final KnownKey calendar_max_desc_in_metadata;
    public static final KnownKey calendar_allow_invite_without_method;
    public static final KnownKey calendar_freebusy_max_days;

    public static final KnownKey calendar_search_max_days;
    public static final KnownKey calendar_cache_enabled;
    public static final KnownKey calendar_cache_directory;
    public static final KnownKey calendar_cache_lru_size;
    public static final KnownKey calendar_cache_range_month_from;
    public static final KnownKey calendar_cache_range_months;
    public static final KnownKey calendar_cache_max_stale_items;

    public static final KnownKey text_attachments_base64;
    
    public static final KnownKey nio_imap_enabled =
        new KnownKey("nio_imap_enabled", "false");

    public static final KnownKey nio_pop3_enabled =
        new KnownKey("nio_pop3_enabled", "false");

    public static final KnownKey nio_lmtp_enabled =
        new KnownKey("nio_lmtp_enabled", "false");
    
    public static final KnownKey imap_max_request_size =
        new KnownKey("imap_max_request_size").setDefault(10 * 1024);
    
    // NIO IMAP configuration settings. Move these to zimbra-attrs.xml once
    // they have been finalized.

    public static final KnownKey nio_imap_min_threads =
        new KnownKey("nio_imap_min_threads").setDefault(20);
    public static final KnownKey nio_imap_max_sessions =
        new KnownKey("nio_imap_max_sessions").setDefault(200);
    public static final KnownKey nio_imap_max_scheduled_write_bytes =
        new KnownKey("nio_imap_max_scheduled_write_bytes").setDefault(1024*1024);
    public static final KnownKey nio_imap_write_timeout =
        new KnownKey("nio_imap_write_timeout").setDefault(60);
    public static final KnownKey nio_imap_write_chunk_size =
        new KnownKey("nio_imap_write_chunk_size").setDefault(8*1024);
    public static final KnownKey nio_imap_thread_keep_alive_time =
        new KnownKey("nio_imap_thread_keep_alive_time").setDefault(60);
    public static final KnownKey data_source_imap_reuse_connections =
        new KnownKey("data_source_imap_reuse_connections", "false");

    public static final KnownKey krb5_keytab;
    public static final KnownKey krb5_service_principal_from_interface_address;
    public static final KnownKey krb5_debug_enabled;
        
    public static final KnownKey zimbra_mtareport_max_users;
    public static final KnownKey zimbra_mtareport_max_hosts;

    public static final KnownKey zmmtaconfig_enable_config_restarts;

    public static final KnownKey zimbra_mailbox_groups;
    
    public static final KnownKey debug_mailboxindex_use_new_locking;

    public static final KnownKey zimbra_class_provisioning;
    public static final KnownKey zimbra_class_accessmanager;
    public static final KnownKey zimbra_class_mboxmanager;
    public static final KnownKey zimbra_class_database;
    public static final KnownKey zimbra_class_store;
    public static final KnownKey zimbra_class_application;
    public static final KnownKey zimbra_class_rulerewriterfactory;
    public static final KnownKey zimbra_class_datasourcemanager;

    // XXX REMOVE AND RELEASE NOTE
    public static final KnownKey data_source_trust_self_signed_certs;
    public static final KnownKey data_source_fetch_size;
    public static final KnownKey data_source_max_message_memory_size;
    public static final KnownKey data_source_new_sync_enabled;
    public static final KnownKey data_source_xsync_class;
    public static final KnownKey data_source_xsync_factory_class;

    public static final KnownKey timezone_file;

    public static final KnownKey search_disable_database_hints;
    public static final KnownKey search_dbfirst_term_percentage_cutoff;

    public static final KnownKey zmstat_log_directory;
    public static final KnownKey zmstat_interval;
    public static final KnownKey zmstat_disk_interval;
    public static final KnownKey zmstat_max_retention;
    
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
    public static final KnownKey zimbra_authtoken_cache_size;
    public static final KnownKey zimbra_authtoken_cookie_domain;
    
    public static final KnownKey zimbra_zmjava_options;
    public static final KnownKey zimbra_zmjava_java_library_path;
    public static final KnownKey zimbra_zmjava_java_ext_dirs;
    
    public static final KnownKey debug_xmpp_disable_client_tls;
    
    public static final KnownKey im_dnsutil_dnsoverride;
    
    public static final KnownKey javamail_pop3_debug;
    public static final KnownKey javamail_imap_debug;
    public static final KnownKey javamail_smtp_debug;
    
    public static final KnownKey javamail_pop3_timeout;
    public static final KnownKey javamail_imap_timeout;
    public static final KnownKey javamail_smtp_timeout;

    public static final KnownKey javamail_pop3_test_timeout;
    public static final KnownKey javamail_imap_test_timeout;
    
    public static final KnownKey javamail_pop3_enable_starttls;
    public static final KnownKey javamail_imap_enable_starttls;
    public static final KnownKey javamail_smtp_enable_starttls;
    
    public static final KnownKey yauth_baseuri;
    
    public static final KnownKey purge_initial_sleep_time;
    public static final KnownKey conversation_max_age_ms;
    public static final KnownKey tombstone_max_age_ms;
    
    public static final KnownKey httpclient_connmgr_max_host_connections;
    public static final KnownKey httpclient_connmgr_max_total_connections;
    public static final KnownKey httpclient_connmgr_keepalive_connections;
    public static final KnownKey httpclient_connmgr_tcp_nodelay;
    public static final KnownKey httpclient_connmgr_connection_timeout;
    public static final KnownKey httpclient_connmgr_so_timeout;
    public static final KnownKey httpclient_client_connection_timeout;
    
    // public static final KnownKey httpclient_connmgr_idle_reaper_initial_sleep_time;  don't use this for now
    public static final KnownKey httpclient_connmgr_idle_reaper_sleep_interval;
    public static final KnownKey httpclient_connmgr_idle_reaper_connection_timeout;
    
    // http client read timeouts
    public static final KnownKey httpclient_soaphttptransport_retry_count;
    public static final KnownKey httpclient_soaphttptransport_so_timeout;
    
    // convertd
    public static final KnownKey httpclient_convertd_so_timeout;
    
    public static final KnownKey client_use_system_proxy;
    public static final KnownKey client_use_native_proxy_selector;
    
    public static final KnownKey shared_mime_info_globs;
    public static final KnownKey shared_mime_info_magic;

    public static final KnownKey xmpp_server_tls_enabled;
    public static final KnownKey xmpp_server_dialback_enabled;
    public static final KnownKey xmpp_server_session_allowmultiple;
    public static final KnownKey xmpp_server_session_idle;
    public static final KnownKey xmpp_server_session_idle_check_time;
    public static final KnownKey xmpp_server_processing_core_threads;
    public static final KnownKey xmpp_server_processing_max_threads;
    public static final KnownKey xmpp_server_processing_queue;
    public static final KnownKey xmpp_server_outgoing_max_threads;
    public static final KnownKey xmpp_server_outgoing_queue;
    public static final KnownKey xmpp_server_read_timeout;
    public static final KnownKey xmpp_server_socket_remoteport;
    public static final KnownKey xmpp_server_compression_policy;
    
    public static final KnownKey xmpp_server_certificate_verify; 
    public static final KnownKey xmpp_server_certificate_verify_chain; 
    public static final KnownKey xmpp_server_certificate_verify_root; 
    public static final KnownKey xmpp_server_certificate_verify_validity; 
    public static final KnownKey xmpp_server_certificate_accept_selfsigned;
    
    public static final KnownKey xmpp_muc_enabled;
    public static final KnownKey xmpp_muc_service_name;
    public static final KnownKey xmpp_muc_discover_locked;
    public static final KnownKey xmpp_muc_restrict_room_creation;
    public static final KnownKey xmpp_muc_room_create_jid_list;
    public static final KnownKey xmpp_muc_unload_empty_hours;
    public static final KnownKey xmpp_muc_sysadmin_jid_list;
    public static final KnownKey xmpp_muc_idle_user_sweep_ms;
    public static final KnownKey xmpp_muc_idle_user_timeout_ms;
    public static final KnownKey xmpp_muc_log_sweep_time_ms;
    public static final KnownKey xmpp_muc_log_batch_size;
    public static final KnownKey xmpp_muc_default_history_type;
    public static final KnownKey xmpp_muc_history_number;
    
    public static final KnownKey xmpp_private_storage_enabled;
    
    public static final KnownKey xmpp_client_compression_policy;
    public static final KnownKey xmpp_client_write_timeout;
    public static final KnownKey xmpp_session_conflict_limit;
    public static final KnownKey xmpp_client_idle_timeout;
    public static final KnownKey xmpp_cloudrouting_idle_timeout;
    
    
    public static final KnownKey xmpp_offline_type;
    public static final KnownKey xmpp_offline_quota;
    
    public static final KnownKey xmpp_dns_override;

    // zmailbox
    public static final KnownKey zmailbox_message_cachesize;
    
    public static final KnownKey freebusy_queue_directory;
    public static final KnownKey contact_ranking_enabled;
    
    public static final KnownKey jdbc_results_streaming_enabled;
    public static final KnownKey smtp_host_retry_millis;
    
    public static final KnownKey freebusy_exchange_cn1;
    public static final KnownKey freebusy_exchange_cn2;
    public static final KnownKey freebusy_exchange_cn3;
    
    public static final KnownKey data_source_scheduling_enabled;
    
    public static final KnownKey notes_enabled;

    public static final KnownKey zimbra_lmtp_validate_messages;
    public static final KnownKey zimbra_lmtp_max_line_length;
    
    public static final KnownKey data_source_eas_sync_email;
    public static final KnownKey data_source_eas_sync_contacts;
    public static final KnownKey data_source_eas_sync_calendar;
    public static final KnownKey data_source_eas_sync_tasks;
    public static final KnownKey data_source_eas_window_size;
    public static final KnownKey data_source_eas_mime_truncation;
    
    public static final KnownKey zimbra_slow_logging_enabled;
    public static final KnownKey zimbra_slow_logging_threshold;

    public static final KnownKey socks_enabled = new KnownKey(
        "socks_enabled", "false", "enable optional support for SOCKS client");
    
    public static final KnownKey socket_connect_timeout = new KnownKey(
        "socket_connect_timeout", "30000", "default socket connect timeout in milliseconds");
    
    public static final KnownKey socket_so_timeout = new KnownKey(
        "socket_so_timeout", "30000", "default socket SO timeout in milliseconds");
    
    public static final KnownKey networkaddress_cache_ttl;

    public static final KnownKey zdesktop_local_account_id = new KnownKey(
        "zdesktop_local_account_id", null, "ZDesktop special local account");

    static {
        @SuppressWarnings("unused")
        final String ZM_MYCNF_CAVEAT = "This value is stored here for use by zmmycnf program.  " +
                "Changing this setting does not immediately reflect in MySQL server.  " +
                "You will have to, with abundant precaution, re-generate my.cnf and " +
                "restart MySQL server for the change to take effect.";
        final String FS = "/";  // Use Unix-style file separator even on Windows.

        // minimize server resources for small servers such as Zimbra Desktop
        zimbra_minimize_resources = new KnownKey("zimbra_minimize_resources", "false");

        zimbra_home = new KnownKey("zimbra_home");
        zimbra_home.setDefault(FS + "opt" + FS + "zimbra");
        zimbra_home.setForceToEdit(true);

        zimbra_java_path = new KnownKey("zimbra_java_path");
        zimbra_java_path.setDefault("java");
        
        zimbra_java_home = new KnownKey("zimbra_java_home");
        if (System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
            zimbra_java_home.setDefault("/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home");
        } else {
            zimbra_java_home.setDefault("${zimbra_home}" + FS + "${zimbra_java_path}");
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
        zimbra_tmp_directory.setDefault("${zimbra_home}" + FS + "data" + FS + "tmp");

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
        zimbra_server_hostname.setDefault("localhost");

        zimbra_user = new KnownKey("zimbra_user");
        zimbra_user.setDefault("zimbra");

        zimbra_uid = new KnownKey("zimbra_uid");
        zimbra_uid.setDefault("-1");

        zimbra_gid = new KnownKey("zimbra_gid");
        zimbra_gid.setDefault("-1");

        zimbra_log4j_properties = new KnownKey("zimbra_log4j_properties");
        zimbra_log4j_properties.setDefault("${zimbra_home}" + FS + "conf" + FS + "log4j.properties");

        zimbra_log4j_properties_watch = new KnownKey("zimbra_log4j_properties_watch", "60000");

        zimbra_attrs_directory = new KnownKey("zimbra_attrs_directory");
        zimbra_attrs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "attrs");
        
        zimbra_rights_directory = new KnownKey("zimbra_rights_directory");
        zimbra_rights_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "rights");

        localized_msgs_directory = new KnownKey("localized_msgs_directory");
        localized_msgs_directory.setDefault("${zimbra_home}" + FS + "conf" + FS + "msgs");

        localized_client_msgs_directory = new KnownKey("localized_client_msgs_directory");
        localized_client_msgs_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "WEB-INF" + FS + "classes" + FS + "messages");

        skins_directory = new KnownKey("skins_directory");
        skins_directory.setDefault("${mailboxd_directory}" + FS + "webapps" + FS + "zimbra" + FS + "skins");

        zimbra_disk_cache_servlet_flush = new KnownKey("zimbra_disk_cache_servlet_flush", "true");
        zimbra_disk_cache_servlet_size = new KnownKey("zimbra_disk_cache_servlet_size", "1000");

        zimbra_store_sweeper_max_age = new KnownKey("zimbra_store_sweeper_max_age");
        zimbra_store_sweeper_max_age.setDefault("480");  // 480 mins = 8 hours

        zimbra_store_copy_buffer_size_kb = new KnownKey("zimbra_store_copy_buffer_size_kb");
        zimbra_store_copy_buffer_size_kb.setDefault("16");  // 16KB

        zimbra_nio_file_copy_chunk_size_kb = new KnownKey("zimbra_nio_file_copy_chunk_size_kb");
        zimbra_nio_file_copy_chunk_size_kb.setDefault("512");  // 512KB

        zimbra_blob_input_stream_buffer_size_kb = new KnownKey("zimbra_blob_input_stream_buffer_size_kb");
        zimbra_blob_input_stream_buffer_size_kb.setDefault("1");  // 1KB

        zimbra_mailbox_manager_hardref_cache = new KnownKey("zimbra_mailbox_manager_hardref_cache");
        zimbra_mailbox_manager_hardref_cache.setDefault("2500");

        zimbra_mailbox_active_cache = new KnownKey("zimbra_mailbox_active_cache");
        zimbra_mailbox_active_cache.setDefault("500");

        zimbra_mailbox_inactive_cache = new KnownKey("zimbra_mailbox_inactive_cache");
        zimbra_mailbox_inactive_cache.setDefault("30");

        zimbra_mailbox_change_checkpoint_frequency = new KnownKey("zimbra_mailbox_change_checkpoint_frequency", "100");

        zimbra_index_factory_classname = new KnownKey("zimbra_index_factory_classname", "", "if set, use the specified classname for the IndexFactory");        
        zimbra_index_max_uncommitted_operations = new KnownKey("zimbra_index_max_uncommitted_operations");
        zimbra_index_max_uncommitted_operations.setDefault("200");

        zimbra_index_lru_size = new KnownKey("zimbra_index_lru_size");
        zimbra_index_lru_size.setDefault("100");

        zimbra_index_idle_flush_time = new KnownKey("zimbra_index_idle_flush_time");
        zimbra_index_idle_flush_time.setDefault("600");

        zimbra_index_sweep_frequency = new KnownKey("zimbra_index_sweep_frequency", "30");
        zimbra_index_completed_pool_size = new KnownKey("zimbra_index_completed_pool_size", "5");
        zimbra_index_flush_pool_size = new KnownKey("zimbra_index_flush_pool_size", "10");
        zimbra_index_reindex_pool_size = new KnownKey("zimbra_index_reindex_pool_size", "10");

        zimbra_index_reader_lru_size  = new KnownKey("zimbra_index_reader_lru_size");
        zimbra_index_reader_lru_size.setDefault("20");

        zimbra_index_reader_idle_flush_time = new KnownKey("zimbra_index_reader_idle_flush_time");
        zimbra_index_reader_idle_flush_time.setDefault("300");

        zimbra_index_reader_idle_sweep_frequency = new KnownKey("zimbra_index_reader_idle_sweep_frequency");
        zimbra_index_reader_idle_sweep_frequency.setDefault("30");

        zimbra_index_deferred_items_delay = new KnownKey("zimbra_index_deferred_items_delay", "10");
        zimbra_index_deferred_items_failure_delay = new KnownKey("zimbra_index_deferred_items_failure_delay", "300");
        
        zimbra_index_max_transaction_bytes = new KnownKey("zimbra_index_max_transaction_bytes", "5000000");
        zimbra_index_max_transaction_items = new KnownKey("zimbra_index_max_transaction_items", "100");
        
        zimbra_index_lucene_autocommit = new KnownKey("zimbra_index_lucene_autocommit", "false");

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
        zimbra_index_lucene_max_terms_per_query = new KnownKey("zimbra_index_lucene_max_terms_per_query", "50000");
        zimbra_index_wildcard_max_terms_expanded = new KnownKey("zimbra_index_wildcard_max_terms_expanded", "20000");
            
        zimbra_index_lucene_nobatch_use_doc_scheduler = new KnownKey("zimbra_index_lucene_nobatch_use_doc_scheduler", "true");
        zimbra_index_lucene_nobatch_min_merge = new KnownKey("zimbra_index_lucene_nobatch_min_merge", "10");
        zimbra_index_lucene_nobatch_max_merge = new KnownKey("zimbra_index_lucene_nobatch_max_merge", Integer.toString(Integer.MAX_VALUE));
        zimbra_index_lucene_nobatch_merge_factor = new KnownKey("zimbra_index_lucene_nobatch_merge_factor", "3");
        zimbra_index_lucene_nobatch_use_compound_file = new KnownKey("zimbra_index_lucene_nobatch_use_compound_file", "true");
        zimbra_index_lucene_nobatch_use_serial_merge_scheduler = new KnownKey("zimbra_index_lucene_nobatch_use_serial_merge_scheduler", "true");
        zimbra_index_lucene_nobatch_max_buffered_docs = new KnownKey("zimbra_index_lucene_nobatch_max_buffered_docs", "200");
        zimbra_index_lucene_nobatch_ram_buffer_size_kb = new KnownKey("zimbra_index_lucene_nobatch_ram_buffer_size_kb", "10240");
        zimbra_index_lucene_nobatch_autocommit = new KnownKey("zimbra_index_lucene_nobatch_autocommit", "false");
        
        zimbra_rights_delegated_admin_supported = new KnownKey("zimbra_rights_delegated_admin_supported", "true");

        zimbra_spam_report_queue_size = new KnownKey("zimbra_spam_report_queue_size");
        zimbra_spam_report_queue_size.setDefault("100");

        zimbra_throttle_op_concurrency = new KnownKey("zimbra_throttle_op_concurrency");
        zimbra_throttle_op_concurrency.setDefault("1000,1000,1000,1000,1000");

        zimbra_web_generate_gzip = new KnownKey("zimbra_web_generate_gzip", "true");

        zimbra_im_chat_flush_time = new KnownKey("zimbra_im_chat_flush_time", "300");
        zimbra_im_chat_close_time = new KnownKey("zimbra_im_chat_close_time", "3600");

        zimbra_http_originating_ip_header = new KnownKey("zimbra_http_originating_ip_header", "X-Forwarded-For");

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

        zimbra_converter_depth_max = new KnownKey("zimbra_converter_depth_max");
        zimbra_converter_depth_max.setDefault("100");
        
        zimbra_ssl_enabled = new KnownKey("zimbra_ssl_enabled", "true");
        stats_img_folder = new KnownKey("stats_img_folder");
        stats_img_folder.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "work");

        soap_fault_include_stack_trace = new KnownKey("soap_fault_include_stack_trace", "true");
        
        soap_response_buffer_size = new KnownKey("soap_response_buffer_size");
        soap_response_buffer_size.setDefault("");
        soap_response_buffer_size.setDoc("the size of the content buffer for sending SOAP http responses." + 
                                         "if not set, use jetty default.");
        
        soap_response_chunked_transfer_encoding_disabled = new KnownKey("soap_response_chunked_transfer_encoding_disabled");
        soap_response_chunked_transfer_encoding_disabled.setDefault("false");
        
        zimbra_servlet_output_stream_buffer_size = new KnownKey("zimbra_servlet_output_stream_buffer_size");
        zimbra_servlet_output_stream_buffer_size.setDefault("5120");
        
        ldap_host = new KnownKey("ldap_host");
        ldap_host.setDefault("");

        ldap_common_loglevel = new KnownKey("ldap_common_loglevel");
        ldap_common_loglevel.setDefault("49152");

        ldap_common_require_tls = new KnownKey("ldap_common_require_tls");
        ldap_common_require_tls.setDefault("0");

        ldap_common_threads = new KnownKey("ldap_common_threads");
        ldap_common_threads.setDefault("8");

        ldap_common_toolthreads = new KnownKey("ldap_common_toolthreads");
        ldap_common_toolthreads.setDefault("1");

        ldap_common_writetimeout = new KnownKey("ldap_common_writetimeout");
        ldap_common_writetimeout.setDefault("0");

        ldap_db_cachefree = new KnownKey("ldap_db_cachefree");
        ldap_db_cachefree.setDefault("1");

        ldap_db_cachesize = new KnownKey("ldap_db_cachesize");
        ldap_db_cachesize.setDefault("10000");

        ldap_db_idlcachesize = new KnownKey("ldap_db_idlcachesize");
        ldap_db_idlcachesize.setDefault("10000");

        ldap_db_dncachesize = new KnownKey("ldap_db_dncachesize");
        ldap_db_dncachesize.setDefault("0");

        ldap_db_shmkey = new KnownKey("ldap_db_shmkey");
        ldap_db_shmkey.setDefault("0");

        ldap_db_checkpoint = new KnownKey("ldap_db_checkpoint");
        ldap_db_checkpoint.setDefault("64 5");

        ldap_accesslog_cachefree = new KnownKey("ldap_accesslog_cachefree");
        ldap_accesslog_cachefree.setDefault("1");

        ldap_accesslog_cachesize = new KnownKey("ldap_accesslog_cachesize");
        ldap_accesslog_cachesize.setDefault("10000");

        ldap_accesslog_idlcachesize = new KnownKey("ldap_accesslog_idlcachesize");
        ldap_accesslog_idlcachesize.setDefault("10000");

        ldap_accesslog_shmkey = new KnownKey("ldap_accesslog_shmkey");
        ldap_accesslog_shmkey.setDefault("0");

        ldap_accesslog_dncachesize = new KnownKey("ldap_accesslog_dncachesize");
        ldap_accesslog_dncachesize.setDefault("0");

        ldap_accesslog_checkpoint = new KnownKey("ldap_accesslog_checkpoint");
        ldap_accesslog_checkpoint.setDefault("64 5");

        ldap_overlay_syncprov_checkpoint = new KnownKey("ldap_overlay_syncprov_checkpoint");
        ldap_overlay_syncprov_checkpoint.setDefault("20 10");

        ldap_overlay_syncprov_sessionlog = new KnownKey("ldap_overlay_syncprov_sessionlog");
        ldap_overlay_syncprov_sessionlog.setDefault("500");

        ldap_overlay_accesslog_logpurge = new KnownKey("ldap_overlay_accesslog_logpurge");
        ldap_overlay_accesslog_logpurge.setDefault("01+00:00  00+04:00");

        ldap_port = new KnownKey("ldap_port");
        ldap_port.setDefault("");

        ldap_url = new KnownKey("ldap_url");
        ldap_url.setDefault("");

        ldap_master_url = new KnownKey("ldap_master_url");
        ldap_master_url.setDefault("");

        ldap_bind_url = new KnownKey("ldap_bind_url");
        ldap_bind_url.setDefault("");

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
        
        ldap_nginx_password = new KnownKey("ldap_nginx_password");
        ldap_nginx_password.setDefault("zmnginx");

        ldap_starttls_supported = new KnownKey("ldap_starttls_supported"); 
        ldap_starttls_supported.setDefault("0");

        ldap_cache_account_maxsize = new KnownKey("ldap_cache_account_maxsize", "20000");

        ldap_cache_account_maxage = new KnownKey("ldap_cache_account_maxage", "15");

        ldap_cache_cos_maxsize = new KnownKey("ldap_cache_cos_maxsize", "100");

        ldap_cache_cos_maxage = new KnownKey("ldap_cache_cos_maxage", "15");

        ldap_cache_domain_maxsize = new KnownKey("ldap_cache_domain_maxsize", "100");

        ldap_cache_domain_maxage = new KnownKey("ldap_cache_domain_maxage", "15");

        ldap_cache_external_domain_maxsize = new KnownKey("ldap_cache_external_domain_maxsize", "2000");

        ldap_cache_external_domain_maxage = new KnownKey("ldap_cache_external_domain_maxage", "15");
        
        ldap_cache_group_maxsize = new KnownKey("ldap_cache_group_maxsize", "2000");

        ldap_cache_group_maxage = new KnownKey("ldap_cache_group_maxage", "15");

        ldap_cache_right_maxsize = new KnownKey("ldap_cache_right_maxsize", "100");

        ldap_cache_right_maxage = new KnownKey("ldap_cache_right_maxage", "15");
        
        ldap_cache_server_maxsize = new KnownKey("ldap_cache_server_maxsize", "100");

        ldap_cache_server_maxage = new KnownKey("ldap_cache_server_maxage", "15");

        ldap_cache_timezone_maxsize = new KnownKey("ldap_cache_timezone_maxsize", "100");
        
        ldap_cache_xmppcomponent_maxsize = new KnownKey("ldap_cache_xmppcomponent_maxsize", "100");
        
        ldap_cache_xmppcomponent_maxage = new KnownKey("ldap_cache_xmppcomponent_maxage", "15");

        ldap_cache_zimlet_maxsize = new KnownKey("ldap_cache_zimlet_maxsize", "100");

        ldap_cache_zimlet_maxage = new KnownKey("ldap_cache_zimlet_maxage", "15");
        
        ldap_cache_reverseproxylookup_domain_maxsize = new KnownKey("ldap_cache_reverseproxylookup_domain_maxsize", "100");

        ldap_cache_reverseproxylookup_domain_maxage = new KnownKey("ldap_cache_reverseproxylookup_domain_maxage", "15");
        
        ldap_cache_reverseproxylookup_server_maxsize = new KnownKey("ldap_cache_reverseproxylookup_server_maxsize", "100");

        ldap_cache_reverseproxylookup_server_maxage = new KnownKey("ldap_cache_reverseproxylookup_server_maxage", "15");
        
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

        mysql_errlogfile = new KnownKey("mysql_errlogfile");
        mysql_errlogfile.setDefault("${zimbra_home}" + FS + "log" + FS + "mysql_error.log");

        mysql_bind_address = new KnownKey("mysql_bind_address");
        mysql_bind_address.setDefault("localhost");

        mysql_port = new KnownKey("mysql_port");
        mysql_port.setDefault("7306");

        mysql_root_password = new KnownKey("mysql_root_password");
        mysql_root_password.setDefault("zimbra");
        mysql_root_password.setForceToEdit(true);

        derby_properties = new KnownKey("derby_properties");
        derby_properties.setDefault("${zimbra_home}" + File.separator + "conf" + File.separator + "derby.properties");

        zimbra_logger_mysql_password = new KnownKey("zimbra_logger_mysql_password");
        zimbra_logger_mysql_password.setDefault("zimbra");
        zimbra_logger_mysql_password.setForceToEdit(true);

        logger_data_directory = new KnownKey("logger_data_directory");
        logger_data_directory.setDefault("${zimbra_home}" + FS + "logger" + FS + "db" + FS + "data");
        logger_zmrrdfetch_port = new KnownKey("logger_zmrrdfetch_port", "10663");

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

        logger_mysql_errlogfile = new KnownKey("logger_mysql_errlogfile");
        logger_mysql_errlogfile.setDefault("${zimbra_home}" + FS + "log" + FS + "logger_mysql_error.log");

        logger_mysql_bind_address = new KnownKey("logger_mysql_bind_address");
        logger_mysql_bind_address.setDefault("localhost");

        logger_mysql_port = new KnownKey("logger_mysql_port");
        logger_mysql_port.setDefault("7307");

        postfix_alias_maps  = new KnownKey("postfix_alias_maps");
        postfix_alias_maps.setDefault("hash:" + FS + "etc" + FS + "aliases");

        postfix_broken_sasl_auth_clients  = new KnownKey("postfix_broken_sasl_auth_clients");
        postfix_broken_sasl_auth_clients.setDefault("yes");

        postfix_bounce_queue_lifetime  = new KnownKey("postfix_bounce_queue_lifetime");
        postfix_bounce_queue_lifetime.setDefault("5d");

        postfix_command_directory  = new KnownKey("postfix_command_directory");
        postfix_command_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin");

        postfix_daemon_directory  = new KnownKey("postfix_daemon_directory");
        postfix_daemon_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "libexec");

        postfix_enable_smtpd_policyd = new KnownKey("postfix_enable_smtpd_policyd");
        postfix_enable_smtpd_policyd.setDefault("no");

        postfix_header_checks  = new KnownKey("postfix_header_checks");
        postfix_header_checks.setDefault("pcre:${zimbra_home}" + FS + "conf" + FS + "postfix_header_checks");

        postfix_mailq_path  = new KnownKey("postfix_mailq_path");
        postfix_mailq_path.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin" + FS + "mailq");

        postfix_manpage_directory  = new KnownKey("postfix_manpage_directory");
        postfix_manpage_directory.setDefault("${zimbra_home}" + FS + "postfix" + FS + "man");

        postfix_newaliases_path  = new KnownKey("postfix_newaliases_path");
        postfix_newaliases_path.setDefault("${zimbra_home}" + FS + "postfix" + FS + "sbin" + FS + "newaliases");

        postfix_policy_time_limit = new KnownKey("postfix_policy_time_limit");
        postfix_policy_time_limit.setDefault("3600");

        postfix_queue_directory  = new KnownKey("postfix_queue_directory");
        postfix_queue_directory.setDefault("${zimbra_home}" + FS + "data" + FS + "postfix" + FS + "spool");

        postfix_smtpd_sasl_authenticated_header  = new KnownKey("postfix_smtpd_sasl_authenticated_header");
        postfix_smtpd_sasl_authenticated_header.setDefault("no");

        postfix_sender_canonical_maps  = new KnownKey("postfix_sender_canonical_maps");
        postfix_sender_canonical_maps.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-scm.cf");

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

        postfix_queue_run_delay  = new KnownKey("postfix_queue_run_delay");
        postfix_queue_run_delay.setDefault("300s");

        postfix_minimal_backoff_time  = new KnownKey("postfix_minimal_backoff_time");
        postfix_minimal_backoff_time.setDefault("300s");

        postfix_maximal_backoff_time  = new KnownKey("postfix_maximal_backoff_time");
        postfix_maximal_backoff_time.setDefault("4000s");

        postfix_lmtp_connection_cache_destinations  = new KnownKey("postfix_lmtp_connection_cache_destinations");
        postfix_lmtp_connection_cache_destinations.setDefault("");

        postfix_lmtp_connection_cache_time_limit  = new KnownKey("postfix_lmtp_connection_cache_time_limit");
        postfix_lmtp_connection_cache_time_limit.setDefault("4s");

        postfix_lmtp_host_lookup  = new KnownKey("postfix_lmtp_host_lookup");
        postfix_lmtp_host_lookup.setDefault("dns");

        postfix_transport_maps  = new KnownKey("postfix_transport_maps");
        postfix_transport_maps.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-transport.cf");

        postfix_propagate_unmatched_extensions  = new KnownKey("postfix_propagate_unmatched_extensions");
        postfix_propagate_unmatched_extensions.setDefault("canonical");

        postfix_virtual_alias_domains  = new KnownKey("postfix_virtual_alias_domains");
        postfix_virtual_alias_domains.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vad.cf");

        postfix_virtual_alias_maps  = new KnownKey("postfix_virtual_alias_maps");
        postfix_virtual_alias_maps.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vam.cf");

        postfix_virtual_mailbox_domains  = new KnownKey("postfix_virtual_mailbox_domains");
        postfix_virtual_mailbox_domains.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmd.cf");

        postfix_virtual_mailbox_maps  = new KnownKey("postfix_virtual_mailbox_maps");
        postfix_virtual_mailbox_maps.setDefault("proxy:ldap:${zimbra_home}" + FS + "conf" + FS + "ldap-vmm.cf");

        postfix_virtual_transport  = new KnownKey("postfix_virtual_transport");
        postfix_virtual_transport.setDefault("error");

        sqlite_cache_size = new KnownKey("sqlite_cache_size", "4000");
        sqlite_journal_mode = new KnownKey("sqlite_journal_mode", "PERSIST");
        sqlite_page_size = new KnownKey("sqlite_page_size", "2048");
        sqlite_sync_mode = new KnownKey("sqlite_sync_mode", "NORMAL");
        
        mailboxd_directory = new KnownKey("mailboxd_directory");
        mailboxd_directory.setDefault("${zimbra_home}" + FS + "mailboxd");

        mailboxd_java_heap_memory_percent = new KnownKey("mailboxd_java_heap_memory_percent");
        mailboxd_java_heap_memory_percent.setDefault("30");

        mailboxd_thread_stack_size = new KnownKey("mailboxd_thread_stack_size");
        mailboxd_thread_stack_size.setDefault("256k");

        mailboxd_java_heap_new_size_percent = new KnownKey("mailboxd_java_heap_new_size_percent");
        mailboxd_java_heap_new_size_percent.setDefault("25");
        
        mailboxd_java_options = new KnownKey("mailboxd_java_options");
        mailboxd_java_options.setDefault("-server -Djava.awt.headless=true -Dsun.net.inetaddr.ttl=${networkaddress_cache_ttl} -XX:+UseConcMarkSweepGC -XX:NewRatio=2 -XX:PermSize=128m -XX:MaxPermSize=128m -XX:SoftRefLRUPolicyMSPerMB=1 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime");

        mailboxd_pidfile = new KnownKey("mailboxd_pidfile");
        mailboxd_pidfile.setDefault("${zimbra_log_directory}" + FS + "mailboxd.pid");

        mailboxd_keystore = new KnownKey("mailboxd_keystore");
        mailboxd_keystore.setDefault("${mailboxd_directory}" + FS + "etc" + FS + "keystore");

        mailboxd_keystore_password = new KnownKey("mailboxd_keystore_password");
        mailboxd_keystore_password.setDefault("zimbra");

        mailboxd_keystore_base = new KnownKey("mailboxd_keystore_base");
        mailboxd_keystore_base.setDefault("${zimbra_home}" + FS + "conf" + FS + "keystore.base");
        
        mailboxd_keystore_base_password = new KnownKey("mailboxd_keystore_base_password");
        mailboxd_keystore_base_password.setDefault("zimbra");
        
        mailboxd_truststore = new KnownKey("mailboxd_truststore");
        mailboxd_truststore.setDefault("${zimbra_java_home}" + FS + "lib" + FS + "security" + FS + "cacerts");

        mailboxd_truststore_password = new KnownKey("mailboxd_truststore_password");
        mailboxd_truststore_password.setDefault("changeit");

        mailboxd_output_filename = new KnownKey("mailboxd_output_filename");
        mailboxd_output_filename.setDefault("zmmailboxd.out");

        mailboxd_output_file = new KnownKey("mailboxd_output_file");
        mailboxd_output_file.setDefault("${zimbra_log_directory}" + FS + "${mailboxd_output_filename}");

        mailboxd_output_rotate_interval = new KnownKey("mailboxd_output_rotate_interval");
        mailboxd_output_rotate_interval.setDefault("86400");

        ssl_allow_untrusted_certs = new KnownKey("ssl_allow_untrusted_certs")
            .setDoc("allow untrusted certificates")
            .setDefault("false");

        ssl_allow_mismatched_certs = new KnownKey("ssl_allow_mismatched_certs")
            .setDoc("allow mismatched certificates (disable hostname verification")
            .setDefault("true");
        
        ssl_allow_accept_untrusted_certs = new KnownKey("ssl_allow_accept_untrusted_certs")
            .setDoc("allow user to accept untrusted certificates")
            .setDefault("true");
        
        zimlet_directory = new KnownKey("zimlet_directory");
        zimlet_directory.setDefault("${zimbra_home}" + FS + "zimlets-deployed");

        wiki_enabled = new KnownKey("wiki_enabled");
        wiki_enabled.setDefault("false");

        wiki_user = new KnownKey("wiki_user");
        wiki_user.setDefault("wiki");

        calendar_outlook_compatible_allday_events = new KnownKey("calendar_outlook_compatible_allday_events");
        calendar_outlook_compatible_allday_events.setDefault("true");

        calendar_entourage_compatible_timezones = new KnownKey("calendar_entourage_compatible_timezones");
        calendar_entourage_compatible_timezones.setDefault("true");

        calendar_apple_ical_compatible_canceled_instances = new KnownKey("calendar_apple_ical_compatible_canceled_instances");
        calendar_apple_ical_compatible_canceled_instances.setDefault("true");

        calendar_ics_import_full_parse_max_size = new KnownKey("calendar_ics_import_full_parse_max_size");
        calendar_ics_import_full_parse_max_size.setDefault("131072");  // 128KB

        calendar_ics_export_buffer_size = new KnownKey("calendar_ics_export_buffer_size");
        calendar_ics_export_buffer_size.setDefault("131072");  // 128KB

        calendar_max_desc_in_metadata = new KnownKey("calendar_max_desc_in_metadata");
        calendar_max_desc_in_metadata.setDefault("4096");  // 4KB

        calendar_allow_invite_without_method = new KnownKey("calendar_allow_invite_without_method");
        calendar_allow_invite_without_method.setDefault("false");

        calendar_freebusy_max_days = new KnownKey("calendar_freebusy_max_days");
        calendar_freebusy_max_days.setDefault("366");

        calendar_search_max_days = new KnownKey("calendar_search_max_days");
        calendar_search_max_days.setDefault("400");

        calendar_cache_enabled = new KnownKey("calendar_cache_enabled");
        calendar_cache_enabled.setDefault("true");

        calendar_cache_directory = new KnownKey("calendar_cache_directory");
        calendar_cache_directory.setDefault("${" + zimbra_tmp_directory.key() + "}" + FS + "calcache");

        calendar_cache_lru_size = new KnownKey("calendar_cache_lru_size");
        calendar_cache_lru_size.setDefault("1000");

        calendar_cache_range_month_from = new KnownKey("calendar_cache_range_month_from");
        calendar_cache_range_month_from.setDefault("0");

        calendar_cache_range_months = new KnownKey("calendar_cache_range_months");
        calendar_cache_range_months.setDefault("3");

        calendar_cache_max_stale_items = new KnownKey("calendar_cache_max_stale_items");
        calendar_cache_max_stale_items.setDefault("10");

        krb5_keytab = new KnownKey("krb5_keytab");
        krb5_keytab.setDefault("${zimbra_home}" + FS + "conf" + FS + "krb5.keytab");
        
        krb5_service_principal_from_interface_address = new KnownKey("krb5_service_principal_from_interface_address");
        krb5_service_principal_from_interface_address.setDefault("false");

        krb5_debug_enabled = new KnownKey("krb5_debug_enabled");
        krb5_debug_enabled.setDefault("false");

        zimbra_mtareport_max_users = new KnownKey("zimbra_mtareport_max_users");
        zimbra_mtareport_max_users.setDefault("50");

        zimbra_mtareport_max_hosts = new KnownKey("zimbra_mtareport_max_hosts");
        zimbra_mtareport_max_hosts.setDefault("50");

	zmmtaconfig_enable_config_restarts = new KnownKey("zmmtaconfig_enable_config_restarts");
	zmmtaconfig_enable_config_restarts.setDefault("TRUE");

        zimbra_auth_always_send_refer = new KnownKey("zimbra_auth_always_send_refer");
        zimbra_auth_always_send_refer.setDefault("false");

        zimbra_admin_service_port = new KnownKey("zimbra_admin_service_port", "7071");

        zimbra_admin_service_scheme = new KnownKey("zimbra_admin_service_scheme", "https://");

        zimbra_zmprov_default_to_ldap = new KnownKey("zimbra_zmprov_default_to_ldap", "false");

        zimbra_zmprov_default_soap_server = new KnownKey("zimbra_zmprov_default_soap_server", "localhost");

        zimbra_require_interprocess_security = new KnownKey("zimbra_require_interprocess_security");
        zimbra_require_interprocess_security.setDefault("1");

        zimbra_relative_volume_path = new KnownKey("zimbra_relative_volume_path");
        zimbra_relative_volume_path.setDefault("false");
        
        zimbra_mailbox_groups = new KnownKey("zimbra_mailbox_groups");
        zimbra_mailbox_groups.setDefault("100");

        debug_mailboxindex_use_new_locking = new KnownKey("debug_mailboxindex_use_new_locking");
        debug_mailboxindex_use_new_locking.setDefault("true");

        zimbra_class_provisioning = new KnownKey("zimbra_class_provisioning", "com.zimbra.cs.account.ldap.LdapProvisioning");
        zimbra_class_accessmanager = new KnownKey("zimbra_class_accessmanager", "com.zimbra.cs.account.accesscontrol.ACLAccessManager");
        zimbra_class_mboxmanager = new KnownKey("zimbra_class_mboxmanager", "com.zimbra.cs.mailbox.MailboxManager");
        zimbra_class_database = new KnownKey("zimbra_class_database", "com.zimbra.cs.db.MySQL");
        zimbra_class_store = new KnownKey("zimbra_class_store", "com.zimbra.cs.store.file.FileBlobStore");
        zimbra_class_application = new KnownKey("zimbra_class_application", "com.zimbra.cs.util.ZimbraApplication");
        zimbra_class_rulerewriterfactory = new KnownKey("zimbra_class_rulerewriterfactory", "com.zimbra.cs.filter.RuleRewriterFactory");
        zimbra_class_datasourcemanager = new KnownKey("zimbra_class_datasourcemanager", "com.zimbra.cs.datasource.DataSourceManager");
        
        data_source_trust_self_signed_certs = new KnownKey("data_source_trust_self_signed_certs", "false");
        data_source_fetch_size = new KnownKey("data_source_fetch_size", "5");
        data_source_max_message_memory_size = new KnownKey("data_source_max_message_memory_size", "2097152"); // 2 megabytes
        data_source_new_sync_enabled = new KnownKey("data_source_new_sync_enabled", "false");
        data_source_xsync_class = new KnownKey("data_source_xsync_class", "");
        data_source_xsync_factory_class = new KnownKey("data_source_xsync_factory_class", "");

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
        
        zmstat_disk_interval = new KnownKey("zmstat_disk_interval");
        zmstat_disk_interval.setDefault("600");

        zmstat_max_retention = new KnownKey("zmstat_max_retention");
        zmstat_max_retention.setDefault("0");

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
        
        zimbra_waitset_initial_sleep_time  = new KnownKey("zimbra_waitset_initial_sleep_time", "1000");
        zimbra_waitset_nodata_sleep_time = new KnownKey("zimbra_waitset_nodata_sleep_time", "3000");

        zimbra_csv_mapping_file = new KnownKey("zimbra_csv_mapping_file");
        zimbra_csv_mapping_file.setDefault("${zimbra_home}" + FS + "conf" + FS + "zimbra-contact-fields.xml");

        zimbra_auth_provider = new KnownKey("zimbra_auth_provider");
        zimbra_auth_provider.setDefault("zimbra");
        zimbra_authtoken_cache_size = new KnownKey("zimbra_authtoken_cache_size", "5000");
        zimbra_authtoken_cookie_domain = new KnownKey("zimbra_authtoken_cookie_domain");
        zimbra_authtoken_cookie_domain.setDefault("");
        
        zimbra_zmjava_options = new KnownKey("zimbra_zmjava_options", "-Xmx256m");
        zimbra_zmjava_java_library_path = new KnownKey("zimbra_zmjava_java_library_path", "");
        zimbra_zmjava_java_ext_dirs = new KnownKey("zimbra_zmjava_java_ext_dirs", "");
        
        debug_xmpp_disable_client_tls = new KnownKey("debug_xmpp_disable_client_tls", "0");
        
        im_dnsutil_dnsoverride = new KnownKey("im_dnsutil_dnsoverride", "");
        
        javamail_pop3_debug = new KnownKey("javamail_pop3_debug");
        javamail_pop3_debug.setDefault("false");

        javamail_imap_debug = new KnownKey("javamail_imap_debug");
        javamail_imap_debug.setDefault("false");

        javamail_smtp_debug = new KnownKey("javamail_smtp_debug");
        javamail_smtp_debug.setDefault("false");

        javamail_pop3_timeout = new KnownKey("javamail_pop3_timeout", "60");
        javamail_imap_timeout = new KnownKey("javamail_imap_timeout", "60");

        javamail_pop3_test_timeout = new KnownKey("javamail_pop3_test_timeout", "20");
        javamail_imap_test_timeout = new KnownKey("javamail_imap_test_timeout", "20");
        
        javamail_smtp_timeout = new KnownKey("javamail_smtp_timeout");
        javamail_smtp_timeout.setDefault("60");

        javamail_pop3_enable_starttls = new KnownKey("javamail_pop3_enable_starttls");
        javamail_pop3_enable_starttls.setDefault("true");

        javamail_imap_enable_starttls = new KnownKey("javamail_imap_enable_starttls");
        javamail_imap_enable_starttls.setDefault("true");

        javamail_smtp_enable_starttls = new KnownKey("javamail_smtp_enable_starttls");
        javamail_smtp_enable_starttls.setDefault("true");
        
	    yauth_baseuri = new KnownKey("yauth_baseuri");
	    yauth_baseuri.setDefault("https://login.yahoo.com/WSLogin/V1");
	    yauth_baseuri.setDoc("base uri for yauth");
        
        purge_initial_sleep_time = new KnownKey(
            "purge_initial_sleep_ms", Long.toString(30 * Constants.MILLIS_PER_MINUTE),
            "Amount of time (in milliseconds) that the purge thread sleeps on startup before doing work.");
        
        conversation_max_age_ms = new KnownKey("conversation_max_age_ms", Long.toString(31 * Constants.MILLIS_PER_DAY));
        tombstone_max_age_ms = new KnownKey("tombstone_max_age_ms", Long.toString(3 * Constants.MILLIS_PER_MONTH));
        
        httpclient_connmgr_max_host_connections = new KnownKey(
                "httpclient_connmgr_max_host_connections", 
                "100",
                "httpclient connection manager: " + 
                "Defines the maximum number of connections allowed per host configuration");
        
        httpclient_connmgr_max_total_connections = new KnownKey(
                "httpclient_connmgr_max_total_connections", 
                "300",
                "httpclient connection manager: " +
                "Defines the maximum number of connections allowed overall");

        httpclient_connmgr_keepalive_connections = new KnownKey(
                "httpclient_connmgr_keepalive_connections", 
                "true",
                "httpclient connection manager: " +
                "Defines whether HTTP keep-alive connections should be used");

        httpclient_connmgr_tcp_nodelay = new KnownKey(
                "httpclient_connmgr_tcp_nodelay", 
                "false",
                "httpclient connection manager: " +
                "Defines whether to disable Nagle algorithm on HTTP socket");

        httpclient_connmgr_connection_timeout = new KnownKey(
                "httpclient_connmgr_connection_timeout", 
                Long.toString(25 * Constants.MILLIS_PER_SECOND),
                "httpclient connection manager: " +
                "Determines the timeout until a connection is established. A value of zero means the timeout is not used");
        
        httpclient_connmgr_so_timeout = new KnownKey(
                "httpclient_connmgr_so_timeout", 
                Long.toString(60 * Constants.MILLIS_PER_SECOND),
                "httpclient connection manager: " +
                "A timeout value of zero is interpreted as an infinite timeout. This value is used when no socket timeout is set in the HTTP method parameters");

        httpclient_client_connection_timeout = new KnownKey(
                "httpclient_client_connection_timeout", 
                Long.toString(30 * Constants.MILLIS_PER_SECOND),
                "httpclient client: " +
                "Sets the timeout in milliseconds used when retrieving an HTTP connection from the HTTP connection manager. ");
        
        httpclient_connmgr_idle_reaper_sleep_interval = new KnownKey(
                "httpclient_connmgr_idle_reaper_sleep_interval", 
                Long.toString(5 * Constants.MILLIS_PER_MINUTE),
                "httpclient connection manager idle reaper: " +
                "Amount of time (in milliseconds) that the http client connection manager idle connection reaper thread sleeps between doing work. " +
                "0 means that reaper thread is disabled");
        
        httpclient_connmgr_idle_reaper_connection_timeout = new KnownKey(
                "httpclient_connmgr_idle_reaper_connection_timeout", 
                Long.toString(5 * Constants.MILLIS_PER_MINUTE),
                "httpclient connection manager idle reaper: " +
                "the timeout value to use when testing for idle connections.");

        
        httpclient_soaphttptransport_retry_count = new KnownKey(
                "httpclient_soaphttptransport_retry_count", 
                "2",
                "Defines the number retries after a temporary failure for SOAP clients using the SoapHttpTransport class");
        
        httpclient_soaphttptransport_so_timeout = new KnownKey(
                "httpclient_soaphttptransport_so_timeout", 
                Long.toString(300 * Constants.MILLIS_PER_SECOND),
                "socket timeout in milliseconds for SOAP clients using the SoapHttpTransport class");
        
        httpclient_convertd_so_timeout = new KnownKey(
                "httpclient_convertd_so_timeout", 
                Long.toString(-1),
                "socket timeout in milliseconds for convertd client." + 
                "if 0 - means no timeout." + 
                "if -1 or not set - means use the default read timeout of the connection manager.");
        
        client_use_system_proxy = new KnownKey("client_use_system_proxy", "false",
                "whether to use system proxies");
        
        client_use_native_proxy_selector = new KnownKey("client_use_native_proxy_selector", "false",
                "whether to use native code for reading system proxy data");
        
        shared_mime_info_globs = new KnownKey("shared_mime_info_globs",
            "${zimbra_home}" + FS + "conf" + FS + "globs2",
            "freedesktop.org shared-mime-info glob file");
        shared_mime_info_magic = new KnownKey("shared_mime_info_magic",
            "${zimbra_home}" + FS + "conf" + FS + "magic",
            "freedesktop.org shared-mime-info magic file");
        
        xmpp_server_tls_enabled = new KnownKey("xmpp_server_tls_enabled","true", "Allow TLS for S2S connections"); 
        xmpp_server_dialback_enabled = new KnownKey("xmpp_server_dialback_enabled", "true", "Allow S2S Server Dialback Protocol");
        xmpp_server_session_allowmultiple = new KnownKey("xmpp_server_session_allowmultiple","true", "Allow multiple simultaneous S2S connections from a given host");
        xmpp_server_session_idle = new KnownKey("xmpp_server_session_idle",Integer.toString(20*60*1000), "Timeout for idle S2S connections"); 
        xmpp_server_session_idle_check_time = new KnownKey("xmpp_server_session_idle_check_time",Integer.toString(5*60*1000), "How frequently we check for idle server connections"); 
        xmpp_server_processing_core_threads = new KnownKey("xmpp_server_processing_core_threads","2", "Core S2S processing threads"); 
        xmpp_server_processing_max_threads = new KnownKey("xmpp_server_processing_max_threads", "50", "Max S2S processing threads"); 
        xmpp_server_processing_queue = new KnownKey("xmpp_server_processing_queue","50", "Length of S2S processing queue"); 
        xmpp_server_outgoing_max_threads = new KnownKey("xmpp_server_outgoing_max_threads", "20", "Max threads in pool for outgoing S2S"); 
        xmpp_server_outgoing_queue = new KnownKey("xmpp_server_outgoing_queue", "50", "Queue length for outgoing S2S queue"); 
        xmpp_server_read_timeout = new KnownKey("xmpp_server_read_timeout", Integer.toString(3*60*1000), "Read timeout for S2S (how long will we wait for the remote server to answer?)"); 
        xmpp_server_socket_remoteport = new KnownKey("xmpp_server_socket_remoteport","5269", "Remote port to connect to for outgoing S2S"); 
        xmpp_server_compression_policy = new KnownKey("xmpp_server_compression_policy", "disabled", "S2S compression optional|disabled"); 

        xmpp_server_certificate_verify = new KnownKey("xmpp_server_certificate_verify", "false", 
        "XMPP server-to-server: master enable/disable SSL certificate checking (currently BROKEN, do not enable)");
        
        xmpp_server_certificate_verify_chain = new KnownKey("xmpp_server_certificate_verify_chain", "true",
        "XMPP server-to-server: enable SSL certificate checking for entire chain of certificates (only if certificate_verify is true)");
        
        xmpp_server_certificate_verify_root = new KnownKey("xmpp_server_certificate_verify_root", "true",
        "XMPP server-to-server: enable SSL certificate checking for root certificate (only if certificate_verify is true)");                                                    
                                                           
        xmpp_server_certificate_verify_validity = new KnownKey("xmpp_server_certificate_verify_validity", "true",
        "XMPP server-to-server: check to see if every certificate is valid at the current time (only if certificate_verify is true");                                                      
                                                               
        xmpp_server_certificate_accept_selfsigned = new KnownKey("xmpp_server_certificate_accept_selfsigned", "true",
        "XMPP server-to-server: accept self-signed certificates from remote servers");

        xmpp_muc_enabled = new KnownKey("xmpp_muc_enabled", "true", "Enable the XMPP Multi-User-Chat service?");
        xmpp_muc_service_name = new KnownKey("xmpp_muc_service_name", "conference", "XMPP name for the Multi-User-Chat service");
        xmpp_muc_discover_locked = new KnownKey("xmpp_muc_discover_locked", "true", "Should MUC Disco requests return locked rooms?"); 
        xmpp_muc_restrict_room_creation = new KnownKey("xmpp_muc_restrict_room_creation", "Should MUC room creation be restricted to only those JIDs listed in xmpp_muc_room_create_jid_list?");
        xmpp_muc_room_create_jid_list = new KnownKey("xmpp_muc_room_create_jid_list", "", "Comma-Separated List of JIDs that are allowed to create MUC rooms");  
        xmpp_muc_unload_empty_hours = new KnownKey("xmpp_muc_unload_empty_hours", "5", "Number of hours MUC will remain empty before it is unloaded by the system.  Persistent MUCs are not deleted, just unloaded"); 
        xmpp_muc_sysadmin_jid_list = new KnownKey("xmpp_muc_sysadmin_jid_list", "", " Temporary: List of JIDs that have sysadmin access for IM Multi-User-Chat"); // FIXME tim will remove this soon
        xmpp_muc_idle_user_sweep_ms = new KnownKey("xmpp_muc_idle_user_sweep_ms", Long.toString(5 * Constants.MILLIS_PER_MINUTE), "XMPP Multi-User-Chat: How frequently to sweep MUC for idle users");
        xmpp_muc_idle_user_timeout_ms = new KnownKey("xmpp_muc_idle_user_timeout_ms", "0", "XMPP Multi-User-Chat: The number of milliseconds a user must be idle before he/she gets kicked from all the rooms.");
        xmpp_muc_log_sweep_time_ms = new KnownKey("xmpp_muc_log_sweep_time_ms", Long.toString(5 * Constants.MILLIS_PER_MINUTE), "XMPP Multi-User Chat: How frequently to log messages from active rooms (if the room is configured to log)");
        xmpp_muc_log_batch_size = new KnownKey("xmpp_muc_log_batch_size", "50", "XMPP Multi-User-Chat: The number of messages to log on each run of the logging process.");
        xmpp_muc_default_history_type = new KnownKey("xmpp_muc_default_history_type", "number", "XMPP Multi-User-Chat: Default log setting for MUC rooms: none|all|number");
        xmpp_muc_history_number = new KnownKey("xmpp_muc_history_number", "25", "XMPP Multi-User-Chat: Default number of chat messages to save (if history type is number)");

        xmpp_private_storage_enabled = new KnownKey("xmpp_private_storage_enabled", "true", "XMPP: Support private storage (XEP-0049)");
        
        xmpp_client_compression_policy = new KnownKey("xmpp_client_compression_policy", "optional", "XMPP C2S compression optional|disabled");
        xmpp_client_write_timeout = new KnownKey("xmpp_client_write_timeout", Long.toString(60 * Constants.MILLIS_PER_SECOND), "Timeout for client socket blocked on a write");
        xmpp_session_conflict_limit = new KnownKey("xmpp_session_conflict_limit", "0", "Conflict Limit for XMPP C2S sessions");
        xmpp_client_idle_timeout = new KnownKey("xmpp_client_idle_timeout", Integer.toString(10 * 60 * 1000), "XMPP Client idle timeout");
        xmpp_cloudrouting_idle_timeout = new KnownKey("xmpp_cloudrouting_idle_timeout", Long.toString(5 * Constants.MILLIS_PER_MINUTE), "CloudRouting socket idle timeout");
        
        xmpp_offline_type = new KnownKey("xmpp_offline_type", "store_and_drop", "What to do with messages received by offline users: drop|bounce|store|store_and_bounce|store_and_drop");
        xmpp_offline_quota = new KnownKey("xmpp_offline_quota", Integer.toString(100 * 1024), "Maximum number of bytes of offline messages stored (if type is store_and_bounce or store_and_drop)");
        
        xmpp_dns_override = new KnownKey("xmpp_dns_override", "", "Override DNS for XMPP server, comma-separated list of entries of the form \"{domain:hostname:port}\"");         
        
        zmailbox_message_cachesize = new KnownKey("zmailbox_message_cachesize", "1", "max number of messages cached in zmailbox");
              
        freebusy_queue_directory = new KnownKey("freebusy_queue_directory");
        freebusy_queue_directory.setDefault("${zimbra_home}" + FS + "fbqueue" + FS);
        contact_ranking_enabled = new KnownKey("contact_ranking_enabled", "true", "Enable contact ranking table");
        
        jdbc_results_streaming_enabled = new KnownKey("jdbc_results_streaming_enabled", "true");
        smtp_host_retry_millis = new KnownKey("smtp_host_retry_millis", "60000");
        
        freebusy_exchange_cn1 = new KnownKey("freebusy_exchange_cn1");
        freebusy_exchange_cn2 = new KnownKey("freebusy_exchange_cn2");
        freebusy_exchange_cn3 = new KnownKey("freebusy_exchange_cn3");

        zimbra_lmtp_validate_messages = new KnownKey("zimbra_lmtp_validate_messages", "true");
        zimbra_lmtp_max_line_length = new KnownKey("zimbra_lmtp_max_line_length", "10240");
        
        data_source_scheduling_enabled = new KnownKey("data_source_scheduling_enabled", "true");
        
        notes_enabled = new KnownKey("notes_enabled", "false");
        
        data_source_eas_sync_email = new KnownKey("data_source_eas_sync_email", "true");
        data_source_eas_sync_contacts = new KnownKey("data_source_eas_sync_contacts", "true");
        data_source_eas_sync_calendar = new KnownKey("data_source_eas_sync_calendar", "true");
        data_source_eas_sync_tasks = new KnownKey("data_source_eas_sync_tasks", "true");
        data_source_eas_window_size = new KnownKey("data_source_eas_window_size", "50");
        data_source_eas_mime_truncation = new KnownKey("data_source_eas_mime_truncation", "4");
        
        zimbra_slow_logging_enabled = new KnownKey("zimbra_slow_logging_enabled", "false");
        zimbra_slow_logging_threshold = new KnownKey("zimbra_slow_logging_threshold", "5000");
        
        
        networkaddress_cache_ttl = new KnownKey("networkaddress_cache_ttl");
        networkaddress_cache_ttl.setDefault("60");
        networkaddress_cache_ttl.setDoc("Value for the java.security.Security networkaddress.cache.ttl property, " +
                "set through -Dsun.net.inetaddr.ttl JVM system property. " +
                "Number of seconds to cache successful hostname-to-IP address lookup from the name service. ");

        text_attachments_base64 = new KnownKey("text_attachments_base64", "true");
        
		// NOTE: When adding a new KnownKey, you do not need to call
		//       setDoc. The documentation string will come from the
		//       ZsMsg properties file, using the same key as the
		//       KnownKey.
		//       You can still use setDoc but it is NOT recommended
		//       because it will not be able to be translated.
    }
}
