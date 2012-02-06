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

    @Test
    public void flatten() throws Exception {
        Element a = Element.parseXML("<a><b foo=\"bar\">doo<c/>wop</b></a>");
        Assert.assertEquals("toplevel is <a>", "a", a.getName());
        Assert.assertEquals("<a> has no attrs", 0, a.listAttributes().size());
        Assert.assertEquals("<a> has 1 child", 1, a.listElements().size());

        Element b = a.listElements().get(0);
        Assert.assertEquals("child is <b>", "b", b.getName());
        Assert.assertEquals("<b> has 1 attr", 1, b.listAttributes().size());
        Assert.assertEquals("<b> attr foo=bar", "bar", b.getAttribute("foo"));
        Assert.assertEquals("<b> has no children", 0, b.listElements().size());
        Assert.assertEquals("<b>'s contents are flattened", "doo<c/>wop", b.getText());
    }
}
