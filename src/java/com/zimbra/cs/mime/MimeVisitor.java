package com.liquidsys.coco.mime;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * A class that implements this interface can be passed to {@link Mime#accept}
 * to walk a MIME node tree.
 *   
 * @author bburtin
 */
public interface MimeVisitor {
    /**
     * This flag is passed to the <code>visitXXX</code> methods
     * before a node's children are visited.
     */
    public static int VISIT_BEGIN = 1;
    /**
     * This flag is passed to the <code>visitXXX</code> methods
     * after a node's children have been visited.
     */
    public static int VISIT_END = 2;
    
    /**
     * @see #VISIT_BEGIN
     * @see #VISIT_END
     */
    public void visitMessage(MimeMessage msg, int visitKind)
    throws MessagingException, IOException;
    
    /**
     * @see #VISIT_BEGIN
     * @see #VISIT_END
     */
    public void visitMultipart(MimeMultipart mp, int visitKind)
    throws MessagingException, IOException;
    
    public void visitBodyPart(MimeBodyPart bp)
    throws MessagingException, IOException;
}
