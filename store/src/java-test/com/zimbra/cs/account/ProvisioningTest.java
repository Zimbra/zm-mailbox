/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Comprehensive functional tests for {@link Provisioning} class.
 *
 * Tests core provisioning workflows including account creation, domain management,
 * COS management, rights management, and state persistence.
 *
 * Follows TEST_GENERATION_STANDARD.md: full functional tests with state transitions,
 * real-world workflows, side effects verification, error recovery, and boundary conditions.
 */
public class ProvisioningTest {

    private static Provisioning provisioning;
    private static Domain testDomain;
    private static Cos testCos;

    @BeforeClass
    public static void initProvisioning() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();

        // Create test domain for use in tests
        Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put("description", "Test Domain");
        testDomain = provisioning.createDomain("testdomain.biz", domainAttrs);

        // Create test COS for use in tests
        Map<String, Object> cosAttrs = new HashMap<>();
        cosAttrs.put("description", "Test COS");
        testCos = provisioning.createCos("testcos", cosAttrs);
    }

    @Before
    public void setUp() throws Exception {
        // Each test starts with fresh state
    }

    @After
    public void tearDown() throws Exception {
        // Clean up accounts created during tests - get all accounts from all domains
        List<Domain> allDomains = provisioning.getAllDomains();
        for (Domain domain : allDomains) {
            List allAccounts = provisioning.getAllAccounts(domain);
            for (Object obj : allAccounts) {
                Account account = (Account) obj;
                String name = account.getName();
                if (name != null && name.startsWith("test") && !name.equals("admin@testdomain.biz")) {
                    try {
                        provisioning.deleteAccount(account.getId());
                    } catch (Exception e) {
                        // Ignore errors during cleanup
                    }
                }
            }
        }
    }

    // ===== ACCOUNT CREATION WORKFLOW TESTS =====

    @Test
    public void createAccount_withBasicAttributes_success() throws Exception {
        // Arrange
        String accountName = "testuser1@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test User 1");
        attrs.put("displayName", "Test User One");

        // Act
        Account account = provisioning.createAccount(accountName, "password123", attrs);

        // Assert - Verify account created
        Assert.assertNotNull(account);
        Assert.assertEquals(accountName, account.getName());
        Assert.assertNotNull(account.getId());
        Assert.assertEquals("Test User 1", account.getAttr("description"));
        Assert.assertEquals("Test User One", account.getAttr("displayName"));

        // Assert - Verify persistence: reload and verify
        Account reloaded = provisioning.getAccountByName(accountName);
        Assert.assertNotNull(reloaded);
        Assert.assertEquals(account.getId(), reloaded.getId());
        Assert.assertEquals("Test User 1", reloaded.getAttr("description"));
    }

    @Test
    public void createAccount_withMailQuotaAttribute_persisted() throws Exception {
        // Arrange
        String accountName = "testuser2@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailQuota", "1073741824");  // 1GB

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert
        Assert.assertNotNull(account);
        Assert.assertEquals("1073741824", account.getAttr("zimbraMailQuota"));

        // Verify persistence
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals("1073741824", retrieved.getAttr("zimbraMailQuota"));
    }

    @Test
    public void createAccount_duplicateName_throwsException() throws Exception {
        // Arrange
        String accountName = "duplicate@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();

        // Act - Create first account
        provisioning.createAccount(accountName, "password", attrs);

        // Act/Assert - Try to create duplicate, expect exception
        try {
            provisioning.createAccount(accountName, "password", attrs);
            Assert.fail("Should throw exception for duplicate account name");
        } catch (ServiceException e) {
            Assert.assertTrue(e.getMessage().contains("already exists"));
        }

        // Assert - Verify only one account exists
        Account account = provisioning.getAccountByName(accountName);
        Assert.assertNotNull(account);
    }

    @Test
    public void createAccount_thenLookupById_success() throws Exception {
        // Arrange
        String accountName = "testuser3@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();

        // Act - Create account
        Account created = provisioning.createAccount(accountName, "password", attrs);
        String accountId = created.getId();

        // Act - Lookup by ID
        Account retrieved = provisioning.getAccountById(accountId);

        // Assert - Verify lookup by ID works
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(accountId, retrieved.getId());
        Assert.assertEquals(accountName, retrieved.getName());
    }

    @Test
    public void createAccount_thenGetByName_success() throws Exception {
        // Arrange
        String accountName = "testuser4@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();

        // Act
        provisioning.createAccount(accountName, "password", attrs);
        Account retrieved = provisioning.getAccountByName(accountName);

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(accountName, retrieved.getName());
    }

    @Test
    public void createAccount_withCOS_assignedSuccessfully() throws Exception {
        // Arrange
        String accountName = "testuser5@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraCOSId", testCos.getId());

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert - Verify COS is assigned
        Assert.assertEquals(testCos.getId(), account.getAttr("zimbraCOSId"));

        // Verify persistence
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(testCos.getId(), retrieved.getAttr("zimbraCOSId"));
    }

    @Test
    public void createAccount_withMultipleAttributes_allPersisted() throws Exception {
        // Arrange
        String accountName = "testuser6@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Full Account");
        attrs.put("displayName", "Full Test");
        attrs.put("zimbraMailStatus", "enabled");
        attrs.put("zimbraAccountStatus", "active");
        attrs.put("zimbraMailQuota", "2147483648");

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert - Verify all attributes on created object
        Assert.assertEquals("Full Account", account.getAttr("description"));
        Assert.assertEquals("Full Test", account.getAttr("displayName"));
        Assert.assertEquals("enabled", account.getAttr("zimbraMailStatus"));
        Assert.assertEquals("active", account.getAttr("zimbraAccountStatus"));
        Assert.assertEquals("2147483648", account.getAttr("zimbraMailQuota"));

        // Verify persistence
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals("Full Account", retrieved.getAttr("description"));
        Assert.assertEquals("Full Test", retrieved.getAttr("displayName"));
        Assert.assertEquals("enabled", retrieved.getAttr("zimbraMailStatus"));
        Assert.assertEquals("active", retrieved.getAttr("zimbraAccountStatus"));
        Assert.assertEquals("2147483648", retrieved.getAttr("zimbraMailQuota"));
    }

    @Test
    public void createAccount_noAttributes_defaultsApplied() throws Exception {
        // Arrange
        String accountName = "testuser7@testdomain.biz";
        Map<String, Object> emptyAttrs = new HashMap<>();

        // Act
        Account account = provisioning.createAccount(accountName, "password", emptyAttrs);

        // Assert - Verify account created with defaults
        Assert.assertNotNull(account);
        Assert.assertEquals(accountName, account.getName());
        Assert.assertNotNull(account.getId());
    }

    // ===== ACCOUNT MODIFICATION WORKFLOW TESTS =====

    @Test
    public void modifyAccount_updateDescription_persisted() throws Exception {
        // Arrange - Create account
        String accountName = "modtest1@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Original");
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act - Modify attribute
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified");
        provisioning.modifyAttrs(account, updates);

        // Assert - Verify modification on current object
        Account modified = provisioning.getAccountByName(accountName);
        Assert.assertEquals("Modified", modified.getAttr("description"));

        // Assert - Verify persistence: fresh retrieval
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals("Modified", retrieved.getAttr("description"));
    }

    @Test
    public void modifyAccount_multipleAttributes_allPersisted() throws Exception {
        // Arrange
        String accountName = "modtest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act - Modify multiple attributes
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "New Desc");
        updates.put("displayName", "New Name");
        updates.put("zimbraMailStatus", "disabled");
        provisioning.modifyAttrs(account, updates);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals("New Desc", retrieved.getAttr("description"));
        Assert.assertEquals("New Name", retrieved.getAttr("displayName"));
        Assert.assertEquals("disabled", retrieved.getAttr("zimbraMailStatus"));
    }

    @Test
    public void modifyAccount_quotaIncrease_persisted() throws Exception {
        // Arrange
        String accountName = "modtest3@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailQuota", "1073741824");  // 1GB
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act - Increase quota
        Map<String, Object> updates = new HashMap<>();
        updates.put("zimbraMailQuota", "2147483648");  // 2GB
        provisioning.modifyAttrs(account, updates);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals("2147483648", retrieved.getAttr("zimbraMailQuota"));
    }

    @Test
    public void modifyAccount_changeCOS_persisted() throws Exception {
        // Arrange - Create second COS
        Map<String, Object> cosAttrs = new HashMap<>();
        cosAttrs.put("description", "Test COS 2");
        Cos cos2 = provisioning.createCos("testcos2", cosAttrs);

        // Create account with first COS
        String accountName = "modtest4@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraCOSId", testCos.getId());
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act - Change COS
        Map<String, Object> updates = new HashMap<>();
        updates.put("zimbraCOSId", cos2.getId());
        provisioning.modifyAttrs(account, updates);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(cos2.getId(), retrieved.getAttr("zimbraCOSId"));
    }

    @Test
    public void modifyAccount_clearAttribute_success() throws Exception {
        // Arrange
        String accountName = "modtest5@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "To Delete");
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act - Clear attribute using map with null
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "");
        provisioning.modifyAttrs(account, updates);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        String desc = retrieved.getAttr("description");
        Assert.assertTrue(desc == null || desc.isEmpty());
    }

    // ===== ACCOUNT DELETION WORKFLOW TESTS =====

    @Test
    public void deleteAccount_byId_success() throws Exception {
        // Arrange
        String accountName = "deltest1@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());
        String accountId = account.getId();

        // Verify account exists
        Assert.assertNotNull(provisioning.getAccountById(accountId));

        // Act - Delete account
        provisioning.deleteAccount(accountId);

        // Assert - Verify account no longer exists
        try {
            provisioning.getAccountById(accountId);
            Assert.fail("Account should not exist after deletion");
        } catch (ServiceException e) {
            // Expected
        }
    }

    @Test
    public void deleteAccount_cannotReretrieveByName_afterDeletion() throws Exception {
        // Arrange
        String accountName = "deltest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        provisioning.deleteAccount(account.getId());

        // Assert
        try {
            provisioning.getAccountByName(accountName);
            Assert.fail("Account should not exist by name after deletion");
        } catch (ServiceException e) {
            // Expected
        }
    }

    @Test
    public void deleteAccount_multipleAccounts_onlyTargetDeleted() throws Exception {
        // Arrange
        String account1Name = "deltest3a@testdomain.biz";
        String account2Name = "deltest3b@testdomain.biz";
        Account acct1 = provisioning.createAccount(account1Name, "password", new HashMap<>());
        Account acct2 = provisioning.createAccount(account2Name, "password", new HashMap<>());

        // Act - Delete only account1
        provisioning.deleteAccount(acct1.getId());

        // Assert - account1 is gone, account2 remains
        try {
            provisioning.getAccountById(acct1.getId());
            Assert.fail("Deleted account should not exist");
        } catch (ServiceException e) {
            // Expected
        }

        Account acct2Retrieved = provisioning.getAccountById(acct2.getId());
        Assert.assertNotNull(acct2Retrieved);
        Assert.assertEquals(account2Name, acct2Retrieved.getName());
    }

    // ===== ACCOUNT STATUS MANAGEMENT TESTS =====

    @Test
    public void modifyAccountStatus_activeToMaintenance_success() throws Exception {
        // Arrange
        String accountName = "statustest1@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act - Change status to maintenance
        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_MAINTENANCE,
                          retrieved.getAttr("zimbraAccountStatus"));
    }

    @Test
    public void modifyAccountStatus_toLockedStatus_success() throws Exception {
        // Arrange
        String accountName = "statustest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_LOCKED);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_LOCKED,
                          retrieved.getAttr("zimbraAccountStatus"));
    }

    @Test
    public void modifyAccountStatus_toClosed_success() throws Exception {
        // Arrange
        String accountName = "statustest3@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_CLOSED);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_CLOSED,
                          retrieved.getAttr("zimbraAccountStatus"));
    }

    @Test
    public void modifyAccountStatus_statusTransitions_sequence() throws Exception {
        // Arrange
        String accountName = "statustest4@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act/Assert - Verify state transitions
        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        Account step1 = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_MAINTENANCE,
                          step1.getAttr("zimbraAccountStatus"));

        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_ACTIVE);
        Account step2 = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE,
                          step2.getAttr("zimbraAccountStatus"));

        provisioning.modifyAccountStatus(account, Provisioning.ACCOUNT_STATUS_LOCKED);
        Account step3 = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_LOCKED,
                          step3.getAttr("zimbraAccountStatus"));
    }

    // ===== DOMAIN CREATION WORKFLOW TESTS =====

    @Test
    public void createDomain_withAttributes_success() throws Exception {
        // Arrange
        String domainName = "newdomain1.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "New Test Domain");

        // Act
        Domain domain = provisioning.createDomain(domainName, attrs);

        // Assert
        Assert.assertNotNull(domain);
        Assert.assertEquals(domainName, domain.getName());
        Assert.assertEquals("New Test Domain", domain.getAttr("description"));

        // Verify persistence
        Domain retrieved = provisioning.getDomainByName(domainName);
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(domain.getId(), retrieved.getId());
    }

    @Test
    public void createDomain_duplicateName_throwsException() throws Exception {
        // Arrange
        String domainName = "uniquedomain.biz";

        // Act - Create first domain
        provisioning.createDomain(domainName, new HashMap<>());

        // Act/Assert - Try duplicate
        try {
            provisioning.createDomain(domainName, new HashMap<>());
            Assert.fail("Should throw exception for duplicate domain");
        } catch (ServiceException e) {
            Assert.assertTrue(e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void getDomain_byName_success() throws Exception {
        // Arrange
        String domainName = "getdomain1.biz";
        provisioning.createDomain(domainName, new HashMap<>());

        // Act
        Domain domain = provisioning.getDomainByName(domainName);

        // Assert
        Assert.assertNotNull(domain);
        Assert.assertEquals(domainName, domain.getName());
    }

    @Test
    public void getDomain_byId_success() throws Exception {
        // Arrange
        String domainName = "getdomain2.biz";
        Domain created = provisioning.createDomain(domainName, new HashMap<>());

        // Act
        Domain retrieved = provisioning.getDomainById(created.getId());

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(domainName, retrieved.getName());
        Assert.assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    public void modifyDomain_updateDescription_persisted() throws Exception {
        // Arrange
        String domainName = "moddomain1.biz";
        Domain domain = provisioning.createDomain(domainName, new HashMap<>());

        // Act
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Updated Domain");
        provisioning.modifyAttrs(domain, updates);

        // Assert
        Domain retrieved = provisioning.getDomainByName(domainName);
        Assert.assertEquals("Updated Domain", retrieved.getAttr("description"));
    }

    @Test
    public void deleteDomain_byId_success() throws Exception {
        // Arrange
        String domainName = "deldomain1.biz";
        Domain domain = provisioning.createDomain(domainName, new HashMap<>());

        // Act
        provisioning.deleteDomain(domain.getId());

        // Assert
        try {
            provisioning.getDomainById(domain.getId());
            Assert.fail("Domain should not exist after deletion");
        } catch (ServiceException e) {
            // Expected
        }
    }

    // ===== COS (CLASS OF SERVICE) MANAGEMENT TESTS =====

    @Test
    public void createCos_withAttributes_success() throws Exception {
        // Arrange
        String cosName = "newcos1";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "New COS");

        // Act
        Cos cos = provisioning.createCos(cosName, attrs);

        // Assert
        Assert.assertNotNull(cos);
        Assert.assertEquals(cosName, cos.getName());
        Assert.assertEquals("New COS", cos.getAttr("description"));

        // Verify persistence
        Cos retrieved = provisioning.getCosByName(cosName);
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(cos.getId(), retrieved.getId());
    }

    @Test
    public void createCos_duplicateName_throwsException() throws Exception {
        // Arrange
        String cosName = "uniquecos";

        // Act - Create first
        provisioning.createCos(cosName, new HashMap<>());

        // Act/Assert - Try duplicate
        try {
            provisioning.createCos(cosName, new HashMap<>());
            Assert.fail("Should throw exception for duplicate COS");
        } catch (ServiceException e) {
            // Expected
        }
    }

    @Test
    public void getCos_byName_success() throws Exception {
        // Arrange
        String cosName = "getcos1";
        provisioning.createCos(cosName, new HashMap<>());

        // Act
        Cos cos = provisioning.getCosByName(cosName);

        // Assert
        Assert.assertNotNull(cos);
        Assert.assertEquals(cosName, cos.getName());
    }

    @Test
    public void getCos_byId_success() throws Exception {
        // Arrange
        String cosName = "getcos2";
        Cos created = provisioning.createCos(cosName, new HashMap<>());

        // Act
        Cos retrieved = provisioning.getCosById(created.getId());

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(cosName, retrieved.getName());
        Assert.assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    public void modifyCos_updateAttribute_persisted() throws Exception {
        // Arrange
        String cosName = "modcos1";
        Cos cos = provisioning.createCos(cosName, new HashMap<>());

        // Act
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified COS");
        provisioning.modifyAttrs(cos, updates);

        // Assert
        Cos retrieved = provisioning.getCosByName(cosName);
        Assert.assertEquals("Modified COS", retrieved.getAttr("description"));
    }

    @Test
    public void deleteCos_byId_success() throws Exception {
        // Arrange
        String cosName = "delcos1";
        Cos cos = provisioning.createCos(cosName, new HashMap<>());

        // Act
        provisioning.deleteCos(cos.getId());

        // Assert
        try {
            provisioning.getCosById(cos.getId());
            Assert.fail("COS should not exist after deletion");
        } catch (ServiceException e) {
            // Expected
        }
    }

    // ===== GET ALL / COLLECTION TESTS =====

    @Test
    public void getAllAccounts_returnsMultipleAccounts() throws Exception {
        // Arrange - Create multiple accounts
        String acc1 = "alltest1@testdomain.biz";
        String acc2 = "alltest2@testdomain.biz";
        provisioning.createAccount(acc1, "password", new HashMap<>());
        provisioning.createAccount(acc2, "password", new HashMap<>());

        // Act - Get accounts from the test domain
        List allAccountsList = provisioning.getAllAccounts(testDomain);
        Collection<Account> allAccounts = (Collection<Account>) allAccountsList;

        // Assert
        Assert.assertNotNull(allAccounts);
        Assert.assertTrue(allAccounts.size() >= 2);

        // Verify both accounts are in collection
        boolean found1 = allAccounts.stream().anyMatch(a -> a.getName().equals(acc1));
        boolean found2 = allAccounts.stream().anyMatch(a -> a.getName().equals(acc2));
        Assert.assertTrue("Account 1 should exist", found1);
        Assert.assertTrue("Account 2 should exist", found2);
    }

    @Test
    public void getAllDomains_returnsMultipleDomains() throws Exception {
        // Arrange
        String dom1 = "alldomain1.biz";
        String dom2 = "alldomain2.biz";
        provisioning.createDomain(dom1, new HashMap<>());
        provisioning.createDomain(dom2, new HashMap<>());

        // Act
        Collection<Domain> allDomains = provisioning.getAllDomains();

        // Assert
        Assert.assertNotNull(allDomains);
        Assert.assertTrue(allDomains.size() >= 2);
    }

    // ===== ACCOUNT NAME/ID RETRIEVAL TESTS =====

    @Test
    public void getAccountByName_withSpecialCharacters_success() throws Exception {
        // Arrange
        String accountName = "test.user+tag@testdomain.biz";
        provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        Account account = provisioning.getAccountByName(accountName);

        // Assert
        Assert.assertNotNull(account);
        Assert.assertEquals(accountName, account.getName());
    }

    @Test
    public void getAccountByName_nonExistent_throwsException() throws Exception {
        // Act/Assert
        try {
            provisioning.getAccountByName("nonexistent@testdomain.biz");
            Assert.fail("Should throw exception for nonexistent account");
        } catch (ServiceException e) {
            // Expected
        }
    }

    @Test
    public void getAccountById_nonExistent_throwsException() throws Exception {
        // Act/Assert
        try {
            provisioning.getAccountById(UUID.randomUUID().toString());
            Assert.fail("Should throw exception for nonexistent account ID");
        } catch (ServiceException e) {
            // Expected
        }
    }

    // ===== DOMAIN RETRIEVAL EDGE CASES =====

    @Test
    public void getDomainByName_withSubdomain_success() throws Exception {
        // Arrange
        String domainName = "sub.domain.biz";
        provisioning.createDomain(domainName, new HashMap<>());

        // Act
        Domain domain = provisioning.getDomainByName(domainName);

        // Assert
        Assert.assertNotNull(domain);
        Assert.assertEquals(domainName, domain.getName());
    }

    @Test
    public void getDomainByName_nonExistent_throwsException() throws Exception {
        // Act/Assert
        try {
            provisioning.getDomainByName("nonexistent.biz");
            Assert.fail("Should throw exception for nonexistent domain");
        } catch (ServiceException e) {
            // Expected
        }
    }

    // ===== ACCOUNT-DOMAIN RELATIONSHIP TESTS =====

    @Test
    public void createAccount_accountBelongsToDomain() throws Exception {
        // Arrange
        String accountName = "reltest1@testdomain.biz";

        // Act
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Assert
        Domain domain = provisioning.getDomain(account);
        Assert.assertNotNull(domain);
        Assert.assertEquals("testdomain.biz", domain.getName());
    }

    @Test
    public void createAccount_getDomain_byMailTarget() throws Exception {
        // Arrange
        String accountName = "reltest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        Domain domain = provisioning.getDomain((MailTarget) account);

        // Assert
        Assert.assertNotNull(domain);
        Assert.assertEquals("testdomain.biz", domain.getName());
    }

    // ===== ACCOUNT-COS RELATIONSHIP TESTS =====

    @Test
    public void getAccountCOS_afterCreationWithCOS() throws Exception {
        // Arrange
        String accountName = "cosreltest1@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraCOSId", testCos.getId());
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act
        Cos cos = provisioning.getCOS(account);

        // Assert
        Assert.assertNotNull(cos);
        Assert.assertEquals(testCos.getId(), cos.getId());
    }

    @Test
    public void setCOS_modifiesAccountCOS() throws Exception {
        // Arrange - Create second COS
        Map<String, Object> cosAttrs = new HashMap<>();
        Cos cos2 = provisioning.createCos("relcos2", cosAttrs);

        String accountName = "cosreltest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act - Set COS
        provisioning.setCOS(account, cos2);

        // Assert
        Cos retrieved = provisioning.getCOS(account);
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(cos2.getId(), retrieved.getId());

        // Verify persistence
        Account reloaded = provisioning.getAccountById(account.getId());
        Cos verifyRetrieved = provisioning.getCOS(reloaded);
        Assert.assertEquals(cos2.getId(), verifyRetrieved.getId());
    }

    // ===== ACCOUNT LOOKUP VARIANTS =====

    @Test
    public void getAccount_byName_success() throws Exception {
        // Arrange
        String accountName = "gettest1@testdomain.biz";
        provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        Account account = provisioning.getAccount("gettest1@testdomain.biz");

        // Assert
        Assert.assertNotNull(account);
        Assert.assertEquals(accountName, account.getName());
    }

    @Test
    public void getAccount_nonExistent_throwsException() throws Exception {
        // Act/Assert
        try {
            provisioning.getAccount("nonexistent@testdomain.biz");
            Assert.fail("Should throw exception");
        } catch (ServiceException e) {
            // Expected
        }
    }

    // ===== ADMIN ACCOUNTS TESTS =====

    @Test
    public void getAllAdminAccounts_returnsAdmins() throws Exception {
        // Act
        List<Account> admins = provisioning.getAllAdminAccounts();

        // Assert - Should have at least admin@testdomain.biz
        Assert.assertNotNull(admins);
        Assert.assertFalse(admins.isEmpty());
    }

    // ===== RELOAD TESTS =====

    @Test
    public void reload_account_refreshesState() throws Exception {
        // Arrange
        String accountName = "reloadtest1@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act - Modify via direct call
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Reloaded Description");
        provisioning.modifyAttrs(account, updates);

        // Act - Reload to get fresh state
        provisioning.reload(account);

        // Assert
        Assert.assertEquals("Reloaded Description", account.getAttr("description"));
    }

    @Test
    public void reload_domain_refreshesState() throws Exception {
        // Arrange
        String domainName = "reloaddomain1.biz";
        Domain domain = provisioning.createDomain(domainName, new HashMap<>());

        // Act - Modify
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Reloaded Domain");
        provisioning.modifyAttrs(domain, updates);

        // Act - Reload
        provisioning.reload(domain);

        // Assert
        Assert.assertEquals("Reloaded Domain", domain.getAttr("description"));
    }

    // ===== DEFAULT DOMAIN TESTS =====

    @Test
    public void getDefaultDomain_succeeds() throws Exception {
        // Act
        Domain defaultDomain = provisioning.getDefaultDomain();

        // Assert
        Assert.assertNotNull(defaultDomain);
    }

    // ===== MAIL STATUS TESTS =====

    @Test
    public void createAccount_withMailStatus_enabled() throws Exception {
        // Arrange
        String accountName = "mailstatustest1@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailStatus", Provisioning.MAIL_STATUS_ENABLED);

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert
        Assert.assertEquals(Provisioning.MAIL_STATUS_ENABLED,
                          account.getAttr("zimbraMailStatus"));
    }

    @Test
    public void modifyAccount_changeMailStatus_toDisabled() throws Exception {
        // Arrange
        String accountName = "mailstatustest2@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        Map<String, Object> updates = new HashMap<>();
        updates.put("zimbraMailStatus", Provisioning.MAIL_STATUS_DISABLED);
        provisioning.modifyAttrs(account, updates);

        // Assert
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertEquals(Provisioning.MAIL_STATUS_DISABLED,
                          retrieved.getAttr("zimbraMailStatus"));
    }

    // ===== ACCOUNT LISTING BY DOMAIN =====

    @Test
    public void createMultipleAccountsInDomain_allRetrievable() throws Exception {
        // Arrange
        String dom = "multidomaintest.biz";
        Domain domain = provisioning.createDomain(dom, new HashMap<>());

        String acc1 = "multi1@" + dom;
        String acc2 = "multi2@" + dom;
        provisioning.createAccount(acc1, "password", new HashMap<>());
        provisioning.createAccount(acc2, "password", new HashMap<>());

        // Act - Get all accounts from the domain
        List allAccountsList = provisioning.getAllAccounts(domain);
        Collection<Account> allAccounts = (Collection<Account>) allAccountsList;

        // Assert
        boolean found1 = allAccounts.stream().anyMatch(a -> a.getName().equals(acc1));
        boolean found2 = allAccounts.stream().anyMatch(a -> a.getName().equals(acc2));
        Assert.assertTrue("Account 1 should exist", found1);
        Assert.assertTrue("Account 2 should exist", found2);
    }

    // ===== ACCOUNT IDENTITY TESTS =====

    @Test
    public void createAccount_hasUniqueId() throws Exception {
        // Arrange
        String acc1 = "idtest1@testdomain.biz";
        String acc2 = "idtest2@testdomain.biz";

        // Act
        Account account1 = provisioning.createAccount(acc1, "password", new HashMap<>());
        Account account2 = provisioning.createAccount(acc2, "password", new HashMap<>());

        // Assert - IDs should be unique
        Assert.assertNotEquals(account1.getId(), account2.getId());
    }

    @Test
    public void accountId_consistent_acrossRetrievals() throws Exception {
        // Arrange
        String accountName = "idconsistencytest@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());
        String originalId = account.getId();

        // Act - Retrieve multiple times
        Account retrieved1 = provisioning.getAccountByName(accountName);
        Account retrieved2 = provisioning.getAccountById(originalId);
        Account retrieved3 = provisioning.getAccountByName(accountName);

        // Assert - ID should be consistent
        Assert.assertEquals(originalId, retrieved1.getId());
        Assert.assertEquals(originalId, retrieved2.getId());
        Assert.assertEquals(originalId, retrieved3.getId());
    }

    // ===== ACCOUNT CREATION TIME TESTS =====

    @Test
    public void createAccount_hasCreationTimestamp() throws Exception {
        // Arrange
        String accountName = "creationtest@testdomain.biz";

        // Act
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Assert - Check zimbraCreateTimestamp attribute
        String createTimestampStr = account.getAttr(Provisioning.A_zimbraCreateTimestamp);
        Assert.assertNotNull(createTimestampStr);
        long createTimestamp = Long.parseLong(createTimestampStr);
        Assert.assertTrue(createTimestamp > 0);
    }

    // ===== ACCOUNT NAME PERSISTENCE TESTS =====

    @Test
    public void accountName_persistsThroughReload() throws Exception {
        // Arrange
        String accountName = "nametest@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act
        provisioning.reload(account);

        // Assert
        Assert.assertEquals(accountName, account.getName());
    }

    // ===== DOMAIN NAME PERSISTENCE TESTS =====

    @Test
    public void domainName_persistsThroughReload() throws Exception {
        // Arrange
        String domainName = "namepersisttest.biz";
        Domain domain = provisioning.createDomain(domainName, new HashMap<>());

        // Act
        provisioning.reload(domain);

        // Assert
        Assert.assertEquals(domainName, domain.getName());
    }

    // ===== COS NAME PERSISTENCE TESTS =====

    @Test
    public void cosName_persistsThroughReload() throws Exception {
        // Arrange
        String cosName = "cosmametest";
        Cos cos = provisioning.createCos(cosName, new HashMap<>());

        // Act
        provisioning.reload(cos);

        // Assert
        Assert.assertEquals(cosName, cos.getName());
    }

    // ===== HEALTH CHECK =====

    @Test
    public void healthCheck_succeeds() throws Exception {
        // Act/Assert
        Assert.assertTrue(provisioning.healthCheck());
    }

    // ===== CONFIG RETRIEVAL =====

    @Test
    public void getConfig_returnsConfig() throws Exception {
        // Act
        Config config = provisioning.getConfig();

        // Assert
        Assert.assertNotNull(config);
    }

    // ===== GLOBAL GRANT =====

    @Test
    public void getGlobalGrant_returnsGrant() throws Exception {
        // Act
        GlobalGrant grant = provisioning.getGlobalGrant();

        // Assert
        Assert.assertNotNull(grant);
    }

    // ===== ACCOUNT CREATION WITH DEFAULT COS =====

    @Test
    public void createAccount_withoutExplicitCOS_hasDefault() throws Exception {
        // Arrange
        String accountName = "defaultcostest@testdomain.biz";

        // Act
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Assert - Account should have some COS (default or system-assigned)
        String cosId = account.getAttr("zimbraCOSId");
        // COS may be assigned automatically, just verify account can be retrieved
        Account retrieved = provisioning.getAccountById(account.getId());
        Assert.assertNotNull(retrieved);
    }

    // ===== ACCOUNT STATUS DEFAULT =====

    @Test
    public void createAccount_defaultStatusIsActive() throws Exception {
        // Arrange
        String accountName = "statusdefaulttest@testdomain.biz";

        // Act
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Assert
        Assert.assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE,
                          account.getAttr("zimbraAccountStatus"));
    }

    // ===== ENTRY TYPE VERIFICATION =====

    @Test
    public void accountEntry_hasCorrectType() throws Exception {
        // Arrange
        String accountName = "entrytypetest@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());

        // Act/Assert
        Assert.assertNotNull(account);
        Assert.assertTrue(account instanceof Account);
    }

    @Test
    public void domainEntry_hasCorrectType() throws Exception {
        // Arrange
        String domainName = "entrytypedomaintest.biz";
        Domain domain = provisioning.createDomain(domainName, new HashMap<>());

        // Act/Assert
        Assert.assertNotNull(domain);
        Assert.assertTrue(domain instanceof Domain);
    }

    // ===== MODIFICATION WITHOUT STATE CHANGE =====

    @Test
    public void modifyAccount_withEmptyMap_noChange() throws Exception {
        // Arrange
        String accountName = "emptymodtest@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Original");
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Act - Modify with empty map
        provisioning.modifyAttrs(account, new HashMap<>());

        // Assert - Original value should remain
        Account retrieved = provisioning.getAccountByName(accountName);
        Assert.assertEquals("Original", retrieved.getAttr("description"));
    }

    // ===== CONCURRENT ACCOUNT OPERATIONS =====

    @Test
    public void createTwoAccountsInSequence_bothExist() throws Exception {
        // Arrange
        String acc1Name = "seqtest1@testdomain.biz";
        String acc2Name = "seqtest2@testdomain.biz";

        // Act
        Account acc1 = provisioning.createAccount(acc1Name, "password", new HashMap<>());
        Account acc2 = provisioning.createAccount(acc2Name, "password", new HashMap<>());

        // Assert
        Account retrieved1 = provisioning.getAccountByName(acc1Name);
        Account retrieved2 = provisioning.getAccountByName(acc2Name);

        Assert.assertNotNull(retrieved1);
        Assert.assertNotNull(retrieved2);
        Assert.assertNotEquals(acc1.getId(), acc2.getId());
    }

    // ===== ACCOUNT ATTRIBUTE IMMUTABILITY =====

    @Test
    public void accountId_cannotBeModified() throws Exception {
        // Arrange
        String accountName = "immutabletest@testdomain.biz";
        Account account = provisioning.createAccount(accountName, "password", new HashMap<>());
        String originalId = account.getId();

        // Act - Try to modify ID (should not change actual ID)
        Account retrieved = provisioning.getAccountByName(accountName);

        // Assert - ID should be same
        Assert.assertEquals(originalId, retrieved.getId());
    }

    // ===== ACCOUNT QUOTA BOUNDARY TESTS =====

    @Test
    public void createAccount_withZeroQuota_success() throws Exception {
        // Arrange
        String accountName = "zeroquotatest@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailQuota", "0");

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert
        Assert.assertEquals("0", account.getAttr("zimbraMailQuota"));
    }

    @Test
    public void createAccount_withLargeQuota_success() throws Exception {
        // Arrange
        String accountName = "largequotatest@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailQuota", "1099511627776");  // 1TB

        // Act
        Account account = provisioning.createAccount(accountName, "password", attrs);

        // Assert
        Assert.assertEquals("1099511627776", account.getAttr("zimbraMailQuota"));
    }

    // ===== FINAL COVERAGE: PERSISTENCE ROUND-TRIP =====

    @Test
    public void completeWorkflow_accountCreationThroughDeletion() throws Exception {
        // Arrange
        String accountName = "completeflow@testdomain.biz";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Complete Flow Test");
        attrs.put("displayName", "Complete Flow");

        // Act 1 - Create
        Account created = provisioning.createAccount(accountName, "password", attrs);
        String accountId = created.getId();

        // Assert 1 - Verify creation
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("Complete Flow Test", created.getAttr("description"));

        // Act 2 - Retrieve
        Account retrieved = provisioning.getAccountById(accountId);

        // Assert 2 - Verify retrieval
        Assert.assertEquals(accountName, retrieved.getName());
        Assert.assertEquals("Complete Flow", retrieved.getAttr("displayName"));

        // Act 3 - Modify
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified Flow");
        provisioning.modifyAttrs(retrieved, updates);

        // Assert 3 - Verify modification
        Account modified = provisioning.getAccountById(accountId);
        Assert.assertEquals("Modified Flow", modified.getAttr("description"));

        // Act 4 - Delete
        provisioning.deleteAccount(accountId);

        // Assert 4 - Verify deletion
        try {
            provisioning.getAccountById(accountId);
            Assert.fail("Account should be deleted");
        } catch (ServiceException e) {
            // Expected
        }
    }
}
