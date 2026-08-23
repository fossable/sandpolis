# Probe test container

A throwaway fixture that brings up one server per protocol the probe layer
speaks, so every probe can be exercised against a real implementation. It is not
wired into `cargo test` — build and run it yourself.

Everything is built from the repo's pinned nixpkgs via `packages.nix`, so the
daemons track the rest of the tree. The image is large (Samba, ffmpeg, and
OpenIPMI all come along), so the first build pulls a lot from the binary cache.

## Build and run

The build context is the repository root (the image needs `nix/nixpkgs.nix`):

```shell
docker build -f sandpolis-probe/tests/Dockerfile -t sandpolis/test-probe .
docker run --rm --network host sandpolis/test-probe
```

Host networking is the easy path: the portmapper hands out ports of its own and
RTSP can fall back to UDP, neither of which survives a hand-written publish list.
To keep the container isolated instead, publish the ports explicitly:

```shell
docker run --rm \
  -p 22:22 -p 80:80 -p 111:111 -p 111:111/udp -p 161:161/udp -p 445:445 \
  -p 554:554 -p 623:623/udp -p 2049:2049 -p 2050:2050 -p 3389:3389 \
  -p 3493:3493 -p 5900:5900 sandpolis/test-probe
```

## What it serves

| Port | Protocol | Daemon | Probe |
|--------------|----------|-------------------------|--------------------|
| 5900 | VNC | TigerVNC `Xvnc` | `vnc.rs` (desktop) |
| 3389 | RDP | `freerdp-shadow-cli` | `rdp.rs` (desktop) |
| 2049, 2050 | NFSv3 | `unfsd` (mount on 2050) | `nfs.rs` |
| 111 | portmapper | `rpcbind` | NFS port resolution |
| 445 | SMB2/3 | Samba `smbd` | `smb.rs` |
| 554 | RTSP | `mediamtx` + `ffmpeg` | `rtsp/` |
| 3493 | NUT | `upsd` + `dummy-ups` | `ups.rs` |
| 22 | SSH | OpenSSH `sshd` | `ssh.rs` (shell) |
| 80 | HTTP | `lighttpd` | `http.rs` |
| 161/udp | SNMP v1/v2c | `snmpd` | `snmp.rs` |
| 623/udp | IPMI | OpenIPMI `ipmi_sim` | `ipmi.rs` |

`Xvnc` is both the X server for `:0` and its VNC server, and
`freerdp-shadow-cli` mirrors that same display — so VNC and RDP show the same
desktop, and an RDP-only run still listens on 5900. RDP negotiates TLS security
(not NLA/CredSSP) with a self-signed certificate, which matches the RDP probe's
first-cut behavior.

HTTP, SNMP, and IPMI are served for probes that are still stubs, so there is
something to develop against. Wake-on-LAN, ONVIF, Docker, and libvirt get no
service: none of them has a server worth running in a container.

## Configuration

Defaults, overridable with `-e` on `docker run`:

| Variable         | Default                                          | Used by                        |
|------------------|--------------------------------------------------|--------------------------------|
| `SERVICES`       | `vnc rdp nfs smb rtsp ups ssh http snmp ipmi`    | which daemons start            |
| `VNC_PASSWORD`   | `password`                                       | VNC auth (8 chars max)         |
| `RDP_PORT`       | `3389`                                           | RDP listen port                |
| `GEOMETRY`       | `1280x720`                                       | desktop size                   |
| `PROBE_USER`     | `probe`                                          | SMB, SSH, NUT, and IPMI login  |
| `PROBE_PASSWORD` | `password`                                       | the same accounts' password    |

`SERVICES` takes a space-separated subset, so a single probe can be worked on
without the rest: `-e SERVICES="nfs smb"`.

`freerdp-shadow-cli` mirrors the desktop without enforcing an account, so the RDP
username and password are only what the probe sends — any values work.

Both filesystem protocols export the same seeded tree, whose contents the manual
checks below assume:

```
/top.txt              "top level"
/docs/readme.txt      "hello probe"
/docs/empty.txt       (empty)
/docs/nested/deep.txt "deep"
```

## Verify from the host

```shell
rpcinfo -p 127.0.0.1                                        # portmapper + NFS/mount
showmount -e 127.0.0.1                                      # exports
smbclient -L //127.0.0.1 -U probe%password                   # SMB shares
ffprobe -rtsp_transport tcp rtsp://127.0.0.1:554/stream1     # RTSP stream
upsc test@127.0.0.1                                          # NUT variables
ssh probe@127.0.0.1                                          # SSH login
curl 127.0.0.1                                               # HTTP
snmpget -v2c -c public 127.0.0.1 sysLocation.0               # SNMP
ipmitool -I lanplus -H 127.0.0.1 -U probe -P password mc info # IPMI
vncviewer 127.0.0.1:5900                                     # VNC desktop
xfreerdp /v:127.0.0.1:3389 /cert:ignore                      # RDP desktop
```

## Point a probe at it

Probe devices are still hand-authored in the realm `.ron` (the register dialog
has no fields for them). Add these to the `probe.devices` list of your
`*.realm.ron`, assuming the server runs on the same host as the container:

```ron
probe: (
    devices: [
        // VNC-only device (the desktop panel prefers VNC when both are present).
        (
            name: "test-probe (VNC)",
            ip: "127.0.0.1",
            rtsp: None, wol: None, ssh: None, rdp: None,
            vnc: ( host: "127.0.0.1", port: 5900, password: "password" ),
            http: None, ipmi: None, snmp: None, onvif: None,
            docker: None, libvirt: None, ups: None, nfs: None, smb: None,
        ),
        // RDP-only device, so the panel exercises the RDP path.
        (
            name: "test-probe (RDP)",
            ip: "127.0.0.1",
            rtsp: None, wol: None, ssh: None,
            rdp: ( host: "127.0.0.1", port: 3389, username: "probe", password: "password", domain: None ),
            vnc: None,
            http: None, ipmi: None, snmp: None, onvif: None,
            docker: None, libvirt: None, ups: None, nfs: None, smb: None,
        ),
        // Everything else, on one device.
        (
            name: "test-probe",
            ip: "127.0.0.1",
            rtsp: ( port: 554, path: "stream1", username: None, password: None, transport: None ),
            wol: None,
            ssh: ( host: "127.0.0.1", port: 22, username: "probe", password: "password", private_key_path: None, fingerprint: None ),
            rdp: None, vnc: None,
            http: None, ipmi: None, snmp: None, onvif: None, docker: None, libvirt: None,
            ups: ( host: "127.0.0.1", port: 3493, ups_name: "test", username: "probe", password: "password" ),
            nfs: ( export: "/srv/nfs", portmapper_port: None, mount_port: 2050, nfs_port: 2049, uid: 0, gid: 0, privileged_port: false ),
            smb: ( share: "media", port: 445, username: "probe", password: "password", domain: "WORKGROUP" ),
        ),
    ],
),
```

Then start a server + client and open the layer that drives each protocol:
**Desktop** for VNC/RDP (expanding a probe node opens its stream), **Filesystem**
for NFS/SMB, **Shell** for SSH, and the **Probe** layer to see which protocols
each device advertises.
