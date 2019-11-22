package org.literacybridge.acm.cloud;

import org.json.simple.JSONObject;
import org.literacybridge.acm.config.ACMConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TbSrnHelper {
    private static final String TBL_INFO_NAME = "tbsrnstore.info";
    private static final File tblInfoFile = new File(ACMConfiguration.getInstance()
        .getApplicationHomeDirectory(), TBL_INFO_NAME);
    private static final File tblInfoFileNew = new File(ACMConfiguration.getInstance()
        .getApplicationHomeDirectory(), TBL_INFO_NAME+".new");

    private static final int BLOCK_SIZE = 4;

    private static final String ID_NAME = "tbloaderid";
    private static final String HEXID_NAME = "tbloaderidhex";
    private static final String NEXTSRN_NAME = "nextsrn";
    private static final String PRIMARY_BEGIN_NAME = "primarybegin";
    private static final String PRIMARY_END_NAME = "primaryend";
    private static final String BACKUP_BEGIN_NAME = "backupbegin";
    private static final String BACKUP_END_NAME = "backupend";

    public static class TbSrnInfo {
        int tbloaderid = -1;
        String tbloaderidHex = null;
        int nextSrn;
        int primaryBegin;
        int primaryEnd;
        int backupBegin;
        int backupEnd;

        TbSrnInfo(int tbloaderid,
            String tbloaderidHex,
            int nextSrn,
            int primaryBegin,
            int primaryEnd,
            int backupBegin,
            int backupEnd)
        {
            this.tbloaderid = tbloaderid;
            this.tbloaderidHex = tbloaderidHex;
            this.nextSrn = nextSrn;
            this.primaryBegin = primaryBegin;
            this.primaryEnd = primaryEnd;
            this.backupBegin = backupBegin;
            this.backupEnd = backupEnd;
        }

        /**
         * Constructor from another TbSrnInfo object.
         * @param other the other TbSrnInfo object.
         */
        TbSrnInfo(TbSrnInfo other) {
            if (other != null) {
                this.tbloaderid = other.tbloaderid;
                this.tbloaderidHex = other.tbloaderidHex;
                this.nextSrn = other.nextSrn;
                this.primaryBegin = other.primaryBegin;
                this.primaryEnd = other.primaryEnd;
                this.backupBegin = other.backupBegin;
                this.backupEnd = other.backupEnd;
            }
        }

        /**
         * Sets the id of this block of SRNs.
         * @param tbloaderid The tbloader id, as an integer.
         * @param tbloaderidHex The tbloader id as a hex string. No leading 0x.
         */
        void setId(int tbloaderid, String tbloaderidHex) {
            this.tbloaderid = tbloaderid;
            this.tbloaderidHex = tbloaderidHex;
        }

        /**
         * Adds a block of available SRNs. Updates the tbloader id, if it has changed (that can
         * happen if we exhause the SRNs for a tbloader id).
         * @param srnAllocation A batch of reserved tbloader ids.
         */
        boolean applyReservation(Map<String,Object> srnAllocation) {
            long begin_l = (Long)srnAllocation.getOrDefault("begin", -1);
            int begin = (int)begin_l;
            long end_l = (Long)srnAllocation.getOrDefault("end", -1);
            int end = (int)end_l;
            long id_l = (Long)srnAllocation.getOrDefault("id", -1);
            int id = (int)id_l;
            String hexid = (String)srnAllocation.getOrDefault("hexid", "");
            if (begin > 0 && end > begin) {
                if (id != tbloaderid) {
                    setId(id, hexid);
                }
                if (primaryBegin == 0 && backupBegin == 0) {
                    // If we have neither block, split the block between primary and backup
                    primaryBegin = begin;
                    primaryEnd = begin + ((end - begin) / 2);
                    backupBegin = primaryEnd;
                    backupEnd = end;
                    nextSrn = primaryBegin;
                } else if (primaryBegin == 0) {
                    // else if we have no primary block, save block as primary
                    primaryBegin = begin;
                    primaryEnd = end;
                    nextSrn = primaryBegin;
                } else if (backupBegin == 0) {
                    // else if we have no backup block, save block as backup
                    backupBegin = begin;
                    backupEnd = end;
                } else {
                    throw new IllegalStateException("Attempt to fill block, but no empty blocks.");
                }
                return true;
            }
            return false;
        }

        /**
         * Query if there is an available SRN.
         * @return true if there is an available SRN.
         */
        boolean hasNext() {
            return (primaryBegin > 0 && primaryEnd > primaryBegin 
                && nextSrn >= primaryBegin && nextSrn < primaryEnd);
        }

        /**
         * Query if there is a backup block.
         * @return true if there is a backup block.
         */
        boolean hasBackup() {
            return (backupBegin > 0 && backupEnd > backupBegin);
        }

        /**
         * Allocate the next SRN. If the allocation exhausts the primary block, switch the
         * backup block. If the backup block is empty, no further allocations will be possible.
         * @return The next srn.
         */
        int allocateNext() {
            int next = 0;
            if (primaryBegin > 0 && primaryEnd > primaryBegin && nextSrn >= primaryBegin
                && nextSrn < primaryEnd) {
                next = nextSrn++;
                // Did that exhaust the primary block?
                if (nextSrn >= primaryEnd) {
                    // Yes, promote backup to primary
                    primaryBegin = backupBegin;
                    primaryEnd = backupEnd;
                    backupBegin = 0;
                    backupEnd = 0;
                    nextSrn = primaryBegin;
                }
            }
            return next;
        }

        /**
         * Query the number of available SRNs.
         * @return the number of available SRNs.
         */
        int available() {
            int available = 0;
            if (primaryBegin > 0 && primaryEnd > primaryBegin) {
                available += primaryEnd - primaryBegin;
            }
            if (backupBegin > 0 && backupEnd > backupBegin) {
                available += backupEnd - backupBegin;
            }
            return available;
        }

        public String getTbloaderidHex() {
            return tbloaderidHex;
        }
    }

    private final Authenticator authInstance = Authenticator.getInstance();

    private final String email;
    private Properties tbSrnStore;
    private TbSrnInfo tbSrnInfo;

    TbSrnHelper(String email) {
        this.email = email;
        tbSrnStore = loadPropertiesFile();
        tbSrnInfo = loadSrnInfo();
    }

    /**
     * Prepare to allocate TB SRNs. Things to check:
     * - if we have no local tbLoaderId, we have no local block(s) of allocated SRNs, and we
     *   need to try to get two blocks.
     * - if we have a local tbLoaderId, get the associated tblInfo. If there is no primary
     *   block, try to promote the backup to primary.
     * - if we have no primary or backup block, try to allocate whichever is missing.
     * - if we changed anything, persist the properties file
     *
     * @return the number of available tb srns.
     */
    public int prepareForAllocation() {
        if (tbSrnInfo == null || tbSrnInfo.primaryBegin == 0 || tbSrnInfo.backupBegin == 0) {
            int nBlocks = (tbSrnInfo ==null || (
                tbSrnInfo.primaryBegin ==0 && tbSrnInfo.backupBegin ==0)) ? 2 : 1;
            TbSrnInfo newTbSrnInfo = new TbSrnInfo(tbSrnInfo);
            // We need at least one block of numbers.
            if (authInstance.isAuthenticated() && authInstance.isOnline()) {
                Map<String,Object> srnAllocation = allocateTbSrnBlock(BLOCK_SIZE * nBlocks);
                if (newTbSrnInfo.applyReservation(srnAllocation)) {
                    // We've successfully allocated a new block of SRNs. Try to persist it.
                    Properties newTbSrnStore = saveSrnInfo(newTbSrnInfo);
                    if (storePropertiesFile(newTbSrnStore)) {
                        // Successfully persisted, so publish to the rest of the app.
                        this.tbSrnInfo = newTbSrnInfo;
                        this.tbSrnStore = newTbSrnStore;
                    }
                } // begin > 0 && end > begin
            } // isAuthenticated
        }

        return tbSrnInfo != null ? tbSrnInfo.available() : 0;
    }

    /**
     * Query whether an SRN can be allocated.
     * @return true if there is any available SRN.
     */
    public boolean hasAvailableSrn() {
        return tbSrnInfo != null && tbSrnInfo.hasNext();
    }

    /**
     * Allocate the next SRN, if possible. The TbSrnInfo must be successfully written
     * to disk first.
     * @return the next SRN, or 0 if none is available.
     */
    public int allocateNextSrn() {
        int allocated = 0;
        if (tbSrnInfo != null && tbSrnInfo.hasNext()) {
            TbSrnInfo newTbSrnInfo = new TbSrnInfo(tbSrnInfo);
            int next = newTbSrnInfo.allocateNext();
            // If we don't have a backup block, and we're authenticated & online, try to get one now.
            if (!newTbSrnInfo.hasBackup() && authInstance.isAuthenticated() && authInstance.isOnline()) {
                Map<String, Object> srnAllocation = allocateTbSrnBlock(BLOCK_SIZE);
                newTbSrnInfo.applyReservation(srnAllocation);
            }
            // Persist to disk before we return to caller.
            Properties newTbLoaderInfo = saveSrnInfo(newTbSrnInfo);
            if (storePropertiesFile(newTbLoaderInfo)) {
                tbSrnInfo = newTbSrnInfo;
                tbSrnStore = newTbLoaderInfo;
                allocated = next;
            }
        }
        return allocated;
    }

    public TbSrnInfo getTbSrnInfo() {
        return new TbSrnInfo(this.tbSrnInfo);
    }

    /**
     * Extract, for the current user, the TbSrnInfo from the TbSrnStore (ie, from the
     * properties file).
     * @return the TbSrnInfo for the current user, or null if there is none or it can't be read.
     */
    public TbSrnInfo loadSrnInfo() {
        TbSrnInfo tbSrnInfo;
        if (tbSrnStore == null) return null;
        int tbLoaderId = getTbLoaderId();
        if (tbLoaderId <= 0) return null;
        String prefix = String.format("%d.", tbLoaderId);

        int tbloaderid = Integer.parseInt(tbSrnStore.getProperty(prefix + ID_NAME));
        String tbloaderidHex = tbSrnStore.getProperty(prefix + HEXID_NAME);
        int nextSrn = Integer.parseInt(tbSrnStore.getProperty(prefix + NEXTSRN_NAME));
        int primaryBase = Integer.parseInt(tbSrnStore.getProperty(prefix + PRIMARY_BEGIN_NAME));
        int primaryMax = Integer.parseInt(tbSrnStore.getProperty(prefix + PRIMARY_END_NAME));
        int backupBase = Integer.parseInt(tbSrnStore.getProperty(prefix + BACKUP_BEGIN_NAME));
        int backupMax = Integer.parseInt(tbSrnStore.getProperty(prefix + BACKUP_END_NAME));

        assert(String.format("%04x", tbloaderid).equalsIgnoreCase(tbloaderidHex));
        assert(nextSrn >= primaryBase && nextSrn < primaryMax);
        assert((backupBase < backupMax) || (backupBase == 0 && backupMax == 0));

        tbSrnInfo = new TbSrnInfo(tbloaderid,
            tbloaderidHex,
            nextSrn,
            primaryBase,
            primaryMax,
            backupBase,
            backupMax);
        return tbSrnInfo;
    }

    /**
     * Saves the given TbSrnInfo into a clone of the TbSrnStore. This lets us save the updated
     * properties to disk before exposing them to the application.
     * @param tbSrnInfo to be saved.
     * @return a new Properties that is a clone of the existing TbSrnStore updated with the TbSrnInfo.
     */
    private Properties saveSrnInfo(TbSrnInfo tbSrnInfo) {
        Properties newTbLoaderInfo = this.tbSrnStore == null ? new Properties() : (Properties)this.tbSrnStore
            .clone();
        
        int tbLoaderId = tbSrnInfo.tbloaderid;
        String prefix = String.format("%d.", tbLoaderId);

        // Associate the email address with the tbloader id
        newTbLoaderInfo.setProperty(email, String.valueOf(tbLoaderId));

        newTbLoaderInfo.setProperty(prefix + ID_NAME, String.valueOf(tbSrnInfo.tbloaderid));
        newTbLoaderInfo.setProperty(prefix + HEXID_NAME, tbSrnInfo.tbloaderidHex);
        newTbLoaderInfo.setProperty(prefix + NEXTSRN_NAME, String.valueOf(tbSrnInfo.nextSrn));
        newTbLoaderInfo.setProperty(prefix + PRIMARY_BEGIN_NAME, String.valueOf(tbSrnInfo.primaryBegin));
        newTbLoaderInfo.setProperty(prefix + PRIMARY_END_NAME, String.valueOf(tbSrnInfo.primaryEnd));
        newTbLoaderInfo.setProperty(prefix + BACKUP_BEGIN_NAME, String.valueOf(tbSrnInfo.backupBegin));
        newTbLoaderInfo.setProperty(prefix + BACKUP_END_NAME, String.valueOf(tbSrnInfo.backupEnd));
        
        return newTbLoaderInfo;
    }

    /**
     * Gets the TB-Loader ID for the current user (by email).
     * @return the TB-Loader id, or -1 if none or can't be read.
     */
    private int getTbLoaderId() {
        int id = -1;
        if (tbSrnStore != null) {
            String tbloaderId = tbSrnStore.getProperty(email, "-1");
            try {
                id = Integer.parseInt(tbloaderId);
            } catch (NumberFormatException ignored) {
                // Ignore and keep -1
            }
        }
        return id;
    }

    /**
     * Loads the TbSrnStore from the properties file.
     * @return the Properties object, or null if none or it can't be read.
     */
    private Properties loadPropertiesFile() {
        Properties tbLoaderInfo = null;
        if (tblInfoFile.exists()) {
            Properties newTbLoaderInfo = new Properties();
            try (FileInputStream fis = new FileInputStream(tblInfoFile);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                newTbLoaderInfo.load(isr);
                tbLoaderInfo = newTbLoaderInfo;
            } catch (Exception ignored) {
                // ignore and return null
            }
        }
        return tbLoaderInfo;
    }

    /**
     * Store the given properties file to the TBL_INFO_NAME file (known internally as TbSrnStore).
     * @param tbLoaderInfo the Properties to write.
     * @return true if it was successfully saved, false otherwise.
     */
    private boolean storePropertiesFile(Properties tbLoaderInfo) {
        boolean ok = false;
        if (tblInfoFileNew.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tblInfoFileNew.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(tblInfoFileNew);
            OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            tbLoaderInfo.store(osw, null);
            osw.flush();
            osw.close();
            //noinspection ResultOfMethodCallIgnored
            tblInfoFile.delete();
            ok = tblInfoFileNew.renameTo(tblInfoFile);
        } catch (IOException ignored) {
            // Ignore and keep "false"
        }
        return ok;
    }

    /**
     * Make a call to reserve a block of serial numbers.
     * @param n Number of SRNs to request.
     * @return a Map of the returned result.
     */
    private Map<String, Object> allocateTbSrnBlock(int n) {
        Map<String,Object> result = new HashMap<>();

        String baseURL = "https://lj82ei7mce.execute-api.us-west-2.amazonaws.com/Prod";
        String requestURL = baseURL + "/reserve";
        if (n > 0) requestURL += "?n="+String.valueOf(n);

        JSONObject jsonResponse = authInstance.authenticatedRestCall(requestURL);

        if (jsonResponse != null) {
            Object o = jsonResponse.get("result");
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,Object> tblMap = (Map<String,Object>) o;
                for (Map.Entry<String,Object> e : tblMap.entrySet())
                    result.put(e.getKey(), e.getValue());
            }
        }

        return result;
    }

}
