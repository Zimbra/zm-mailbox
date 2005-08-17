package com.liquidsys.coco.client.soap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.liquidsys.coco.service.account.AccountService;
import com.zimbra.soap.DomUtil;


public class LmcModifyPrefsRequest extends LmcSoapRequest {

    private Map mPrefMods;      

    public void setPrefMods(Map m) { mPrefMods = m; }
    
    public Map getPrefMods() { return mPrefMods; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountService.MODIFY_PREFS_REQUEST);

        Set s = mPrefMods.entrySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            Element pe = DomUtil.add(request, AccountService.E_PREF, 
                                     (String) entry.getValue());  
            DomUtil.addAttr(pe, AccountService.A_NAME, (String) entry.getKey());
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
    {
        // there is no data provided in the response
        return new LmcModifyPrefsResponse();
    }

}
