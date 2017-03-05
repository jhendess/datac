package org.xlrnet.datac.vcs.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.domain.validation.Regex;
import org.xlrnet.datac.vcs.domain.validation.ValidReleaseBranches;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collection;

/**
 * VCS config of a specific project. Information in the object is used in order to determine how the version control
 * system of a project should be processed.
 */
@Entity
@Table(name = "vcs_config")
@ValidReleaseBranches
public class VcsConfig extends AbstractEntity implements VcsRemoteCredentials {

    @OneToOne(optional = false, targetEntity = Project.class)
    @JoinColumn(name = "project_id", referencedColumnName = "id", unique = true, nullable = false, updatable = false)
    private Project project;

    @NotEmpty
    @Size(max = 20)
    @Column(name = "type")
    private String type;

    @NotEmpty
    @Size(max = 200)
    @Column(name = "adapter")
    private String adapterClass;

    @NotEmpty
    @Size(max = 200)
    @Column(name = "url")
    private String url;

    @Size(max = 50)
    @Column(name = "username")
    private String username;

    @Size(max = 50)
    @Column(name = "password")
    private String password;

    @Min(1)
    @Max(60)
    @Column(name = "poll_interval")
    private int pollInterval = 1;

    @Regex
    @NotEmpty
    @Size(max = 200)
    @Column(name = "new_branch_pattern")
    private String newBranchPattern;

    @NotEmpty
    @Size(max = 500)
    @Column(name = "changelog_location")
    private String changelogLocation;

    @NotNull
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH},
            targetEntity = Branch.class)
    @JoinColumn(name = "vcs_config_id", referencedColumnName = "id", nullable = false)
    private Branch developmentBranch;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH},
            targetEntity = Branch.class)
    @JoinColumn(name = "vcs_config_id", referencedColumnName = "id", nullable = false)
    private Collection<Branch> branches;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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
        return developmentBranch;
    }

    public void setDevelopmentBranch(Branch developmentBranch) {
        this.developmentBranch = developmentBranch;
    }

    public Collection<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Collection<Branch> branches) {
        this.branches = branches;
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
}
