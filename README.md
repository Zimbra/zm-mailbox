This repository has the core platform code for Zimbra Collaboration Suite.

[![Build Status](https://travis-ci.org/Zimbra/zm-mailbox.svg?branch=master)](https://travis-ci.org/Zimbra/zm-mailbox)

## Translation support using the shared Copilot skill

Use the shared [`zimbra-translation` skill](https://github.com/Zimbra/zm-frontend-ai-skills/blob/master/README.md#the-zimbra-translation-skill) for backend translation work in this repository. It supports adding a language, fixing a translation issue, syncing non-English translations after English strings change, and auditing translation quality.

The repository configuration at `.agents/translation/config.yml` describes the supported locale identifiers, message bundles, and context files. The configured bundles are:

| Bundle | Purpose |
|---|---|
| `store-conf/conf/msgs/ZsMsg.properties` | Server messages, including notifications, calendar replies, and errors. |
| `store-conf/conf/msgs/ZsMsgRights.properties` | Permission descriptions returned to the administration interface. This file is generated from rights definitions; **do not edit it by hand**. |

Backend messages can appear in different product surfaces, so English text alone may not establish a message's intended meaning. The skill uses the maintained context files under `.agents/translation/context/` and should ask for clarification when the context is uncertain. A native-language review is still needed before considering translations complete.

For setup instructions and example requests covering all four workflows, see [**How to invoke the translation skill from a consumer repository**](https://github.com/Zimbra/zm-frontend-ai-skills/blob/master/README.md#how-to-invoke-the-zimbra-translation-skill-from-a-consumer-repository).

The developer machine needs Git and Node.js, plus the sibling skills checkout. The skill checks Node.js and its bundled checker before starting; the checker has no npm dependencies. Run relevant `zm-mailbox` build or test checks in the repository's configured development environment.

### Backend locale and packaging notes

- Use the locale identifiers declared in `.agents/translation/config.yml`. Java resource-bundle identifiers can differ from frontend locale codes; for example, Indonesian may use `in` and Hebrew may use `iw`. Do not derive backend identifiers from frontend locale names.
- New `ZsMsg` and `ZsMsgRights` locale bundles must be added to `pkg-builder.pl` under `stage_zimbra_common_mbox_conf_msgs` so they are included in the package.
- Check `store-conf/conf/msgs/L10nMsg.properties` as a locale display-name touchpoint. Follow the existing pattern; this file is primarily locale registration/display metadata, not a normal prose translation bundle.
- Preserve Java `.properties` structure, escaping, continuation lines, and message-format placeholders such as `{0}`. Where required by the repository, encode non-ASCII characters as `\uXXXX`.
- Regional bundles may intentionally contain only overrides and inherit other keys from a parent locale or the base bundle. Do not treat every absent key as a missing translation without checking the resource-bundle fallback.

The skill runs structural validation for the configured files, but validation does not replace native-language review or repository-specific build and test checks.
