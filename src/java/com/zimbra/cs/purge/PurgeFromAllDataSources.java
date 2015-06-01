package com.zimbra.cs.purge;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Mailbox;

public class PurgeFromAllDataSources extends DataSourcePurge {

    public PurgeFromAllDataSources(Mailbox mbox) {
        super(mbox);
    }

    @Override
    List<DataSource> getPurgeableDataSources(DataSource incoming) throws ServiceException {
        return getAllDataSources();
    }

}
