package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataTypes {
        @JsonProperty("id")
        private long id;

        @JsonProperty("slug")
        private String slug;

        @JsonProperty("name")
        private String name;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
}
