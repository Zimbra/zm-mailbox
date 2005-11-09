/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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
 * Created on Jan 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Comparator;

/**
 * @author Greg Solovyev
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AccountComparator implements Comparator {

	private int iCompareField;
	private String szAttrName;
	
	public AccountComparator(int field, String attrName) {
		iCompareField = field;
		szAttrName = attrName;
	}
	
	public AccountComparator () {
		iCompareField = 0;
		szAttrName = null;
	}
	
	public int compare(Object o1, Object o2) {
		int retVal = 0;
		try {
			if (o1 instanceof Account && o2 instanceof Account) {
				switch (iCompareField) {
				case 0:
					retVal = (((Account) o1).getId().compareTo(((Account) o2)
							.getId()));
					break;
				case 1:
					retVal = ((Account) o1).getDomainName().compareTo(
							((Account) o2).getDomainName());
					break;
				case 2:
					retVal = ((Account) o1).getAttr(szAttrName).compareTo(
							((Account) o2).getAttr(szAttrName));
					break;
				default:
					retVal = 0;
					break;
				}
			} else {
				retVal = 0;
			}
		} catch (Exception ex) {
			retVal = 0;
		}
		return retVal;
	}
	
	public void setCompareByID() {
		iCompareField = 0;
	}
	
	public void setCompareByDomain() {
		iCompareField = 1;
	}
	
	public void setCompareByAttribute(String attrName) {
		szAttrName = attrName;
		iCompareField = 2;
	}
}
