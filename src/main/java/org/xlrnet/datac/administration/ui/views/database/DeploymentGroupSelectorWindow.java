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
import org.xlrnet.datac.database.domain.InstanceType;
import org.xlrnet.datac.database.util.DatabaseGroupHierarchicalDataProvider;
import org.xlrnet.datac.database.util.DatabaseInstanceIconProvider;

import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple modal window which allows the selection of a deployment group.
 */
@Slf4j
public class DeploymentGroupSelectorWindow extends MWindow {

    private final DatabaseGroupHierarchicalDataProvider dataProvider;

    private final MVerticalLayout content = new MVerticalLayout();

    private final MHorizontalLayout buttonLayout = new MHorizontalLayout();

    /** Handler which will be called on success. May not be null. */
    @Setter
    private Consumer<DeploymentGroup> successHandler;

    /** Flag whether the root group can be selected or not. */
    @Setter
    private boolean allowRootSelection;

    /** The selected value. */
    private IDatabaseInstance selectedValue;

    public DeploymentGroupSelectorWindow(DatabaseGroupHierarchicalDataProvider dataProvider) {
        super();
        this.dataProvider = dataProvider;
        buildContent();
        setContent(content);
        setModal(true);
    }

    private void buildContent() {
        MButton selectButton = new MButton("Select").withStyleName(ValoTheme.BUTTON_PRIMARY).withEnabled(false)
                .withListener((e) -> {
                    if (selectedValue instanceof DeploymentRoot) {
                        successHandler.accept(null);
                    } else {
                        successHandler.accept((DeploymentGroup) selectedValue);
                    }
                    close();
                });
        MButton cancelButton = new MButton("Cancel").withListener(x -> close());
        buttonLayout.with(selectButton);
        buttonLayout.with(cancelButton);

        TreeGrid<IDatabaseInstance> treeGrid = new TreeGrid<>();
        treeGrid.addSelectionListener((d) -> {
            boolean enabled = false;
            if (d.getFirstSelectedItem().isPresent()) {
                IDatabaseInstance selection = d.getFirstSelectedItem().get();
                if ((selection.getInstanceType() == InstanceType.ROOT && allowRootSelection)
                        || selection.getInstanceType() == InstanceType.GROUP) {
                    enabled = true;
                }
            }
            selectButton.setEnabled(enabled);
            selectedValue = enabled ? d.getFirstSelectedItem().get() : null;
        });
        treeGrid.setDataProvider(dataProvider);
        treeGrid.addColumn(new DatabaseInstanceIconProvider(), new HtmlRenderer());
        treeGrid.setHeaderVisible(false);

        MLabel label = new MLabel("Select a parent group for the new object");

        content.with(label, treeGrid, buttonLayout);

        this.addAttachListener((e) -> {
            LOGGER.trace("Attaching window {} to UI", DeploymentGroupSelectorWindow.class.getName());
            treeGrid.collapse(dataProvider.getDeploymentRoot());
            dataProvider.refreshAll();
        });
    }
}
