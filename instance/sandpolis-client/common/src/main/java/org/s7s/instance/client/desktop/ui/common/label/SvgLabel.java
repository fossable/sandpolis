//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common.label;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.s7s.instance.client.desktop.ui.common.SvgUtil;

import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.SimpleStyleableStringProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.scene.control.Label;
import javafx.scene.paint.Paint;

/**
 * A {@link Label} capable of displaying simple SVG icons.
 *
 * @author cilki
 * @since 5.0.2
 */
public class SvgLabel extends Label {

	private static final CssMetaData<SvgLabel, String> SVG_METADATA = new CssMetaData<>("-fx-svg",
			StyleConverter.getStringConverter()) {

		@Override
		public boolean isSettable(SvgLabel styleable) {
			return !styleable.svg.isBound();
		}

		@Override
		public StyleableProperty<String> getStyleableProperty(SvgLabel styleable) {
			return styleable.svg;
		}
	};

	private static final CssMetaData<SvgLabel, Paint> SVG_FILL_METADATA = new CssMetaData<>("-fx-svg-fill",
			StyleConverter.getPaintConverter()) {

		@Override
		public boolean isSettable(SvgLabel styleable) {
			return !styleable.svgFill.isBound();
		}

		@Override
		public StyleableProperty<Paint> getStyleableProperty(SvgLabel styleable) {
			return styleable.svgFill;
		}
	};

	private static final CssMetaData<SvgLabel, Number> SVG_WIDTH_METADATA = new CssMetaData<>("-fx-svg-width",
			StyleConverter.getSizeConverter()) {

		@Override
		public boolean isSettable(SvgLabel styleable) {
			return !styleable.svgWidth.isBound();
		}

		@Override
		public StyleableProperty<Number> getStyleableProperty(SvgLabel styleable) {
			return styleable.svgWidth;
		}
	};

	private static final CssMetaData<SvgLabel, Number> SVG_HEIGHT_METADATA = new CssMetaData<>("-fx-svg-height",
			StyleConverter.getSizeConverter()) {

		@Override
		public boolean isSettable(SvgLabel styleable) {
			return !styleable.svgHeight.isBound();
		}

		@Override
		public StyleableProperty<Number> getStyleableProperty(SvgLabel styleable) {
			return styleable.svgHeight;
		}
	};

	private static final List<CssMetaData<? extends Styleable, ?>> CONTROL_METADATA = Stream
			.concat(Label.getClassCssMetaData().stream(),
					Stream.of(SVG_METADATA, SVG_FILL_METADATA, SVG_WIDTH_METADATA, SVG_HEIGHT_METADATA))
			.collect(Collectors.toUnmodifiableList());

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return CONTROL_METADATA;
	}

	private final StyleableStringProperty svg = new SimpleStyleableStringProperty(SVG_METADATA, this, "svg");
	private final StyleableObjectProperty<Paint> svgFill = new SimpleStyleableObjectProperty<>(SVG_FILL_METADATA, this,
			"svgFill");
	private final StyleableDoubleProperty svgWidth = new SimpleStyleableDoubleProperty(SVG_WIDTH_METADATA, this,
			"svgWidth", 16.0);
	private final StyleableDoubleProperty svgHeight = new SimpleStyleableDoubleProperty(SVG_HEIGHT_METADATA, this,
			"svgHeight", 16.0);

	public StringProperty svgProperty() {
		return svg;
	}

	public SvgLabel() {
		getStyleClass().add("default-svg-label");

		svg.addListener((p, o, n) -> {
			setGraphic(SvgUtil.getSvg(n, svgWidth, svgHeight, svgFill));
		});
	}
}
