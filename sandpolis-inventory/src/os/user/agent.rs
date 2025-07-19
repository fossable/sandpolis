use super::UserData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_database::{RealmDatabase, ResidentVec};
use sysinfo::{User, Users};
use tracing::trace;

pub struct UserCollector {
    data: ResidentVec<UserData>,
    users: Users,
}

impl UserCollector {
    pub fn new(db: RealmDatabase) -> Result<Self> {
        Ok(Self {
            users: Users::new(),
            data: db.resident_vec(())?,
        })
    }
}

impl Collector for UserCollector {
    async fn refresh(&mut self) -> Result<()> {
        self.users.refresh();
        trace!(info = ?self.users, "Polled user info");

        // Update or add any new users
        'next_user: for user in self.users.list() {
            for resident_user in self.data.iter() {
                if resident_user.read().uid == **user.id() {
                    resident_user.update(|u| {
                        u.gid = *user.group_id();
                        u.username = Some(user.name().to_string());
                        Ok(())
                    });
                    continue 'next_user;
                }
            }
            self.data.push(user.into())?;
        }

        // Remove old users
        // TODO
        Ok(())
    }
}

impl From<&User> for UserData {
    fn from(value: &User) -> Self {
        Self {
            uid: **value.id(),
            ..Default::default()
        }
    }
}
