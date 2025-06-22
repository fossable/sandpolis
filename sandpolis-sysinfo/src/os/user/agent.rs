use super::UserData;
use crate::os::user::UserDataKey;
use anyhow::Result;
use native_db::Database;
use sandpolis_agent::Collector;
use sandpolis_database::{DbTimestamp, RealmDatabase, ResidentVec};
use sysinfo::Users;

pub struct UserCollector {
    db: RealmDatabase,
    users: Users,
}

impl UserCollector {
    pub fn new(db: RealmDatabase) -> Self {
        Self {
            users: Users::new(),
            db,
        }
    }
}

impl Collector for UserCollector {
    async fn refresh(&mut self) -> Result<()> {
        self.users.refresh();
        trace!(info = ?self.users, "Polled user info");

        let users: ResidentVec<UserData> = self
            .db
            .query()
            .equal(UserDataKey::uid, **user.id() as u64)
            .latest();

        // Update or add any new users
        for user in self.users.list() {
            match resident_users.iter().find(|u| u.uid == **user.id()) {
                Some(u) => u.update(),
                None => {}
            }
            let r = self.db.r_transaction()?;
            let db_users: Vec<UserData> = r
                .scan()
                .secondary(UserDataKey::_timestamp)?
                .equal(DbTimestamp::Latest(0))?
                .and(
                    r.scan()
                        .secondary(UserDataKey::uid)?
                        .equal(**user.id() as u64)?,
                )
                .try_collect()?;

            self.document(**user.id())?.mutate(|data| {
                data.uid = **user.id() as u64;
                data.gid = *user.group_id() as u64;
                data.username = Some(user.name().to_string());
                Ok(())
            });
        }

        // Remove old users
        Ok(())
    }
}
