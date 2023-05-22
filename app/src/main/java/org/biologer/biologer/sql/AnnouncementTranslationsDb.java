package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class AnnouncementTranslationsDb {

    @Id(assignable = true)
    long id;
    private long announcementId;
    private String locale;
    private String title;
    private String message;

    public AnnouncementTranslationsDb(long id, long announcementId,
                                      String locale, String title, String message) {
        this.id = id;
        this.announcementId = announcementId;
        this.locale = locale;
        this.title = title;
        this.message = message;
    }

    public long getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(long announcementId) {
        this.announcementId = announcementId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
