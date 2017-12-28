package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;

@SpringComponent
@SpringView(name = ProjectRevisionSubview.VIEW_NAME)
public class ProjectRevisionSubview extends AbstractSubview {

    public static final String VIEW_NAME = "project/revisions";

    @NotNull
    @Override
    protected Component buildMainPanel() {
        return null;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {

    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return null;
    }

    @NotNull
    @Override
    protected String getTitle() {
        return null;
    }
}
