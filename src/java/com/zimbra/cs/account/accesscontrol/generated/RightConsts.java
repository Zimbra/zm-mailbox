package com.zimbra.cs.account.accesscontrol.generated;

//
// auto-generated file
// 
// DO NOT MODIFY
//
// To generate, under ZimbraServer, run: 
//     ant generate-rights
//

public class RightConsts {
    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20090216-2350 */


    /*
    ============
    user rights:
    ============
    */


    /**
     * automatically add meeting invites from grantee to the target&#039;s
     * calendar
     */
    public static final String RT_invite = "invite";

    /**
     * login as
     */
    public static final String RT_loginAs = "loginAs";

    /**
     * send as
     */
    public static final String RT_sendAs = "sendAs";

    /**
     * view free/busy
     */
    public static final String RT_viewFreeBusy = "viewFreeBusy";


    /*
    =============
    admin rights:
    =============
    */


    /**
     * access GAL by doing AutoCompleteGal/SearchGal/SyncGal requests
     */
    public static final String RT_accessGAL = "accessGAL";

    /**
     * add account alias
     */
    public static final String RT_addAccountAlias = "addAccountAlias";

    /**
     * add calendar resource alias
     */
    public static final String RT_addCalendarResourceAlias = "addCalendarResourceAlias";

    /**
     * add distribution list alias
     */
    public static final String RT_addDistributionListAlias = "addDistributionListAlias";

    /**
     * add member to distribution list
     */
    public static final String RT_addDistributionListMember = "addDistributionListMember";

    /**
     * login as the user as an admin. This is different from the loginAs user
     * right as follows: - loginAs is effective only when logged in as an
     * user - adminLoginAs is effective only when logged in as an admin That
     * is: If you are an admin and has the adminLoginAs right on user1, the
     * adminLoginAs is effective only when you login as an admin. It is not
     * effective if you login as a regular user. Likewise if another user
     * granted you the loginAs right, the right is effective when you logged
     * in as a regular user, it is not effective when you logged in as an
     * admin.
     */
    public static final String RT_adminLoginAs = "adminLoginAs";

    /**
     * assign the cos (to domains or accounts)
     */
    public static final String RT_assignCos = "assignCos";

    /**
     * check doamin MX record
     */
    public static final String RT_checkDomainMXRecord = "checkDomainMXRecord";

    /**
     * configure admin UI
     */
    public static final String RT_configureAdminUI = "configureAdminUI";

    /**
     * configure cos constraint
     */
    public static final String RT_configureCosConstraint = "configureCosConstraint";

    /**
     * configure attributes for external auth
     */
    public static final String RT_configureExternaAuth = "configureExternaAuth";

    /**
     * configure attributes for external GAL
     */
    public static final String RT_configureExternalGAL = "configureExternalGAL";

    /**
     * configure global config constraint
     */
    public static final String RT_configureGlobalConfigConstraint = "configureGlobalConfigConstraint";

    /**
     * configure password strength
     */
    public static final String RT_configurePasswordStrength = "configurePasswordStrength";

    /**
     * configure quota
     */
    public static final String RT_configureQuota = "configureQuota";

    /**
     * count accounts in a domain
     */
    public static final String RT_countAccount = "countAccount";

    /**
     * create account in the domain
     */
    public static final String RT_createAccount = "createAccount";

    /**
     * create alias in this domain
     */
    public static final String RT_createAlias = "createAlias";

    /**
     * create calendar resource in the domain
     */
    public static final String RT_createCalendarResource = "createCalendarResource";

    /**
     * create cos
     */
    public static final String RT_createCos = "createCos";

    /**
     * create distribution list in the domain
     */
    public static final String RT_createDistributionList = "createDistributionList";

    /**
     * create server
     */
    public static final String RT_createServer = "createServer";

    /**
     * create sub domain
     */
    public static final String RT_createSubDomain = "createSubDomain";

    /**
     * create a top-level domain
     */
    public static final String RT_createTopDomain = "createTopDomain";

    /**
     * create zimlet
     */
    public static final String RT_createZimlet = "createZimlet";

    /**
     * delete account
     */
    public static final String RT_deleteAccount = "deleteAccount";

    /**
     * delete alias in this domain
     */
    public static final String RT_deleteAlias = "deleteAlias";

    /**
     * delete calendar resource
     */
    public static final String RT_deleteCalendarResource = "deleteCalendarResource";

    /**
     * delete cos
     */
    public static final String RT_deleteCos = "deleteCos";

    /**
     * delete distribution list
     */
    public static final String RT_deleteDistributionList = "deleteDistributionList";

    /**
     * delete domain
     */
    public static final String RT_deleteDomain = "deleteDomain";

    /**
     * delete server
     */
    public static final String RT_deleteServer = "deleteServer";

    /**
     * delete zimlet
     */
    public static final String RT_deleteZimlet = "deleteZimlet";

    /**
     * get all account attributes
     */
    public static final String RT_getAccount = "getAccount";

    /**
     * for unittest only, do we have a use case for this?
     */
    public static final String RT_getAccountFeatures = "getAccountFeatures";

    /**
     * get account id, home server, cos id and name, and access URL
     */
    public static final String RT_getAccountInfo = "getAccountInfo";

    /**
     * get all groups the account is a member of
     */
    public static final String RT_getAccountMembership = "getAccountMembership";

    /**
     * get all calendar resource attributes
     */
    public static final String RT_getCalendarResource = "getCalendarResource";

    /**
     * get all cos attributes
     */
    public static final String RT_getCos = "getCos";

    /**
     * get all distribution list attributes
     */
    public static final String RT_getDistributionList = "getDistributionList";

    /**
     * get all domain attributes
     */
    public static final String RT_getDomain = "getDomain";

    /**
     * get domain quota usage
     */
    public static final String RT_getDomainQuotaUsage = "getDomainQuotaUsage";

    /**
     * get all global config attributes
     */
    public static final String RT_getGlobalConfig = "getGlobalConfig";

    /**
     * get mailbox id and size of an account
     */
    public static final String RT_getMailboxInfo = "getMailboxInfo";

    /**
     * get all server attributes
     */
    public static final String RT_getServer = "getServer";

    /**
     * get all zimlet attributes
     */
    public static final String RT_getZimlet = "getZimlet";

    /**
     * see account in GetAllAccounts/SearchDirectoryResponse
     */
    public static final String RT_listAccount = "listAccount";

    /**
     * see calendar resource in
     * GetAllCalendarResources/SearchDirectoryResponse
     */
    public static final String RT_listCalendarResource = "listCalendarResource";

    /**
     * see cos in GetAllCos/SearchDirectoryResponse
     */
    public static final String RT_listCos = "listCos";

    /**
     * see distribution list in GetAllCos/SearchDirectoryResponse
     */
    public static final String RT_listDistributionList = "listDistributionList";

    /**
     * see domain in GetAllCos/SearchDirectoryResponse
     */
    public static final String RT_listDomain = "listDomain";

    /**
     * see server in GetAllCos/SearchDirectoryResponse
     */
    public static final String RT_listServer = "listServer";

    /**
     * see zimlet in GetAllZimlets
     */
    public static final String RT_listZimlet = "listZimlet";

    /**
     * take actions on mail queues
     */
    public static final String RT_manageMailQueue = "manageMailQueue";

    /**
     * modify all account attributes
     */
    public static final String RT_modifyAccount = "modifyAccount";

    /**
     * modify all calendar resource attributes
     */
    public static final String RT_modifyCalendarResource = "modifyCalendarResource";

    /**
     * set all cos attributes
     */
    public static final String RT_modifyCos = "modifyCos";

    /**
     * set all distribution list attributes
     */
    public static final String RT_modifyDistributionList = "modifyDistributionList";

    /**
     * set all domain attributes
     */
    public static final String RT_modifyDomain = "modifyDomain";

    /**
     * modify all global config attributes
     */
    public static final String RT_modifyGlobalConfig = "modifyGlobalConfig";

    /**
     * set all server attributes
     */
    public static final String RT_modifyServer = "modifyServer";

    /**
     * set all zimlet attributes
     */
    public static final String RT_modifyZimlet = "modifyZimlet";

    /**
     * reindex mailbox
     */
    public static final String RT_reindexMailbox = "reindexMailbox";

    /**
     * remove account alias
     */
    public static final String RT_removeAccountAlias = "removeAccountAlias";

    /**
     * remove calendar resource alias
     */
    public static final String RT_removeCalendarResourceAlias = "removeCalendarResourceAlias";

    /**
     * remove distribution list alias
     */
    public static final String RT_removeDistributionListAlias = "removeDistributionListAlias";

    /**
     * remove member from distribution list
     */
    public static final String RT_removeDistributionListMember = "removeDistributionListMember";

    /**
     * rename account
     */
    public static final String RT_renameAccount = "renameAccount";

    /**
     * rename calendar resource
     */
    public static final String RT_renameCalendarResource = "renameCalendarResource";

    /**
     * rename cos
     */
    public static final String RT_renameCos = "renameCos";

    /**
     * rename distribution list
     */
    public static final String RT_renameDistributionList = "renameDistributionList";

    /**
     * rename domain
     */
    public static final String RT_renameDomain = "renameDomain";

    /**
     * rename server
     */
    public static final String RT_renameServer = "renameServer";

    /**
     * set account password
     */
    public static final String RT_setAccountPassword = "setAccountPassword";

    /**
     * set calendar resource password
     */
    public static final String RT_setCalendarResourcePassword = "setCalendarResourcePassword";

    /**
     * view the account&#039;s email
     */
    public static final String RT_viewEmail = "viewEmail";

    /**
     * view mail queues
     */
    public static final String RT_viewMailQueue = "viewMailQueue";

    /**
     * configure password strength
     */
    public static final String RT_viewPasswordStrength = "viewPasswordStrength";

    ///// END-AUTO-GEN-REPLACE
}
