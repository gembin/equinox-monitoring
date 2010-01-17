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
package org.eclipse.equinox.jmx.internal.client.ui.jmxserversview;

import java.util.*;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import org.eclipse.equinox.jmx.client.*;
import org.eclipse.equinox.jmx.common.JMXConstants;
import org.eclipse.equinox.jmx.internal.client.ui.contributionsview.ContributionsViewPart;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class JmxServersView extends ViewPart {

	private ListViewer viewer;

	/**
	 *
	 * @author Stephen Evanchik (evanchsa@gmail.com)
	 *
	 */
	private static final class JMXServiceDescriptorLabelProvider extends LabelProvider {

		public JMXServiceDescriptorLabelProvider() {
			super();
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			final JMXServiceDescriptor jmxServiceDescriptor = (JMXServiceDescriptor)element;
			return jmxServiceDescriptor.getName() + " - " + jmxServiceDescriptor.getUrl().toString(); //$NON-NLS-1$
		}
	}

	/**
	 *
	 * @author Stephen Evanchik (evanchsa@gmail.com)
	 *
	 */
	private static final class JMXServiceDescriptorContentProvider
		implements IStructuredContentProvider, IJMXServiceListener
	{
		protected ListViewer concreteViewer;

		public JMXServiceDescriptorContentProvider() {
			// This space intentionally left blank
		}

		public Object[] getElements(Object element) {
			final IJMXServiceManager jmxServiceManager = JMXClientPlugin.getDefault().getJMXServiceManager();
			final List<JMXServiceDescriptor> jmxServiceDescriptors = jmxServiceManager.getJMXServices();

			return jmxServiceDescriptors.toArray();
		}

		public void dispose() {
			// What to do?
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if(viewer instanceof ListViewer) {
				concreteViewer = (ListViewer) viewer;
			}
		}

		public void jmxServiceAdded(final JMXServiceDescriptor jmxService) {
			concreteViewer.getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					concreteViewer.add(jmxService);
				}

			});
		}

		public void jmxServiceRemoved(final JMXServiceDescriptor jmxService) {
			concreteViewer.getControl().getDisplay().syncExec(new Runnable() {
				public void run() {
					concreteViewer.remove(jmxService);
				}
			});
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		final JMXServiceDescriptorContentProvider contentProvider =
			new JMXServiceDescriptorContentProvider();

		viewer = new ListViewer(parent, SWT.SINGLE);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new JMXServiceDescriptorLabelProvider());
		viewer.setInput(JMXClientPlugin.getDefault().getJMXServiceManager());

		JMXClientPlugin.getDefault().getJMXServiceManager().addJMXServiceListener(contentProvider);

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				final IStructuredSelection selection =
					(IStructuredSelection)event.getSelection();

				final JMXServiceDescriptor connection =
					(JMXServiceDescriptor)selection.getFirstElement();

				final JMXConnector connector =
					JMXClientPlugin.getDefault()
						.getJMXTransportRegistry().getJMXConnector(connection);

				ContributionsViewPart.reloadContributionsView(connection, connector);
			}
		});

		addLocalMBeanServer();
		initContextMenu();

		JMXClientPlugin.getDefault().getJMXTransportRegistry().loadTransportExtensions();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
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

    private void addLocalMBeanServer() {
		final MBeanServer mbs = findLocalMBeanServer();
		if(mbs == null) {
			return;
		}

		final JMXServiceDescriptor localJmxService =
			JMXServiceDescriptor.getLocalJMXServiceDescriptor(null, null);
		JMXClientPlugin.getDefault().getJMXServiceManager().addJMXService(localJmxService);
	}

	private MBeanServer findLocalMBeanServer() {
		final ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(null);
		final Iterator iter = mbeanServers.iterator();
		while (iter.hasNext()) {
			final MBeanServer mbeanServer = (MBeanServer) iter.next();
			if (mbeanServer.getDefaultDomain().equals(JMXConstants.DEFAULT_DOMAIN)) {
				return mbeanServer;
			}
		}
		return null;
	}
}
