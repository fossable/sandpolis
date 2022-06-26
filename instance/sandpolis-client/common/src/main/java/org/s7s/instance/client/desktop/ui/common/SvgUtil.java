//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.client.desktop.ui.common;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/**
 * {@link SvgUtil} implements a small parser for extremely simple SVG files.
 *
 * @since 5.0.2
 */
public final class SvgUtil {

	private static final Logger log = LoggerFactory.getLogger(SvgUtil.class);

	/**
	 * Load an SVG image from the classpath.
	 *
	 * @param url       The SVG classpath url
	 * @param svgWidth  The width property
	 * @param svgHeight The height property
	 * @param svgFill   The fill property
	 * @return The SVG or {@code null} if it failed to load
	 */
	public static Node getSvg(String url, ReadOnlyDoubleProperty svgWidth, ReadOnlyDoubleProperty svgHeight,
			ObjectProperty<Paint> svgFill) {
		try (InputStream in = SvgUtil.class.getResourceAsStream(url)) {
			return readSvg(in, svgWidth, svgHeight, svgFill);
		} catch (Exception e) {
			log.warn("Failed to load svg: {}", url);
			return null;
		}
	}

	public static Node getSvg(String url, double svgWidth, double svgHeight, ObjectProperty<Paint> svgFill) {
		try (InputStream in = SvgUtil.class.getResourceAsStream(url)) {
			return readSvg(in, new SimpleDoubleProperty(svgWidth), new SimpleDoubleProperty(svgHeight), svgFill);
		} catch (Exception e) {
			log.warn("Failed to load svg: {}", url);
			return null;
		}
	}

	/**
	 * Parse an SVG from the given input stream.
	 *
	 * @param in        The SVG input stream
	 * @param svgWidth  The width property
	 * @param svgHeight The height property
	 * @param svgFill   The fill property
	 * @return The SVG image
	 * @throws Exception
	 */
	static Group readSvg(InputStream in, ReadOnlyDoubleProperty svgWidth, ReadOnlyDoubleProperty svgHeight,
			ObjectProperty<Paint> svgFill) throws Exception {

		NodeList paths = (NodeList) XPathFactory.newDefaultInstance().newXPath().evaluate("/svg/g/path",
				DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), XPathConstants.NODESET);

		Group group = new Group();
		for (int i = 0; i < paths.getLength(); i++) {
			NamedNodeMap attributes = paths.item(i).getAttributes();

			SVGPath path = new SVGPath();

			var fill = attributes.getNamedItem("fill");
			if (fill != null)
				path.setFill(fill.getTextContent().equals("none") ? Color.TRANSPARENT
						: Paint.valueOf(fill.getTextContent()));
			else if (svgFill.get() != null)
				path.setFill(svgFill.get());

			var fillRule = attributes.getNamedItem("fill-rule");
			if (fillRule != null)
				switch (fillRule.getTextContent()) {
				case "evenodd":
					path.setFillRule(FillRule.EVEN_ODD);
					break;
				case "nonzero":
					path.setFillRule(FillRule.NON_ZERO);
					break;
				default:
					log.warn("Unknown fill-rule: " + fillRule.getTextContent());
					break;
				}

			var d = attributes.getNamedItem("d");
			if (d != null)
				path.setContent(d.getTextContent());

			var style = attributes.getNamedItem("style");
			if (style != null)
				path.setStyle(style.getTextContent());

			group.getChildren().add(path);
		}

		group.scaleXProperty().bind(Bindings.divide(svgWidth, group.getBoundsInParent().getWidth()));
		group.scaleYProperty().bind(Bindings.divide(svgHeight, group.getBoundsInParent().getHeight()));

		return group;
	}

	private SvgUtil() {
	}
}
