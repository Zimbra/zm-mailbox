#!/bin/bash
# ---------------------------------------------------------------------------
# tfatest.sh -- manage test accounts for the PreAuth + email-2FA flow.
#
#   ./tfatest.sh create  <user> [password]   create account, enable 2FA feature
#   ./tfatest.sh reset   <user>              back to "available, not enrolled"
#   ./tfatest.sh enrolled <user>             report whether 2FA is enrolled
#   ./tfatest.sh status  <user>              show all relevant attrs
#   ./tfatest.sh url     <user>              print a fresh preauth URL
#   ./tfatest.sh code    [recovery-user]     show newest 2FA code in the inbox
#   ./tfatest.sh delete  <user>              remove the account
#
# <user> may be a bare name (rm6) or a full address.
# Run as the ubuntu user; it sudoes to zimbra itself.
# ---------------------------------------------------------------------------
set -u

DOMAIN="rakeshdev-machine1.zimbradev.com"
PREAUTH_KEY="80cf431190dc6bea72be7f5ba1a5c2a2763f6dbf68024b4a72f893d25e75d923"
RECOVERY_DEFAULT="rm1@${DOMAIN}"
DEFAULT_PASSWORD="TestPass123!"

zp() { sudo su - zimbra -c "zmprov $*" 2>&1; }

fq() { case "$1" in *@*) echo "$1";; *) echo "$1@${DOMAIN}";; esac; }

need_user() {
  if [ -z "${1:-}" ]; then echo "error: <user> required" >&2; exit 2; fi
}

# Attributes that make an account eligible for the enrolment prompt.
apply_feature_settings() {
  local u="$1"
  zp "ma $u \
    zimbraFeatureTwoFactorAuthAvailable TRUE \
    zimbraTwoFactorAuthMethodAllowed email \
    zimbraPrefPrimaryTwoFactorAuthMethod email" >/dev/null
}

# Everything written during enrolment, cleared so the account is un-enrolled again.
clear_enrolment() {
  local u="$1"
  # zimbraTwoFactorAuthEnabled must go FALSE before the secret is removed: its
  # callback rejects enabling without a secret, and we want the pair consistent.
  zp "ma $u zimbraTwoFactorAuthEnabled FALSE" >/dev/null
  for a in zimbraTwoFactorAuthMethodEnabled \
           zimbraTwoFactorAuthSecret \
           zimbraTwoFactorAuthScratchCodes \
           zimbraPrefPasswordRecoveryAddress \
           zimbraRecoveryAccountVerificationData \
           zimbraResetPasswordRecoveryCode; do
    zp "ma $u $a ''" >/dev/null
  done
  zp "ma $u zimbraPrefPasswordRecoveryAddressStatus pending" >/dev/null
}

cmd_create() {
  need_user "${1:-}"
  local u; u="$(fq "$1")"
  local pw="${2:-$DEFAULT_PASSWORD}"

  if zp "ga $u zimbraId" | grep -q "zimbraId:"; then
    echo "account $u already exists -- applying settings only"
  else
    local out; out="$(zp "ca $u '$pw'")"
    case "$out" in
      *ERROR*) echo "failed to create $u:"; echo "$out"; exit 1;;
    esac
    echo "created $u (password: $pw)"
  fi

  apply_feature_settings "$u"
  clear_enrolment "$u"
  echo "configured: 2FA available, method=email, not enrolled"
  cmd_status "$u"
}

cmd_reset() {
  need_user "${1:-}"
  local u; u="$(fq "$1")"
  apply_feature_settings "$u"
  clear_enrolment "$u"
  echo "reset $u -> available, not enrolled (enrolment prompt will show again)"
  cmd_status "$u"
}

cmd_status() {
  need_user "${1:-}"
  local u; u="$(fq "$1")"
  echo ""
  echo "--- $u ---"
  zp "ga $u \
    zimbraAccountStatus \
    zimbraFeatureTwoFactorAuthAvailable \
    zimbraFeatureTwoFactorAuthRequired \
    zimbraTwoFactorAuthEnabled \
    zimbraTwoFactorAuthMethodAllowed \
    zimbraTwoFactorAuthMethodEnabled \
    zimbraPrefPrimaryTwoFactorAuthMethod \
    zimbraPrefPasswordRecoveryAddress \
    zimbraPrefPasswordRecoveryAddressStatus" | sed '/^#/d;/^$/d' | sed 's/^/  /'
  local enabled; enabled="$(zp "ga $u zimbraTwoFactorAuthEnabled" | grep -c 'TRUE')"
  if [ "$enabled" -gt 0 ]; then
    echo "  => expect: CHALLENGE (code prompt, no session until verified)"
  else
    echo "  => expect: ENROLMENT (session issued, setup page, skippable)"
  fi
}

cmd_url() {
  need_user "${1:-}"
  local u; u="$(fq "$1")"
  python3 - "$u" "$PREAUTH_KEY" "$DOMAIN" <<'PY'
import hmac, hashlib, time, sys, urllib.parse
acct, key, host = sys.argv[1], sys.argv[2], sys.argv[3]
ts = str(int(time.time() * 1000))
p = {"account": acct, "by": "name", "expires": "0", "timestamp": ts}
mac = hmac.new(key.encode(), "|".join(p[k] for k in sorted(p)).encode(), hashlib.sha1).hexdigest()
qs = urllib.parse.urlencode({"account": acct, "by": "name", "timestamp": ts,
                             "expires": "0", "preauth": mac})
print("https://%s/service/preauth?%s" % (host, qs))
print("", file=sys.stderr)
print("account : %s" % acct, file=sys.stderr)
print("expires : %s (5 min window)"
      % time.strftime("%H:%M:%S", time.localtime(int(ts) / 1000 + 300)), file=sys.stderr)
PY
}

cmd_code() {
  local rec="${1:-$RECOVERY_DEFAULT}"
  rec="$(fq "$rec")"
  local mid
  mid="$(sudo su - zimbra -c "zmmailbox -z -m $rec search -l 1 -t message 'in:inbox'" 2>/dev/null \
         | awk '/^1\./{print $2}' | tr -d '-')"
  if [ -z "$mid" ]; then echo "no messages in $rec inbox"; return 1; fi
  echo "newest message in $rec (id $mid):"
  sudo su - zimbra -c "zmmailbox -z -m $rec getMessage $mid" 2>/dev/null \
    | grep -iE "subject:|code:|expires by" | sed 's/^/  /'
  echo ""
  echo "note: mail on this box can lag several minutes, and each 'Send code'"
  echo "      overwrites the stored code -- send once, then wait for the mail."
}

cmd_delete() {
  need_user "${1:-}"
  local u; u="$(fq "$1")"
  zp "da $u" >/dev/null && echo "deleted $u"
}

case "${1:-}" in
  create)   shift; cmd_create "$@";;
  reset)    shift; cmd_reset "$@";;
  status)   shift; cmd_status "$@";;
  enrolled) shift; cmd_status "$@";;
  url)      shift; cmd_url "$@";;
  code)     shift; cmd_code "$@";;
  delete)   shift; cmd_delete "$@";;
  *)
    sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'
    exit 1;;
esac
