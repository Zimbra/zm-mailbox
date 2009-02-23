package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllLocales extends AdminDocumentHandler {
    
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Locale locales[] = L10nUtil.getAllLocalesSorted();
        Element response = zsc.createElement(AdminConstants.GET_ALL_LOCALES_RESPONSE);
        for (Locale locale : locales)
            ToXML.encodeLocale(response, locale, Locale.US);
        return response;
    }

    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Allow all admins");
    }
}
