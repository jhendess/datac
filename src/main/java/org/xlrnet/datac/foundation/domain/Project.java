package org.xlrnet.datac.foundation.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.xlrnet.datac.foundation.domain.validation.Regex;
import org.xlrnet.datac.foundation.domain.validation.ValidBranches;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.domain.Branch;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A project represents the main configuration entity in the application. It contains both meta information (e.g. name)
 * and the actual configuration on how to access the backing VCS and which branches should be watched..
 */
@Entity
@ValidBranches
@Table(name = "project")
public class Project extends AbstractEntity implements VcsRemoteCredentials {

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
    @Column(name = "type")
    private String type;

    /**
     * The adapter used for connecting to the VCS.
     */
    @NotEmpty
    @Size(max = 200)
    @Column(name = "adapter")
    private String adapterClass;

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
     * The password which should be used for logging in to the VCS. If the login is anonymous, this value should be
     * ignored.
     */
    @Size(max = 50)
    @Column(name = "password")
    private String password;

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
     * Collection of branches in the VCS. Contains both watched and unwatched changes.
     */
    @OneToMany(cascade = CascadeType.ALL, targetEntity = Branch.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    private Collection<Branch> branches = new ArrayList<>();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAdapterClass() {
        return adapterClass;
    }

    public void setAdapterClass(String adapterClass) {
        this.adapterClass = adapterClass;
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

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return pollInterval == project.pollInterval &&
                Objects.equals(name, project.name) &&
                Objects.equals(description, project.description) &&
                Objects.equals(website, project.website) &&
                Objects.equals(type, project.type) &&
                Objects.equals(adapterClass, project.adapterClass) &&
                Objects.equals(url, project.url) &&
                Objects.equals(username, project.username) &&
                Objects.equals(password, project.password) &&
                Objects.equals(newBranchPattern, project.newBranchPattern) &&
                Objects.equals(changelogLocation, project.changelogLocation) &&
                Objects.equals(lastChangeCheck, project.lastChangeCheck) &&
                Objects.equals(branches, project.branches);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, website, type, adapterClass, url, username, password, pollInterval, newBranchPattern, changelogLocation, lastChangeCheck, branches);
    }
}
