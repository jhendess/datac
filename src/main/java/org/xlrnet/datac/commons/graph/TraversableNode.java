package org.xlrnet.datac.commons.graph;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Single node in a graph which can be traversed by a tree walker.
 */
public interface TraversableNode<T extends TraversableNode> {

    /**
     * Returns all parent nodes of the current node.
     *
     * @return all parent nodes of the current node.
     */
    @NotNull
    Collection<T> getParents();

    /**
     * Returns all child nodes of the current node.
     *
     * @return all child nodes of the current node.
     */
    @NotNull
    Collection<T> getChildren();
}
