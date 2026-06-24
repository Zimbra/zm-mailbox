/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DataSourceQuota#preModify}. Drives the real data-source quota
 * validation against {@link Account} and {@link Cos} entries created through the in-memory
 * harness, covering the account-quota ceiling, the per-source vs total-quota ordering rules,
 * and the unset/no-op paths.
 */
public class DataSourceQuotaTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private CallbackContext ctx() {
        return new CallbackContext(Op.MODIFY);
    }

    @Test
    public void preModifyUnsettingQuotaReturnsWithoutValidation() throws Exception {
        // Arrange -- empty value is treated as UNSETTING and must short-circuit
        Account acct = prov.createAccount("dsq-unset@example.com", "test123",
                new HashMap<String, Object>());
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "");

        // Act -- no exception expected even though no further checks run
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "", toModify, acct);

        // Assert
        assertTrue("unsetting must short-circuit before quota math", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyQuotaExceedsAccountQuotaThrowsFailure() throws Exception {
        // Arrange -- account has an effective quota of 1000; setting source quota above it fails
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailQuota, "1000");
        Account acct = prov.createAccount("dsq-over@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "5000");

        // Act + Assert
        try {
            new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                    "5000", toModify, acct);
            fail("expected FAILURE when data source quota exceeds account quota");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertEquals("message explains the account-quota ceiling", true,
                    e.getMessage().contains("cannot exceed account quota"));
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifySourceQuotaExceedsTotalQuotaThrowsFailure() throws Exception {
        // Arrange -- no account quota cap, but per-source quota exceeds the configured total
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "2000");
        Account acct = prov.createAccount("dsq-srcgttotal@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "3000");

        // Act + Assert
        try {
            new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                    "3000", toModify, acct);
            fail("expected FAILURE when source quota exceeds total quota");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertEquals("message explains the total-quota ceiling", true,
                    e.getMessage().contains("higher than total data source quota"));
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifySourceQuotaWithinTotalPasses() throws Exception {
        // Arrange -- per-source quota below the total and no account cap => valid
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "5000");
        Account acct = prov.createAccount("dsq-ok@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "1000");

        // Act -- no exception
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "1000", toModify, acct);

        // Assert
        assertTrue("source quota under total must be accepted", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyTotalQuotaBelowSourceQuotaThrowsFailure() throws Exception {
        // Arrange -- changing the TOTAL quota lower than the existing per-source quota fails
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "3000");
        Account acct = prov.createAccount("dsq-totallow@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "1000");

        // Act + Assert
        try {
            new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceTotalQuota,
                    "1000", toModify, acct);
            fail("expected FAILURE when total quota is lower than source quota");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertEquals("message explains total cannot be below source", true,
                    e.getMessage().contains("lower than data source quota"));
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifyTotalQuotaAboveSourceQuotaPasses() throws Exception {
        // Arrange -- raising the total quota above the per-source quota is allowed
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "1000");
        Account acct = prov.createAccount("dsq-totalok@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "4000");

        // Act
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceTotalQuota,
                "4000", toModify, acct);

        // Assert
        assertTrue("total quota above source must be accepted", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyCosSourceQuotaExceedsTotalThrowsFailure() throws Exception {
        // Arrange -- the Cos branch of the per-source-vs-total rule
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "2000");
        Cos cos = prov.createCos("dsq-cos-over", cosAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "9000");

        // Act + Assert
        try {
            new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                    "9000", toModify, cos);
            fail("expected FAILURE for COS source quota above total");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertEquals("message explains the total-quota ceiling", true,
                    e.getMessage().contains("higher than total data source quota"));
        } finally {
            prov.deleteCos(cos.getId());
        }
    }

    @Test
    public void preModifyCosTotalQuotaBelowSourceQuotaThrowsFailure() throws Exception {
        // Arrange -- the Cos branch lowering total below an existing per-source quota
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "3000");
        Cos cos = prov.createCos("dsq-cos-totallow", cosAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "1000");

        // Act + Assert
        try {
            new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceTotalQuota,
                    "1000", toModify, cos);
            fail("expected FAILURE for COS total quota below source");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertEquals("message explains total cannot be below source", true,
                    e.getMessage().contains("lower than data source quota"));
        } finally {
            prov.deleteCos(cos.getId());
        }
    }

    @Test
    public void preModifyCosSourceQuotaWithinTotalPasses() throws Exception {
        // Arrange -- valid COS configuration
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "5000");
        Cos cos = prov.createCos("dsq-cos-ok", cosAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "1000");

        // Act
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "1000", toModify, cos);

        // Assert
        assertTrue("COS source quota under total must be accepted", true);
        prov.deleteCos(cos.getId());
    }

    @Test
    public void preModifySourceQuotaEqualsAccountQuotaPasses() throws Exception {
        // Boundary for L44 (newQuota > accountQuota): equal must NOT throw.
        // A >= mutant would reject the equal case.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailQuota, "1000");
        Account acct = prov.createAccount("dsq-eqacct@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "1000");

        // Act -- equal to account quota; no total quota configured so no further check fires.
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "1000", toModify, acct);

        // Assert -- reaching here without ServiceException is the kill condition.
        assertEquals("source quota equal to account quota must be accepted", 1000L,
                accountUtilQuota(acct));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifySourceQuotaEqualsTotalQuotaPasses() throws Exception {
        // Boundary for L49 (newQuota > totalQuota): equal must NOT throw.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "2000");
        Account acct = prov.createAccount("dsq-eqtotal@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "2000");

        // Act -- equal to total; a >= mutant on L49 would wrongly throw.
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "2000", toModify, acct);

        // Assert
        assertEquals("account total quota unchanged", 2000L, acct.getDataSourceTotalQuota());
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyTotalQuotaEqualsSourceQuotaPasses() throws Exception {
        // Boundary for L54 (newQuota < dataSourceQuota): equal must NOT throw.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "3000");
        Account acct = prov.createAccount("dsq-eqsrc@example.com", "test123", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "3000");

        // Act -- new total equals existing source; a <= mutant on L54 would wrongly throw.
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceTotalQuota,
                "3000", toModify, acct);

        // Assert
        assertEquals("existing source quota unchanged", 3000L, acct.getDataSourceQuota());
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyCosSourceQuotaEqualsTotalQuotaPasses() throws Exception {
        // Boundary for L62 (COS newQuota > totalQuota): equal must NOT throw.
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "2000");
        Cos cos = prov.createCos("dsq-cos-eqtotal", cosAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "2000");

        // Act
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota,
                "2000", toModify, cos);

        // Assert
        assertEquals("cos total quota unchanged", 2000L, cos.getDataSourceTotalQuota());
        prov.deleteCos(cos.getId());
    }

    @Test
    public void preModifyCosTotalQuotaEqualsSourceQuotaPasses() throws Exception {
        // Boundary for L67 (COS newQuota < dataSourceQuota): equal must NOT throw.
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(ZAttrProvisioning.A_zimbraDataSourceQuota, "3000");
        Cos cos = prov.createCos("dsq-cos-eqsrc", cosAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ZAttrProvisioning.A_zimbraDataSourceTotalQuota, "3000");

        // Act
        new DataSourceQuota().preModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceTotalQuota,
                "3000", toModify, cos);

        // Assert
        assertEquals("cos source quota unchanged", 3000L, cos.getDataSourceQuota());
        prov.deleteCos(cos.getId());
    }

    private static long accountUtilQuota(Account acct) throws ServiceException {
        return com.zimbra.cs.util.AccountUtil.getEffectiveQuota(acct);
    }

    @Test
    public void postModifyNoOpDoesNotThrow() throws Exception {
        // Arrange
        Account acct = prov.createAccount("dsq-post@example.com", "test123",
                new HashMap<String, Object>());

        // Act -- postModify is an intentional no-op
        new DataSourceQuota().postModify(ctx(), ZAttrProvisioning.A_zimbraDataSourceQuota, acct);

        // Assert
        assertTrue("no-op postModify must not throw", true);
        prov.deleteAccount(acct.getId());
    }
}
