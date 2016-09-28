package com.zimbra.common.soap;

import org.dom4j.QName;

public class SmimeConstants {

    public static final String E_SEND_SECURE_MSG_REQUEST = "SendSecureMsgRequest";
    public static final String E_SEND_SECURE_MSG_RESPONSE = "SendSecureMsgResponse";
    public static final String E_GET_CERT_INFO_REQUEST = "GetCertificateInfoRequest";
    public static final String E_GET_CERT_INFO_RESPONSE = "GetCertificateInfoResponse";
    public static final String E_SAVE_SMIME_CERTIFICATE_REQUEST = "SaveSmimeCertificateRequest";
    public static final String E_SAVE_SMIME_CERTIFICATE_RESPONSE = "SaveSmimeCertificateResponse";

    public static final QName SEND_SECURE_MSG_REQUEST = QName.get(E_SEND_SECURE_MSG_REQUEST, MailConstants.NAMESPACE);
    public static final QName SEND_SECURE_MSG_RESPONSE = QName.get(E_SEND_SECURE_MSG_RESPONSE, MailConstants.NAMESPACE);
    public static final QName GET_CERT_INFO_REQUEST = QName.get(E_GET_CERT_INFO_REQUEST, AccountConstants.NAMESPACE);
    public static final QName GET_CERT_INFO_RESPONSE = QName.get(E_GET_CERT_INFO_RESPONSE, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_REQUEST = QName.get(E_SAVE_SMIME_CERTIFICATE_REQUEST, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_RESPONSE = QName.get(E_SAVE_SMIME_CERTIFICATE_RESPONSE, AccountConstants.NAMESPACE);

    public static final String A_SIGN = "sign";
    public static final String A_ENCRYPT = "encrypt";
    public static final String A_CERT_ID = "certId";
    public static final String A_CERTIFICATE_PASSWORD = "password";
    public static final String A_REPLACE_ID = "replaceId";

    public static final String E_CERTIFICATE = "certificate";
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

}
