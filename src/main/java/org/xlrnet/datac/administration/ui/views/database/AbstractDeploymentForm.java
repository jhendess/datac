package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.ComboBox;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.vaadin.viritin.fields.MTextField;
import org.xlrnet.datac.database.domain.AbstractDeploymentInstance;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.foundation.services.ValidationService;
import org.xlrnet.datac.foundation.ui.components.AbstractEntityForm;
import org.xlrnet.datac.vcs.domain.Branch;

import java.util.Collection;

/**
 * Abstract form for deployment instances and groups.
 */
public abstract class AbstractDeploymentForm<T extends AbstractDeploymentInstance, S extends AbstractTransactionalService<T, ?>> extends AbstractEntityForm<T, S> {

    /** Name of the group. */
    @Getter(AccessLevel.PROTECTED)
    private final MTextField name = new MTextField().withFullWidth();

    /** Read-only name of the parent group. */
    @Getter(AccessLevel.PROTECTED)
    private final MTextField parentGroupName = new MTextField("Parent group").withReadOnly(true).withFullWidth();

    /** Branch which should be tracked by this instance. */
    @Getter(AccessLevel.PROTECTED)
    private final ComboBox<Branch> branch = new ComboBox<>("Tracked branch (inherited if empty)");

    AbstractDeploymentForm(Class<T> entityType, S transactionalService, ValidationService validationService) {
        super(entityType, transactionalService, validationService);
        branch.setWidth("100%");
        branch.setEmptySelectionAllowed(true);
        branch.setItemCaptionGenerator(Branch::getName);
    }

    @Override
    public void setEntity(T entity) {
        super.setEntity(entity);
        getParentGroupName().setValue(determineParentGroupName(entity));
    }

    void setAvailableBranches(Collection<Branch> branches) {
        getBranch().setDataProvider(DataProvider.ofCollection(branches));
    }

    @NotNull
    abstract String determineParentGroupName(@NotNull T entity);

    @Override
    protected void adjustResetButtonState() {
        if (getPopup() != null && getPopup().getParent() != null) {
            // Assume cancel button in a form opened to a popup also closes
            // it, allows closing via cancel button by default
            getResetButton().setEnabled(true);
            return;
        }
        if (isBound()) {
            // Always show the cancel button for this form
            getResetButton().setEnabled(true);
        }
    }
}
