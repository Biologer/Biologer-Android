package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brjovanovic on 3/23/2018.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "first_name",
        "last_name",
        "email",
        "institution",
        "settings",
        "deleted_at",
        "full_name",
        "is_verified",
        "roles"
})
public class UserDataSer {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("email")
    private String email;
    @JsonProperty("institution")
    private Object institution;
    @JsonProperty("settings")
    private UserSettings settings;
    @JsonProperty ("deleted_at")
    private String deletedAt;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("is_verified")
    private boolean isEmailVerified;
    @JsonProperty("roles")
    private List<Object> roles = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("first_name")
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty("first_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonProperty("last_name")
    public String getLastName() {
        return lastName;
    }

    @JsonProperty("last_name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("institution")
    public Object getInstitution() {
        return institution;
    }

    @JsonProperty("institution")
    public void setInstitution(Object institution) {
        this.institution = institution;
    }

    @JsonProperty("settings")
    public UserSettings getSettings() {
        return settings;
    }

    @JsonProperty("settings")
    public void setSettings(UserSettings settings) {
        this.settings = settings;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("roles")
    public List<Object> getRoles() {
        return roles;
    }

    @JsonProperty("roles")
    public void setRoles(List<Object> roles) {
        this.roles = roles;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }
}
