#!/bin/bash
#
# Deploys the IP-based conditional MFA POC (ZCS-19245) onto a single Zimbra
# 10.1.x test box. Run as root on the target; it drops to the zimbra user where
# required. Everything it replaces is backed up to /opt/zimbra/poc-backup-<ts>.
#
# Not for production: this replaces jars in place rather than installing a package.
#
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ZIMBRA_HOME=/opt/zimbra
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$ZIMBRA_HOME/poc-backup-$STAMP"

say() { printf '\n== %s\n' "$1"; }

if [ "$(id -u)" -ne 0 ]; then
    echo "run as root" >&2
    exit 1
fi
if [ ! -d "$ZIMBRA_HOME" ]; then
    echo "$ZIMBRA_HOME not found -- is this a Zimbra server?" >&2
    exit 1
fi

say "Checking prerequisite: the Network two-factor auth extension"
# Without it TwoFactorAuth falls back to TwoFactorAuthUnavailable, which reports
# twoFactorAuthRequired()==false for every account, and the bypass becomes
# unobservable -- the POC would prove nothing.
if ! su - zimbra -c "zmprov gacf zimbraFeatureTwoFactorAuthAvailable" 2>/dev/null | grep -qi true; then
    echo "WARNING: zimbraFeatureTwoFactorAuthAvailable is not TRUE globally."
    echo "         Confirm the twofactorauth extension is deployed before testing,"
    echo "         or the bypass cannot be distinguished from 2FA being off."
    read -r -p "Continue anyway? [y/N] " reply
    [ "$reply" = "y" ] || exit 1
fi

mkdir -p "$BACKUP"/{jars,conf,ldap,admin}
say "Backing up to $BACKUP"

# ---- 1. Server jars -------------------------------------------------------
# A jar lives in more than one place. zm-build's own patch manifest
# (rpmconf/Patch/conf/zmpatch.xml) replaces zimbrastore.jar in four locations and
# zimbracommon.jar in two, because each webapp carries its own WEB-INF/lib copy.
# Updating only lib/jars leaves mailboxd loading the old classes from the webapp,
# which looks exactly like the feature not working.
say "Installing jars"
install_jar() {
    local jar="$1"; shift
    local n=0
    for dest in "$@"; do
        # Only replace copies this install actually has; skip layouts that differ.
        if [ -f "$dest" ]; then
            mkdir -p "$BACKUP/jars$(dirname "$dest")"
            cp -p "$dest" "$BACKUP/jars$dest"
            install -o zimbra -g zimbra -m 444 "$HERE/jars/$jar" "$dest"
            echo "  $dest"
            n=$((n+1))
        fi
    done
    if [ "$n" -eq 0 ]; then
        echo "  WARNING: found no existing copy of $jar in any expected location" >&2
    fi
}

install_jar zimbracommon.jar \
    "$ZIMBRA_HOME/lib/jars/zimbracommon.jar" \
    "$ZIMBRA_HOME/jetty/common/lib/zimbracommon.jar" \
    "$ZIMBRA_HOME/jetty_base/common/lib/zimbracommon.jar"

install_jar zimbrastore.jar \
    "$ZIMBRA_HOME/lib/jars/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty/webapps/service/WEB-INF/lib/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty/webapps/zimbra/WEB-INF/lib/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty/webapps/zimbraAdmin/WEB-INF/lib/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty_base/webapps/service/WEB-INF/lib/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty_base/webapps/zimbra/WEB-INF/lib/zimbrastore.jar" \
    "$ZIMBRA_HOME/jetty_base/webapps/zimbraAdmin/WEB-INF/lib/zimbrastore.jar"

# soap and client are not in zmpatch.xml; lib/jars is the only known copy, but
# sweep the webapps too in case this install carries one.
install_jar zimbrasoap.jar \
    "$ZIMBRA_HOME/lib/jars/zimbrasoap.jar" \
    "$ZIMBRA_HOME/jetty/webapps/service/WEB-INF/lib/zimbrasoap.jar" \
    "$ZIMBRA_HOME/jetty_base/webapps/service/WEB-INF/lib/zimbrasoap.jar"

install_jar zimbraclient.jar \
    "$ZIMBRA_HOME/lib/jars/zimbraclient.jar" \
    "$ZIMBRA_HOME/jetty/webapps/service/WEB-INF/lib/zimbraclient.jar" \
    "$ZIMBRA_HOME/jetty_base/webapps/service/WEB-INF/lib/zimbraclient.jar"

say "Confirming no stale copies remain"
for j in zimbracommon zimbrastore zimbrasoap zimbraclient; do
    find "$ZIMBRA_HOME" -name "$j.jar" -not -path "$BACKUP/*" 2>/dev/null | while read -r f; do
        if cmp -s "$HERE/jars/$j.jar" "$f"; then
            echo "  ok    $f"
        else
            echo "  STALE $f  <-- not replaced, mailboxd may load old classes from here" >&2
        fi
    done
done

# ---- 2. Attribute metadata ------------------------------------------------
# AttributeManager reads this at mailboxd startup: it supplies the new
# attribute's cardinality and wires the MFABypassIPCallback validator.
say "Installing zimbra-attrs.xml"
cp -p "$ZIMBRA_HOME/conf/attrs/zimbra-attrs.xml" "$BACKUP/conf/"
install -o zimbra -g zimbra -m 444 "$HERE/conf/zimbra-attrs.xml" \
    "$ZIMBRA_HOME/conf/attrs/zimbra-attrs.xml"

# ---- 3. LDAP schema -------------------------------------------------------
# OpenLDAP rejects an unknown attribute on zimbraCOS/zimbraDomain, so the
# schema has to know about zimbraMFAbyPassIP before zmprov can set it.
say "Updating the LDAP schema"
cp -p "$ZIMBRA_HOME/common/etc/openldap/schema/zimbra.schema" "$BACKUP/ldap/" 2>/dev/null || true
install -o zimbra -g zimbra -m 444 "$HERE/ldap/zimbra.schema" \
    "$ZIMBRA_HOME/common/etc/openldap/schema/zimbra.schema"
su - zimbra -c "/opt/zimbra/libexec/zmldapschema" || {
    echo "zmldapschema failed -- restore from $BACKUP before retrying" >&2
    exit 1
}

# ---- 4. Admin console (unpacked, served via ?dev=1) -----------------------
# The production war excludes js/zimbraAdmin/**, so these sources are only
# loaded when the console is opened with ?dev=1.
say "Installing admin console sources"
ADMIN="$ZIMBRA_HOME/jetty_base/webapps/zimbraAdmin"
if [ ! -d "$ADMIN" ]; then
    ADMIN="$ZIMBRA_HOME/jetty/webapps/zimbraAdmin"
fi
if [ ! -d "$ADMIN" ]; then
    echo "cannot find the zimbraAdmin webapp -- skipping the console files" >&2
else
    for f in $(cd "$HERE/admin" && find js -type f); do
        dest="$ADMIN/$f"
        [ -f "$dest" ] && { mkdir -p "$BACKUP/admin/$(dirname "$f")"; cp -p "$dest" "$BACKUP/admin/$f"; }
        mkdir -p "$(dirname "$dest")"
        install -o zimbra -g zimbra -m 444 "$HERE/admin/$f" "$dest"
        echo "  $f"
    done
    MSGS="$ADMIN/WEB-INF/classes/messages"
    mkdir -p "$MSGS"
    [ -f "$MSGS/ZaMsg.properties" ] && cp -p "$MSGS/ZaMsg.properties" "$BACKUP/admin/"
    install -o zimbra -g zimbra -m 444 "$HERE/admin/messages/ZaMsg.properties" \
        "$MSGS/ZaMsg.properties"
    echo "  WEB-INF/classes/messages/ZaMsg.properties"
fi

# ---- 5. Restart -----------------------------------------------------------
say "Restarting mailboxd"
su - zimbra -c "zmmailboxdctl restart"

say "Verifying the attribute is settable"
su - zimbra -c "zmprov desc -a zimbraMFAbyPassIP" | head -5

cat <<EOF

Done. Backup: $BACKUP

Configure and test:
  zmprov mc default zimbraFeatureTwoFactorAuthRequired TRUE
  zmprov mc default zimbraMFAbyPassIP 10.0.0.0/8
  zmprov gc default zimbraMFAbyPassIP

Watch the decision for every login:
  tail -f /opt/zimbra/log/mailbox.log | grep 'MFA IP bypass'

Admin console (unpacked): https://<host>:7071/zimbraAdmin/?dev=1
  COS    -> Advanced -> Two-Factor Authentication
  Domain -> Advanced -> Two-Factor Authentication
  Account-> Features -> General -> trusted device toggle

To roll back:
  cp -Rp $BACKUP/jars/opt/zimbra/. /opt/zimbra/   # restores every replaced copy
  cp -p $BACKUP/conf/zimbra-attrs.xml $ZIMBRA_HOME/conf/attrs/
  cp -p $BACKUP/ldap/zimbra.schema $ZIMBRA_HOME/common/etc/openldap/schema/
  su - zimbra -c "/opt/zimbra/libexec/zmldapschema; zmmailboxdctl restart"
EOF
