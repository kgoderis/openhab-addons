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
 * JSON DTOs for the ATS Advanced dumb gateway (no ServiceStack).
 *
 * @author Karel Goderis - Initial contribution
 */
public final class PanelClient {

    private PanelClient() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigurePanel {
        @JsonProperty("hostaddress")
        public String Hostaddress;
        @JsonProperty("port")
        public Integer Port;
        @JsonProperty("password")
        public String Password;
        @JsonProperty("hearbeat")
        public Integer Hearbeat;
        @JsonProperty("timeout")
        public Integer Timeout;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        public String name;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageResponse {
        public String name;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        public String id;
        public Integer index;
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
