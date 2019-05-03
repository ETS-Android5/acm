package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.AbstractManualMatcherDialog;

public class ManualMatcherDialog extends AbstractManualMatcherDialog<GreetingMatchable> {

    @Override
    protected String leftDescription() {
        return "Recipient";
    }

    @Override
    protected String rightDescription() {
        return "Greeting";
    }
}
