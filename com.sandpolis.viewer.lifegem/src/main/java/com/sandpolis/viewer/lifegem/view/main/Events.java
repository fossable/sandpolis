//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.viewer.lifegem.view.main;

import java.util.List;

import com.sandpolis.core.instance.event.Event;
import com.sandpolis.core.instance.event.ParameterizedEvent;
import com.sandpolis.core.profile.store.Profile;

import javafx.scene.layout.Region;

public final class Events {

	/**
	 * Close the main menu.
	 */
	public static class MenuCloseEvent extends Event {
	}

	/**
	 * Open the main menu with the given {@link Region}.
	 */
	public static class MenuOpenEvent extends ParameterizedEvent<Region> {
		public MenuOpenEvent(Region parameter) {
			super(parameter);
		}
	}

	/**
	 * Close the host detail.
	 */
	public static class HostDetailCloseEvent extends Event {
	}

	/**
	 * Open the host detail.
	 */
	public static class HostDetailOpenEvent extends ParameterizedEvent<Profile> {
		public HostDetailOpenEvent(Profile parameter) {
			super(parameter);
		}
	}

	/**
	 * Change the headers in the host list.
	 */
	public static class HostListHeaderChangeEvent extends ParameterizedEvent<List<?>> {
		public HostListHeaderChangeEvent(List<?> parameter) {
			super(parameter);
		}
	}

	/**
	 * Open the auxiliary detail.
	 */
	public static class AuxDetailOpenEvent extends ParameterizedEvent<String> {
		public AuxDetailOpenEvent(String parameter) {
			super(parameter);
		}
	}

	/**
	 * Close the auxiliary detail.
	 */
	public static class AuxDetailCloseEvent extends ParameterizedEvent<String> {
		public AuxDetailCloseEvent(String parameter) {
			super(parameter);
		}
	}

	/**
	 * Change the primary view.
	 */
	public static class ViewChangeEvent extends ParameterizedEvent<String> {
		public ViewChangeEvent(String parameter) {
			super(parameter);
		}
	}

}
