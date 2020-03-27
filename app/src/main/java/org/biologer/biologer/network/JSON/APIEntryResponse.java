package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by brjovanovic on 3/12/2018.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIEntryResponse {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
