package com.sandpolis.server.vanilla.hibernate;

import java.util.function.Function;

import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.service.spi.ServiceException;

import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.state.StateObject;
import com.sandpolis.core.instance.store.provider.StoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;

public class HibernateStoreProviderFactory implements StoreProviderFactory {

	private EntityManagerFactory emf;

	public HibernateStoreProviderFactory(Configuration config) throws ServiceException {
		this.emf = config.buildSessionFactory();
	}

	@Override
	public <E extends StateObject> StoreProvider<E> supply(Class<E> type, Function<Document, E> constructor) {
		var em = emf.createEntityManager();
		var provider = em.find(HibernateStoreProvider.class, type.getName());
		if (provider == null) {
			provider = new HibernateStoreProvider<E>(type);
			try {
				em.getTransaction().begin();
				em.persist(provider);
				em.flush();
				em.getTransaction().commit();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		provider.em = em;
		provider.constructor = constructor;

		return provider;
	}

}
