use super::UserData;
use crate::agent::layer::agent::Collector;
use crate::core::database::{Collection, Oid};
use crate::core::layer::sysinfo::os::user::UserData;
use crate::os::user::UserDataKey;
use anyhow::Result;
use native_db::Database;
use sandpolis_agent::Collector;
use sandpolis_database::{DataView, DbTimestamp, GroupDatabase};
use sysinfo::Users;

pub struct UserCollector {
    db: Database<'static>,
    users: Users,
}

impl UserCollector {
    pub fn new(db: Database<'static>) -> Self {
        Self {
            users: Users::new(),
            db,
        }
    }
}

impl Collector for UserCollector {
    fn refresh(&mut self) -> Result<()> {
        self.users.refresh();

        for user in self.users.list() {
            let r = self.db.r_transaction()?;
            let db_users: Vec<UserData> = r
                .scan()
                .secondary(UserDataKey::_timestamp)?
                .range(DbTimestamp::Latest(0)..=DbTimestamp::Latest(0))?
                .and(
                    r.scan()
                        .secondary(UserDataKey::uid)?
                        .range(**user.id() as u64..=**user.id() as u64)?,
                )
                .try_collect()?;

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
