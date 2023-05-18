package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by Miloš Popović on 2.3.2019.
 */
@Entity
public class TaxonDb {

    @Id(assignable = true)
    long id;
    private long parentId;
    private final String latinName;
    private final String rank;
    private final long rankLevel;
    private final String speciesAuthor;
    private final boolean restricted;
    private boolean useAtlasCode;
    private final String ancestorNames;
    private String groups;

    public TaxonDb(long id, long parentId, String latinName, String rank, long rankLevel, String speciesAuthor,
                   boolean restricted, boolean useAtlasCode, String ancestorNames, String groups) {
        this.id = id;
        this.parentId = parentId;
        this.latinName = latinName;
        this.rank = rank;
        this.rankLevel = rankLevel;
        this.speciesAuthor = speciesAuthor;
        this.restricted = restricted;
        this.useAtlasCode = useAtlasCode;
        this.ancestorNames = ancestorNames;
        this.groups = groups;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getLatinName() {
        return latinName;
    }

    public String getRank() {
        return rank;
    }

    public long getRankLevel() {
        return rankLevel;
    }

    public String getSpeciesAuthor() {
        return speciesAuthor;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public String getAncestorNames() {
        return ancestorNames;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        id = id;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public boolean isUseAtlasCode() {
        return useAtlasCode;
    }

    public void setUseAtlasCode(boolean useAtlasCode) {
        this.useAtlasCode = useAtlasCode;
    }
}