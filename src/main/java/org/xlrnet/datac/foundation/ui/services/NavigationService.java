package org.xlrnet.datac.foundation.ui.services;

import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.administration.ui.views.projects.AdminEditProjectSubview;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.session.ui.views.ProjectChangeSubview;
import org.xlrnet.datac.session.ui.views.ProjectRevisionSubview;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import com.vaadin.ui.UI;

/**
 * Service which provides convenience methods for navigation.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class NavigationService {

    private final RevisionGraphService revisionGraphService;

    @Autowired
    public NavigationService(RevisionGraphService revisionGraphService) {
        this.revisionGraphService = revisionGraphService;
    }

    /**
     * Opens the last revision on the given branch in the change overview.
     *
     * @param branch
     *         The branch to display.
     */
    public void openChangeView(@NotNull Branch branch) {
        Revision revision = revisionGraphService.findLastRevisionOnBranch(branch);
        checkState(revision != null, "No last revision on branch - this should never happen");
        navigateTo(String.format("%s/%d?branch=%d", ProjectChangeSubview.VIEW_NAME, revision.getId(), branch.getId()));
    }

    /**
     * Opens the last revision on the given branch in the change overview.
     *
     * @param revision
     *         The revision to display.
     * @param branch
     *         The branch to display - will only be used as the default selected branch.
     */
    public void openChangeView(Revision revision, Branch branch) {
        Revision revision2 = revisionGraphService.findLastRevisionOnBranch(branch);
        checkState(revision2 != null && revision2.getId().equals(revision.getId()), "Revision on branch is either null or doesn't match requested revision");
        navigateTo(String.format("%s/%d?branch=%d", ProjectChangeSubview.VIEW_NAME, revision.getId(), branch.getId()));
    }

    /**
     * Opens the revision graph for a single revision.
     *
     * @param revision
     *         The revision to display.
     */
    public void openRevisionView(@NotNull Revision revision) {
        navigateTo(String.format("%s/%d", ProjectRevisionSubview.VIEW_NAME, revision.getId()));
    }

    /**
     * Opens the revision graph for a single revision.
     *
     * @param branch
     *         The branch to display..
     */
    public void openRevisionView(@NotNull Branch branch) {
        Revision revision = revisionGraphService.findLastRevisionOnBranch(branch);
        checkState(revision != null, "No last revision on branch - this should never happen");
        navigateTo(String.format("%s/%d?branch=%d", ProjectRevisionSubview.VIEW_NAME, revision.getId(), branch.getId()));
    }

    /**
     * Opens the revision graph for a given branch.
     *
     * @param revision
     *         The revision to display.
     * @param branch
     *         The branch to display - will only be used as the default selected branch.
     */
    public void openRevisionView(@NotNull Revision revision, @NotNull Branch branch) {
        Revision revision2 = revisionGraphService.findLastRevisionOnBranch(branch);
        checkState(revision2 != null && revision2.getId().equals(revision.getId()), "Revision on branch is either null or doesn't match requested revision");
        navigateTo(String.format("%s/%d?branch=%d", ProjectRevisionSubview.VIEW_NAME, revision.getId(), branch.getId()));
    }

    private void navigateTo(String target) {
        UI.getCurrent().getNavigator().navigateTo(target);
    }

    /**
     * Open the project edit view.
     * @param project The project to edit.
     */
    public void openEditProjectView(Project project) {
        navigateTo(AdminEditProjectSubview.VIEW_NAME + "/" + project.getId());
    }
}
