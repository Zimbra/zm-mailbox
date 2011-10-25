package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.mail.CalendarUtils;

/*
 * To run this test:
 * zmsoap -v -z RunUnitTestsRequest/test=com.zimbra.qa.unittest.TestCalAttendeesDiff
 *
 */

// do not extend TestCase once ZimbraSuite supports JUnit 4 annotations.
// we ahve to now because this test ahs to run in the sever.
public class TestCalAttendeesDiff extends TestCase {

    private static final String ZIMBRA_DOMAIN = "zimbra.galgrouptest";
    private static final String ZIMBRA_GROUP = TestUtil.getAddress("zimbra-group", ZIMBRA_DOMAIN);

    private static final String EXTERNAL_DOMAIN = "external.galgrouptest";
    private static final String EXTERNAL_GROUP = TestUtil.getAddress("external-group", EXTERNAL_DOMAIN);

    private static final String REQUESTER = TestUtil.getAddress("requester", ZIMBRA_DOMAIN);
    private static final String USER_L1 = TestUtil.getAddress("user-L1", ZIMBRA_DOMAIN);
    private static final String USER_L2 = TestUtil.getAddress("user-L2", ZIMBRA_DOMAIN);
    private static final String USER_L2_ALIAS = TestUtil.getAddress("user-L2-alias", ZIMBRA_DOMAIN);
    private static final String USER_L3 = TestUtil.getAddress("user-L3", ZIMBRA_DOMAIN);
    private static final String USER_R1 = TestUtil.getAddress("user-R1", ZIMBRA_DOMAIN);
    private static final String USER_R2 = TestUtil.getAddress("user-R2-ext", EXTERNAL_DOMAIN);
    private static final String USER_R3 = TestUtil.getAddress("user-R3", ZIMBRA_DOMAIN);

    //////////////
    // TODO: remove once ZimbraSuite supports JUnit 4 annotations.
    private static boolean initialized = false;
    private static boolean allDone = false;

    public void setUp() throws Exception {
        if (!initialized) {
            init();
            initialized = true;
        }
    }

    public void tearDown() throws Exception {
        if (allDone) {
            cleanup();
            initialized = false;
        }
    }
    //////////////

    @BeforeClass
    public static void init() throws Exception {
        // TestUtil.cliSetup();
        // CliUtil.toolSetup();

        Provisioning prov = Provisioning.getInstance();
        
        // create the zimbra domain
        if (prov.get(Key.DomainBy.name, ZIMBRA_DOMAIN) == null) {
            ZimbraLog.test.info("Creating domain " + ZIMBRA_DOMAIN);
            Domain domain = prov.createDomain(ZIMBRA_DOMAIN, new HashMap<String, Object>());
            
            // configure external GAL
            Map<String, Object> attrs = new HashMap<String, Object>();
            domain.setGalMode(GalMode.both, attrs);
            domain.addGalLdapURL("ldap://localhost:389", attrs);
            domain.setGalLdapBindDn("cn=config", attrs);
            domain.setGalLdapBindPassword("zimbra");
            domain.setGalLdapSearchBase(LdapUtilCommon.domainToDN(EXTERNAL_DOMAIN));
            domain.setGalAutoCompleteLdapFilter("zimbraAccountAutoComplete");
            domain.setGalLdapFilter("zimbraAccounts");
            
            prov.modifyAttrs(domain, attrs);
        }

        // create a domain to simulate entries in external GAL
        if (prov.get(Key.DomainBy.name, EXTERNAL_DOMAIN) == null) {
            ZimbraLog.test.info("Creating domain " + EXTERNAL_DOMAIN);
            prov.createDomain(EXTERNAL_DOMAIN, new HashMap<String, Object>());
        }

        // create the test users
        String[] users = new String[] { REQUESTER, USER_L1, USER_L2, USER_L3, USER_R1, USER_R2, USER_R3 };
        for (String userAddr : users) {
            if (prov.get(AccountBy.name, userAddr) == null) {
                prov.createAccount(userAddr, "test123", null);
            }
        }
        // add L2's alias
        Account acctL2 = prov.get(AccountBy.name, USER_L2);
        acctL2.addAlias(USER_L2_ALIAS);

        // create zimbra group and add members
        DistributionList group = prov.get(Key.DistributionListBy.name, ZIMBRA_GROUP);
        if (group == null) {
            group = prov.createDistributionList(ZIMBRA_GROUP, new HashMap<String, Object>());
            prov.addMembers(group, new String[] { USER_L1, USER_L2_ALIAS, USER_R1 });
        }

        // create group in the external domain and add members
        DistributionList extGroup = prov.get(Key.DistributionListBy.name, EXTERNAL_GROUP);
        if (extGroup == null) {
            extGroup = prov.createDistributionList(EXTERNAL_GROUP, new HashMap<String, Object>());
            prov.addMembers(extGroup, new String[] { USER_R2 });
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        TestSearchGal.disableGalSyncAccount(prov, ZIMBRA_DOMAIN);
        
        // delete the test users
        String[] users = new String[] { REQUESTER, USER_L1, USER_L2, USER_L3, USER_R1, USER_R2, USER_R3 };
        for (String userAddr : users) {
            Account userAcct = prov.get(AccountBy.name, userAddr);
            if (userAcct != null) {
                prov.deleteAccount(userAcct.getId());
            }
        }

        // delete external group and domain
        DistributionList extGroup = prov.get(Key.DistributionListBy.name, EXTERNAL_GROUP);
        if (extGroup != null) {
            prov.deleteDistributionList(extGroup.getId());
        }

        Domain extDomain = prov.get(Key.DomainBy.name, EXTERNAL_DOMAIN);
        if (extDomain != null) {
            ZimbraLog.test.info("Deleting domain " + EXTERNAL_DOMAIN);
            prov.deleteDomain(extDomain.getId());
        }

        // delete zimbra group and domain
        DistributionList group = prov.get(Key.DistributionListBy.name, ZIMBRA_GROUP);
        if (group != null) {
            prov.deleteDistributionList(group.getId());
        }
        
        Domain domain = prov.get(Key.DomainBy.name, ZIMBRA_DOMAIN);
        if (domain != null) {
            ZimbraLog.test.info("Deleting domain " + ZIMBRA_DOMAIN);
            prov.deleteDomain(domain.getId());
        }
    }

    @Test
    public void testAttendeesDiff() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account requester = prov.get(AccountBy.name, REQUESTER);

        List<ZAttendee> oldList = new ArrayList<ZAttendee>();
        oldList.add(new ZAttendee(ZIMBRA_GROUP));
        oldList.add(new ZAttendee(EXTERNAL_GROUP));
        oldList.add(new ZAttendee(USER_L1));  // in ZIMBRA_GROUP
        oldList.add(new ZAttendee(USER_L2));  // in ZIMBRA_GROUP via alias
        oldList.add(new ZAttendee(USER_L3));  // not in any group
        oldList.add(new ZAttendee(USER_R1));  // in ZIMBRA_GROUP
        oldList.add(new ZAttendee(USER_R2));  // in EXTERNAL_GROUP
        oldList.add(new ZAttendee(USER_R3));  // not in any group

        // New attendee list has all individual attendees removed.
        List<ZAttendee> newList = new ArrayList<ZAttendee>();
        newList.add(new ZAttendee(ZIMBRA_GROUP));
        newList.add(new ZAttendee(EXTERNAL_GROUP));

        Set<String> diff;

        // Which attendees were really removed after considering group memberships?
        diff = toAddressSet(CalendarUtils.getRemovedAttendees(oldList, newList, true, requester));
        assertTrue(USER_L3 + " was removed", diff.contains(USER_L3));
        assertTrue(USER_R3 + " was removed", diff.contains(USER_R3));
        assertEquals("number of attendees removed", 2, diff.size());

        // Do a simple diff.
        diff = toAddressSet(CalendarUtils.getRemovedAttendees(oldList, newList, false, requester));
        assertTrue(USER_L1 + " was removed", diff.contains(USER_L1));
        assertTrue(USER_L2 + " was removed", diff.contains(USER_L2));
        assertTrue(USER_L3 + " was removed", diff.contains(USER_L3));
        assertTrue(USER_R1 + " was removed", diff.contains(USER_R1));
        assertTrue(USER_R2 + " was removed", diff.contains(USER_R2));
        assertTrue(USER_R3 + " was removed", diff.contains(USER_R3));
        assertEquals("number of attendees removed", 6, diff.size());
    }

    private Set<String> toAddressSet(List<ZAttendee> attList) {
        Set<String> set = new HashSet<String>();
        for (ZAttendee att : attList) {
            set.add(att.getAddress());
        }
        return set;
    }

    // TODO: remove once ZimbraSuite supports JUnit 4 annotations. 
    @Test
    public void testLast() throws Exception {
        allDone = true;
    }
}
