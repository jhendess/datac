package org.xlrnet.datac.foundation.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.xlrnet.datac.foundation.domain.validation.Regex;
import org.xlrnet.datac.foundation.domain.validation.ValidBranches;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * A project represents the main configuration entity in the application. It contains both meta information (e.g. name)
 * and the actual configuration on how to access the backing VCS and which branches should be watched..
 */
@Entity
@ValidBranches
@Table(name = "project")
@EntityListeners(ProjectCredentialsEncryptionListener.class)
public class Project extends AbstractEntity implements VcsRemoteCredentials, Lockable {

    /**
     * Name of the project.
     */
    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    /**
     * Description of the project.
     */
    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    /**
     * Website of the project.
     */
    @URL
    @Size(max = 200)
    private String website;

    /**
     * Type of version control system that is used by the project. This may be used as a fallback if the concrete VCS
     * adapter is not available anymore.
     */
    @NotEmpty
    @Size(max = 20)
    @Column(name = "vcs_type")
    private String vcsType;

    /**
     * The adapter used for connecting to the VCS.
     */
    @NotEmpty
    @Size(max = 200)
    @Column(name = "vcs_adapter")
    private String vcsAdapterClass;

    /**
     * The adapter class used for accessing database changes.
     */
    @NotEmpty
    @Size(max = 200)
    @Column(name = "change_system_adapter")
    private String changeSystemAdapterClass;

    /**
     * Checkout URL for the VCS. This may be e.g. a git repository to clone or a SVN path to checkout.
     */
    @NotEmpty
    @Size(max = 200)
    @Column(name = "url")
    private String url;

    /**
     * The username which should be used for logging in to the VCS. If this value is null, an anonymous login should be
     * performed.
     */
    @Size(max = 50)
    @Column(name = "username")
    private String username;

    /**
     * The encrypted password which should be used for logging in to the VCS. If the login is anonymous, this value
     * should be ignored.
     */
    @Column(name = "password")
    private String encryptedPassword;

    /**
     * Salt used for encrypting the user credentials. Will be automatically set by an entity listener.
     */
    @Column(name = "salt")
    private byte[] salt;

    /**
     * Unencrypted password representation.
     */
    @Transient
    @Size(max = 50)
    private transient String password;

    /**
     * Interval in minutes between checks for new updates.
     */
    @Min(1)
    @Max(300)
    @Column(name = "poll_interval")
    private int pollInterval = 1;

    /**
     * A regular expression that matches new branches. Whenever a new branch is detected that matches this pattern, it
     * should be automatically added to the list of watched branches.
     */
    @Regex
    @NotEmpty
    @Size(max = 200)
    @Column(name = "new_branch_pattern")
    private String newBranchPattern;

    /**
     * Location of the master changelog file relative to the checkout directory.
     */
    @NotEmpty
    @Size(max = 500)
    @Column(name = "changelog_location")
    private String changelogLocation;

    /**
     * The time when the project was last checked for new changes in the source repository.
     */
    @Column(name = "last_change_check")
    private LocalDateTime lastChangeCheck;

    /**
     * Flag to indicate if the project's VCS has been initialized.
     */
    @Column(name = "initialized")
    private boolean initialized;

    /**
     * The current state of the project.
     */
    @NotNull
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private ProjectState state;

    /**
     * Collection of branches in the VCS. Contains both watched and unwatched changes.
     */
    @OneToMany(cascade = CascadeType.ALL, targetEntity = Branch.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    private Set<Branch> branches = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getVcsType() {
        return vcsType;
    }

    public void setVcsType(String vcsType) {
        this.vcsType = vcsType;
    }

    public String getVcsAdapterClass() {
        return vcsAdapterClass;
    }

    public void setVcsAdapterClass(String vcsAdapterClass) {
        this.vcsAdapterClass = vcsAdapterClass;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @NotNull
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public Branch getDevelopmentBranch() {
        for (Branch branch : branches) {
            if (branch.isDevelopment()) {
                return branch;
            }
        }
        return null;
    }

    public Collection<Branch> getBranches() {
        return branches;
    }

    public void addBranch(Branch branch) {
        if (branch != null && !branches.contains(branch)) {
            this.branches.add(branch);
            branch.setProject(this);
        }
    }

    public void setBranches(Collection<Branch> branches) {
        if (branches != null) {
            for (Branch branch : branches) {
                addBranch(branch);
            }
        }
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public String getChangelogLocation() {
        return changelogLocation;
    }

    public void setChangelogLocation(String changelogLocation) {
        this.changelogLocation = changelogLocation;
    }

    public String getNewBranchPattern() {
        return newBranchPattern;
    }

    public void setNewBranchPattern(String newBranchPattern) {
        this.newBranchPattern = newBranchPattern;
    }

    public LocalDateTime getLastChangeCheck() {
        return lastChangeCheck;
    }

    public void setLastChangeCheck(LocalDateTime lastChangeCheck) {
        this.lastChangeCheck = lastChangeCheck;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public ProjectState getState() {
        return state;
    }

    public Project setState(ProjectState state) {
        this.state = state;
        return this;
    }

    public byte[] getSalt() {
        return salt;
    }

    void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getChangeSystemAdapterClass() {
        return changeSystemAdapterClass;
    }

    public Project setChangeSystemAdapterClass(String changeSystemAdapterClass) {
        this.changeSystemAdapterClass = changeSystemAdapterClass;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return pollInterval == project.pollInterval &&
                Objects.equals(name, project.name) &&
                Objects.equals(description, project.description) &&
                Objects.equals(website, project.website) &&
                Objects.equals(vcsType, project.vcsType) &&
                Objects.equals(vcsAdapterClass, project.vcsAdapterClass) &&
                Objects.equals(url, project.url) &&
                Objects.equals(username, project.username) &&
                Objects.equals(encryptedPassword, project.encryptedPassword) &&
                Objects.equals(newBranchPattern, project.newBranchPattern) &&
                Objects.equals(changelogLocation, project.changelogLocation) &&
                Objects.equals(changeSystemAdapterClass, project.changeSystemAdapterClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, website, vcsType, vcsAdapterClass, url, username, encryptedPassword, pollInterval, newBranchPattern, changelogLocation, changeSystemAdapterClass);
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public String getLockKey() {
        return getId() != null ? getId().toString() : "?";
    }
}
