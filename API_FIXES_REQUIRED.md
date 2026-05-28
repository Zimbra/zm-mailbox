# API Fixes Required for Generated Tests

## Root Cause
The test generation agents used incorrect API assumptions. The actual Zimbra APIs differ from the assumed APIs.

## Fixes Required

### 1. Provisioning Access
**WRONG**:
```java
Provisioning prov = MailboxTestUtil.getProvisioning();
```

**CORRECT**:
```java
// First call initProvisioning() in @BeforeClass
MailboxTestUtil.initProvisioning();

// Then access via getInstance()
Provisioning prov = Provisioning.getInstance();
```

### 2. Config Class
**Issue**: Config doesn't have getId() or getName() methods
**Solution**: Config is a singleton - just use it directly or check the actual methods

### 3. Group Methods
**WRONG**:
```java
provisioning.createGroup("testdl", attrs);
provisioning.getGroup(Provisioning.GroupBy.name, "testdl@example.com");
provisioning.addGroupMember(dl, member);
provisioning.removeGroupMember(dl, member);
```

**CORRECT**:
- Check actual method signatures in Provisioning class
- May need different imports or wrapper methods

### 4. EntryType Enum
**WRONG**:
```java
EntryType.GLOBALGRANT
EntryType.IDENTITY
EntryType.DISTRIBUTIONLIST
```

**CORRECT**:
- Import Entry.EntryType (not standalone EntryType)
- Use Entry.EntryType.ACCOUNT, Entry.EntryType.DOMAIN, etc.

### 5. JWTInfo Constructor
**WRONG**:
```java
new JWTInfo("test-account-id", "test-salt");
```

**CORRECT**: 
- Second parameter is long, not String
- Check actual JWTInfo constructor signature

## Affected Test Files

- [ ] ConfigTest.java - Config.getId(), Config.getName()
- [ ] DataSourceTest.java - EntryType reference
- [ ] DistributionListTest.java - createGroup(), GroupBy, EntryType
- [ ] GlobalGrantTest.java - MailboxTestUtil.getProvisioning(), EntryType
- [ ] GroupTest.java - addGroupMember(), getGroupMembers(), EntryType
- [ ] IdentityTest.java - EntryType
- [ ] JWTCacheTest.java - JWTInfo constructor
- [ ] MailTargetTest.java - (need to check)
- [ ] ServerTest.java - (need to check)

## Next Steps

1. Fix each test file to use correct APIs
2. Run compilation check
3. Fix remaining errors iteratively

