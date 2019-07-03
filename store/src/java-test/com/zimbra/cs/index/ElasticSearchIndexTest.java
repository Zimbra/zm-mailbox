/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Ignore;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexStore.Factory;
import com.zimbra.cs.index.elasticsearch.ElasticSearchConnector;
import com.zimbra.cs.index.elasticsearch.ElasticSearchIndex;

/**
 * Unit test for {@link ElasticSearchIndex}.
 */

@Ignore("Disabled as generally ElasticSearch will not be running.")
public final class ElasticSearchIndexTest extends AbstractIndexStoreTest {

    @Override
    protected String getIndexStoreFactory() {
        return "com.zimbra.cs.index.elasticsearch.ElasticSearchIndex$Factory";
    }

    @Override
    protected boolean indexStoreAvailable() {
        String indexUrl = String.format("%s?_status", LC.zimbra_index_elasticsearch_url_base.value());
        HttpGet method = new HttpGet(indexUrl);
        try {
            ElasticSearchConnector connector = new ElasticSearchConnector();
            connector.executeMethod(method);
        } catch (IOException e) {
            ZimbraLog.index.error("Problem accessing the ElasticSearch Index store", e);
            return false;
        }
        return true;
    }

    @Override
    protected void cleanupForIndexStore() {
        String key = testAcct.getId();
        String indexUrl = String.format("%s%s/", LC.zimbra_index_elasticsearch_url_base.value(), key);
        HttpRequestBase method = new HttpDelete(indexUrl);
        try {
            ElasticSearchConnector connector = new ElasticSearchConnector();
            int statusCode = connector.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                boolean ok = connector.getBooleanAtJsonPath(new String[] {"ok"}, false);
                boolean acknowledged = connector.getBooleanAtJsonPath(new String[] {"acknowledged"}, false);
                if (!ok || !acknowledged) {
                    ZimbraLog.index.debug("Delete index status ok=%b acknowledged=%b", ok, acknowledged);
                }
            } else {
                String error = connector.getStringAtJsonPath(new String[] {"error"});
                if (error != null && error.startsWith("IndexMissingException")) {
                    ZimbraLog.index.debug("Unable to delete index for key=%s.  Index is missing", key);
                } else {
                    ZimbraLog.index.error("Problem deleting index for key=%s error=%s", key, error);
                }
            }
        } catch (IOException e) {
            ZimbraLog.index.error("Problem Deleting index with key=" + key, e);
        }
    }

    // @Test  Just used for exploring code
    public void listIndexes() {
        Factory factory = IndexStore.getFactory();
        ZimbraLog.test.debug("--->TEST listIndexes %s", factory.getClass().getName());
        if (factory instanceof ElasticSearchIndex.Factory) {
            ElasticSearchIndex.Factory esiFactory = (ElasticSearchIndex.Factory) factory;
            List<String> indexNames = esiFactory.getIndexes();
            ZimbraLog.test.debug("indexNames %s", indexNames.toString());
        } else {
            ZimbraLog.test.debug("NOT ESI factory!");
        }
    }
}
