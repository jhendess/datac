package org.xlrnet.datac.foundation.ui.components;

import org.vaadin.viritin.button.MButton;
import org.xlrnet.datac.commons.util.MessageGenerator;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

import de.steinwedel.messagebox.MessageBox;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced version of Viritin's AbstractForm. Provides a customizable delete dialog.
 */
@Slf4j
@SpringComponent
public abstract class AbstractEntityForm<T extends AbstractEntity> extends org.vaadin.viritin.form.AbstractForm<T> {

    /** Default message generator. */
    private static final MessageGenerator DEFAULT_DELETE_MESSAGE_GENERATOR = (e) -> "Are you sure?";

    /**
     * Generator which provides a warning message before deleting an entity. Can be customized on a per item base. Will
     * never be called with a null value. */
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private MessageGenerator<T> messageGenerator = DEFAULT_DELETE_MESSAGE_GENERATOR;

    public AbstractEntityForm(Class<T> entityType) {
        super(entityType);
    }

    @Override
    protected Button createDeleteButton() {
        return new MButton(getDeleteCaption()).withStyleName(ValoTheme.BUTTON_DANGER).withVisible(false);
    }

    @Override
    protected void delete(Button.ClickEvent e) {
        if (getEntity() != null) {
                MessageBox.createWarning()
                        .withMessage(getMessageGenerator().generate(getEntity()))
                        .withYesButton(() -> super.delete(e))
                        .withNoButton()
                        .open();
        } else {
            LOGGER.warn("Attempting to delete null object from UI");
        }
    }
}
