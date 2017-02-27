package org.xlrnet.datac.session.ui.components;

import org.xlrnet.datac.session.services.PasswordService;
import org.xlrnet.datac.session.validation.ValidPassword;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Data for password fields in {@link UserProfileWindow}.
 */
public class PasswordData {

    @NotNull
    private String oldPassword;

    @NotNull
    @ValidPassword
    @Size(min = PasswordService.MINIMUM_PASSWORD_SIZE, max = PasswordService.MAXIMUM_PASSWORD_SIZE)
    private String newPassword;

    @NotNull
    @Size(min = PasswordService.MINIMUM_PASSWORD_SIZE, max = PasswordService.MAXIMUM_PASSWORD_SIZE)
    private String newPasswordConfirmation;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirmation() {
        return newPasswordConfirmation;
    }

    public void setNewPasswordConfirmation(String newPasswordConfirmation) {
        this.newPasswordConfirmation = newPasswordConfirmation;
    }
}
