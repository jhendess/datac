package org.xlrnet.datac.vcs.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.xlrnet.datac.vcs.domain.Revision;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Basic container which holds cached revision data for a single project.
 */
public class ProjectRevisionCache {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Multimap<String, String> parentChildMap = MultimapBuilder.hashKeys().arrayListValues(2).build();

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Multimap<String, String> childParentMap = MultimapBuilder.hashKeys().arrayListValues(2).build();

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Map<String, Revision> revisionMap = new HashMap<>();

    /**
     * Returns all parent revisions of the revision with the given internal id.
     * @param internalId The internal of the revision.
     * @return all parent revisions of the revision with the given internal id.
     */
    public List<Revision> getParents(String internalId) {
        Collection<String> strings = childParentMap.get(internalId);
        return strings.stream().map(revisionMap::get).collect(Collectors.toList());
    }

    /**
     * Returns all children revisions of the revision with the given internal id.
     * @param internalId The internal of the revision.
     * @return all children revisions of the revision with the given internal id.
     */
    public List<Revision> getChildren(String internalId) {
        Collection<String> strings = parentChildMap.get(internalId);
        return strings.stream().map(revisionMap::get).collect(Collectors.toList());
    }

    public Revision getRevisionByInternalId(String internalId) {
        return getRevisionMap().get(internalId);
    }
}
