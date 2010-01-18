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
package org.osgi.jmx.codec;

import static org.osgi.framework.Constants.SERVICE_ID;

import java.util.*;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.SimpleType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author Hal Hildebrand Date: Nov 24, 2008 Time: 7:09:25 AM
 * 
 *         Static utilities used by the system
 * 
 */
@SuppressWarnings("unchecked")
public class Util {
    public static long[] bundleIds(Bundle[] bundles) {
        if (bundles == null) {
            return new long[0];
        }
        long[] ids = new long[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            ids[i] = bundles[i].getBundleId();
        }
        return ids;
    }

    public static long[] bundleIds(RequiredBundle[] bundles) {
        if (bundles == null) {
            return new long[0];
        }
        long[] ids = new long[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            ids[i] = bundles[i].getBundle().getBundleId();
        }
        return ids;
    }

    public static long[] getBundlesRequiring(Bundle b, BundleContext bc,
                                             PackageAdmin admin) {
        Bundle[] all = bc.getBundles();
        ArrayList<Long> required = new ArrayList<Long>();
        for (Bundle anAll : all) {
            long[] requiring = getBundleDependencies(anAll, admin);
            if (requiring == null) {
                continue;
            }
            for (long r : requiring) {
                if (r == b.getBundleId()) {
                    required.add(anAll.getBundleId());
                }
            }
        }
        long[] ids = new long[required.size()];
        for (int i = 0; i < required.size(); i++) {
            ids[i] = required.get(i);
        }
        return ids;
    }

    public static String[] getBundleExportedPackages(Bundle b,
                                                     PackageAdmin admin) {
        ArrayList<String> packages = new ArrayList<String>();
        ExportedPackage[] exportedPackages = admin.getExportedPackages(b);
        if (exportedPackages == null) {
            return new String[0];
        }
        for (ExportedPackage pkg : exportedPackages) {
            packages.add(packageString(pkg));
        }
        return packages.toArray(new String[packages.size()]);
    }

    public static long[] getBundleFragments(Bundle b, PackageAdmin admin) {
        Bundle[] fragments = admin.getFragments(b);
        if (fragments == null) {
            return new long[0];
        }
        long ids[] = new long[fragments.length];
        for (int i = 0; i < fragments.length; i++) {
            ids[i] = fragments[i].getBundleId();
        }
        return ids;
    }

    public static Map<String, String> getBundleHeaders(Bundle b) {
        Map<String, String> headers = new Hashtable<String, String>();
        Dictionary h = b.getHeaders();
        for (Enumeration keys = h.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            headers.put(key, (String) h.get(key));
        }
        return headers;
    }

    public static String[] getBundleImportedPackages(Bundle b,
                                                     BundleContext bc,
                                                     PackageAdmin admin) {
        ArrayList<String> imported = new ArrayList<String>();
        Bundle[] allBundles = bc.getBundles();
        for (Bundle bundle : allBundles) {
            ExportedPackage[] eps = admin.getExportedPackages(bundle);
            if (eps == null) {
                continue;
            }
            for (ExportedPackage ep : eps) {
                Bundle[] imp = ep.getImportingBundles();
                if (imp == null) {
                    continue;
                }
                for (Bundle b2 : imp) {
                    if (b2.getBundleId() == b.getBundleId()) {
                        imported.add(packageString(ep));
                        break;
                    }
                }
            }
        }
        if (imported.size() == 0) {
            return new String[0];
        } else {
            return imported.toArray(new String[imported.size()]);
        }

    }

    public static long[] getBundleDependencies(Bundle bundle, PackageAdmin admin) {
        String symbolicName = bundle.getSymbolicName();
        if (symbolicName == null) {
            return new long[0];
        }
        RequiredBundle[] required = admin.getRequiredBundles(symbolicName);
        if (required == null || required.length == 0) {
            return new long[0];
        }
        return bundleIds(required);
    }

    public static String getBundleState(Bundle b) {
        switch (b.getState()) {
            case Bundle.ACTIVE:
                return "ACTIVE";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.STARTING:
                return "STARTING";
            case Bundle.STOPPING:
                return "STOPPING";
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            default:
                return "UNKNOWN";
        }
    }

    public static RequiredBundle getRequiredBundle(Bundle bundle,
                                                   BundleContext bc,
                                                   PackageAdmin admin) {
        Bundle[] all = bc.getBundles();
        for (Bundle anAll : all) {
            String symbolicName = anAll.getSymbolicName();
            if (symbolicName != null) {
                RequiredBundle[] requiring = admin.getRequiredBundles(symbolicName);
                if (requiring == null) {
                    continue;
                }
                for (RequiredBundle r : requiring) {
                    if (r.getBundle().equals(bundle)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isBundleFragment(Bundle bundle, PackageAdmin admin) {
        return admin.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    public static boolean isBundlePersistentlyStarted(Bundle bundle,
                                                      StartLevel sl) {
        return bundle.getBundleId() == 0
               || sl.isBundlePersistentlyStarted(bundle);
    }

    public static boolean isBundleRequired(Bundle bundle, BundleContext bc,
                                           PackageAdmin admin) {
        return getRequiredBundle(bundle, bc, admin) != null;
    }

    public static boolean isRequiredBundleRemovalPending(Bundle bundle,
                                                         BundleContext bc,
                                                         PackageAdmin admin) {
        RequiredBundle r = getRequiredBundle(bundle, bc, admin);
        return r != null && r.isRemovalPending();
    }

    public static String packageString(ExportedPackage pkg) {
        return pkg.getName() + ";" + pkg.getVersion();
    }

    public static long[] serviceIds(ServiceReference[] refs) {
        if (refs == null) {
            return new long[0];
        }
        long[] ids = new long[refs.length];
        for (int i = 0; i < refs.length; i++) {
            ids[i] = (Long) refs[i].getProperty(SERVICE_ID);
        }
        return ids;
    }

    public static Long[] LongArrayFrom(long[] array) {
        if (array == null) {
            return new Long[0];
        }
        Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static long[] longArrayFrom(Long[] array) {
        if (array == null) {
            return new long[0];
        }
        long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static ArrayType LONG_ARRAY_TYPE;

    public static ArrayType STRING_ARRAY_TYPE;

    private static final Log log = LogFactory.getLog(Util.class);

    static {
        try {
            LONG_ARRAY_TYPE = new ArrayType(1, SimpleType.LONG);
            STRING_ARRAY_TYPE = new ArrayType(1, SimpleType.STRING);
        } catch (OpenDataException e) {
            log.error("Cannot create array open data type", e);
        }
    }
}
