/*
 * Created on Jan 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account;

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
