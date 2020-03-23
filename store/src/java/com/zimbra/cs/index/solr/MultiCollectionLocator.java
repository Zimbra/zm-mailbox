package com.zimbra.cs.index.solr;

import java.util.zip.CRC32;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ShardParams;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class MultiCollectionLocator extends SolrCollectionLocator {

    @Override
    String getCollectionName(String accountId, IndexType indexType) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccountById(accountId);
        if (account == null) {
            throw ServiceException.FAILURE(
                    String.format("mailbox index name not found because account=%s not found", accountId), null);
        }
        int numCollections;
        switch (indexType) {
        case CONTACTS:
            numCollections = LC.zimbra_contact_index_num_collections.intValue();
            if(numCollections == 1) {
                return "contact_index"; //TODO: no hardcoding!
            } else {
                return getCollectionName(accountId, LC.zimbra_index_collections_prefix.value(), numCollections);
            }
        case MAILBOX:
            numCollections = LC.zimbra_index_num_collections.intValue();
            if(numCollections == 1) {
                //Check for backward compatibility
                return prov.getConfig().getMailboxIndexName();
            } else {
                return getCollectionName(accountId, LC.zimbra_index_collections_prefix.value(), numCollections);
            }
        case EVENTS:
            // event index isn't currently configured; adding this for completeness
            return prov.getConfig().getEventIndexName();
        default:
            throw ServiceException.FAILURE(String.format("unrecognized index type: %s", indexType), null);
        }
    }

    private String getCollectionName(String accountId, String collectionPrefix, int numCollections) {
        long indexNum = getCollectionNum(accountId, numCollections);
        String indexName = collectionPrefix + indexNum;
        ZimbraLog.index.debug("getCollectionName - indexname is %s", indexName);
        return indexName;
    }

    private long getCollectionNum(String accountId, int numCollections) {
        CRC32 crc = new CRC32();
        crc.update(accountId.getBytes());
        return crc.getValue() % numCollections;
    }

    @Override
    void finalizeDoc(SolrInputDocument document, String accountId) {
        if (document.getFieldValue(LuceneFields.L_ACCOUNT_ID) == null) {
            // only set it once; this can be called multiple times if the item is indexed in multiple collections
            document.addField(LuceneFields.L_ACCOUNT_ID, accountId);
        }
    }

    @Override
    boolean needsAccountFilter() {
        return true;
    }

    @Override
    void finalizeQuery(SolrQuery query, String accountId) {
        query.addFilterQuery(SolrUtils.getAccountFilter(accountId));
        query.set(ShardParams._ROUTE_, String.format("%s!", accountId));
    }

    @Override
    String getSolrId(String accountId, Object... idParts) {
        return String.format("%s!%s", accountId, idJoiner.join(idParts));
    }

}
