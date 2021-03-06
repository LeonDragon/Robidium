package com.robidium.demo.compiler.builder.UIPathActions.browser.navigation;

import com.robidium.demo.compiler.builder.ScriptBuilder;
import com.robidium.demo.compiler.builder.UIPathElement;
import org.w3c.dom.Element;

public class NewTab extends UIPathElement implements Tab {
    private Element browserScope;

    public NewTab(ScriptBuilder scriptBuilder) {
        createSendHotKey();
        browserScope = createBrowserScope(doc, NEW_TAB_TITLE);
        createBrowserScopeBody(doc, browserScope, scriptBuilder);
    }

    private void createSendHotKey() {
        Element sendHotKey = doc.createElement("ui:SendHotkey");
        sendHotKey.setAttribute("Activate", "True");
        sendHotKey.setAttribute("DisplayName", "Create New Tab");
        sendHotKey.setAttribute("Key", "t");
        sendHotKey.setAttribute("KeyModifiers", "Ctrl");

        doSequence.appendChild(sendHotKey);
    }

    @Override
    public Element getDomElement() {
        return browserScope;
    }
}
