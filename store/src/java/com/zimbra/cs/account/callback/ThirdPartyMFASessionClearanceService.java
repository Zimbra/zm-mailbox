/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Mailbox-side helper that clears MFA sessions across all cluster nodes.
 * It calls the existing ClearMFASessionForDomain SOAP handler directly.
 *
 * <p>Uses a cached thread pool executor for managing background cleanup tasks.
 * This follows the standard Zimbra pattern for async operations.
 *
 * <p>Supports timestamp-based filtering to prevent deletion of MFA sessions
 * created after the auth mechanism change was initiated. This ensures that
 * sessions created during the cleanup operation are not accidentally deleted.
 */
public final class ThirdPartyMFASessionClearanceService {

    private static final ExecutorService EXECUTOR =
            newCachedThreadPool(newDaemonThreadFactory("ThirdPartyMFASessionClearance"));

    private ThirdPartyMFASessionClearanceService() {
    }

    /**
     * Clears MFA sessions for a domain asynchronously across all cluster nodes.
     * Sessions will only be deleted if they were created before the given timestamp.
     *
     * @param domainName the name of the domain for which to clear sessions
     * @param changeTimestamp the timestamp (in milliseconds) when auth mech change was initiated;
     *                         only sessions created before this time will be deleted.
     *                         If null or empty, current time is used as fallback.
     */
    public static void clearSessionsForDomain(String domainName, String changeTimestamp) {
        if (domainName == null || domainName.isEmpty()) {
            return;
        }

        EXECUTOR.submit(() -> clearSessionsForDomainAsync(domainName, changeTimestamp));
    }

    /**
     * Clears MFA sessions for a domain across all cluster nodes in the current thread.
     * This is intended for callers that need to wait until the cluster fan-out finishes,
     * such as the zmprov CLI entry point.
     *
     * @param domainName the name of the domain for which to clear sessions
     * @param changeTimestamp the timestamp (in milliseconds) when auth mech change was initiated;
     *                         only sessions created before this time will be deleted.
     *                         If null or empty, current time is used as fallback.
     */
    public static void clearSessionsForDomainSync(String domainName, String changeTimestamp) {
        clearSessionsForDomainAsync(domainName, changeTimestamp);
    }

    /**
     * Clears MFA sessions for a domain asynchronously across all cluster nodes.
     * This method uses current time as the cutoff timestamp (fallback for direct SOAP calls).
     *
     * @param domainName the name of the domain for which to clear sessions
     * @param changeTimestamp the timestamp (in milliseconds) when auth mech change was initiated;
     *                         only sessions created before this time will be deleted.
     *                         If null or empty, current time is used as fallback.
     */
    private static void clearSessionsForDomainAsync(String domainName, String changeTimestamp) {
        try {
            Provisioning prov = Provisioning.getInstance();
            List<Server> servers = getMailboxServers(prov);
            if (servers == null || servers.isEmpty()) {
                ZimbraLog.account.warn("No mailbox-capable servers found while clearing MFA sessions " +
                        "for domain %s", domainName);
                return;
            }

            ZimbraLog.account.debug("Clearing MFA sessions for domain %s across %d mailbox servers " +
                            "(cutoff timestamp: %s)", domainName, servers.size(), changeTimestamp);
            for (Server server : servers) {
                try {
                    clearOnServer(server, domainName, changeTimestamp);
                } catch (Exception e) {
                    ZimbraLog.account.warn("Failed to clear MFA sessions for domain %s on server %s", domainName,
                            server == null ? "unknown" : server.getName(), e);
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to clear MFA sessions for domain %s", domainName, e);
        }
    }

    private static List<Server> getMailboxServers(Provisioning prov) {
        if (prov == null) {
            return new ArrayList<>();
        }

        List<Server> mailboxServers = new ArrayList<>();
        try {
            List<Server> servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
            if (servers == null || servers.isEmpty()) {
                return mailboxServers;
            }

            for (Server server : servers) {
                if (server == null) {
                    continue;
                }
                try {
                    if (server.getMultiAttrSet(Provisioning.A_zimbraServiceEnabled)
                            .contains(Provisioning.SERVICE_MAILBOX)) {
                        mailboxServers.add(server);
                    }
                } catch (Exception e) {
                    ZimbraLog.account.warn("Skipping server %s during MFA session clearance because mailbox " +
                                    "service metadata is unavailable",
                            server.getName(), e);
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to determine mailbox servers for MFA session clearance", e);
        }
        return mailboxServers;
    }

    /**
     * Clears MFA sessions for a domain on a specific server via SOAP.
     *
     * <p>This method constructs a SOAP provisioning client to target a specific cluster node
     * and invokes the ClearMFASessionForDomain handler on that server. The SOAP connection
     * uses local configuration authentication to ensure proper authorization.
     *
     * <p>Sessions will only be deleted if they were created before the given timestamp,
     * allowing for fine-grained control over which sessions are cleared during auth
     * mechanism transitions.
     *
     * @param server the target server on which to clear MFA sessions. Must not be null.
     * @param domainName the name of the domain for which to clear sessions. Must not be null.
     * @param changeTimestamp the timestamp (in milliseconds) when auth mech change was initiated;
     *                         only sessions created before this time will be deleted.
     *                         If null or empty, current time is used as fallback.
     * @throws ServiceException if the SOAP call fails, the server cannot be reached, or any other
     *                          error occurs during session clearance. Callers should handle this
     *                          exception to log failures without stopping the multi-server operation.
     */
    private static void clearOnServer(Server server, String domainName, String changeTimestamp)
            throws ServiceException {
        if (server == null) {
            return;
        }

        String hostname = server.getServiceHostname();
        int adminPort = server.getAdminPort();
        if (hostname == null || hostname.trim().isEmpty()) {
            ZimbraLog.account.warn("Skipping MFA session clearance on server %s because service hostname is missing",
                    server.getName());
            return;
        }
        if (adminPort <= 0) {
            ZimbraLog.account.warn("Skipping MFA session clearance on server %s because admin port is invalid: %s",
                    server.getName(), adminPort);
            return;
        }

        String uri = LC.zimbra_admin_service_scheme.value() + hostname + ":" + adminPort
                + AdminConstants.ADMIN_SERVICE_URI;
        SoapProvisioning.Options options = new SoapProvisioning.Options();
        options.setUri(uri);
        options.setLocalConfigAuth(true);
        SoapProvisioning soapProv = new SoapProvisioning(options);
        soapProv.clearMFASessionForDomain(domainName, changeTimestamp);
        ZimbraLog.account.debug("MFA session invalidation completed for " +
                "domain %s on server %s", domainName, server.getName());
    }
}
