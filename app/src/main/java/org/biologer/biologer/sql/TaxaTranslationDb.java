package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TaxaTranslationDb {

    @Id(assignable = true)
    long id;
    private long taxonId;
    private String locale;
    private final String nativeName;
    private final String latinName;
    private final boolean useAtlasCode;
    private String description;

    public TaxaTranslationDb(long id, long taxonId, String locale, String nativeName, String latinName, boolean useAtlasCode, String description) {
        this.id = id;
        this.taxonId = taxonId;
        this.locale = locale;
        this.nativeName = nativeName;
        this.latinName = latinName;
        this.useAtlasCode = useAtlasCode;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        id = id;
    }

    public long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLatinName() {
        return latinName;
    }

    public boolean isUseAtlasCode() {
        return useAtlasCode;
    }

}
