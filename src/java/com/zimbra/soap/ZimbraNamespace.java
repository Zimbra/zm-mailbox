/*
 * Created on May 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * @author schemers
 *
 * Registry for assigned XML namespaces
 */
public class ZimbraNamespace {
	
	public static final String LIQUID_STR = "urn:liquid";
	public static final Namespace LIQUID = Namespace.get(LIQUID_STR);

    public static final QName E_BATCH_REQUEST = QName.get("BatchRequest", LIQUID);
    public static final QName E_BATCH_RESPONSE = QName.get("BatchResponse", LIQUID);
    public static final QName E_CODE = QName.get("Code", LIQUID); 
    public static final QName E_ERROR = QName.get("Error", LIQUID); 

    public static final String A_ONERROR = "onerror";
    public static final String DEF_ONERROR = "continue";
    
	public static final String LIQUID_ACCOUNT_STR = "urn:liquidAccount";
	public static final Namespace LIQUID_ACCOUNT = Namespace.get(LIQUID_ACCOUNT_STR);

}
