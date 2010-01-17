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
package org.eclipse.equinox.jmx.internal.client.ui.actions;

import java.net.*;
import java.util.Iterator;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import org.eclipse.equinox.jmx.client.JMXClientPlugin;
import org.eclipse.equinox.jmx.client.JMXServiceDescriptor;
import org.eclipse.equinox.jmx.common.JMXConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * @since 1.0
 */
public class ConnectionSelectionDialog extends SelectionDialog {

	// sizing constants
	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
	private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

	private JMXServiceDescriptor connection;
	private JMXConnector connector;

	private Text nameText, hostText, portText, serviceNameText;
	private Combo transport;
	private Composite parentComposite;

	public ConnectionSelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	public JMXConnector getJMXConnector() {
		return connector;
	}

	public JMXServiceDescriptor getJMXServiceDescriptor() {
		return connection;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(ActionMessages.connection_selection_dialog_title);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);

		parentComposite = parent;

		final Font font = parent.getFont();
		composite.setFont(font);

		createMessageArea(composite);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		gridData.widthHint = SIZING_SELECTION_WIDGET_WIDTH;

		final Composite fieldComposite = new Composite(composite, SWT.NULL);
		fieldComposite.setLayout(new GridLayout(2, false));

		// Connection name: label
		Label label = new Label(fieldComposite, SWT.CENTER);
		label.setText(ActionMessages.connection_name);

		// Connection name: text entry
		nameText = new Text(fieldComposite, SWT.BORDER);
		nameText.setText(ActionMessages.new_jmx_connection);
		nameText.selectAll();

		gridData = new GridData();
		gridData.widthHint = convertWidthInCharsToPixels(25);
		nameText.setLayoutData(gridData);

		// host: label
		label = new Label(fieldComposite, SWT.CENTER);
		label.setText(ActionMessages.host);

		// host: text entry
		hostText = new Text(fieldComposite, SWT.BORDER);
		hostText.setText("localhost"); //$NON-NLS-1$

		gridData = new GridData();
		gridData.widthHint = convertWidthInCharsToPixels(25);
		hostText.setLayoutData(gridData);

		// port: label
		label = new Label(fieldComposite, SWT.CENTER);
		label.setText(ActionMessages.port);

		// port: text entry
		portText = new Text(fieldComposite, SWT.BORDER);
		portText.setTextLimit(5);
		portText.setText(JMXConstants.DEFAULT_PORT);

		gridData = new GridData();
		gridData.widthHint = convertWidthInCharsToPixels(7);
		portText.setLayoutData(gridData);

		// service name: label
		label = new Label(fieldComposite, SWT.CENTER);
		label.setText(ActionMessages.jmx_service_name);

		// service name: text entry
		serviceNameText = new Text(fieldComposite, SWT.BORDER);
		serviceNameText.setText(JMXConstants.DEFAULT_DOMAIN);

		gridData = new GridData();
		gridData.widthHint = convertWidthInCharsToPixels(25);
		serviceNameText.setLayoutData(gridData);

		// protocol selection: label
		label = new Label(fieldComposite, SWT.CENTER);
		label.setText(ActionMessages.jmx_protocol);

		// protocol selection: combo box
		transport = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.READ_ONLY);

		// add transport provided as extensions to supported list
		final Iterator iter = JMXClientPlugin.getDefault()
								.getJMXTransportRegistry().getConnectorNames()
								.iterator();
		while (iter.hasNext()) {
			transport.add((String) iter.next());
		}
		transport.select(0);

		return composite;
	}

	@Override
	protected void okPressed() {
		if (nameText.getText().equals("")) { //$NON-NLS-1$
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_name);
			return;
		}

		if (hostText.getText().equals("")) { //$NON-NLS-1$
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_host);
			return;
		}

		try {
			InetAddress.getByName(hostText.getText());
		} catch (UnknownHostException e) {
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_host);
			return;
		}

		if (portText.getText().equals("")) { //$NON-NLS-1$
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_port);
			return;
		}

		int port;
		try {
			port = Integer.parseInt(portText.getText());
			if (port < 1 || port > 0xffff) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_port);
			return;
		}

		if (serviceNameText.getText().equals("")) { //$NON-NLS-1$
			MessageDialog.openError(parentComposite.getShell(), ActionMessages.error_message, ActionMessages.invalid_jmx_service_name);
			return;
		}


		try {
			final JMXServiceURL url = new JMXServiceURL(
					transport.getText(),
					hostText.getText(),
					port,
					"/" + serviceNameText.getText()); //$NON-NLS-1$

			connection =
				new JMXServiceDescriptor(
						nameText.getText(),
						url,
						null,
						null);
		} catch (MalformedURLException e) {
			MessageDialog.openError(null, ActionMessages.error_message, e.getMessage());

			super.cancelPressed();

			return;
		}

		connector = JMXClientPlugin.getDefault()
						.getJMXTransportRegistry().getJMXConnector(connection);
		if(connector == null) {
			MessageDialog.openError(null, ActionMessages.error_message, ActionMessages.unable_create_connector);

			super.cancelPressed();

			return;
		}

		super.okPressed();
	}
}
