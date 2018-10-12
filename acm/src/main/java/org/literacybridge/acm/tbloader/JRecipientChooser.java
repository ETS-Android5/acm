package org.literacybridge.acm.tbloader;

import com.google.common.collect.ImmutableBiMap;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;

/**
 * A Swing component to choose a recipient by country/region/district/community/group/supportentity-group.
 *
 * Smart about the fields displayed and enabled, so that user only needs to chose where there are
 * actually multiple items to choose from.
 */
public class JRecipientChooser extends JPanel {

    private boolean haveChoice = false;

    // For the case with a recipients list.
    private ProgramSpec programSpec;
    private RecipientList recipients;
    private int maxHierarchy = -1;
    private final List<String> selections = new ArrayList<>();
    private final List<JComboBox<String>> choosers = new ArrayList<>();

    // For the case with only a list of filenames.
    private FilteringComboBoxModel<String> communityModel;
    private JTextField communityFilter;
    private JComboBox<String> communityChooser;
    private String selectedCommunity;


    JRecipientChooser() {
        super(new GridBagLayout());
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class,l);
    }

    /**
     * Populates the recipient chooser from the program spec and list of files.
     * @param programSpec for the project
     * @param files in the communities directory
     */
    void populate(ProgramSpec programSpec, File[] files) {
        this.programSpec = programSpec;
        clear();
        RecipientList recipients = programSpec.getRecipients();
        if (recipients != null)
            populateRecipients(recipients);
        else
            populateCommunities(files);
    }

    /**
     * Returns the directory for the selected community. If the community was selected via a
     * RecipientList, then the directory is looked up in the recipient map from the recipient id.
     *
     * If the community was selected via a list of directories, the directory is selected
     * directly.
     */
    String getCommunityDirectory() {
        if (!haveChoice) {
            return null;
        }
        // Selection by community directory?
        if (selectedCommunity != null) {
            return selectedCommunity;
        }
        // Selection by recipient hierarchy.
        Recipient recipient = recipients.getRecipient(selections);
        Map<String,String> recipientMap = programSpec.getRecipientsMap();
        return recipientMap.get(recipient.recipientid);
    }

    void setSelectedCommunity(String community, String recipientid) {
        if (programSpec == null) return;
        boolean didSelect = false;
        
        if (communityChooser != null) {
            int count = communityChooser.getItemCount();
            for (int i = 0; i < count; i++) {
                if (communityChooser.getItemAt(i).equalsIgnoreCase(community)) {
                    communityChooser.setSelectedIndex(i);
                    selectedCommunity = communityChooser.getSelectedItem().toString();
                    didSelect = true;
                    break;
                }
            }
        } else {
            if (recipientid == null) {
                ImmutableBiMap<String, String> dir2id = new ImmutableBiMap.Builder<String, String>()
                    .putAll(programSpec.getRecipientsMap())
                    .build()
                    .inverse();
                recipientid = dir2id.get(community);
            }
            List<String> path = recipients.getPath(recipientid);
            // If we have a full path...
            if (path.size() == maxHierarchy+1) {
                didSelect = setSelectionWithPath(path);
            } else {
                // Otherwise, just reset to the base level.
                didSelect = false;
                fillChoosers(0);
            }
        }
        setDone(didSelect);
    }

    private void clear() {
        for (JComboBox<String> chooser : choosers) remove(chooser);
        choosers.clear();
        selections.clear();

        if (communityFilter != null) remove(communityFilter);
        if (communityChooser != null) remove(communityChooser);
        communityFilter = null;
        communityChooser = null;
        selectedCommunity = null;
    }

    private void populateCommunities(File[] communityDirs) {
        setDone(false);

        GridBagConstraints c;
        int y=0;

        c = gbc(y++, false);
        c.weightx = 1;
        communityModel = new FilteringComboBoxModel<>();
        communityFilter = new JTextField("", 40);
        communityFilter.getDocument().addDocumentListener(communityFilterListener);
        PromptSupport.setPrompt("Type to filter...", communityFilter);
        Box box = Box.createHorizontalBox();
        box.add(communityFilter);
        box.add(Box.createHorizontalGlue());
        add(communityFilter, c);

        // Select community.
        c = gbc(y++, true);
        communityChooser = new JComboBox<>(communityModel);
        communityChooser.setRenderer(new MyComboBoxRenderer(communityChooser, "Choose community", ""));
        communityChooser.addActionListener(this::communityActionListener);
        box = Box.createHorizontalBox();
        box.add(communityChooser);
        box.add(Box.createHorizontalGlue());
        add(communityChooser, c);

        List<String> names = Arrays.stream(communityDirs)
            .map(File::getName)
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
        for (String name : names) {
            communityChooser.addItem(name);
        }
        communityChooser.setSelectedItem(-1);
    }

    /**
     * Given a RecipientList, create the combo boxes for each level in the hierarchy.
     * @param recipientList to be navigated.
     */
    private void populateRecipients(RecipientList recipientList) {
        this.recipients = recipientList;
        this.maxHierarchy = recipients.getMaxLevel();
        GridBagConstraints c;

        setDone(false);

        int y = 0;

        for (int ix=0; ix<=maxHierarchy; ix++) {
            c = gbc(y++, ix==maxHierarchy);
            c.weightx = 1;
            // Use a vertical box to hold the prompt and the combo box.
            Box stack = Box.createVerticalBox();
            // Make a prompt for the level. Use a small text.
            Box box = Box.createHorizontalBox();
            JLabel label = new JLabel(recipients.getNameOfLevel(ix));
            label.setFont(new Font("Sans Serif", Font.ITALIC, 10));
            box.add(label);
            box.add(Box.createHorizontalGlue()); // absorbs any extra width.
            stack.add(box);

            // Make the combo box chooser for the level.
            box = Box.createHorizontalBox();
            JComboBox<String> chooser = new JComboBox<>();
            choosers.add(chooser);
            selections.add("");
            chooser.setRenderer(new MyComboBoxRenderer(ix));
            IndexedActionListener listener = new IndexedActionListener(ix);
            chooser.addActionListener(listener);
            box.add(chooser);
            stack.add(box);

            this.add(stack, c);
        }

        fillChoosers(0);
    }

    /**
     * The constraints that we're using.
     * @param y Line for the constraints.
     * @param last If the last line, add a little extra inset on the bottom.
     * @return The constraints.
     */
    private GridBagConstraints gbc(int y, boolean last) {
        Insets zi = new Insets(0, 3, (last?2:0), 2);
        return new GridBagConstraints(0, y, 1, 1, 0,
            0, LINE_START, HORIZONTAL, zi, 0, 0);
    }

   /**
     * Sets whether a selection is complete. When the selection is NOT complete, the component
     * is drawn with a red border. When it IS complete, the border turns black.
     * @param done Is the selection done?
     */
    private void setDone(boolean done) {
        boolean changed = this.haveChoice != done;
        this.haveChoice = done;
        if (done) {
            //Color doneBorderColor = new Color(0, 0, 0, 0);
            Color doneBorderColor = Color.gray;
            setBorder(new LineBorder(doneBorderColor));
        } else {
            setBorder(new LineBorder(Color.RED));
        }
        // Don't keep sending updates when nothing's selected.
        if (this.haveChoice || changed) {
            fireActionEvent();
        }
    }

    private boolean setSelectionWithPath(List<String> path) {
        boolean success = true;
        for (int level=0; level<=maxHierarchy; level++) {
            // Get the values at this level.
            Vector<String> values = getFilteredValues(level);
            // If the value that we were given for this level isn't in the list of available
            // values, stop looking.
            int valueIx = values.indexOf(path.get(level));
            if (valueIx < 0) {
                success = false;
                break;
            }
            // Put the values into the combo box for this level.
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(values);
            JComboBox<String> chooser = choosers.get(level);
            chooser.setModel(model);
            // Select it.
            chooser.setSelectedIndex(valueIx);
            // Enable selection if there is more than one to choose from.
            chooser.setEnabled(values.size() > 1);
        }
        if (success) {
            setDone(true);
        } else {
            fillChoosers(0);
        }
        return success;
    }

    /**
     * Fills the chooser at the given level, from recipients. If there's only one item, auto-
     * select it, and fill the next level. If there are multiple things from which to choose,
     * clear the levels below this, so there's no stale values.
     * @param levelToFill The level we wish to fill.
     */
    private void fillChoosers(int levelToFill) {
        // Get the values for the level.
        Vector<String> values = getFilteredValues(levelToFill);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(values);
        // Put them into the combo box, and enable it.
        JComboBox<String> chooser = choosers.get(levelToFill);
        chooser.setModel(model);
        // If there's only one value, auto-select it.
        if (values.size() == 1) {
            // There's nothing to choose here, so disable / dim the combo box.
            chooser.setEnabled(false);
            selections.set(levelToFill, values.get(0));
            // If there is a next level, repeat the process there...
            if (levelToFill < maxHierarchy) {
                fillChoosers(levelToFill+1);
            } else {
                // No more levels; we have our selection.
                setDone(true);
            }
        } else {
            // Need to make a choice here before we can see anything below. Clear lower levels.
            chooser.setEnabled(true);
            chooser.setSelectedIndex(-1);
            setDone(false);
            clearChoosers(levelToFill+1);
        }
    }

    /**
     * Clears the choosers, starting at the given level.
     * @param levelToClear starting level.
     */
    private void clearChoosers(int levelToClear) {
        for (; levelToClear <= maxHierarchy; levelToClear++) {
            JComboBox<String> chooser = choosers.get(levelToClear);
            chooser.removeAllItems();
            chooser.setEnabled(false);
        }
    }

    private Vector<String> getFilteredValues(int level) {
        return new Vector<>(recipients.getChildrenOfPath(selections.subList(0, level)));
    }

    /**
     * An ActionListener class that knows what level it applies to.
     */
    private class IndexedActionListener implements ActionListener {
        int n;
        IndexedActionListener(int n) {
            this.n = n;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<String> c = choosers.get(n);
            Object selected = c.getSelectedItem();
            if (selected == null) return;
            String s = selected.toString();

            // We have a selection. Remember it.
            selections.set(n, s);

            // If there are further levels, fill them appropriately.
            if (n < maxHierarchy) {
                fillChoosers(n+1);
            } else {
                // Otherwise we have our choice.
                setDone(true);
            }
        }
    }

    /**
     * Renderer for various levels. When nothing is selected, prompts user to "Select a Whatever".
     * When displaying an empty thing, displays it as "-- No Things --".
     */
    private class MyComboBoxRenderer extends DefaultListCellRenderer // JLabel implements ListCellRenderer
    {
        private int level = -1;
        private String prompt;
        private String empty;
        private Component component;
        // We can determine that we want to format an item in italic at a different place than we
        // actually do the formatting.
        private boolean wantItalic = false;

        MyComboBoxRenderer(Component component, String prompt, String emptyValue) {
            this.component = component;
            this.prompt = prompt;
            this.empty = emptyValue;
        }

        MyComboBoxRenderer(int level)
        {
            this.level = level;
            this.component = choosers.get(level);
            this.prompt = String.format("Select %s...", recipients.getSingular(level));
            this.empty = String.format("-- No %s --", recipients.getNameOfLevel(level));
        }

        /**
         * How many TBs are in the given entity?
         * @param entity for which to get the number.
         * @return the number, as a string.
         */
        String getNumTbsLabel(String entity) {
            if (level < 0) return "";
            List<String> path = new ArrayList<>();
            path.addAll(selections.subList(0, level));
            path.add(entity);
            int n = recipients.getNumTbs(path);
            return String.format(" (%d TB%s)", n, n==1?"":"s");
        }

        /**
         * This is because the disabled combo box is too dim to easily read on Windows.
         * Here we choose a somewhat darker text color. Note that future L&F changes could
         * break this, but Swing L&F work seems to be pretty dead, so it's unlikely.
         *
         * This is also apparently where we have to set the font, to affect the box itself
         * (not the list).
         */
        @Override
        public void paint(Graphics g) {
            if (!component.isEnabled()) {
                setForeground(Color.gray);
            }
            if (wantItalic) {
                setFont(new Font(getFont().getName(), Font.ITALIC, getFont().getSize()));
            } else if (getFont().getStyle() != Font.PLAIN) {
                setFont(new Font(getFont().getName(), Font.PLAIN, getFont().getSize()));
            }
            super.paint(g);
        }

        /**
         * This lets us prompt the user to "Enter a ..." when nothing is selected,
         * and display the string "No ..." when the selected value's string is empty.
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean hasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            wantItalic = false;
            if (index == -1 && value == null && component.isEnabled()) {
                wantItalic = true;
                setText(prompt);
            } else if (value != null) {
                String str = value.toString();
                if (str.length() == 0) {
                    // -- No Geo-poli-entity -- + (123 TBx)
                    wantItalic = true;
                    setText(empty + getNumTbsLabel(str));
                } else {
                    // (county|village|person) + (123 TBx)
                    setText(str + getNumTbsLabel(str));
                }
            } else
                setText("");
            return this;
        }
    }


    /**
     * Handles combo box selections for the Community list.
     *
     * @param unused The combo selection event.
     */
    private void communityActionListener(ActionEvent unused) {
        Object selectedItem = communityChooser.getSelectedItem();
        selectedCommunity = selectedItem != null ? selectedItem.toString() : null;
        setDone(selectedCommunity != null);
    }

    /**
     * Listens for changes to the community filter text box, and updates the filter itself.
     */
    private DocumentListener communityFilterListener = new DocumentListener() {
        // Listen for any change to the text
        public void changedUpdate(DocumentEvent e) { common(); }
        public void removeUpdate(DocumentEvent e) { common(); }
        public void insertUpdate(DocumentEvent e) { common(); }
        void common() { communityModel.setFilterString(communityFilter.getText()); }
    };

    private boolean firingActionEvent = false;
    private void fireActionEvent() {
        if (!firingActionEvent) {
            // Set flag to ensure that an infinite loop is not created
            firingActionEvent = true;
            ActionEvent e = null;
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            long mostRecentEventTime = EventQueue.getMostRecentEventTime();
            int modifiers = 0;
            AWTEvent currentEvent = EventQueue.getCurrentEvent();
            if (currentEvent instanceof InputEvent) {
                modifiers = ((InputEvent)currentEvent).getModifiers();
            } else if (currentEvent instanceof ActionEvent) {
                modifiers = ((ActionEvent)currentEvent).getModifiers();
            }
            // Process the listeners last to first, notifying
            // those that are interested in this event
            try {
                for ( int i = listeners.length-2; i>=0; i-=2 ) {
                    if ( listeners[i]==ActionListener.class ) {
                        // Lazily create the event:
                        if ( e == null )
                            e = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,
                                "recipientChanged",
                                mostRecentEventTime, modifiers);
                        ((ActionListener)listeners[i+1]).actionPerformed(e);
                    }
                }
            } finally {
                firingActionEvent = false;
            }
        }
    }


}
