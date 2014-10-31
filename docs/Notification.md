Email notification options
---
After a good amount of deliberation, we've finally decided on the
approach we will take for addressing notification systems. This
document attempts to capture the discussions we've had, to avoid
rehashing this issue in the future.

Ultimately the goal is to support notification while making mail
loops unlikely. Here are some of the approaches we use to avoid
mail loops:

* Set sender envelope to <>, which tells the receiving mail
system to not bounce an invalid address.
* Set the Auto-Submitted header to allow us to detect mail loops.
* Limit out-of-office notifications to once per week.
* Send new message notifications from postmaster, so that a second
notification isn't sent in response to a bounced notification.

Out-of-office: from=user, envelope=<>
---
<table style="text-align: left;" border="1" cellpadding="2"
 cellspacing="2">
  <tbody>
    <tr>
      <th style="vertical-align: top;">Case<br>
      </th>
      <th style="vertical-align: top;">Result<br>
      </th>
    </tr>
    <tr>
      <td style="vertical-align: top;">Message from end-user with valid
reply-to address<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>User receives out-of-office reply.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Auto-generated message<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>Don't notify on spam, empty envelope sender, Precedence:
bulk, list or spam, envelope sender is empty, majordomo, mailer-daemon,
listserv, etc.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Bounce (invalid destination)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>Most invalid addresses should not bounce, due to empty
envelope sender.<br>
        </li>
        <li>Most bounces will be caught by the auto-generated message
checks above.<br>
        </li>
        <li>If we don't detect the bounce due to a malformed bounce
message, we send notification to any address once per week.&nbsp; This
fallback plan avoids any mail loops.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Mail loop (simple)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>We set the Auto-Submitted header for notification
messages.&nbsp; If we detect the header on the incoming messages, we
don't send notification.<br>
        </li>
        <li>If the Auto-Submitted header has been stripped out in
transit, we fall back on the once-per-week limit.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Mail loop (complex, such as two
accounts which notify each other)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>If Auto-Submitted is set, don't notify.</li>
        <li>Fall back on once-per-week limit.<br>
        </li>
      </ul>
      </td>
    </tr>
  </tbody>
</table>

New message notification: from=postmaster, envelope=<>
---
New mail notification is more sensitive than out-of-office, since
there's no once-per-week limit. As a result, we set the "from"
header to postmaster. If a notification bounces, it gets returned
to postmaster rather than the user.

<table style="text-align: left;" border="1" cellpadding="2"
 cellspacing="2">
  <tbody>
    <tr>
      <th style="vertical-align: top;">Case<br>
      </th>
      <th style="vertical-align: top;">Result<br>
      </th>
    </tr>
    <tr>
      <td style="vertical-align: top;">Valid notification address<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>Notification is sent and received.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Auto-generated message<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>Don't notify on spam, empty envelope sender, Precedence:
bulk or spam<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Bounce (invalid destination)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>Most invalid addresses should not bounce, due to empty
envelope sender.<br>
        </li>
        <li>Bounces are sent to postmaster, which avoids mail loops.<br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Mail loop (simple)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>We set the Auto-Submitted header for notification
messages.&nbsp; If we detect the header on the incoming messages, we
don't
send notification.<br>
        </li>
        <li><span style="font-weight: bold;">If the Auto-Submitted
header has been stripped out in transit, a mail loop will occur.</span><br>
        </li>
      </ul>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;">Mail loop (complex, such as two
accounts which notify each other)<br>
      </td>
      <td style="vertical-align: top;">
      <ul>
        <li>If Auto-Submitted is set, don't notify.</li>
        <li><span style="font-weight: bold;">If the Auto-Submitted
header is not set on the incoming message, a mail loop will occur.</span><br>
        </li>
      </ul>
      </td>
    </tr>
  </tbody>
</table>
</body>
</html>
