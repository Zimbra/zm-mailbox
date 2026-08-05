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
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link BackupCSDDedupe}. Drives the real pre/post modify logic against
 * {@link Server} and {@link Config} entries from the in-memory harness and verifies the
 * persisted side effects: preModify rewrites a nodedupe value back to dedupe, and postModify
 * always stamps zimbraBackupCSDReset=TRUE on the entry.
 */
public class BackupCSDDedupeTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Server newServer(String name, String dedupValue) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        // The in-memory harness keys servers by name and derives Server.getId() from the
        // zimbraId attribute; seed it so getId()/deleteServer behave like a provisioned server.
        attrs.put(Provisioning.A_zimbraId, java.util.UUID.randomUUID().toString());
        if (dedupValue != null) {
            attrs.put(Provisioning.A_zimbraBackupDeduplication, dedupValue);
        }
        return prov.createServer(name, attrs);
    }

    @Test
    public void preModifyServerWithNodedupeResetsDeduplicationToDedupe() throws Exception {
        // Arrange -- a server explicitly set to nodedupe
        Server server = newServer("bcsd-nodedupe.example.com",
                ZAttrProvisioning.BackupDeduplication.nodedupe.name());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- preModify must force the value back to dedupe via a real modifyAttrs
        new BackupCSDDedupe().preModify(ctx, Provisioning.A_zimbraBackupDeduplication,
                ZAttrProvisioning.BackupDeduplication.nodedupe.name(), new HashMap<String, Object>(),
                server);

        // Assert -- the persisted value was rewritten to dedupe
        Server reloaded = prov.get(com.zimbra.common.account.Key.ServerBy.name,
                "bcsd-nodedupe.example.com");
        assertEquals("nodedupe must be rewritten to dedupe",
                ZAttrProvisioning.BackupDeduplication.dedupe.name(),
                reloaded.getAttr(Provisioning.A_zimbraBackupDeduplication));
        prov.deleteServer(server.getId());
    }

    @Test
    public void preModifyServerWithDedupeLeavesValueUnchanged() throws Exception {
        // Arrange -- already dedupe; preModify must not touch anything
        Server server = newServer("bcsd-dedupe.example.com",
                ZAttrProvisioning.BackupDeduplication.dedupe.name());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new BackupCSDDedupe().preModify(ctx, Provisioning.A_zimbraBackupDeduplication,
                ZAttrProvisioning.BackupDeduplication.dedupe.name(), new HashMap<String, Object>(),
                server);

        // Assert -- value remains dedupe
        Server reloaded = prov.get(com.zimbra.common.account.Key.ServerBy.name,
                "bcsd-dedupe.example.com");
        assertEquals("dedupe value must be left intact",
                ZAttrProvisioning.BackupDeduplication.dedupe.name(),
                reloaded.getAttr(Provisioning.A_zimbraBackupDeduplication));
        prov.deleteServer(server.getId());
    }

    @Test
    public void preModifyServerWithDefaultDedupeLeavesValueUnset() throws Exception {
        // Arrange -- no value set; getAttr defaults to dedupe, so the nodedupe branch is skipped
        Server server = newServer("bcsd-default.example.com", null);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new BackupCSDDedupe().preModify(ctx, Provisioning.A_zimbraBackupDeduplication,
                null, new HashMap<String, Object>(), server);

        // Assert -- preModify did not write the attribute (still defaulting)
        Server reloaded = prov.get(com.zimbra.common.account.Key.ServerBy.name,
                "bcsd-default.example.com");
        assertEquals("default (unset) dedupe must not trigger a rewrite",
                null, reloaded.getAttr(Provisioning.A_zimbraBackupDeduplication));
        prov.deleteServer(server.getId());
    }

    @Test
    public void preModifyConfigWithNodedupeResetsDeduplicationToDedupe() throws Exception {
        // Arrange -- the global Config branch with nodedupe
        Config config = prov.getConfig();
        Map<String, Object> setNodedupe = new HashMap<String, Object>();
        setNodedupe.put(Provisioning.A_zimbraBackupDeduplication,
                ZAttrProvisioning.BackupDeduplication.nodedupe.name());
        prov.modifyAttrs(config, setNodedupe);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new BackupCSDDedupe().preModify(ctx, Provisioning.A_zimbraBackupDeduplication,
                ZAttrProvisioning.BackupDeduplication.nodedupe.name(),
                new HashMap<String, Object>(), config);

        // Assert -- global config value rewritten to dedupe
        assertEquals("config nodedupe must be rewritten to dedupe",
                ZAttrProvisioning.BackupDeduplication.dedupe.name(),
                prov.getConfig().getAttr(Provisioning.A_zimbraBackupDeduplication));
    }

    @Test
    public void postModifyServerStampsBackupCSDResetTrue() throws Exception {
        // Arrange
        Server server = newServer("bcsd-post-server.example.com", null);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- postModify must persist zimbraBackupCSDReset=TRUE on the server
        new BackupCSDDedupe().postModify(ctx, Provisioning.A_zimbraBackupDeduplication, server);

        // Assert
        Server reloaded = prov.get(com.zimbra.common.account.Key.ServerBy.name,
                "bcsd-post-server.example.com");
        assertEquals("postModify must set the CSD reset flag on the server", "TRUE",
                reloaded.getAttr(Provisioning.A_zimbraBackupCSDReset));
        prov.deleteServer(server.getId());
    }

    @Test
    public void postModifyConfigStampsBackupCSDResetTrue() throws Exception {
        // Arrange -- the global Config branch of postModify
        Config config = prov.getConfig();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new BackupCSDDedupe().postModify(ctx, Provisioning.A_zimbraBackupDeduplication, config);

        // Assert
        assertEquals("postModify must set the CSD reset flag on global config", "TRUE",
                prov.getConfig().getAttr(Provisioning.A_zimbraBackupCSDReset));
    }

    @Test
    public void preModifyThenPostModifyServerFullWorkflowResetsAndStamps() throws Exception {
        // Arrange -- realistic flow: a nodedupe server gets both corrected and flagged
        Server server = newServer("bcsd-flow.example.com",
                ZAttrProvisioning.BackupDeduplication.nodedupe.name());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- pre then post, as the modify pipeline invokes them
        new BackupCSDDedupe().preModify(ctx, Provisioning.A_zimbraBackupDeduplication,
                ZAttrProvisioning.BackupDeduplication.nodedupe.name(),
                new HashMap<String, Object>(), server);
        new BackupCSDDedupe().postModify(ctx, Provisioning.A_zimbraBackupDeduplication, server);

        // Assert -- both side effects are persisted
        Server reloaded = prov.get(com.zimbra.common.account.Key.ServerBy.name,
                "bcsd-flow.example.com");
        assertEquals("dedup corrected to dedupe",
                ZAttrProvisioning.BackupDeduplication.dedupe.name(),
                reloaded.getAttr(Provisioning.A_zimbraBackupDeduplication));
        assertEquals("CSD reset flag stamped", "TRUE",
                reloaded.getAttr(Provisioning.A_zimbraBackupCSDReset));
        prov.deleteServer(server.getId());
    }

    @Test
    public void postModifyUnrecognizedEntryIsNoOp() throws Exception {
        // Arrange -- neither Server nor Config: postModify must silently do nothing
        com.zimbra.cs.account.Account acct = prov.createAccount("bcsd-acct@example.com",
                "test123", new HashMap<String, Object>());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new BackupCSDDedupe().postModify(ctx, Provisioning.A_zimbraBackupDeduplication, acct);

        // Assert -- the account is untouched by the reset flag
        assertTrue("unrecognized entry types are a no-op for postModify",
                acct.getAttr(Provisioning.A_zimbraBackupCSDReset) == null);
        prov.deleteAccount(acct.getId());
    }
}
