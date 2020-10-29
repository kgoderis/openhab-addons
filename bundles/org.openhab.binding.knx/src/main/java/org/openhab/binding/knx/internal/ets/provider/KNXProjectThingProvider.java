/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.internal.ets.provider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.knx.KNXBindingConstants;
import org.openhab.binding.knx.KNXTypeMapper;
import org.openhab.binding.knx.ets.KNXProjectParser;
import org.openhab.binding.knx.ets.KNXProjectProvider;
import org.openhab.binding.knx.internal.dpt.KNXCoreTypeMapper;
import org.openhab.binding.knx.internal.ets.config.ETSBridgeConfiguration;
import org.openhab.binding.knx.internal.ets.parser.KNXProject13Parser;
import org.openhab.binding.knx.internal.ets.parser.KNXProject14Parser;
import org.openhab.binding.knx.internal.factory.KNXHandlerFactory;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.GenericItem;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.util.ThingHelper;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The {@link KNXProjectThingProvider} provides Things from knxproject files
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class KNXProjectThingProvider implements ThingProvider, KNXProjectProvider {

    private final Logger logger = LoggerFactory.getLogger(KNXProjectThingProvider.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("knx");

    private KNXHandlerFactory factory;
    private final KNXTypeMapper typeHelper = new KNXCoreTypeMapper();
    private @Nullable KNXProjectParser knxParser;
    private Set<File> knxProjects = new CopyOnWriteArraySet<File>();
    private HashMap<File, HashMap<ThingUID, Thing>> providedThings = new HashMap<File, HashMap<ThingUID, Thing>>();
    protected List<ProviderChangeListener<Thing>> providerListeners = new CopyOnWriteArrayList<ProviderChangeListener<Thing>>();

    private final Bridge bridge;

    public KNXProjectThingProvider(Bridge bridge, KNXHandlerFactory factory) {
        this.bridge = bridge;
        this.factory = factory;
    }

    @Override
    public Collection<Thing> getAll() {
        ArrayList<Thing> allThings = new ArrayList<Thing>();
        for (File aFile : providedThings.keySet()) {
            allThings.addAll(providedThings.get(aFile).values());
        }

        return allThings;
    }

    @Override
    public final void addProviderChangeListener(ProviderChangeListener<Thing> listener) {
        if (listener != null) {
            providerListeners.add(listener);
        }
    }

    @Override
    public final void removeProviderChangeListener(ProviderChangeListener<Thing> listener) {
        if (listener != null) {
            providerListeners.remove(listener);
        }
    }

    private enum EventType {
        ADDED,
        REMOVED,
        UPDATED;
    }

    private void notifyListeners(Thing oldElement, Thing element, EventType eventType) {
        for (ProviderChangeListener<Thing> listener : this.providerListeners) {
            try {
                switch (eventType) {
                    case ADDED:
                        listener.added(this, element);
                        break;
                    case REMOVED:
                        listener.removed(this, element);
                        break;
                    case UPDATED:
                        listener.updated(this, oldElement, element);
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                logger.error("Could not inform the listener '{}' about the '{}' event : '{}'", listener,
                        eventType.name(), ex.getMessage(), ex);
            }
        }
    }

    private void notifyListeners(Thing element, EventType eventType) {
        notifyListeners(element, element, eventType);
    }

    protected void notifyListenersAboutAddedElement(Thing element) {
        notifyListeners(element, EventType.ADDED);
    }

    protected void notifyListenersAboutRemovedElement(Thing element) {
        notifyListeners(element, EventType.REMOVED);
    }

    protected void notifyListenersAboutUpdatedElement(Thing oldElement, Thing element) {
        notifyListeners(oldElement, element, EventType.UPDATED);
    }

    private class ZipRunnable implements Runnable {
        private File file;
        private FileInputStream openInputStream;

        public ZipRunnable(File file, FileInputStream openInputStream) {
            this.file = file;
            this.openInputStream = openInputStream;
        }

        @Override
        public void run() {
            logger.trace("Unzipping the KNX Project file");

            byte[] buffer = new byte[1024];
            HashMap<Path, String> xmlFiles = new HashMap<Path, String>();

            KNXCoreTypeMapper typeHelper = new KNXCoreTypeMapper();

            try {
                Path tempDir = Files.createTempDirectory(file.getName());
                tempDir.toFile().deleteOnExit();

                ZipInputStream zis = new ZipInputStream(openInputStream);
                ZipEntry ze = zis.getNextEntry();

                while (ze != null) {
                    String fileName = ze.getName().substring(ze.getName().lastIndexOf(File.separator) + 1,
                            ze.getName().length());

                    if (!fileName.equals("Catalog.xml") && fileName.contains(".xml")) {
                        Path tempPath = Files.createTempFile(tempDir, fileName, ".tmp");
                        tempPath.toFile().deleteOnExit();
                        xmlFiles.put(tempPath, fileName);

                        FileOutputStream fos = new FileOutputStream(tempPath.toString());
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();

                logger.trace("Processing the XML Repository");

                for (Path anXml : xmlFiles.keySet()) {
                    if (xmlFiles.get(anXml).equals("knx_master.xml")) {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(Files.readAllBytes(anXml)));
                        Element root = doc.getDocumentElement();

                        switch (root.getNamespaceURI()) {
                            case KNXBindingConstants.KNX_PROJECT_12: {
                                // ETS4
                                logger.warn("ETS4 project files are not supported");
                                break;
                            }
                            case KNXBindingConstants.KNX_PROJECT_13: {
                                // ETS5
                                knxParser = new KNXProject13Parser();
                                break;
                            }
                            case KNXBindingConstants.KNX_PROJECT_14: {
                                // ETS5.5
                                knxParser = new KNXProject14Parser();
                                break;
                            }
                        }
                    }
                }

                if (knxParser != null) {
                    logger.trace("Feeding the XML Repository to the KNX Project Parser");

                    knxProjects.add(file);
                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("knx_master.xml")) {
                            knxParser.addXML("knx_master.xml", new String(Files.readAllBytes(anXml)));
                            break;
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("project.xml")) {
                            knxParser.addXML("project.xml", new String(Files.readAllBytes(anXml)));
                            break;
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("Hardware.xml")) {
                            knxParser.addXML("Hardware.xml", new String(Files.readAllBytes(anXml)));
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (!xmlFiles.get(anXml).equals("knx_master.xml") && !xmlFiles.get(anXml).equals("project.xml")
                                && !xmlFiles.get(anXml).equals("Hardware.xml")
                                && !xmlFiles.get(anXml).equals("Baggages.xml")) {
                            knxParser.addXML(xmlFiles.get(anXml), new String(Files.readAllBytes(anXml)));
                        }
                    }

                    knxParser.postProcess();

                    if (factory != null) {
                        Set<String> devices = knxParser.getIndividualAddresses();
                        HashMap<ThingUID, Thing> newThings = new HashMap<ThingUID, Thing>();
                        for (String device : devices) {

                            Configuration configuration = knxParser.getDeviceConfiguration(device);
                            Thing theThing = factory.createThing(KNXBindingConstants.THING_TYPE_GENERIC, configuration,
                                    null, bridge.getUID());

                            Map<String, String> properties = knxParser.getDeviceProperties(device);
                            properties.put("pingInterval", "0");
                            properties.put("readInterval", "3600");
                            theThing.setProperties(properties);

                            logger.info("Added a Thing {} for an actor of type {} made by {}", theThing.getUID(),
                                    properties.get(KNXBindingConstants.MANUFACTURER_HARDWARE_TYPE),
                                    properties.get(KNXBindingConstants.MANUFACTURER_NAME));

                            List<Channel> channelsToAdd = new ArrayList<Channel>();

                            Set<String> groupAddresses = knxParser.getGroupAddresses(device);
                            for (String groupAddress : groupAddresses) {
                                String dpt = knxParser.getDPT(groupAddress);

                                if (dpt != null) {
                                    Class<? extends Type> type = typeHelper.toTypeClass(dpt);
                                    logger.trace("DPT '{}' maps to Type '{}'", dpt, type.getSimpleName());
                                    Class<? extends GenericItem> itemType = typeHelper.toItemTypeClass(type);
                                    logger.trace("Type '{}' maps to ItemType '{}'", type.getSimpleName(),
                                            itemType.getSimpleName());
                                    String id = groupAddress.replace("/", "_");

                                    if (itemType != null) {
                                        Configuration channelConfiguration = knxParser
                                                .getGroupAddressConfiguration(groupAddress, device);
                                        channelConfiguration.put(KNXBindingConstants.GROUPADDRESS, groupAddress);
                                        channelConfiguration.put(KNXBindingConstants.DPT, dpt);

                                        // Convert configuration to a pure String
                                        StringBuilder configBuilder = new StringBuilder();
                                        configBuilder.append(dpt);
                                        configBuilder.append(":");
                                        configBuilder.append(groupAddress);
                                        if ((boolean) channelConfiguration.get(KNXBindingConstants.READ)) {
                                            configBuilder.append("+R");
                                        }
                                        if ((boolean) channelConfiguration.get(KNXBindingConstants.WRITE)) {
                                            configBuilder.append("+W");
                                        }
                                        if ((boolean) channelConfiguration.get(KNXBindingConstants.TRANSMIT)) {
                                            configBuilder.append("+T");
                                        }
                                        if ((boolean) channelConfiguration.get(KNXBindingConstants.UPDATE)) {
                                            configBuilder.append("+U");
                                        }
                                        channelConfiguration.put("ga", configBuilder.toString());

                                        ChannelBuilder channelBuilder = ChannelBuilder
                                                .create(new ChannelUID(theThing.getUID(), id),
                                                        StringUtils.substringBefore(itemType.getSimpleName(), "Item"))
                                                .withKind(ChannelKind.STATE).withConfiguration(channelConfiguration)
                                                .withType(new ChannelTypeUID(bridge.getUID().getBindingId(),
                                                        KNXBindingConstants.CHANNEL_GENERIC))
                                                .withDescription((String) channelConfiguration
                                                        .get(KNXBindingConstants.DESCRIPTION));

                                        Channel theChannel = channelBuilder.build();
                                        channelsToAdd.add(theChannel);

                                        logger.info("Added the Channel {} (Type {}, DPT {}, {}/{}/{}/{}, '{}', '{}')",
                                                theChannel.getUID(), theChannel.getAcceptedItemType(),
                                                theChannel.getConfiguration().get(KNXBindingConstants.DPT),
                                                theChannel.getConfiguration().get(KNXBindingConstants.READ),
                                                theChannel.getConfiguration().get(KNXBindingConstants.WRITE),
                                                theChannel.getConfiguration().get(KNXBindingConstants.TRANSMIT),
                                                theChannel.getConfiguration().get(KNXBindingConstants.UPDATE),
                                                theChannel.getDescription(), theChannel.getConfiguration().get("ga"));

                                    } else {
                                        logger.warn(
                                                "The Type for the Channel with ID '{}' and KNXBindingConstants.DPT '{}' can not be resolved",
                                                new ChannelUID(theThing.getUID(), id), dpt);
                                    }
                                } else {
                                    logger.warn(
                                            "The KNXBindingConstants.DPT for the Group Address '{}' can not be resolved",
                                            groupAddress);
                                }
                            }

                            ThingHelper.addChannelsToThing(theThing, channelsToAdd);

                            newThings.put(theThing.getUID(), theThing);
                        }

                        HashMap<ThingUID, Thing> oldThings = providedThings.get(file);
                        if (oldThings != null) {
                            for (ThingUID thingUID : newThings.keySet()) {
                                if (oldThings.keySet().contains(thingUID)) {
                                    oldThings.put(thingUID, newThings.get(thingUID));
                                    logger.debug("Updating Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutUpdatedElement(oldThings.get(thingUID),
                                            newThings.get(thingUID));
                                } else {
                                    oldThings.put(thingUID, newThings.get(thingUID));
                                    logger.debug("Adding Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutAddedElement(newThings.get(thingUID));
                                }
                            }

                            for (ThingUID thingUID : oldThings.keySet()) {
                                if (!newThings.keySet().contains(thingUID)) {
                                    oldThings.remove(thingUID);
                                    logger.debug("Removing Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutRemovedElement(oldThings.get(thingUID));
                                }
                            }

                            providedThings.put(file, oldThings);
                        } else {
                            providedThings.put(file, newThings);
                            for (ThingUID thingUID : newThings.keySet()) {
                                logger.debug("Adding Thing '{}' from knxproject file '{}'.", thingUID, file.getName());
                                notifyListenersAboutAddedElement(newThings.get(thingUID));
                            }
                        }
                    }
                    HashMap<ThingUID, Thing> oldThings = providedThings.get(file);
                } else {
                    logger.warn("The KNX Project does not contain any master data");
                }

                knxParser = null;

            } catch (Exception e) {
                logger.error("An exception occurred while parsing the KNX Project file : '{}' : {}", file.getName(),
                        e.getMessage(), e);
            }
        }
    };

    @Override
    public @NonNull Iterable<File> getAllProjects() {
        return knxProjects;
    }

    @Override
    public void removeProject(File projectToRemove) {
        HashMap<ThingUID, Thing> projectThings = providedThings.remove(projectToRemove);

        for (ThingUID thingUID : projectThings.keySet()) {
            logger.trace("Removing thing '{}' from knxproject file '{}'.", thingUID, projectToRemove.getName());
            notifyListenersAboutRemovedElement(projectThings.get(thingUID));
        }
    }

    @Override
    public void addOrRefreshProject(File file) {
        FileInputStream openInputStream = null;
        try {
            openInputStream = FileUtils.openInputStream(file);

            if (bridge != null) {
                ETSBridgeConfiguration config = bridge.getConfiguration().as(ETSBridgeConfiguration.class);
                String knxProj = config.getKnxProj();

                if (knxProj != null && knxProj.equals(file.getName())) {
                    if (knxProjects.contains(file)) {
                        logger.trace("Ah... removing the project file {}", file);
                        removeProject(file);
                    }
                    scheduler.schedule(new ZipRunnable(file, openInputStream), 0, TimeUnit.SECONDS);
                }
            }

        } catch (IOException e) {
            logger.error("An exception has occurred while opening the file {} : {}", file.getName(), e.getMessage(), e);
        }
    }
}
