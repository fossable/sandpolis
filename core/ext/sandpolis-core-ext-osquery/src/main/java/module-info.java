//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
open module org.s7s.core.integration.osquery {
	exports org.s7s.core.integration.osquery;

	requires org.s7s.core.foundation;
	requires org.slf4j;
	requires java.net.http;
	requires org.s7s.core.integration.pacman;
	requires com.fasterxml.jackson.databind;
}
