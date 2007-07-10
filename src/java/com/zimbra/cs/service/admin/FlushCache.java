package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.SkinUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class FlushCache extends AdminDocumentHandler {

    private static enum CacheType {
        // directory scan caches
        skin,
        locale,
        
        // ldap caches
        account,
        cos,
        domain,
        server,
        zimlet;
   }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element eCache = request.getElement(AdminConstants.E_CACHE);
        String type = eCache.getAttribute(AdminConstants.A_TYPE);
        
        try {
            CacheType cacheType = CacheType.valueOf(type);
            
            switch (cacheType) {
            case skin:
                SkinUtil.flushSkinCache();
                break;
            case locale:
                L10nUtil.flushLocaleCache();
                break;
            default:
                throw ServiceException.FAILURE("cache type "+type+" is not yet implemented", null);
            }
            
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid cache type "+type, null);
        }

        Element response = lc.createElement(AdminConstants.FLUSH_CACHE_RESPONSE);
        return response;
    }

}