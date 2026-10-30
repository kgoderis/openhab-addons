/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.atsadvanced.internal;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson DTOs for JSON exchanged with the ATS Advanced dumb gateway (no ServiceStack on the Java side).
 * <p>
 * Request bodies mirror the legacy NAce HTTP API: {@link ConfigurePanel} for {@code POST /configurepanel},
 * {@link Message} for {@code POST /message}, and {@link MessageResponse} for synchronous replies plus
 * SSE {@code panelevent} payloads. Field names use gateway spelling (e.g. {@code hearbeat}).
 *
 * @author Karel Goderis - Initial contribution
 */
public final class PanelClient {

    private PanelClient() {
    }

    /**
     * Panel TCP connection parameters sent to the gateway before {@code device.getConnect}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigurePanel {
        /** Panel IP or hostname as seen from the gateway process. */
        @JsonProperty("hostaddress")
        public String Hostaddress;
        /** Panel TCP port (typically 32000). */
        @JsonProperty("port")
        public Integer Port;
        /** Encryption password string (binding uses 52 zeros). */
        @JsonProperty("password")
        public String Password;
        /** Heartbeat interval in milliseconds (gateway field name {@code hearbeat}). */
        @JsonProperty("hearbeat")
        public Integer Hearbeat;
        /** Connect/read timeout in milliseconds. */
        @JsonProperty("timeout")
        public Integer Timeout;
        /** Retry count for panel socket operations. */
        @JsonProperty("retries")
        public Integer Retries;

        public String getHostaddress() {
            return Hostaddress;
        }

        public ConfigurePanel setHostaddress(String value) {
            this.Hostaddress = value;
            return this;
        }

        public Integer getPort() {
            return Port;
        }

        public ConfigurePanel setPort(Integer value) {
            this.Port = value;
            return this;
        }

        public String getPassword() {
            return Password;
        }

        public ConfigurePanel setPassword(String value) {
            this.Password = value;
            return this;
        }

        public Integer getHearbeat() {
            return Hearbeat;
        }

        public ConfigurePanel setHearbeat(Integer value) {
            this.Hearbeat = value;
            return this;
        }

        public Integer getTimeout() {
            return Timeout;
        }

        public ConfigurePanel setTimeout(Integer value) {
            this.Timeout = value;
            return this;
        }

        public Integer getRetries() {
            return Retries;
        }

        public ConfigurePanel setRetries(Integer value) {
            this.Retries = value;
            return this;
        }
    }

    /**
     * Gateway acknowledgement for {@code /configurepanel} ({@code "1"} = success).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigurePanelResponse {
        @JsonProperty("result")
        public String Result;

        public String getResult() {
            return Result;
        }

        public ConfigurePanelResponse setResult(String value) {
            this.Result = value;
            return this;
        }
    }

    /**
     * Outbound panel protocol invocation: {@code name} is the NAce message id (e.g. {@code getSTAT.ZONE}),
     * {@code properties} carries indexed parameters.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        /** Protocol message name. */
        public String name;
        /** Ordered property bag for the message. */
        public ArrayList<Property> properties = new ArrayList<>();

        public String getName() {
            return name;
        }

        public Message setName(String value) {
            this.name = value;
            return this;
        }

        public ArrayList<Property> getProperties() {
            return properties != null ? properties : new ArrayList<>();
        }

        public Message setProperties(ArrayList<Property> value) {
            this.properties = value;
            return this;
        }
    }

    /**
     * Inbound panel or gateway event: {@code name} discriminates the payload
     * ({@code returnSTAT.*}, {@code return.error}, {@code msg.MONITOR}, etc.).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageResponse {
        /** Response or event type name. */
        public String name;
        /** Property bag attached to the response. */
        public ArrayList<Property> properties = new ArrayList<>();

        public String getName() {
            return name;
        }

        public MessageResponse setName(String value) {
            this.name = value;
            return this;
        }

        public ArrayList<Property> getProperties() {
            return properties != null ? properties : new ArrayList<>();
        }

        public MessageResponse setProperties(ArrayList<Property> value) {
            this.properties = value;
            return this;
        }
    }

    /**
     * Single named field in a {@link Message} or {@link MessageResponse}.
     * {@code id} matches protocol property names; {@code value} may be boolean, number, or string.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        /** Property identifier (e.g. {@code objectID}, {@code ZNEV_ALARM}). */
        public String id;
        /** Property index (usually 0). */
        public Integer index;
        /** Property value as returned by the gateway. */
        public Object value;

        public String getId() {
            return id;
        }

        public Property setId(String value) {
            this.id = value;
            return this;
        }

        public Integer getIndex() {
            return index;
        }

        public Property setIndex(Integer value) {
            this.index = value;
            return this;
        }

        public Object getValue() {
            return value;
        }

        public Property setValue(Object value) {
            this.value = value;
            return this;
        }
    }
}
