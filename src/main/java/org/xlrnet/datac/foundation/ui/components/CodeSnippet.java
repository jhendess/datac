package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.UUID;

/**
 * Code snippet which may be formatted by Javascript. Wraps code in pre- and code-tags and escapes HTML. Each element will be generated with a unique id.
 */
public class CodeSnippet extends Label {

    private final String generatedId;

    public CodeSnippet(String content) {
        super();
        generatedId = UUID.randomUUID().toString();
        setValue(content);
        setContentMode(ContentMode.HTML);
    }

    @Override
    public void setValue(String value) {
        String escapedCode = StringEscapeUtils.escapeHtml4(StringEscapeUtils.escapeHtml4(value));
        String formattedValue = String.format("<pre id=\"%s\" class=\"%s\"><code>%s</pre></code>", generatedId, "language-sql", escapedCode);
        super.setValue(formattedValue);
    }

    /**
     * Run client-side javascript to highlight this codesnippet using prism.js - the component must be visible for this to work correctly.
     */
    public void formatWithPrism() {
        JavaScript.getCurrent().execute(String.format("Prism.highlightElement(document.getElementById(\"%s\"));", generatedId));
    }

    /**
     * Returns the id used in the pre-tag.
     */
    public String getGeneratedId() {
        return generatedId;
    }
}
