package org.xlrnet.datac.administration.ui.views.eventlog;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.util.QueryUtils;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.services.EventLogService;

import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;

/**
 * Re-usable layout which displays events.
 */
@Component
public class EventLogLayout extends HorizontalLayout {

    private final EventLogService eventLogService;

    @Autowired
    public EventLogLayout(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostConstruct
    public void init() {
        setWidth("100%");

        Grid<EventLogMessage> logMessageGrid = new Grid<>();
        logMessageGrid.setWidth("80%");

        logMessageGrid.addColumn(EventLogMessage::getCreatedAt)
                .setSortProperty("createdAt")
                .setCaption("Time");
        logMessageGrid.addColumn(EventLogMessage::getSeverity)
                .setCaption("Severity")
                .setSortProperty("severity");       // TODO: Convert to icon
        logMessageGrid.addColumn(EventLogMessage::getProjectName)
                .setCaption("Project")
                .setSortProperty("project");
        logMessageGrid.addColumn(EventLogMessage::getUserName)
                .setCaption("User")
                .setSortProperty("user");
        logMessageGrid.addColumn(EventLogMessage::getShortMessage)
                .setCaption("Short message")
                .setSortProperty("shortMessage");

        logMessageGrid.setDataProvider(
                new FetchEventLogMessagesCallback(),
                () -> Math.toIntExact(eventLogService.countAll())
        );

        addComponent(logMessageGrid);
    }

    private class FetchEventLogMessagesCallback implements Grid.FetchItemsCallback<EventLogMessage> {

        @Override
        public Stream<EventLogMessage> fetchItems(List<QuerySortOrder> sortOrder, int offset, int limit) {
            Sort querySortOrder = null;
            if (sortOrder != null && sortOrder.size() > 0) {
                querySortOrder = querySortOrder = QueryUtils.convertVaadinToSpringSort(sortOrder);
            }
            return eventLogService.findAllMessagesPaged(limit, offset, querySortOrder).stream();
        }
    }
}
