package org.xlrnet.datac.commons.domain;

import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.util.ThrowingConsumer;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;

/**
 * Traverses a graph of {@link TraversableNode} objects using a breadth first algorithm.
 */
public class BreadthFirstTraverser<T extends TraversableNode> {

    /**
     * Traverses the given node and its children of the given traversable node and performs a given action when the
     * given matcher returns true. If the action is executed, the current branch is cut and won't be traversed any
     * further.
     *
     * @param nodeToBegin
     *         The node where the traversal should begin.
     * @param matcher
     *         The matcher function.
     * @param action
     *         The action that will be executed when the matcher matches.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseChildrenCutOnMatch(T nodeToBegin, Function<T, Boolean> matcher, ThrowingConsumer<T> action) throws DatacTechnicalException {
        Set<T> visited = new HashSet<>();
        Deque<T> candidates = new LinkedList<>();
        candidates.add(nodeToBegin);
        while (!candidates.isEmpty()) {
            T next = candidates.pop();
            if (!visited.contains(next)) {
                visited.add(next);
                if (matcher.apply(next)) {
                    action.accept(next);
                } else {
                    candidates.addAll(next.getChildren());
                }
            }
        }
    }

}
