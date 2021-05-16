package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.List;

/**
 * Created by Miloš Popović on 2.3.2019.
 */
@Entity
public class TaxonData {

    @Id (autoincrement = true)
    private Long Id;
    private Long parentId;
    private String latinName;
    private String rank;
    private Long rankLevel;
    private String speciesAuthor;
    private boolean restricted;
    private boolean useAtlasCode;
    private String ancestorNames;
    private String groups;

    @Generated(hash = 605078756)
    public TaxonData(Long Id, Long parentId, String latinName, String rank, Long rankLevel, String speciesAuthor,
            boolean restricted, boolean useAtlasCode, String ancestorNames, String groups) {
        this.Id = Id;
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
    @Generated(hash = 345389106)
    public TaxonData() {
    }
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Long getRankLevel() {
        return rankLevel;
    }

    public void setRankLevel(Long rankLevel) {
        this.rankLevel = rankLevel;
    }

    public String getSpeciesAuthor() {
        return speciesAuthor;
    }

    public void setSpeciesAuthor(String speciesAuthor) {
        this.speciesAuthor = speciesAuthor;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public boolean isUseAtlasCode() {
        return useAtlasCode;
    }

    public void setUseAtlasCode(boolean useAtlasCode) {
        this.useAtlasCode = useAtlasCode;
    }

    public String getAncestorNames() {
        return ancestorNames;
    }

    public void setAncestorNames(String ancestorNames) {
        this.ancestorNames = ancestorNames;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public boolean getRestricted() {
        return this.restricted;
    }

    public boolean getUseAtlasCode() {
        return this.useAtlasCode;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

}

