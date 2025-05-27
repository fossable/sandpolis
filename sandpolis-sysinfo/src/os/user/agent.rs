use super::UserData;
use crate::agent::layer::agent::Collector;
use crate::core::database::{Collection, Oid};
use crate::core::layer::sysinfo::os::user::UserData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_database::{DataView, GroupDatabase};
use sysinfo::Users;

pub struct UserCollector {
    data: DataView<UserData>,
    users: Users,
}

impl UserCollector {
    pub fn new(db: GroupDatabase) -> Self {
        Self {
            users: Users::new(),
            data: db.instance_view(),
        }
    }
}

impl Collector for UserCollector {
    fn refresh(&mut self) -> Result<()> {
        for user in Users::new_with_refreshed_list().list() {
            self.document(**user.id())?.mutate(|data| {
                data.uid = **user.id() as u64;
                data.gid = *user.group_id() as u64;
                data.username = Some(user.name().to_string());
                Ok(())
            });
        }
        Ok(())
    }
}
