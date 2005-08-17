/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account;

import java.util.Map;

import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Server extends NamedEntry {
    public Map getAttrs(boolean applyConfig) throws ServiceException;
}
