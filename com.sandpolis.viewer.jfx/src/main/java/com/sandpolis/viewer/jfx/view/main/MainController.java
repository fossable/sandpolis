/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.viewer.jfx.view.main;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.pane.CarouselPane;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane;
import com.sandpolis.viewer.jfx.common.pane.ExtendPane.ExtendSide;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailCloseEvent;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.MenuCloseEvent;
import com.sandpolis.viewer.jfx.view.main.Events.MenuOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.ViewChangeEvent;
import com.sandpolis.viewer.jfx.view.main.list.HostListController;
import com.sandpolis.viewer.jfx.view.main.menu.VerticalMenuController;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class MainController extends AbstractController {

	// TODO to config
	private static final ExtendSide MENU_SIDE = ExtendSide.LEFT;
	private static final ExtendSide DETAIL_SIDE = ExtendSide.RIGHT;

	@FXML
	private CarouselPane carousel;
	@FXML
	private ExtendPane extend;
	@FXML
	private VerticalMenuController menuController;
	@FXML
	private HostListController hostListController;

	private Cache<Profile, Pane> detailCache;

	private StringProperty phase = new SimpleStringProperty();

	@FXML
	public void initialize() {
		register(menuController, hostListController);

		detailCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

		phase.addListener((p, o, n) -> {
			if (o == n)
				return;

			carousel.moveTo(n);
		});
	}

	@Subscribe
	public void switchView(ViewChangeEvent event) {
		phase.set(Objects.requireNonNull(event.get()));
	}

	@Subscribe
	public void showDetail(HostDetailOpenEvent event) throws ExecutionException {
		Pane pane = detailCache.get(event.get(), () -> {
			// TODO detail factory
			return new Pane();
		});

		extend.raise(pane, DETAIL_SIDE, 1000, 200);
	}

	@Subscribe
	public void hideDetail(HostDetailCloseEvent event) {
		extend.drop(DETAIL_SIDE);
	}

	@Subscribe
	public void hideMenu(MenuCloseEvent event) {
		extend.drop(MENU_SIDE);
	}

	@Subscribe
	public void showMenu(MenuOpenEvent event) {
		if (event.get() != extend.get(MENU_SIDE))
			extend.raise(event.get(), MENU_SIDE, 400, 130);
		else
			extend.drop(MENU_SIDE);
	}

}
