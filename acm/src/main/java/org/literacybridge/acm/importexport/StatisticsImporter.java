package org.literacybridge.acm.importexport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;

public class StatisticsImporter {
    public void importStatsFolder(MetadataStore store, File folder) throws IOException {
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                if (name.toLowerCase().endsWith(".csv")) {
                    return true;
                }

                return false;
            }
        });

        for (File file : files) {
            importStatsFile(store, file);
        }
    }

    public void importStatsFile(MetadataStore store, File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> tokens = tokenizeCSV(reader.readLine());
        String deviceId = tokens.get(0);
        int bootCycleNumber = Integer.parseInt(tokens.get(1));

        while(reader.ready()) {
            tokens = tokenizeCSV(reader.readLine());
            String audioItemID = tokens.get(0);
            AudioItem audioItem = store.getAudioItem(audioItemID);
            if (audioItem != null) {
                Metadata metadata = audioItem.getMetadata();

                metadata.setStatistic(MetadataSpecification.LB_OPEN_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(1)));
                metadata.setStatistic(MetadataSpecification.LB_COMPLETION_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(2)));
                metadata.setStatistic(MetadataSpecification.LB_COPY_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(3)));
                metadata.setStatistic(MetadataSpecification.LB_SURVEY1_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(4)));
                metadata.setStatistic(MetadataSpecification.LB_APPLY_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(5)));
                metadata.setStatistic(MetadataSpecification.LB_NOHELP_COUNT, deviceId,
                        bootCycleNumber, Integer.parseInt(tokens.get(6)));

                metadata.commit();
            }
        }
    }

    private final List<String> tokenizeCSV(String line) {
        final StringTokenizer tokenizer = new StringTokenizer(line, " ,");
        List<String> list = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        return list;
    }
}
