package com.robidium.demo.compiler.builder.UIPathActions.browser.navigation;

import com.robidium.demo.compiler.builder.ScriptBuilder;
import com.robidium.demo.compiler.builder.UIPathElement;
import com.robidium.demo.compiler.entity.Action;
import org.w3c.dom.Element;

public class SelectTab extends UIPathElement implements Tab {
    private Action action;
    private Element browserScope;

    public SelectTab(Action action, ScriptBuilder scriptBuilder) {
        this.action = action;
        createSelectTab(scriptBuilder);
    }

    private void createSelectTab(ScriptBuilder scriptBuilder) {
        browserScope = createBrowserScope(doc, action.getTargetTitle());
        createBrowserScopeBody(doc, browserScope, scriptBuilder);
    }

    @Override
    public Element getDomElement() {
        return browserScope;
    }
}
