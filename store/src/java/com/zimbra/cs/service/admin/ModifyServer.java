/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since Jun 17, 2004
 * @author schemers
 */
public final class ModifyServer extends AdminDocumentHandler {

    private static final String[] TARGET_SERVER_PATH = new String[] { AdminConstants.E_ID };

    @Override
    protected String[] getProxiedServerPath() {
        return TARGET_SERVER_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);
        Map<String, Object> attrs = AdminService.getAttrs(request);

        Server server = prov.get(Key.ServerBy.id, id);
        if (server == null) {
            throw AccountServiceException.NO_SUCH_SERVER(id);
        }
        checkRight(zsc, context, server, attrs);

        startOrStopPostSRSd(server, attrs);

        // pass in true to checkImmutable
        prov.modifyAttrs(server, attrs, true);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyServer","name", server.getName()}, attrs));

        Element response = zsc.createElement(AdminConstants.MODIFY_SERVER_RESPONSE);
        GetServer.encodeServer(response, server);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY,
                Admin.R_modifyServer.getName(), "server"));
    }

    /**
     * Enable/disable postsrs service
     * @param server to check what services are available
     * @param attrs existing map to populate services
     * @return nothing
     */
    public void startOrStopPostSRSd(Server server, Map<String, Object> attrs) throws ServiceException {

        List<String> command = new ArrayList<>();
        List<String> response = new ArrayList<>();
        List<String> attrsUI = new ArrayList<>();
        List<String> attrsLDAP = new ArrayList<>();
        boolean UIWantsToEnablePostsrs = false;
        boolean isPostsrsEnabledInLDAP = false;
        final String POSTSRSD_SECRET = "/opt/zimbra/common/etc/postsrsd.secret";
        final String POSTSRSD_EXE = LC.zimbra_home.value() + "/common/sbin/postsrsd";

        try {
            if (!attrs.isEmpty()) {
                Collections.addAll(attrsUI, (String[]) attrs.get(Provisioning.A_zimbraServiceEnabled));
                ZimbraLog.mailbox.info("attrsUI: " + attrsUI);
                UIWantsToEnablePostsrs = attrsUI.contains("postsrs");
            }

            if (!server.getAttrs().isEmpty()) {
                Collections.addAll(attrsLDAP, server.getServiceEnabled());
                ZimbraLog.mailbox.info("attrsLDAP: " + attrsLDAP);
                isPostsrsEnabledInLDAP = attrsLDAP.contains("postsrs");
            }

            if (UIWantsToEnablePostsrs && !isPostsrsEnabledInLDAP) {
                command = Stream.of(POSTSRSD_EXE, "-s", POSTSRSD_SECRET, "-d", server.getName(), "-D")
                        .collect(Collectors.toList());
                response = executeLinuxCommand(command);
                ZimbraLog.mailbox.info(response);
                ZimbraLog.mailbox.info("postsrsd has been enabled");
            } else if (UIWantsToEnablePostsrs && isPostsrsEnabledInLDAP) {
                ZimbraLog.mailbox.info("postsrsd is already enabled");
            } else if (!UIWantsToEnablePostsrs && isPostsrsEnabledInLDAP) {
                // There is no command to disable SRS so far. The only way is killing the
                // process.
                command = Stream.of("pgrep", "-f", "postsrsd").collect(Collectors.toList());
                response = executeLinuxCommand(command);
                ZimbraLog.mailbox.info("response: " + response);
                if (response.isEmpty()) {
                    ZimbraLog.mailbox.info("postsrsd is already disabled");
                } else {
                    String postSrsdPID = response.get(0);
                    command.clear();
                    response.clear();
                    command = Stream.of("kill", "-9", postSrsdPID).collect(Collectors.toList());
                    response = executeLinuxCommand(command);
                    ZimbraLog.mailbox.info("response: " + response);
                    ZimbraLog.mailbox.info("postsrsd has been disabled");
                }
            } else if (!UIWantsToEnablePostsrs && !isPostsrsEnabledInLDAP) {
                ZimbraLog.mailbox.info("postsrsd is already disabled");
            }
        } catch (IOException e) {
            ZimbraLog.mailbox.warn(e);
        } catch (InterruptedException e) {
            ZimbraLog.mailbox.warn(e);
        }
    }

    /**
     * Execute linux command
     * @param command to be executed
     * @return list of string
     */
    public List<String> executeLinuxCommand(List<String> command) throws IOException, InterruptedException  {

        InputStream is = null;
        ProcessBuilder pb = null;
        Process ps = null;
        List<String> lines = new ArrayList<>();

        ZimbraLog.mailbox.info("command: " + command);
        // Executing the linux command
        pb = new ProcessBuilder(command);
        ps = pb.start();
        int exitValue = ps.waitFor();
        ZimbraLog.mailbox.info("command executed");
        // Getting executed command response as List
        if (exitValue == 0) {
            is = ps.getInputStream();
        } else {
            is = ps.getErrorStream();
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        ps.destroy();

        return lines;
    }
}