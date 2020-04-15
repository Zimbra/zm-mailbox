package com.zimbra.cs.index.solr;

public class MailboxJointCollectionLocator extends JointCollectionLocator {

    private static final IndexNameFunc INDEX_LOOKUP_FUNC = (accountId, indexTypes) -> SolrUtils.getMailboxIndexName(accountId);

    public MailboxJointCollectionLocator() {
        super(INDEX_LOOKUP_FUNC);
    }

}
