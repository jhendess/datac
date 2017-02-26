package org.xlrnet.datac.administration.domain;

import org.hibernate.validator.constraints.Email;

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
public class User extends org.xlrnet.datac.foundation.domain.AbstractEntity {

    /**
     * Login name of the user.
     */
    @NotNull
    @Size(min = 4, max = 20)
    private String loginName;

    /**
     * First name of the user.
     */
    @NotNull
    @Size(min = 3, max = 64)
    private String firstName;

    /**
     * Last name of the user.
     */
    @NotNull
    @Size(min = 3, max = 64)
    private String lastName;

    /**
     * Email of the user.
     */
    @Email
    @NotNull
    @Size(min = 1, max = 64)
    private String email;

    /**
     * Login password of the user.
     */
    private String password;

    /**
     * Password salt.
     */
    private String salt;

    public String getLoginName() {
        return loginName;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
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
