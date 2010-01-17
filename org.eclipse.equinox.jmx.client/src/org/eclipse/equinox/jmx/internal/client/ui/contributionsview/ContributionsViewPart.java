/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.jmx.internal.client.ui.contributionsview;

import java.io.IOException;
import javax.management.*;
import javax.management.remote.JMXConnector;
import org.eclipse.equinox.jmx.client.JMXClientPlugin;
import org.eclipse.equinox.jmx.client.JMXServiceDescriptor;
import org.eclipse.equinox.jmx.internal.client.MBeanServerProxy;
import org.eclipse.equinox.jmx.internal.client.ui.ClientUI;
import org.eclipse.equinox.jmx.internal.client.ui.actions.ActionMessages;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

/**
 * @since 1.0
 */
public class ContributionsViewPart extends ViewPart {

	private Composite viewParent;

	protected TreeViewer viewer;
	protected ContributionContentProvider contentProvider;
	protected ContributionLabelProvider labelProvider;

	protected JMXConnector connector;
	protected JMXServiceDescriptor serviceDescriptor;

	private ConnectionState connectionState;

	protected MBeanServerProxy serverProxy;

	private static enum ConnectionState {
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		DISCONNECTED,
		ERROR;
	}

	public static void reloadContributionsView(JMXServiceDescriptor jmxServiceDescriptor, JMXConnector jmxConnector) {
		try {
			final ContributionsViewPart contributionsView =
				(ContributionsViewPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ClientUI.VIEWID_CONTRIBUTIONS);
			if (contributionsView != null) {
				contributionsView.disconnect();
				contributionsView.connect(jmxServiceDescriptor, jmxConnector);
			}
		} catch (PartInitException e) {
			JMXClientPlugin.log(e);
		}
	}

	public void connect(JMXServiceDescriptor jmxServiceDescriptor, JMXConnector jmxConnector) {
		this.connector = jmxConnector;
		this.serviceDescriptor = jmxServiceDescriptor;

		try {
			doConnect();
			contentProvider.setServerContributionProxy(serverProxy);
			updateViewer();
		} catch(Exception e) {
			JMXClientPlugin.logError(e);
		}
	}

	@Deprecated
	public void connect(MBeanServerProxy proxy) {
		this.serverProxy = proxy;
		try {
			contentProvider.setServerContributionProxy(proxy);
			updateViewer();
		} catch (Exception e) {
			JMXClientPlugin.logError(e);
		}
	}

	public void connectionClosed(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), ActionMessages.info_message, message);
	}

	@Override
	public void createPartControl(Composite parent) {
		viewParent = parent;

		viewer = new TreeViewer(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setUseHashlookup(true);
		viewer.setSorter(new ViewerSorter());

		connectionState = ConnectionState.DISCONNECTED;

		contentProvider = new ContributionContentProvider(viewer, null);
		viewer.setContentProvider(contentProvider);

		labelProvider = new ContributionLabelProvider();
		viewer.setLabelProvider(labelProvider);

		getSite().setSelectionProvider(viewer);

		initContextMenu();
	}

	public void disconnect() {
		// check if we must close a remote connection
		if (connectionState != ConnectionState.CONNECTED) {
			return;
		}

		connectionState = ConnectionState.DISCONNECTING;

		try {
			connector.close();
		} catch (IOException e) {
			MessageDialog.openError(viewParent.getShell(), ActionMessages.error_message, e.getMessage());
			JMXClientPlugin.log(e);
		}

		// These three statements should execute even if there is an Exception
		connector = null;
		connectionState = ConnectionState.DISCONNECTED;
		serverProxy = null;

		try {
			updateViewer();
		} catch(Exception e) {
			MessageDialog.openError(viewParent.getShell(), ActionMessages.error_message, e.getMessage());
			JMXClientPlugin.log(e);
		}

	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public MBeanServerProxy getMBeanServerProxy() {
		return serverProxy;
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	protected ConnectionState getConnectionState() {
		return connectionState;
	}

	protected Composite getViewParent() {
		return viewParent;
	}

    protected void initContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                menuMgr.add(new Separator());
                menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });

        final Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    protected void setConnectionState(ConnectionState newConnectionState) {
    	connectionState = newConnectionState;
    }

    private void doConnect() {
		connectionState = ConnectionState.CONNECTING;

		connector.addConnectionNotificationListener(new NotificationListener() {
			public void handleNotification(Notification notification, Object handback) {
				if (getConnectionState() == ConnectionState.DISCONNECTING
					|| getConnectionState() == ConnectionState.CONNECTING)
				{
					// receiving callback from user action
					return;
				}

				setConnectionState(ConnectionState.DISCONNECTED);

				connector = null;
				serverProxy = null;

				getViewParent().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(getViewParent().getShell(), ActionMessages.info_message, ActionMessages.server_closed_connection);
					}
				});
			}
		}, null, null);

		try {
			connector.connect();
			serverProxy = new MBeanServerProxy(connector.getMBeanServerConnection());

			connectionState = ConnectionState.CONNECTED;
		} catch(IOException e) {
			// TODO: Evaluate whether to use ConnectionState.ERROR
			connectionState = ConnectionState.DISCONNECTED;

			connector = null;
			serverProxy = null;

			JMXClientPlugin.log(e);
		}
    }

	private void updateViewer()
		throws InstanceNotFoundException, MBeanException, ReflectionException, IOException
	{
		if (serverProxy == null) {
			viewer.setSelection(null);
			viewer.setInput(null);
			return;
		}

		viewer.setInput(serverProxy.getRootContribution());
	}
}