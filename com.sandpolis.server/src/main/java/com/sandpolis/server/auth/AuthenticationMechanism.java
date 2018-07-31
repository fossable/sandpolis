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
package com.sandpolis.server.auth;

import com.sandpolis.server.store.group.Group;

/**
 * An {@link AuthenticationMechanism} can be added to a {@link Group} which will
 * then allow clients to authenticate with that group.<br>
 * <br>
 * 
 * Groups can have any number of {@code AuthenticationMechanism}s. A group with
 * no {@code AuthenticationMechanism}s will effectively become an insecure group
 * which allows any client to authenticate.
 * 
 * @author cilki
 * @since 5.0.0
 */
public abstract class AuthenticationMechanism {
}
