package org.xlrnet.datac.commons.graph;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;

import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.util.ThrowingConsumer;

/**
 * Traverses a graph of {@link TraversableNode} objects using a breadth first algorithm.
 */
public class BreadthFirstTraverser<T extends TraversableNode> {

    /**
     * Traverses the given node and its children of the given traversable node and performs a given action.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed when the matcher matches.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseChildren(T nodeToBegin, ThrowingConsumer<T> action) throws DatacTechnicalException {
        genericTraverseCutOnMatch(TraversableNode::getChildren, nodeToBegin, (n -> false), action);
    }

    /**
     * Traverses the given node and its children of the given traversable node and performs a given action when the
     * given matcher returns true. If the action is executed, the current branch is cut and won't be traversed any
     * further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param matcher
     *         The matcher function. If this function returns true, the branch will be cut and traversal resumes on
     *         another.
     * @param action
     *         The action that will be executed when the matcher matches.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseChildrenCutOnMatch(T nodeToBegin, Function<T, Boolean> matcher, ThrowingConsumer<T> action) throws DatacTechnicalException {
        genericTraverseCutOnMatch(TraversableNode::getChildren, nodeToBegin, matcher, action);
    }

    /**
     * Traverses the given node and its parents of the given traversable node and performs a given action when the
     * given matcher returns true. If the action is executed, the current branch is cut and won't be traversed any
     * further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param action
     *         The action that will be executed.
     * @param matcher
     *         The matcher function. If this function returns true, the branch will be cut and traversal resumes on
     *         another.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseParentsCutOnMatch(T nodeToBegin, ThrowingConsumer<T> action, Function<T, Boolean> matcher) throws DatacTechnicalException {
        genericTraverseCutOnMatch(TraversableNode::getParents, nodeToBegin, matcher, action);
    }

    private void genericTraverseCutOnMatch(Function<T, Collection<T>> supplier, T nodeToBegin, Function<T, Boolean> matcher, ThrowingConsumer<T> action) throws DatacTechnicalException {
        Set<T> visited = new HashSet<>();
        Deque<T> candidates = new LinkedList<>();
        candidates.add(nodeToBegin);
        while (!candidates.isEmpty()) {
            T next = candidates.pop();
            if (next != null && !visited.contains(next)) {
                visited.add(next);
                action.accept(next);
                Boolean returnValue = matcher.apply(next);
                if (!returnValue) {
                    candidates.addAll(supplier.apply(next));
                }
            }
        }
    }
}
