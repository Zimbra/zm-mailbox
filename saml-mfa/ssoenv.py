"""
Config loader for the SSO 2FA test kit (ZCS-20575 / ZCS-20807).

Secrets and host names are NEVER hardcoded in these scripts: zm-mailbox is a public repository
(github.com/Zimbra/zm-mailbox), and a domain PreAuth key in particular can mint a login for any
account in its domain.

Resolution order, first hit wins:
  1. the process environment
  2. saml-mfa/local.env   (untracked -- see .gitignore; copy local.env.example to start)

Shell scripts read the same local.env, so there is one file to fill in.
"""
import os
import pathlib

ENV_FILE = pathlib.Path(__file__).resolve().parent / "local.env"


def _load():
    if not ENV_FILE.exists():
        return
    for raw in ENV_FILE.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, val = line.split("=", 1)
        val = val.strip().strip('"').strip("'")
        # environment wins over the file
        os.environ.setdefault(key.strip(), val)


_load()


def need(name):
    """Return a required setting, or exit with a message that says how to fix it."""
    val = os.environ.get(name)
    if not val:
        raise SystemExit(
            "Missing setting %s.\n"
            "  cp %s.example %s   # then fill it in\n"
            "  (or export %s=... in your shell)"
            % (name, ENV_FILE, ENV_FILE, name)
        )
    return val


def get(name, default=None):
    return os.environ.get(name, default)
