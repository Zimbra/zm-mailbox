# zm-soap Repository which hosts JAXB classes used internally by Zimbra Server and the basis of our WSDL definition

## Inputs from Perforce

- `./build.xml`
- `./docs`
- `./soapdocs`
- `./src/java`
- `./src/java-test`

## Dependencies

- `zm-common`
- `zm-thirdparty-jars`

## Artifacts

- `zimbrasoap.jar`
- `soapapi-changelog.zip`
- `soapapi-zimbra-doc.zip`

## Implementation Notes for JAXB classes

See also [Testing Notes](#testingNotes).  It is important to validate that any new classes can be used within
Zimbra and **also** by code generated from our WSDL definition, which is based on these JAXB classes

### Steps to Create JAXB classes for a new request/response pair

1. See `Steps to add a new namespace` if necessary - although it is recommended that new namespaces are
   **NOT** created unless absolutely necessary.
2. Determine the package name **{PKG}** appropriate for the namespace of your request/response pair.  For instance:

        grep namespaceURI src/java/com/zimbra/soap/*/message/package-info.java

   currently yields output like:

        src/java/com/zimbra/soap/account/message/package-info.java:        @XmlNs(prefix="account", namespaceURI = "urn:zimbraAccount")
        src/java/com/zimbra/soap/admin/message/package-info.java:        @XmlNs(prefix="admin", namespaceURI = "urn:zimbraAdmin")
        src/java/com/zimbra/soap/adminext/message/package-info.java:        @XmlNs(prefix="adminExt", namespaceURI = "urn:zimbraAdminExt")
        src/java/com/zimbra/soap/mail/message/package-info.java:        @XmlNs(prefix="mail", namespaceURI = "urn:zimbraMail")
        src/java/com/zimbra/soap/replication/message/package-info.java:        @XmlNs(prefix="repl", namespaceURI = "urn:zimbraRepl")
        src/java/com/zimbra/soap/sync/message/package-info.java:        @XmlNs(prefix="sync", namespaceURI = "urn:zimbraSync")
        src/java/com/zimbra/soap/voice/message/package-info.java:        @XmlNs(prefix="voice", namespaceURI = "urn:zimbraVoice")

    `{PKG}` is the name of the directory component before message/package-info.java

3. Create `src/java/com/zimbra/soap/{PKG}/message/{Request}.java` where `{Request}` is the top level element name for
   your new request.  Base this on other similar files.  See below for generic advice on JAXB annotations.
4. Create `src/java/com/zimbra/soap/{PKG}/message/{Response}.java` where `{Response}` is the top level element
   name for your new response.
5. Create other JAXB classes for data types needed by your request/response pair if they don't exist already.
6. Update the **MESSAGE_CLASSES** array in `src/java/com/zimbra/soap/JaxbUtil.java`

### Steps to add a new namespace

Ideally, don't!  It makes things way more complicated than it needs to be when you want to share objects.
So, just in case you have chosen to ignore that advice, this is how to proceed:

1.  Choose a new sub-package name `{PKG}` and a service name `{SVC}`  
    e.g. for AppBlast, might choose `{PKG}="appblast"` and `{SVC}=AppblastService`
2.  Create new directories:

        src/java/com/zimbra/soap/{PKG}/message
        src/java/com/zimbra/soap/{PKG}/type

3.  Create new files:

        src/java/com/zimbra/soap/{PKG}/message/package-info.java
        src/java/com/zimbra/soap/{PKG}/type/package-info.java

    These can be based on similar files for another namespace.
    Need to update namespace/package etc info in these files.
4.  Update `src/java/com/zimbra/soap/util/WsdlGenerator.java`.
    Either the `addAdminNamespaceInfo` or `addUserNamespaceInfo` method needs something like :

        nsInfoList.add(WsdlInfoForNamespace.create(AppBlastConstants.NAMESPACE_STR, zcsService,
                       packageToRequestListMap.get("com.zimbra.soap.appblast.message")));

5.  Update `soapdocs/src/java/com/zimbra/doc/soap/WsdlDocGenerator.java`
    the static block which initialises **serviceDescriptions** needs updating
6.  Update binding file `../zm-wsdl-test/bindings/xsdBindings-zcs.xml` to ensure that WSDL tests will function
    correctly.  Any public Wiki articles on how to use our WSDL might need changes too.
    This will need a `<jaxb:bindings>` for the new targetNamespace.

### Manual steps

#### Marshalling / Unmarshalling support

The code that is responsible for marshalling and unmarshalling JAXB classes needs to know about the top level
requests and response classes.


See the `MESSAGE_CLASSES` array in `src/java/com/zimbra/soap/JaxbUtil.java`

#### Ordering of elements.

For pre-existing requests, best not to force an ordering of elements in case this breaks clients.
For responses, because the server controls the order, it is reasonable to enforce an order.

Enforcing an order is achieved via the @XmlType annotation.  For example :

    @XmlType(propOrder = {"hostNames", "stats", "note"})

The strings in the list are the names of fields which map to elements (NOT the `@XmlElement` names).
Attributes cannot be ordered, so their field names are excluded.

#### @XmlRootElement

SOAP Request and Response classes must have this.

In a lot of JAXB, the root element associated with a field is chosen by the `@XmlElement` annotation associated with
the field - so there is no need for an `@XmlRootElement`.  This has the added advantage that classes can
represent types independant of the element name used with them.
Problems were encountered in some of the COS related classes when an un-necessary `@XmlRootElement` was used.
On the other hand, see the gotchas.  Sometimes an `@XmlRootElement` is required...

#### @XmlAccessorType

Tend to prefer `XmlAccessType.NONE` and explicitly label everything that needs
to be annotated rather than letting JAXB do some defaults.

#### enums

For the values of some fields in JAXB objects, Enums are needed under `zm-soap`.
For preference, non-`zm-soap` code should use the `zm-soap` enums to avoid having to keep 2 sets of enums in lock step.

If that isn't possible, either because an enum is already widely used or because it references objects not visible
in `zm-soap`, then using the `zm-soap` enum in the constructor of the other enum should help avoid the enums getting
out of step.

For example, see JAXB enum `com.zimbra.soap.type.TargetType` and `zm-store` enum
`com.zimbra.cs.account.accesscontrol.TargetType`.  The latter enum includes the
`toJaxb()` method and the static method `fromJaxb(com.zimbra.soap.type.TargetType)`.

## Gotchas

*   Do NOT reference non-simple classes outside the `zm-soap` hierarchy in fields relevant to JAXB.
    Failing to do this will result in errors in the resulting WSDL as it will not have
    the information it needs to specify the structure of these classes.
*   Unmarshalling from Xml may not correctly identify superclasses.
    Test before using! e.g. See : `JaxbToElementTest.ConvActionRequestJaxbSubclassHandlingTestDisabled()`
*   Target objects for `@XmlElementRef` etc must have an `@XmlRootElement` annotation
*   For structured fields, e.g. comma separated strings, there is a temptation to use non-JAXB fields for the real data.
*   Elements which have both sub-elements AND a value.  End up having to used `@XmlMixed` which is horrible.  
    **No new code should allow this**.
*   You cannot use basic types like `int` for optional attributes/variables.
    This is because you need something which can have a null value to represent absence.

## Special handling

### Boolean / boolean support using ZmBoolean

XML schema regards the literals `{true, false, 1, 0}` as legal
However, the canonical representation for boolean is the set of literals `{true, false}` and this is reflected in
JAXB behavior when marshalling.  Historically, Zimbra Booleans have been rendered in SOAP response XML as `{1, 0}`
To further complicate things, SOAP response JSON uses `{true, false}`.

When adding `Boolean` or `boolean` attributes/elements to SOAP requests or responses:

1.  Specify the corresponding fields in JAXB with type `ZmBoolean`
2.  Add methods which get and set these fields via Boolean (or boolean for required fields).
    The static methods `ZmBoolean.fromBool(Boolean)`, `ZmBoolean.toBool(ZmBoolean)` and
    `ZmBoolean.toBool(ZmBoolean, boolean)` should be used from these methods.
3.  Do NOT create `ZmBoolean` getters and setters!
4.  If you are creating an element that should be treated as an element in JSON, use an `@JsonSerialize` annotation,
    for example :

        @XmlElement(name=AdminConstants.E_STATUS, required=true)
        @JsonSerialize(using=ZmBooleanContentSerializer.class)
        private final ZmBoolean status;

### FAQ

1. How do you define an `XmlAttribute` or `XmlElement` whose value is an enum?  
   You need to create an enum in `zm-soap`.  Ideally, used this new enum in all code rather than duplicating
   from somewhere else.  For a fairly simple example - see:

        src/java/com/zimbra/soap/type/AccountBy.java

    which uses the `@XmlEnum` annotation.  See:

        src/java/com/zimbra/soap/admin/type/AccountSelector.java

    for how this enum is used for an `@XmlAttribute`.
    If you want the enum values to differ from what is in the xml, see another example - Folder.View:

        src/java/com/zimbra/soap/mail/type/Folder.java

    This also uses `@XmlEnumValue` annotations.

2. For some JAXB classes, `@XmlElement` or `@XmlAttribute` might have property `required` set as `false` even when it
   is not optional.  Is this intentional? 

   If the `@XmlElement` or `@XmlAttribute` is required in all contexts then this is an error.  When the original
   implementor was constructing JAXB classes, situations were encountered where looking at the first use of a
   JAXB type some of these were clearly required but later uses had them as optional.   As this situation can
   be a bit difficult to spot a lot of newer JAXB classes were written with a more relaxed view of whether
   things were required or not.

3. I'm creating new JAXB classes for a request/response in the `urn:zimbraAdmin` namespace which calls code which
   is already used for request/responses in the `urn:zimbraAccount` namespace.  Can I re-use the JAXB objects in
   package `com.zimbra.soap.account.type`?

   You need to ensure that you won't have namespace related problems.  If the SOAP handler code uses Element
   code to build the structure, most elements are added using Element.addElement(String).  This causes the namespace
   associated with the parent to be associated with the new element, which will often mean that it will differ
   depending on the main namespace associated with the request/response being handled and the same JAXB class cannot
   be used.

   There are 2 suggested solutions:

     1.  If the current code is only used in one namespace, then namespace lock the data structure by changing
         the creation of the top level Element for the structure from `Element.addElement(String)` to
         `Element.addElement(QName)`.  Where possible, this is the preferred option as it avoids propagating almost
         duplicate JAXB objects for different namespaces.
         Check the .xsd files produced to make sure they make sense.  There is a gotcha with schemagen where
         it sometimes uses references (ref=)to the other .xsd file but has a poor algorithm for choosing the reference
         name (it just uses the xml element name).  If more than one reference ends up using the same name, you can
         end up with the target type being something like :

             <xs:element name="device" nillable="true" type="xs:anyType"/>

     2.  If the current code is used in multiple namespaces, then the approach I'm taking these days is to create
         almost duplicate JAXB objects in each namespace where the data structure is used and have the objects
         implement an interface which encapsulates the important aspects of their behavior in package
         `com.zimbra.soap.base`.  The plan is that when we move to using JAXB code instead of Element code we will be
         able to use the interface combined with appropriate JAXB factories.  This is rather fiddly but I don't
         think there is a good alternative.

    Aside:
        In some cases in the past, if a JAXB class has only had `@XmlAttribute` (i.e. no `@XmlElement`)
        I have moved the JAXB class to `com.zimbra.soap.type` and re-used it.  This works because we have things
        setup not to tie `@XmlAttribute` to a namespace when processing the JAXB.  However, the issue was raised
        that this is confusing as it isn't clear why this works but objects containing elements don't.
        So, although this results in simpler code it is a deprecated practise.

4. I have wrapped lists which when they have 2 entries end up with an example structure like:

        <ExampleResponse>
            <numbers>
                <number>4</number>
                <number>3</number>
            </numbers>
        </ExampleResponse>

   If I have no numbers, I get:

        <ExampleResponse>
            <numbers/>
        </ExampleResponse>

   but I would like:

        <ExampleResponse>
        </ExampleResponse>

   how do I achieve that?

   Answer - see the `WrapperAbsentIfEmpty` class and its usage from `JaxbToJsonTest`.  Note that
   `@XmlElementWrapper` / `@XmlElement` are on the getter rather than the field (which should have the `@XmlTransient`
   annotation).  JAXB then lets the getter and setter methods control the mapping with XML.  If the return from
   a getter is null, no XML wrapper element will be output, but if the return is an empty list, the wrapper
   element will be present.

<a name="testingNotes"></a>
## Testing notes

It is important to validate that any new classes can be used within Zimbra and **also** by code generated from our
WSDL definition, which is based on these JAXB classes

### ant "test" target
These tests do not require Zimbra to be installed on the system under test.
Useful for exercising marshalling and unmarshalling for JAXB objects under `src/java/`

### zmsoap -z RunUnitTestsRequest

This is the classic `zm-store` RunUnitTests framework which relies on Zimbra being installed on the local
system and some test accounts etc being populated.

Some of this exercices `zm-soap` JAXB classes.
e.g. see `com.zimbra.qa.unittest.TestJaxbProvisioning` in the `zm-store` repository.

### The zm-wsdl-test repository ant "test" target

These tests are intended to be a clean room test of Zimbra's WSDL, with no dependencies on Zimbra product
source code - only using the `.wsdl` and `.xsd` files discoverable from the installed Zimbra Server.

The tests rely on Java API files automatically generated from Zimbra's WSDL definition.

The main intention is to validate that correct, working WSDL client software can be written based on our WSDL.

The JAXB classes under `src/java/` in the `zm-soap` repository are NOT tested directly by these tests, although to
some extent their correctness is being validated as the WSDL files are based on them and the test API is based on
the WSDL.

These tests have the same requirements as the classic `zm-store` RunUnitTests framework.

### TODO

It would be good to have demonstration clients using other programming languages based on our WSDL
