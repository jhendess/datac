package org.xlrnet.datac.administration.ui.views.eventlog;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.TemporalRenderer;
import org.xlrnet.datac.commons.util.DisplayUtils;
import org.xlrnet.datac.commons.util.QueryUtils;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.services.EventLogService;

import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ButtonRenderer;

/**
 * Re-usable layout which displays events.
 */
@Component
@UIScope
public class EventLogLayout extends MVerticalLayout {

    private final EventLogService eventLogService;

    @Autowired
    public EventLogLayout(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostConstruct
    public void init() {
        withFullSize();

        MGrid<EventLogMessage> logMessageGrid = new MGrid<>();
        logMessageGrid.withFullSize().withResponsive(true);

        logMessageGrid.addColumn(msg -> "Details", new ButtonRenderer<>(e -> openDetails(e.getItem())))
                .setCaption("Details")
                .setMaximumWidth(100);
        logMessageGrid.addColumn(EventLogMessage::getCreatedAt, new TemporalRenderer())
                .setSortProperty("createdAt")
                .setMaximumWidth(200)
                .setCaption("Time");
        logMessageGrid.addColumn(EventLogMessage::getSeverity)
                .setCaption("Severity")
                .setMaximumWidth(115)
                .setSortProperty("severity");       // TODO: Convert to icon
        logMessageGrid.addColumn(EventLogMessage::getProjectName)
                .setCaption("Project")
                .setMaximumWidth(130)
                .setSortProperty("eventLog.project.name");
        logMessageGrid.addColumn(EventLogMessage::getUserName)
                .setCaption("User")
                .setMaximumWidth(130)
                .setSortProperty("eventLog.user.loginName");
        logMessageGrid.addColumn(EventLogMessage::getShortMessage)
                .setCaption("Short message")
                .setSortProperty("shortMessage");

        logMessageGrid.setStyleGenerator(m -> DisplayUtils.severityToStyle(m.getSeverity()));

        logMessageGrid.setDataProvider(
                new FetchEventLogMessagesCallback(),
                () -> Math.toIntExact(eventLogService.countAll())
        );

        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcons.REFRESH);
        refreshButton.addClickListener(e -> logMessageGrid.getDataProvider().refreshAll());

        addComponent(refreshButton);
        addComponent(logMessageGrid);
    }

    private void openDetails(EventLogMessage message) {
        EventLogDetailWindow eventLogDetailWindow = new EventLogDetailWindow(message);
        eventLogDetailWindow.center();
        UI.getCurrent().addWindow(eventLogDetailWindow);
    }

    private class FetchEventLogMessagesCallback implements Grid.FetchItemsCallback<EventLogMessage> {

        @Override
        public Stream<EventLogMessage> fetchItems(List<QuerySortOrder> sortOrder, int offset, int limit) {
            Sort querySortOrder = null;
            if (sortOrder != null && !sortOrder.isEmpty()) {
                querySortOrder = QueryUtils.convertVaadinToSpringSort(sortOrder);
            }
            return eventLogService.findAllMessagesPaged(limit, offset, querySortOrder).stream();
        }
    }
}
