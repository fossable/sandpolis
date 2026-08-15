//! Android notifications, posted through the app's `Notifications` helper.
//!
//! Building a `NotificationCompat` entirely through JNI is a lot of fragile
//! call-by-signature code, so the Java side owns channel creation and posting
//! (see `sandpolis-mobile/android/.../Notifications.java`) and this is one
//! static call.
//!
//! The class is resolved through the `Context`'s own `ClassLoader` rather than
//! `Env::find_class`, which cannot see application classes when called from a
//! native thread — and [`super::deliver`] always calls in on one.

use super::warn_once;
use crate::notification::Severity;
use jni::objects::{JObject, JValue};
use jni::refs::LoaderContext;
use jni::strings::JNIStr;
use jni::vm::JavaVM;
use jni::{jni_sig, jni_str};

/// Binary name (dots, not slashes) of the app-side helper.
const HELPER: &JNIStr = jni_str!("org.sandpolis.mobile.Notifications");

pub fn show(title: &str, body: Option<&str>, severity: Severity) {
    if let Err(e) = try_show(title, body, severity) {
        // A build without the mobile app's Java sources has no helper class, so
        // this is a normal outcome rather than a bug.
        warn_once(format!("Android notifications are unavailable: {e}"));
    }
}

fn try_show(title: &str, body: Option<&str>, severity: Severity) -> jni::errors::Result<()> {
    let context = ndk_context::android_context();

    // SAFETY: `android_context` hands back the process's JavaVM and Activity
    // context, both valid for the life of the process.
    let vm = unsafe { JavaVM::from_raw(context.vm().cast()) };
    let context_obj = context.context();

    vm.attach_current_thread(|env| {
        // SAFETY: as above — this is the Activity `Context` the runtime owns.
        let activity = unsafe { JObject::from_raw(env, context_obj.cast()) };

        let title = env.new_string(title)?;
        let body = env.new_string(body.unwrap_or(""))?;

        let class = LoaderContext::FromObject(&activity).load_class(env, HELPER, true)?;

        env.call_static_method(
            &class,
            jni_str!("show"),
            jni_sig!((android.content.Context, java.lang.String, java.lang.String, int) -> void),
            &[
                JValue::Object(&activity),
                JValue::Object(&title),
                JValue::Object(&body),
                JValue::Int(severity.rank()),
            ],
        )?;

        Ok(())
    })
}
