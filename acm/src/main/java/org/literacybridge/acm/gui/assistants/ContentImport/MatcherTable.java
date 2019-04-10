package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MatcherTable extends JTable {

    private MatcherTableModel model;
    private TableRowSorter<MatcherTableModel> sorter;
    private MatcherFilter<MatchableImportableAudio> filter;

    MatcherTable() {
        super();

        model = new MatcherTableModel(this);
        setModel(model);
        sorter = new TableRowSorter<>(model);
        filter = new MatcherFilter<>(model);
        sorter.setRowFilter(filter);

        model.addTableModelListener((final TableModelEvent tableModelEvent) -> {
            SwingUtilities.invokeLater(() -> {
                tableModelListener(tableModelEvent);
            });
        });

        setRowSorter(sorter);
        model.setupSorter(sorter);

        setPreferredScrollableViewportSize(new Dimension(500, 70));
    }

    public <T extends MatchableItem<?,?>> void setFilter(Predicate<T> predicate) {
        filter.setPredicate((Predicate) predicate);
    }

    void setRenderer(MatcherTableModel.Columns column, TableCellRenderer renderer) {
        TableColumn columnModel = getColumnModel().getColumn(column.ordinal());
        columnModel.setCellRenderer(renderer);
    }


    private void tableModelListener(TableModelEvent tableModelEvent) {
        if (tableModelEvent.getFirstRow() == TableModelEvent.HEADER_ROW) {
            sizeColumns();
        }
    }

    void sizeColumns() {
        Map<Integer, Stream<Object>> columnValues = new HashMap<>();
        // Set column 2 width (Status) on header & values.
        final int statusColumnNo = MatcherTableModel.Columns.Status.ordinal();
        Stream<Object> values = IntStream
            .range(0, this.getRowCount())
            .mapToObj(r -> getValueAt(r, statusColumnNo).toString());
        columnValues.put(statusColumnNo, values);

        // Set column 1 width (Update?) on header only.
        final int updateColumnNo = MatcherTableModel.Columns.Update.ordinal();
        values = Stream.empty();
        columnValues.put(updateColumnNo, values);

        AssistantPage.sizeColumns(this, columnValues);
    }

    @Override
    public MatcherTableModel getModel() {
        return model;
    }
}
