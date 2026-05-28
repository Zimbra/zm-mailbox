## QUALITY STANDARDS — READ THIS CAREFULLY

The user's primary requirement: **quality tests that exercise business/functional logic**. Single-line, mock-only, or placeholder tests are unacceptable and will be rejected in review.

### Minimum contract for EVERY test

Every test MUST satisfy ALL of these:

1. **Realistic, domain-meaningful inputs** in the Arrange section — not `"foo"`, `"bar"`, `"test"`, `"aaa"`. Use real-looking email addresses, account names, MIME messages, calendar dates, folder paths, attribute values. Prefer `MailboxTestUtil` fixtures over fabricated mocks.
2. **An Act section** that invokes the actual production method under test.
3. **At least one assertion that verifies the BUSINESS OUTCOME** — return value, mutated mailbox/db/index state, response Element structure, thrown exception code+message — not just `verify(mock)` on a collaborator.
4. **Test name describes the scenario**: `methodName_specificCondition_specificExpectedBehavior`. Not `testHappyPath`, not `test1`, not `testMethodWorks`.
5. **Exercise the actual production code path** — do not mock the class under test, do not `spy` to bypass real logic in the method being tested.

### REJECTION CRITERIA — any test matching these is unacceptable

Do NOT write tests that:

- Only assert "no exception thrown" with no other assertion
- Only verify a getter returns what a setter set (unless the getter has logic)
- Mock every collaborator AND only assert mock interactions — this tests Mockito, not the code
- Have only a single `verify(...)` call with no state or return-value assertion
- Take the form `when(mock.x()).thenReturn(expected); assertEquals(expected, sut.delegate())` — that is a tautology
- Have placeholder bodies, `Assert.assertTrue(true)`, empty bodies, or `// TODO`
- Test trivial getter/setter pairs on POJOs (apply the skip list)
- Use generic inputs that do not reflect real usage
- Duplicate an existing test with only wording changes
- Mock the class under test or `spy` to skip the actual logic
- Mock `Mailbox`, `Folder`, or `MailItem` when `MailboxTestUtil` could provide a real instance

### GOOD vs BAD examples

**BAD — rejected:**

```java
@Test
public void testAddMessage() throws Exception {
    mailbox.addMessage(mockParsedMessage);
    verify(mockParsedMessage).getRawSize();
}
```

Why rejected: mocks the input, asserts nothing about the resulting mailbox state, verifies only an irrelevant mock interaction. The test would pass even if `addMessage` did nothing.

**GOOD — accepted:**

```java
@Test
public void addMessage_textMessageToInbox_persistsAndIncrementsFolderCount() throws Exception {
    Account account = MailboxTestUtil.createAccount();
    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
    Folder inbox = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);
    int countBefore = inbox.getItemCount();

    ParsedMessage pm = MailboxTestUtil.generateMessage(
        "From: alice@example.com\r\nTo: bob@example.com\r\nSubject: Q4 report\r\n\r\nSee attached.");
    int msgId = mbox.addMessage(null, pm, MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

    Message stored = mbox.getMessageById(null, msgId);
    assertEquals("Q4 report", stored.getSubject());
    assertEquals(Mailbox.ID_FOLDER_INBOX, stored.getFolderId());
    assertEquals(countBefore + 1, mbox.getFolderById(Mailbox.ID_FOLDER_INBOX).getItemCount());
    assertTrue(stored.getSize() > 0);
}
```

Why accepted: real in-memory mailbox, real MIME content, asserts persisted state (subject, folder, count, size). Catches regressions across the full add-message path.

**BAD — rejected:**

```java
@Test
public void testGetCalendarRequest() throws Exception {
    Element response = handler.handle(request, context);
    assertNotNull(response);
}
```

Why rejected: only asserts response is non-null. A handler returning an empty element would pass.

**GOOD — accepted:**

```java
@Test
public void handle_getCalendarOnEmptyMailbox_returnsResponseWithZeroAppointments() throws Exception {
    Element request = new Element.XMLElement(MailConstants.GET_CALENDAR_REQUEST);
    request.addAttribute(MailConstants.A_FOLDER, Mailbox.ID_FOLDER_CALENDAR);

    Element response = handler.handle(request, getContext(account));

    assertEquals(MailConstants.GET_CALENDAR_RESPONSE, response.getName());
    assertEquals(0, response.listElements(MailConstants.E_APPOINTMENT).size());
}

@Test
public void handle_getCalendarWithTwoAppointments_returnsBothWithCorrectTimes() throws Exception {
    long now = System.currentTimeMillis();
    MailboxTestUtil.createAppointment(mbox, "Team standup", now + 3600_000L, now + 7200_000L);
    MailboxTestUtil.createAppointment(mbox, "Code review",  now + 7200_000L, now + 10800_000L);

    Element response = handler.handle(buildRequest(Mailbox.ID_FOLDER_CALENDAR), getContext(account));

    List<Element> appts = response.listElements(MailConstants.E_APPOINTMENT);
    assertEquals(2, appts.size());
    assertEquals("Team standup", appts.get(0).getAttribute(MailConstants.A_NAME));
    assertEquals("Code review",  appts.get(1).getAttribute(MailConstants.A_NAME));
}
```

Why accepted: asserts the response shape, count, and per-item content. Realistic calendar fixture.

### BUSINESS-SCENARIO TESTS — required per non-trivial class

In ADDITION to per-method branch tests, every non-trivial class MUST have at least one end-to-end behavioral test simulating a realistic production scenario. Examples:

- **Mailbox / MailItem operations**: simulate a multi-step user flow (add → move → tag → delete) and assert final state.
- **Sieve filter**: parse a realistic rule (`if header :contains "Subject" "INVOICE" { fileinto "Receipts"; }`), apply it to a matching message, assert the message landed in `Receipts`.
- **Service handler**: end-to-end request → response, asserting both the response Element AND the downstream mailbox/db change.
- **Redo op**: serialize → write to stream → read back → replay on fresh mailbox → assert state matches direct application.
- **Index query builder**: build a real user query, run it against a small real Lucene index, assert hit list ordering.
- **DAV PROPFIND**: build a real depth-1 PROPFIND request on a calendar folder with appointments, assert the multistatus response contains correct hrefs, ETags, and component properties.
- **IMAP command sequence**: drive LOGIN → SELECT → FETCH against a real in-memory mailbox, assert the wire-format responses.
- **Auth flow**: drive `Auth → AuthToken → authenticated call → token expiry → re-auth`, asserting state at each step.

Aim for 1–3 such scenario tests per non-trivial class. They catch integration bugs that pure per-method unit tests miss.

### Coverage-per-method minimum

For every non-trivial public method on a class in scope, write a MINIMUM of:

- 1 happy-path test with realistic input
- 1 test per distinct branch (if/else, switch, catch block)
- 1 test per declared `throws` clause that the method actually triggers
- 1 boundary test where the contract has a boundary (0, -1, null, empty, max int, exact-limit values, DST transition, leap year, quota at exactly the limit)

A class with 5 public methods averaging 2 branches each should produce roughly 15–25 tests, not 5.