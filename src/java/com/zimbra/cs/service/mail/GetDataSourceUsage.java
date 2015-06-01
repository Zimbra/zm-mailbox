package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.DataSourceUsage;
import com.zimbra.soap.mail.message.GetDataSourceUsageResponse;

public class GetDataSourceUsage extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        GetDataSourceUsageResponse resp = new GetDataSourceUsageResponse();
        resp.setDataSourceQuota(account.getDataSourceQuota());
        resp.setDataSourceTotalQuota(account.getDataSourceTotalQuota());
        for (DataSource ds: account.getAllDataSources()) {
            DataSourceUsage dsu = new DataSourceUsage();
            dsu.setId(ds.getId());
            dsu.setUsage(ds.getUsage());
            resp.addDataSourceUsage(dsu);
        }
        return zsc.jaxbToElement(resp);
    }
}
