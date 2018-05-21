package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;
import de.steinwedel.messagebox.MessageBox;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.viritin.button.MButton;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.commons.util.MessageGenerator;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.foundation.services.ValidationService;

import javax.validation.ConstraintViolationException;

/**
 * Enhanced version of Viritin's AbstractForm. Provides a customizable delete dialog.
 */
@Slf4j
@SpringComponent
public abstract class AbstractEntityForm<T extends AbstractEntity, S extends AbstractTransactionalService<T, ?>> extends org.vaadin.viritin.form.AbstractForm<T> {

    /** Default message generator. */
    private static final MessageGenerator DEFAULT_DELETE_MESSAGE_GENERATOR = (e) -> "Are you sure?";

    /**
     * Generator which provides a warning message before deleting an entity. Can be customized on a per item base. Will
     * never be called with a null value. */
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private MessageGenerator<T> deleteMessageGenerator = DEFAULT_DELETE_MESSAGE_GENERATOR;

    /** Service for performing custom validations. */
    private final ValidationService validationService;

    /** Transactional service which is used for reloading the entity if it was already persisted. */
    @Getter(AccessLevel.PRIVATE)
    private final S transactionalService;

    public AbstractEntityForm(Class<T> entityType, S transactionalService, ValidationService validationService) {
        super(entityType);
        this.validationService = validationService;
        this.transactionalService = transactionalService;
    }

    @Override
    protected Button createDeleteButton() {
        return new MButton(getDeleteCaption()).withStyleName(ValoTheme.BUTTON_DANGER).withVisible(false);
    }

    @Override
    protected void save(Button.ClickEvent e) {
        boolean performSave = preSave(getEntity());
        if (performSave) {
            super.save(e);
            postSave();
        }
    }

    @Override
    public void setEntity(T entity) {
        T reloadedEntity = entity;
        if (getTransactionalService() != null && entity.isPersisted()) {
            reloadedEntity = getTransactionalService().refresh(entity);
        }
        super.setEntity(reloadedEntity);
    }

    /**
     * Custom post-save action. Shows a success notification by default.
     */
    void postSave() {
        NotificationUtils.showSaveSuccess();
    }

    /**
     * Custom pre-save validation which can be overridden by concrete implementations. Return true to run the save
     * action or false to prevent saving.
     * Runs a full validation on the entity by default.
     */
    boolean preSave(T entity) {
        try {
            validationService.checkConstraints(entity);
        } catch (ConstraintViolationException cve) { // NOSONAR: Part of regular error handling (not nice, but works)
            NotificationUtils.showValidationError("Validation failed", cve.getConstraintViolations());
            return false;
        }
        return true;
    }

    @Override
    protected void delete(Button.ClickEvent e) {
        if (getEntity() != null && getEntity().isPersisted()) {
                MessageBox.createWarning()
                        .withMessage(getDeleteMessageGenerator().generate(getEntity()))
                        .withYesButton(() -> super.delete(e))
                        .withNoButton()
                        .open();
        } else {
            NotificationUtils.showWarning("You can only delete persisted objects.");
        }
    }
}
