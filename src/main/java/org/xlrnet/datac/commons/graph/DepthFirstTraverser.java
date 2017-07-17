package org.xlrnet.datac.commons.graph;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;

import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.util.ThrowingConsumer;

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
     * @param preCondition
     *         The precondition which must be true, or the branch will be cut.
     * @param action
     *         The action that will be executed on each node.
     * @throws DatacTechnicalException
     *         May be thrown by the action.
     */
    public void traverseChildrenAbortOnConditionFailure(T nodeToBegin, Function<T, Boolean> preCondition, ThrowingConsumer<T> action) throws DatacTechnicalException {
        Set<T> visited = new HashSet<>();
        Deque<T> nodeStack = new LinkedList<>();
        nodeStack.push(nodeToBegin);
        while (!nodeStack.isEmpty()) {
            T next = nodeStack.pop();
            if (!visited.contains(next)) {
                visited.add(next);
                if (preCondition.apply(next)) {
                    action.accept(next);
                    for (Object node : next.getChildren()) {
                        nodeStack.push((T) node);
                    }
                }
            }
        }
    }
}
