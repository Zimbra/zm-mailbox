/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.mail.Transport;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AccessControlUtil;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public class ComputeAggregateQuotaUsage extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        final ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (!AccessControlUtil.isGlobalAdmin(getAuthenticatedAccount(zsc))) {
            throw ServiceException.PERM_DENIED("only global admin is allowed");
        }

        Element response = zsc.createElement(AdminConstants.COMPUTE_AGGR_QUOTA_USAGE_RESPONSE);
        Map<String, Long> domainAggrQuotaUsed = new HashMap<String, Long>();

        Provisioning prov = Provisioning.getInstance();
        final List<Domain> domains = prov.getAllDomains();
        if (domains.size() == 1) {
            // Short-circuit LDAP lookups, etc. Just calculate sum of size_checkpoint of all mailboxes.
            DbConnection conn = null;
            try {
                Domain domain = domains.get(0);
                conn = DbPool.getConnection();
                Long aggregateQuotaUsage = DbMailbox.getAllMailboxSizes(conn);

                final Element domainElt = response.addNonUniqueElement(AdminConstants.E_DOMAIN);
                domainElt.addAttribute(AdminConstants.A_NAME, domain.getName());
                domainElt.addAttribute(AdminConstants.A_ID, domain.getId());
                domainElt.addAttribute(AdminConstants.A_QUOTA_USED, aggregateQuotaUsage);
            } finally {
                DbPool.quietClose(conn);
            }
        } else {
            List<Server> servers = prov.getAllMailClientServers();
            // make number of threads in pool configurable?
            ExecutorService executor = Executors.newFixedThreadPool(LC.compute_aggregate_quota_threads.intValue());
            List<Future<Map<String, Long>>> futures = new LinkedList<Future<Map<String, Long>>>();
            for (final Server server : servers) {
                futures.add(executor.submit(new Callable<Map<String, Long>>() {

                    @Override
                    public Map<String, Long> call() throws Exception {
                        ZimbraLog.misc.debug("Invoking %s on server %s",
                                AdminConstants.E_GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST, server.getName());

                        Element req = new Element.XMLElement(AdminConstants.GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST);
                        String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
                        SoapHttpTransport mTransport = new SoapHttpTransport(adminUrl);
                        mTransport.setAuthToken(zsc.getRawAuthToken());
                        Element resp;
                        try {
                            resp = mTransport.invoke(req);
                        } catch (Exception e) {
                            throw new Exception(
                                    "Error in invoking " + AdminConstants.E_GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST
                                            + " on server " + server.getName(),
                                    e);
                        }
                        List<Element> domainElts = resp.getPathElementList(new String[] { AdminConstants.E_DOMAIN });
                        Map<String, Long> retMap = new HashMap<String, Long>();
                        for (Element domainElt : domainElts) {
                            retMap.put(domainElt.getAttribute(AdminConstants.A_ID),
                                    domainElt.getAttributeLong(AdminConstants.A_QUOTA_USED));
                        }
                        return retMap;
                    }
                }));
            }
            shutdownAndAwaitTermination(executor);

            // Aggregate all results
            for (Future<Map<String, Long>> future : futures) {
                Map<String, Long> result;
                try {
                    result = future.get();
                } catch (Exception e) {
                    throw ServiceException.FAILURE("Error in getting task execution result", e);
                }
                for (String domainId : result.keySet()) {
                    Long delta = result.get(domainId);
                    Long aggr = domainAggrQuotaUsed.get(domainId);
                    domainAggrQuotaUsed.put(domainId, aggr == null ? delta : aggr + delta);
                }
            }

            ExecutorService sendWarnMsgExecutor = Executors.newSingleThreadExecutor();
            for (String domainId : domainAggrQuotaUsed.keySet()) {
                Domain domain = prov.getDomainById(domainId);
                Long used = domainAggrQuotaUsed.get(domainId);
                domain.setAggregateQuotaLastUsage(used);
                Long max = domain.getDomainAggregateQuota();
                if (max != 0 && used * 100 / max > domain.getDomainAggregateQuotaWarnPercent()) {
                    sendWarnMsg(domain, sendWarnMsgExecutor);
                }
                Element domainElt = response.addNonUniqueElement(AdminConstants.E_DOMAIN);
                domainElt.addAttribute(AdminConstants.A_NAME, domain.getName());
                domainElt.addAttribute(AdminConstants.A_ID, domainId);
                domainElt.addAttribute(AdminConstants.A_QUOTA_USED, used);
            }
            sendWarnMsgExecutor.shutdown();
        }

        return response;
    }

    private void sendWarnMsg(final Domain domain, ExecutorService executor) {
        final String[] recipients = domain.getDomainAggregateQuotaWarnEmailRecipient();
        if (ArrayUtil.isEmpty(recipients)) {
            return;
        }
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession(domain));

                    // should From be configurable?
                    out.setFrom(new JavaMailInternetAddress("Postmaster <postmaster@" + domain.getName() + ">"));

                    for (String recipient : recipients) {
                        out.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(recipient));
                    }

                    out.setSentDate(new Date());

                    // using default locale since not sure which locale to pick
                    Locale locale = Locale.getDefault();
                    out.setSubject(L10nUtil.getMessage(L10nUtil.MsgKey.domainAggrQuotaWarnMsgSubject, locale));

                    out.setText(L10nUtil.getMessage(L10nUtil.MsgKey.domainAggrQuotaWarnMsgBody, locale,
                            domain.getName(), domain.getAggregateQuotaLastUsage() / 1024.0 / 1024.0,
                            domain.getDomainAggregateQuotaWarnPercent(),
                            domain.getDomainAggregateQuota() / 1024.0 / 1024.0));

                    Transport.send(out);
                } catch (Exception e) {
                    ZimbraLog.misc.warn("Error in sending aggregate quota warning msg for domain %s", domain.getName(),
                            e);
                }
            }
        });
    }

    private static void shutdownAndAwaitTermination(ExecutorService executor) throws ServiceException {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait for existing tasks to terminate
            // make wait timeout configurable?
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                throw ServiceException.FAILURE(
                        "Time out waiting for " + AdminConstants.E_GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST + " result",
                        null);
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
