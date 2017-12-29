package org.xlrnet.datac.vcs.util;

import org.xlrnet.datac.vcs.domain.Revision;

import java.util.Comparator;

/**
 * Comparator for revision commit times.
 */
public class RevisionTimestampComparator implements Comparator<Revision> {

    @Override
    public int compare(Revision o1, Revision o2) {
        return o1.getCommitTime().compareTo(o2.getCommitTime());
    }
}
