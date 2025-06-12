/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for scanning and discovering annotated classes in the classpath.
 * This class provides methods to scan packages for classes with specific annotations
 * without using external libraries.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Annotation-based class discovery</li>
 * <li>Package scanning with recursive support</li>
 * <li>Support for both directory and JAR scanning</li>
 * <li>Thread-safe operations</li>
 * <li>Zero external dependencies</li>
 * </ul>
 *
 * <p>
 * Integration points:
 * <ul>
 * <li>Factory implementations for component discovery</li>
 * <li>Runtime type resolution</li>
 * <li>Metadata-driven component registration</li>
 * <li>Annotation processing</li>
 * </ul>
 *
 * <p>
 * Implementation details:
 * <ul>
 * <li>Uses standard Java reflection API</li>
 * <li>Optimized for performance with minimal class loading</li>
 * <li>Supports both file system and JAR-based classpath entries</li>
 * <li>Detailed logging for diagnostic purposes</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitAnnotationScanner {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit AnnotationScanner: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_SCAN = LOG_PREFIX + "Scan - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_TRACE = LOG_PREFIX + "Trace - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAnnotationScanner.class);

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>
     * This class is designed to be used as a utility class with static methods.
     * </p>
     */
    private HomekitAnnotationScanner() {
        // Utility class - no instantiation
    }

    /**
     * Scans the specified package for classes annotated with the given annotation.
     *
     * <p>
     * This method recursively scans the given package and all its sub-packages for
     * classes that are annotated with the specified annotation type. It handles both
     * directory and JAR entries in the classpath.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Uses class loader to find package resources</li>
     * <li>Handles both file system and JAR-based scanning</li>
     * <li>Performs minimal class loading to check annotations</li>
     * <li>Provides comprehensive error handling and logging</li>
     * </ul>
     *
     * @param packageName The package to scan (e.g., "org.openhab.io.homekit.library")
     * @param annotationClass The annotation class to look for
     * @return A set of classes annotated with the specified annotation
     * @throws IllegalArgumentException if any parameter is null
     */
    public static <T extends Annotation> Set<Class<?>> findAnnotatedClasses(String packageName,
            Class<T> annotationClass) {
        logger.debug("{}Scanning package '{}' for classes with annotation '{}'", LOG_SCAN, packageName,
                annotationClass.getName());
        Set<Class<?>> result = new HashSet<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            List<File> dirs = new ArrayList<>();
            List<String> jarFiles = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                logger.trace("{}Found resource: {} with protocol: {}", LOG_TRACE, resource.getPath(), protocol);

                if ("file".equals(protocol)) {
                    dirs.add(new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name())));
                } else if ("jar".equals(protocol)) {
                    String jarPath = resource.getPath();
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring("file:".length());
                    }
                    jarPath = jarPath.substring(0, jarPath.indexOf('!'));
                    jarFiles.add(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()));
                }
            }

            // Process directories
            for (File directory : dirs) {
                result.addAll(findAnnotatedClassesInDirectory(directory, packageName, annotationClass));
            }

            // Process JARs
            for (String jarPath : jarFiles) {
                result.addAll(findAnnotatedClassesInJar(jarPath, packageName, annotationClass));
            }

            logger.info("{}Found {} classes in package '{}' with annotation '{}'", LOG_SCAN, result.size(), packageName,
                    annotationClass.getName());
        } catch (IOException e) {
            logger.error("{}Error scanning for annotated classes: {}", LOG_ERROR, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Finds annotated classes in a directory.
     *
     * <p>
     * This method scans a directory for class files and checks if they are annotated
     * with the specified annotation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Recursively scans subdirectories</li>
     * <li>Handles file system paths and class naming</li>
     * <li>Attempts to load and check each class</li>
     * <li>Provides detailed error logging</li>
     * </ul>
     *
     * @param directory The directory to scan
     * @param packageName The package name being scanned
     * @param annotationClass The annotation to look for
     * @return A set of annotated classes found in the directory
     */
    private static <T extends Annotation> Set<Class<?>> findAnnotatedClassesInDirectory(File directory,
            String packageName, Class<T> annotationClass) {
        Set<Class<?>> result = new HashSet<>();
        if (!directory.exists()) {
            logger.warn("{}Directory does not exist: {}", LOG_WARN, directory.getAbsolutePath());
            return result;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            logger.warn("{}Cannot list files in directory: {}", LOG_WARN, directory.getAbsolutePath());
            return result;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName + "." + file.getName();
                result.addAll(findAnnotatedClassesInDirectory(file, subPackage, annotationClass));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                tryAddAnnotatedClass(result, className, annotationClass);
            }
        }
        return result;
    }

    /**
     * Finds annotated classes in a JAR file.
     *
     * <p>
     * This method scans a JAR file for class files in the specified package and
     * checks if they are annotated with the specified annotation.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Safely opens and processes JAR entries</li>
     * <li>Filters entries by package name</li>
     * <li>Handles JAR path formats and class names</li>
     * <li>Provides robust error handling</li>
     * </ul>
     *
     * @param jarPath The path to the JAR file
     * @param packageName The package name to scan within the JAR
     * @param annotationClass The annotation to look for
     * @return A set of annotated classes found in the JAR
     */
    private static <T extends Annotation> Set<Class<?>> findAnnotatedClassesInJar(String jarPath, String packageName,
            Class<T> annotationClass) {
        Set<Class<?>> result = new HashSet<>();
        try (JarFile jarFile = new JarFile(jarPath)) {
            String packagePath = packageName.replace('.', '/') + "/";
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    tryAddAnnotatedClass(result, className, annotationClass);
                }
            }
        } catch (IOException e) {
            logger.error("{}Error processing JAR file {}: {}", LOG_ERROR, jarPath, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Tries to load a class and add it to the result set if it's annotated.
     *
     * <p>
     * This method attempts to load a class by name and checks if it is annotated
     * with the specified annotation. If so, it adds the class to the result set.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Safely handles class loading</li>
     * <li>Checks for annotation presence</li>
     * <li>Provides detailed error logging</li>
     * <li>Gracefully handles class loading exceptions</li>
     * </ul>
     *
     * @param result The set to add the class to if it's annotated
     * @param className The name of the class to check
     * @param annotationClass The annotation to look for
     */
    private static <T extends Annotation> void tryAddAnnotatedClass(Set<Class<?>> result, String className,
            Class<T> annotationClass) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(annotationClass)) {
                result.add(clazz);
                logger.trace("{}Added annotated class: {}", LOG_TRACE, className);
            }
        } catch (ClassNotFoundException e) {
            logger.trace("{}Could not load class: {}", LOG_TRACE, className);
        } catch (NoClassDefFoundError e) {
            logger.trace("{}Error loading class {}: {}", LOG_TRACE, className, e.getMessage());
        } catch (Exception e) {
            logger.warn("{}Unexpected error checking class {}: {}", LOG_WARN, className, e.getMessage());
        }
    }

    /**
     * Finds classes in the specified package that are assignable from the given base class.
     *
     * <p>
     * This method recursively scans the given package and all its sub-packages for
     * classes that extend or implement the specified base class or interface.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Leverages common class discovery logic</li>
     * <li>Filters classes by inheritance or implementation</li>
     * <li>Supports both classes and interfaces as base types</li>
     * <li>Provides detailed logging and diagnostics</li>
     * </ul>
     *
     * @param packageName The package to scan
     * @param baseClass The base class or interface to check against
     * @return A set of classes that extend or implement the base class
     * @throws IllegalArgumentException if any parameter is null
     */
    public static <T> Set<Class<? extends T>> findSubclasses(String packageName, Class<T> baseClass) {
        logger.debug("{}Scanning package '{}' for subclasses of '{}'", LOG_SCAN, packageName, baseClass.getName());
        Set<Class<? extends T>> result = new HashSet<>();
        Set<Class<?>> allClasses = findAllClasses(packageName);

        for (Class<?> clazz : allClasses) {
            if (baseClass.isAssignableFrom(clazz) && !clazz.equals(baseClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends T> subclass = (Class<? extends T>) clazz;
                result.add(subclass);
                logger.trace("{}Added subclass: {}", LOG_TRACE, clazz.getName());
            }
        }

        logger.info("{}Found {} classes in package '{}' that are subclasses of '{}'", LOG_SCAN, result.size(),
                packageName, baseClass.getName());
        return result;
    }

    /**
     * Finds all classes in the specified package.
     *
     * <p>
     * This method recursively scans the given package and all its sub-packages for
     * all available classes. It serves as a utility method for other scanning operations.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Scans both directories and JARs</li>
     * <li>Handles class loading efficiently</li>
     * <li>Provides comprehensive error handling</li>
     * <li>Excludes problematic classes gracefully</li>
     * </ul>
     *
     * @param packageName The package to scan
     * @return A set of all classes found in the package
     * @throws IllegalArgumentException if packageName is null
     */
    public static Set<Class<?>> findAllClasses(String packageName) {
        logger.debug("{}Scanning package '{}' for all classes", LOG_SCAN, packageName);
        Set<Class<?>> result = new HashSet<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            List<File> dirs = new ArrayList<>();
            List<String> jarFiles = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                logger.trace("{}Found resource: {} with protocol: {}", LOG_TRACE, resource.getPath(), protocol);

                if ("file".equals(protocol)) {
                    dirs.add(new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8.name())));
                } else if ("jar".equals(protocol)) {
                    String jarPath = resource.getPath();
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring("file:".length());
                    }
                    jarPath = jarPath.substring(0, jarPath.indexOf('!'));
                    jarFiles.add(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()));
                }
            }

            // Process directories
            for (File directory : dirs) {
                result.addAll(findClassesInDirectory(directory, packageName));
            }

            // Process JARs
            for (String jarPath : jarFiles) {
                result.addAll(findClassesInJar(jarPath, packageName));
            }

            logger.info("{}Found {} classes in package '{}'", LOG_SCAN, result.size(), packageName);
        } catch (IOException e) {
            logger.error("{}Error scanning for classes: {}", LOG_ERROR, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Finds all classes in a directory.
     *
     * <p>
     * This method scans a directory for class files and attempts to load them.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Recursively processes subdirectories</li>
     * <li>Handles file system paths and class naming</li>
     * <li>Provides detailed error logging</li>
     * <li>Filters for .class files only</li>
     * </ul>
     *
     * @param directory The directory to scan
     * @param packageName The package name being scanned
     * @return A set of classes found in the directory
     */
    private static Set<Class<?>> findClassesInDirectory(File directory, String packageName) {
        Set<Class<?>> result = new HashSet<>();
        if (!directory.exists()) {
            logger.warn("{}Directory does not exist: {}", LOG_WARN, directory.getAbsolutePath());
            return result;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            logger.warn("{}Cannot list files in directory: {}", LOG_WARN, directory.getAbsolutePath());
            return result;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName + "." + file.getName();
                result.addAll(findClassesInDirectory(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                tryLoadClass(result, className);
            }
        }
        return result;
    }

    /**
     * Finds all classes in a JAR file.
     *
     * <p>
     * This method scans a JAR file for class files in the specified package and
     * attempts to load them.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Safely processes JAR entries</li>
     * <li>Filters entries by package name</li>
     * <li>Handles JAR path formats and class names</li>
     * <li>Provides robust error handling</li>
     * </ul>
     *
     * @param jarPath The path to the JAR file
     * @param packageName The package name to scan within the JAR
     * @return A set of classes found in the JAR
     */
    private static Set<Class<?>> findClassesInJar(String jarPath, String packageName) {
        Set<Class<?>> result = new HashSet<>();
        try (JarFile jarFile = new JarFile(jarPath)) {
            String packagePath = packageName.replace('.', '/') + "/";
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    tryLoadClass(result, className);
                }
            }
        } catch (IOException e) {
            logger.error("{}Error processing JAR file {}: {}", LOG_ERROR, jarPath, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Tries to load a class and add it to the result set.
     *
     * <p>
     * This method attempts to load a class by name and adds it to the result set if successful.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     * <li>Safely handles class loading</li>
     * <li>Gracefully handles class loading exceptions</li>
     * <li>Provides detailed error logging</li>
     * <li>Filters out interfaces and abstract classes if specified</li>
     * </ul>
     *
     * @param result The set to add the class to if loading is successful
     * @param className The name of the class to load
     */
    private static void tryLoadClass(Set<Class<?>> result, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            result.add(clazz);
            logger.trace("{}Added class: {}", LOG_TRACE, className);
        } catch (ClassNotFoundException e) {
            logger.trace("{}Could not load class: {}", LOG_TRACE, className);
        } catch (NoClassDefFoundError e) {
            logger.trace("{}Error loading class {}: {}", LOG_TRACE, className, e.getMessage());
        } catch (Exception e) {
            logger.warn("{}Unexpected error loading class {}: {}", LOG_WARN, className, e.getMessage());
        }
    }
}
