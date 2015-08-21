package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.twofactor.ClearTwoFactorAuthDataTask;
import com.zimbra.cs.account.auth.twofactor.ClearTwoFactorAuthDataTask.TaskStatus;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetClearTwoFactorAuthDataStatusRequest;
import com.zimbra.soap.admin.message.GetClearTwoFactorAuthDataStatusResponse;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;

public class GetClearTwoFactorAuthDataStatus extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        GetClearTwoFactorAuthDataStatusRequest req = JaxbUtil.elementToJaxb(request);
        GetClearTwoFactorAuthDataStatusResponse resp = new GetClearTwoFactorAuthDataStatusResponse();
        CosSelector cosSelector = req.getCos();
        Provisioning prov = Provisioning.getInstance();
        Cos cos;
        if (cosSelector.getBy() == CosBy.id) {
            cos = prov.get(com.zimbra.common.account.Key.CosBy.id, cosSelector.getKey());
        } else {
            cos = prov.get(com.zimbra.common.account.Key.CosBy.name, cosSelector.getKey());
        }
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(cosSelector.getKey());
        } else {
            ClearTwoFactorAuthDataTask clearDataTask = ClearTwoFactorAuthDataTask.getInstance();
            TaskStatus status = clearDataTask.getCosTaskStatus(cos.getId());
            resp.setStatus(status.toString());
            return zsc.jaxbToElement(resp);
        }
    }
}
