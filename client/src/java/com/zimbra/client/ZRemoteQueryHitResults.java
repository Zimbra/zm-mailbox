package com.zimbra.client;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.mailbox.ZimbraQueryHit;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.service.ServiceException;

public class ZRemoteQueryHitResults implements ZimbraQueryHitResults {

    private Iterator<ZImapSearchHit> hitsIter;
    public ZRemoteQueryHitResults(List<ZImapSearchHit> hits) {
        this.hitsIter = hits.iterator();
    }

    @Override
    public ZimbraQueryHit getNext() throws ServiceException {
        return hitsIter.hasNext() ? hitsIter.next() : null;
    }

    @Override
    public void close() throws IOException {}
}
