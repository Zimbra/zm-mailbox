package com.zimbra.common.soap;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class ElementTest {

    @Test
    public void prettyPrintSafeXml() {
        prettyPrintSafe(new Element.XMLElement("dummy"));
    }

    @Test
    public void prettyPrintSafeJson() {
        prettyPrintSafe(new Element.JSONElement("dummy"));
    }

    private void prettyPrintSafe(Element element) {
        element.addElement("password").addText("secret");
        element.addElement("pfxPassword").addText("secret");
        element.addElement("a").addAttribute("n", "pfxPassword").addText("secret");
        element.addElement("dummy2").
                addAttribute("password", "secret").
                addAttribute("pass", "secret").
                addAttribute("pwd", "secret");
        String elementStr = element.prettyPrint(true);
        Assert.assertFalse("Sensitive values have not been masked\n" + elementStr, elementStr.contains("secret"));
    }
}
