package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;

import java.util.HashMap;
import java.util.Map;

public class ContentImportAssistant {

    public static Assistant<ContentImportContext> create() {
        // create the AssistantContainer:

        ContentImportContext context = new ContentImportContext();
        // Debugging & development:
        if (ACMConfiguration.isTestData()) {
            context.deploymentNo = 1;
            context.languagecode = "en";
        }

        Map<String, Object> props = new HashMap<>();
        Assistant<ContentImportContext> assistant = new Assistant.Factory<ContentImportContext>()
            .withContext(context)
            .withPageFactories(WelcomePage::new,
                FilesPage::new,
                MatchPage::new,
//                ManualMatchPage::new,
                ReviewPage::new,
                ImportedPage::new)
            .withTitle("Content Import Assistant")
            .create();

        return assistant;
    }

}
