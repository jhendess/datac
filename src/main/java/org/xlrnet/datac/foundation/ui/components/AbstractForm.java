package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import javax.annotation.PostConstruct;

/**
 * Abstract implementation of a form with a save, cancel and delete button. Inherit from this class to build specific
 * forms.
 */
@SpringComponent
public abstract class AbstractForm<T extends AbstractEntity> extends FormLayout {

    /**
     * Save action button.
     */
    protected final Button save = new Button("Save", VaadinIcons.CHECK);

    /**
     * Cancel action button.
     */
    protected final Button cancel = new Button("Cancel");

    /**
     * Delete action button.
     */
    protected final Button delete = new Button("Delete", VaadinIcons.TRASH);

    /**
     * The currently edited entity.
     */
    protected T entity;

    /**
     * Binds fields in this class to fields in the User class when values are changed.
     */
    protected BeanValidationBinder<T> binder = buildBinder();

    /**
     * Handler which will be called when the save button has been clicked and the entity has been successfully
     * validated.
     */
    protected EntityChangeHandler<T> saveHandler = new DummyEntityChangeHandler<>();

    /**
     * Handler which will be called when the delete button has been clicked.
     */
    protected EntityChangeHandler<T> deleteHandler = new DummyEntityChangeHandler<>();

    /**
     * Handler which will be called when the cancel button has been clicked.
     */
    private GenericHandler cancelHandler = () -> {
        // Default action is to do nothing when clicking cancel
    };

    @PostConstruct
    private void init() {
        Component fields = getContent();
        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel, delete);
        addComponents(fields, buttonLayout);

        buttonLayout.setSpacing(true);

        // Bind text fields to actual bean properties with the same name
        binder.bindInstanceFields(this);

        // Configure and style components
        setSpacing(true);
        save.setStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        // wire action buttons to save, delete and reset
        save.addClickListener(e -> {
            binder.validate();
            if (binder.isValid()) {
                saveHandler.onChange(entity);
            }
        });
        delete.addClickListener(e -> deleteHandler.onChange(entity));
        cancel.addClickListener(e -> {
            setEntity(null);
            cancelHandler.handle();
        });
        setVisible(false);
    }

    protected boolean isDeletable(T entity) {
        return entity.getId() != null;
    }

    public void setCancelHandler(@NotNull GenericHandler cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    public void setDeleteHandler(@NotNull EntityChangeHandler<T> deleteHandler) {
        this.deleteHandler = deleteHandler;
    }

    public final void setEntity(T entity) {
        if (entity == null) {
            setVisible(false);
            return;
        }
        final boolean persisted = isDeletable(entity);
        delete.setVisible(persisted);

        this.entity = entity;
        binder.setBean(this.entity);
        setVisible(true);

        // A hack to ensure the whole form is visible
        save.focus();
    }

    public void setSaveHandler(@NotNull EntityChangeHandler<T> handler) {
        this.saveHandler = handler;
    }

    @NotNull
    protected abstract Component getContent();

    @NotNull
    protected abstract BeanValidationBinder<T> buildBinder();
}
