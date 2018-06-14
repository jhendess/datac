package org.xlrnet.datac.vcs.util;

import java.time.Instant;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.ProjectRevisionCache;

/**
 * Decorator for {@link Revision} which operates on cached data. All attempts to modify any data throw a {@link UnsupportedOperationException}.
 */
public class CachedRevisionDecorator extends Revision {

    /** The delegate which is decorated. */
    private final Revision delegate;

    /** The cached data. */
    private final ProjectRevisionCache cache;

    public CachedRevisionDecorator(Revision delegate, ProjectRevisionCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Long getId() {
        return delegate.getId();
    }

    @Override
    public void setId(Long id) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    @NotNull
    public String getInternalId() {
        return delegate.getInternalId();
    }

    @Override
    public Revision setInternalId(String internalId) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public String getAuthor() {
        return delegate.getAuthor();
    }

    @Override
    public Revision setAuthor(String author) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public String getReviewer() {
        return delegate.getReviewer();
    }

    @Override
    public Revision setReviewer(String reviewer) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public Revision setMessage(String message) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public Instant getCommitTime() {
        return delegate.getCommitTime();
    }

    @Override
    public Revision setCommitTime(Instant commitTime) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    @NotNull
    public List<Revision> getParents() {
        return cache.getParents(this.getInternalId());
    }

    @Override
    public Revision setParents(List<Revision> parents) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public Revision addParent(@NotNull Revision parent) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public Revision addChild(@NotNull Revision child) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public Revision replaceParent(@NotNull Revision oldParent, @NotNull Revision newParent) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    public Project getProject() {
        return delegate.getProject();
    }

    @Override
    public Revision setProject(Project project) {
        throw new UnsupportedOperationException("Modification is not allowed");
    }

    @Override
    @NotNull
    public List<Revision> getChildren() {
        return cache.getChildren(this.getInternalId());
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
