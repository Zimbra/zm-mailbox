package com.zimbra.common.util;

import java.io.IOException;

import javax.servlet.ServletOutputStream;


/**
 * A wrapper of ServletOutputStream that supports the Appendable interface.
 * 
 * We should avoid using a Writer when write to the response stream because:
 * 
 * 1. The Writer returned by HttpServletResponse.getWriter() is a PrintWriter.
 *    PrintWriter eats up IOException then turns on an error flag, and app 
 *    has to call PrintWriter.checkError() in order to detect any error on the wire.
 *    However, calling checkError() on each append() call is not practical, because 
 *    checkError() flushes whatever data in the buffer out to the wire each time 
 *    when it is called, as that is the only way to detect any connection errors.
 *    This is very inefficient.
 *    
 * 2. If we construct a BufferedWriter/OutputStreamWriter from the Stream(a ServletOutputStream)
 *    returned by HttpServletResponse.getOutputStream() and just use the append 
 *    methods of the Writer, then data is buffered in the Writer object and a 
 *    Writer.flush() is needed to flush any buffered data to the jetty's Stream after
 *    we have appended all our data.   The problem of invoking Writer.flush() is 
 *    that it will trigger OutputStream.flush() on the underling OutputStream,
 *    and this interferes with jetty's logic of determining whether to use http 
 *    chunked transfer encoding in the response.  This is because when a flush() 
 *    is invoked on jetty's OutputStream, jetty has no way of knowing if this is the 
 *    end of the response.  As a result, it has to assume there is more data, and flushes 
 *    the current data in the response buffer to the wire as a chunk.  This causes all 
 *    responses, no matter how small it is, is always returned as chunked encoding
 *    (Transfer-Encoding: chunked, instead of setting a Content-Length), even it has only one 
 *    small chunk.  This will add extra overhead to http clients.  We want jetty to only chunk 
 *    large responses.
 *    
 * Our Element class outputs data using the Appendable interface methods.  
 * This wrapper implements the append methods by converting data to UTF-8 and 
 * write to the ServletOutputStream.  This way IOexception won't be eaten(problem of doing 1), 
 * and we don't interfere with jetty's process in deciding on the transfer encoding(problem of doing 2).
 */

public class ZimbraServletOutputStream implements Appendable {

    ServletOutputStream mOut;
    
    public ZimbraServletOutputStream(ServletOutputStream out) {
        mOut = out;
    }

    public Appendable append(CharSequence csq) throws IOException {
        mOut.write(csq.toString().getBytes("utf-8"));
        return this;
    }

    public Appendable append(char c) throws IOException {
        mOut.write(Character.toString(c).getBytes("utf-8"));
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        mOut.write(csq.subSequence(start, end).toString().getBytes("utf-8"));
        return this;
    }

}
