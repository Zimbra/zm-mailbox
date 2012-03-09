package com.zimbra.cs.account.cache;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

public interface IMimeTypeCache {
    public void flushCache(Provisioning prov) throws ServiceException;
    
    public List<MimeTypeInfo> getAllMimeTypes(Provisioning prov) throws ServiceException;
    
    public List<MimeTypeInfo> getMimeTypes(Provisioning prov, String mimeType) throws ServiceException;
}
