/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 15, 2005
 *
 */
package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * Interface for iterating through ZimbraHits.  This class is the thing
 * that is returned when you do a Search.
 * 
 */
public interface ZimbraQueryResults {
    
    void resetIterator() throws ServiceException;
    
    ZimbraHit getNext() throws ServiceException;
    
    ZimbraHit peekNext() throws ServiceException;
    
	ZimbraHit getFirstHit() throws ServiceException;
	
    ZimbraHit skipToHit(int hitNo) throws ServiceException;
    
    boolean hasNext() throws ServiceException;
    
    /**
     * MUST be called when you are done with this iterator!!
     * 
     * @throws ServiceException
     */
    void doneWithSearchResults() throws ServiceException;
}
