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
package org.groom.translation.model;

import com.vaadin.data.Property;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.shared.ui.MarginInfo;
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
import org.groom.model.Repository;
import org.groom.review.dao.ReviewDao;
import org.groom.translation.ui.TranslationFields;
import org.groom.translation.service.TranslationSynchronizer;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entry list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class EntriesFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The container. */
    private LazyEntityContainer<Entry> container;
    /** The grid. */
    private Grid grid;
    /** The repository field. */
    private ComboBox repositoryField;

    @Override
    public String getFlowletKey() {
        return "entries";
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
        final List<FieldDescriptor> fieldDescriptors = TranslationFields.getFieldDescriptors(Entry.class);

        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();

        filterDefinitions.add(new FilterDescriptor("basename", "basename", "Basename", new TextField(),
                200, "like", String.class, ""));

        filterDefinitions.add(new FilterDescriptor("language", "language", "Language", new TextField(),
                30, "=", String.class, ""));

        filterDefinitions.add(new FilterDescriptor("country", "country", "Country", new TextField(),
                30, "=", String.class, ""));

        filterDefinitions.add(new FilterDescriptor("key", "key", "Key", new TextField(),
                200, "like", String.class, ""));

        filterDefinitions.add(new FilterDescriptor("value", "value", "Value", new TextField(),
                200, "like", String.class, ""));

        final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        container = new LazyEntityContainer<Entry>(entityManager, true, true, false, Entry.class, 1000,
                new String[] {"basename", "key", "language", "country"},
                new boolean[] {true, true, true, true}, "entryId");

        ContainerUtil.addContainerProperties(container, fieldDescriptors);

        final GridLayout gridLayout = new GridLayout(1, 3);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(1, 1f);
        setViewContent(gridLayout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setSizeUndefined();
        gridLayout.addComponent(buttonLayout, 0, 1);

        final Table table = new FormattingTable();
        grid = new Grid(table, container);
        grid.setFields(fieldDescriptors);
        grid.setFilters(filterDefinitions);

        table.setColumnCollapsed("entryId", true);
        table.setColumnCollapsed("path", true);
        table.setColumnCollapsed("created", true);
        table.setColumnCollapsed("modified", true);
        grid.setHeight(UI.getCurrent().getPage().getBrowserWindowHeight() - 350, Unit.PIXELS);

        gridLayout.addComponent(grid, 0, 2);

        /*final Button addButton = getSite().getButton("add");
        buttonLayout.addComponent(addButton);
        addButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Entry entry = new Entry();
                entry.setCreated(new Date());
                entry.setModified(entry.getCreated());
                entry.setOwner((Company) getSite().getSiteContext().getObject(Company.class));
                final EntryFlowlet entryView = getFlow().forward(EntryFlowlet.class);
                entryView.edit(entry, true);
            }
        });*/

        final Button editButton = getSite().getButton("edit");
        buttonLayout.addComponent(editButton);
        editButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Entry entity = container.getEntity(grid.getSelectedItemId());
                final EntryFlowlet entryView = getFlow().forward(EntryFlowlet.class);
                entryView.edit(entity, false);
            }
        });

        final Button removeButton = getSite().getButton("remove");
        buttonLayout.addComponent(removeButton);
        removeButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Entry entity = container.getEntity(grid.getSelectedItemId());
                entity.setDeleted(new Date());
                entity.setAuthor(getSite().getSecurityProvider().getUser());
                entityManager.getTransaction().begin();
                try {
                    entityManager.persist(entityManager.merge(entity));
                    entityManager.getTransaction().commit();
                } catch (final Exception e) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    throw new RuntimeException(e);
                }
                container.refresh();
            }
        });

        final Button unRemoveButton = getSite().getButton("unremove");
        buttonLayout.addComponent(unRemoveButton);
        unRemoveButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Entry entity = container.getEntity(grid.getSelectedItemId());
                entity.setDeleted(null);
                entity.setAuthor(getSite().getSecurityProvider().getUser());
                entityManager.getTransaction().begin();
                try {
                    entityManager.persist(entityManager.merge(entity));
                    entityManager.getTransaction().commit();
                } catch (final Exception e) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    throw new RuntimeException(e);
                }
                container.refresh();
            }
        });
/*
        final Button permanentRemoveButton = getSite().getButton("permanent-remove");
        buttonLayout.addComponent(permanentRemoveButton);
        permanentRemoveButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Entry entity = container.getEntity(grid.getSelectedItemId());
                entity.setDeleted(null);
                entityManager.getTransaction().begin();
                try {
                    entityManager.remove(entityManager.merge(entity));
                    entityManager.getTransaction().commit();
                } catch (final Exception e) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    throw new RuntimeException(e);
                }
                container.refresh();
            }
        });
*/

        final Button synchronizeButton = getSite().getButton("synchronize");
        buttonLayout.addComponent(synchronizeButton);
        synchronizeButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                TranslationSynchronizer.startSynchronize();
                Notification.show(getSite().localize("message-synchronization-started"));
            }
        });

        final Company company = getSite().getSiteContext().getObject(Company.class);
        container.removeDefaultFilters();
        container.addDefaultFilter(
                new Compare.Equal("owner.companyId", company.getCompanyId()));
        grid.refresh();

        repositoryField = new ComboBox(getSite().localize("field-repository"));
        repositoryField.setNullSelectionAllowed(false);
        repositoryField.setTextInputAllowed(true);
        repositoryField.setNewItemsAllowed(false);
        repositoryField.setInvalidAllowed(false);
        repositoryField.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                container.removeDefaultFilters();
                container.addDefaultFilter(
                        new Compare.Equal("owner.companyId", company.getCompanyId()));
                final Repository repository = (Repository) repositoryField.getValue();
                if (repository != null) {
                    container.addDefaultFilter(
                            new Compare.Equal("repository.repositoryId", repository.getRepositoryId()));
                }
            }
        });
        final List<Repository> repositories =
                ReviewDao.getRepositories(entityManager, (Company) getSite().getSiteContext().getObject(Company.class));

        for (final Repository repository : repositories) {
            repositoryField.addItem(repository);
            repositoryField.setItemCaption(repository, repository.getPath());
            if (repositoryField.getItemIds().size() == 1) {
                repositoryField.setValue(repository);
            }
        }
        final VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setMargin(new MarginInfo(false, false, true, false));
        verticalLayout.addComponent(repositoryField);
        gridLayout.addComponent(verticalLayout, 0, 0);
    }

    @Override
    public void enter() {
        container.refresh();
    }

}
