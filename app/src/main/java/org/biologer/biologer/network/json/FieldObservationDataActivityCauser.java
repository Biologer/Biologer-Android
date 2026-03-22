package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataActivityCauser {
        @JsonProperty("id")
        private long id;

        @JsonProperty("full_name")
        private String fullName;

        public long getId() { return id; }
        public String getFullName() { return fullName; }

}
