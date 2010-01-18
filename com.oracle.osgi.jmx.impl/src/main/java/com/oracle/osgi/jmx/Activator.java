/*
 * Copyright 2008 Oracle Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.oracle.osgi.jmx;

import static org.osgi.jmx.Constants.BUNDLE_STATE;
import static org.osgi.jmx.Constants.CM_SERVICE;
import static org.osgi.jmx.Constants.FRAMEWORK;
import static org.osgi.jmx.Constants.PACKAGE_STATE;
import static org.osgi.jmx.Constants.PA_SERVICE;
import static org.osgi.jmx.Constants.PS_SERVICE;
import static org.osgi.jmx.Constants.SERVICE_STATE;
import static org.osgi.jmx.Constants.UA_SERVICE;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.compendium.ConfigAdminManagerMBean;
import org.osgi.jmx.compendium.PermissionManagerMBean;
import org.osgi.jmx.compendium.ProvisioningMBean;
import org.osgi.jmx.compendium.UserManagerMBean;
import org.osgi.jmx.core.BundleStateMBean;
import org.osgi.jmx.core.FrameworkMBean;
import org.osgi.jmx.core.PackageStateMBean;
import org.osgi.jmx.core.ServiceStateMBean;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.oracle.osgi.jmx.compendium.ConfigAdminManager;
import com.oracle.osgi.jmx.compendium.PermissionManager;
import com.oracle.osgi.jmx.compendium.Provisioning;
import com.oracle.osgi.jmx.compendium.UserManager;
import com.oracle.osgi.jmx.core.BundleState;
import com.oracle.osgi.jmx.core.Framework;
import com.oracle.osgi.jmx.core.PackageState;
import com.oracle.osgi.jmx.core.ServiceState;

/**
 * @author Hal Hildebrand Date: Nov 23, 2008 Time: 5:45:55 PM
 * 
 *         The bundle activator which starts and stops the system, as well as
 *         providing the service tracker which listens for the MBeanServer. When
 *         the MBeanServer is found, the MBeans representing the OSGi services
 *         will be installed.
 * 
 */
public class Activator implements BundleActivator {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        frameworkName = new ObjectName(FRAMEWORK);
        bundlesStateName = new ObjectName(BUNDLE_STATE);
        serviceStateName = new ObjectName(SERVICE_STATE);
        packageStateName = new ObjectName(PACKAGE_STATE);

        mbeanServiceTracker = new ServiceTracker(
                                                 bundleContext,
                                                 MBeanServer.class.getCanonicalName(),
                                                 new MBeanServiceTracker());
        log.debug("Awaiting MBeanServer service registration");
        mbeanServiceTracker.open();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext arg0) throws Exception {
        mbeanServiceTracker.close();
        deregisterServices();
        mbeanServer = null;
    }

    /**
     */
    protected synchronized void deregisterServices() {
        if (!servicesRegistered.get()) {
            return;
        }
        log.debug("Deregistering framework with MBeanServer: " + mbeanServer);
        try {
            mbeanServer.unregisterMBean(frameworkName);
        } catch (InstanceNotFoundException e) {
            log.trace("FrameworkMBean not found on deregistration", e);
        } catch (MBeanRegistrationException e) {
            log.debug("FrameworkMBean deregistration problem", e);
        }
        framework = null;

        try {
            mbeanServer.unregisterMBean(bundlesStateName);
        } catch (InstanceNotFoundException e) {
            log.trace("OSGi BundleStateMBean not found on deregistration", e);
        } catch (MBeanRegistrationException e) {
            log.debug("OSGi BundleStateMBean deregistration problem", e);
        }
        bundleState = null;

        log.debug("Deregistering services monitor with MBeanServer: "
                  + mbeanServer);
        try {
            mbeanServer.unregisterMBean(serviceStateName);
        } catch (InstanceNotFoundException e) {
            log.trace("OSGi ServiceStateMBean not found on deregistration", e);
        } catch (MBeanRegistrationException e) {
            log.debug("OSGi ServiceStateMBean deregistration problem", e);
        }
        serviceState = null;

        log.debug("Deregistering packages monitor with MBeanServer: "
                  + mbeanServer);
        try {
            mbeanServer.unregisterMBean(packageStateName);
        } catch (InstanceNotFoundException e) {
            log.trace("OSGi PackageStateMBean not found on deregistration", e);
        } catch (MBeanRegistrationException e) {
            log.debug("OSGi PackageStateMBean deregistration problem", e);
        }
        packageState = null;
        configAdminTracker.close();
        configAdminTracker = null;
        permissionAdminTracker.close();
        permissionAdminTracker = null;
        provisioningServiceTracker.close();
        provisioningServiceTracker = null;
        userAdminTracker.close();
        userAdminTracker = null;

        servicesRegistered.set(false);
    }

    /**
     */
    protected synchronized void registerServices() {
        PackageAdmin admin = (PackageAdmin) bundleContext.getService(bundleContext.getServiceReference(PackageAdmin.class.getCanonicalName()));
        StartLevel sl = (StartLevel) bundleContext.getService(bundleContext.getServiceReference(StartLevel.class.getCanonicalName()));
        try {
            framework = new StandardMBean(new Framework(bundleContext, admin,
                                                        sl),
                                          FrameworkMBean.class);
        } catch (NotCompliantMBeanException e) {
            log.fatal("Unable to create StandardMBean for Framework", e);
            return;
        }
        try {
            bundleState = new StandardMBean(new BundleState(bundleContext, sl,
                                                            admin),
                                            BundleStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            log.fatal("Unable to create StandardMBean for BundleState", e);
            return;
        }
        try {
            serviceState = new StandardMBean(new ServiceState(bundleContext),
                                             ServiceStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            log.fatal("Unable to create StandardMBean for ServiceState", e);
            return;
        }
        try {
            packageState = new StandardMBean(new PackageState(bundleContext,
                                                              admin),
                                             PackageStateMBean.class);
        } catch (NotCompliantMBeanException e) {
            log.fatal("Unable to create StandardMBean for PackageState", e);
            return;
        }

        log.debug("Registering Framework with MBeanServer: "
                           + mbeanServer + " with name: " + frameworkName);
        try {
            mbeanServer.registerMBean(framework, frameworkName);
        } catch (InstanceAlreadyExistsException e) {
            log.error("Cannot register OSGi framework MBean", e);
        } catch (MBeanRegistrationException e) {
            log.error("Cannot register OSGi framework MBean", e);
        } catch (NotCompliantMBeanException e) {
            log.error("Cannot register OSGi framework MBean", e);
        }

        log.debug("Registering bundle state monitor with MBeanServer: "
                           + mbeanServer + " with name: " + bundlesStateName);
        try {
            mbeanServer.registerMBean(bundleState, bundlesStateName);
        } catch (InstanceAlreadyExistsException e) {
            log.error("Cannot register OSGi BundleStateMBean", e);
        } catch (MBeanRegistrationException e) {
            log.error("Cannot register OSGi BundleStateMBean", e);
        } catch (NotCompliantMBeanException e) {
            log.error("Cannot register OSGi BundleStateMBean", e);
        }

        log.debug("Registering services monitor with MBeanServer: "
                           + mbeanServer + " with name: " + serviceStateName);
        try {
            mbeanServer.registerMBean(serviceState, serviceStateName);
        } catch (InstanceAlreadyExistsException e) {
            log.error("Cannot register OSGi ServiceStateMBean", e);
        } catch (MBeanRegistrationException e) {
            log.error("Cannot register OSGi ServiceStateMBean", e);
        } catch (NotCompliantMBeanException e) {
            log.error("Cannot register OSGi ServiceStateMBean", e);
        }

        log.debug("Registering packages monitor with MBeanServer: "
                           + mbeanServer + " with name: " + packageStateName);
        try {
            mbeanServer.registerMBean(packageState, packageStateName);
        } catch (InstanceAlreadyExistsException e) {
            log.error("Cannot register OSGi PackageStateMBean", e);
        } catch (MBeanRegistrationException e) {
            log.error("Cannot register OSGi PackageStateMBean", e);
        } catch (NotCompliantMBeanException e) {
            log.error("Cannot register OSGi PackageStateMBean", e);
        }

        configAdminTracker = new ServiceTracker(
                                                bundleContext,
                                                "org.osgi.service.cm.ConfigurationAdmin",
                                                new ConfigAdminTracker());
        permissionAdminTracker = new ServiceTracker(
                                                    bundleContext,
                                                    "org.osgi.service.permissionadmin.PermissionAdmin",
                                                    new PermissionAdminTracker());
        provisioningServiceTracker = new ServiceTracker(
                                                        bundleContext,
                                                        "org.osgi.service.provisioning.ProvisioningService",
                                                        new ProvisioningServiceTracker());
        userAdminTracker = new ServiceTracker(
                                              bundleContext,
                                              "org.osgi.service.useradmin.UserAdmin",
                                              new UserAdminTracker());
        configAdminTracker.open();
        permissionAdminTracker.open();
        provisioningServiceTracker.open();
        userAdminTracker.open();
        servicesRegistered.set(true);
    }

    private static final Log log = LogFactory.getLog(Activator.class);

    protected MBeanServer mbeanServer;
    protected StandardMBean bundleState;
    protected StandardMBean packageState;
    protected StandardMBean serviceState;
    protected BundleContext bundleContext;
    protected ObjectName bundlesStateName;
    protected StandardMBean framework;
    protected ObjectName frameworkName;
    protected ServiceTracker mbeanServiceTracker;
    protected ObjectName packageStateName;
    protected ObjectName serviceStateName;
    protected AtomicBoolean servicesRegistered = new AtomicBoolean(false);
    protected ServiceTracker configAdminTracker;
    protected ServiceTracker permissionAdminTracker;
    protected ServiceTracker provisioningServiceTracker;
    protected ServiceTracker userAdminTracker;

    class MBeanServiceTracker implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference servicereference) {
            try {
                log.debug("Adding MBeanServer: " + servicereference);
                mbeanServer = (MBeanServer) bundleContext.getService(servicereference);
                Runnable registration = new Runnable() {
                    public void run() {
                        registerServices();
                    }
                };
                Thread registrationThread = new Thread(registration,
                                                       "JMX Core MBean Registration");
                registrationThread.setDaemon(true);
                registrationThread.start();

                return mbeanServer;
            } catch (RuntimeException e) {
                log.error("uncaught exception in addingService", e);
                throw e;
            }
        }

        public void modifiedService(ServiceReference servicereference,
                                    Object obj) {
            // no op
        }

        public void removedService(ServiceReference servicereference, Object obj) {
            try {
                log.debug("Removing MBeanServer: " + servicereference);
                Runnable deregister = new Runnable() {
                    public void run() {
                        deregisterServices();
                        mbeanServer = null;
                    }
                };

                Thread deregisterThread = new Thread(deregister,
                                                     "JMX Core MBean Deregistration");
                deregisterThread.setDaemon(true);
                deregisterThread.start();

            } catch (Throwable e) {
                log.debug("uncaught exception in removedService", e);
            }
        }
    }

    class ConfigAdminTracker implements ServiceTrackerCustomizer {
        StandardMBean manager;
        ObjectName name;

        public ConfigAdminTracker() {
            try {
                name = new ObjectName(CM_SERVICE);
            } catch (Throwable e) {
                throw new IllegalStateException(
                                                "unable to create object name: "
                                                        + CM_SERVICE);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.
         * osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            log.debug("Registering configuration admin with MBeanServer: "
                               + mbeanServer + " with name: " + name);
            ConfigurationAdmin admin = (ConfigurationAdmin) bundleContext.getService(reference);
            try {
                manager = new StandardMBean(new ConfigAdminManager(admin),
                                            ConfigAdminManagerMBean.class);
            } catch (NotCompliantMBeanException e1) {
                log.fatal("Unable to create Configuration Admin Manager");
                return admin;
            }
            try {
                mbeanServer.registerMBean(manager, name);
            } catch (InstanceAlreadyExistsException e) {
                log.error("Cannot register Configuration Manager MBean", e);
            } catch (MBeanRegistrationException e) {
                log.error("Cannot register Configuration Manager MBean", e);
            } catch (NotCompliantMBeanException e) {
                log.error("Cannot register Configuration Manager MBean", e);
            }
            return admin;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {

            log.debug("deregistering configuration admin from: "
                               + mbeanServer + " with name: " + name);
            try {
                mbeanServer.unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                log.debug("Configuration Manager MBean was never registered");
            } catch (MBeanRegistrationException e) {
                log.error("Cannot deregister Configuration Manager MBean", e);
            }
        }
    }

    class PermissionAdminTracker implements ServiceTrackerCustomizer {
        StandardMBean manager;
        ObjectName name;

        public PermissionAdminTracker() {
            try {
                name = new ObjectName(PA_SERVICE);
            } catch (Throwable e) {
                throw new IllegalStateException(
                                                "unable to create object name: "
                                                        + PA_SERVICE);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.
         * osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            log.debug("Registering permission admin with MBeanServer: "
                               + mbeanServer + " with name: " + name);
            PermissionAdmin admin = (PermissionAdmin) bundleContext.getService(reference);
            try {
                manager = new StandardMBean(new PermissionManager(admin),
                                            PermissionManagerMBean.class);
            } catch (NotCompliantMBeanException e1) {
                log.fatal("Unable to create Permission Admin Manager");
                return admin;
            }
            try {
                mbeanServer.registerMBean(manager, name);
            } catch (InstanceAlreadyExistsException e) {
                log.error("Cannot register Permission Manager MBean", e);
            } catch (MBeanRegistrationException e) {
                log.error("Cannot register Permission Manager MBean", e);
            } catch (NotCompliantMBeanException e) {
                log.error("Cannot register Permission Manager MBean", e);
            }
            return admin;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            log.debug("deregistering permission admin with MBeanServer: "
                               + mbeanServer + " with name: " + name);
            try {
                mbeanServer.unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                log.debug("Permission Manager MBean was never registered");
            } catch (MBeanRegistrationException e) {
                log.error("Cannot deregister Permission Manager MBean", e);
            }
        }
    }

    class ProvisioningServiceTracker implements ServiceTrackerCustomizer {
        StandardMBean provisioning;
        ObjectName name;

        public ProvisioningServiceTracker() {
            try {
                name = new ObjectName(PS_SERVICE);
            } catch (Throwable e) {
                throw new IllegalStateException(
                                                "unable to create object name: "
                                                        + PS_SERVICE);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.
         * osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            log.debug("Registering provisioning service with MBeanServer: "
                               + mbeanServer + " with name: " + name);
            ProvisioningService admin = (ProvisioningService) bundleContext.getService(reference);
            try {
                provisioning = new StandardMBean(new Provisioning(admin),
                                                 ProvisioningMBean.class);
            } catch (NotCompliantMBeanException e1) {
                log.fatal("Unable to create Provisioning Service Manager");
                return admin;
            }
            try {
                mbeanServer.registerMBean(provisioning, name);
            } catch (InstanceAlreadyExistsException e) {
                log.error("Cannot register Provisioning Service MBean", e);
            } catch (MBeanRegistrationException e) {
                log.error("Cannot register Provisioning Service MBean", e);
            } catch (NotCompliantMBeanException e) {
                log.error("Cannot register Provisioning Service MBean", e);
            }
            return admin;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            log.debug("deregistering provisioning service with MBeanServer: "
                               + mbeanServer + " with name: " + name);
            try {
                mbeanServer.unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                log.debug("Provisioning Service MBean was never registered");
            } catch (MBeanRegistrationException e) {
                log.error("Cannot deregister Provisioning Service MBean", e);
            }
        }
    }

    class UserAdminTracker implements ServiceTrackerCustomizer {
        StandardMBean manager;
        ObjectName name;

        public UserAdminTracker() {
            try {
                name = new ObjectName(UA_SERVICE);
            } catch (Throwable e) {
                throw new IllegalStateException(
                                                "unable to create object name: "
                                                        + UA_SERVICE);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.
         * osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            log.debug("Registering user admin with MBeanServer: " + mbeanServer
                      + " with name: " + name);
            UserAdmin admin = (UserAdmin) bundleContext.getService(reference);
            try {
                manager = new StandardMBean(new UserManager(admin),
                                            UserManagerMBean.class);
            } catch (NotCompliantMBeanException e1) {
                log.fatal("Unable to create User Admin Manager");
                return admin;
            }
            try {
                mbeanServer.registerMBean(manager, name);
            } catch (InstanceAlreadyExistsException e) {
                log.error("Cannot register User Manager MBean", e);
            } catch (MBeanRegistrationException e) {
                log.error("Cannot register User Manager MBean", e);
            } catch (NotCompliantMBeanException e) {
                log.error("Cannot register User Manager MBean", e);
            }
            return admin;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void modifiedService(ServiceReference reference, Object service) {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org
         * .osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            log.debug("Deregistering user admin with MBeanServer: "
                      + mbeanServer + " with name: " + name);
            try {
                mbeanServer.unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                log.debug("User Manager MBean was never registered");
            } catch (MBeanRegistrationException e) {
                log.error("Cannot deregister User Manager MBean", e);
            }
        }
    }
}
