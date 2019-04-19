package org.literacybridge.acm.gui.Assistant;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The abstract base class for Assistant pages. Provides access to Assistant properties.
 *
 * Derived classes must implement 'OnPageEnterer'.
 */
public abstract class AssistantPage<Context> extends JPanel {
    // This is because the SeaGlass L&F makes combo boxes too small
    // for the content. To work around, we measure the size of an
    // empty ComboBox, and add a 5px buffer.in a page measure the size of the longest string,
    // and add a 5px buffer.
    /**
     * This is because the SeaGlass L&F makes combo boxes too small for the content. To work
     * around, we measure the size of an empty ComboBox, and add a 5px buffer.
     *
     * In each page, for each combo box, measure the size of the longest string, and set the
     * combo preferred size (and, optionally, min or max size) to the string + baseComboWidth
     */
    public static final int baseComboWidth = new JComboBox().getPreferredSize().width + 5;
    public static void setComboWidth(JComboBox cb, String string) {
        int textWidth = new JLabel("").getFontMetrics(cb.getFont()).stringWidth(string);
        Dimension size = cb.getPreferredSize();
        size.width = baseComboWidth + textWidth;
        cb.setPreferredSize(size);
    }

    protected static final Border greenBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
    protected static final Border redBorder = new LineBorder(Color.RED, 1, true);
    protected static final Border blankBorder = new LineBorder(new Color(0, 0, 0, 0), 1, true);
    protected static final Border parameterBorder = new CompoundBorder(greenBorder, new EmptyBorder(2,3,2,4));

    public static JLabel makeBoxedLabel() { return makeBoxedLabel(null); }
    public static JLabel makeBoxedLabel(String text) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(Color.white);
        label.setBorder(parameterBorder);
        if (!StringUtils.isEmpty(text)) label.setText(text);
        return label;
    }

    public static class SizingParams {
        public static final int IGNORE = Integer.MIN_VALUE;

        public SizingParams() {
            this(-1);
        }
        public SizingParams(int modelColumn) {
            this.modelColumn = modelColumn;
            this.minPadding = IGNORE;
            this.preferredPadding = 2;
            this.maxPadding = 42;
        }
        public SizingParams(int modelColumn,
            int minPadding,
            int preferredPadding,
            int maxPadding)
        {
            this.modelColumn = modelColumn;
            this.minPadding = minPadding;
            this.preferredPadding = preferredPadding;
            this.maxPadding = maxPadding;
        }
        public SizingParams(int minPadding, int preferredPadding, int maxPadding) {
            this.modelColumn = -1;
            this.minPadding = minPadding;
            this.preferredPadding = preferredPadding;
            this.maxPadding = maxPadding;
        }

        public List<SizingParams> forColumns(Integer... columnNos) {
            List<SizingParams> params = Arrays.stream(columnNos)
                .map(n -> new SizingParams(n, minPadding, preferredPadding, maxPadding))
                .collect(Collectors.toList());
            return params;
        }

        int modelColumn;
        int minPadding;
        int preferredPadding;
        int maxPadding;
    }
    /**
     * Method to size "small" header columns. This is useful for columns with fairly consistent
     * and fairly small data. Sizes the column to show every item.
     * @param table to be sized.
     * @param columns a list of model column numbers to be sized.
     */
    public static void sizeColumns(JTable table, Integer... columns) {
        List<SizingParams> params = Arrays.stream(columns).map(SizingParams::new).collect(
            Collectors.toList());
        sizeColumns(table, params);
    }

    public static void sizeColumns(JTable table, List<SizingParams> params) {
        TableModel model = table.getModel();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (SizingParams param : params) {
            final int modelColumnNo = param.modelColumn;
            final int viewCol = table.convertColumnIndexToView(modelColumnNo);
            TableColumn column = table.getColumnModel().getColumn(modelColumnNo);

            int headerWidth = headerRenderer.getTableCellRendererComponent(null,
                column.getHeaderValue(),
                false,
                false,
                0,
                0).getPreferredSize().width;

            // To get the column class from the model we need to use the model column index. But to
            // get the render component we need to use the view index.

            Stream<Object> values = IntStream
                .range(0, table.getRowCount())
                .mapToObj(row -> table.getValueAt(row, modelColumnNo));


            int cellWidth = IntStream
                .range(0, table.getRowCount())
                .mapToObj(row -> table.getValueAt(row, modelColumnNo))
                .filter(item -> item != null)
                .map(item -> table.getDefaultRenderer(model.getColumnClass(modelColumnNo))
                    .getTableCellRendererComponent(table, item, false, false, 0, viewCol)
                    .getPreferredSize().width)
                .max(Integer::compareTo)
                .orElse(1);

            int w = Math.max(headerWidth, cellWidth) + 2;
            if (param.minPadding != SizingParams.IGNORE) column.setMinWidth(w + param.minPadding);
            if (param.maxPadding != SizingParams.IGNORE) column.setMaxWidth(w + param.maxPadding);
            if (param.preferredPadding != SizingParams.IGNORE) column.setPreferredWidth(w + param.preferredPadding);
        }
    }

    /**
     * Creates "standard" GridBagConstraints for a grid-based Assistant page. Optimized for
     * one column, so each item is it's own row, but is a good starting point 
     * @return a "standard" GridBagConstraints object.
     */
    public static GridBagConstraints getGBC() {
        Insets insets = new Insets(0,0,15,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);
        return gbc;
    }

    private final Assistant.PageHelper<Context> pageHelper;
    private boolean isComplete = false;

    public AssistantPage(Assistant.PageHelper<Context> pageHelper) {
        super();
        this.pageHelper = pageHelper;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(pageHelper.getAssistant().factory.background);
    }

    protected abstract void onPageEntered(boolean progressing);
    protected void onPageLeaving(boolean progressing) {}

    protected String getTitle() { return ""; }
    protected boolean isSummaryPage() { return false; }

    /**
     * Sets the completed state of the page. The Assistant uses this state to enable/disable
     * Next / Finish buttons.
     */
    protected void setComplete() { setComplete(true); }
    protected void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
        pageHelper.onComplete(isComplete);
    }
    boolean isComplete() { return isComplete; }

    protected final Context getContext() { return pageHelper.getContext(); }

    // Helpers to convert between decorated and un-decorated playlist names.

    /**
     * Given a playlist title, a deployment, and a language, build the decorated playlist name,
     * like 1-Health-swh
     * @param title of the playlist
     * @param deploymentNo of the deployment
     * @param languagecode of the playlist
     * @return the decorated name.
     */
    public static String decoratedPlaylistName(String title, int deploymentNo, String languagecode) {
        title = normalizePlaylistTitle(title);
        return String.format("%d-%s-%s", deploymentNo, title, languagecode);
    }

    private static Pattern playlistPattern = Pattern.compile("\\d+-(.*)-\\w+");

    /**
     * Given a decorated playlist name, strip off the deployment and language, and return just
     * the name. Underscores are converted (back) to spaces.
     * @param decoratedName to be un-decorated.
     * @return the un-decorated name.
     */
    public static String undecoratedPlaylistName(String decoratedName) {
        Matcher matcher = playlistPattern.matcher(decoratedName);
        if (matcher.matches() && matcher.groupCount()==1) {
            return matcher.group(1).replaceAll("_", " ");
        }
        return null;
    }

    /**
     * Given a playlist title, trim leading and trailing spaces, and replace remaining spaces
     * with underscores.
     * @param title, possibly with spaces.
     * @return title without spaces.
     */
    private static String normalizePlaylistTitle(String title) {
        title = title.trim().replaceAll(" ", "_");
        return title;
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<T>() {
            public T next() {
                return e.nextElement();
            }

            public boolean hasNext() {
                return e.hasMoreElements();
            }
        }, Spliterator.ORDERED), false);
    }



}
