package org.xlrnet.datac.session.domain;

/**
 * A user who may access the application.
 */
public class User {

    /**
     * Internal id of the user.
     */
    private long id;

    /**
     * Login name of the user.
     */
    private String loginName;

    /**
     * First name of the user.
     */
    private String firstName;

    /**
     * Last name of the user.
     */
    private String lastName;

    public long getId() {
        return id;
    }

    public User setId(long id) {
        this.id = id;
        return this;
    }

    public String getLoginName() {
        return loginName;
    }

    public User setLoginName(String loginName) {
        this.loginName = loginName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
}
