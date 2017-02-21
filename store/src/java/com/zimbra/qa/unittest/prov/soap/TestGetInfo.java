/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.type.DiscoverRightsInfo;
import com.zimbra.soap.account.type.DiscoverRightsTarget;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class TestGetInfo  extends SoapTest {
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    @Test
    public void discoverRights() throws Exception {
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);
        Group group = provUtil.createGroup(genGroupNameLocalPart(), domain, false);

        prov.grantRight(TargetType.domain.getCode(), TargetBy.name, domain.getName(),
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null,
                User.R_createDistList.getName(), null);

        prov.grantRight(TargetType.dl.getCode(), TargetBy.name, group.getName(),
                GranteeType.GT_USER.getCode(), GranteeBy.name, acct.getName(), null,
                User.R_sendToDistList.getName(), null);

        SoapTransport transport = authUser(acct.getName());

        GetInfoRequest req = new GetInfoRequest();
        req.addRight(User.R_createDistList.getName());
        req.addRight(User.R_sendToDistList.getName());
        GetInfoResponse resp = invokeJaxb(transport, req);

        List<DiscoverRightsInfo> rightsInfo = resp.getDiscoveredRights();

        Set<String> result = Sets.newHashSet();

        for (DiscoverRightsInfo rightInfo : rightsInfo) {
            String right = rightInfo.getRight();
            List<DiscoverRightsTarget> targets = rightInfo.getTargets();

            for (DiscoverRightsTarget target : targets) {
                String id = target.getId();
                String name = target.getName();
                String type = target.getType().toString();

                result.add(Verify.makeResultStr(right, id, name, type));
            }
        }

        Verify.verifyEquals(
                Sets.newHashSet(
                        Verify.makeResultStr(User.R_createDistList.getName(), domain.getId(), domain.getName(), TargetType.domain.getCode()),
                        Verify.makeResultStr(User.R_sendToDistList.getName(), group.getId(), group.getName(), TargetType.dl.getCode())),
                result);
    }

}
