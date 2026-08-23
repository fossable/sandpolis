#!/usr/bin/env bash
#
# Brings up one server per protocol the probe layer speaks, all in one
# container. Each protocol is a `start_<name>` function; `SERVICES` selects which
# of them run, so a developer working on one probe can start just that daemon.
#
# Every daemon's configuration is generated here rather than taken from the
# package: the nix store paths are read-only, so nothing may rely on a package's
# built-in sysconfdir.
set -euo pipefail

SERVICES="${SERVICES:-vnc rdp nfs smb rtsp ups ssh http snmp ipmi}"
VNC_PASSWORD="${VNC_PASSWORD:-password}"
RDP_PORT="${RDP_PORT:-3389}"
GEOMETRY="${GEOMETRY:-1280x720}"
PROBE_USER="${PROBE_USER:-probe}"
PROBE_PASSWORD="${PROBE_PASSWORD:-password}"

# Pinned so the sample realm config can name them directly. The mount service
# gets its own port because `unfs3` serves it separately from NFS; both are still
# registered with the portmapper, so resolving them via GETPORT works too.
NFS_PORT=2049
MOUNT_PORT=2050
RTSP_PORT=554

export DISPLAY=:0
export HOME="${HOME:-/root}"
export NUT_CONFPATH=/etc/nut
export NUT_STATEPATH=/var/state/ups

log() {
  printf '[test-probe] %s\n' "$*" >&2
}

# Whether `$1` appears in the SERVICES list.
wants() {
  [[ " ${SERVICES} " == *" $1 "* ]]
}

# Block until something is listening on a TCP port, so daemons that register
# with or publish to another daemon don't race it.
wait_for_port() {
  local port="$1"
  for _ in $(seq 1 100); do
    if (exec 3<>"/dev/tcp/127.0.0.1/${port}") 2>/dev/null; then
      return 0
    fi
    sleep 0.1
  done
  log "timed out waiting for port ${port}"
  return 1
}

# The tree both filesystem protocols serve. Its contents are asserted on by hand
# (see README.md), so keep them stable.
seed_tree() {
  local root="$1"
  mkdir -p "$root/docs/nested"
  printf 'top level' >"$root/top.txt"
  printf 'hello probe' >"$root/docs/readme.txt"
  : >"$root/docs/empty.txt"
  printf 'deep' >"$root/docs/nested/deep.txt"
}

# `useradd` reads these from its own sysconfdir, which doesn't exist here.
useradd_defaults() {
  [[ -f /etc/login.defs ]] || : >/etc/login.defs
  mkdir -p /etc/default
  [[ -f /etc/default/useradd ]] || printf 'SHELL=/bin/sh\n' >/etc/default/useradd
}

# The unix account SMB and SSH authenticate against. NUT and IPMI reuse the same
# name and password, but only inside their own config files.
ensure_user() {
  if id "$PROBE_USER" >/dev/null 2>&1; then
    return
  fi
  useradd_defaults
  useradd --create-home --shell /bin/sh "$PROBE_USER"
  printf '%s:%s\n' "$PROBE_USER" "$PROBE_PASSWORD" | chpasswd
}

# Xvnc is both the X server for :0 and its VNC server on 5900, so an RDP-only
# run still starts it (and still listens on 5900).
start_x() {
  log "starting X/VNC on 5900"

  # Xvnc reads its password from an obfuscated file; `vncpasswd -f` produces one
  # from the plaintext on stdin.
  mkdir -p "$HOME/.vnc"
  vncpasswd -f <<<"$VNC_PASSWORD" >"$HOME/.vnc/passwd"
  chmod 600 "$HOME/.vnc/passwd"

  Xvnc :0 \
    -geometry "$GEOMETRY" \
    -depth 24 \
    -rfbport 5900 \
    -SecurityTypes VncAuth \
    -PasswordFile "$HOME/.vnc/passwd" \
    -AlwaysShared &

  # Wait for the display to accept connections before starting clients on it.
  for _ in $(seq 1 50); do
    xdpyinfo >/dev/null 2>&1 && break
    sleep 0.2
  done

  xsetroot -solid steelblue || true
  openbox &
  xterm -geometry 100x30+40+40 &
}

# freerdp-shadow-cli mirrors the same :0 over RDP. It generates a self-signed
# certificate on first run, which the probe accepts (it negotiates TLS security,
# not NLA). Adjust the flags here if your freerdp build differs.
start_rdp() {
  log "starting RDP on ${RDP_PORT}"
  freerdp-shadow-cli /port:"$RDP_PORT" &
}

start_nfs() {
  log "starting NFS on ${NFS_PORT} (mount ${MOUNT_PORT}, portmapper 111)"
  seed_tree /srv/nfs

  # A bare option list exports to every host; `unfsd` rejects a `*` wildcard.
  # `insecure` accepts clients whose source port is unprivileged, so the probe
  # works with `privileged_port` either way.
  cat >/etc/exports <<'EOF'
/srv/nfs (rw,no_root_squash,insecure)
EOF

  # rpcbind resolves its own port by service name, and drops privileges to a
  # dedicated account; neither exists in the base image.
  grep -q '^sunrpc' /etc/services 2>/dev/null || cat >>/etc/services <<'EOF'
sunrpc	111/tcp	portmapper
sunrpc	111/udp	portmapper
EOF
  if ! id rpc >/dev/null 2>&1; then
    useradd_defaults
    useradd -r -d /var/lib/rpcbind -s /bin/sh rpc ||
      log "could not create the rpc user; rpcbind will not start"
  fi
  mkdir -p /run/rpcbind /var/run/rpcbind /var/lib/rpcbind
  rpcbind -f -w &

  # Without a portmapper the probe can still reach the pinned ports, so a failed
  # rpcbind degrades the GETPORT path rather than the whole container.
  if wait_for_port 111; then
    unfsd -d -e /etc/exports -n "$NFS_PORT" -m "$MOUNT_PORT" &
  else
    log "rpcbind did not come up; serving NFS without portmapper registration"
    unfsd -d -p -e /etc/exports -n "$NFS_PORT" -m "$MOUNT_PORT" &
  fi
}

start_smb() {
  log "starting SMB on 445"
  ensure_user
  seed_tree /srv/smb
  chown -R "$PROBE_USER" /srv/smb
  mkdir -p /var/lib/samba/{private,lock,state,cache} /run/samba

  cat >/etc/smb.conf <<EOF
[global]
  workgroup = WORKGROUP
  server string = sandpolis test probe
  security = user
  smb ports = 445
  # No nmbd, so nothing needs the NetBIOS ports.
  disable netbios = yes
  server min protocol = SMB2
  map to guest = Never
  # Everything Samba writes has to land outside the read-only nix store.
  private dir = /var/lib/samba/private
  lock directory = /var/lib/samba/lock
  state directory = /var/lib/samba/state
  cache directory = /var/lib/samba/cache
  pid directory = /run/samba
  logging = file
  log file = /var/log/samba.log

[media]
  path = /srv/smb
  browseable = yes
  writable = yes
  valid users = $PROBE_USER
EOF

  # The `smb` client crate speaks raw NTLMSSP with Kerberos off, which is what
  # Samba's default (NTLMv2-only) accepts.
  printf '%s\n%s\n' "$PROBE_PASSWORD" "$PROBE_PASSWORD" |
    smbpasswd -s -c /etc/smb.conf -a "$PROBE_USER"

  smbd --foreground --no-process-group --debug-stdout -s /etc/smb.conf &
}

start_rtsp() {
  log "starting RTSP on ${RTSP_PORT}"

  # ffmpeg publishes a test pattern into mediamtx, which restreams it. Baseline
  # H.264 in yuv420p because the client decodes with openh264.
  cat >/etc/mediamtx.yml <<EOF
logLevel: info
rtspAddress: :${RTSP_PORT}
# Nothing here restreams over anything but RTSP.
rtmp: no
hls: no
webrtc: no
srt: no
api: no
metrics: no
pprof: no
paths:
  stream1:
    runOnInit: >
      ffmpeg -nostats -loglevel warning -re -f lavfi -i testsrc=size=1280x720:rate=30
      -pix_fmt yuv420p -c:v libx264 -profile:v baseline -preset ultrafast
      -tune zerolatency -g 30 -f rtsp rtsp://127.0.0.1:${RTSP_PORT}/stream1
    runOnInitRestart: yes
EOF

  mediamtx /etc/mediamtx.yml &
}

start_ups() {
  log "starting NUT on 3493"
  mkdir -p "$NUT_CONFPATH" "$NUT_STATEPATH"

  printf 'MODE=standalone\n' >"$NUT_CONFPATH/nut.conf"

  cat >"$NUT_CONFPATH/ups.conf" <<'EOF'
[test]
  driver = dummy-ups
  port = test.dev
  desc = "sandpolis test UPS"
EOF

  # dummy-ups replays this file in a loop, so the probe sees live variables.
  cat >"$NUT_CONFPATH/test.dev" <<'EOF'
device.mfr: Sandpolis
device.model: Test UPS
device.type: ups
ups.mfr: Sandpolis
ups.model: Test UPS
ups.status: OL
ups.load: 23
battery.charge: 100
battery.runtime: 3600
input.voltage: 120.0
output.voltage: 120.0
EOF

  cat >"$NUT_CONFPATH/upsd.conf" <<'EOF'
LISTEN 0.0.0.0 3493
EOF

  cat >"$NUT_CONFPATH/upsd.users" <<EOF
[$PROBE_USER]
  password = $PROBE_PASSWORD
  upsmon primary
  actions = SET
  instcmds = ALL
EOF
  chmod 600 "$NUT_CONFPATH/upsd.users" "$NUT_CONFPATH/upsd.conf"

  upsdrvctl -u root start
  upsd -u root -D &
}

start_ssh() {
  log "starting SSH on 22"
  ensure_user
  mkdir -p /etc/ssh /var/empty
  chmod 755 /var/empty

  # Privilege separation is mandatory and wants a dedicated account.
  if ! id sshd >/dev/null 2>&1; then
    useradd_defaults
    useradd -r -d /var/empty -s /bin/sh sshd
  fi

  [[ -f /etc/ssh/ssh_host_ed25519_key ]] ||
    ssh-keygen -q -t ed25519 -N '' -f /etc/ssh/ssh_host_ed25519_key
  [[ -f /etc/ssh/ssh_host_rsa_key ]] ||
    ssh-keygen -q -t rsa -b 3072 -N '' -f /etc/ssh/ssh_host_rsa_key

  cat >/etc/ssh/sshd_config <<'EOF'
Port 22
ListenAddress 0.0.0.0
HostKey /etc/ssh/ssh_host_ed25519_key
HostKey /etc/ssh/ssh_host_rsa_key
PasswordAuthentication yes
PermitRootLogin yes
UsePAM no
StrictModes no
PrintMotd no
PidFile /run/sshd.pid
EOF

  # sshd re-execs itself and refuses to start unless invoked by absolute path.
  "$(command -v sshd)" -D -e -f /etc/ssh/sshd_config &
}

start_http() {
  log "starting HTTP on 80"
  mkdir -p /srv/http

  cat >/srv/http/index.html <<'EOF'
<!doctype html>
<title>sandpolis test probe</title>
<h1>sandpolis test probe</h1>
EOF

  cat >/etc/lighttpd.conf <<'EOF'
server.document-root = "/srv/http"
server.port = 80
server.modules = ( "mod_dirlisting" )
dir-listing.activate = "enable"
index-file.names = ( "index.html" )
mimetype.assign = ( ".html" => "text/html", ".txt" => "text/plain" )
EOF

  lighttpd -D -f /etc/lighttpd.conf &
}

start_snmp() {
  log "starting SNMP on 161/udp"
  mkdir -p /var/lib/net-snmp

  cat >/etc/snmpd.conf <<'EOF'
rocommunity public
sysLocation sandpolis test container
sysContact probe@example.com
sysServices 72
EOF

  snmpd -f -Lo -C -c /etc/snmpd.conf udp:161 &
}

start_ipmi() {
  log "starting IPMI on 623/udp"
  mkdir -p /etc/ipmi /var/ipmi_sim

  cat >/etc/ipmi/lan.conf <<EOF
name "ipmisim1"

set_working_mc 0x20

  startlan 1
    addr 0.0.0.0 623
    priv_limit admin
    allowed_auths_callback none md2 md5 straight
    allowed_auths_user none md2 md5 straight
    allowed_auths_operator none md2 md5 straight
    allowed_auths_admin none md2 md5 straight
    guid a123456789abcdefa123456789abcdef
  endlan

  startnow false

  user 1 true  ""            "test"             user  10 none md2 md5 straight
  user 2 true  "$PROBE_USER" "$PROBE_PASSWORD"  admin 10 none md2 md5 straight
EOF

  cat >/etc/ipmi/sim.emu <<'EOF'
mc_setbmc 0x20
mc_add 0x20 0 no-device-sdrs 0x23 9 8 0x9f 0x1291 0xf02
sel_enable 0x20 1000 0x0a
mc_enable 0x20
EOF

  # -n: no stdio console. -p: don't persist simulated state.
  ipmi_sim -c /etc/ipmi/lan.conf -f /etc/ipmi/sim.emu -n -p -s /var/ipmi_sim &
}

if [[ -z "${SERVICES// /}" ]]; then
  log "SERVICES is empty; nothing to run"
  exit 1
fi

for service in $SERVICES; do
  case "$service" in
  vnc | rdp | nfs | smb | rtsp | ups | ssh | http | snmp | ipmi) ;;
  *)
    log "unknown service: ${service}"
    exit 1
    ;;
  esac
done

if wants vnc || wants rdp; then
  start_x
fi
if wants rdp; then start_rdp; fi
if wants nfs; then start_nfs; fi
if wants smb; then start_smb; fi
if wants rtsp; then start_rtsp; fi
if wants ups; then start_ups; fi
if wants ssh; then start_ssh; fi
if wants http; then start_http; fi
if wants snmp; then start_snmp; fi
if wants ipmi; then start_ipmi; fi

log "services up: ${SERVICES}"

# Exit (and let the container stop) as soon as any server dies.
wait -n
