package com.zimbra.cs.mime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;
import net.freeutils.tnef.mime.TNEFMime;

import com.zimbra.cs.util.JMSession;

/**
 * Provides access to all TNEF attachments in the MIME structure that
 * accepts this visitor.
 *  
 * @author bburtin
 */
public class TnefExtractor implements MimeVisitor {

    private List /* MimeBodyPart */ mTnefBodyParts = new ArrayList();
    private MimeMessage[] mTnefMessages;

    /**
     * Returns all TNEF attachments, converted into MimeMessages, or an
     * empty array if no TNEF attachments are found.
     */
    public MimeMessage[] getTnefsAsMime()
    throws IOException, MessagingException {
        if (mTnefMessages == null) {
            initialize();
        }
        return mTnefMessages;
    }
    
    private void initialize()
    throws IOException, MessagingException {
        // Use a temporary list, in case any of the conversions fail
        List /*MimeMessage */ converted = new ArrayList();
        for (int i = 0; i < mTnefBodyParts.size(); i++) {
            MimeBodyPart bp = (MimeBodyPart) mTnefBodyParts.get(i);
            TNEFInputStream in = new TNEFInputStream(bp.getInputStream());
            MimeMessage msg = TNEFMime.convert(JMSession.getSession(), in);
            converted.add(msg);
        }
        mTnefMessages = new MimeMessage[converted.size()];
        converted.toArray(mTnefMessages);
    }
    
    ////////// MimeVisitor implementation //////////
    
    public void visitBodyPart(MimeBodyPart bp) throws MessagingException {
        if (TNEFUtils.isTNEFMimeType(bp.getContentType())) {
            mTnefBodyParts.add(bp);
        }
    }

    public void visitMessage(MimeMessage msg, int visitKind) {
    }

    public void visitMultipart(MimeMultipart mp, int visitKind) {
    }
}
