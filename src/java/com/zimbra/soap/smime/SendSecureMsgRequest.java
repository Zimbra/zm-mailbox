package com.zimbra.soap.smime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.mail.message.SendMsgRequest;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SEND_SECURE_MSG_REQUEST)
public class SendSecureMsgRequest extends SendMsgRequest {

}
