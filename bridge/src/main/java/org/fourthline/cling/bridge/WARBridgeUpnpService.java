/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.fourthline.cling.bridge;

import org.fourthline.cling.bridge.link.LinkManager;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.ControlPointImpl;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryImpl;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.RouterImpl;

import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class WARBridgeUpnpService implements BridgeUpnpService {

    final private static Logger log = Logger.getLogger(BridgeUpnpService.class.getName());

    protected BridgeUpnpServiceConfiguration configuration;
    protected ControlPoint controlPoint;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;
    protected Router router;
    protected LinkManager linkManager;

    public WARBridgeUpnpService() {
    }

    public WARBridgeUpnpService(BridgeUpnpServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    synchronized public void start() {
        if (configuration == null) {
            throw new IllegalStateException("Configuration required to start service");
        }
        if (getRegistry() != null || getRouter() != null) {
            throw new IllegalStateException("Service already running");
        }
        log.info(">>> Starting WAR Bridge UPnP service...");
        log.info("Using configuration: " + getConfiguration().getClass().getName());
        // Instantiation order is important: Router needs to start its network services after registry is ready
        this.protocolFactory = createProtocolFactory();
        this.registry = createRegistry(protocolFactory);
        this.router = createRouter(protocolFactory, registry);
        this.controlPoint = createControlPoint(protocolFactory, registry);

        this.linkManager = createLinkManager();
        getRegistry().addListener(linkManager.getDeviceDiscovery());

        log.info("<<< WAR Bridge UPnP service started successfully");

    }

    protected ProtocolFactory createProtocolFactory() {
        return new BridgeProtocolFactory(this);
    }

    protected Registry createRegistry(ProtocolFactory protocolFactory) {
        return new RegistryImpl(this);
    }

    protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
        return new RouterImpl(getConfiguration(), protocolFactory);
    }

    protected ControlPoint createControlPoint(ProtocolFactory protocolFactory, Registry registry) {
        return new ControlPointImpl(getConfiguration(), protocolFactory, registry);
    }

    protected LinkManager createLinkManager() {
        return new LinkManager(this);
    }

    synchronized public BridgeUpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    synchronized public void setConfiguration(BridgeUpnpServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    synchronized public ControlPoint getControlPoint() {
        return controlPoint;
    }

    synchronized public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    synchronized public Registry getRegistry() {
        return registry;
    }

    synchronized public Router getRouter() {
        return router;
    }

    synchronized public LinkManager getLinkManager() {
        return linkManager;
    }

    synchronized public void shutdown() {
        if (getRegistry() == null || getRouter() == null) return;
        // Well, since java.util.logging has its own shutdown hook, this
        // might actually make it into the log or not...
        log.info(">>> Shutting down UPnP service...");

        getLinkManager().shutdown();
        getRegistry().shutdown();
        getRouter().shutdown();
        getConfiguration().shutdown();

        log.info("<<< UPnP service shutdown completed");
    }
}
