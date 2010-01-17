/**
 * Copyright (c) 2009 Stephen Evanchik
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Stephen Evanchik - initial implementation
 */
package org.eclipse.equinox.jmx.client;

/**
 * A {@code IJMXTransportListener} is notified when a
 * {@link IJMXConnectorProvider} is added or removed from a
 * {@link IJMXTransportRegistry}.
 *
 * @see IJMXTransportRegistry
 *
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public interface IJMXTransportListener {

	/**
	 * Called when a {@link IJMXConnectorProvider} has been added to an object
	 * that this listener is observing.

	 * @param jmxConnectorProvider
	 * 		the {@code IJMXConnectorProvider} that was added
	 */
	public void jmxTransportAdded(IJMXConnectorProvider jmxConnectorProvider);

	/**
	 * Called when a {@link IJMXConnectorProvider} has been removed from an
	 * object that this listener is observing.
	 *
	 * @param jmxConnectorProvider
	 * 		the {@code IJMXConnectorProvider} that was removed
	 */
	public void jmxTransportRemoved(IJMXConnectorProvider jmxConnectorProvider);
}
