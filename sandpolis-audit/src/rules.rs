//! Detection rules that decide which audit records are worth keeping and
//! which of those warrant an immediate notification.
//!
//! [`evaluate`] is a pure function over the record type and outcome so that
//! realm-config-driven rules can replace the static table later without
//! touching the ingestion service.

use crate::AuditEventData;
use linux_audit_parser::{Body, Message, MessageType, Value};
use sandpolis_instance::InstanceId;
use sandpolis_instance::notification::Severity;

/// Audit prints this value (`(unsigned)-1`) for an unset id field.
const UNSET_ID: i64 = 4294967295;

/// How much of the raw log line is kept on a stored event.
const MAX_RAW_LEN: usize = 1024;

/// What the rule table decided about one audit record.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Verdict {
    pub severity: Severity,
    /// Whether the event warrants an immediate notification.
    pub notify: bool,
    pub label: &'static str,
}

const fn verdict(severity: Severity, notify: bool, label: &'static str) -> Verdict {
    Verdict {
        severity,
        notify,
        label,
    }
}

/// Decide whether a record of this type and outcome is interesting.
///
/// `None` means the record is not persisted at all.
pub fn evaluate(ty: MessageType, success: Option<bool>) -> Option<Verdict> {
    use MessageType as MT;
    let failed = success == Some(false);

    Some(match ty {
        MT::USER_LOGIN if failed => verdict(Severity::Warn, true, "Failed login attempt"),
        MT::USER_LOGIN => verdict(Severity::Info, false, "Login"),

        // Failed PAM stages accompany the failed login itself; keep them for
        // the audit trail but let USER_LOGIN carry the alert.
        MT::USER_AUTH | MT::USER_ACCT | MT::USER_ERR | MT::CRED_ACQ if failed => {
            verdict(Severity::Info, false, "Authentication failure")
        }

        MT::USER_START => verdict(Severity::Info, false, "Session start"),
        MT::USER_END => verdict(Severity::Info, false, "Session end"),

        MT::ADD_USER
        | MT::DEL_USER
        | MT::ADD_GROUP
        | MT::DEL_GROUP
        | MT::USER_MGMT
        | MT::GRP_MGMT
        | MT::CHGRP_ID
        | MT::CHUSER_ID
        | MT::USER_CHAUTHTOK
        | MT::GRP_CHAUTHTOK
        | MT::ACCT_LOCK
        | MT::ACCT_UNLOCK
        | MT::ROLE_ASSIGN
        | MT::ROLE_REMOVE => verdict(Severity::Warn, true, "Account modification"),

        MT::USER_CMD if failed => verdict(Severity::Warn, true, "Privileged command denied"),
        MT::USER_CMD => verdict(Severity::Info, false, "Privileged command"),

        MT::CONFIG_CHANGE | MT::MAC_CONFIG_CHANGE | MT::MAC_POLICY_LOAD | MT::FEATURE_CHANGE => {
            verdict(Severity::Info, true, "Audit configuration changed")
        }

        MT::DAEMON_ABORT => verdict(Severity::Warn, true, "auditd aborted"),

        MT::DAEMON_START | MT::DAEMON_END | MT::SYSTEM_BOOT | MT::SYSTEM_SHUTDOWN | MT::LOGIN => {
            verdict(Severity::Info, false, "System event")
        }

        // ANOM_* — kernel (1700s) and userspace (2100s) anomaly records.
        t if matches!(t.0, 1700..=1799 | 2100..=2199) => {
            verdict(Severity::Error, true, "Anomaly detected")
        }

        // RESP_* — an audit countermeasure fired.
        t if matches!(t.0, 2200..=2299) => {
            verdict(Severity::Error, true, "Audit countermeasure triggered")
        }

        _ => return None,
    })
}

fn value_string(value: &Value) -> Option<String> {
    match value {
        Value::Str(s, _) => Some(String::from_utf8_lossy(s).into_owned()),
        Value::Owned(s) => Some(String::from_utf8_lossy(s).into_owned()),
        Value::Number(n) => Some(n.to_string()),
        Value::Segments(parts) => {
            let bytes: Vec<u8> = parts.iter().flat_map(|p| p.iter().copied()).collect();
            Some(String::from_utf8_lossy(&bytes).into_owned())
        }
        _ => None,
    }
}

/// Audit writes a literal `?` where it has no value.
fn not_placeholder(s: String) -> Option<String> {
    if s == "?" { None } else { Some(s) }
}

/// Look up a field by name, first at the record's top level and then inside
/// the parsed `msg='…'` map that [`linux_audit_parser::Parser::split_msg`]
/// produces.
pub fn field(body: &Body, key: &str) -> Option<String> {
    if let Some(value) = body.get(key)
        && let Some(s) = value_string(value)
    {
        return not_placeholder(s);
    }
    if let Some(Value::Map(pairs)) = body.get("msg") {
        for (k, v) in pairs {
            if k.to_string() == key {
                return value_string(v).and_then(not_placeholder);
            }
        }
    }
    None
}

pub fn field_u32(body: &Body, key: &str) -> Option<u32> {
    let n: i64 = field(body, key)?.parse().ok()?;
    if n == UNSET_ID {
        return None;
    }
    u32::try_from(n).ok()
}

/// The record's outcome: `res=success|failed`, falling back to
/// `success=yes|no` and the numeric forms some record types use.
pub fn success(body: &Body) -> Option<bool> {
    let s = field(body, "res").or_else(|| field(body, "success"))?;
    match s.as_str() {
        "success" | "yes" | "1" => Some(true),
        "failed" | "no" | "0" => Some(false),
        _ => None,
    }
}

fn truncated(s: &str, max: usize) -> String {
    if s.len() <= max {
        return s.to_string();
    }
    let mut end = max;
    while !s.is_char_boundary(end) {
        end -= 1;
    }
    s[..end].to_string()
}

/// Evaluate one parsed audit record. `Some` when it matched a rule and should
/// be persisted; the verdict says whether it also warrants a notification.
pub fn to_event(
    msg: &Message,
    raw: &[u8],
    instance_id: InstanceId,
) -> Option<(AuditEventData, Verdict)> {
    let success = success(&msg.body);
    let verdict = evaluate(msg.ty, success)?;

    let event = AuditEventData {
        _instance_id: instance_id,
        timestamp: msg.id.timestamp,
        sequence: msg.id.sequence,
        record_type: msg.ty.to_string(),
        severity: verdict.severity,
        label: verdict.label.to_string(),
        success,
        uid: field_u32(&msg.body, "uid"),
        auid: field_u32(&msg.body, "auid"),
        session: field_u32(&msg.body, "ses"),
        pid: field_u32(&msg.body, "pid"),
        exe: field(&msg.body, "exe"),
        comm: field(&msg.body, "comm"),
        acct: field(&msg.body, "acct"),
        terminal: field(&msg.body, "terminal"),
        addr: field(&msg.body, "addr"),
        hostname: field(&msg.body, "hostname"),
        key: field(&msg.body, "key"),
        raw: truncated(String::from_utf8_lossy(raw).trim_end(), MAX_RAW_LEN),
        _id: Default::default(),
        _revision: Default::default(),
        _creation: Default::default(),
    };
    Some((event, verdict))
}

#[cfg(test)]
mod tests {
    use super::*;
    use anyhow::Result;
    use linux_audit_parser::Parser;
    use sandpolis_instance::InstanceId;

    /// The parser requires the trailing newline a real log line carries.
    fn parse(line: &str) -> Result<linux_audit_parser::Message<'static>> {
        Ok(Parser::default().parse(format!("{line}\n").as_bytes())?)
    }

    fn some_instance() -> InstanceId {
        sandpolis_instance::AgentId::random().into()
    }

    const FAILED_LOGIN: &str = "type=USER_LOGIN msg=audit(1723400000.123:456): pid=1234 uid=0 auid=1000 ses=3 msg='op=login acct=\"tyler\" exe=\"/usr/sbin/sshd\" hostname=203.0.113.7 addr=203.0.113.7 terminal=ssh res=failed'";

    #[test]
    fn failed_login_notifies() -> Result<()> {
        let msg = parse(FAILED_LOGIN)?;
        let (event, verdict) = to_event(&msg, FAILED_LOGIN.as_bytes(), some_instance()).unwrap();

        assert_eq!(verdict.severity, Severity::Warn);
        assert!(verdict.notify);
        assert_eq!(event.record_type, "USER_LOGIN");
        assert_eq!(event.timestamp, 1723400000123);
        assert_eq!(event.sequence, 456);
        assert_eq!(event.success, Some(false));
        assert_eq!(event.acct.as_deref(), Some("tyler"));
        assert_eq!(event.addr.as_deref(), Some("203.0.113.7"));
        assert_eq!(event.exe.as_deref(), Some("/usr/sbin/sshd"));
        assert_eq!(event.terminal.as_deref(), Some("ssh"));
        assert_eq!(event.uid, Some(0));
        assert_eq!(event.auid, Some(1000));
        Ok(())
    }

    #[test]
    fn successful_login_persists_quietly() -> Result<()> {
        let line = FAILED_LOGIN.replace("res=failed", "res=success");
        let msg = parse(&line)?;
        let (event, verdict) = to_event(&msg, line.as_bytes(), some_instance()).unwrap();

        assert_eq!(event.success, Some(true));
        assert!(!verdict.notify);
        Ok(())
    }

    #[test]
    fn account_modification_notifies() -> Result<()> {
        let line = "type=ADD_USER msg=audit(1723400001.000:457): pid=999 uid=0 auid=1000 ses=2 msg='op=adding user id=1001 exe=\"/usr/sbin/useradd\" hostname=? addr=? terminal=pts/0 res=success'";
        let msg = parse(line)?;
        let (event, verdict) = to_event(&msg, line.as_bytes(), some_instance()).unwrap();

        assert_eq!(verdict.severity, Severity::Warn);
        assert!(verdict.notify);
        // "?" placeholders must not come through as values
        assert_eq!(event.hostname, None);
        assert_eq!(event.addr, None);
        assert_eq!(event.terminal.as_deref(), Some("pts/0"));
        Ok(())
    }

    #[test]
    fn anomaly_is_an_error() -> Result<()> {
        let line = "type=ANOM_ABEND msg=audit(1723400002.500:458): auid=1000 uid=1000 ses=2 pid=4321 comm=\"myapp\" exe=\"/usr/bin/myapp\" sig=11 res=1";
        let msg = parse(line)?;
        let (event, verdict) = to_event(&msg, line.as_bytes(), some_instance()).unwrap();

        assert_eq!(verdict.severity, Severity::Error);
        assert!(verdict.notify);
        assert_eq!(event.comm.as_deref(), Some("myapp"));
        assert_eq!(event.success, Some(true));
        Ok(())
    }

    #[test]
    fn config_change_notifies_as_info() -> Result<()> {
        let line = "type=CONFIG_CHANGE msg=audit(1723400003.000:459): auid=1000 ses=2 op=add_rule key=\"exec\" list=4 res=1";
        let msg = parse(line)?;
        let (event, verdict) = to_event(&msg, line.as_bytes(), some_instance()).unwrap();

        assert_eq!(verdict.severity, Severity::Info);
        assert!(verdict.notify);
        assert_eq!(event.key.as_deref(), Some("exec"));
        Ok(())
    }

    #[test]
    fn uninteresting_records_are_dropped() -> Result<()> {
        let line = "type=CRED_REFR msg=audit(1723400004.000:460): pid=1 uid=0 auid=1000 ses=2 msg='op=PAM:setcred acct=\"root\" exe=\"/usr/bin/sudo\" res=success'";
        let msg = parse(line)?;
        assert!(to_event(&msg, line.as_bytes(), some_instance()).is_none());

        // Successful PAM stages are noise too
        let line = "type=USER_AUTH msg=audit(1723400005.000:461): pid=1 uid=0 auid=1000 ses=2 msg='op=PAM:authentication acct=\"root\" exe=\"/usr/bin/sudo\" res=success'";
        let msg = parse(line)?;
        assert!(to_event(&msg, line.as_bytes(), some_instance()).is_none());
        Ok(())
    }

    #[test]
    fn unset_id_fields_are_none() -> Result<()> {
        let line = "type=DAEMON_START msg=audit(1723400006.000:462): op=start ver=4.0 format=enriched auid=4294967295 pid=800 uid=0 ses=4294967295 res=success";
        let msg = parse(line)?;
        let (event, verdict) = to_event(&msg, line.as_bytes(), some_instance()).unwrap();

        assert!(!verdict.notify);
        assert_eq!(event.auid, None);
        assert_eq!(event.session, None);
        assert_eq!(event.uid, Some(0));
        Ok(())
    }
}
