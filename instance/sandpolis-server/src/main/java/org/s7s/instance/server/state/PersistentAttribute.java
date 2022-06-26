//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.state;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.MongoDatabase;
import org.s7s.core.protocol.Stream.EV_STStreamData;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.EphemeralAttribute.EphemeralAttributeValue;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;

public class PersistentAttribute implements STAttribute {

	private STAttribute container;

	private Document mongoDocument;

	public PersistentAttribute(STAttribute container, MongoDatabase database) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addListener(Object listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Oid oid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STDocument parent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListener(Object listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<EphemeralAttributeValue> history() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get() {
		return mongoDocument.get("value");
	}

	@Override
	public void set(Object value) {
		mongoDocument.put("value", value);
	}

	@Override
	public void source(Supplier<?> source) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long timestamp() {
		return mongoDocument.getInteger("timestamp");
	}

	@Override
	public void merge(EV_STStreamData snapshot) {
		// TODO Auto-generated method stub

	}

	@Override
	public void replaceParent(STDocument parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public Stream<EV_STStreamData> snapshot(STSnapshotStruct config) {
		// TODO Auto-generated method stub
		return null;
	}

}
