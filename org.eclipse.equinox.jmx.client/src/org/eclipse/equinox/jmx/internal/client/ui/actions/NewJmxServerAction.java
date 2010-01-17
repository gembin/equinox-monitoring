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
package org.eclipse.equinox.jmx.internal.client.ui.actions;

import org.eclipse.equinox.jmx.client.JMXClientPlugin;
import org.eclipse.equinox.jmx.client.JMXServiceDescriptor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class NewJmxServerAction implements IViewActionDelegate {

	private IViewPart viewPart;

	public void init(IViewPart view) {
		viewPart = view;
	}

	public void run(IAction action) {
		final ConnectionSelectionDialog dialog =
			new ConnectionSelectionDialog(viewPart.getSite().getShell());

		if (dialog.open() != Window.OK) {
			return;
		}

		final JMXServiceDescriptor jmxServiceDescriptor =
			dialog.getJMXServiceDescriptor();

		JMXClientPlugin.getDefault().getJMXServiceManager().addJMXService(jmxServiceDescriptor);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Intentionally left blank
	}
}
