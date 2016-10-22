/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.soap;

import org.dom4j.QName;

public class SmimeConstants {

    public static final String E_SEND_SECURE_MSG_REQUEST = "SendSecureMsgRequest";
    public static final String E_SEND_SECURE_MSG_RESPONSE = "SendSecureMsgResponse";
    public static final String E_GET_SMIME_CERT_INFO_REQUEST = "GetSmimeCertificateInfoRequest";
    public static final String E_GET_SMIME_CERT_INFO_RESPONSE = "GetSmimeCertificateInfoResponse";
    public static final String E_SAVE_SMIME_CERTIFICATE_REQUEST = "SaveSmimeCertificateRequest";
    public static final String E_SAVE_SMIME_CERTIFICATE_RESPONSE = "SaveSmimeCertificateResponse";

    public static final QName SEND_SECURE_MSG_REQUEST = QName.get(E_SEND_SECURE_MSG_REQUEST, MailConstants.NAMESPACE);
    public static final QName SEND_SECURE_MSG_RESPONSE = QName.get(E_SEND_SECURE_MSG_RESPONSE, MailConstants.NAMESPACE);
    public static final QName GET_SMIME_CERT_INFO_REQUEST = QName.get(E_GET_SMIME_CERT_INFO_REQUEST, AccountConstants.NAMESPACE);
    public static final QName GET_SMIME_CERT_INFO_RESPONSE = QName.get(E_GET_SMIME_CERT_INFO_RESPONSE, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_REQUEST = QName.get(E_SAVE_SMIME_CERTIFICATE_REQUEST, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_RESPONSE = QName.get(E_SAVE_SMIME_CERTIFICATE_RESPONSE, AccountConstants.NAMESPACE);

    public static final String A_SIGN = "sign";
    public static final String A_ENCRYPT = "encrypt";
    public static final String A_CERT_ID = "certId";
    public static final String A_CERTIFICATE_PASSWORD = "password";
    public static final String A_REPLACE_ID = "replaceId";

    public static final String E_CERTIFICATE = "certificate";
    public static final String E_EMAIL_ADDR = "emailAddress";
    public static final String E_SUBJECT_DN = "subjectDn";
    public static final String E_ISSUER_DN = "issuerDn";
    public static final String E_SERIAL_NO = "serialNo";
    public static final String E_ALGORITHM = "algorithm";
    public static final String E_VALIDITY = "validity";
    public static final String E_SIGNATURE = "signature";
    public static final String E_START_DATE = "startDate";
    public static final String E_END_DATE = "endDate";
    public static final String A_PUB_CERT_ID = "pubCertId";
    public static final String A_PVT_KEY_ID = "pvtKeyId";
    public static final String A_DEFAULT = "default";

    public static final String PUB_CERT = "pubCert";
    public static final String PVT_KEY = "pvtKey";
    public static final String ALIAS = "alias";
    public static final String CERT_FOLDER_NAME = "-smimecertificates";

    public static final String CERT_SUFFIX = ".pem";
    public static final String KEY_SUFFIX = ".key";
    public static final String PUB_CERT_TYPE = "X.509";
    public static final String PRIVATE_KEY_ALGORITHM = "keyAlgorithm";
    public static final String PRIVATE_KEY_FORMAT = "keyFormat";
    public static final String PRIVATE_KEY_INFO = "privateKeyInfo";
    public static final String PUBLIC_CERT_INFO = "pubCertInfo";

}
