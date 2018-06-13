package org.xlrnet.datac.vcs.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.domain.Project;

import com.google.common.collect.Multimap;

public class CachedRevisionDecorator extends Revision {

    private final Revision delegate;

    private final Multimap<String, String> parentChildMap;

    private final Multimap<String, String> childParentMap;

    private final Map<String, Revision> cachedRevisions;

    public CachedRevisionDecorator(Revision delegate, Map<String, Revision> cachedRevisions, Multimap<String, String> parentChildMap, Multimap<String, String> childParentMap) {
        this.delegate = delegate;
        this.parentChildMap = parentChildMap;
        this.cachedRevisions = cachedRevisions;
        this.childParentMap = childParentMap;
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
        Collection<String> strings = childParentMap.get(this.getInternalId());
        return strings.stream().map(cachedRevisions::get).collect(Collectors.toList());
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
        Collection<String> strings = parentChildMap.get(this.getInternalId());
        return strings.stream().map(cachedRevisions::get).collect(Collectors.toList());
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
