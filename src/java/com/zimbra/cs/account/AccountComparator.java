/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
