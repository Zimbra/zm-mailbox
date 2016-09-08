package com.zimbra.cs.account;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.cs.account.AttributeManagerUtil.SetterType;

public class TestGenerateEphemeralGetters {

    @Test
    public void testStringGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral, AttributeFlag.expirable);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.purge, true);;

        String getter = "    public String getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getValue(null);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(String zimbraEphemeralAttribute, com.zimbra.cs.ephemeral.EphemeralInput.Expiration expiration) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute, false, false, expiration);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        String purger = "    public void purgeEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        purgeEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute);\n" +
                        "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
        testGeneratedMethod(sb, purger);
    }

    @Test
    public void testMultiStringGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral, AttributeFlag.expirable);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_ASTRING, null, "", true, null, null, AttributeCardinality.multi,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.add, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.remove, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.purge, true);;

        String getter = "    public String[] getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getValues();\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(String[] zimbraEphemeralAttribute, com.zimbra.cs.ephemeral.EphemeralInput.Expiration expiration) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute, false, false, expiration);\n" +
                        "    }";

        String adder = "    public void addEphemeralAttribute(String zimbraEphemeralAttribute, com.zimbra.cs.ephemeral.EphemeralInput.Expiration expiration) throws com.zimbra.common.service.ServiceException {\n" +
                       "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute, true, false, expiration);\n" +
                       "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        String remover = "    public void removeEphemeralAttribute(String zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                         "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute);\n" +
                         "    }";

        String purger = "    public void purgeEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        purgeEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute);\n" +
                        "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, adder);
        testGeneratedMethod(sb, unsetter);
        testGeneratedMethod(sb, remover);
        testGeneratedMethod(sb, purger);
    }

    @Test
    public void testIntGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_INTEGER, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public int getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getIntValue(-1);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(int zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, Integer.toString(zimbraEphemeralAttribute), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testLongGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_LONG, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public long getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getLongValue(-1L);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(long zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, Long.toString(zimbraEphemeralAttribute), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testBooleanGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_BOOLEAN, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public boolean isEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getBoolValue(false);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(boolean zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute ? Provisioning.TRUE : Provisioning.FALSE, false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testEnumGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_ENUM, null, "foo,bar", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public ZAttrProvisioning.EphemeralAttribute getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        try { String v = getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getValue(); return v == null ? null : ZAttrProvisioning.EphemeralAttribute.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(ZAttrProvisioning.EphemeralAttribute zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute.toString(), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testPortGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_PORT, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public int getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getIntValue(-1);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(int zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, Integer.toString(zimbraEphemeralAttribute), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testDurationGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_DURATION, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public long getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        return getEphemeralTimeInterval(Provisioning.A_zimbraEphemeralAttribute, -1L);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(String zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute, false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testTimeGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_GENTIME, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public Date getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        String v = getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getValue(null); return v == null ? null : LdapDateUtil.parseGeneralizedTime(v);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(Date zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute==null ? \"\" : LdapDateUtil.toGeneralizedTime(zimbraEphemeralAttribute), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    @Test
    public void testBinaryGetters() throws Exception {
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.ephemeral);
        AttributeInfo ai = new AttributeInfo("zimbraEphemeralAttribute", 1, null, 0, null, AttributeType.TYPE_BINARY, null, "", true, null, null, AttributeCardinality.single,
                Sets.newHashSet(AttributeClass.account),
                null, flags, null, null, null, null, null, "Test Ephemeral Attribute", null, null, null);
        StringBuilder sb = new StringBuilder();
        AttributeManagerUtil.generateGetter(sb, ai, false, AttributeClass.account);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.set, true);
        AttributeManagerUtil.generateSetter(sb, ai, false, SetterType.unset, true);

        String getter = "    public byte[] getEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                        "        String v = getEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute).getValue(null); return v == null ? null : ByteUtil.decodeLDAPBase64(v);\n" +
                        "    }";

        String setter = "    public void setEphemeralAttribute(byte[] zimbraEphemeralAttribute) throws com.zimbra.common.service.ServiceException {\n" +
                        "        modifyEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, zimbraEphemeralAttribute==null ? \"\" : ByteUtil.encodeLDAPBase64(zimbraEphemeralAttribute), false, false, null);\n" +
                        "    }";

        String unsetter = "    public void unsetEphemeralAttribute() throws com.zimbra.common.service.ServiceException {\n" +
                          "        deleteEphemeralAttr(Provisioning.A_zimbraEphemeralAttribute, null);\n" +
                          "    }";

        testGeneratedMethod(sb, getter);
        testGeneratedMethod(sb, setter);
        testGeneratedMethod(sb, unsetter);
    }

    private void testGeneratedMethod(StringBuilder generated, String shouldContain) {
        assertTrue(generated.toString().contains(shouldContain));
    }
}
