/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.admin;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.StringUtil;
import com.liquidsys.soap.DocumentDispatcher;
import com.liquidsys.soap.DocumentService;

/**
 * @author schemers
 */
public class AdminService implements DocumentService {

	public static final String NAMESPACE_STR = "urn:liquidAdmin";
	public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
	
	public static final QName PING_REQUEST = QName.get("PingRequest", NAMESPACE);
	public static final QName PING_RESPONSE = QName.get("PingResponse", NAMESPACE);
    public static final QName CHECK_HEALTH_REQUEST = QName.get("CheckHealthRequest", NAMESPACE);
    public static final QName CHECK_HEALTH_RESPONSE = QName.get("CheckHealthResponse", NAMESPACE);

	public static final QName EXPORTMAILBOX_REQUEST = QName.get("ExportMailboxRequest", NAMESPACE);
	public static final QName EXPORTMAILBOX_RESPONSE = QName.get("ExportMailboxResponse", NAMESPACE);
	
    public static final QName AUTH_REQUEST = QName.get("AuthRequest", NAMESPACE);
    public static final QName AUTH_RESPONSE = QName.get("AuthResponse", NAMESPACE);
    public static final QName CREATE_ACCOUNT_REQUEST = QName.get("CreateAccountRequest", NAMESPACE);
    public static final QName CREATE_ACCOUNT_RESPONSE = QName.get("CreateAccountResponse", NAMESPACE);
    public static final QName CREATE_ADMIN_ACCOUNT_REQUEST = QName.get("CreateAdminAccountRequest", NAMESPACE);
    public static final QName CREATE_ADMIN_ACCOUNT_RESPONSE = QName.get("CreateAdminAccountResponse", NAMESPACE);
    public static final QName DELEGATE_AUTH_REQUEST = QName.get("DelegateAuthRequest", NAMESPACE);
    public static final QName DELEGATE_AUTH_RESPONSE = QName.get("DelegateAuthResponse", NAMESPACE);    
    public static final QName GET_ACCOUNT_REQUEST = QName.get("GetAccountRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_RESPONSE = QName.get("GetAccountResponse", NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_REQUEST = QName.get("GetAllAccountsRequest", NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_RESPONSE = QName.get("GetAllAccountsResponse", NAMESPACE);    
    public static final QName GET_ALL_ADMIN_ACCOUNTS_REQUEST = QName.get("GetAllAdminAccountsRequest", NAMESPACE);
    public static final QName GET_ALL_ADMIN_ACCOUNTS_RESPONSE = QName.get("GetAllAdminAccountsResponse", NAMESPACE);
    public static final QName MODIFY_ACCOUNT_REQUEST = QName.get("ModifyAccountRequest", NAMESPACE);
    public static final QName MODIFY_ACCOUNT_RESPONSE = QName.get("ModifyAccountResponse", NAMESPACE);
    public static final QName DELETE_ACCOUNT_REQUEST = QName.get("DeleteAccountRequest", NAMESPACE);
    public static final QName DELETE_ACCOUNT_RESPONSE = QName.get("DeleteAccountResponse", NAMESPACE);
    public static final QName SET_PASSWORD_REQUEST = QName.get("SetPasswordRequest", NAMESPACE);
    public static final QName SET_PASSWORD_RESPONSE = QName.get("SetPasswordResponse", NAMESPACE);
    public static final QName ADD_ACCOUNT_ALIAS_REQUEST = QName.get("AddAccountAliasRequest", NAMESPACE);
    public static final QName ADD_ACCOUNT_ALIAS_RESPONSE = QName.get("AddAccountAliasResponse", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_REQUEST = QName.get("RemoveAccountAliasRequest", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_RESPONSE = QName.get("RemoveAccountAliasResponse", NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_REQUEST = QName.get("SearchAccountsRequest", NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_RESPONSE = QName.get("SearchAccountsResponse", NAMESPACE);    
    public static final QName RENAME_ACCOUNT_REQUEST = QName.get("RenameAccountRequest", NAMESPACE);
    public static final QName RENAME_ACCOUNT_RESPONSE = QName.get("RenameAccountResponse", NAMESPACE);

    public static final QName CREATE_DOMAIN_REQUEST = QName.get("CreateDomainRequest", NAMESPACE);
    public static final QName CREATE_DOMAIN_RESPONSE = QName.get("CreateDomainResponse", NAMESPACE);
    public static final QName GET_DOMAIN_REQUEST = QName.get("GetDomainRequest", NAMESPACE);
    public static final QName GET_DOMAIN_RESPONSE = QName.get("GetDomainResponse", NAMESPACE);
    public static final QName MODIFY_DOMAIN_REQUEST = QName.get("ModifyDomainRequest", NAMESPACE);
    public static final QName MODIFY_DOMAIN_RESPONSE = QName.get("ModifyDomainResponse", NAMESPACE);
    public static final QName DELETE_DOMAIN_REQUEST = QName.get("DeleteDomainRequest", NAMESPACE);
    public static final QName DELETE_DOMAIN_RESPONSE = QName.get("DeleteDomainResponse", NAMESPACE);
    public static final QName GET_ALL_DOMAINS_REQUEST = QName.get("GetAllDomainsRequest", NAMESPACE);
    public static final QName GET_ALL_DOMAINS_RESPONSE = QName.get("GetAllDomainsResponse", NAMESPACE);
    
    public static final QName CREATE_COS_REQUEST = QName.get("CreateCosRequest", NAMESPACE);
    public static final QName CREATE_COS_RESPONSE = QName.get("CreateCosResponse", NAMESPACE);
    public static final QName GET_COS_REQUEST = QName.get("GetCosRequest", NAMESPACE);
    public static final QName GET_COS_RESPONSE = QName.get("GetCosResponse", NAMESPACE);
    public static final QName MODIFY_COS_REQUEST = QName.get("ModifyCosRequest", NAMESPACE);
    public static final QName MODIFY_COS_RESPONSE = QName.get("ModifyCosResponse", NAMESPACE);
    public static final QName DELETE_COS_REQUEST = QName.get("DeleteCosRequest", NAMESPACE);
    public static final QName DELETE_COS_RESPONSE = QName.get("DeleteCosResponse", NAMESPACE);
    public static final QName GET_ALL_COS_REQUEST = QName.get("GetAllCosRequest", NAMESPACE);
    public static final QName GET_ALL_COS_RESPONSE = QName.get("GetAllCosResponse", NAMESPACE);    
    public static final QName RENAME_COS_REQUEST = QName.get("RenameCosRequest", NAMESPACE);        
    public static final QName RENAME_COS_RESPONSE = QName.get("RenameCosResponse", NAMESPACE);            
    
    public static final QName CREATE_SERVER_REQUEST = QName.get("CreateServerRequest", NAMESPACE);
    public static final QName CREATE_SERVER_RESPONSE = QName.get("CreateServerResponse", NAMESPACE);
    public static final QName GET_SERVER_REQUEST = QName.get("GetServerRequest", NAMESPACE);
    public static final QName GET_SERVER_RESPONSE = QName.get("GetServerResponse", NAMESPACE);
    public static final QName MODIFY_SERVER_REQUEST = QName.get("ModifyServerRequest", NAMESPACE);
    public static final QName MODIFY_SERVER_RESPONSE = QName.get("ModifyServerResponse", NAMESPACE);
    public static final QName DELETE_SERVER_REQUEST = QName.get("DeleteServerRequest", NAMESPACE);
    public static final QName DELETE_SERVER_RESPONSE = QName.get("DeleteServerResponse", NAMESPACE);
    public static final QName GET_ALL_SERVERS_REQUEST = QName.get("GetAllServersRequest", NAMESPACE);
    public static final QName GET_ALL_SERVERS_RESPONSE = QName.get("GetAllServersResponse", NAMESPACE);        

    public static final QName GET_CONFIG_REQUEST = QName.get("GetConfigRequest", NAMESPACE);
    public static final QName GET_CONFIG_RESPONSE = QName.get("GetConfigResponse", NAMESPACE);
    public static final QName MODIFY_CONFIG_REQUEST = QName.get("ModifyConfigRequest", NAMESPACE);
    public static final QName MODIFY_CONFIG_RESPONSE = QName.get("ModifyConfigResponse", NAMESPACE);
    public static final QName GET_ALL_CONFIG_REQUEST = QName.get("GetAllConfigRequest", NAMESPACE);
    public static final QName GET_ALL_CONFIG_RESPONSE = QName.get("GetAllConfigResponse", NAMESPACE);    
    
    public static final QName GET_SERVER_AGGREGATE_STATS_REQUEST = QName.get("GetServerAggregateStatsRequest", NAMESPACE);
    public static final QName GET_SERVER_AGGREGATE_STATS_RESPONSE = QName.get("GetServerAggregateStatsResponse", NAMESPACE);
    
    public static final QName GET_SERVICE_STATUS_REQUEST = QName.get("GetServiceStatusRequest", NAMESPACE);
    public static final QName GET_SERVICE_STATUS_RESPONSE = QName.get("GetServiceStatusResponse", NAMESPACE);
    
    public static final QName BACKUP_REQUEST = QName.get("BackupRequest", NAMESPACE);
    public static final QName BACKUP_RESPONSE = QName.get("BackupResponse", NAMESPACE);
    public static final QName BACKUP_QUERY_REQUEST = QName.get("BackupQueryRequest", NAMESPACE);
    public static final QName BACKUP_QUERY_RESPONSE = QName.get("BackupQueryResponse", NAMESPACE);
    public static final QName BACKUP_ACCOUNT_QUERY_REQUEST = QName.get("BackupAccountQueryRequest", NAMESPACE);
    public static final QName BACKUP_ACCOUNT_QUERY_RESPONSE = QName.get("BackupAccountQueryResponse", NAMESPACE);
    public static final QName RESTORE_REQUEST = QName.get("RestoreRequest", NAMESPACE);
    public static final QName RESTORE_RESPONSE = QName.get("RestoreResponse", NAMESPACE);
    public static final QName PURGE_MESSAGES_REQUEST = QName.get("PurgeMessagesRequest", NAMESPACE);
    public static final QName PURGE_MESSAGES_RESPONSE= QName.get("PurgeMessagesResponse", NAMESPACE);
    public static final QName DELETE_MAILBOX_REQUEST = QName.get("DeleteMailboxRequest", NAMESPACE);
    public static final QName DELETE_MAILBOX_RESPONSE= QName.get("DeleteMailboxResponse", NAMESPACE);
    public static final QName GET_MAILBOX_REQUEST = QName.get("GetMailboxRequest", NAMESPACE);
    public static final QName GET_MAILBOX_RESPONSE= QName.get("GetMailboxResponse", NAMESPACE);    

    public static final QName SEARCH_MULTIPLE_MAILBOXES_REQUEST = QName.get("SearchMultiMailboxRequest", NAMESPACE);
    public static final QName SEARCH_MULTIPLE_MAILBOXES_RESPONSE = QName.get("SearchMultiMailboxResponse", NAMESPACE);
    
    public static final QName MAINTAIN_TABLES_REQUEST = QName.get("MaintainTablesRequest", NAMESPACE);
    public static final QName MAINTAIN_TABLES_RESPONSE = QName.get("MaintainTablesResponse", NAMESPACE);
    
    public static final QName RUN_UNIT_TESTS_REQUEST = QName.get("RunUnitTestsRequest", NAMESPACE);
    public static final QName RUN_UNIT_TESTS_RESPONSE = QName.get("RunUnitTestsResponse", NAMESPACE);
    
    public static final QName CHECK_HOSTNAME_RESOLVE_REQUEST = QName.get("CheckHostnameResolveRequest", NAMESPACE);
    public static final QName CHECK_HOSTNAME_RESOLVE_RESPONSE = QName.get("CheckHostnameResolveResponse", NAMESPACE);    
    public static final QName CHECK_AUTH_CONFIG_REQUEST = QName.get("CheckAuthConfigRequest", NAMESPACE);
    public static final QName CHECK_AUTH_CONFIG_RESPONSE = QName.get("CheckAuthConfigResponse", NAMESPACE);    
    public static final QName CHECK_GAL_CONFIG_REQUEST = QName.get("CheckGalConfigRequest", NAMESPACE);
    public static final QName CHECK_GAL_CONFIG_RESPONSE = QName.get("CheckGalConfigResponse", NAMESPACE);        

    public static final String E_ACCOUNT = "account";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_NAME = "name";
    public static final String E_NEW_NAME = "newName";
    public static final String E_BINDDN = "bindDn";
    public static final String E_CODE = "code";
    public static final String E_COS = "cos";
    public static final String E_CN = "cn";    
    public static final String E_DOMAIN = "domain";
    public static final String E_HOSTNAME = "hostname";    
    public static final String E_LIFETIME = "lifetime";
    public static final String E_MESSAGE = "message";
    public static final String E_PASSWORD = "password";
    public static final String E_NEW_PASSWORD = "newPassword";
    public static final String E_QUERY = "query";
    public static final String E_SERVER = "server";
    public static final String E_STATUS = "status";    
    public static final String E_END_TIME = "endTime";
    public static final String E_START_TIME = "startTime";
    public static final String E_STAT_NAME = "statName";
    public static final String E_PERIOD = "period";
    public static final String E_A = "a";
    public static final String E_S = "s";
    public static final String E_ALIAS = "alias";    
	public static final String E_ID = "id";		
	public static final String E_PAGE_NUMBER = "pagenum";
	public static final String E_ORDER_BY = "orderby";
	public static final String E_IS_ASCENDING = "isascending";
	public static final String E_RESULTS_PERPAGE = "pageresultsnum";
	public static final String E_ATTRS_TO_GET = "attrstoget";
	public static final String E_MAX_SEARCH_RESULTS = "maxsearchresults";
	public static final String E_MAILBOX = "mbox";
	public static final String E_NUM_OF_PAGES = "numpages";
	
    public static final String A_APPLY_CONFIG = "applyConfig";
    public static final String A_APPLY_COS = "applyCos";
    public static final String A_ID = "id";
    public static final String A_LIMIT = "limit";
    public static final String A_OFFSET = "offset";    
    public static final String A_DOMAIN = "domain";
    public static final String A_ATTRS = "attrs";  
    public static final String A_SEARCH_TOTAL = "searchTotal";
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";    
    public static final String A_C = "c";    
    public static final String A_T = "t";    
    public static final String A_NAME = "name"; 
    public static final String A_MORE = "more";     
    public static final String A_BY = "by";
	public static final String A_N = "n";
	public static final String A_HOSTNAME = "hn";
    public static final String A_ACCOUNTID = "id";
    public static final String A_MAILBOXID = "mbxid";
    
    public static final String A_HEALTHY = "healthy";
    public static final String A_SIZE = "s";
    public static final String A_SERVICE = "service";
    public static final String A_SERVER = "server";
    public static final String A_STATUS = "status";        
    public static final String A_TIME = "time";
    public static final String A_NUM_TABLES = "numTables";
    
    public static final String A_NUM_EXECUTED = "numExecuted";
    public static final String A_NUM_FAILED = "numFailed";
    public static final String A_OUTPUT = "output";
    public static final String A_DURATION = "duration";

	public static final String ADMIN_URI = "https://localhost:7071/";
	
    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(PING_REQUEST, new Ping());
        dispatcher.registerHandler(CHECK_HEALTH_REQUEST, new CheckHealth());

        dispatcher.registerHandler(AUTH_REQUEST, new Auth());
		dispatcher.registerHandler(CREATE_ACCOUNT_REQUEST, new CreateAccount());
        dispatcher.registerHandler(DELEGATE_AUTH_REQUEST, new DelegateAuth());        
        dispatcher.registerHandler(GET_ACCOUNT_REQUEST, new GetAccount());
        dispatcher.registerHandler(GET_ALL_ACCOUNTS_REQUEST, new GetAllAccounts());        
        dispatcher.registerHandler(GET_ALL_ADMIN_ACCOUNTS_REQUEST, new GetAllAdminAccounts());
        dispatcher.registerHandler(MODIFY_ACCOUNT_REQUEST, new ModifyAccount());
        dispatcher.registerHandler(DELETE_ACCOUNT_REQUEST, new DeleteAccount());
        dispatcher.registerHandler(SET_PASSWORD_REQUEST, new SetPassword());
        dispatcher.registerHandler(ADD_ACCOUNT_ALIAS_REQUEST, new AddAccountAlias());
        dispatcher.registerHandler(REMOVE_ACCOUNT_ALIAS_REQUEST, new RemoveAccountAlias());
        dispatcher.registerHandler(SEARCH_ACCOUNTS_REQUEST, new SearchAccounts());        
        dispatcher.registerHandler(RENAME_ACCOUNT_REQUEST, new RenameAccount());        
        
        dispatcher.registerHandler(CREATE_DOMAIN_REQUEST, new CreateDomain());
        dispatcher.registerHandler(GET_DOMAIN_REQUEST, new GetDomain());
        dispatcher.registerHandler(GET_ALL_DOMAINS_REQUEST, new GetAllDomains());
        dispatcher.registerHandler(MODIFY_DOMAIN_REQUEST, new ModifyDomain());
        dispatcher.registerHandler(DELETE_DOMAIN_REQUEST, new DeleteDomain());

        dispatcher.registerHandler(CREATE_COS_REQUEST, new CreateCos());
        dispatcher.registerHandler(GET_COS_REQUEST, new GetCos());
        dispatcher.registerHandler(GET_ALL_COS_REQUEST, new GetAllCos());
        dispatcher.registerHandler(MODIFY_COS_REQUEST, new ModifyCos());
        dispatcher.registerHandler(DELETE_COS_REQUEST, new DeleteCos());
        dispatcher.registerHandler(RENAME_COS_REQUEST, new RenameCos());                
        
        dispatcher.registerHandler(CREATE_SERVER_REQUEST, new CreateServer());
        dispatcher.registerHandler(GET_SERVER_REQUEST, new GetServer());
        dispatcher.registerHandler(GET_ALL_SERVERS_REQUEST, new GetAllServers());
        dispatcher.registerHandler(MODIFY_SERVER_REQUEST, new ModifyServer());
        dispatcher.registerHandler(DELETE_SERVER_REQUEST, new DeleteServer());
        
        dispatcher.registerHandler(GET_CONFIG_REQUEST, new GetConfig());
        dispatcher.registerHandler(GET_ALL_CONFIG_REQUEST, new GetAllConfig());
        dispatcher.registerHandler(MODIFY_CONFIG_REQUEST, new ModifyConfig());
        
        dispatcher.registerHandler(GET_SERVER_AGGREGATE_STATS_REQUEST, new GetServerAggregateStats());
        dispatcher.registerHandler(GET_SERVICE_STATUS_REQUEST, new GetServiceStatus());        
        
        dispatcher.registerHandler(PURGE_MESSAGES_REQUEST, new PurgeMessages());
        dispatcher.registerHandler(DELETE_MAILBOX_REQUEST, new DeleteMailbox());
        dispatcher.registerHandler(GET_MAILBOX_REQUEST, new GetMailbox());        

        dispatcher.registerHandler(SEARCH_MULTIPLE_MAILBOXES_REQUEST, new SearchMultipleMailboxes());

        dispatcher.registerHandler(MAINTAIN_TABLES_REQUEST, new MaintainTables());
        
        dispatcher.registerHandler(RUN_UNIT_TESTS_REQUEST, new RunUnitTests());
        
        dispatcher.registerHandler(CHECK_AUTH_CONFIG_REQUEST, new CheckAuthConfig());
        dispatcher.registerHandler(CHECK_GAL_CONFIG_REQUEST, new CheckGalConfig());
        dispatcher.registerHandler(CHECK_HOSTNAME_RESOLVE_REQUEST, new CheckHostnameResolve());
    }

    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map getAttrs(Element request) throws ServiceException {
        return getAttrs(request, false);
    }
    
    /**
     * @param request
     * @return
     * @throws ServiceException
     */
    public static Map getAttrs(Element request, boolean ignoreEmptyValues) throws ServiceException {
        HashMap result = new HashMap();
        for (Iterator ait = request.elementIterator(AdminService.E_A); ait.hasNext(); ) {
            Element a = (Element) ait.next();
            String name = a.getAttribute(AdminService.A_N);
            String value = a.getText();
            if (!ignoreEmptyValues || (value != null && value.length() > 0))
                StringUtil.addToMultiMap(result, name, value);
        }
        return result;
    }    
}
