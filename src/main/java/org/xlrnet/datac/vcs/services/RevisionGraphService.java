package org.xlrnet.datac.vcs.services;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.domain.LimitOffsetPageable;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.foundation.services.ValidationService;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.domain.repository.RevisionRepository;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service for accessing and manipulating VCS revision graphs.
 */
@Service
@Transactional
public class RevisionGraphService extends AbstractTransactionalService<Revision, RevisionRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevisionGraphService.class);

    /**
     * Project repository.
     */
    private final ProjectRepository projectRepository;

    /**
     * Bean validation service.
     */
    private final ValidationService validator;

    /**
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private final BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param projectRepository
     * @param validator
     */
    @Autowired
    public RevisionGraphService(RevisionRepository crudRepository, ProjectRepository projectRepository, ValidationService validator) {
        super(crudRepository);
        this.projectRepository = projectRepository;
        this.validator = validator;
    }

    /**
     * Find all revisions of a single project.
     *
     * @param project
     *         The project.
     * @return All unordered revisions in the project.
     */
    @NotNull
    @Transactional(readOnly = true)
    public List<Revision> findAllByProject(@NotNull Project project) {
        return getRepository().findAllByProject(project);
    }

    /**
     * Checks if a revision with the given ID exists in the given project.
     *
     * @param project
     *         The project in which the revision should exist.
     * @param revisionId
     *         The id of the revision.
     * @return True if the revision exists, false if not.
     */
    @Transactional(readOnly = true)
    public boolean existsRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().countRevisionByInternalIdAndProject(revisionId, project) > 0;
    }

    @Transactional(readOnly = true)
    @Nullable
    public Revision findByInternalIdAndProject(String internalId, Project project) {
        return getRepository().findByInternalIdAndProject(internalId, project);
    }

    /**
     * Returns a list of the last revisions in the given project. The returned revisions are ordered by their timestamp.
     * Note, that this orders only by the timestamp when the revision was committed.
     *
     * @param project
     *         The project in which the revisions must lie.
     * @param limit
     *         Amount of revisions to find.
     * @return a list of the last revisions in the given project.
     */
    @NotNull
    @Transactional(readOnly = true)
    public List<Revision> findLastRevisionsInProjectPaged(Project project, int limit, int offset) {
        return getRepository().findAllByProject(project, new LimitOffsetPageable(limit, offset, new Sort(
                new Sort.Order(Sort.Direction.DESC, "commitTime"))
        ));
    }

    /**
     * Fetches the revision with a given id in a given project.
     *
     * @param project
     *         The project in which the revision exists.
     * @param revisionId
     *         The revision id.
     * @return The revision if it exists, or null.
     */
    @Nullable
    @Transactional(readOnly = true)
    public Revision findRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().findByInternalIdAndProject(revisionId, project);
    }

    /**
     * Performs a safe save operation without causing a stack overflow by performing a depth-first traversal and saving
     * each revision on its own.
     *
     * @param revision
     *         The revision to save.
     * @return The saved revision.
     */
    @Override
    @Transactional
    public Revision save(@NotNull Revision revision) {
        LOGGER.trace("Begin safe saving of revision graph");
        Multimap<Revision, Revision> revisionChildMap = MultimapBuilder.hashKeys().linkedListValues().build();
        Deque<Revision> revisionStack = new LinkedList<>();
        revisionStack.push(revision);

        while (!revisionStack.isEmpty()) {
            Revision nextRevision = revisionStack.peek();
            if (nextRevision.getParents().isEmpty()) {
                saveAndReplaceChildren(nextRevision, revisionChildMap);
                revisionStack.pop();
                continue;
            }
            boolean allParentsSave = true;
            for (Revision parent : nextRevision.getParents()) {
                revisionChildMap.put(parent, nextRevision);
                if (!parent.isPersisted()) {
                    revisionStack.push(parent);
                }
                allParentsSave &= parent.isPersisted();
            }
            // Replace children after all parents were saved
            if (allParentsSave) {
                saveAndReplaceChildren(nextRevision, revisionChildMap);
                revisionStack.pop();
            }
        }
        LOGGER.trace("Finished safe saving of revision graph");

        return findRevisionInProject(revision.getProject(), revision.getInternalId());
    }


    /**
     * Returns the root revision in a project - i.e. the earliest revision without any parents.
     *
     * @param project
     *         The project to check
     * @return
     */
    @Transactional(readOnly = true)
    public Revision findProjectRootRevision(Project project) {
        checkArgument(project.isPersisted(), "Project must be persisted");
        return getRepository().findProjectRootRevision(project.getId());
    }

    /**
     * Finds all internal {@link Revision} objects which have the same internal id as the {@link VcsRevision} objects in
     * the given collection. If an object does not exist, it will throw a null pointer exception.
     *
     * @param project
     *         The project in which the revisions must exist
     * @param externalRevisions
     *         The external revisions from a VCS that should be found.
     * @return Internal revision objects with the same internal ids as the given external revision objects.
     */
    @Transactional(readOnly = true)
    public Collection<Revision> findMatchingInternalRevisions(Project project, Collection<VcsRevision> externalRevisions) {
        Set<Revision> internalRevisions = new HashSet<>();
        for (VcsRevision externalRevision : externalRevisions) {
            Revision internalRevision = getRepository().findByInternalIdAndProject(externalRevision.getInternalId(), project);
            LOGGER.debug("Revision {} was not found in project {} [id={}]", externalRevision.getInternalId(), project.getName(), project.getId());
            if (internalRevision != null) {
                internalRevisions.add(internalRevision);
            }
        }

        return internalRevisions;
    }

    /**
     * Recursive implementation which converts external {@link VcsRevision} objects to {@link Revision} entities. If any
     * of the revision objects already exist in the database, the rest of the graph will be fetched from the database.
     * The revision will be saved afterwards. The project must be already persisted or an {@link
     * IllegalArgumentException} will be thrown.
     *
     * @param rootRevision
     *         The root revision used for starting the conversion.
     * @param project
     *         The project in which the revisions will be stored.
     * @return A pair containing the converted revision and the number of new revisions.
     */
    @Transactional
    public Pair<Revision, Long> convertRevisionAndSave(VcsRevision rootRevision, Project project) {
        Pair<Revision, Long> convertedRevision = convertRevision(rootRevision, project);
        Revision savedRevision = save(convertedRevision.getLeft());
        return ImmutablePair.of(savedRevision, convertedRevision.getRight());
    }

    /**
     * Finds the last revisions on the given branch.
     *
     * @param branch
     *         The branch on which to search.
     * @param amount
     *         The amount of revisions to return.
     * @return The last x revisions on the given branch.
     * @throws DatacTechnicalException
     */
    public List<Revision> findLastRevisionsOnBranch(@NotNull Branch branch, int amount) throws DatacTechnicalException {
        Project project = branch.getProject();
        Revision lastDevRevision = findByInternalIdAndProject(branch.getInternalId(), project);
        List<Revision> revisions = new ArrayList<>(amount);
        breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, revisions::add, (r -> revisions.size() >= amount));

        return revisions;
    }

    /**
     * Returns the latest revision on the given branch.
     *
     * @param branch
     *         The branch on which to get the revision.
     * @return the latest revision on the given branch.
     */
    @Nullable
    @Transactional(readOnly = true)
    public Revision findLastRevisionOnBranch(@NotNull Branch branch) {
        Project project = branch.getProject();
        return findByInternalIdAndProject(branch.getInternalId(), project);
    }

    /**
     * Returns a collection of all revisions which are merge revisions (i.e. all revisions which have more than one
     * parent).
     *
     * @param project
     *         The project in which the revisions must lie.
     * @return
     */
    @NotNull
    @Transactional(readOnly = true)
    public Iterable<Revision> findMergeRevisionsInProject(Project project) {
        checkArgument(project.isPersisted(), "Project must be persisted");

        List<BigInteger> mergeRevisionIdsInProject = this.getRepository().findMergeRevisionIdsInProject(project.getId());
        return getRepository().findAll(mergeRevisionIdsInProject.stream().map(BigInteger::longValue).collect(Collectors.toList()));
    }

    /**
     * Recursive implementation which converts external {@link VcsRevision} objects to {@link Revision} entities. If any
     * of the revision objects already exist in the database, the rest of the graph will be fetched from the database.
     * Use this method only if you only want to convert but not save the revision. Consider {@link
     * #convertRevisionAndSave(VcsRevision, Project)} to save the graph also in the same transaction.
     *
     * @param rootRevision
     *         The root revision used for starting the conversion.
     * @param project
     *         The project in which the revisions will be stored.
     * @return A pair containing the converted revision and the number of new revisions.
     */
    @NotNull
    @Transactional(readOnly = true)
    protected Pair<Revision, Long> convertRevision(@NotNull VcsRevision rootRevision, @NotNull Project project) {
        checkArgument(project.isPersisted(), "Project must be persisted");
        Project reloadedProject = projectRepository.findOne(project.getId());

        Map<String, Revision> revisionMap = buildRevisionMap(rootRevision, reloadedProject);
        long newRevisions = collectParents(rootRevision, revisionMap);

        return ImmutablePair.of(revisionMap.get(rootRevision.getInternalId()), newRevisions);
    }

    private long collectParents(@NotNull VcsRevision rootRevision, Map<String, Revision> revisionMap) {
        Set<String> importedRevisions = new HashSet<>(revisionMap.size());
        Queue<VcsRevision> revisionsToImport = new LinkedList<>();
        revisionsToImport.add(rootRevision);
        long newRevisions = 0;

        while (!revisionsToImport.isEmpty()) {
            VcsRevision revision = revisionsToImport.poll();
            String internalId = revision.getInternalId();
            if (importedRevisions.contains(internalId)) {
                LOGGER.trace("Encountered visited revision {}", internalId);
                continue;
            } else {
                importedRevisions.add(internalId);
            }
            Revision converted = revisionMap.get(internalId);
            if (converted == null) {
                LOGGER.trace("Revision {} is already imported", internalId);
                continue;
            }
            for (VcsRevision parent : revision.getParents()) {
                Revision convertedParent = revisionMap.get(parent.getInternalId());
                if (convertedParent == null) {
                    // The parent will be missing, because its child already exists
                    continue;
                }
                LOGGER.trace("Adding revision {} as parent of {}", parent.getInternalId(), internalId);
                converted.addParent(convertedParent);
                revisionsToImport.add(parent);
                newRevisions++;
            }
        }
        return newRevisions;
    }


    @NotNull
    private Map<String, Revision> buildRevisionMap(@NotNull VcsRevision rootRevision, @NotNull Project project) {
        Map<String, Revision> revisionMap = new HashMap<>();
        Queue<VcsRevision> revisionsToConvert = new LinkedList<>();
        revisionsToConvert.add(rootRevision);

        while (!revisionsToConvert.isEmpty()) {
            VcsRevision revision = revisionsToConvert.poll();
            validator.checkConstraints(revision);
            String internalId = revision.getInternalId();
            if (revisionMap.containsKey(internalId)) {
                // Already visited this revision - skip other parents
                LOGGER.trace("Found visited revision {}", internalId);
                continue;
            }
            Revision converted = findRevisionInProject(project, internalId);
            if (converted != null) {
                // Existing revision means that all parents have already been persisted - skip other parents
                LOGGER.trace("Found existing revision {} in database", internalId);
            } else {
                // Convert new revision and add all its parents to the queue
                converted = new Revision(revision).setProject(project);
                LOGGER.trace("Found new revision {}", internalId);
                revisionsToConvert.addAll(revision.getParents());
            }
            revisionMap.put(internalId, converted);
        }
        return revisionMap;
    }

    private void saveAndReplaceChildren(Revision revisionToPersist, Multimap<Revision, Revision> revisionChildMap) {
        LOGGER.trace("Saving revision {}", revisionToPersist.getInternalId());
        Revision persistedRevision = super.save(revisionToPersist);
        for (Revision child : revisionChildMap.get(revisionToPersist)) {
            child.replaceParent(revisionToPersist, persistedRevision);
        }
    }
}
