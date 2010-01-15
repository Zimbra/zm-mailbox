/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.BEncoding.BEncodingException;

public class SSLCertInfo {
	
	private static final String ACCEPTABLE = "accept";
	private static final String MISMATCH = "mismatch";
	private static final String ALIAS = "alias";
	private static final String HOSTNAME = "host";
	private static final String COMMON_NAME = "CN";
	private static final String ORGANIZATION_UNIT = "OU";
	private static final String ORGANIZATION = "O";
	private static final String SERIAL_NUMBER = "s";
	private static final String ISSUER_COMMON_NAME = "icn";
	private static final String ISSUER_ORGANIZATION_UNIT = "iou";
	private static final String ISSUER_ORGANIZATION = "io";
	private static final String ISSUED_ON = "from";
	private static final String EXPIRES_ON = "to";
	private static final String SHA1 = "sha1";
	private static final String MD5 = "md5";
	
	private boolean isAcceptable;
	private boolean isMismatch;
	private String alias;
	private String hostname;
	
	private String commonName;
	private String organizationUnit;
	private String organization;
	private String serialNumber;
	
	private String issuerCommonName;
	private String issuerOrganizationUnit;
	private String issuerOrganization;
	
	private Date issuedOn;
	private Date expiresOn;
	
	private String sha1;
	private String md5;
	
	private SSLCertInfo() {}
	
	public SSLCertInfo(String alias, String hostname, X509Certificate cert, boolean isAcceptable, boolean isMismatch) throws GeneralSecurityException {
		this.isAcceptable = isAcceptable;
		this.isMismatch = isMismatch;
		this.alias = alias;
		this.hostname = hostname;
		
		String subjectDn = cert.getSubjectX500Principal().getName();
		commonName = getComponent(subjectDn, COMMON_NAME);
		organizationUnit = getComponent(subjectDn, ORGANIZATION_UNIT);
		organization = getComponent(subjectDn, ORGANIZATION);
		
		serialNumber = cert.getSerialNumber().toString(16).toUpperCase();
		
		String issuerDn = cert.getIssuerDN().getName();
		issuerCommonName = getComponent(issuerDn, COMMON_NAME);
		issuerOrganizationUnit = getComponent(issuerDn, ORGANIZATION_UNIT);
		issuerOrganization = getComponent(issuerDn, ORGANIZATION);
		
		issuedOn = cert.getNotBefore();
		expiresOn = cert.getNotAfter();
		
		byte[] encoded = cert.getEncoded();

		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.update(encoded);
		sha1 = getHexString(md.digest());
		
		md = MessageDigest.getInstance("MD5");
		md.update(encoded);
		md5 = getHexString(md.digest());
	}
	
	public static String getHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes)
			sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1).toUpperCase());
		return sb.toString();
	}
	
	public String serialize() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(ALIAS, alias);
		map.put(HOSTNAME, hostname);
		map.put(COMMON_NAME, commonName);
		map.put(ORGANIZATION_UNIT, organizationUnit);
		map.put(ORGANIZATION, organization);
		map.put(SERIAL_NUMBER, serialNumber);
		map.put(ISSUER_COMMON_NAME, issuerCommonName);
		map.put(ISSUER_ORGANIZATION_UNIT, issuerOrganizationUnit);
		map.put(ISSUER_ORGANIZATION, issuerOrganization);
		map.put(ISSUED_ON, issuedOn.getTime());
		map.put(EXPIRES_ON, expiresOn.getTime());
		map.put(SHA1, sha1);
		map.put(MD5, md5);
		map.put(ACCEPTABLE, Boolean.valueOf(isAcceptable).toString());
		map.put(MISMATCH, Boolean.valueOf(isMismatch).toString());
		return BEncoding.encode(map);
	}
	
	public static SSLCertInfo deserialize(String s) {
		SSLCertInfo certInfo = new SSLCertInfo();
		try {
			Map map = (Map)BEncoding.decode(s);
            certInfo.alias = (String)map.get(ALIAS);
            certInfo.hostname = (String)map.get(HOSTNAME);
            certInfo.commonName = (String)map.get(COMMON_NAME);
            certInfo.organizationUnit = (String)map.get(ORGANIZATION_UNIT);
            certInfo.organization = (String)map.get(ORGANIZATION);
            certInfo.serialNumber = (String)map.get(SERIAL_NUMBER);
            certInfo.issuerCommonName = (String)map.get(ISSUER_COMMON_NAME);
            certInfo.issuerOrganizationUnit = (String)map.get(ISSUER_ORGANIZATION_UNIT);
            certInfo.issuerOrganization = (String)map.get(ISSUER_ORGANIZATION);
            certInfo.issuedOn = new Date((Long)map.get(ISSUED_ON));
            certInfo.expiresOn = new Date((Long)map.get(EXPIRES_ON));
            certInfo.sha1 = (String)map.get(SHA1);
            certInfo.md5 = (String)map.get(MD5);
            certInfo.isAcceptable = Boolean.parseBoolean((String)map.get(ACCEPTABLE));
            certInfo.isMismatch = Boolean.parseBoolean((String)map.get(MISMATCH));
			return certInfo;
		} catch (BEncodingException x) {}
		return null;
	}

	public static String getCertificateCN(X509Certificate cert) {
		return getComponent(cert.getSubjectX500Principal().getName(), COMMON_NAME);
	}
	
	private static String getComponent(String dn, String component) {
		int start = dn.indexOf(component + "=");
		if (start == -1)
			return "";
		start += (component + "=").length();
		int i = start;
		for (; i < dn.length(); ++i) {
			if (dn.charAt(i) == ',' && dn.charAt(i - 1) != '\\')
				break;
		}
		return dn.substring(start, i);
	}
	
	public boolean isAcceptable() {
		return isAcceptable;
	}
	
	public boolean isMismatch() {
		return isMismatch;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public String getCommonName() {return commonName;}
	public String getOrganizationUnit() {return organizationUnit;}
	public String getOrganization() {return organization;}
	public String getSerialNumber() {return serialNumber;}
	        
	public String getIssuerCommonName() {return issuerCommonName;}
	public String getIssuerOrganizationUnit() {return issuerOrganizationUnit;}
	public String getIssuerOrganization() {return issuerOrganization;}
	        
	public Date getIssuedOn() {return issuedOn;}
	public Date getExpiresOn() {return expiresOn;}
	        
	public String getSha1() {return sha1;}
	public String getMd5() {return md5;}
	
	public static void main(String[] args) {
		String dn = "CN=abc, O=foo=bar\\,asdf,OU=123";
		String cn = getComponent(dn, COMMON_NAME);
		String ou = getComponent(dn, ORGANIZATION_UNIT);
		String o = getComponent(dn, ORGANIZATION);
		System.out.println(cn);
		System.out.println(ou);
		System.out.println(o);
	}
}
