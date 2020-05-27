package org.biologer.biologer.sql;

import org.biologer.biologer.Localisation;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by Miloš Popović on 2.3.2019.
 */
@Entity
public class TaxonData {

    @Id (autoincrement = true)
    private Long id;
    private Long taxonId;
    private String latinName;
    private boolean useAtlasCode;
    private String ancestorNames;
    private String locale;
    private String nativeName;
    @Generated(hash = 1218675374)
    public TaxonData(Long id, Long taxonId, String latinName, boolean useAtlasCode,
            String ancestorNames, String locale, String nativeName) {
        this.id = id;
        this.taxonId = taxonId;
        this.latinName = latinName;
        this.useAtlasCode = useAtlasCode;
        this.ancestorNames = ancestorNames;
        this.locale = locale;
        this.nativeName = nativeName;
    }
    @Generated(hash = 345389106)
    public TaxonData() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLatinName() {
        return this.latinName;
    }

    void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public String getLocale() { return this.locale; }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getNativeName() {
        return this.nativeName;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long taxonId) {
        this.taxonId = taxonId;
    }

    public boolean getUseAtlasCode() {
        return useAtlasCode;
    }

    void setUseAtlasCode(boolean useAtlasCode) {
        this.useAtlasCode = useAtlasCode;
    }

    String getAncestorNames() {
        return ancestorNames;
    }

    void setAncestorNames(String ancestorNames) {
        this.ancestorNames = ancestorNames;
    }
    void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }
}

