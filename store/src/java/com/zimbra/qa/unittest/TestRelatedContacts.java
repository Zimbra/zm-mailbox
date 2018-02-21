package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.params.CoreAdminParams;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.contacts.AffinityScope;
import com.zimbra.cs.contacts.ContactAffinityQuery;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityTarget;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityType;
import com.zimbra.cs.contacts.RelatedContactsResults;
import com.zimbra.cs.contacts.RelatedContactsResults.RelatedContact;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.logger.BatchingEventLogger;
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;
import com.zimbra.cs.index.solr.SolrUtils;

public class TestRelatedContacts {
    private static CloudSolrClient client;
    private static String zkHost;
    private static String USER_NAME = "TestRelatedContacts";
    private static Account acct;
    private static BatchingEventLogger eventLogger;
    private static String EVENT_COLLECTION_NAME = "TestRelatedContacts";
    private static SolrCloudHelper helper;
    private int idCounter = 0;

    @Before
    public void setUp() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getEventBackendURL();
        Assume.assumeTrue(solrUrl.startsWith("solrcloud"));
        zkHost = solrUrl.substring("solrcloud:".length());
        client = SolrUtils.getCloudSolrClient(zkHost);
        cleanUp();
        acct = TestUtil.createAccount(USER_NAME);
        acct.setContactAffinityEventLoggingEnabled(true);
        SolrCollectionLocator locator = new JointCollectionLocator(EVENT_COLLECTION_NAME);
        helper = new SolrCloudHelper(locator, client, IndexType.EVENTS);
        SolrEventCallback callback = new SolrEventCallback(helper);
        eventLogger = new BatchingEventLogger(10, 1000, callback);
    }

    private void logOutgoingMsgEvents(int nTimes, long timestamp, Recipient... recipients) throws Exception {
        for (int i=0; i<nTimes; i++) {
            for (Event event: getOutgoingEvents(idCounter++, timestamp, recipients)) {
                eventLogger.log(event);
            }
        }
        eventLogger.sendAllBatched();
        commit();
    }

    private void logIncomingMsgEvents(int nTimes, long timestamp, String sender, Recipient... recipients) throws Exception {
        for (int i=0; i<nTimes; i++) {
            for (Event event: getIncomingEvents(idCounter++, timestamp, sender, recipients)) {
                eventLogger.log(event);
            }
        }
        eventLogger.sendAllBatched();
        commit();
    }

    private static Recipient recip(String recipType, String name) {
        return new Recipient(recipType, name);
    }

    private List<Event> getIncomingEvents(int msgId, long timestamp, String sender, Recipient... recipients) {
        List<Event> events = new ArrayList<>();
        for (Recipient recipient: recipients) {
            Event event = Event.generateEvent(acct.getId(), msgId, sender, recipient.addr, EventType.AFFINITY, null, recipient.type, timestamp);
            events.add(event);
        }
        return events;
    }

    private List<Event> getOutgoingEvents(int msgId, long timestamp, Recipient... recipients) {
        List<Event> events = new ArrayList<>();
        for (Recipient recipient: recipients) {
            Event event = Event.generateSentEvent(acct.getId(), msgId, acct.getName(), recipient.addr, null, recipient.type, timestamp);
            events.add(event);
        }
        return events;
    }

    private static void deleteCollection(String collection) {
        CollectionAdminRequest.Delete deleteCollectionRequest = CollectionAdminRequest.deleteCollection(collection);
        try {
            deleteCollectionRequest.process(client);
        } catch (RemoteSolrException | SolrServerException | IOException e) {
            //collection may not exist, that's OK
        }
    }

    protected void commit() throws Exception {
        UpdateRequest commitReq = new UpdateRequest();
        commitReq.setAction(ACTION.COMMIT, true, true);
        commitReq.setParam(CoreAdminParams.COLLECTION, EVENT_COLLECTION_NAME);
        commitReq.process(client);
    }

    public static void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        deleteCollection(EVENT_COLLECTION_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        helper.close();
    }

    private void testResult(RelatedContact contact, String expectedName, AffinityScope expectedScope, Integer expectedCount) {
        assertEquals("wrong affinity scope", expectedScope.getLevel(), contact.getScope());
        assertEquals("wrong contact", expectedName, contact.getName());
        if (expectedCount != null) {
            assertEquals("wrong count", new Double(expectedCount.intValue()), new Double(contact.getScore()));
        }
    }

    private List<RelatedContact> runQuery(RelatedContactsParams params, AffinityScope scope) throws Exception {
        RelatedContactsResults relateContacts = new ContactAffinityQuery(helper, params).execute(scope);
        return relateContacts.getResults();
    }

    @Test
    public void testOutgoingAffinity() throws Exception {
        long timestamp = System.currentTimeMillis();
        logOutgoingMsgEvents(4, timestamp, recip("to", "A"), recip("to", "B"), recip("cc", "C"), recip("bcc", "D"));
        logOutgoingMsgEvents(3, timestamp, recip("to", "A"), recip("to", "B"));
        logOutgoingMsgEvents(2, timestamp, recip("to", "A"), recip("to", "C"));
        logOutgoingMsgEvents(1, timestamp, recip("to", "D"), recip("to", "E"), recip("cc", "F"));

        RelatedContactsParams params = new RelatedContactsParams(acct.getId());
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));

        List<RelatedContact> results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 3 related contacts", 3, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_EXACT_MATCH, 7);
        testResult(results.get(1), "C", AffinityScope.OUTGOING_EXACT_MATCH, 6);
        testResult(results.get(2), "D", AffinityScope.OUTGOING_EXACT_MATCH, 4);

        params.setRequestedAffinityType(AffinityType.to);

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 2 related contacts for 'to' affinity", 2, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_EXACT_MATCH, 7);
        testResult(results.get(1), "C", AffinityScope.OUTGOING_EXACT_MATCH, 2);

        params.setRequestedAffinityType(AffinityType.cc);

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 1 related contacts for 'cc' affinity", 1, results.size());
        testResult(results.get(0), "C", AffinityScope.OUTGOING_EXACT_MATCH, 4);

        params.setRequestedAffinityType(AffinityType.bcc);

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 1 related contacts for 'bcc' affinity", 1, results.size());
        testResult(results.get(0), "D", AffinityScope.OUTGOING_EXACT_MATCH, 4);

        //test multiple targets
        params.addTarget(new AffinityTarget(AffinityType.to, "B"));
        params.addTarget(new AffinityTarget(AffinityType.cc, "C"));

        params.setRequestedAffinityType(AffinityType.bcc);

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 1 contact related to [A,B,C]", 1, results.size());
        testResult(results.get(0), "D", AffinityScope.OUTGOING_EXACT_MATCH, 4);

        //test slightly broader affinity scope that doesn't take into account which fields the targets are in
        params = new RelatedContactsParams(acct.getId());
        params.addTarget(new AffinityTarget(AffinityType.cc, "A"));
        params.addTarget(new AffinityTarget(AffinityType.cc, "B"));
        params.addTarget(new AffinityTarget(AffinityType.cc, "C"));
        params.setRequestedAffinityType(AffinityType.all);

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertTrue("shouldn't find any contacts related to [A,B,C] for the wrong fields", results.isEmpty());

        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH_ANY_FIELD);
        testResult(results.get(0), "D", AffinityScope.OUTGOING_EXACT_MATCH_ANY_FIELD, 4);

        // test broad match affinity
        params = new RelatedContactsParams(acct.getId());
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));
        params.addTarget(new AffinityTarget(AffinityType.to, "D"));
        params.setRequestedAffinityType(AffinityType.to);

        results = runQuery(params, AffinityScope.OUTGOING_BROAD_MATCH);
        assertEquals("should see 3 contacts related to [A,D] with broad match", 3, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_BROAD_MATCH, 7);
        testResult(results.get(1), "C", AffinityScope.OUTGOING_BROAD_MATCH, 2);
        testResult(results.get(2), "E", AffinityScope.OUTGOING_BROAD_MATCH, 1);

        // test broad match affinity ignoring target fields
        params = new RelatedContactsParams(acct.getId());
        params.addTarget(new AffinityTarget(AffinityType.cc, "A"));
        params.addTarget(new AffinityTarget(AffinityType.cc, "D"));
        params.setRequestedAffinityType(AffinityType.to);

        results = runQuery(params, AffinityScope.OUTGOING_BROAD_MATCH_ANY_FIELD);
        assertEquals("should see 3 contacts related to [A,D] with broad match", 3, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_BROAD_MATCH_ANY_FIELD, 7);
        testResult(results.get(1), "C", AffinityScope.OUTGOING_BROAD_MATCH_ANY_FIELD, 2);
        testResult(results.get(2), "E", AffinityScope.OUTGOING_BROAD_MATCH_ANY_FIELD, 1);
    }

    @Test
    public void testIncomingAffinity() throws Exception {
        long timestamp = System.currentTimeMillis();
        // receiving emails from A that also contain B and C
        logIncomingMsgEvents(4, timestamp, toAddr("A"), recip("to", "B"), recip("cc", "C"));
        // receiving emails from D that also contain B and E
        logIncomingMsgEvents(3, timestamp, toAddr("D"), recip("to", "B"), recip("cc", "E"));
        // receiving emails from Z that also contain X and Y
        logIncomingMsgEvents(2, timestamp, toAddr("Z"), recip("to", "X"), recip("to", "Y"));

        RelatedContactsParams params = newParams();
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));
        params.addTarget(new AffinityTarget(AffinityType.to, "B"));
        List<RelatedContact> results = runQuery(params, AffinityScope.INCOMING_FROM_TARGET);
        assertEquals("should see 1 contact related to [A,B]", 1, results.size());
        testResult(results.get(0), "C", AffinityScope.INCOMING_FROM_TARGET, 4);

        params = newParams();
        params.addTarget(new AffinityTarget(AffinityType.to, "B"));
        results = runQuery(params, AffinityScope.INCOMING_FROM_ANY_SENDER);
        assertEquals("should see 2 contacts related to [A,B]", 2, results.size());
        testResult(results.get(0), "C", AffinityScope.INCOMING_FROM_ANY_SENDER, 4);
        testResult(results.get(1), "E", AffinityScope.INCOMING_FROM_ANY_SENDER, 3);

        //add another target from a different affinity group
        params.addTarget(new AffinityTarget(AffinityType.to, "X"));
        results = runQuery(params, AffinityScope.INCOMING_FROM_ANY_SENDER);
        assertEquals("should see 2 contacts related to [A,B,X]", 3, results.size());
        testResult(results.get(0), "C", AffinityScope.INCOMING_FROM_ANY_SENDER, 4);
        testResult(results.get(1), "E", AffinityScope.INCOMING_FROM_ANY_SENDER, 3);
        testResult(results.get(2), "Y", AffinityScope.INCOMING_FROM_ANY_SENDER, 2);
    }

    @Test
    public void testTimeCutoff() throws Exception {
        long timestamp = System.currentTimeMillis();
        logOutgoingMsgEvents(2, timestamp-2000, recip("to", "A"), recip("to", "B"), recip("cc", "C"));
        logOutgoingMsgEvents(1, timestamp, recip("to", "A"), recip("to", "B"));

        RelatedContactsParams params = newParams();
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));
        params.setRequestedAffinityType(AffinityType.all);

        //sanity check
        List<RelatedContact> results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 2 related contacts", 2, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_EXACT_MATCH, 3);
        testResult(results.get(1), "C", AffinityScope.OUTGOING_EXACT_MATCH, 2);

        //this should exclude the first two messages
        params.setDateCutoff(timestamp-1000);
        results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 1 related contact", 1, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_EXACT_MATCH, 1);
    }

    @Test
    public void testMinCooccur() throws Exception {
        long timestamp = System.currentTimeMillis();
        logOutgoingMsgEvents(2, timestamp, recip("to", "A"), recip("to", "B"));
        logOutgoingMsgEvents(1, timestamp, recip("to", "A"), recip("to", "C"));

        RelatedContactsParams params = newParams();
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));
        params.setMinOccurCount(2);
        List<RelatedContact> results = runQuery(params, AffinityScope.OUTGOING_EXACT_MATCH);
        assertEquals("should see 1 related contact when minOccur=2", 1, results.size());
        testResult(results.get(0), "B", AffinityScope.OUTGOING_EXACT_MATCH, 2);
    }

    @Test
    public void testExpandingScope() throws Exception {
        long timestamp = System.currentTimeMillis();
        //exact outgoing match
        logOutgoingMsgEvents(6, timestamp, recip("to", "A"), recip("to", "B"), recip("to", "C"));
        //field-agnostic exact outgoing match
        logOutgoingMsgEvents(5, timestamp, recip("to", "A"), recip("cc", "B"), recip("cc", "D"));
        //partial outgoing match
        logOutgoingMsgEvents(4, timestamp, recip("to", "A"), recip("to", "E"));
        //partial field-agnostic outgoing match
        logOutgoingMsgEvents(3, timestamp, recip("cc", "A"), recip("to", "F"));

        //incoming match through target
        logIncomingMsgEvents(2, timestamp, toAddr("A"), recip("to", "B"), recip("cc", "G"));
        //incoming match though non-target
        logIncomingMsgEvents(1, timestamp, toAddr("X"), recip("to", "B"), recip("to", "H"));

        RelatedContactsParams params = newParams();
        params.setLimit(10);
        params.addTarget(new AffinityTarget(AffinityType.to, "A"));
        params.addTarget(new AffinityTarget(AffinityType.to, "B"));
        params.setIncludeIncomingMsgAffinity(true);
        RelatedContactsResults relatedContacts = new ContactAffinityQuery(helper, params).executeWithExpandingScope();
        List<RelatedContact> results = relatedContacts.getResults();
        assertEquals("should see 6 related contacts", 6, results.size());
        testResult(results.get(0), "C", AffinityScope.OUTGOING_EXACT_MATCH, 6);
        testResult(results.get(1), "D", AffinityScope.OUTGOING_EXACT_MATCH_ANY_FIELD, 5);
        testResult(results.get(2), "E", AffinityScope.OUTGOING_BROAD_MATCH, 4);
        testResult(results.get(3), "F", AffinityScope.OUTGOING_BROAD_MATCH_ANY_FIELD, 3);
        testResult(results.get(4), "G", AffinityScope.INCOMING_FROM_TARGET, 2);
        testResult(results.get(5), "H", AffinityScope.INCOMING_FROM_ANY_SENDER, 1);
    }

    private RelatedContactsParams newParams() {
        return new RelatedContactsParams(acct.getId());
    }

    private static String toAddr(String name) {
        return String.format("%s <%s@zimbra.com>", name, name);
    }
    private static class Recipient extends Pair<String, String> {

        private String type;
        private String addr;

        public Recipient(String recipType, String name) {
            super(recipType, toAddr(name));
            this.type = getFirst();
            this.addr = getSecond();
        }
    }
}
