package org.xlrnet.datac.commons.ui;

import com.vaadin.ui.renderers.TextRenderer;
import elemental.json.JsonValue;
import org.xlrnet.datac.commons.util.DateTimeUtils;

import java.time.temporal.TemporalAccessor;

/**
 * Implementation of {@link TextRenderer} which formats {@link TemporalAccessor} objects.
 */
public class TemporalRenderer extends TextRenderer {

    @Override
    public JsonValue encode(Object value) {
        TemporalAccessor temporalAccessor = (TemporalAccessor) value;
        if (temporalAccessor != null) {
            return super.encode(DateTimeUtils.format(temporalAccessor));
        } else {
            return super.encode(null);
        }
    }
}
