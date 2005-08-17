package com.zimbra.cs.account;

import java.util.Map;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public interface GalContact {
    
    public String getId();

    public Map getAttrs() throws ServiceException;

}
