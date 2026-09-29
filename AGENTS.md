# Translation work

Use the shared **zimbra-translation** skill for requests to add a language, fix a
translation, sync locales after English changes, or audit translation quality
in this repository. The skill lives in the sibling repository
`../zm-frontend-ai-skills`, not in this repository. Developers do not need to
install it at user level.

Before starting translation work:

1. Check for `../zm-frontend-ai-skills/skills/zimbra-translation/SKILL.md`.
   If it is missing, stop and ask the developer to clone the skills repository
   alongside this one:

   ```bash
   cd .. && git clone git@github.com:Zimbra/zm-frontend-ai-skills.git
   ```

   Do not improvise the workflow or copy an older skill.
2. Check the skills checkout at `../zm-frontend-ai-skills`. Fetch `origin
   master`, the skills repository's default branch, and fast-forward only if
   that checkout is on `master`, clean, and can be fast-forwarded. If it is on
   another branch, dirty, or cannot fast-forward, do not stash, reset, or
   switch branches; explain that the skill may be out of date and ask whether
   to use the checked-out version.
3. Read `../zm-frontend-ai-skills/skills/zimbra-translation/SKILL.md`, then
   its session protocol and the one workflow matching the request. Paths
   inside the skill resolve from its own folder; paths beginning `.agents/`
   refer to this repository.

The repository's translation configuration is
`.agents/translation/config.yml`. It declares the `server-messages` and
`rights-descriptions` units, their locale files, and their separate context
files in `.agents/translation/context/`. Follow that configuration rather
than guessing paths or supported locales. For an unresolved key, use the
`user_context` column when supplied; do not overwrite it or translate a key
whose context remains unknown.

For a new language, follow the skill's `new-language` workflow and use the
locale code agreed with the user for this repository. Add both configured
bundles and update their locale lists in the config. Add the bundle files to
`stage_zimbra_common_mbox_conf_msgs` in `pkg-builder.pl` so they are packaged.
Check the `L10nMsg.properties` display-name touchpoint, but do not assume
that editing it makes a locale available in the client.

For a translation bug, follow `fix-bug`; for changed English strings, follow
`sync-english-changes`. This repository's base branch is `develop`, not
`master`: branch translation work from `develop` and compare English source
changes against it. `ZsMsg.properties` contains server messages;
`ZsMsgRights.properties` is generated from rights definitions and must not
be edited by hand. Preserve Java `.properties` escaping and message-format
placeholders. Java resource bundles can inherit keys from a parent language
and the base file: do not fill an intentionally partial regional override or
report its absent keys as missing translations without checking the fallback.
Run relevant validation and report any limits of the current checker.

Respect the skill's confirmation gates. Do not commit, push, or open a pull
request without the user's approval.
