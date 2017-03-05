package org.xlrnet.datac.foundation.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.xlrnet.datac.vcs.domain.VcsConfig;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by jhendess on 02.03.2017.
 */
@Entity
@Table(name = "project")
public class Project extends AbstractEntity {

    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    @URL
    @Size(max = 200)
    private String website;

    @NotNull
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH,
            CascadeType.DETACH}, targetEntity = VcsConfig.class, orphanRemoval = true)
    @JoinColumn(name = "project_id")
    private VcsConfig vcsConfig;

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

    public VcsConfig getVcsConfig() {
        return vcsConfig;
    }

    public void setVcsConfig(VcsConfig vcsConfig) {
        this.vcsConfig = vcsConfig;
    }
}
