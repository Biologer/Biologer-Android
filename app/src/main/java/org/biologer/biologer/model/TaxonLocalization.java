package org.biologer.biologer.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by Miloš Popović on 2.3.2019.
 */
@Entity
public class TaxonLocalization {

    @Id (autoincrement = true)
    private Long id;
    private String name;
    private long taxonId;
    private long translationID;
    private String locale;
    private String nativeName;
    private String latinAndNativeName;
    @Generated(hash = 1649918886)
    public TaxonLocalization(Long id, String name, long taxonId, long translationID, String locale, String nativeName,
            String latinAndNativeName) {
        this.id = id;
        this.name = name;
        this.taxonId = taxonId;
        this.translationID = translationID;
        this.locale = locale;
        this.nativeName = nativeName;
        this.latinAndNativeName = latinAndNativeName;
    }
    @Generated(hash = 1201112011)
    public TaxonLocalization() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getTaxonId() {
        return this.taxonId;
    }
    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }
    public long getTranslationId() {
        return this.translationID;
    }
    public void setTranslationId(long translationID) {
        this.translationID = translationID;
    }
    String getLocale() { return this.locale; }
    void setLocale(String locale) {
        this.locale = locale;
    }
    long getTranslationID() {
        return this.translationID;
    }
    void setTranslationID(long translationID) {
        this.translationID = translationID;
    }
    String getNativeName() {
        return this.nativeName;
    }
    void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }
    public String getLatinAndNativeName() {
        return this.latinAndNativeName;
    }
    void setLatinAndNativeName(String latinAndNativeName) {
        this.latinAndNativeName = latinAndNativeName;
    }
    }

