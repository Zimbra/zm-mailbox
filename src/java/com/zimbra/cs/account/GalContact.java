package com.liquidsys.coco.account;

import java.util.Map;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 */
public interface GalContact {
    
    public String getId();

    public Map getAttrs() throws ServiceException;

}
