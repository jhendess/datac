package org.xlrnet.datac.foundation.ui.services;

import com.vaadin.ui.UI;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.ui.views.ProjectChangeSubview;
import org.xlrnet.datac.foundation.ui.views.ProjectRevisionSubview;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import static com.google.common.base.Preconditions.checkState;

/**
 * Service which provides convenience methods for navigation.
 */
@Service
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
     * Opens the revision graph for a single revision.
     *
     * @param revision
     *         The revision to display.
     */
    public void openRevisionView(@NotNull Revision revision) {
        navigateTo(String.format("%s/%d", ProjectRevisionSubview.VIEW_NAME, revision.getId()));
    }

    /**
     * Opens the revision graph for a given branch.
     *
     * @param branch
     *         The branch to display.
     */
    public void openRevisionView(@NotNull Revision revision, @NotNull Branch branch) {
        Revision revision2 = revisionGraphService.findLastRevisionOnBranch(branch);
        checkState(revision2 != null && revision2.getId().equals(revision.getId()), "Revision on branch is either null or doesn't match requested revision");
        navigateTo(String.format("%s/%d?branch=%d", ProjectRevisionSubview.VIEW_NAME, revision.getId(), branch.getId()));
    }

    private void navigateTo(String target) {
        UI.getCurrent().getNavigator().navigateTo(target);
    }
}
