package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public class SmimeConstants {

    public static final Namespace NAMESPACE = Namespace.get(MailConstants.NAMESPACE_STR);

    public static final String E_SEND_SECURE_MSG_REQUEST = "SendSecureMsgRequest";
    public static final String E_SEND_SECURE_MSG_RESPONSE = "SendSecureMsgResponse";

    public static final QName SEND_SECURE_MSG_REQUEST = QName.get(E_SEND_SECURE_MSG_REQUEST, NAMESPACE);
    public static final QName SEND_SECURE_MSG_RESPONSE = QName.get(E_SEND_SECURE_MSG_RESPONSE, NAMESPACE);

    public static final String A_SIGN = "sign";
    public static final String A_ENCRYPT = "encrypt";

}
