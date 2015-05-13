/**
 *
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.ClusterStatus;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.util.ZimbraHttpClientManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.IndexStore.Factory;
import com.zimbra.cs.index.solr.SolrCloudIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * @author Greg Solovyev
 *
 */
public class TestSolrCloud {

	protected static final String BASE_DOMAIN_NAME = TestLdap.baseDomainName(TestSolrCloud.class);
	protected static final String USER_NAME = "TestSolrCloud-user1";
	private int originalMaxShardsPerNode = 1;
	private int originalReplicationFactor = 2;
	@Before
    public void setUp() throws Exception {
    	Assume.assumeTrue("com.zimbra.cs.index.solr.SolrCloudIndex$Factory".equals(IndexStore.getFactory().getClass().getName()));
    	originalMaxShardsPerNode = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraSolrMaxShardsPerNode, 1);
    	originalReplicationFactor = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraSolrReplicationFactor, 2);
    	cleanUp();
        //create test domain
        TestUtil.createDomain(BASE_DOMAIN_NAME);
    }

    @After
    public void tearDown() throws Exception {
    	cleanUp();
    	Provisioning.getInstance().getLocalServer().setSolrReplicationFactor(originalReplicationFactor);
    	Provisioning.getInstance().getLocalServer().setSolrMaxShardsPerNode(originalMaxShardsPerNode);
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
		//check that zimbraIndexURL points to ZK, not to Solr
		String solrServiceURL = Provisioning.getInstance().getLocalServer().getIndexURL().substring(10);
		assertFalse("zimbraIndexURL should contain ZooKeeper URL(s) in form host:port,host:port", solrServiceURL.contains("http"));

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
	
	@Test
	public void testNumShardsPerNode1_1() throws Exception {
	    int replFactor = 1;
	    int maxShardsPerNode = 1;
	    Provisioning.getInstance().getLocalServer().setSolrReplicationFactor(replFactor);
	    Provisioning.getInstance().getLocalServer().setSolrMaxShardsPerNode(maxShardsPerNode);
	    CloudSolrClient cloudSolrServer = cloudSolrServer = new CloudSolrClient(
	            Provisioning.getInstance().getLocalServer().getIndexURL().substring(10), 
                new LBHttpSolrClient(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
	    String acctName = genAccountName();
        Account acct = TestUtil.createAccount(acctName);
        
        assertTrue("failed to create an account", TestUtil.accountExists(acctName));
        TestUtil.addMessage(TestUtil.getMailbox(acctName), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        TestUtil.search(TestUtil.getMailbox(acctName), "chorus", MailItem.Type.MESSAGE);
        
        //check clusterstatus
        ModifiableSolrParams params = new ModifiableSolrParams();
        ClusterStatus req =  new CollectionAdminRequest.ClusterStatus();
        req.setCollectionName(acct.getId());
        try {
            CollectionAdminResponse resp = req.process(cloudSolrServer);
            assertNotNull("clusterstatus response is null", resp);
            NamedList<Object> response = resp.getResponse();
            Object clusterObj = response.get("cluster");
            assertNotNull("could not find 'cluster' element in clusterstatus response", clusterObj);
            assertTrue("'cluster' element should be a list", clusterObj instanceof NamedList);
            NamedList clusterList = (NamedList)clusterObj;
            Object collectionsObj = clusterList.get("collections");
            assertNotNull("could not find 'collections' element in 'cluster' list", collectionsObj);
            assertTrue("'collections' element should be a list", collectionsObj instanceof NamedList);
            NamedList collectionsList = (NamedList)collectionsObj;
            Object collectionObj = collectionsList.get(acct.getId());
            assertNotNull("could not find a list element for account " + acct.getId(), collectionObj);
            assertTrue(acct.getId() + " element should be a Map ", collectionObj instanceof HashMap);
            HashMap collectionProps = (HashMap)collectionObj;
            
            Object replicationFactorObj = collectionProps.get("replicationFactor");
            assertNotNull("could not find 'replicationFactor' element in " + acct.getId(), replicationFactorObj);
            
            Object maxShardsPerNodeObj = collectionProps.get("maxShardsPerNode");
            assertNotNull("could not find 'maxShardsPerNode' element in " + acct.getId(), maxShardsPerNodeObj);
            
            assertEquals("replicationFactor should be 2", Integer.parseInt(replicationFactorObj.toString()),replFactor);
            assertEquals("maxShardsPerNode should be 1", Integer.parseInt(maxShardsPerNodeObj.toString()),maxShardsPerNode);
            
            
        } catch (SolrException | IOException | SolrServerException e) {
            fail(e.getMessage());
        }
	}
	
	 /**
     * Test creating two copies of a collection with max 1 copy per node 
     * @throws Exception
     */
	@Test
    public void test2_1Replication() throws Exception {
        int replFactor = 2;
        int maxShardsPerNode = 1;
        Provisioning.getInstance().getLocalServer().setSolrReplicationFactor(replFactor);
        Provisioning.getInstance().getLocalServer().setSolrMaxShardsPerNode(maxShardsPerNode);
        CloudSolrClient cloudSolrServer = cloudSolrServer = new CloudSolrClient(
                Provisioning.getInstance().getLocalServer().getIndexURL().substring(10), 
                new LBHttpSolrClient(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
        String acctName = genAccountName();
        Account acct = TestUtil.createAccount(acctName);
        
        assertTrue("failed to create an account", TestUtil.accountExists(acctName));
        TestUtil.addMessage(TestUtil.getMailbox(acctName), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        TestUtil.search(TestUtil.getMailbox(acctName), "chorus", MailItem.Type.MESSAGE);
        
        //check clusterstatus
        ModifiableSolrParams params = new ModifiableSolrParams();
        ClusterStatus req =  new CollectionAdminRequest.ClusterStatus();
        req.setCollectionName(acct.getId());
        try {
            CollectionAdminResponse resp = req.process(cloudSolrServer);
            assertNotNull("clusterstatus response is null", resp);
            NamedList<Object> response = resp.getResponse();
            Object clusterObj = response.get("cluster");
            assertNotNull("could not find 'cluster' element in clusterstatus response", clusterObj);
            assertTrue("'cluster' element should be a list", clusterObj instanceof NamedList);
            NamedList clusterList = (NamedList)clusterObj;
            Object collectionsObj = clusterList.get("collections");
            assertNotNull("could not find 'collections' element in 'cluster' list", collectionsObj);
            assertTrue("'collections' element should be a list", collectionsObj instanceof NamedList);
            NamedList collectionsList = (NamedList)collectionsObj;
            Object collectionObj = collectionsList.get(acct.getId());
            assertNotNull("could not find a list element for account " + acct.getId(), collectionObj);
            assertTrue(acct.getId() + " element should be a Map ", collectionObj instanceof HashMap);
            HashMap collectionProps = (HashMap)collectionObj;
            
            Object replicationFactorObj = collectionProps.get("replicationFactor");
            assertNotNull("could not find 'replicationFactor' element in " + acct.getId(), replicationFactorObj);
            
            Object maxShardsPerNodeObj = collectionProps.get("maxShardsPerNode");
            assertNotNull("could not find 'maxShardsPerNode' element in " + acct.getId(), maxShardsPerNodeObj);
            
            assertEquals("replicationFactor should be 2", Integer.parseInt(replicationFactorObj.toString()),replFactor);
            assertEquals("maxShardsPerNode should be 1", Integer.parseInt(maxShardsPerNodeObj.toString()),maxShardsPerNode);
            
            
        } catch (SolrException | IOException | SolrServerException e) {
            fail(e.getMessage());
        }
    }
	
	/**
	 * Test creating three copies of a collection with max 2 copies per node 
	 * @throws Exception
	 */
	@Test
    public void test3_2Replication() throws Exception {
        int replFactor = 3; 
        int maxShardsPerNode = 2;
        Provisioning.getInstance().getLocalServer().setSolrReplicationFactor(replFactor);
        Provisioning.getInstance().getLocalServer().setSolrMaxShardsPerNode(maxShardsPerNode);
        CloudSolrClient cloudSolrServer = cloudSolrServer = new CloudSolrClient(
                Provisioning.getInstance().getLocalServer().getIndexURL().substring(10), 
                new LBHttpSolrClient(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
        String acctName = genAccountName();
        Account acct = TestUtil.createAccount(acctName);
        
        assertTrue("failed to create an account", TestUtil.accountExists(acctName));
        TestUtil.addMessage(TestUtil.getMailbox(acctName), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
        TestUtil.search(TestUtil.getMailbox(acctName), "chorus", MailItem.Type.MESSAGE);
        
        //check clusterstatus
        ModifiableSolrParams params = new ModifiableSolrParams();
        ClusterStatus req =  new CollectionAdminRequest.ClusterStatus();
        req.setCollectionName(acct.getId());
        try {
            CollectionAdminResponse resp = req.process(cloudSolrServer);
            assertNotNull("clusterstatus response is null", resp);
            NamedList<Object> response = resp.getResponse();
            Object clusterObj = response.get("cluster");
            assertNotNull("could not find 'cluster' element in clusterstatus response", clusterObj);
            assertTrue("'cluster' element should be a list", clusterObj instanceof NamedList);
            NamedList clusterList = (NamedList)clusterObj;
            Object collectionsObj = clusterList.get("collections");
            assertNotNull("could not find 'collections' element in 'cluster' list", collectionsObj);
            assertTrue("'collections' element should be a list", collectionsObj instanceof NamedList);
            NamedList collectionsList = (NamedList)collectionsObj;
            Object collectionObj = collectionsList.get(acct.getId());
            assertNotNull("could not find a list element for account " + acct.getId(), collectionObj);
            assertTrue(acct.getId() + " element should be a Map ", collectionObj instanceof HashMap);
            HashMap collectionProps = (HashMap)collectionObj;
            
            Object replicationFactorObj = collectionProps.get("replicationFactor");
            assertNotNull("could not find 'replicationFactor' element in " + acct.getId(), replicationFactorObj);
            
            Object maxShardsPerNodeObj = collectionProps.get("maxShardsPerNode");
            assertNotNull("could not find 'maxShardsPerNode' element in " + acct.getId(), maxShardsPerNodeObj);
            
            assertEquals("replicationFactor should be 2", Integer.parseInt(replicationFactorObj.toString()),replFactor);
            assertEquals("maxShardsPerNode should be 1", Integer.parseInt(maxShardsPerNodeObj.toString()),maxShardsPerNode);
            
            
        } catch (SolrException | IOException | SolrServerException e) {
            fail(e.getMessage());
        }
    }
	
	/**
     * Test a setting that requires more nodes than are available 
     * @throws Exception
     */
    @Test
    public void testInvalidReplicationSettings() throws Exception {
        int maxShardsPerNode = 1;
        Provisioning.getInstance().getLocalServer().setSolrMaxShardsPerNode(maxShardsPerNode);
        CloudSolrClient cloudSolrServer = cloudSolrServer = new CloudSolrClient(
                Provisioning.getInstance().getLocalServer().getIndexURL().substring(10), 
                new LBHttpSolrClient(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
        //check clusterstatus
        ModifiableSolrParams params = new ModifiableSolrParams();
        ClusterStatus req =  new CollectionAdminRequest.ClusterStatus();
        try {
            CollectionAdminResponse resp = req.process(cloudSolrServer);
            assertNotNull("clusterstatus response is null", resp);
            NamedList<Object> response = resp.getResponse();
            Object clusterObj = response.get("cluster");
            assertNotNull("could not find 'cluster' element in clusterstatus response", clusterObj);
            assertTrue("'cluster' element should be a list", clusterObj instanceof NamedList);
            NamedList clusterList = (NamedList)clusterObj;
            Object liveNodesObj = clusterList.get("live_nodes");
            assertNotNull("could not find 'live_nodes' element in 'cluster' list", liveNodesObj);
            assertTrue("'live_nodes' element should be a Collection, but it is " + liveNodesObj.getClass().getName(), liveNodesObj instanceof Collection);
            Collection liveNodes = (Collection)liveNodesObj;
            int numLiveNodes = liveNodes.size();
            assertTrue("SolrCloud does not have any live nodes, cannot continue the test", numLiveNodes > 0);
            /*setting replication factor to number higher than the number of available nodes together with
             * allowing only one replica per node (maxShardsPerNode = 1) should generate an error 
             */
            int replFactor = numLiveNodes+1;
            Provisioning.getInstance().getLocalServer().setSolrReplicationFactor(replFactor);
            
            try {
                String acctName = genAccountName();
                Account acct = TestUtil.createAccount(acctName);
                
                assertTrue("failed to create an account", TestUtil.accountExists(acctName));
                TestUtil.addMessage(TestUtil.getMailbox(acctName), "chorus at end by pupils from the Fourth Form Music Class Islington Green School, London");
                TestUtil.search(TestUtil.getMailbox(acctName), "chorus", MailItem.Type.MESSAGE);
                fail("should have thrown an error");
            } catch(Exception ex) {
                //
            }
            
            
        } catch (SolrException | IOException | SolrServerException e) {
            fail(e.getMessage());
        }
    }
	
    private String genAccountName() {
        return USER_NAME + "@" + BASE_DOMAIN_NAME;
    }

}
