package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIEntryResponse {
    @JsonProperty("data")
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("activity")
        private List<FieldObservationDataActivity> activity;
        private List<PhotoResponseData> photos;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public List<PhotoResponseData> getPhotos() { return photos; }
        public void setPhotos(List<PhotoResponseData> photos) { this.photos = photos; }

        public List<FieldObservationDataActivity> getActivity() {
            return activity;
        }

        public void setActivity(List<FieldObservationDataActivity> activity) {
            this.activity = activity;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotoResponseData {
        private long id;
        private String author;
        private PhotoLicense license;
        private String metadata;
        private String path;
        private String url;

        public PhotoLicense getLicense() {
            return license;
        }

        public void setLicense(PhotoLicense license) {
            this.license = license;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PhotoLicense {
            private long id;
            private String name;
            private String link;

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
}
