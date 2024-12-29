//! This layer allows various kinds of events to trigger user notifications.

pub enum AlertLevel {
    /// Indicates normal operations
    Normal,
    /// Indicates the user is "away" and thus unexpected user activity is considered suspicious
    Away,
}

pub enum DeviceClass {
    Workstation,
    Server,
    Embedded,
}
