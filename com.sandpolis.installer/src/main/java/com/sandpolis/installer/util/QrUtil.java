/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.installer.util;

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
		try (var in = new ByteArrayInputStream(QrCode.encodeText(token, Ecc.HIGH).toSvgString(1).getBytes())) {
			return buildSvg(in, width, height, fill);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
