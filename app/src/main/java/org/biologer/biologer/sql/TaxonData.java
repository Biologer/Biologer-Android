package org.biologer.biologer.sql;

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
    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }
    public String getLocale() { return this.locale; }
    public void setLocale(String locale) {
        this.locale = locale;
    }
    public String getNativeName() {
        return this.nativeName;
    }
    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getLatinNativeNames() {
        if (getNativeName() == null) {
            return getLatinName();
        }
        return getLatinName() + " (" + getNativeName() + ")";
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

    public void setUseAtlasCode(boolean useAtlasCode) {
        this.useAtlasCode = useAtlasCode;
    }

    public String getAncestorNames() {
        return ancestorNames;
    }

    public void setAncestorNames(String ancestorNames) {
        this.ancestorNames = ancestorNames;
    }
}

