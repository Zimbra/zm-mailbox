package com.zimbra.common.soap;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.soap.Element.XMLElement;

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
        element.addElement("a").addAttribute("n", "hostPwd").addText("secret");
        element.addElement("a").addAttribute("n", "webexZimlet_pwd1").addText("secret");
        element.addElement("dummy2")
               .addAttribute("password", "secret")
               .addAttribute("pass", "secret")
               .addAttribute("pwd", "secret");
        element.addElement("prop").addAttribute("name", "passwd").addText("secret");
        String elementStr = element.prettyPrint(true);
        Assert.assertFalse("Sensitive values have not been masked\n" + elementStr, elementStr.contains("secret"));
    }

    @Test
    public void jsonNamespace() throws Exception {
        Element json = Element.parseJSON("{ \"purge\": [{}] }");
        Assert.assertEquals("default toplevel namespace", "urn:zimbraSoap", json.getNamespaceURI(""));

        json = Element.parseJSON("{ \"purge\": [{}], \"_jsns\": \"urn:zimbraMail\" }");
        Assert.assertEquals("explicit toplevel namespace", "urn:zimbraMail", json.getNamespaceURI(""));

        json = Element.parseJSON("{ \"purge\": [{}], foo: { a: 1, \"_jsns\": \"urn:zimbraMail\" } }");
        Assert.assertEquals("explicit child namespace", "urn:zimbraMail", json.getElement("foo").getNamespaceURI(""));
    }

    @Test
    public void getPathElementList() {
        Element e = XMLElement.mFactory.createElement("parent");
        e.addElement("child");
        Assert.assertEquals(1, e.getPathElementList(new String[] { "child" } ).size());
        Assert.assertEquals(0, e.getPathElementList(new String[] { "bogus" } ).size());
    }
}
