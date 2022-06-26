//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.installer.java.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrCode.Ecc;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

public class QrUtil {

	public static Group buildQr(String token, ReadOnlyDoubleProperty width, ReadOnlyDoubleProperty height, Paint fill) {
		/*
		 * try (var in = new ByteArrayInputStream(QrCode.encodeText(token,
		 * Ecc.HIGH).toSvgString(1).getBytes())) { return buildSvg(in, width, height,
		 * fill); } catch (Exception e) { throw new RuntimeException(e); }
		 */
		return null;
	}

	private static Group buildSvg(InputStream in, ReadOnlyDoubleProperty svgWidth, ReadOnlyDoubleProperty svgHeight,
			Paint fill) throws Exception {

		NodeList paths = (NodeList) XPathFactory.newDefaultInstance().newXPath().evaluate("/svg/path",
				DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), XPathConstants.NODESET);

		Group group = new Group();
		for (int i = 0; i < paths.getLength(); i++) {
			NamedNodeMap attributes = paths.item(i).getAttributes();

			SVGPath path = new SVGPath();
			path.setFill(fill);

			var d = attributes.getNamedItem("d");
			if (d != null)
				path.setContent(d.getTextContent());

			group.getChildren().add(path);
		}

		group.scaleXProperty().bind(Bindings.divide(svgWidth, group.getBoundsInParent().getWidth()));
		group.scaleYProperty().bind(Bindings.divide(svgHeight, group.getBoundsInParent().getHeight()));

		return group;
	}
}
