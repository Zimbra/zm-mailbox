package com.zimbra.common.soap;

import org.dom4j.QName;

public class SmimeConstants {

    public static final String E_SEND_SECURE_MSG_REQUEST = "SendSecureMsgRequest";
    public static final String E_SEND_SECURE_MSG_RESPONSE = "SendSecureMsgResponse";

    public static final QName SEND_SECURE_MSG_REQUEST = QName.get(E_SEND_SECURE_MSG_REQUEST, MailConstants.NAMESPACE);
    public static final QName SEND_SECURE_MSG_RESPONSE = QName.get(E_SEND_SECURE_MSG_RESPONSE, MailConstants.NAMESPACE);

    public static final String A_SIGN = "sign";
    public static final String A_ENCRYPT = "encrypt";

}
