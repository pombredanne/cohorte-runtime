/*******************************************************************************
 * Copyright (c) 2011 www.isandlatech.com (www.isandlatech.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ogattaz  (isandlaTech) - 18 nov. 2011 - initial API and implementation
 *******************************************************************************/
package org.psem2m.isolates.ui.admin.panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.psem2m.isolates.base.IIsolateLoggerSvc;
import org.psem2m.isolates.ui.admin.api.EUiAdminFont;
import org.psem2m.utilities.CXException;
import org.psem2m.utilities.CXStringUtils;

/**
 * @author ogattaz
 * 
 */
public class CJPanelTableBundles extends CJPanelTable<Bundle> {

    /**
     * 
     * By default a left mouse click on a JTable row will select that row, but
     * it’s not the same for the right mouse button. It takes bit more work to
     * get a JTable to select a row based on right mouse clicks. You might be
     * able to do it by subclassing JTable or ListSelectionModel, but there’s an
     * easier way.
     * 
     * 1. Use a MouseAdapter to detect right clicks on the JTable.
     * 
     * 2. Get the Point (X-Y coordinate) of the click.
     * 
     * 3. Find out what row contains that Point
     * 
     * 4. Get the ListSelectionModel of the JTable and 5. Tell the
     * ListSelectionModel to select that row. Here’s what it looks like:
     * 
     * 
     * @see http://www.stupidjavatricks.com/?p=12
     * @author ogattaz
     * 
     */
    class CMouseListener extends MouseAdapter {

        private JPopupMenu createPopUp(final int aRowIndex) {

            final String wName = String.valueOf(pBundlesTableModel.getValueAt(
                    aRowIndex, COLUMN_IDX_NAME));

            final Bundle wBundle = findInList(aRowIndex);

            final boolean wIsActive = Bundle.ACTIVE == wBundle.getState();

            final String wAction = wIsActive ? "Stop" : "Start";

            final JMenuItem wMenuItem1 = new JMenuItem(String.format("%s %s",
                    wAction, wName));
            wMenuItem1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {

                    logAction(actionEvent, aRowIndex);

                    try {
                        if (wIsActive) {
                            wBundle.stop();
                        } else {
                            wBundle.start();
                        }
                    } catch (final BundleException e) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "actionPerformed", e);
                        }
                    }

                }
            });

            final JMenuItem wMenuItem3 = new JMenuItem(String.format(
                    "Uninstall %s", wName));
            wMenuItem3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {

                    logAction(actionEvent, aRowIndex);
                    try {
                        wBundle.uninstall();
                    } catch (final BundleException e) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "actionPerformed", e);
                        }
                    }
                }
            });

            final JMenuItem wMenuItem4 = new JMenuItem(String.format(
                    "Update %s", wName));
            wMenuItem4.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent actionEvent) {

                    logAction(actionEvent, aRowIndex);
                    try {
                        wBundle.update();
                    } catch (final BundleException e) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "actionPerformed", e);
                        }
                    }
                }
            });

            final JPopupMenu wJPopupMenu = new JPopupMenu();
            wJPopupMenu.add(wMenuItem1);
            wJPopupMenu.addSeparator();
            wJPopupMenu.add(wMenuItem3);
            wJPopupMenu.add(wMenuItem4);

            return wJPopupMenu;
        }

        /**
         * @param actionEvent
         * @param aRowIndex
         */
        private void logAction(final ActionEvent actionEvent,
                final int aRowIndex) {

            if (hasLogger()) {
                getLogger().logInfo(this, "actionPerformed",
                        "rowIdx=[%d] action=[%s]", aRowIndex,
                        actionEvent.getActionCommand());
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseClicked(final MouseEvent e) {

            // Left mouse click
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (hasLogger()) {
                    getLogger().logInfo(this, "mouseClicked",
                            "isLeftMouseButton");
                }
            }
            // Right mouse click
            else if (SwingUtilities.isRightMouseButton(e)) {
                if (hasLogger()) {
                    getLogger().logInfo(this, "mouseClicked",
                            "isRightMouseButton");
                }
                rightClik(e);
            }
        }

        public void rightClik(final MouseEvent e) {

            final int r = pBundlesTable.rowAtPoint(e.getPoint());
            if (r >= 0 && r < pBundlesTable.getRowCount()) {
                pBundlesTable.setRowSelectionInterval(r, r);
            } else {
                pBundlesTable.clearSelection();
            }

            int wRowIdx = pBundlesTable.getSelectedRow();
            if (wRowIdx < 0) {
                return;
            }

            try {
                // if sorted
                wRowIdx = pBundlesTable.convertRowIndexToModel(wRowIdx);

                createPopUp(wRowIdx).show(e.getComponent(), e.getX(), e.getY());
            } catch (final ArrayIndexOutOfBoundsException e1) {
                if (hasLogger()) {
                    getLogger().logSevere(this, "stateChanged",
                            "ArrayIndexOutOfBoundsException !");
                }
            } catch (final Exception e2) {
                if (hasLogger()) {
                    getLogger().logSevere(this, "stateChanged", e);
                }
            }
        }
    }

    /**
     * Look at TableSelectionDemo.java from java tutorial to learn how work the
     * JTable selection model
     * 
     * @author ogattaz
     * 
     */
    class CSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(final ListSelectionEvent aListSelectionEvent) {

            execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        final int wRowIdx = pBundlesTable.getSelectionModel()
                                .getLeadSelectionIndex();

                        if (wRowIdx > -1
                                && wRowIdx < pBundlesTableModel.getRowCount()) {

                            if (hasLogger()) {
                                getLogger().logInfo(this, "valueChanged",
                                        "RowIdx=[%d]", wRowIdx);
                            }

                            // if sorted
                            final int wRealRowIdx = pBundlesTable
                                    .convertRowIndexToModel(wRowIdx);

                            if (hasLogger()) {
                                getLogger().logInfo(this, "valueChanged",
                                        "RealRowIdx=[%d]", wRealRowIdx);
                            }
                            // set the text info of the service
                            pBundleTextArea
                                    .setText(buildTextInfos(wRealRowIdx));

                        }
                    } catch (final ArrayIndexOutOfBoundsException e1) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "valueChanged",
                                    "ArrayIndexOutOfBoundsException !");
                        }
                    } catch (final Exception e2) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "valueChanged", e2);
                        }
                    }
                }
            });
        }
    }

    private final static int COLUMN_IDX_ID = 2;
    private final static int COLUMN_IDX_NAME = 0;
    private final static int COLUMN_IDX_STATE = 1;

    private static final long serialVersionUID = -6506936458249187873L;

    private final boolean[] COLUMNS_EDITABLE = { false, false, false };
    private final int[] COLUMNS_SIZE = { 200, 15, 5 };
    private final String[] COLUMNS_TITLE = { "Bundle name", "State", "Bndl" };

    private JPanel pBundleInfoPanel;
    /**
     * @param aRowIdx
     * @return
     */

    private List<Bundle> pBundlesList = new ArrayList<Bundle>();
    private JSplitPane pBundlesSplitPane;
    private JTable pBundlesTable;
    private DefaultTableModel pBundlesTableModel;
    private JScrollPane pBundlesTablScrollPane;
    private JTextArea pBundleTextArea;
    private JScrollPane pBundleTextAreaScrollPane;
    private CMouseListener pMouseListener = null;
    private CSelectionListener pSelectionListener = null;

    /**
     * 
     */
    public CJPanelTableBundles() {

        super();
        newGUI();
    }

    /**
     * Create the panel.
     */
    public CJPanelTableBundles(final Executor aUiExecutor,
            final IIsolateLoggerSvc aLogger, final JPanel aPanel) {

        super(aUiExecutor, aLogger);
        aPanel.setLayout(new BorderLayout(0, 0));
        aPanel.add(newGUI(), BorderLayout.CENTER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanelTable#addRow(java.lang.Object)
     */
    @Override
    boolean addRow(final Bundle aBundle) {

        execute(new Runnable() {
            @Override
            public void run() {

                try {
                    pBundlesTableModel.addRow(buildRowData(aBundle));
                    pBundlesTable.setRowSelectionInterval(0, 0);

                    pBundlesList.add(aBundle);

                } catch (final Exception e) {
                    if (hasLogger()) {
                        getLogger().logSevere(this, "addRow",
                                CXException.eMiniInString(e));
                    }
                }
            }
        });

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanelTable#addRows(T[])
     */
    @Override
    void addRows(final Bundle[] aBundles) {

        if (aBundles.length > 0) {
            for (final Bundle wBundle : aBundles) {
                addRow(wBundle);
            }
            fireUpdateTable();
        }
    }

    /**
     * @param aBundle
     * @return
     */
    @Override
    String[] buildRowData(final Bundle aBundle) {

        final String[] wRowData = new String[COLUMNS_TITLE.length];
        wRowData[COLUMN_IDX_NAME] = aBundle.getSymbolicName();
        wRowData[COLUMN_IDX_ID] = String.valueOf(aBundle.getBundleId());
        wRowData[COLUMN_IDX_STATE] = stateToString(aBundle.getState());
        return wRowData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanelTable#buildTextInfos(java.
     * lang.Object)
     */
    @Override
    String buildTextInfos(final Bundle wBundle) {

        final StringBuilder wSB = new StringBuilder();
        try {

            CXStringUtils.appendFormatStrInBuff(wSB, "bundle.name=[%s]\n",
                    wBundle.getSymbolicName());
            CXStringUtils.appendFormatStrInBuff(wSB, "bundle.id=[%d]\n",
                    wBundle.getBundleId());
            if (wBundle.getRegisteredServices() != null) {
                int wI = 0;
                for (final ServiceReference wServiceReference : wBundle
                        .getRegisteredServices()) {
                    CXStringUtils.appendFormatStrInBuff(wSB,
                            "Registered.service(%d)=[%s]\n", wI,
                            wServiceReference.toString());
                    wI++;
                }
            }
            if (wBundle.getServicesInUse() != null) {

                int wJ = 0;
                for (final ServiceReference wServiceReference : wBundle
                        .getServicesInUse()) {
                    CXStringUtils.appendFormatStrInBuff(wSB,
                            "used.service(%d)=[%s]\n", wJ,
                            wServiceReference.toString());
                    wJ++;
                }
            }
        } catch (final Exception e) {
            wSB.append(CXException.eInString(e));
        }
        // if (hasLogger()) {
        // getLogger().logInfo(this, "buildTextInfos", wSB.toString());
        // }
        return wSB.toString();
    }

    /**
     * @param aRowIdx
     * @return
     */
    private String buildTextInfos(final int aRowIdx) {

        return buildTextInfos(findInList(aRowIdx));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanel#destroy()
     */
    @Override
    public void destroy() {

        super.destroy();

        if (pMouseListener != null) {
            pBundlesTable.addMouseListener(pMouseListener);
        }
        if (pSelectionListener != null) {
            pBundlesTable.getSelectionModel().removeListSelectionListener(
                    pSelectionListener);
        }
    }

    /**
     * @param aRowIdx
     * @return
     */
    Bundle findInList(final int aRowIdx) {

        if (aRowIdx < 0 && aRowIdx >= pBundlesList.size()) {
            return null;
        }
        return pBundlesList.get(aRowIdx);
    }

    /**
     * @param aBundle
     * @return
     */
    int findInTable(final Bundle aBundle) {

        final String wName = aBundle.getSymbolicName();
        final int wMax = pBundlesTableModel.getRowCount();
        for (int wI = 0; wI < wMax; wI++) {
            if (wName
                    .equals(pBundlesTableModel.getValueAt(wI, COLUMN_IDX_NAME))) {
                return wI;
            }
        }
        return -1;
    }

    /**
     * 
     */
    private void fireUpdateTable() {

        execute(new Runnable() {
            @Override
            public void run() {

                try {
                    pBundlesTable.tableChanged(new TableModelEvent(
                            pBundlesTableModel));
                    pBundlesTable.updateUI();

                } catch (final Exception e) {
                    if (hasLogger()) {
                        getLogger().logSevere(this, "fireUpdateTable",
                                CXException.eMiniInString(e));
                    }
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanelTable#getTable()
     */
    @Override
    JTable getTable() {

        return pBundlesTable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanel#newGUI()
     */
    @Override
    public JPanel newGUI() {

        setLayout(new BorderLayout(0, 0));

        pBundlesSplitPane = new JSplitPane();
        pBundlesSplitPane.setResizeWeight(0.5);
        pBundlesSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        add(pBundlesSplitPane, BorderLayout.CENTER);

        {
            pBundlesTablScrollPane = new JScrollPane();
            pBundlesSplitPane.add(pBundlesTablScrollPane, JSplitPane.TOP);

            pBundlesTable = new JTable();
            pBundlesTableModel = new DefaultTableModel(new Object[][] {},
                    COLUMNS_TITLE) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(final int row, final int column) {

                    return COLUMNS_EDITABLE[column];
                }
            };
            pBundlesTable.setModel(pBundlesTableModel);

            pBundlesTable.getColumnModel().getColumn(COLUMN_IDX_NAME)
                    .setPreferredWidth(COLUMNS_SIZE[COLUMN_IDX_NAME]);
            pBundlesTable.getColumnModel().getColumn(COLUMN_IDX_ID)
                    .setPreferredWidth(COLUMNS_SIZE[COLUMN_IDX_ID]);
            pBundlesTable.getColumnModel().getColumn(COLUMN_IDX_STATE)
                    .setPreferredWidth(COLUMNS_SIZE[COLUMN_IDX_STATE]);

            final TableRowSorter<TableModel> wServicesSorter = new TableRowSorter<TableModel>(
                    pBundlesTableModel);
            pBundlesTable.setRowSorter(wServicesSorter);

            final List<SortKey> wSortKeys = new ArrayList<SortKey>();
            wSortKeys.add(new SortKey(COLUMN_IDX_NAME, SortOrder.ASCENDING));
            wServicesSorter.setSortKeys(wSortKeys);

            pBundlesTable
                    .setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            pBundlesTable.setColumnSelectionAllowed(false);
            pBundlesTable.setRowSelectionAllowed(true);

            // Look at TableSelectionDemo.java from java
            // tutorial to learn how work the JTable selection
            // model
            pSelectionListener = new CSelectionListener();
            pBundlesTable.getSelectionModel().addListSelectionListener(
                    pSelectionListener);

            setTableFont(EUiAdminFont.NORMAL);
            pMouseListener = new CMouseListener();
            pBundlesTable.addMouseListener(pMouseListener);

            pBundlesTablScrollPane.setViewportView(pBundlesTable);
        }
        {

            pBundleInfoPanel = new JPanel();
            pBundlesSplitPane.add(pBundleInfoPanel, JSplitPane.BOTTOM);
            pBundleInfoPanel.setLayout(new BorderLayout(0, 0));

            pBundleTextAreaScrollPane = new JScrollPane();
            pBundleInfoPanel
                    .add(pBundleTextAreaScrollPane, BorderLayout.CENTER);

            pBundleTextArea = new JTextArea();
            setText("Info...");
            pBundleTextAreaScrollPane.setViewportView(pBundleTextArea);
            setTextFont(EUiAdminFont.NORMAL);
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanelTable#removeAllRows()
     */
    @Override
    void removeAllRows() {

        execute(new Runnable() {
            @Override
            public void run() {

                try {
                    for (int wI = pBundlesTableModel.getRowCount() - 1; wI > -1; wI--) {
                        pBundlesTableModel.removeRow(wI);
                    }
                    pBundlesList.clear();
                } catch (final Exception e) {
                    if (hasLogger()) {
                        getLogger().logSevere(this, "removeAllRows",
                                CXException.eMiniInString(e));
                    }
                }
            }
        });

    }

    /**
     * @param aBundle
     */
    @Override
    void removeRow(final Bundle aBundle) {

        execute(new Runnable() {
            @Override
            public void run() {

                final int wRowIdx = findInTable(aBundle);
                if (wRowIdx != -1) {
                    try {
                        pBundlesTableModel.removeRow(wRowIdx);
                        pBundlesList.remove(aBundle);
                    } catch (final Exception e) {
                        if (hasLogger()) {
                            getLogger().logSevere(this, "removeRow",
                                    CXException.eMiniInString(e));
                        }
                    }
                }
            }
        });
        fireUpdateTable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanelTable#setRow(java.lang.Object)
     */
    @Override
    void setRow(final Bundle aBundle) {

        final int wRowIdx = findInTable(aBundle);
        if (wRowIdx == -1) {
            addRow(aBundle);
        } else {
            updateRow(wRowIdx, aBundle);
        }
        fireUpdateTable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.ui.admin.panels.CJPanelTable#setRows(T[])
     */
    @Override
    void setRows(final Bundle[] aBundles) {

        removeAllRows();
        addRows(aBundles);
        fireUpdateTable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanelTable#setTableFont(java.lang
     * .String, int)
     */
    @Override
    Font setTableFont(final EUiAdminFont aUiAdminFont) {

        pBundlesTable.setFont(aUiAdminFont.getTableFont());
        return aUiAdminFont.getTableFont();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanel#setText(java.lang.String)
     */
    @Override
    public void setText(final String aText) {

        pBundleTextArea.setText(aText);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.isolates.ui.admin.panels.CJPanel#setTextFont(java.lang.String,
     * int)
     */
    @Override
    public Font setTextFont(final EUiAdminFont aUiAdminFont) {

        pBundleTextArea.setFont(aUiAdminFont.getTextFont());
        return aUiAdminFont.getTextFont();
    }

    /**
     * @param aBundleState
     * @return
     */
    private String stateToString(final int aBundleState) {

        switch (aBundleState) {
        case Bundle.UNINSTALLED:
            return String.format("%d UNINSTALLED", aBundleState);
        case Bundle.INSTALLED:
            return String.format("%d INSTALLED", aBundleState);
        case Bundle.RESOLVED:
            return String.format("%d RESOLVED", aBundleState);
        case Bundle.STARTING:
            return String.format("%d STARTING", aBundleState);
        case Bundle.STOPPING:
            return String.format("%d STOPPING", aBundleState);
        case Bundle.ACTIVE:
            return String.format("%d ACTIVE", aBundleState);
        default:
            return String.format("%d ???", aBundleState);
        }
    }

    /**
     * @param aRowIdx
     * @param aBundle
     */
    @Override
    void updateRow(final int aRowIdx, final Bundle aBundle) {

        final String[] wRowData = buildRowData(aBundle);
        int wI = 0;
        for (final String wColumnValue : wRowData) {
            pBundlesTableModel.setValueAt(wColumnValue, aRowIdx, wI);
            wI++;
        }
        fireUpdateTable();
    }

}