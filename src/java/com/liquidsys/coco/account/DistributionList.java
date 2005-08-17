/*
 * Created on Nov 24, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account;

import com.liquidsys.coco.service.ServiceException;

/**
 * @author anandp
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface DistributionList extends NamedEntry {

    public void addMember(String member) throws ServiceException;

    public void removeMember(String member) throws ServiceException;

    public String[] getAllMembers() throws ServiceException;
    
}
