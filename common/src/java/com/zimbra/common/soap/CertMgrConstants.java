/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class CertMgrConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    // ZimbraCertMgrService
    public static final String E_INSTALL_CERT_REQUEST = "InstallCertRequest";
    public static final String E_INSTALL_CERT_RESPONSE = "InstallCertResponse";
    public static final String E_GET_CERT_REQUEST = "GetCertRequest";
    public static final String E_GET_CERT_RESPONSE = "GetCertResponse";
    public static final String E_GEN_CSR_REQUEST = "GenCSRRequest";
    public static final String E_GEN_CSR_RESPONSE = "GenCSRResponse";
    public static final String E_GET_CSR_REQUEST = "GetCSRRequest";
    public static final String E_GET_CSR_RESPONSE = "GetCSRResponse";
    public static final String E_VERIFY_CERTKEY_REQUEST = "VerifyCertKeyRequest";
    public static final String E_VERIFY_CERTKEY_RESPONSE = "VerifyCertKeyResponse";
    public static final String E_UPLOAD_DOMCERT_REQUEST = "UploadDomCertRequest";
    public static final String E_UPLOAD_DOMCERT_RESPONSE = "UploadDomCertResponse";
    public static final String E_UPLOAD_PROXYCA_REQUEST = "UploadProxyCARequest";
    public static final String E_UPLOAD_PROXYCA_RESPONSE = "UploadProxyCAResponse";

    public static final QName INSTALL_CERT_REQUEST = QName.get(E_INSTALL_CERT_REQUEST, NAMESPACE);
    public static final QName INSTALL_CERT_RESPONSE = QName.get(E_INSTALL_CERT_RESPONSE, NAMESPACE);
    public static final QName GET_CERT_REQUEST = QName.get(E_GET_CERT_REQUEST, NAMESPACE);
    public static final QName GET_CERT_RESPONSE = QName.get(E_GET_CERT_RESPONSE, NAMESPACE);
    public static final QName GEN_CSR_REQUEST = QName.get(E_GEN_CSR_REQUEST, NAMESPACE);
    public static final QName GEN_CSR_RESPONSE = QName.get(E_GEN_CSR_RESPONSE, NAMESPACE);
    public static final QName GET_CSR_REQUEST = QName.get(E_GET_CSR_REQUEST, NAMESPACE);
    public static final QName GET_CSR_RESPONSE = QName.get(E_GET_CSR_RESPONSE, NAMESPACE);
    public static final QName VERIFY_CERTKEY_REQUEST = QName.get(E_VERIFY_CERTKEY_REQUEST, NAMESPACE);
    public static final QName VERIFY_CERTKEY_RESPONSE = QName.get(E_VERIFY_CERTKEY_RESPONSE, NAMESPACE);
    public static final QName UPLOAD_DOMCERT_REQUEST = QName.get(E_UPLOAD_DOMCERT_REQUEST, NAMESPACE);
    public static final QName UPLOAD_DOMCERT_RESPONSE = QName.get(E_UPLOAD_DOMCERT_RESPONSE, NAMESPACE);
    public static final QName UPLOAD_PROXYCA_REQUEST = QName.get(E_UPLOAD_PROXYCA_REQUEST, NAMESPACE);
    public static final QName UPLOAD_PROXYCA_RESPONSE = QName.get(E_UPLOAD_PROXYCA_RESPONSE, NAMESPACE);

    // InstallCert
    public static final String A_FILENAME = "filename" ;
    public static final String E_VALIDATION_DAYS = "validation_days" ;
    public static final String E_SUBJECT = "subject" ;
    public static final String E_DIGEST = "digest";
    public static final String E_KEYSIZE = "keysize" ;
    public static final String E_comm_cert = "comm_cert";
    public static final String E_cert = "cert";
    public static final String E_rootCA = "rootCA";
    public static final String E_intermediateCA = "intermediateCA";
    public static final String E_SKIP_CLEANUP = "skipCleanup";

    // GenerateCSR
    public static final String E_SUBJECT_ALT_NAME = "SubjectAltName" ;
    public static final String E_subjectAttr_C = "C";
    public static final String E_subjectAttr_ST = "ST";
    public static final String E_subjectAttr_L = "L";
    public static final String E_subjectAttr_O = "O";
    public static final String E_subjectAttr_OU = "OU";
    public static final String E_subjectAttr_CN = "CN" ;
    public static final String A_new = "new" ;

    // GetCert
    public static final String A_OPTION = "option";

    // GetCSR
    public static final String A_csr_exists = "csr_exists";
    public static final String A_isComm = "isComm";

    // VerifyCertKey
    public static final String A_verifyResult = "verifyResult";
    public static final String A_privkey = "privkey";

    // UploadDomCert
    public static final String A_CERT_AID = "cert.aid";
    public static final String A_CERT_NAME = "cert.filename";
    public static final String A_KEY_AID = "key.aid";
    public static final String A_KEY_NAME = "key.filename";
    public static final String A_cert_content = "cert_content";
    public static final String A_key_content = "key_content";
}
