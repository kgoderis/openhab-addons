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

package org.openhab.io.homekit.network.http;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jetty.client.AbstractConnectionPool;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Pool;
import org.eclipse.jetty.util.annotation.ManagedAttribute;

/**
 * Custom DuplexConnectionPool that exposes all connections (idle and active).
 * 
 * @author Karel Goderis - Initial contribution
     */
public class HomekitConnectionPool extends AbstractConnectionPool {
    private final List<Connection> idleConnections = new CopyOnWriteArrayList<>();
    private final List<Connection> activeConnections = new CopyOnWriteArrayList<>();

    public HomekitConnectionPool(HttpDestination destination, int maxConnections, Callback requester) {
        this(destination, maxConnections, false, requester);
    }

    public HomekitConnectionPool(HttpDestination destination, int maxConnections, boolean cache, Callback requester) {
        super(destination, Pool.StrategyType.FIRST, maxConnections, cache, requester);
    }

    @Deprecated
    public HomekitConnectionPool(HttpDestination destination, Pool<Connection> pool, Callback requester) {
        super(destination, pool, requester);
    }

    @Override
    @ManagedAttribute(value = "The maximum amount of times a connection is used before it gets closed")
    public int getMaxUsageCount() {
        return super.getMaxUsageCount();
    }

    @Override
    public void setMaxUsageCount(int maxUsageCount) {
        super.setMaxUsageCount(maxUsageCount);
    }

    @Override
    protected void onCreated(Connection connection) {
        idleConnections.add(connection);
    }

    @Override
    protected void removed(Connection connection) {
        idleConnections.remove(connection);
        activeConnections.remove(connection);
    }

    @Override
    protected void acquired(Connection connection) {
        idleConnections.remove(connection);
        activeConnections.add(connection);
    }

    @Override
    protected void released(Connection connection) {
        activeConnections.remove(connection);
        idleConnections.add(connection);
    }

    /**
     * Expose all connections (idle + active).
     */
    public List<Connection> getAllConnections() {
        List<Connection> all = new CopyOnWriteArrayList<>(idleConnections);
        all.addAll(activeConnections);
        return all;
    }

    @Override
    public BlockingQueue<Connection> getIdleConnections() {
        return new LinkedBlockingQueue<>(idleConnections);
    }

    @Override
    public List<Connection> getActiveConnections() {
        return new CopyOnWriteArrayList<>(activeConnections);
    }
}
