package org.xlrnet.datac.session.domain;

import org.hibernate.validator.constraints.Email;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * A user who may access the application.
 */
@Entity
@Table(name = "user")
public class User extends AbstractEntity {

    /**
     * Login name of the user.
     */
    @NotNull
    @Size(min = 4, max = 20)
    @Column(name = "login_name")
    private String loginName;

    /**
     * First name of the user.
     */
    @NotNull
    @Size(min = 3, max = 64)
    @Column(name = "first_name")
    private String firstName;

    /**
     * Last name of the user.
     */
    @NotNull
    @Size(min = 3, max = 64)
    @Column(name = "last_name")
    private String lastName;

    /**
     * Email of the user.
     */
    @Email
    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "email")
    private String email;

    /**
     * Hashed login password of the user.
     */
    @NotNull
    @Column(name = "password")
    private byte[] password;

    /**
     * Password salt.
     */
    @NotNull
    @Column(name = "salt")
    private byte[] salt;

    /**
     * Indicator if a password change is necessary.
     */
    @Column(name = "pw_change_necessary")
    private boolean pwChangeNecessary;

    public String getLoginName() {
        return loginName;
    }

    public boolean isPwChangeNecessary() {
        return pwChangeNecessary;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setPwChangeNecessary(boolean pwChangeNecessary) {
        this.pwChangeNecessary = pwChangeNecessary;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(loginName, user.loginName) &&
                Objects.equals(firstName, user.firstName) &&
                Objects.equals(lastName, user.lastName) &&
                Objects.equals(email, user.email) &&
                Objects.equals(password, user.password) &&
                Objects.equals(salt, user.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, loginName, firstName, lastName, email, password, salt);
    }
}
