/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.common;

import static com.sandpolis.viewer.jfx.common.MicroSvgParser.readSvg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

class MicroSvgParserTest {

	@Test
	@DisplayName("Test a simple SVG")
	void readSvg_1() throws Exception {
		InputStream in = new ByteArrayInputStream(
				"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><g>\n        <path fill=\"none\" d=\"M0 0h24v24H0z\"/>\n        <path d=\"M3 4h18v2H3V4zm0 7h18v2H3v-2zm0 7h18v2H3v-2z\"/>\n    </g>\n</svg>"
						.getBytes());

		Group svg = readSvg(in, new SimpleDoubleProperty(64), new SimpleDoubleProperty(64),
				new SimpleObjectProperty<Paint>(null));

		assertNotNull(svg);
		assertEquals(64, svg.getBoundsInParent().getWidth());
		assertEquals(64, svg.getBoundsInParent().getHeight());
		assertEquals(2, svg.getChildren().size());
		assertEquals("M0 0h24v24H0z", ((SVGPath) svg.getChildren().get(0)).getContent());
		assertEquals("M3 4h18v2H3V4zm0 7h18v2H3v-2zm0 7h18v2H3v-2z", ((SVGPath) svg.getChildren().get(1)).getContent());
	}

}