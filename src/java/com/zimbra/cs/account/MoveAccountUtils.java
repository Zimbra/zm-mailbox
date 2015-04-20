/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;


/**
 * @author zimbra
 *
 */
public final class MoveAccountUtils {

    private MoveAccountUtils() {

    }

    /**
     * This method return true when the source server and target server belong to the same cluster.
     * @param sourceServerName
     * @param targetServerName
     * @return
     * @throws ServiceException
     */
    public static boolean isTargetAndSourceServerInSameCluster(String sourceServerName, String targetServerName)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server sourceServer = prov.getServerByName(sourceServerName);
        Server targetServer = prov.getServerByName(targetServerName);

        String sourceClusterId = sourceServer.getAlwaysOnClusterId();
        String targetClusterId = targetServer.getAlwaysOnClusterId();
        if (sourceClusterId != null && Objects.equals(sourceClusterId, targetClusterId)) {
            return true;
        } else {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("Target and source server clusterId mismatch. Target server is: %s, source server is: %s",
                    targetServer.getAlwaysOnClusterId(), sourceServer.getAlwaysOnClusterId());
            }
            return false;
        }
    }


    /**
     * This method sets the zimbraMailHost attribute of the account to the target server
     * @param account
     * @param targetServer
     * @throws ServiceException
     */
    public static void updateHomeServerForAccount(Provisioning prov , Account account, String targetServer) throws ServiceException {
        updateHomeServerForAccount(prov, account, targetServer, Provisioning.FALSE);

    }

    /**
     * This method sets the zimbraMailHost attribute of the account to the target server and also
     * sets the zimbraAccountServerStickyness  attribute
     * @param account
     * @param targetServer
     * @param serverStickyness
     * @throws ServiceException
     */
    public static void updateHomeServerForAccount(Provisioning prov , Account account, String targetServer,String serverStickyness) throws ServiceException {

        String originalStatus = account.getAccountStatus(prov);
        prov.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        prov.reload(account);

        Map<String, String> attrs = new HashMap<String, String>(2);
        String targetHost = prov.getServerByName(targetServer).getServiceHostname();
        attrs.put(Provisioning.A_zimbraMailHost, targetHost);
        attrs.put(Provisioning.A_zimbraMailHostToAccountBinding, String.valueOf(serverStickyness));
        prov.modifyAttrs(account, attrs);

        if (originalStatus == null) {
            originalStatus = Provisioning.ACCOUNT_STATUS_ACTIVE;
        }
        prov.modifyAccountStatus(account, originalStatus);
        prov.reload(account);

        ZimbraLog.account.info("Account %s moved to %s ", account.getName(), targetHost);
    }

    /**
    *
    * @param prov
    * @param account
    * @throws ServiceException
    */
   public static void removeAccountFromMaintenance(Provisioning prov, Account account)
       throws ServiceException {

       String originalStatus = account.getAccountStatus(prov);
       prov.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
       prov.reload(account);
       if (originalStatus == null) {
           originalStatus = Provisioning.ACCOUNT_STATUS_ACTIVE;
       }
       prov.modifyAccountStatus(account, originalStatus);
       prov.reload(account);

   }

    /**
     * @param sourceServer
     * @param targetServer
     * @return
     */
    public static boolean isSharedDbTheSame(String sourceServerName, String targetServerName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server sourceServer = prov.getServerByName(sourceServerName);
        Server targetServer = prov.getServerByName(targetServerName);

        String sourceDbUrl = sourceServer.getMailboxDbConnectionUrl();
        String targetDbUrl = targetServer.getMailboxDbConnectionUrl();
        if (sourceDbUrl != null && Objects.equals(sourceDbUrl ,targetDbUrl)) {
            return true;
        } else {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("Target and source server db connection URL is not same. Target server is: %s, source server is: %s",
                    targetServer.getMailboxDbConnectionUrl(), sourceServer.getMailboxDbConnectionUrl());
            }
            return false;
        }
    }

    /**
     * @param sourceServer
     * @param targetServer
     * @return
     * @throws ServiceException
     */
    public static boolean isSolrUrlTheSame(String sourceServerName, String targetServerName) throws ServiceException {

        Provisioning prov = Provisioning.getInstance();
        Server sourceServer = prov.getServerByName(sourceServerName);
        Server targetServer = prov.getServerByName(targetServerName);

        String sourceSolrUrl = sourceServer.getSolrURLBase();
        String targetSolrUrl = targetServer.getSolrURLBase();
        if (sourceSolrUrl != null && Objects.equals(sourceSolrUrl, targetSolrUrl)) {
            return true;
        } else {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("Target and source server solr URL is not same. Target server is: %s, source server is: %s",
                    targetServer.getSolrURLBase(), sourceServer.getSolrURLBase());
            }
            return false;
        }
    }
}
