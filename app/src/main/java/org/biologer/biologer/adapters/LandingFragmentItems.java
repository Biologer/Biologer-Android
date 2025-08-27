package org.biologer.biologer.adapters;

import java.util.Date;

public class LandingFragmentItems {
    private Long observationId;
    private Integer timedCountId;
    private String title;
    private String subtitle;
    private String image;
    private Date date;

    public LandingFragmentItems(Long observationId, Integer timedCountId,
                                String title, String subtitle, String image,
                                Date date) {
        this.observationId = observationId;
        this.timedCountId = timedCountId;
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.date = date;
    }

    public Long getObservationId() {
        return observationId;
    }

    public void setObservationId(Long observationId) {
        this.observationId = observationId;
    }

    public Integer getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer timedCountId) {
        this.timedCountId = timedCountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
