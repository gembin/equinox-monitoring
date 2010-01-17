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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.jmx.client.*;
import org.eclipse.equinox.jmx.common.JMXConstants;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class JMXTransportRegistry implements IJMXTransportRegistry {

	private final Map<String, IJMXConnectorProvider> transports;

	protected final ListenerList jmxTransportListeners;

	private static enum EventType {
		ADDED,
		REMOVED;
	}

	private class JMXTransportNotifier implements ISafeRunnable {

		private EventType event;

		private IJMXTransportListener listener;

		private IJMXConnectorProvider jmxConnectorProvider;

		public JMXTransportNotifier() {
			// Intentionally left blank
		}

		public void handleException(Throwable exception) {
			final IStatus status =
				new Status(
						IStatus.ERROR,
						JMXClientPlugin.PI_NAMESPACE,
						120,
						"An exception occurred during breakpoint change notification.", exception);  //$NON-NLS-1$
			JMXClientPlugin.getDefault().getLog().log(status);
		}

		public void run() throws Exception {
			switch (event) {
				case ADDED:
					listener.jmxTransportAdded(jmxConnectorProvider);
					break;
				case REMOVED:
					listener.jmxTransportRemoved(jmxConnectorProvider);
					break;
			}
		}

		/**
		 * Notifies the listeners of the add/remove
		 *
		 * @param jmxConnectorProviders
		 * 		the {@link IJMXConnectorProvider}s that changed
		 * @param event
		 * 			the type of change
		 */
		public void notify(List<IJMXConnectorProvider> jmxConnectorProviders, EventType e) {
			this.event = e;

			Object[] copiedListeners = jmxTransportListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				listener = (IJMXTransportListener)copiedListeners[i];
				for(IJMXConnectorProvider connector : jmxConnectorProviders) {
					jmxConnectorProvider = connector;
                    SafeRunner.run(this);
				}
			}

			listener = null;
			jmxConnectorProvider = null;
		}
	}

	public JMXTransportRegistry() {
		transports = Collections.synchronizedMap(new HashMap<String, IJMXConnectorProvider>());
		jmxTransportListeners = new ListenerList();
	}

	public void addJMXTransportListener(IJMXTransportListener listener) {
		jmxTransportListeners.add(listener);
	}

	public Set<String> getConnectorNames() {
		return Collections.unmodifiableSet(transports.keySet());
	}

	public IJMXConnectorProvider getConnectorProvider(String key) {
		final IJMXConnectorProvider connector = transports.get(key);

		// TODO: Add logging

		return connector;
	}

	public JMXConnector getJMXConnector(JMXServiceDescriptor serviceDescriptor) {
		try {
			final String transport = serviceDescriptor.getUrl().getProtocol();

			final IJMXConnectorProvider ctorp = getConnectorProvider(transport);

			// TODO: Use something other than JMXConstants.DEFAULT_DOMAIN
			final JMXServiceURL url = ctorp.getJMXServiceURL(
									serviceDescriptor.getUrl().getHost(),
									serviceDescriptor.getUrl().getPort(),
									serviceDescriptor.getUrl().getProtocol(),
									JMXConstants.DEFAULT_DOMAIN);

			Map<String, Object> environment = null;
			if(serviceDescriptor.getUsername() != null) {
				environment = new HashMap<String, Object>();
				String[] credentials = new String[] {
							serviceDescriptor.getUsername(),
							serviceDescriptor.getPassword()
				};
				environment.put(JMXConnector.CREDENTIALS, credentials);
			}

			return ctorp.newJMXConnector(url, environment);
		} catch (Exception e) {
			JMXClientPlugin.log(e);
			return null;
		}
	}

	public void removeJMXTransportListner(IJMXTransportListener listener) {
		jmxTransportListeners.remove(listener);
	}

	private JMXTransportNotifier getJMXTransportrNotifier() {
		return new JMXTransportNotifier();
	}

	public void loadTransportExtensions() {
		final IExtensionPoint point =
			RegistryFactory.getRegistry().getExtensionPoint(
					JMXClientPlugin.PI_NAMESPACE,
					JMXClientPlugin.PT_TRANSPORT);

		final IExtension[] types = point.getExtensions();

		for (int i = 0; i < types.length; i++) {
			loadTransportConfigurationElements(types[i].getConfigurationElements());
		}
	}

	private void loadTransportConfigurationElements(IConfigurationElement[] configElems) {
		for (int j = 0; j < configElems.length; j++) {
			final IConfigurationElement element = configElems[j];
			final String elementName = element.getName();
			String transport;
			if (elementName.equals(JMXClientPlugin.PT_TRANSPORT)
					&& null != element.getAttribute("class") //$NON-NLS-1$
					&& null != (transport = element.getAttribute("protocol"))) //$NON-NLS-1$
			{
				try {
					Object obj = element.createExecutableExtension("class"); //$NON-NLS-1$
					if (obj instanceof IJMXConnectorProvider) {
						transports.put(transport, (IJMXConnectorProvider)obj);
					}
				} catch (CoreException e) {
					JMXClientPlugin.log(e);
				}
			}
		}

		final List<IJMXConnectorProvider> transportsAdded = new ArrayList<IJMXConnectorProvider>();
		transportsAdded.addAll(transports.values());

		getJMXTransportrNotifier().notify(transportsAdded, EventType.ADDED);
	}
}
