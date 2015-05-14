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
package org.groom.review.ui.flows.admin;

import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.bubblecloud.ilves.component.flow.AbstractFlowlet;
import org.bubblecloud.ilves.component.grid.FieldDescriptor;
import org.bubblecloud.ilves.component.grid.FilterDescriptor;
import org.bubblecloud.ilves.component.grid.FormattingTable;
import org.bubblecloud.ilves.component.grid.Grid;
import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.util.ContainerUtil;
import org.groom.review.ui.ReviewFields;
import org.groom.shell.Shell;
import org.groom.model.Repository;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class RepositoriesFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The container. */
    private LazyEntityContainer<Repository> container;
    /** The grid. */
    private Grid grid;

    @Override
    public String getFlowletKey() {
        return "repositories";
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
        final List<FieldDescriptor> fieldDescriptors = ReviewFields.getFieldDescriptors(Repository.class);

        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();

        final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        container = new LazyEntityContainer<Repository>(entityManager, true, true, false, Repository.class, 1000,
                new String[] {"path"},
                new boolean[] {true}, "repositoryId");

        ContainerUtil.addContainerProperties(container, fieldDescriptors);

        final GridLayout gridLayout = new GridLayout(1, 2);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(1, 1f);
        setViewContent(gridLayout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setSizeUndefined();
        gridLayout.addComponent(buttonLayout, 0, 0);

        final Table table = new FormattingTable();
        grid = new Grid(table, container);
        grid.setFields(fieldDescriptors);
        grid.setFilters(filterDefinitions);
        grid.setWidth(100, Unit.PERCENTAGE);
        grid.setHeight(UI.getCurrent().getPage().getBrowserWindowHeight() - 285, Unit.PIXELS);

        table.setColumnCollapsed("repositoryId", true);
        gridLayout.addComponent(grid, 0, 1);

        final Button addButton = getSite().getButton("add");
        buttonLayout.addComponent(addButton);
        addButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Repository repository = new Repository();
                repository.setCreated(new Date());
                repository.setModified(repository.getCreated());
                repository.setOwner((Company) getSite().getSiteContext().getObject(Company.class));
                final RepositoryFlowlet repositoryView = getFlow().forward(RepositoryFlowlet.class);
                repositoryView.edit(repository, true);
            }
        });

        final Button editButton = getSite().getButton("edit");
        buttonLayout.addComponent(editButton);
        editButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Repository entity = container.getEntity(grid.getSelectedItemId());
                final RepositoryFlowlet repositoryView = getFlow().forward(RepositoryFlowlet.class);
                repositoryView.edit(entity, false);
            }
        });

        final Button removeButton = getSite().getButton("remove");
        buttonLayout.addComponent(removeButton);
        removeButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                container.removeItem(grid.getSelectedItemId());
                container.commit();
            }
        });

        final Button cloneButton = new Button("Clone");
        buttonLayout.addComponent(cloneButton);
        cloneButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Repository entity = container.getEntity(grid.getSelectedItemId());
                Notification.show(Shell.execute("git clone --mirror " + entity.getUrl() + " " + entity.getPath(), ""),
                        Notification.Type.TRAY_NOTIFICATION);
            }
        });

        final Company company = getSite().getSiteContext().getObject(Company.class);
        container.removeDefaultFilters();
        container.addDefaultFilter(
                new Compare.Equal("owner.companyId", company.getCompanyId()));
        grid.refresh();
    }

    @Override
    public void enter() {
        container.refresh();
    }

}
