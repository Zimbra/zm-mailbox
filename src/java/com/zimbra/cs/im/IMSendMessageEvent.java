package com.zimbra.cs.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMSendMessageEvent implements IMEvent {
    
    IMAddr mFromAddr;
    String mThreadId;
    List<IMAddr> mTargets;
    IMMessage mMessage;
    
    IMSendMessageEvent(IMAddr fromAddr, String threadId, List<IMAddr> targets, IMMessage message) {
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mTargets = targets;
        mMessage = message;
    }
    
    /**
     * @author tim
     *
     * Use an inner class for the notification here since each target mbox's notification
     * has to be different (different sequence ID for each chat participant)
     * 
     */
    public class IMMessageNotification implements IMNotification {
        int mSeqNo;
        
        private IMMessageNotification(int seq) {
            mSeqNo = seq;
        }
        
        public Element toXml(Element parent) throws ServiceException {
            Element e = parent.addElement(IMService.E_MESSAGE);
            e.addAttribute(IMService.A_FROM, mFromAddr.toString());
            e.addAttribute(IMService.A_THREAD_ID, mThreadId);
            e.addAttribute(IMService.A_TIMESTAMP, mMessage.getTimestamp());
            
            e.addAttribute(IMService.A_SEQ, mSeqNo);
            
            mMessage.toXml(e);
            return e;
        }
    }
    
    IMMessageNotification getNotification(int seq) {
        return new IMMessageNotification(seq);
    }

    public void run() throws ServiceException {
        for (IMAddr addr : mTargets) {
            Mailbox mbox = IMRouter.getInstance().getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
                int seqNo = persona.handleMessage(mFromAddr, mThreadId, mMessage);
                mbox.postIMNotification(getNotification(seqNo));
            }
        }
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                mFromAddr, mThreadId, mMessage.toString()).toString();
    }
    
}
