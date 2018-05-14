package org.xlrnet.datac.database.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xlrnet.datac.database.domain.IDatabaseInstance;

import com.vaadin.data.ValueProvider;

/**
 * Implementation of {@link ValueProvider} which returns a HTML snippet containing the icon of the given {@link IDatabaseInstance}.
 */
public class DatabaseInstanceIconProvider implements ValueProvider<IDatabaseInstance, String> {
    @Override
    public String apply(IDatabaseInstance databaseInstance) {
        String iconHtml = databaseInstance.getInstanceType().getIconResource().getHtml();
        return String.format("%s %s", iconHtml, StringEscapeUtils.escapeHtml4(databaseInstance.getName()));
    }
}
