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

import java.io.Serializable;
import java.net.MalformedURLException;
import javax.management.remote.JMXServiceURL;
import org.eclipse.equinox.jmx.common.JMXConstants;

/**
 * A {@code JMXServiceDescriptor} contains all of the elements necessary to
 * describe a connection to a JMX endpoint:<br>
 * <br>
 * <ol>
 * <li>A human readable name - Suitable for displaying via the UI</li>
 * <li>A {@link JMXServiceURL} - Used to connect to the MBeanServer</li>
 * <li>Credentials - A username and password credential pair</li>
 * </ol>
 * <br>
 * These elements form the necessary descriptive information to use in the JMX
 * Client UI.
 *
 * @see JMXServiceURL
 *
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public final class JMXServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

	/**
	 * Gets the {@code JMXServiceDescriptor} for the local MBean server
	 *
	 * @param username
	 *            the username or null if authentication is not necessary
	 * @param password
	 *            the password or null if authentication is not necessary
	 * @return
	 */
    public static JMXServiceDescriptor getLocalJMXServiceDescriptor(String username, String password) {
    	try {
    		final JMXServiceURL url =
    			new JMXServiceURL(
    					JMXConstants.DEFAULT_PROTOCOL,
    					"localhost", //$NON-NLS-1$
    					JMXConstants.DEFAULT_PORT_AS_INT,
    					"/" + JMXConstants.DEFAULT_DOMAIN); //$NON-NLS-1$

    		return new JMXServiceDescriptor("Local JMX Service", url, username, password);
    	} catch(MalformedURLException e) {
    		// This is a programming error
    		throw new AssertionError(e);
    	}
    }

    private final String name;

    private final String username;

    private final String password;

    /**
     * The {@link JMXServiceURL} of the JMX server
     */
    private final JMXServiceURL jmxServiceUrl;

	/**
	 * Constructor that creates a {@link JMXServiceURL} connection description
	 * using a preexisting {@code JMXServiceURL} instance.
	 *
	 * @param name the name for this connection descriptor
	 * @param url the {@code JMXServiceURL} to use
	 * @param username the username or null if authentication is not necessary
	 * @param password the password or null if authentication is not necessary
	 */
    public JMXServiceDescriptor(
    		String name,
    		JMXServiceURL url,
    		String username,
    		String password)
	{
        this.name = name;
        this.username = username;
        this.password = password;

        this.jmxServiceUrl = url;
    }

	/**
	 * Constructor that creates a {@link JMXServiceURL} connection description
	 * using a String URL as the primary means of connecting.
	 *
	 * @param name
	 *            the name for this connection descriptor
	 * @param url
	 *            a URL to the MBeanServer, similar to {@code
	 *            service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi}
	 * @param username
	 *            the username or null if authentication is not necessary
	 * @param password
	 *            the password or null if authentication is not necessary
	 * @throws MalformedURLException if the given string URL is not well formed
	 */
    public JMXServiceDescriptor(
    		String name,
    		String url,
    		String username,
    		String password) throws MalformedURLException
	{
    	this(name, new JMXServiceURL(url), username, password);
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public JMXServiceURL getUrl() {
        return jmxServiceUrl;
    }

    public String getUsername() {
        return username;
    }
}
