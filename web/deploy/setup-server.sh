#!/usr/bin/env bash
#
# One-time server bootstrap: nginx vhost + TLS certificate. Idempotent — safe to re-run.
#
#   sudo deploy/setup-server.sh
#
# Deliberately NOT part of deploy.sh. A deploy runs on every change and must not touch nginx
# or certificates; this runs once, needs root, and depends on DNS being live. Re-running it
# after certbot has been here is a no-op that just re-checks and reloads.
#
# Everything it does is scoped to this one vhost. It never restarts nginx (only reloads,
# which is graceful for the other sites on this box) and it never touches another site's file.

set -euo pipefail

DOMAIN="${VOLUNTARIAT_DOMAIN:-voluntariat.casavirupa.com}"
EMAIL="${VOLUNTARIAT_CERT_EMAIL:-oscar@artscontemplatives.com}"
PORT="${VOLUNTARIAT_PORT:-3003}"

SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/nginx.conf"
SITE=/etc/nginx/sites-available/voluntariat
LINK=/etc/nginx/sites-enabled/voluntariat

if [[ "$(id -u)" != 0 ]]; then
  echo "!!! run with sudo: sudo $0" >&2
  exit 1
fi

# certbot's nginx plugin writes the TLS block and the redirect in place, which is how the
# other vhosts on this box are already managed. Without the plugin, `certbot --nginx` fails.
if ! command -v nginx >/dev/null || ! command -v certbot >/dev/null; then
  echo "==> installing nginx / certbot"
  apt-get install -y nginx certbot python3-certbot-nginx
elif ! certbot plugins 2>/dev/null | grep -q nginx; then
  echo "==> installing python3-certbot-nginx"
  apt-get install -y python3-certbot-nginx
fi

# --- DNS ------------------------------------------------------------------------------
# Checked before anything is changed, because a missing record is the one failure that makes
# every later step look broken for the wrong reason: certbot's HTTP-01 challenge has to
# reach THIS host, and nginx serves both families.
echo "==> checking DNS for $DOMAIN"

# `dig` asks DNS directly. `getent` is the fallback (always present on Debian) but it goes
# through nsswitch, so an /etc/hosts entry would report a record that does not really exist —
# which is exactly the mistake this check is here to catch.
resolve() {   # resolve <type: A|AAAA> <name>
  local type="$1" name="$2"
  if command -v dig >/dev/null 2>&1; then
    dig +short +time=3 +tries=1 "$type" "$name" 2>/dev/null \
      | grep -vE '\.$' | head -n 1
  else
    local db=ahostsv4
    [[ "$type" == AAAA ]] && db=ahostsv6
    getent "$db" "$name" 2>/dev/null | awk 'NR==1 {print $1}'
  fi
}

a4="$(resolve A "$DOMAIN" || true)"
a6="$(resolve AAAA "$DOMAIN" || true)"
my4="$(curl -4 -fsS -m 5 https://ifconfig.me 2>/dev/null || true)"
my6="$(curl -6 -fsS -m 5 https://ifconfig.me 2>/dev/null || true)"

echo "    A    $DOMAIN -> ${a4:-<none>}   (this host: ${my4:-<no ipv4>})"
echo "    AAAA $DOMAIN -> ${a6:-<none>}   (this host: ${my6:-<no ipv6>})"

if [[ -z "$a4" && -z "$a6" ]]; then
  echo "!!! $DOMAIN does not resolve at all. Add the DNS records first — certbot cannot" >&2
  echo "!!! validate a name that does not point here." >&2
  exit 1
fi
if [[ -n "$my4" && -n "$a4" && "$a4" != "$my4" ]]; then
  echo "!!! the A record points at $a4, not at this host ($my4)." >&2
  exit 1
fi
# A warning, not a fatal: the site works over IPv4 alone, it is just not reachable for a
# v6-only client. certbot validates over whichever family answers.
if [[ -n "$my6" && -z "$a6" ]]; then
  echo "    !! no AAAA record, but this host has IPv6 ($my6) and nginx listens on [::]."
  echo "    !! add:  AAAA  voluntariat  ->  $my6"
fi

# --- vhost ----------------------------------------------------------------------------
# If certbot has already been here, the live file contains its TLS block and its redirect.
# Overwriting it with the HTTP-only source would silently take the site off HTTPS, so don't.
if [[ -f "$SITE" ]] && grep -q 'managed by Certbot' "$SITE"; then
  echo "==> $SITE already has certbot's TLS block — leaving it alone"
else
  echo "==> installing $SITE"
  # Keep a copy of whatever was there, so a failed `nginx -t` can be put back exactly.
  BACKUP=""
  if [[ -f "$SITE" ]]; then
    BACKUP="$SITE.bak-$(date +%Y%m%d-%H%M%S)"
    cp -p "$SITE" "$BACKUP"
  fi
  install -m 644 "$SRC" "$SITE"
  ln -sfn "$SITE" "$LINK"

  # `nginx -t` validates EVERY enabled site. If it fails, undo our change before exiting:
  # leaving a broken file in sites-enabled would block the next reload for all the neighbours.
  if ! nginx -t; then
    echo "!!! nginx -t failed — reverting" >&2
    rm -f "$LINK"
    if [[ -n "$BACKUP" ]]; then mv -f "$BACKUP" "$SITE"; else rm -f "$SITE"; fi
    nginx -t >/dev/null 2>&1 || echo "!!! nginx -t still fails: pre-existing problem, not ours" >&2
    exit 1
  fi
  systemctl reload nginx
fi

# --- certificate ----------------------------------------------------------------------
if [[ -d "/etc/letsencrypt/live/$DOMAIN" ]]; then
  echo "==> certificate for $DOMAIN already exists"
else
  echo "==> requesting certificate (certbot --nginx)"
  # --redirect makes certbot add the http->https redirect itself, the same shape the other
  # vhosts on this box have. certbot reloads nginx on success.
  certbot --nginx --redirect -d "$DOMAIN" \
    --agree-tos -m "$EMAIL" --no-eff-email --non-interactive
fi

# certbot mirrors the http block's listen directives when it adds the TLS one, but only if it
# recognises them — verify rather than assume, since a missing [::]:443 is invisible from an
# IPv4 client and the whole point here is that both families work.
if ! grep -qE 'listen[[:space:]]+\[::\]:443' "$SITE"; then
  echo "    !! no 'listen [::]:443 ssl' in $SITE — IPv6 clients cannot reach HTTPS."
  echo "    !! add it next to the 'listen 443 ssl;' line, then: sudo nginx -t && sudo systemctl reload nginx"
fi

# --- verify ---------------------------------------------------------------------------
echo "==> verifying"
nginx -t
grep -c "$DOMAIN" "$SITE" >/dev/null && echo "    vhost installed: $SITE"

if curl -fsS -o /dev/null -m 5 "http://127.0.0.1:$PORT/login"; then
  echo "    app is up on 127.0.0.1:$PORT"
else
  echo "    !! nothing serving on 127.0.0.1:$PORT — nginx will return 502 until the app runs."
  echo "    !! run deploy/deploy.sh, then: pm2 logs voluntariat --lines 30 --nostream"
fi

code="$(curl -o /dev/null -sw '%{http_code}' -m 10 "https://$DOMAIN/login" || echo 000)"
echo "    https://$DOMAIN/login -> HTTP $code"
case "$code" in
  200|3??) echo "done." ;;
  502)     echo "done — nginx is routing; the 502 is just the app not running yet." ;;
  404)     echo "!!! 404 means another vhost answered, not this one. Check: nginx -T | grep -n $DOMAIN" >&2 ;;
  000)     echo "!!! no TLS response at all. Check: systemctl status nginx, and the DNS above." >&2 ;;
  *)       echo "!!! unexpected. Check /var/log/nginx/voluntariat.error.log" >&2 ;;
esac
