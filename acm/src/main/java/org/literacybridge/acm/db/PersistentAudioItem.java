package org.literacybridge.acm.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.AudioItemCache;
import org.literacybridge.acm.index.AudioItemIndex;

@Entity
@NamedQueries({
    @NamedQuery(name = "PersistentAudioItem.findAll", query = "select o from PersistentAudioItem o")
})
@Table(name = "t_audioitem")
class PersistentAudioItem extends PersistentObject {

    private static final long serialVersionUID = 6523719801839346881L;

    private static final String COLUMN_VALUE = "gen_audioitem";

    @TableGenerator(name = COLUMN_VALUE,
            table = PersistentObject.SEQUENCE_TABLE_NAME,
            pkColumnName = PersistentObject.SEQUENCE_KEY,
            valueColumnName = PersistentObject.SEQUENCE_VALUE,
            pkColumnValue = COLUMN_VALUE,
            allocationSize = PersistentObject.ALLOCATION_SIZE)
    @Column(name = "id", nullable = false)
    @Id @GeneratedValue(generator = COLUMN_VALUE)
    private Integer id;

    @Column(name="uuid")
    private String uuid;

    @ManyToMany
    @JoinTable(
            name = "t_audioitem_has_category",
            joinColumns =
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "category", referencedColumnName = "id")
            )
    private Set<PersistentCategory> persistentCategoryList = new LinkedHashSet<PersistentCategory>();

    @ManyToMany
    @JoinTable(
            name = "t_audioitem_has_tag",
            joinColumns =
            @JoinColumn(name = "audioitem", referencedColumnName = "id"),
            inverseJoinColumns =
            @JoinColumn(name = "tag", referencedColumnName = "id")
            )
    private Set<PersistentTag> persistentTagList = new LinkedHashSet<PersistentTag>();


    @OneToMany(mappedBy = "persistentAudioItem", cascade = {CascadeType.ALL})
    private List<PersistentLocalizedAudioItem> persistentLocalizedAudioItemList = new ArrayList<PersistentLocalizedAudioItem>();

    public PersistentAudioItem() {
        persistentLocalizedAudioItemList.add(new PersistentLocalizedAudioItem());
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        getPersistentLocalizedAudioItem().setUuid(uuid);
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Collection<PersistentCategory> getPersistentCategoryList() {
        return persistentCategoryList;
    }

    public PersistentCategory addPersistentAudioItemCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().add(persistentCategory);
        return persistentCategory;
    }

    public boolean hasPersistentAudioItemCategory(PersistentCategory persistentCategory) {
        return getPersistentCategoryList().contains(persistentCategory);
    }

    public PersistentCategory removePersistentCategory(PersistentCategory persistentCategory) {
        getPersistentCategoryList().remove(persistentCategory);
        return persistentCategory;
    }

    public void removeAllPersistentCategories() {
        getPersistentCategoryList().clear();
    }

    public Collection<PersistentTag> getPersistentTagList() {
        return persistentTagList;
    }

    public PersistentTag addPersistentAudioItemTag(PersistentTag persistentTag) {
        if (!getPersistentTagList().contains(persistentTag)) {
            getPersistentTagList().add(persistentTag);
            persistentTag.getPersistentAudioItemList().add(this);
        }
        return persistentTag;
    }

    public boolean hasPersistentAudioItemTag(PersistentTag persistentTag) {
        return getPersistentTagList().contains(persistentTag);
    }

    public PersistentTag removePersistentTag(PersistentTag persistentTag) {
        getPersistentTagList().remove(persistentTag);
        persistentTag.getPersistentAudioItemList().remove(this);
        return persistentTag;
    }

    public void removeAllPersistentTags() {
        for (PersistentTag tag : getPersistentTagList()) {
            tag.getPersistentAudioItemList().remove(this);
        }
        getPersistentTagList().clear();
    }

    public PersistentLocalizedAudioItem getPersistentLocalizedAudioItem() {
        return persistentLocalizedAudioItemList.get(0);
    }

    @Override
    protected void afterCommitHook() {
        DBConfiguration db = ACMConfiguration.getCurrentDB();
        if (db != null) {
            AudioItemIndex index = db.getAudioItemIndex();
            if (index != null) {
                try {
                    index.updateAudioItem(new DBAudioItem(this));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            AudioItemCache cache = db.getAudioItemCache();
            if (cache != null) {
                cache.invalidate(getUuid());
            }
        }
    }

    public static List<PersistentAudioItem> getFromDatabase() {
        return PersistentQueries.getPersistentObjects(PersistentAudioItem.class);
    }

    public static PersistentAudioItem getFromDatabase(int id) {
        return PersistentQueries.getPersistentObject(PersistentAudioItem.class, id);
    }

    public static PersistentAudioItem getFromDatabase(String uuid) {
        EntityManager em = ACMConfiguration.getCurrentDB().getEntityManager();
        PersistentAudioItem result = null;
        try {
            Query findObject = em.createQuery("SELECT o FROM PersistentAudioItem o WHERE o.uuid = '" + uuid + "'");
            result = (PersistentAudioItem) findObject.getSingleResult();
        } catch (NoResultException e) {
            // do nothing
        } finally {
            em.close();
        }
        return result;
    }
}
