/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;


import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author schemers
 */
public class AdminService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(AdminConstants.PING_REQUEST, new Ping());
        dispatcher.registerHandler(AdminConstants.CHECK_HEALTH_REQUEST, new CheckHealth());

        dispatcher.registerHandler(AdminConstants.AUTH_REQUEST, new Auth());
        dispatcher.registerHandler(AdminConstants.CREATE_ACCOUNT_REQUEST, new CreateAccount());
        dispatcher.registerHandler(AdminConstants.DELEGATE_AUTH_REQUEST, new DelegateAuth());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_REQUEST, new GetAccount());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_INFO_REQUEST, new GetAccountInfo());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ACCOUNTS_REQUEST, new GetAllAccounts());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ADMIN_ACCOUNTS_REQUEST, new GetAllAdminAccounts());
        dispatcher.registerHandler(AdminConstants.MODIFY_ACCOUNT_REQUEST, new ModifyAccount());
        dispatcher.registerHandler(AdminConstants.DELETE_ACCOUNT_REQUEST, new DeleteAccount());
        dispatcher.registerHandler(AdminConstants.SET_PASSWORD_REQUEST, new SetPassword());
        dispatcher.registerHandler(AdminConstants.CHECK_PASSWORD_STRENGTH_REQUEST, new CheckPasswordStrength());
        dispatcher.registerHandler(AdminConstants.ADD_ACCOUNT_ALIAS_REQUEST, new AddAccountAlias());
        dispatcher.registerHandler(AdminConstants.REMOVE_ACCOUNT_ALIAS_REQUEST, new RemoveAccountAlias());
        dispatcher.registerHandler(AdminConstants.SEARCH_ACCOUNTS_REQUEST, new SearchAccounts());
        dispatcher.registerHandler(AdminConstants.RENAME_ACCOUNT_REQUEST, new RenameAccount());

        dispatcher.registerHandler(AdminConstants.SEARCH_DIRECTORY_REQUEST, new SearchDirectory());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_MEMBERSHIP_REQUEST, new GetAccountMembership());

        dispatcher.registerHandler(AdminConstants.CREATE_DOMAIN_REQUEST, new CreateDomain());
        dispatcher.registerHandler(AdminConstants.GET_DOMAIN_REQUEST, new GetDomain());
        dispatcher.registerHandler(AdminConstants.GET_DOMAIN_INFO_REQUEST, new GetDomainInfo());
        dispatcher.registerHandler(AdminConstants.GET_ALL_DOMAINS_REQUEST, new GetAllDomains());
        dispatcher.registerHandler(AdminConstants.MODIFY_DOMAIN_REQUEST, new ModifyDomain());
        dispatcher.registerHandler(AdminConstants.DELETE_DOMAIN_REQUEST, new DeleteDomain());

        dispatcher.registerHandler(AdminConstants.CREATE_COS_REQUEST, new CreateCos());
        dispatcher.registerHandler(AdminConstants.COPY_COS_REQUEST, new CopyCos());
        dispatcher.registerHandler(AdminConstants.GET_COS_REQUEST, new GetCos());
        dispatcher.registerHandler(AdminConstants.GET_ALL_COS_REQUEST, new GetAllCos());
        dispatcher.registerHandler(AdminConstants.MODIFY_COS_REQUEST, new ModifyCos());
        dispatcher.registerHandler(AdminConstants.DELETE_COS_REQUEST, new DeleteCos());
        dispatcher.registerHandler(AdminConstants.RENAME_COS_REQUEST, new RenameCos());

        dispatcher.registerHandler(AdminConstants.CREATE_SERVER_REQUEST, new CreateServer());
        dispatcher.registerHandler(AdminConstants.GET_SERVER_REQUEST, new GetServer());
        dispatcher.registerHandler(AdminConstants.GET_ALL_SERVERS_REQUEST, new GetAllServers());
        dispatcher.registerHandler(AdminConstants.MODIFY_SERVER_REQUEST, new ModifyServer());
        dispatcher.registerHandler(AdminConstants.DELETE_SERVER_REQUEST, new DeleteServer());

        dispatcher.registerHandler(AdminConstants.GET_CONFIG_REQUEST, new GetConfig());
        dispatcher.registerHandler(AdminConstants.GET_ALL_CONFIG_REQUEST, new GetAllConfig());
        dispatcher.registerHandler(AdminConstants.MODIFY_CONFIG_REQUEST, new ModifyConfig());

        dispatcher.registerHandler(AdminConstants.GET_SERVICE_STATUS_REQUEST, new GetServiceStatus());

        dispatcher.registerHandler(AdminConstants.PURGE_MESSAGES_REQUEST, new PurgeMessages());
        dispatcher.registerHandler(AdminConstants.DELETE_MAILBOX_REQUEST, new DeleteMailbox());
        dispatcher.registerHandler(AdminConstants.GET_MAILBOX_REQUEST, new GetMailbox());

        dispatcher.registerHandler(AdminConstants.MAINTAIN_TABLES_REQUEST, new MaintainTables());

        dispatcher.registerHandler(AdminConstants.RUN_UNIT_TESTS_REQUEST, new RunUnitTests());

        dispatcher.registerHandler(AdminConstants.CHECK_AUTH_CONFIG_REQUEST, new CheckAuthConfig());
        dispatcher.registerHandler(AdminConstants.CHECK_GAL_CONFIG_REQUEST, new CheckGalConfig());
        dispatcher.registerHandler(AdminConstants.CHECK_HOSTNAME_RESOLVE_REQUEST, new CheckHostnameResolve());

        dispatcher.registerHandler(AdminConstants.CREATE_VOLUME_REQUEST, new CreateVolume());
        dispatcher.registerHandler(AdminConstants.GET_VOLUME_REQUEST, new GetVolume());
        dispatcher.registerHandler(AdminConstants.GET_ALL_VOLUMES_REQUEST, new GetAllVolumes());
        dispatcher.registerHandler(AdminConstants.MODIFY_VOLUME_REQUEST, new ModifyVolume());
        dispatcher.registerHandler(AdminConstants.DELETE_VOLUME_REQUEST, new DeleteVolume());
        dispatcher.registerHandler(AdminConstants.GET_CURRENT_VOLUMES_REQUEST, new GetCurrentVolumes());
        dispatcher.registerHandler(AdminConstants.SET_CURRENT_VOLUME_REQUEST, new SetCurrentVolume());

        dispatcher.registerHandler(AdminConstants.CREATE_DISTRIBUTION_LIST_REQUEST, new CreateDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_DISTRIBUTION_LIST_REQUEST, new GetDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_REQUEST, new GetAllDistributionLists());
        dispatcher.registerHandler(AdminConstants.ADD_DISTRIBUTION_LIST_MEMBER_REQUEST, new AddDistributionListMember());
        dispatcher.registerHandler(AdminConstants.REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST, new RemoveDistributionListMember());
        dispatcher.registerHandler(AdminConstants.MODIFY_DISTRIBUTION_LIST_REQUEST, new ModifyDistributionList());
        dispatcher.registerHandler(AdminConstants.DELETE_DISTRIBUTION_LIST_REQUEST, new DeleteDistributionList());
        dispatcher.registerHandler(AdminConstants.ADD_DISTRIBUTION_LIST_ALIAS_REQUEST, new AddDistributionListAlias());
        dispatcher.registerHandler(AdminConstants.REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST, new RemoveDistributionListAlias());
        dispatcher.registerHandler(AdminConstants.RENAME_DISTRIBUTION_LIST_REQUEST, new RenameDistributionList());
        dispatcher.registerHandler(AdminConstants.GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST, new GetDistributionListMembership());

        dispatcher.registerHandler(AdminConstants.GET_VERSION_INFO_REQUEST, new GetVersionInfo());
        dispatcher.registerHandler(AdminConstants.GET_LICENSE_INFO_REQUEST, new GetLicenseInfo());

        dispatcher.registerHandler(AdminConstants.REINDEX_REQUEST, new ReIndex());

        // zimlet
        dispatcher.registerHandler(AdminConstants.GET_ZIMLET_REQUEST, new GetZimlet());
        dispatcher.registerHandler(AdminConstants.CREATE_ZIMLET_REQUEST, new CreateZimlet());
        dispatcher.registerHandler(AdminConstants.DELETE_ZIMLET_REQUEST, new DeleteZimlet());
        dispatcher.registerHandler(AdminConstants.GET_ADMIN_EXTENSION_ZIMLETS_REQUEST, new GetAdminExtensionZimlets());
        dispatcher.registerHandler(AdminConstants.GET_ZIMLET_STATUS_REQUEST, new GetZimletStatus());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ZIMLETS_REQUEST, new GetAllZimlets());
        dispatcher.registerHandler(AdminConstants.DEPLOY_ZIMLET_REQUEST, new DeployZimlet());
        dispatcher.registerHandler(AdminConstants.UNDEPLOY_ZIMLET_REQUEST, new UndeployZimlet());
        dispatcher.registerHandler(AdminConstants.CONFIGURE_ZIMLET_REQUEST, new ConfigureZimlet());
        dispatcher.registerHandler(AdminConstants.MODIFY_ZIMLET_REQUEST, new ModifyZimlet());
        dispatcher.registerHandler(AdminConstants.DUMP_SESSIONS_REQUEST, new DumpSessions());
        dispatcher.registerHandler(AdminConstants.GET_SESSIONS_REQUEST, new GetSessions());

        // calendar resources
        dispatcher.registerHandler(AdminConstants.CREATE_CALENDAR_RESOURCE_REQUEST,   new CreateCalendarResource());
        dispatcher.registerHandler(AdminConstants.DELETE_CALENDAR_RESOURCE_REQUEST,   new DeleteCalendarResource());
        dispatcher.registerHandler(AdminConstants.MODIFY_CALENDAR_RESOURCE_REQUEST,   new ModifyCalendarResource());
        dispatcher.registerHandler(AdminConstants.RENAME_CALENDAR_RESOURCE_REQUEST,   new RenameCalendarResource());
        dispatcher.registerHandler(AdminConstants.GET_CALENDAR_RESOURCE_REQUEST,      new GetCalendarResource());
        dispatcher.registerHandler(AdminConstants.GET_ALL_CALENDAR_RESOURCES_REQUEST, new GetAllCalendarResources());
        dispatcher.registerHandler(AdminConstants.SEARCH_CALENDAR_RESOURCES_REQUEST,  new SearchCalendarResources());

        // QUOTA
        dispatcher.registerHandler(AdminConstants.GET_QUOTA_USAGE_REQUEST, new GetQuotaUsage());

        // Mail queue management
        dispatcher.registerHandler(AdminConstants.GET_MAIL_QUEUE_INFO_REQUEST, new GetMailQueueInfo());
        dispatcher.registerHandler(AdminConstants.GET_MAIL_QUEUE_REQUEST, new GetMailQueue());
        dispatcher.registerHandler(AdminConstants.MAIL_QUEUE_ACTION_REQUEST, new MailQueueAction());
        dispatcher.registerHandler(AdminConstants.MAIL_QUEUE_FLUSH_REQUEST, new MailQueueFlush());

        dispatcher.registerHandler(AdminConstants.INIT_NOTEBOOK_REQUEST, new InitNotebook());

        dispatcher.registerHandler(AdminConstants.AUTO_COMPLETE_GAL_REQUEST, new AutoCompleteGal());
        dispatcher.registerHandler(AdminConstants.SEARCH_GAL_REQUEST, new SearchGal());

        // throttling
        dispatcher.registerHandler(AdminConstants.SET_THROTTLE_REQUEST, new SetThrottle());

        // data source
        dispatcher.registerHandler(AdminConstants.GET_DATA_SOURCES_REQUEST, new GetDataSources());
        dispatcher.registerHandler(AdminConstants.CREATE_DATA_SOURCE_REQUEST, new CreateDataSource());
        dispatcher.registerHandler(AdminConstants.MODIFY_DATA_SOURCE_REQUEST, new ModifyDataSource());
        dispatcher.registerHandler(AdminConstants.DELETE_DATA_SOURCE_REQUEST, new DeleteDataSource());

        // calendar time zone fixup
        dispatcher.registerHandler(AdminConstants.FIX_CALENDAR_TIME_ZONE_REQUEST, new FixCalendarTimeZone());
        dispatcher.registerHandler(AdminConstants.FIX_CALENDAR_TZ_REQUEST, new FixCalendarTZ());
        // calendar item end time fixup
        dispatcher.registerHandler(AdminConstants.FIX_CALENDAR_END_TIME_REQUEST, new FixCalendarEndTime());
        
        // admin saved searches
        dispatcher.registerHandler(AdminConstants.GET_ADMIN_SAVED_SEARCHES_REQUEST, new GetAdminSavedSearches());
        dispatcher.registerHandler(AdminConstants.MODIFY_ADMIN_SAVED_SEARCHES_REQUEST, new ModifyAdminSavedSearches());
        
        dispatcher.registerHandler(AdminConstants.ADD_ACCOUNT_LOGGER_REQUEST, new AddAccountLogger());
        dispatcher.registerHandler(AdminConstants.REMOVE_ACCOUNT_LOGGER_REQUEST, new RemoveAccountLogger());
        dispatcher.registerHandler(AdminConstants.GET_ACCOUNT_LOGGERS_REQUEST, new GetAccountLoggers());
        dispatcher.registerHandler(AdminConstants.GET_ALL_ACCOUNT_LOGGERS_REQUEST, new GetAllAccountLoggers());
        
        dispatcher.registerHandler(AdminConstants.CHECK_DIRECTORY_REQUEST, new CheckDirectory());
        
        dispatcher.registerHandler(AdminConstants.FLUSH_CACHE_REQUEST, new FlushCache());
        
        dispatcher.registerHandler(AdminConstants.GET_SERVER_NIFS_REQUEST, new GetServerNIFs());
        
        // f/b mgmt
        dispatcher.registerHandler(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_REQUEST, new GetAllFreeBusyProviders());
        dispatcher.registerHandler(AdminConstants.GET_FREE_BUSY_QUEUE_INFO_REQUEST, new GetFreeBusyQueueInfo());
        dispatcher.registerHandler(AdminConstants.PUSH_FREE_BUSY_REQUEST, new PushFreeBusy());
    }

    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map<String, Object> getAttrs(Element request) throws ServiceException {
        return getAttrs(request, false);
    }
    
    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map<String, Object> getAttrs(Element request, boolean ignoreEmptyValues) throws ServiceException {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Element a : request.listElements(AdminConstants.E_A)) {
            String name = a.getAttribute(AdminConstants.A_N);
            String value = a.getText();
            if (!ignoreEmptyValues || (value != null && value.length() > 0))
                StringUtil.addToMultiMap(result, name, value);
        }
        return result;
    }    
}
