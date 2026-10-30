package org.openhab.binding.atsadvanced.poc.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class MessageDtos {

    private MessageDtos() {
    }

    public static final class ConfigurePanelRequest {
        public String hostaddress;
        public int port;
        public String password;
        public int hearbeat = 10000;
        public int timeout = 5000;
        public int retries = 2;
    }

    public static final class ConfigurePanelResponse {
        public String result;
    }

    public static final class Property {
        public String id;
        public int index;
        public Object value;

        public Property() {
        }

        public Property(String id, int index, Object value) {
            this.id = id;
            this.index = index;
            this.value = value;
        }
    }

    public static final class MessageRequest {
        public String name;
        public List<Property> properties = new ArrayList<>();

        public MessageRequest() {
        }

        public MessageRequest(String name) {
            this.name = name;
        }

        public MessageRequest prop(String id, Object value) {
            properties.add(new Property(id, 0, value));
            return this;
        }
    }

    public static final class MessageResponse {
        public String name;
        public List<Property> properties = new ArrayList<>();

        public Object property(String id) {
            if (properties == null) {
                return null;
            }
            for (Property p : properties) {
                if (id.equals(p.id)) {
                    return p.value;
                }
            }
            return null;
        }
    }

    public static final class HealthResponse {
        public String status;
        public boolean configured;
        @JsonProperty("runtime")
        public String runtime;
    }
}
