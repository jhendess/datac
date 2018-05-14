package org.xlrnet.datac.administration.ui.views.database;

import java.util.function.Consumer;

import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentRoot;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.util.DatabaseGroupHierarchicalDataProvider;
import org.xlrnet.datac.database.util.DatabaseInstanceIconProvider;

import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Simple modal window which allows the selection of a deployment group.
 */
public class DeploymentGroupSelectorWindow extends MWindow {

    private final DatabaseGroupHierarchicalDataProvider dataProvider;

    private final MVerticalLayout content = new MVerticalLayout();

    private final MHorizontalLayout buttonLayout = new MHorizontalLayout();

    /** Handler which will be called on success. */
    private final Consumer<DeploymentGroup> successHandler;

    /** The selected value. */
    private IDatabaseInstance selectedValue;

    public DeploymentGroupSelectorWindow(DatabaseGroupHierarchicalDataProvider dataProvider, Consumer<DeploymentGroup> successHandler) {
        super();
        this.dataProvider = dataProvider;
        this.successHandler = successHandler;
        buildContent();
        setContent(content);
        setModal(true);
    }

    private void buildContent() {
        MButton selectButton = new MButton("Select").withStyleName(ValoTheme.BUTTON_PRIMARY).withEnabled(false)
                .withListener((e) -> {
                    if (successHandler != null) {
                        if (selectedValue instanceof DeploymentRoot) {
                            successHandler.accept(null);
                        } else {
                            successHandler.accept((DeploymentGroup) selectedValue);
                        }
                    }
                    close();
                });
        MButton cancelButton = new MButton("Cancel").withListener(x -> close());
        buttonLayout.with(selectButton);
        buttonLayout.with(cancelButton);

        TreeGrid<IDatabaseInstance> treeGrid = new TreeGrid<>();
        treeGrid.addSelectionListener((d) -> {
            selectButton.setEnabled(d.getFirstSelectedItem().isPresent());
            selectedValue = d.getFirstSelectedItem().isPresent() ? d.getFirstSelectedItem().get() : null;
        });
        treeGrid.setDataProvider(dataProvider);
        treeGrid.addColumn(new DatabaseInstanceIconProvider(), new HtmlRenderer());
        treeGrid.setHeaderVisible(false);

        MLabel label = new MLabel("Select a new parent group");

        content.with(label, treeGrid, buttonLayout);
    }
}
