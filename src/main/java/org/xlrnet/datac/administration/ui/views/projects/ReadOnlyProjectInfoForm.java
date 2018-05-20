package org.xlrnet.datac.administration.ui.views.projects;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Component;
import org.vaadin.viritin.fields.IntegerField;
import org.vaadin.viritin.fields.MCheckBox;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;
import org.xlrnet.datac.database.services.DatabaseChangeSystemAdapterRegistry;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.services.ValidationService;
import org.xlrnet.datac.foundation.ui.components.AbstractEntityForm;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.VersionControlSystemRegistry;

import java.util.Optional;

/**
 * Read-only version of the project form.
 */
@UIScope
@SpringComponent
public class ReadOnlyProjectInfoForm extends AbstractEntityForm<Project, ProjectService> {

    /**
     * Name of the project.
     */
    private final MTextField name = new MTextField("Project name").withReadOnly(true).withFullWidth();

    /**
     * Selection box for various VCS implementations.
     */
    private final MTextField selectedVcs = new MTextField("VCS System").withReadOnly(true).withFullWidth();

    /**
     * Text field for vcs target URL.
     */
    private final MTextField url = new MTextField("VCS URL").withReadOnly(true).withFullWidth();

    /**
     * Text field for vcs username.
     */
    private final MTextField username = new MTextField("Username").withReadOnly(true).withFullWidth();

    /**
     * Selection box for various database change system implementations.
     */
    private final MTextField selectedDcs = new MTextField("Database change system").withReadOnly(true).withFullWidth();

    /**
     * Text field for changelog master file.
     */
    private final MTextField changelogLocation = new MTextField("Changelog master file").withReadOnly(true).withFullWidth();

    /**
     * Selection box for the VCS development branch.
     */
    private final MTextField devBranch = new MTextField("Development branch").withReadOnly(true).withFullWidth();

    /**
     * Poll interval for new VCS.
     */
    private final IntegerField pollInterval = new IntegerField("Poll interval in minutes").withReadOnly(true).withFullWidth();

    /**
     * Checkboxes for selecting release branches.
     */
    private final CheckBoxGroup<Branch> releaseBranches = new CheckBoxGroup<>("Release branches");

    /**
     * Checkbox to enable automatic import of new branches.
     */
    private final MTextField newBranchPattern = new MTextField("Pattern for new branches").withReadOnly(true).withFullWidth();

    /**
     * Enable automatic scheduled polling for new changes.
     */
    private MCheckBox automaticPollingEnabled = new MCheckBox("Automatic polling enabled").withReadOnly(true).withFullWidth();

    /**
     * VCS registry.
     */
    private final VersionControlSystemRegistry vcsRegistry;

    /**
     * DCS registry.
     */
    private final DatabaseChangeSystemAdapterRegistry dcsRegistry;

    public ReadOnlyProjectInfoForm(VersionControlSystemRegistry vcsRegistry, DatabaseChangeSystemAdapterRegistry dcsRegistry, ValidationService validationService) {
        super(Project.class, null, validationService);
        this.vcsRegistry = vcsRegistry;
        this.dcsRegistry = dcsRegistry;
    }

    @Override
    protected Component createContent() {
        MVerticalLayout layout = new MVerticalLayout().withMargin(false);
        releaseBranches.setReadOnly(true);
        releaseBranches.setItemEnabledProvider(Branch::isWatched);
        releaseBranches.setItemCaptionGenerator(Branch::getName);
        getBinder().addStatusChangeListener((e) -> updateBeanDependentFields((Project) e.getBinder().getBean()));
        return layout.with(name, selectedDcs, changelogLocation, selectedVcs, url, username, automaticPollingEnabled, pollInterval, devBranch, newBranchPattern, releaseBranches);
    }

    private void updateBeanDependentFields(Project project) {
        if (project != null) {
            Optional<VcsMetaInfo> vcsMetaInfo = vcsRegistry.findMetaInfoByAdapterClassName(project.getVcsAdapterClass());
            if (vcsMetaInfo.isPresent()) {
                selectedVcs.setValue(String.format("%s (%s)", vcsMetaInfo.get().getVcsName(), vcsMetaInfo.get().getAdapterName()));
            } else {
                selectedVcs.setValue("Unknown");
            }
            Optional<DatabaseChangeSystemMetaInfo> dcsMetaInfo = dcsRegistry.findMetaInfoByAdapterClassName(project.getChangeSystemAdapterClass());
            if (dcsMetaInfo.isPresent()) {
                selectedDcs.setValue(dcsMetaInfo.get().getAdapterName());
            } else {
                selectedDcs.setValue("Unknown");
            }
            devBranch.setData(project.getDevelopmentBranch().getName());
            releaseBranches.setDataProvider(DataProvider.fromStream(project.getBranches().stream().filter(Branch::isWatched).sorted()));
        } else {
            selectedVcs.setValue("");
            selectedDcs.setValue("");
            devBranch.setValue("");
            releaseBranches.setDataProvider(DataProvider.ofItems());
        }
    }
}
