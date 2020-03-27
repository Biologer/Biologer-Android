package org.biologer.biologer.network.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "taxon_id",
    "stage_id"
})
public class Pivot {

    @JsonProperty("taxon_id")
    private String taxonId;
    @JsonProperty("stage_id")
    private String stageId;

    @JsonProperty("taxon_id")
    public String getTaxonId() {
        return taxonId;
    }

    @JsonProperty("taxon_id")
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    @JsonProperty("stage_id")
    public String getStageId() {
        return stageId;
    }

    @JsonProperty("stage_id")
    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

}
