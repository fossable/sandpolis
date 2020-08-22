package com.sandpolis.core.server.hibernate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import com.sandpolis.core.instance.store.StoreMetadata;

@Entity
public class HibernateStoreProviderMetadata implements StoreMetadata {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String db_id;

	@Column
	int initCount;

	@Override
	public int getInitCount() {
		return initCount;
	}

}
