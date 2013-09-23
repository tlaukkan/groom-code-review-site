/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groom;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Entry list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class LogFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    private TextField since;
    private TextField until;

    @Override
    public String getFlowletKey() {
        return "log";
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void initialize() {

        final GridLayout gridLayout = new GridLayout(1, 3);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(2, 1f);
        setViewContent(gridLayout);

        final HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setSpacing(true);
        filterLayout.setSizeUndefined();
        gridLayout.addComponent(filterLayout, 0, 0);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setSizeUndefined();
        gridLayout.addComponent(buttonLayout, 0, 1);

        final Validator validator = new Validator() {
            @Override
            public void validate(Object o) throws InvalidValueException {
                final String value = (String) o;
                if (value.indexOf("..") != -1) {
                    throw new InvalidValueException("..");
                }
                for (int i = 0; i < value.length(); i++) {
                    final char c = value.charAt(i);
                    if (!(Character.isLetter(c) || Character.isDigit(c) || "-.".indexOf(c) != -1)) {
                        throw new InvalidValueException("" + c);
                    }
                }
            }
        };

        since = new TextField(getSite().localize("field-since"));
        since.setValue("master");
        since.setValidationVisible(true);
        since.addValidator(validator);
        filterLayout.addComponent(since);

        until = new TextField(getSite().localize("field-until"));
        until.setValidationVisible(true);
        until.addValidator(validator);
        filterLayout.addComponent(until);

        final BeanQueryFactory<CommitBeanQuery> beanQueryFactory =
                new BeanQueryFactory<CommitBeanQuery>(CommitBeanQuery.class);

        final LazyQueryContainer container = new LazyQueryContainer(beanQueryFactory,"hash",
                20, false);

        container.addContainerFilter(new Compare.Equal("branch", since.getValue()));

        container.addContainerProperty("hash", String.class, null, true, false);
        container.addContainerProperty("committerDate", Date.class, null, true, false);
        container.addContainerProperty("committer", String.class, null, true, false);
        container.addContainerProperty("authorDate", Date.class, null, true, false);
        container.addContainerProperty("author", String.class, null, true, false);
        container.addContainerProperty("subject", String.class, null, true, false);

        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Table table = new Table() {

            @Override
            protected String formatPropertyValue(Object rowId, Object colId,
                                                 Property property) {
                Object v = property.getValue();
                if (v instanceof Date) {
                    Date dateValue = (Date) v;
                    return format.format(v);
                }
                return super.formatPropertyValue(rowId, colId, property);
            }

        };

        table.setSizeFull();
        table.setContainerDataSource(container);
        table.setVisibleColumns(new Object[] {
                "hash",
                "committerDate",
                "committer",
                "authorDate",
                "author",
                "subject"});

        table.setColumnWidth("hash", 50);
        table.setColumnWidth("committerDate", 120);
        table.setColumnWidth("committer", 100);
        table.setColumnWidth("authorDate", 120);
        table.setColumnWidth("author", 100);

        table.setColumnHeaders(new String[]{
                getSite().localize("field-hash"),
                getSite().localize("field-committer-date"),
                getSite().localize("field-committer"),
                getSite().localize("field-author-date"),
                getSite().localize("field-author"),
                getSite().localize("field-subject")
        });

        table.setColumnCollapsingAllowed(true);
        table.setColumnCollapsed("authorDate", true);
        table.setColumnCollapsed("author", true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        gridLayout.addComponent(table, 0, 2);

        final Button refreshButton = getSite().getButton("refresh");
        buttonLayout.addComponent(refreshButton);
        refreshButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        refreshButton.addStyleName("default");
        refreshButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                if (since.isValid() && until.isValid()) {
                    final StringBuilder range = new StringBuilder(since.getValue());
                    if (until.getValue().length() > 0) {
                        range.append("..");
                        range.append(until);
                    }
                    container.removeAllContainerFilters();
                    container.addContainerFilter(new Compare.Equal("range", range.toString()));
                    container.refresh();
                }
            }
        });

    }

    @Override
    public void enter() {
    }

}
