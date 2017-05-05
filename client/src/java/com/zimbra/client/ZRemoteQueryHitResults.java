package com.zimbra.client;
import java.io.IOException;
import java.util.Iterator;

import com.zimbra.common.mailbox.ZimbraQueryHit;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.service.ServiceException;

public class ZRemoteQueryHitResults implements ZimbraQueryHitResults {

    private Iterator<ZImapSearchHit> hits;
    public ZRemoteQueryHitResults(ZSearchResult result) {
        this.hits = result.getImapHits().iterator();
    }

    @Override
    public ZimbraQueryHit getNext() throws ServiceException {
        return hits.hasNext() ? hits.next() : null;
    }

    @Override
    public void close() throws IOException {}
}
