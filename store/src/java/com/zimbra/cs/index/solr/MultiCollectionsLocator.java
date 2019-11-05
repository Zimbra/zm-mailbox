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

public class MultiCollectionsLocator extends SolrCollectionLocator {

    @Override
    String getCollectionName(String accountId) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccountById(accountId);
        if (account == null) {
            throw ServiceException.FAILURE(
                    String.format("mailbox index name not found because account=%s not found", accountId), null);
        }

        //Check for backward compatibility
        if(LC.zimbra_index_num_collections.intValue() == 1) {
            return prov.getConfig().getMailboxIndexName();
        }

        CRC32 crc = new CRC32();
        crc.update(accountId.getBytes());
        long val = crc.getValue();
        long index = val % LC.zimbra_index_num_collections.intValue();
        String indexName =  LC.zimbra_index_collections_prefix.value() + index;
        ZimbraLog.index.debug("getCollectionName - indexname is %s", indexName);
        return indexName;
    }

    @Override
    void finalizeDoc(SolrInputDocument document, String accountId) {
        document.addField(LuceneFields.L_ACCOUNT_ID, accountId);
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
