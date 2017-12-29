package org.xlrnet.datac.commons.graph;

import com.google.common.collect.Ordering;
import org.apache.commons.collections.ComparatorUtils;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.util.ThrowingConsumer;

import java.util.*;
import java.util.function.Function;

/**
 * Traverses a graph of {@link TraversableNode} objects using a depth first algorithm.
 */
public class DepthFirstTraverser<T extends TraversableNode> {

    /**
     * Traverses the given node and its children of the given traversable node and performs a given action on each node.
     * If the given precondition function returns false or the current has been visited, the current branch will be cut
     * and won't be traversed any further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed on each node.
     * @param matcher
     *         The precondition which must be true, or the branch will be cut.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseChildrenCutOnMatch(T nodeToBegin, ThrowingConsumer<T> action, Function<T, Boolean> matcher) throws DatacTechnicalException {
        genericCutOnMatch(T::getChildren, nodeToBegin, ComparatorUtils.naturalComparator(), false, action, matcher);
    }

    /**
     * Traverses the given node and its parents of the given traversable node and performs a given action on each node.
     * If the given precondition function returns false or the current has been visited, the current branch will be cut
     * and won't be traversed any further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed on each node.
     * @param matcher
     *         The precondition which must be true, or the branch will be cut.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseParentsCutOnMatch(T nodeToBegin, ThrowingConsumer<T> action, Function<T, Boolean> matcher) throws DatacTechnicalException {
        traverseParentsCutOnMatch(nodeToBegin, action, matcher, false);
    }

    /**
     * Traverses the given node and its parents of the given traversable node and performs a given action on each node.
     * If the given precondition function returns false, the current branch will be cut and won't be traversed any further. Nodes may be revisited multiple times.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed on each node.
     * @param matcher
     *         The precondition which must be true, or the branch will be cut.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseParentsCutOnMatchVisitRevisited(T nodeToBegin, ThrowingConsumer<T> action, Function<T, Boolean> matcher) throws DatacTechnicalException {
        traverseParentsCutOnMatch(nodeToBegin, action, matcher, true);
    }

    /**
     * Traverses the given node and its parents of the given traversable node and performs a given action on each node.
     * If the given precondition function returns false or the current has been visited, the current branch will be cut
     * and won't be traversed any further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed on each node.
     * @param matcher
     *         The precondition which must be true, or the branch will be cut.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    private void traverseParentsCutOnMatch(T nodeToBegin, ThrowingConsumer<T> action, Function<T, Boolean> matcher, boolean revisitVisited) throws DatacTechnicalException {
        genericCutOnMatch(T::getParents, nodeToBegin, ComparatorUtils.naturalComparator(), revisitVisited, action, matcher);
    }

    public void genericCutOnMatch(@NotNull Function<T, Collection<T>> supplier, @NotNull T nodeToBegin, @NotNull Comparator<T> comparator, boolean revisitVisited, @NotNull ThrowingConsumer<T> action, @NotNull Function<T, Boolean> matcher) throws DatacTechnicalException {
        Set<T> visited = new HashSet<>();
        Deque<T> nodeStack = new LinkedList<>();
        nodeStack.push(nodeToBegin);
        Ordering ordering = Ordering.from(comparator);
        while (!nodeStack.isEmpty()) {
            T next = nodeStack.pop();
            if (revisitVisited || !visited.contains(next)) {
                visited.add(next);
                action.accept(next);
                if (matcher.apply(next)) {
                    Collection<T> supplied = supplier.apply(next);
                    List<T> ordered = ordering.sortedCopy(supplied);
                    for (Object node : ordered) {
                        nodeStack.push((T) node);
                    }
                }
            }
        }
    }
}
