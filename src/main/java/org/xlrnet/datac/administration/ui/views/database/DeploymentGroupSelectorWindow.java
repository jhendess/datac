package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringEscapeUtils;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentRoot;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.util.DatabaseGroupHierarchicalDataProvider;

import java.util.function.Consumer;

/**
 * Simple modal window which allows the selection of a deployment group.
 */
public class DeploymentGroupSelectorWindow extends MWindow {

    private final DatabaseGroupHierarchicalDataProvider dataProvider;

    private final MVerticalLayout content = new MVerticalLayout();

    private final MHorizontalLayout buttonLayout = new MHorizontalLayout();

    private final Consumer<DeploymentGroup> successHandler;

    private IDatabaseInstance selectedValue;

    /** Renderer for name incl. an type-specific icon. */
    private final ValueProvider<IDatabaseInstance, String> nameRenderer = (i) -> {
        String iconHtml = ((i instanceof DeploymentRoot) ? VaadinIcons.PACKAGE : VaadinIcons.FOLDER).getHtml();
        return String.format("%s %s", iconHtml, StringEscapeUtils.escapeHtml4(i.getName()));
    };

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
        treeGrid.addColumn(nameRenderer, new HtmlRenderer());
        treeGrid.setHeaderVisible(false);

        MLabel label = new MLabel("Select a new parent group");

        content.with(label, treeGrid, buttonLayout);
    }
}
