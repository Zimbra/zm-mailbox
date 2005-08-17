
package com.zimbra.cs.html;
import org.cyberneko.html.filters.Writer;

/**
 * extend Writer to override printEntity behavior. IE doesn't like "apos" entities, so write
 * out #39 instead.
 */
public class DefangWriter extends Writer {
    /** Print entity. */
    public DefangWriter(java.io.Writer writer, String encoding) {
        super(writer, encoding);
    }
    
    protected void printEntity(String name) {
        fPrinter.print('&');
        if (name.equals("apos"))
            fPrinter.print("#39");
        else
            fPrinter.print(name);
        fPrinter.print(';');
        fPrinter.flush();
    }
}
