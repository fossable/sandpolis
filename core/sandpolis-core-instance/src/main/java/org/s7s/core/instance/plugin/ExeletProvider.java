//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.plugin;

import org.s7s.core.instance.exelet.Exelet;

/**
 * @author cilki
 * @since 5.0.0
 */
public interface ExeletProvider {

	/**
	 * Get the Exelet classes that the plugin contains.
	 *
	 * @return A list of Exelet classes
	 */
	public Class<? extends Exelet>[] getExelets();
}
