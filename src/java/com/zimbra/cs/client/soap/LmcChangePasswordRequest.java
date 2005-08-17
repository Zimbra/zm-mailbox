package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.account.AccountService;
import com.liquidsys.coco.service.admin.AdminService;
import com.liquidsys.coco.service.ServiceException;

public class LmcChangePasswordRequest extends LmcSoapRequest {

    private String mOldPassword;
    private String mPassword;
    private String mAccount;
    
    private static final String BY_NAME = "name";
    
    public void setOldPassword(String o) { mOldPassword = o; }
    public void setPassword(String p) { mPassword = p; }
    public void setAccount(String a) { mAccount = a; }
    
    public String getOldPassword() { return mOldPassword; }
    public String getPassword() { return mPassword; }
    public String getAccount() { return mAccount; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountService.CHANGE_PASSWORD_REQUEST);
        // <account>
        Element a = DomUtil.add(request, AccountService.E_ACCOUNT, mAccount);
        DomUtil.addAttr(a, AdminService.A_BY, BY_NAME);
        // <old password>
        DomUtil.add(request, AccountService.E_OLD_PASSWORD, mOldPassword);  
        // <password>
        DomUtil.add(request, AccountService.E_PASSWORD, mPassword);  
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        // there is no response to the request, only a fault
        LmcChangePasswordResponse response = new LmcChangePasswordResponse();
        return response;
    }

}
