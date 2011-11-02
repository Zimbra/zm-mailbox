/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.dom4j.DocumentException;

import com.google.common.base.Strings;
import com.zimbra.common.util.Constants;

/**
 * Provides convenient means to get at local configuration - stuff that we do
 * not want in LDAP.
 * <p>
 * NOTE: When adding a new KnownKey, you do not need to call setDoc. The
 * documentation string will come from the ZsMsg properties file, using the same
 * key as the KnownKey. You can still use setDoc but it is NOT recommended
 * because it will not be able to be translated.
 */
public final class LC {

    public static String get(String key) {
        try {
            return Strings.nullToEmpty(LocalConfig.getInstance().get(key));
        } catch (ConfigException never) {
            assert false : never;
            return "";
        }
    }

    public static String[] getAllKeys() {
        return LocalConfig.getInstance().allKeys();
    }

    /**
     * Reloads the local config file.
     *
     * @throws DocumentException if the config file was syntactically invalid
     * @throws ConfigException if the config file was semantically invalid
     */
    public static void reload() throws DocumentException, ConfigException {
        LocalConfig.load(null);
    }

    static void init() {
        // This method is there to guarantee static initializer of this
        // class is run.
    }


    public static final KnownKey zimbra_minimize_resources = KnownKey.newKey(false);

    @Supported
    public static final KnownKey zimbra_home = KnownKey.newKey("/opt/zimbra").protect();
    public static final KnownKey zimbra_java_path = KnownKey.newKey("java");
    @Supported
    public static final KnownKey zimbra_java_home = KnownKey.newKey( System.getProperty("os.name").equalsIgnoreCase("Mac OS X") ?
            "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" : "${zimbra_home}/${zimbra_java_path}");

    @Supported
    public static final KnownKey zimbra_log_directory = KnownKey.newKey("${zimbra_home}/log");

    @Supported
    public static final KnownKey zimbra_index_directory = KnownKey.newKey("${zimbra_home}/index");

    @Supported
    public static final KnownKey zimbra_store_directory = KnownKey.newKey("${zimbra_home}/store");

    @Supported
    public static final KnownKey zimbra_db_directory = KnownKey.newKey("${zimbra_home}/db");

    @Supported
    public static final KnownKey zimbra_tmp_directory = KnownKey.newKey("${zimbra_home}/data/tmp");

    @Supported
    public static final KnownKey zimbra_extension_directory = KnownKey.newKey("${zimbra_home}/lib/ext");

    @Supported
    public static final KnownKey zimbra_extension_common_directory = KnownKey.newKey("${zimbra_home}/lib/ext-common");

    @Supported
    public static final KnownKey zimbra_mysql_user = KnownKey.newKey("zimbra");

    @Supported
    public static final KnownKey zimbra_mysql_password = KnownKey.newKey("zimbra").protect();

    @Supported
    public static final KnownKey zimbra_ldap_userdn = KnownKey.newKey("uid=zimbra,cn=admins,cn=zimbra");

    @Supported
    public static final KnownKey zimbra_ldap_user = KnownKey.newKey("zimbra");

    @Supported
    public static final KnownKey zimbra_ldap_password = KnownKey.newKey("zimbra").protect();

    @Supported
    public static final KnownKey zimbra_server_hostname = KnownKey.newKey("localhost");

    @Supported
    public static final KnownKey zimbra_attrs_directory = KnownKey.newKey("${zimbra_home}/conf/attrs");
    public static final KnownKey zimbra_rights_directory = KnownKey.newKey("${zimbra_home}/conf/rights");

    @Supported
    public static final KnownKey zimbra_user = KnownKey.newKey("zimbra");

    @Supported
    public static final KnownKey zimbra_uid = KnownKey.newKey(-1);

    @Supported
    public static final KnownKey zimbra_gid = KnownKey.newKey(-1);

    @Supported
    public static final KnownKey zimbra_log4j_properties = KnownKey.newKey("${zimbra_home}/conf/log4j.properties");

    @Supported
    public static final KnownKey zimbra_auth_always_send_refer = KnownKey.newKey(false);

    @Supported
    public static final KnownKey zimbra_admin_service_port = KnownKey.newKey(7071);

    @Supported
    public static final KnownKey zimbra_admin_service_scheme = KnownKey.newKey("https://");

    @Supported
    public static final KnownKey zimbra_zmprov_default_to_ldap = KnownKey.newKey(false);

    @Supported
    public static final KnownKey zimbra_zmprov_default_soap_server = KnownKey.newKey("localhost");
    public static final KnownKey zimbra_require_interprocess_security = KnownKey.newKey(1);
    public static final KnownKey zimbra_relative_volume_path = KnownKey.newKey(false);

    @Supported
    public static final KnownKey localized_msgs_directory = KnownKey.newKey("${zimbra_home}/conf/msgs");

    @Supported
    public static final KnownKey localized_client_msgs_directory =
        KnownKey.newKey("${mailboxd_directory}/webapps/zimbra/WEB-INF/classes/messages");

    @Supported
    public static final KnownKey skins_directory = KnownKey.newKey("${mailboxd_directory}/webapps/zimbra/skins");
    public static final KnownKey zimbra_disk_cache_servlet_flush = KnownKey.newKey(true);
    public static final KnownKey zimbra_disk_cache_servlet_size = KnownKey.newKey(1000);

    @Supported
    public static final KnownKey zimbra_store_sweeper_max_age = KnownKey.newKey(480); // 480 mins = 8 hours

    @Supported
    public static final KnownKey zimbra_store_copy_buffer_size_kb = KnownKey.newKey(16); // KB
    public static final KnownKey zimbra_nio_file_copy_chunk_size_kb = KnownKey.newKey(512); // KB
    public static final KnownKey zimbra_blob_input_stream_buffer_size_kb = KnownKey.newKey(1); // KB

    @Supported
    public static final KnownKey zimbra_mailbox_manager_hardref_cache = KnownKey.newKey(2500);

    @Supported
    public static final KnownKey zimbra_mailbox_active_cache = KnownKey.newKey(500);

    @Supported
    public static final KnownKey zimbra_mailbox_inactive_cache = KnownKey.newKey(30);

    @Supported
    public static final KnownKey zimbra_mailbox_change_checkpoint_frequency = KnownKey.newKey(100);

    @Reloadable
    public static final KnownKey zimbra_mailbox_lock_max_waiting_threads = KnownKey.newKey(15);

    @Reloadable
    public static final KnownKey zimbra_mailbox_lock_timeout = KnownKey.newKey(60); // seconds

    @Supported
    public static final KnownKey zimbra_index_threads = KnownKey.newKey(10);

    @Supported
    public static final KnownKey zimbra_reindex_threads = KnownKey.newKey(10);

    @Supported
    public static final KnownKey zimbra_index_max_readers = KnownKey.newKey(30);

    @Supported
    public static final KnownKey zimbra_index_max_writers = KnownKey.newKey(100);

    @Supported
    public static final KnownKey zimbra_index_reader_cache_size = KnownKey.newKey(20);

    @Supported
    public static final KnownKey zimbra_index_reader_cache_ttl = KnownKey.newKey(300);

    @Supported
    public static final KnownKey zimbra_index_reader_cache_sweep_frequency = KnownKey.newKey(30);

    @Supported
    public static final KnownKey zimbra_index_deferred_items_failure_delay = KnownKey.newKey(300);

    @Supported
    public static final KnownKey zimbra_index_max_transaction_bytes = KnownKey.newKey(5000000);

    @Supported
    public static final KnownKey zimbra_index_max_transaction_items = KnownKey.newKey(100);

    @Supported
    @Reloadable
    public static final KnownKey zimbra_index_max_pending_deletes = KnownKey.newKey(10000);

    public static final KnownKey zimbra_index_lucene_io_impl = KnownKey.newKey("nio");

    @Supported
    public static final KnownKey zimbra_index_lucene_merge_policy = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_index_lucene_min_merge = KnownKey.newKey(1000);

    @Supported
    public static final KnownKey zimbra_index_lucene_max_merge = KnownKey.newKey(Integer.MAX_VALUE);

    @Supported
    public static final KnownKey zimbra_index_lucene_merge_factor = KnownKey.newKey(10);

    @Supported
    public static final KnownKey zimbra_index_lucene_use_compound_file = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_index_lucene_max_buffered_docs = KnownKey.newKey(200);

    @Supported
    public static final KnownKey zimbra_index_lucene_ram_buffer_size_kb = KnownKey.newKey(10240);

    @Supported
    public static final KnownKey zimbra_index_lucene_term_index_divisor = KnownKey.newKey(1);

    @Supported
    public static final KnownKey zimbra_index_lucene_max_terms_per_query = KnownKey.newKey(50000);

    @Supported
    public static final KnownKey zimbra_index_wildcard_max_terms_expanded = KnownKey.newKey(20000);

    public static final KnownKey zimbra_rights_delegated_admin_supported = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_spam_report_queue_size = KnownKey.newKey(100);

    public static final KnownKey zimbra_web_generate_gzip = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_im_chat_flush_time = KnownKey.newKey(300);

    @Supported
    public static final KnownKey zimbra_im_chat_close_time = KnownKey.newKey(3600);

    @Supported
    public static final KnownKey zimbra_http_originating_ip_header = KnownKey.newKey("X-Forwarded-For");

    @Supported
    public static final KnownKey zimbra_session_limit_imap = KnownKey.newKey(15);

    @Supported
    public static final KnownKey zimbra_session_timeout_soap = KnownKey.newKey(600);

    @Supported
    public static final KnownKey zimbra_session_max_pending_notifications = KnownKey.newKey(400);

    @Supported
    public static final KnownKey zimbra_converter_enabled_uuencode = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_converter_enabled_tnef = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_converter_depth_max = KnownKey.newKey(100);

    @Supported
    public static final KnownKey zimbra_ssl_enabled  = KnownKey.newKey(true);

    @Supported
    public static final KnownKey stats_img_folder = KnownKey.newKey("${zimbra_home}/logger/db/work");

    @Reloadable
    public static final KnownKey soap_fault_include_stack_trace = KnownKey.newKey(false);

    @Supported
    public static final KnownKey soap_response_buffer_size = KnownKey.newKey("");

    @Supported
    @Reloadable
    public static final KnownKey soap_response_chunked_transfer_encoding_enabled = KnownKey.newKey(true);
    public static final KnownKey zimbra_servlet_output_stream_buffer_size = KnownKey.newKey(5120);

    @Reloadable
    @Supported
    public static final KnownKey servlet_max_concurrent_requests_per_session = KnownKey.newKey(0);

    public static final KnownKey index_store = KnownKey.newKey("lucene");
    public static final KnownKey cassandra_host = KnownKey.newKey("localhost:9160");
    public static final KnownKey hbase_host = KnownKey.newKey("localhost");
    public static final KnownKey hbase_table_pool_size = KnownKey.newKey(10);

    @Supported
    public static final KnownKey ldap_host = KnownKey.newKey("");

    @Supported
    public static final KnownKey ldap_port = KnownKey.newKey("");

    @Supported
    public static final KnownKey ldap_url = KnownKey.newKey("");

    @Supported
    public static final KnownKey ldap_master_url = KnownKey.newKey("");
    public static final KnownKey ldap_bind_url = KnownKey.newKey("");;

    @Supported
    public static final KnownKey ldap_is_master = KnownKey.newKey(false);

    @Supported
    public static final KnownKey ldap_root_password = KnownKey.newKey("zimbra").protect();

    @Supported
    public static final KnownKey ldap_connect_timeout = KnownKey.newKey(30000);

    @Supported
    public static final KnownKey ldap_read_timeout = KnownKey.newKey(30000);

    @Supported
    public static final KnownKey ldap_deref_aliases = KnownKey.newKey("always");

    @Supported
    public static final KnownKey ldap_connect_pool_master = KnownKey.newKey(false);

    @Supported
    public static final KnownKey ldap_connect_pool_debug = KnownKey.newKey(false);

    @Supported
    public static final KnownKey ldap_connect_pool_initsize = KnownKey.newKey(1);

    @Supported
    public static final KnownKey ldap_connect_pool_maxsize = KnownKey.newKey(50);

    @Supported
    public static final KnownKey ldap_connect_pool_prefsize = KnownKey.newKey(0);

    @Supported
    public static final KnownKey ldap_connect_pool_timeout = KnownKey.newKey(120000);

    @Supported
    public static final KnownKey ldap_connect_pool_health_check_on_checkout_enabled = KnownKey.newKey(false);

    @Supported
    public static final KnownKey ldap_connect_pool_health_check_background_interval_millis = KnownKey.newKey(30000);

    @Supported
    public static final KnownKey ldap_connect_pool_health_check_max_response_time_millis = KnownKey.newKey(30000);

    @Supported
    public static final KnownKey ldap_replication_password = KnownKey.newKey("zmreplica");

    @Supported
    public static final KnownKey ldap_postfix_password = KnownKey.newKey("zmpostfix");

    @Supported
    public static final KnownKey ldap_amavis_password = KnownKey.newKey("zmamavis");
    public static final KnownKey ldap_nginx_password = KnownKey.newKey("zmnginx");
    public static final KnownKey ldap_bes_searcher_password = KnownKey.newKey("zmbes-searcher");

    @Supported
    public static final KnownKey ldap_starttls_supported = KnownKey.newKey(0);

    @Supported
    public static final KnownKey ldap_starttls_required = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimbra_directory_max_search_result = KnownKey.newKey(5000);

    public static final KnownKey ldap_common_loglevel = KnownKey.newKey(49152);
    public static final KnownKey ldap_common_require_tls = KnownKey.newKey(0);
    public static final KnownKey ldap_common_threads = KnownKey.newKey(8);
    public static final KnownKey ldap_common_toolthreads = KnownKey.newKey(1);
    public static final KnownKey ldap_common_writetimeout = KnownKey.newKey(0);
    public static final KnownKey ldap_db_cachefree = KnownKey.newKey(1);
    public static final KnownKey ldap_db_cachesize = KnownKey.newKey(10000);
    public static final KnownKey ldap_db_checkpoint = KnownKey.newKey("64 5");
    public static final KnownKey ldap_db_dncachesize = KnownKey.newKey(0);
    public static final KnownKey ldap_db_idlcachesize = KnownKey.newKey(10000);
    public static final KnownKey ldap_db_shmkey = KnownKey.newKey(0);
    public static final KnownKey ldap_accesslog_cachefree = KnownKey.newKey(1);
    public static final KnownKey ldap_accesslog_cachesize = KnownKey.newKey(10000);
    public static final KnownKey ldap_accesslog_checkpoint = KnownKey.newKey("64 5");
    public static final KnownKey ldap_accesslog_dncachesize = KnownKey.newKey(0);
    public static final KnownKey ldap_accesslog_idlcachesize = KnownKey.newKey(10000);
    public static final KnownKey ldap_accesslog_shmkey = KnownKey.newKey(0);
    public static final KnownKey ldap_overlay_syncprov_checkpoint = KnownKey.newKey("20 10");
    public static final KnownKey ldap_overlay_syncprov_sessionlog = KnownKey.newKey(500);
    public static final KnownKey ldap_overlay_accesslog_logpurge = KnownKey.newKey("01+00:00  00+04:00");

    public static final KnownKey empty_folder_batch_sleep_ms = KnownKey.newKey(1L);

    @Supported
    public static final KnownKey ldap_cache_account_maxsize = KnownKey.newKey(20000);

    @Supported
    public static final KnownKey ldap_cache_account_maxage = KnownKey.newKey(15);

    @Supported
    public static final KnownKey ldap_cache_cos_maxsize = KnownKey.newKey(100);

    @Supported
    public static final KnownKey ldap_cache_cos_maxage = KnownKey.newKey(15);

    @Supported
    public static final KnownKey ldap_cache_domain_maxsize = KnownKey.newKey(100);

    @Supported
    public static final KnownKey ldap_cache_domain_maxage = KnownKey.newKey(15);

    @Supported
    public static final KnownKey ldap_cache_mime_maxage = KnownKey.newKey(15);


    public static final KnownKey ldap_cache_external_domain_maxsize = KnownKey.newKey(2000);
    public static final KnownKey ldap_cache_external_domain_maxage = KnownKey.newKey(15);
    public static final KnownKey ldap_cache_group_maxsize = KnownKey.newKey(2000);
    public static final KnownKey ldap_cache_group_maxage = KnownKey.newKey(15);
    public static final KnownKey ldap_cache_right_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_right_maxage = KnownKey.newKey(15);
    public static final KnownKey ldap_cache_server_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_server_maxage = KnownKey.newKey(15);

    @Supported
    public static final KnownKey ldap_cache_timezone_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_xmppcomponent_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_xmppcomponent_maxage = KnownKey.newKey(15);

    @Supported
    public static final KnownKey ldap_cache_zimlet_maxsize = KnownKey.newKey(100);

    @Supported
    public static final KnownKey ldap_cache_zimlet_maxage = KnownKey.newKey(15);

    public static final KnownKey ldap_cache_reverseproxylookup_domain_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_reverseproxylookup_domain_maxage = KnownKey.newKey(15);
    public static final KnownKey ldap_cache_reverseproxylookup_server_maxsize = KnownKey.newKey(100);
    public static final KnownKey ldap_cache_reverseproxylookup_server_maxage = KnownKey.newKey(15);

    // This combination will consume 128M (128K per target) of memory if the cache is full
    public static final KnownKey acl_cache_target_maxsize = KnownKey.newKey(1024);
    public static final KnownKey acl_cache_target_maxage = KnownKey.newKey(15);
    public static final KnownKey acl_cache_credential_maxsize = KnownKey.newKey(512);
    public static final KnownKey acl_cache_enabled = KnownKey.newKey(true);

    @Supported
    public static final KnownKey gal_group_cache_maxsize_per_domain = KnownKey.newKey(0);

    @Supported
    public static final KnownKey gal_group_cache_maxsize_domains = KnownKey.newKey(10);

    @Supported
    public static final KnownKey gal_group_cache_maxage = KnownKey.newKey(10080);  // 7 days

    public static final KnownKey calendar_resource_ldap_search_maxsize = KnownKey.newKey(1000);

    // This value is stored here for use by zmmycnf program. Changing this
    // setting does not immediately reflect in MySQL server. You will have to,
    // with abundant precaution, re-generate my.cnf and restart MySQL server for
    // the change to take effect.
    @Supported
    public static final KnownKey mysql_directory = KnownKey.newKey("${zimbra_home}/mysql");

    @Supported
    public static final KnownKey mysql_data_directory = KnownKey.newKey("${zimbra_db_directory}/data");

    @Supported
    public static final KnownKey mysql_socket = KnownKey.newKey("${zimbra_db_directory}/mysql.sock");

    @Supported
    public static final KnownKey mysql_pidfile = KnownKey.newKey("${zimbra_db_directory}/mysql.pid");

    @Supported
    public static final KnownKey mysql_mycnf = KnownKey.newKey("${zimbra_home}/conf/my.cnf");

    @Supported
    public static final KnownKey mysql_errlogfile = KnownKey.newKey("${zimbra_home}/log/mysql_error.log");

    @Supported
    public static final KnownKey mysql_bind_address = KnownKey.newKey("127.0.0.1");

    @Supported
    public static final KnownKey mysql_port = KnownKey.newKey(7306);

    @Supported
    public static final KnownKey mysql_root_password = KnownKey.newKey("zimbra").protect();

    @Supported
    public static final KnownKey mysql_memory_percent = KnownKey.newKey(30);
    public static final KnownKey mysql_innodb_log_buffer_size = KnownKey.newKey(null);
    public static final KnownKey mysql_innodb_log_file_size = KnownKey.newKey(null);
    public static final KnownKey mysql_sort_buffer_size = KnownKey.newKey(null);
    public static final KnownKey mysql_read_buffer_size = KnownKey.newKey(null);
    public static final KnownKey mysql_table_cache = KnownKey.newKey(null);


    @Supported
    public static final KnownKey derby_properties = KnownKey.newKey("${zimbra_home}/conf/derby.properties");

    public final static KnownKey logger_data_directory = KnownKey.newKey("${zimbra_home}/logger/db/data");
    public final static KnownKey logger_zmrrdfetch_port = KnownKey.newKey(10663);

    @Supported
    public static final KnownKey logger_mysql_directory = KnownKey.newKey("${zimbra_home}/logger/mysql");

    @Supported
    public static final KnownKey logger_mysql_data_directory = KnownKey.newKey("${zimbra_home}/logger/db/data");

    @Supported
    public static final KnownKey logger_mysql_socket = KnownKey.newKey("${zimbra_home}/logger/db/mysql.sock");

    @Supported
    public static final KnownKey logger_mysql_pidfile = KnownKey.newKey("${zimbra_home}/logger/db/mysql.pid");

    @Supported
    public static final KnownKey logger_mysql_mycnf = KnownKey.newKey("${zimbra_home}/conf/my.logger.cnf");
    public static final KnownKey logger_mysql_errlogfile = KnownKey.newKey("${zimbra_home}/log/logger_mysql_error.log");
    public static final KnownKey logger_mysql_bind_address = KnownKey.newKey("127.0.0.1");
    public static final KnownKey logger_mysql_port = KnownKey.newKey(7307);
    public static final KnownKey zimbra_logger_mysql_password = KnownKey.newKey("zimbra").protect();

    public static final KnownKey postfix_alias_maps = KnownKey.newKey("hash:/etc/aliases");
    public static final KnownKey postfix_always_add_missing_headers = KnownKey.newKey("yes");
    public static final KnownKey postfix_broken_sasl_auth_clients = KnownKey.newKey("yes");
    public static final KnownKey postfix_bounce_notice_recipient = KnownKey.newKey("postmaster");
    public static final KnownKey postfix_bounce_queue_lifetime = KnownKey.newKey("5d");
    public static final KnownKey postfix_command_directory = KnownKey.newKey("${zimbra_home}/postfix/sbin");
    public static final KnownKey postfix_daemon_directory = KnownKey.newKey("${zimbra_home}/postfix/libexec");
    public static final KnownKey postfix_enable_smtpd_policyd = KnownKey.newKey("no");
    public static final KnownKey postfix_delay_warning_time = KnownKey.newKey("0h");
    public static final KnownKey postfix_header_checks = KnownKey.newKey("pcre:${zimbra_home}/conf/postfix_header_checks");
    public static final KnownKey postfix_in_flow_delay = KnownKey.newKey("1s");
    public static final KnownKey postfix_lmtp_connection_cache_destinations = KnownKey.newKey("");
    public static final KnownKey postfix_lmtp_connection_cache_time_limit = KnownKey.newKey("4s");
    public static final KnownKey postfix_lmtp_host_lookup = KnownKey.newKey("dns");
    public static final KnownKey postfix_mailq_path = KnownKey.newKey("${zimbra_home}/postfix/sbin/mailq");
    public static final KnownKey postfix_manpage_directory = KnownKey.newKey("${zimbra_home}/postfix/man");
    public static final KnownKey postfix_maximal_backoff_time = KnownKey.newKey("4000s");
    public static final KnownKey postfix_minimal_backoff_time = KnownKey.newKey("300s");
    public static final KnownKey postfix_newaliases_path = KnownKey.newKey("${zimbra_home}/postfix/sbin/newaliases");
    public static final KnownKey postfix_notify_classes = KnownKey.newKey("resource,software");
    public static final KnownKey postfix_policy_time_limit = KnownKey.newKey(3600);
    public static final KnownKey postfix_propagate_unmatched_extensions = KnownKey.newKey("canonical");
    public static final KnownKey postfix_queue_directory = KnownKey.newKey("${zimbra_home}/data/postfix/spool");
    public static final KnownKey postfix_queue_run_delay = KnownKey.newKey("300s");
    public static final KnownKey postfix_sender_canonical_maps = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-scm.cf");
    public static final KnownKey postfix_sendmail_path = KnownKey.newKey("${zimbra_home}/postfix/sbin/sendmail");

    public static final KnownKey postfix_smtp_cname_overrides_servername = KnownKey.newKey("no");
    public static final KnownKey postfix_smtp_sasl_auth_enable = KnownKey.newKey("no");
    public static final KnownKey postfix_smtp_sasl_security_options = KnownKey.newKey("noplaintext,noanonymous");
    public static final KnownKey postfix_smtp_tls_security_level = KnownKey.newKey(null);
    public static final KnownKey postfix_smtp_sasl_mechanism_filter = KnownKey.newKey(null);
    public static final KnownKey postfix_smtp_sasl_password_maps = KnownKey.newKey(null);

    public static final KnownKey postfix_smtpd_sasl_authenticated_header = KnownKey.newKey("no");
    public static final KnownKey postfix_smtpd_sasl_security_options = KnownKey.newKey("noanonymous");
    public static final KnownKey postfix_smtpd_sasl_tls_security_options = KnownKey.newKey("$smtpd_sasl_security_options");
    public static final KnownKey postfix_smtpd_client_restrictions = KnownKey.newKey("reject_unauth_pipelining");
    public static final KnownKey postfix_smtpd_data_restrictions = KnownKey.newKey("reject_unauth_pipelining");
    public static final KnownKey postfix_smtpd_helo_required = KnownKey.newKey("yes");
    public static final KnownKey postfix_smtpd_tls_cert_file = KnownKey.newKey("${zimbra_home}/conf/smtpd.crt");
    public static final KnownKey postfix_smtpd_tls_key_file = KnownKey.newKey("${zimbra_home}/conf/smtpd.key");
    public static final KnownKey postfix_smtpd_tls_loglevel = KnownKey.newKey(1);
    public static final KnownKey postfix_transport_maps = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-transport.cf");
    public static final KnownKey postfix_virtual_alias_domains = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-vad.cf");
    public static final KnownKey postfix_virtual_alias_expansion_limit = KnownKey.newKey(10000);
    public static final KnownKey postfix_virtual_alias_maps = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-vam.cf");
    public static final KnownKey postfix_virtual_mailbox_domains = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-vmd.cf");
    public static final KnownKey postfix_virtual_mailbox_maps = KnownKey.newKey("proxy:ldap:${zimbra_home}/conf/ldap-vmm.cf");
    public static final KnownKey postfix_virtual_transport = KnownKey.newKey("error");

    public static final KnownKey sasl_smtpd_mech_list = KnownKey.newKey("PLAIN LOGIN");

    public static final KnownKey cbpolicyd_pid_file = KnownKey.newKey("${zimbra_log_directory}/cbpolicyd.pid");
    public static final KnownKey cbpolicyd_log_file = KnownKey.newKey("${zimbra_log_directory}/cbpolicyd.log");
    public static final KnownKey cbpolicyd_db_file = KnownKey.newKey("${zimbra_home}/data/cbpolicyd/db/cbpolicyd.sqlitedb");
    public static final KnownKey cbpolicyd_log_level = KnownKey.newKey(3);
    public static final KnownKey cbpolicyd_log_mail = KnownKey.newKey("main");
    public static final KnownKey cbpolicyd_log_detail = KnownKey.newKey("modules");
    public static final KnownKey cbpolicyd_bind_host = KnownKey.newKey("127.0.0.1");
    public static final KnownKey cbpolicyd_bind_port = KnownKey.newKey(10031);
    public static final KnownKey cbpolicyd_timeout = KnownKey.newKey(120);
    public static final KnownKey cbpolicyd_bypass_timeout = KnownKey.newKey(30);
    public static final KnownKey cbpolicyd_bypass_mode = KnownKey.newKey("tempfail");
    public static final KnownKey cbpolicyd_module_accesscontrol = KnownKey.newKey(0);
    public static final KnownKey cbpolicyd_module_greylisting = KnownKey.newKey(0);
    public static final KnownKey cbpolicyd_module_checkhelo = KnownKey.newKey(0);
    public static final KnownKey cbpolicyd_module_checkspf = KnownKey.newKey(0);
    public static final KnownKey cbpolicyd_module_quotas = KnownKey.newKey(1);

    public static final KnownKey sqlite_shared_cache_enabled = KnownKey.newKey(false);
    public static final KnownKey sqlite_cache_size = KnownKey.newKey(500);
    public static final KnownKey sqlite_journal_mode = KnownKey.newKey("PERSIST");
    public static final KnownKey sqlite_page_size = KnownKey.newKey(4096);
    public static final KnownKey sqlite_sync_mode = KnownKey.newKey("NORMAL");

    @Supported
    public static final KnownKey mailboxd_directory = KnownKey.newKey("${zimbra_home}/mailboxd");

    @Supported
    public static final KnownKey mailboxd_java_heap_size = KnownKey.newKey(null);

    @Supported
    public static final KnownKey mailboxd_java_heap_new_size_percent = KnownKey.newKey(25);

    @Supported
    public static final KnownKey mailboxd_thread_stack_size = KnownKey.newKey("256k");

    @Supported
    public static final KnownKey mailboxd_java_options = KnownKey.newKey("-server" +
            " -Djava.awt.headless=true" +
            " -Dsun.net.inetaddr.ttl=${networkaddress_cache_ttl}" +
            " -XX:+UseConcMarkSweepGC" +
            " -XX:PermSize=128m" +
            " -XX:MaxPermSize=128m" +
            " -XX:SoftRefLRUPolicyMSPerMB=1" +
            " -verbose:gc" +
            " -XX:+PrintGCDetails" +
            " -XX:+PrintGCTimeStamps" +
            " -XX:+PrintGCApplicationStoppedTime" +
            " -XX:-OmitStackTraceInFastThrow");
    @Supported
    public static final KnownKey mailboxd_pidfile = KnownKey.newKey("${zimbra_log_directory}/mailboxd.pid");

    @Supported
    public static final KnownKey mailboxd_keystore = KnownKey.newKey("${mailboxd_directory}/etc/keystore");

    @Supported
    public static final KnownKey mailboxd_keystore_password = KnownKey.newKey("zimbra");

    public static final KnownKey mailboxd_keystore_base = KnownKey.newKey("${zimbra_home}/conf/keystore.base");
    public static final KnownKey mailboxd_keystore_base_password = KnownKey.newKey("zimbra");

    @Supported
    public static final KnownKey mailboxd_truststore = KnownKey.newKey("${zimbra_java_home}/lib/security/cacerts");

    @Supported
    public static final KnownKey mailboxd_truststore_password = KnownKey.newKey("changeit");

    public static final KnownKey mailboxd_output_filename = KnownKey.newKey("zmmailboxd.out");

    @Supported
    public static final KnownKey mailboxd_output_file = KnownKey.newKey("${zimbra_log_directory}/${mailboxd_output_filename}");

    @Supported
    public static final KnownKey mailboxd_output_rotate_interval = KnownKey.newKey(86400);

    @Supported
    public static final KnownKey ssl_allow_untrusted_certs = KnownKey.newKey(false);

    public static final KnownKey ssl_allow_mismatched_certs = KnownKey.newKey(true);

    public static final KnownKey ssl_allow_accept_untrusted_certs = KnownKey.newKey(true);

    @Supported
    public static final KnownKey zimlet_directory = KnownKey.newKey("${zimbra_home}/zimlets-deployed");

    @Supported
    public static final KnownKey wiki_enabled = KnownKey.newKey(false);

    @Supported
    public static final KnownKey wiki_user = KnownKey.newKey("wiki");

    @Supported
    public static final KnownKey calendar_outlook_compatible_allday_events = KnownKey.newKey(false);

    @Supported
    public static final KnownKey calendar_entourage_compatible_timezones = KnownKey.newKey(true);
    public static final KnownKey calendar_apple_ical_compatible_canceled_instances = KnownKey.newKey(true);

    @Supported
    public static final KnownKey calendar_ics_import_full_parse_max_size = KnownKey.newKey(131072); // 128KB
    public static final KnownKey calendar_ics_export_buffer_size = KnownKey.newKey(131072); // 128KB
    public static final KnownKey calendar_max_desc_in_metadata = KnownKey.newKey(4096); // 4KB
    public static final KnownKey calendar_allow_invite_without_method = KnownKey.newKey(false);
    public static final KnownKey calendar_freebusy_max_days = KnownKey.newKey(366);
    public static final KnownKey calendar_search_max_days  = KnownKey.newKey(400);

    @Supported
    public static final KnownKey calendar_cache_enabled = KnownKey.newKey(true);

    @Supported
    public static final KnownKey calendar_cache_directory = KnownKey.newKey("${zimbra_tmp_directory}/calcache");

    @Supported
    public static final KnownKey calendar_cache_lru_size = KnownKey.newKey(1000);

    @Supported
    public static final KnownKey calendar_cache_range_month_from = KnownKey.newKey(0);

    @Supported
    public static final KnownKey calendar_cache_range_months = KnownKey.newKey(3);
    public static final KnownKey calendar_cache_max_stale_items = KnownKey.newKey(10);
    public static final KnownKey calendar_exchange_form_auth_url = KnownKey.newKey("/exchweb/bin/auth/owaauth.dll");

    public static final KnownKey spnego_java_options =  KnownKey.newKey(
            "-Djava.security.krb5.conf=${mailboxd_directory}/etc/krb5.ini " +
            "-Djava.security.auth.login.config=${mailboxd_directory}/etc/spnego.conf " +
            "-Djavax.security.auth.useSubjectCredsOnly=false");

    public static final KnownKey text_attachments_base64 = KnownKey.newKey(true);

    public static final KnownKey nio_imap_enabled = KnownKey.newKey(true);
    public static final KnownKey nio_pop3_enabled = KnownKey.newKey(true);

    public static final KnownKey imap_max_request_size = KnownKey.newKey(10 * 1024);

    @Supported
    @Reloadable
    public static final KnownKey imap_max_consecutive_error = KnownKey.newKey(5);

    @Supported
    public static final KnownKey imap_inactive_session_cache_size = KnownKey.newKey(1000);
    public static final KnownKey imap_write_timeout = KnownKey.newKey(10);
    public static final KnownKey imap_write_chunk_size = KnownKey.newKey(8 * 1024);
    public static final KnownKey imap_thread_keep_alive_time = KnownKey.newKey(60);
    public static final KnownKey imap_max_idle_time = KnownKey.newKey(60);
    public static final KnownKey imap_authenticated_max_idle_time = KnownKey.newKey(1800);
    public static final KnownKey data_source_imap_reuse_connections = KnownKey.newKey(false);

    public static final KnownKey pop3_write_timeout = KnownKey.newKey(10);
    public static final KnownKey pop3_thread_keep_alive_time = KnownKey.newKey(60);
    public static final KnownKey pop3_max_idle_time = KnownKey.newKey(60);

    public static final KnownKey milter_bind_port = KnownKey.newKey(0);
    public static final KnownKey milter_bind_address = KnownKey.newKey(null);
    public static final KnownKey milter_max_idle_time = KnownKey.newKey(120);
    public static final KnownKey milter_in_process_mode = KnownKey.newKey(false);
    public static final KnownKey milter_write_timeout = KnownKey.newKey(10);
    public static final KnownKey milter_write_chunk_size = KnownKey.newKey(1024);
    public static final KnownKey milter_thread_keep_alive_time = KnownKey.newKey(60);

    @Supported
    public static final KnownKey krb5_keytab = KnownKey.newKey("${zimbra_home}/conf/krb5.keytab");
    public static final KnownKey krb5_service_principal_from_interface_address = KnownKey.newKey(false);

    @Supported
    public static final KnownKey krb5_debug_enabled = KnownKey.newKey(false);

    @Supported
    public static final KnownKey zimbra_mtareport_max_users = KnownKey.newKey(50);

    @Supported
    public static final KnownKey zimbra_mtareport_max_hosts = KnownKey.newKey(50);

    public static final KnownKey zmconfigd_enable_config_restarts = KnownKey.newKey("true");
    public static final KnownKey zmconfigd_interval = KnownKey.newKey(60);
    public static final KnownKey zmconfigd_log_level = KnownKey.newKey(3);
    public static final KnownKey zmconfigd_listen_port = KnownKey.newKey(7171);

    @Supported
    public static final KnownKey zimbra_mailbox_groups = KnownKey.newKey(100);

    /*
    public static final KnownKey zimbra_class_ldap_client = KnownKey.newKey("com.zimbra.cs.ldap.jndi.JNDILdapClient");
    public static final KnownKey zimbra_class_provisioning = KnownKey.newKey("com.zimbra.cs.account.ldap.legacy.LegacyLdapProvisioning");
    */

    public static final KnownKey zimbra_class_ldap_client = KnownKey.newKey("com.zimbra.cs.ldap.unboundid.UBIDLdapClient");
    public static final KnownKey zimbra_class_provisioning = KnownKey.newKey("com.zimbra.cs.account.ldap.LdapProvisioning");


    public static final KnownKey zimbra_class_accessmanager = KnownKey.newKey("com.zimbra.cs.account.accesscontrol.ACLAccessManager");
    public static final KnownKey zimbra_class_mboxmanager = KnownKey.newKey("com.zimbra.cs.mailbox.MailboxManager");
    public static final KnownKey zimbra_class_database = KnownKey.newKey("com.zimbra.cs.db.MySQL");
    public static final KnownKey zimbra_class_store = KnownKey.newKey("com.zimbra.cs.store.file.FileBlobStore");
    public static final KnownKey zimbra_class_application = KnownKey.newKey("com.zimbra.cs.util.ZimbraApplication");
    public static final KnownKey zimbra_class_rulerewriterfactory = KnownKey.newKey("com.zimbra.cs.filter.RuleRewriterFactory");
    public static final KnownKey zimbra_class_datasourcemanager = KnownKey.newKey("com.zimbra.cs.datasource.DataSourceManager");
    public static final KnownKey zimbra_class_attrmanager = KnownKey.newKey("com.zimbra.cs.account.AttributeManager");
    public static final KnownKey zimbra_class_soapsessionfactory = KnownKey.newKey("com.zimbra.soap.SoapSessionFactory");
    public static final KnownKey zimbra_class_dbconnfactory = KnownKey.newKey("com.zimbra.cs.db.ZimbraConnectionFactory");
    public static final KnownKey zimbra_class_customproxyselector = KnownKey.newKey(""); //intentionally has no value; set one if u want to use a custom proxy selector
    public static final KnownKey zimbra_class_galgroupinfoprovider = KnownKey.newKey("com.zimbra.cs.gal.GalGroupInfoProvider");

    // XXX REMOVE AND RELEASE NOTE
    public static final KnownKey data_source_trust_self_signed_certs = KnownKey.newKey(false);
    public static final KnownKey data_source_fetch_size = KnownKey.newKey(5);
    public static final KnownKey data_source_max_message_memory_size = KnownKey.newKey(2097152); // 2 MB
    public static final KnownKey data_source_new_sync_enabled = KnownKey.newKey(false);
    public static final KnownKey data_source_xsync_class = KnownKey.newKey("");
    public static final KnownKey data_source_xsync_factory_class = KnownKey.newKey("");
    public static final KnownKey data_source_config = KnownKey.newKey("${zimbra_home}/conf/datasource.xml");
    public static final KnownKey data_source_ioexception_handler_class = KnownKey.newKey("com.zimbra.cs.datasource.IOExceptionHandler");

    @Supported
    public static final KnownKey timezone_file = KnownKey.newKey("${zimbra_home}/conf/timezones.ics");

    public static final KnownKey search_disable_database_hints = KnownKey.newKey(false);
    public static final KnownKey search_dbfirst_term_percentage_cutoff = KnownKey.newKey(0.8F);

    public static final KnownKey zmstat_log_directory = KnownKey.newKey("${zimbra_home}/zmstat");
    public static final KnownKey zmstat_interval = KnownKey.newKey(30);
    public static final KnownKey zmstat_disk_interval = KnownKey.newKey(600);
    public static final KnownKey zmstat_max_retention = KnownKey.newKey(0);

    public static final KnownKey zmstat_df_excludes = KnownKey.newKey("");

    public static final KnownKey zimbra_noop_default_timeout = KnownKey.newKey(300);
    public static final KnownKey zimbra_noop_min_timeout = KnownKey.newKey(30);
    public static final KnownKey zimbra_noop_max_timeout = KnownKey.newKey(1200);
    public static final KnownKey zimbra_waitset_default_request_timeout = KnownKey.newKey(300);
    public static final KnownKey zimbra_waitset_min_request_timeout = KnownKey.newKey(30);
    public static final KnownKey zimbra_waitset_max_request_timeout = KnownKey.newKey(1200);
    public static final KnownKey zimbra_waitset_max_per_account = KnownKey.newKey(5);

    // *_disable_tiemout settings are here for bug 56458
    // This is a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade
    // we should re-evaluate/remove these settings and the code that uses them
    public static final KnownKey zimbra_archive_formatter_disable_timeout = KnownKey.newKey(true);
    public static final KnownKey zimbra_archive_formatter_search_chunk_size = KnownKey.newKey(4096);
    public static final KnownKey zimbra_gal_sync_disable_timeout = KnownKey.newKey(true);

    public static final KnownKey zimbra_admin_waitset_default_request_timeout = KnownKey.newKey(300);
    public static final KnownKey zimbra_admin_waitset_min_request_timeout = KnownKey.newKey(0);
    public static final KnownKey zimbra_admin_waitset_max_request_timeout = KnownKey.newKey(3600);

    public static final KnownKey zimbra_waitset_initial_sleep_time = KnownKey.newKey(1000);
    public static final KnownKey zimbra_waitset_nodata_sleep_time = KnownKey.newKey(3000);

    public static final KnownKey zimbra_csv_mapping_file = KnownKey.newKey("${zimbra_home}/conf/zimbra-contact-fields.xml");
    public static final KnownKey zimbra_auth_provider = KnownKey.newKey("zimbra");
    public static final KnownKey zimbra_authtoken_cache_size = KnownKey.newKey(5000);
    public static final KnownKey zimbra_authtoken_cookie_domain = KnownKey.newKey("");
    public static final KnownKey zimbra_zmjava_options = KnownKey.newKey("-Xmx256m");
    public static final KnownKey zimbra_zmjava_java_library_path = KnownKey.newKey("");
    public static final KnownKey zimbra_zmjava_java_ext_dirs = KnownKey.newKey("");
    public static final KnownKey debug_xmpp_disable_client_tls = KnownKey.newKey(0);
    public static final KnownKey im_dnsutil_dnsoverride = KnownKey.newKey("");

    @Supported
    @Reloadable
    public static final KnownKey zimbra_enable_text_extraction = KnownKey.newKey(true);

    /**
     * {@code true} to use Zimbra's SMTP client implementation
     * ({@code com.zimbra.cs.mailclient.smtp.SmtpTransport}),
     * otherwise use JavaMail's default implementation
     * ({@code com.sun.mail.smtp.SMTPTransport}).
     */
    public static final KnownKey javamail_zsmtp = KnownKey.newKey(true);
    /**
     * {@code true} to use Zimbra's MIME parser implementation
     * ({@code com.zimbra.common.mime.shim.JavaMailMimeMessage}),
     * otherwise use JavaMail's default implementation
     * ({@code javax.mail.internet.MimemMessage}).
     */
    public static final KnownKey javamail_zparser = KnownKey.newKey(true);
    public static final KnownKey javamail_pop3_debug = KnownKey.newKey(false);
    public static final KnownKey javamail_imap_debug = KnownKey.newKey(false);
    public static final KnownKey javamail_smtp_debug = KnownKey.newKey(false);
    public static final KnownKey javamail_pop3_timeout = KnownKey.newKey(60);
    public static final KnownKey javamail_imap_timeout = KnownKey.newKey(60);
    public static final KnownKey javamail_smtp_timeout = KnownKey.newKey(60);
    public static final KnownKey javamail_pop3_test_timeout = KnownKey.newKey(20);
    public static final KnownKey javamail_imap_test_timeout = KnownKey.newKey(20);
    public static final KnownKey javamail_pop3_enable_starttls = KnownKey.newKey(true);
    public static final KnownKey javamail_imap_enable_starttls = KnownKey.newKey(true);
    public static final KnownKey javamail_smtp_enable_starttls = KnownKey.newKey(true);

    public static final KnownKey yauth_baseuri = KnownKey.newKey("https://login.yahoo.com/WSLogin/V1");

    public static final KnownKey purge_initial_sleep_ms = KnownKey.newKey(30 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey conversation_max_age_ms = KnownKey.newKey(31 * Constants.MILLIS_PER_DAY);
    public static final KnownKey tombstone_max_age_ms = KnownKey.newKey(3 * Constants.MILLIS_PER_MONTH);

    public static final KnownKey autpprov_initial_sleep_ms = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    @Supported
    public static final KnownKey httpclient_internal_connmgr_max_host_connections = KnownKey.newKey(100);
    @Supported
    public static final KnownKey httpclient_external_connmgr_max_host_connections = KnownKey.newKey(100);

    @Supported
    public static final KnownKey httpclient_internal_connmgr_max_total_connections = KnownKey.newKey(300);
    @Supported
    public static final KnownKey httpclient_external_connmgr_max_total_connections = KnownKey.newKey(300);

    public static final KnownKey httpclient_internal_connmgr_keepalive_connections = KnownKey.newKey(true);
    public static final KnownKey httpclient_external_connmgr_keepalive_connections = KnownKey.newKey(true);

    public static final KnownKey httpclient_internal_connmgr_tcp_nodelay = KnownKey.newKey(false);
    public static final KnownKey httpclient_external_connmgr_tcp_nodelay = KnownKey.newKey(false);

    public static final KnownKey httpclient_internal_connmgr_connection_timeout = KnownKey.newKey(25 * Constants.MILLIS_PER_SECOND);
    public static final KnownKey httpclient_external_connmgr_connection_timeout = KnownKey.newKey(25 * Constants.MILLIS_PER_SECOND);

    public static final KnownKey httpclient_internal_connmgr_so_timeout = KnownKey.newKey(60 * Constants.MILLIS_PER_SECOND);
    public static final KnownKey httpclient_external_connmgr_so_timeout = KnownKey.newKey(60 * Constants.MILLIS_PER_SECOND);

    public static final KnownKey httpclient_internal_client_connection_timeout = KnownKey.newKey(30 * Constants.MILLIS_PER_SECOND);
    public static final KnownKey httpclient_external_client_connection_timeout = KnownKey.newKey(30 * Constants.MILLIS_PER_SECOND);

    public static final KnownKey httpclient_internal_connmgr_idle_reaper_sleep_interval = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);
    public static final KnownKey httpclient_external_connmgr_idle_reaper_sleep_interval = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey httpclient_internal_connmgr_idle_reaper_connection_timeout = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);
    public static final KnownKey httpclient_external_connmgr_idle_reaper_connection_timeout = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey httpclient_soaphttptransport_retry_count = KnownKey.newKey(2);

    public static final KnownKey httpclient_soaphttptransport_so_timeout = KnownKey.newKey(300 * Constants.MILLIS_PER_SECOND);


    /**
     * Bug: 47051 Known key for the CLI utilities SOAP HTTP transport timeout.
     * The default value is set to 0 i.e. no timeout.
     */
    public static final KnownKey cli_httpclient_soaphttptransport_so_timeout  = KnownKey.newKey(0);


    public static final KnownKey httpclient_convertd_so_timeout = KnownKey.newKey(-1);

    @Supported
    public static final KnownKey client_use_system_proxy = KnownKey.newKey(false);

    @Supported
    public static final KnownKey client_use_native_proxy_selector = KnownKey.newKey(false);

    public static final KnownKey shared_mime_info_globs = KnownKey.newKey("${zimbra_home}/conf/globs2");
    public static final KnownKey shared_mime_info_magic = KnownKey.newKey("${zimbra_home}/conf/magic");

    public static final KnownKey xmpp_server_tls_enabled = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_dialback_enabled = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_session_allowmultiple = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_session_idle = KnownKey.newKey(20 * 60 * 1000);

    public static final KnownKey xmpp_server_session_idle_check_time = KnownKey.newKey(5 * 60 * 1000);

    public static final KnownKey xmpp_server_processing_core_threads = KnownKey.newKey(2);

    public static final KnownKey xmpp_server_processing_max_threads = KnownKey.newKey(50);

    public static final KnownKey xmpp_server_processing_queue = KnownKey.newKey(50);

    public static final KnownKey xmpp_server_outgoing_max_threads = KnownKey.newKey(20);

    public static final KnownKey xmpp_server_outgoing_queue = KnownKey.newKey(50);

    public static final KnownKey xmpp_server_read_timeout = KnownKey.newKey(3 * 60 * 1000);

    public static final KnownKey xmpp_server_socket_remoteport = KnownKey.newKey(5269);

    public static final KnownKey xmpp_server_compression_policy = KnownKey.newKey("disabled");

    public static final KnownKey xmpp_server_certificate_verify = KnownKey.newKey(false);

    public static final KnownKey xmpp_server_certificate_verify_chain = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_certificate_verify_root = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_certificate_verify_validity = KnownKey.newKey(true);

    public static final KnownKey xmpp_server_certificate_accept_selfsigned = KnownKey.newKey(true);

    public static final KnownKey xmpp_muc_enabled = KnownKey.newKey(true);

    public static final KnownKey xmpp_muc_service_name = KnownKey.newKey("conference");

    public static final KnownKey xmpp_muc_discover_locked = KnownKey.newKey(true);

    public static final KnownKey xmpp_muc_restrict_room_creation = KnownKey.newKey(false);

    public static final KnownKey xmpp_muc_room_create_jid_list = KnownKey.newKey("");

    public static final KnownKey xmpp_muc_unload_empty_hours = KnownKey.newKey(5);

    public static final KnownKey xmpp_muc_sysadmin_jid_list = KnownKey.newKey("");

    public static final KnownKey xmpp_muc_idle_user_sweep_ms = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey xmpp_muc_idle_user_timeout_ms = KnownKey.newKey(0);

    public static final KnownKey xmpp_muc_log_sweep_time_ms = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey xmpp_muc_log_batch_size = KnownKey.newKey(50);

    public static final KnownKey xmpp_muc_default_history_type = KnownKey.newKey("number");

    public static final KnownKey xmpp_muc_history_number = KnownKey.newKey(25);

    public static final KnownKey xmpp_private_storage_enabled = KnownKey.newKey(true);

    public static final KnownKey xmpp_client_compression_policy = KnownKey.newKey("optional");

    public static final KnownKey xmpp_client_write_timeout = KnownKey.newKey(60 * Constants.MILLIS_PER_SECOND);

    public static final KnownKey xmpp_session_conflict_limit = KnownKey.newKey(0);

    public static final KnownKey xmpp_client_idle_timeout = KnownKey.newKey(10 * 60 * 1000);

    public static final KnownKey xmpp_cloudrouting_idle_timeout = KnownKey.newKey(5 * Constants.MILLIS_PER_MINUTE);

    public static final KnownKey xmpp_offline_type = KnownKey.newKey("store_and_drop");

    public static final KnownKey xmpp_offline_quota = KnownKey.newKey(100 * 1024);

    public static final KnownKey xmpp_dns_override = KnownKey.newKey("");

    public static final KnownKey zmailbox_message_cachesize = KnownKey.newKey(1);

    @Supported
    public static final KnownKey contact_ranking_enabled = KnownKey.newKey(true);


    public static final KnownKey jdbc_results_streaming_enabled = KnownKey.newKey(true);

    public static final KnownKey freebusy_queue_directory = KnownKey.newKey("${zimbra_home}/fbqueue/");
    public static final KnownKey freebusy_exchange_cn1 = KnownKey.newKey(null);
    public static final KnownKey freebusy_exchange_cn2 = KnownKey.newKey(null);
    public static final KnownKey freebusy_exchange_cn3 = KnownKey.newKey(null);
    public static final KnownKey freebusy_disable_nodata_status = KnownKey.newKey(false);

    public static final KnownKey notes_enabled = KnownKey.newKey(false);

    public static final KnownKey zimbra_lmtp_validate_messages = KnownKey.newKey(true);
    public static final KnownKey zimbra_lmtp_max_line_length = KnownKey.newKey(10240);

    public static final KnownKey data_source_scheduling_enabled = KnownKey.newKey(true);
    public static final KnownKey data_source_eas_sync_email = KnownKey.newKey(true);
    public static final KnownKey data_source_eas_sync_contacts = KnownKey.newKey(true);
    public static final KnownKey data_source_eas_sync_calendar = KnownKey.newKey(true);
    public static final KnownKey data_source_eas_sync_tasks = KnownKey.newKey(true);
    public static final KnownKey data_source_eas_window_size = KnownKey.newKey(50);
    public static final KnownKey data_source_eas_mime_truncation = KnownKey.newKey(4);

    public static final KnownKey zimbra_activesync_versions = KnownKey.newKey("2.0,2.1,2.5,12.0,12.1");
    public static final KnownKey zimbra_activesync_contact_image_size = KnownKey.newKey(2*1024*1024);
    public static final KnownKey zimbra_activesync_autodiscover_url = KnownKey.newKey(null);
    public static final KnownKey zimbra_activesync_autodiscover_use_service_url = KnownKey.newKey(false);
    public static final KnownKey zimbra_activesync_metadata_cache_expiration = KnownKey.newKey(3600);
    public static final KnownKey zimbra_activesync_metadata_cache_max_size = KnownKey.newKey(5000);

    public static final KnownKey zimbra_slow_logging_enabled = KnownKey.newKey(false);
    public static final KnownKey zimbra_slow_logging_threshold = KnownKey.newKey(5000);

    public static final KnownKey smtp_host_retry_millis = KnownKey.newKey(60000);
    public static final KnownKey smtp_to_lmtp_enabled = KnownKey.newKey(false);
    public static final KnownKey smtp_to_lmtp_port = KnownKey.newKey(7024);

    @Supported
    public static final KnownKey socks_enabled = KnownKey.newKey(false);

    public static final KnownKey socket_connect_timeout = KnownKey.newKey(30000);

    @Supported
    public static final KnownKey socket_so_timeout = KnownKey.newKey(30000);

    public static final KnownKey networkaddress_cache_ttl = KnownKey.newKey(60);

    public static final KnownKey zdesktop_local_account_id = KnownKey.newKey(null);

    @Supported
    public static final KnownKey out_of_disk_error_unix = KnownKey.newKey("No space left on device");

    @Supported
    public static final KnownKey out_of_disk_error_windows = KnownKey.newKey("There is not enough space on the disk");

    public static final KnownKey antispam_mysql_directory = KnownKey.newKey("${zimbra_home}/mta/mysql");
    public static final KnownKey antispam_mysql_data_directory = KnownKey.newKey("${zimbra_home}/data/amavisd/mysql/data");
    public static final KnownKey antispam_mysql_errlogfile = KnownKey.newKey("${zimbra_home}/log/antispam-mysqld.log");
    public static final KnownKey antispam_mysql_mycnf = KnownKey.newKey("${zimbra_home}/conf/antispam-my.cnf");
    public static final KnownKey antispam_mysql_pidfile = KnownKey.newKey("${zimbra_home}/data/amavisd/mysql/mysql.pid");
    public static final KnownKey antispam_mysql_host = KnownKey.newKey("127.0.0.1");
    public static final KnownKey antispam_mysql_port = KnownKey.newKey(7308);
    public static final KnownKey antispam_mysql_socket = KnownKey.newKey("${zimbra_home}/data/amavisd/mysql/mysql.sock");
    public static final KnownKey antispam_mysql_user = KnownKey.newKey("zimbra");
    public static final KnownKey antispam_mysql_root_password = KnownKey.newKey("");
    public static final KnownKey antispam_mysql_password = KnownKey.newKey("");

    // LDAP Custom DIT base DN for LDAP app admin entries
    public static final KnownKey ldap_dit_base_dn_appadmin      = KnownKey.newKey("");
    // LDAP Custom DIT base DN for config branch
    public static final KnownKey ldap_dit_base_dn_config        = KnownKey.newKey("");
    //LDAP Custom DIT base DN for cos entries
    public static final KnownKey ldap_dit_base_dn_cos           = KnownKey.newKey("");
    //LDAP Custom DIT base DN for domain entries
    public static final KnownKey ldap_dit_base_dn_domain        = KnownKey.newKey("");
    // LDAP Custom DIT base DN for mail(accounts, aliases, DLs, resources) entries
    public static final KnownKey ldap_dit_base_dn_mail          = KnownKey.newKey("");
    // LDAP Custom DIT base DN for mime entries"
    public static final KnownKey ldap_dit_base_dn_mime          = KnownKey.newKey("");
    // LDAP Custom DIT base DN for server entries
    public static final KnownKey ldap_dit_base_dn_server        = KnownKey.newKey("");
    // LDAP Custom DIT base DN for xmpp component entries
    public static final KnownKey ldap_dit_base_dn_xmppcomponent = KnownKey.newKey("");
    // LDAP Custom DIT base DN for zimlet entries
    public static final KnownKey ldap_dit_base_dn_zimlet        = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for cos entries
    public static final KnownKey ldap_dit_naming_rdn_attr_cos          = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for globalconfig entry
    public static final KnownKey ldap_dit_naming_rdn_attr_globalconfig = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for globalgrant entry
    public static final KnownKey ldap_dit_naming_rdn_attr_globalgrant  = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for group entries
    public static final KnownKey ldap_dit_naming_rdn_attr_group = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for mime entries
    public static final KnownKey ldap_dit_naming_rdn_attr_mime         = KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for server entries
    public static final KnownKey ldap_dit_naming_rdn_attr_server       = KnownKey.newKey("");
    public static final KnownKey ldap_dit_naming_rdn_attr_user         =KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for xmpp component entries
    public static final KnownKey ldap_dit_naming_rdn_attr_xmppcomponent= KnownKey.newKey("");
    // LDAP Custom DIT RDN attr for zimlet entries
    public static final KnownKey ldap_dit_naming_rdn_attr_zimlet       = KnownKey.newKey("");
    // LDAP Custom DIT base DN for LDAP admin entries
    public static final KnownKey ldap_dit_base_dn_admin         = KnownKey.newKey("");

    @Supported
    public static final KnownKey zmprov_tmp_directory = KnownKey.newKey("${zimbra_tmp_directory}/zmprov");

    public static final KnownKey command_line_editing_enabled = KnownKey.newKey(true);
    public static final KnownKey thread_pool_warn_percent = KnownKey.newKey(100);

    public static final KnownKey robots_txt = KnownKey.newKey("${zimbra_home}/conf/robots.txt");

    @Supported
    public static final KnownKey compute_aggregate_quota_threads = KnownKey.newKey(10);

    // Remove this in 8.0.
    public static final KnownKey filter_null_env_sender_for_dsn_redirect = KnownKey.newKey(true);

    //appliance
    public static final KnownKey zimbra_vami_user = KnownKey.newKey("vmware");
    public static final KnownKey zimbra_vami_password = KnownKey.newKey("vmware").protect();
    public static final KnownKey zimbra_vami_installmode = KnownKey.newKey("single");

    public static final KnownKey max_image_size_to_resize = KnownKey.newKey(10 * 1024 * 1024);

    //octopus
    public static final KnownKey documents_disable_instant_parsing = KnownKey.newKey(false);
    public static final KnownKey default_profile_image = KnownKey.newKey("${zimbra_home}/jetty/webapps/zimbra/img/no-photo.png");
    public static final KnownKey rest_pdf_converter_url = KnownKey.newKey("http://localhost:7070/zoo/convertpdf");
    public static final KnownKey pdf2swf_path = KnownKey.newKey("/sw/bin/pdf2swf");

    public static final KnownKey octopus_incoming_patch_max_age = KnownKey.newKey(120); // 120 mins = 2 hours
    public static final KnownKey octopus_stored_patch_max_age = KnownKey.newKey(360); // 360 mins = 6 hours

    static {
        // Automatically set the key name with the variable name.
        for (Field field : LC.class.getFields()) {
            if (field.getType() == KnownKey.class) {
                assert(Modifier.isPublic(field.getModifiers()));
                assert(Modifier.isStatic(field.getModifiers()));
                assert(Modifier.isFinal(field.getModifiers()));
                try {
                    KnownKey key = (KnownKey) field.get(null);
                    // Automatically set the key name with the variable name.
                    key.setKey(field.getName());

                    // process annotations
                    if(field.isAnnotationPresent(Supported.class)) {
                        key.setSupported(true);
                    }
                    if(field.isAnnotationPresent(Reloadable.class)) {
                        key.setReloadable(true);
                    }

                } catch (Throwable never) {
                    assert false : never;
                }
            }
        }
    }

    /**
     * Used to apply the reloadable flag to a KnownKey in LC
     * @author jpowers
     *
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Reloadable {
    }

    /**
     * This annotation represents a supported local config setting. To make a new
     * setting show up in the zmlocalconfig -i command, use this annotation
     *
     * @author jpowers
     *
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Supported {

    }



}
