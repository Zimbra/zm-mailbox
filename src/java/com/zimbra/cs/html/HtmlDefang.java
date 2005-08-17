/*
 * Created on Feb 4, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.html;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;

import com.zimbra.cs.util.ByteUtil;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HtmlDefang {

    public static String defang(String html, boolean neuterImages) throws IOException
    {
        return defang(new StringReader(html), neuterImages);
    }
    
    public static String defang(InputStream html, boolean neuterImages) throws IOException
    {
        return defang(new InputStreamReader(html), neuterImages);
    }
    
    public static String defang(Reader htmlReader, boolean neuterImages) throws IOException
    {
        StringWriter out = new StringWriter();
        // create writer filter
        // TODO: uft-8 right?
        /*
        org.cyberneko.html.filters.Writer writer =
            new org.cyberneko.html.filters.Writer(out, "utf-8");
            */
        DefangWriter writer = new DefangWriter(out, "utf-8");
        
        DefangFilter defang = new DefangFilter(neuterImages);
        
        // setup filter chain
        XMLDocumentFilter[] filters = {
            defang,
            writer,
        };

        // create HTML parser
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "match"); 

        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false); 
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        // parse document
        XMLInputSource source = new XMLInputSource(null, null, null, htmlReader, null);
        parser.parse(source);
        return out.toString();
    }

    public static void main(String[] args) throws IOException {
        String html = new String(ByteUtil.getContent(new File(args[0])));
        System.out.println(defang(html, true));
    }
}
