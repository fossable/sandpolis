//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.state;

import java.util.function.Consumer;

import org.bson.Document;

import com.mongodb.client.MongoDatabase;
import org.s7s.core.instance.state.oid.Oid;
import org.s7s.core.instance.state.st.STAttribute;
import org.s7s.core.instance.state.st.STDocument;

public class PersistentDocument implements STDocument {

	private Document mongoDocument;

	public PersistentDocument(STDocument container, MongoDatabase database) {
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
	public STAttribute attribute(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int attributeCount() {
		return mongoDocument.size();
	}

	@Override
	public STDocument document(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int documentCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void forEachAttribute(Consumer<STAttribute> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void forEachDocument(Consumer<STDocument> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(STAttribute attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(String id, STAttribute attribute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(String id, STDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public void replaceParent(STDocument parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public STDocument getDocument(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public STAttribute getAttribute(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
