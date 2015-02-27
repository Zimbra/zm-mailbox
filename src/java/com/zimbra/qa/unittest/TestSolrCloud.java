/**
 *
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.IndexStore.Factory;
import com.zimbra.cs.index.solr.SolrCloudIndex;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @author Greg Solovyev
 *
 */
public class TestSolrCloud {

	protected static final String BASE_DOMAIN_NAME = TestLdap.baseDomainName(TestSolrCloud.class);
	protected static final String USER_NAME = "TestSolrCloud-user1";

	@Before
    public void setUp() throws Exception {
    	Assume.assumeTrue("com.zimbra.cs.index.solr.SolrCloudIndex$Factory".equals(LC.zimbra_class_index_store_factory.value()));
    	cleanUp();
        //create test domain
        TestUtil.createDomain(BASE_DOMAIN_NAME);
    }

    @After
    public void tearDown() throws Exception {
    	cleanUp();
    }

    private void cleanUp() throws Exception {
    	TestUtil.deleteAccount(genAccountName());
    	TestUtil.deleteDomain(BASE_DOMAIN_NAME);
    }

	@Test
	public void testCreateIndex() throws Exception {
		//check that index store factory is set to com.zimbra.cs.index.solr.SolrCloudIndex$Factory
		Factory indexStoreFactory = IndexStore.getFactory();
		if(!(indexStoreFactory instanceof SolrCloudIndex.Factory)) {
			fail("Server is not configured to use com.zimbra.cs.index.solr.SolrCloudIndex$Factory");
		}
		//check that zimbraSolrURLBase points to ZK, not to Solr
		String solrServiceURL = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraSolrURLBase, true);
		assertFalse("zimbraSolrURLBase should contain ZooKeeper URL(s) in form host:port,host:port", solrServiceURL.contains("http"));

		//create an account
		String acctName = genAccountName();
		Account acct = TestUtil.createAccount(acctName);
		assertTrue("failed to create an account", TestUtil.accountExists(acctName));
		TestUtil.addMessage(TestUtil.getMailbox(acctName), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
		TestUtil.search(TestUtil.getMailbox(acctName), "chorus", MailItem.Type.MESSAGE);
		IndexStore indexStore = indexStoreFactory.getIndexStore(acct.getId());
		assertTrue("failed to create an index", indexStore.indexExists());

		//check number of shards and replicas
		SolrCloudIndex cloudIndex = (SolrCloudIndex)indexStore;
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("action", CollectionParams.CollectionAction.CLUSTERSTATUS.toString());
		params.set("collection", acct.getId());
		SolrRequest request = new QueryRequest(params);
		request.setPath("/admin/collections");

		NamedList<Object> rsp = cloudIndex.getSolrServer().request(request);
		NamedList<Object> cluster = (NamedList<Object>) rsp.get("cluster");
		assertNotNull("Cluster state should not be null", cluster);
		NamedList<Object> collections = (NamedList<Object>) cluster.get("collections");
		assertNotNull("Collections should not be null in cluster state", collections);
		assertNotNull(collections.get(acct.getId()));
		assertEquals("cannot have more than one collection with the same name", 1, collections.size());
		Map<String, Object> collection = (Map<String, Object>) collections.get(acct.getId());
		Integer replicationFactor = Integer.parseInt(collection.get("replicationFactor").toString());
		assertNotNull("replicationFactor cannot be null", replicationFactor);
		assertEquals("replicationFactor should be 2", 2, (int)replicationFactor);
		Map<String, Object> shardStatus = (Map<String, Object>) collection.get("shards");
		assertEquals("should have only one shard", 1, shardStatus.size());
		Map<String, Object> selectedShardStatus = (Map<String, Object>) shardStatus.get("shard1");
		assertNotNull("shard1 status should not be null", selectedShardStatus);
		Map<String, Object> replicas = (Map<String, Object>) selectedShardStatus.get("replicas");
		assertNotNull("replicas should not be null", replicas);
		Map<String,Object> replica1 = (Map<String,Object>)replicas.get("core_node1");
		assertNotNull("replica1 should not be null", replica1);
		Map<String,Object> replica2 = (Map<String,Object>)replicas.get("core_node2");
		assertNotNull("replica1 should not be null", replica2);
		String node1Name = replica1.get("node_name").toString();
		String node2Name = replica2.get("node_name").toString();
		assertFalse("should be provisioned on different nodes", node1Name.equalsIgnoreCase(node2Name));
	}
	
    private String genAccountName() {
        return USER_NAME + "@" + BASE_DOMAIN_NAME;
    }

}
