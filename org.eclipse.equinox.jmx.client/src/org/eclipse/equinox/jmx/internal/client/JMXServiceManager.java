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
package org.eclipse.equinox.jmx.internal.client;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.jmx.client.*;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class JMXServiceManager implements IJMXServiceManager {

	private final List<JMXServiceDescriptor> jmxServiceDescriptors;

	protected final ListenerList jmxServerListeners;

	private static enum EventType {
		ADDED,
		REMOVED;
	}

	private class JMXServerNotifier implements ISafeRunnable {

		private EventType event;

		private IJMXServiceListener listener;

		private JMXServiceDescriptor jmxServiceDescriptor;

		public JMXServerNotifier() {
			// Intentionally left blank
		}

		public void handleException(Throwable exception) {
			final IStatus status =
				new Status(
						IStatus.ERROR,
						JMXClientPlugin.PI_NAMESPACE,
						120,
						"An exception occurred during JMX Server change notification.", exception);  //$NON-NLS-1$
			JMXClientPlugin.getDefault().getLog().log(status);
		}

		public void run() throws Exception {
			switch (event) {
				case ADDED:
					listener.jmxServiceAdded(jmxServiceDescriptor);
					break;
				case REMOVED:
					listener.jmxServiceRemoved(jmxServiceDescriptor);
					break;
			}
		}

		/**
		 * Notifies the listeners of the add/remove
		 *
		 * @param jmxServiceUrls the {@link JMXServiceDescriptor}s that changed
		 * @param event the type of change
		 */
		public void notify(List<JMXServiceDescriptor> jmxServiceUrls, EventType e) {
			this.event = e;

			Object[] copiedListeners = jmxServerListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				listener = (IJMXServiceListener)copiedListeners[i];
				for(JMXServiceDescriptor url : jmxServiceUrls) {
					jmxServiceDescriptor = url;
                    SafeRunner.run(this);
				}
			}

			listener = null;
			jmxServiceDescriptor = null;
		}
	}

	public JMXServiceManager() {
		jmxServiceDescriptors = Collections.synchronizedList(new ArrayList<JMXServiceDescriptor>());
		jmxServerListeners = new ListenerList();
	}

	public void addJMXService(JMXServiceDescriptor jmxService) {
		addJMXService(Collections.singletonList(jmxService));
	}

	public void addJMXServiceListener(IJMXServiceListener listener) {
		jmxServerListeners.add(listener);
	}

	public void addJMXService(List<JMXServiceDescriptor> jmxServices) {
		jmxServiceDescriptors.addAll(jmxServices);
		getJMXServerNotifier().notify(jmxServices, EventType.ADDED);
	}

	public List<JMXServiceDescriptor> getJMXServices() {
		return Collections.unmodifiableList(jmxServiceDescriptors);
	}

	public boolean isRegistered(JMXServiceDescriptor jmxService) {
		return jmxServiceDescriptors.contains(jmxService);
	}

	public void removeJMXService(JMXServiceDescriptor jmxService) {
		removeJMXService(Collections.singletonList(jmxService));
	}

	public void removeJMXServiceListner(IJMXServiceListener listener) {
		jmxServerListeners.remove(listener);
	}

	public void removeJMXService(List<JMXServiceDescriptor> jmxServices) {
		jmxServiceDescriptors.removeAll(jmxServices);
		getJMXServerNotifier().notify(jmxServices, EventType.REMOVED);
	}

	private JMXServerNotifier getJMXServerNotifier() {
		return new JMXServerNotifier();
	}
}
