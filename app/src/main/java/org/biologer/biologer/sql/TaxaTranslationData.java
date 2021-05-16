package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class TaxaTranslationData {

    @Id (autoincrement = true)
    private Long Id;
    private Long taxonId;
    private String locale;
    private String nativeName;
    private String description;

    @Generated(hash = 559793365)
    public TaxaTranslationData(Long Id, Long taxonId, String locale, String nativeName, String description) {
        this.Id = Id;
        this.taxonId = taxonId;
        this.locale = locale;
        this.nativeName = nativeName;
        this.description = description;
    }

    @Generated(hash = 43108861)
    public TaxaTranslationData() {
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long taxonId) {
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

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
