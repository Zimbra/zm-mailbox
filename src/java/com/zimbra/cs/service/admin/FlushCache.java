package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.SkinUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class FlushCache extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element eCache = request.getElement(AdminConstants.E_CACHE);
        String type = eCache.getAttribute(AdminConstants.A_TYPE);
        
        if (type.equals("skin"))
            SkinUtil.flushSkinCache();
        else if (type.equals("locale"))
            L10nUtil.flushLocaleCache();
        else {
            Provisioning.CacheEntryType cacheType = Provisioning.CacheEntryType.fromString(type);
                
            List<Element> eEntries = eCache.listElements(AdminConstants.E_ENTRY);
            CacheEntry[] entries = null;
            if (eEntries.size() > 0) {
                entries = new CacheEntry[eEntries.size()];
                int i = 0;
                for (Element eEntry : eEntries) {
                    entries[i++] = new CacheEntry(CacheEntryBy.valueOf(eEntry.getAttribute(AdminConstants.A_BY)),
                                                  eEntry.getText());
                }
            }
            Provisioning.getInstance().flushCache(cacheType, entries);
        }

        Element response = lc.createElement(AdminConstants.FLUSH_CACHE_RESPONSE);
        return response;
    }

}