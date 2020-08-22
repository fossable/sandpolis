package com.sandpolis.core.server.hibernate;

import java.util.function.Function;

import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.service.spi.ServiceException;

import com.sandpolis.core.instance.state.Document;
import com.sandpolis.core.instance.state.Oid;
import com.sandpolis.core.instance.state.VirtObject;
import com.sandpolis.core.instance.store.provider.StoreProvider;
import com.sandpolis.core.instance.store.provider.StoreProviderFactory;

public class HibernateStoreProviderFactory implements StoreProviderFactory {

	private EntityManagerFactory emf;

	public HibernateStoreProviderFactory(Configuration config) throws ServiceException {
		this.emf = config.buildSessionFactory();
	}

	@Override
	public <E extends VirtObject> StoreProvider<E> supply(Class<E> type, Function<Document, E> constructor,
			Oid<?> oid) {
		var em = emf.createEntityManager();
		@SuppressWarnings("unchecked")
		HibernateStoreProvider<E> provider = em.find(HibernateStoreProvider.class, type.getName());
		if (provider == null) {
			provider = new HibernateStoreProvider<E>(type, oid);

			// Save the provider
			em.getTransaction().begin();
			em.persist(provider);
			em.flush();
			em.getTransaction().commit();
		}

		provider.em = em;
		provider.type = type;
		provider.constructor = constructor;

		return provider;
	}

}
