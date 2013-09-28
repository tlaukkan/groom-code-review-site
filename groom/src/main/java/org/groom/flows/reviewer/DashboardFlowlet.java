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
package org.groom.flows.reviewer;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Or;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.groom.GroomFields;
import org.groom.model.Review;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.FieldDescriptor;
import org.vaadin.addons.sitekit.grid.FilterDescriptor;
import org.vaadin.addons.sitekit.grid.FormattingTable;
import org.vaadin.addons.sitekit.grid.Grid;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Group;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.site.SecurityProviderSessionImpl;
import org.vaadin.addons.sitekit.util.ContainerUtil;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Review list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class DashboardFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The container. */
    private LazyEntityContainer<Review> container;
    /** The grid. */
    private Grid grid;

    @Override
    public String getFlowletKey() {
        return "dashboard";
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
        final List<FieldDescriptor> fieldDescriptors = GroomFields.getFieldDescriptors(Review.class);

        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();

        filterDefinitions.add(new FilterDescriptor("title", "title", "Title", new TextField(),
                200, "like", String.class, ""));

        final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        container = new LazyEntityContainer<Review>(entityManager, true, false, false, Review.class, 0,
                new String[] {"created"},
                new boolean[] {true}, "reviewId");

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
        //grid.setWidth(550, Unit.PIXELS);
        grid.setFields(fieldDescriptors);
        grid.setFilters(filterDefinitions);

        table.setNullSelectionAllowed(false);
        table.setColumnCollapsed("modified", true);
        table.setColumnCollapsed("reviewId", true);
        table.setColumnCollapsed("path", true);
        //table.setColumnCollapsed("sinceHash", true);
        //table.setColumnCollapsed("untilHash", true);
        table.setColumnCollapsed("completed", true);
        //table.setColumnCollapsed("reviewGroup", true);

        gridLayout.addComponent(grid, 0, 1);

        /*table.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
                if (grid.getSelectedItemId() != null) {
                    final Review entity = container.getEntity(grid.getSelectedItemId());
                    final ReviewFlowlet reviewView = getViewSheet().getFlowlet(ReviewFlowlet.class);
                    reviewView.edit(entity, false);
                    getViewSheet().forward(ReviewFlowlet.class);
                    table.setValue(null);
                }
            }
        });*/

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                final Review entity = container.getEntity(event.getItemId());
                final ReviewFlowlet reviewView = getViewSheet().getFlowlet(ReviewFlowlet.class);
                reviewView.edit(entity, false);
                getViewSheet().forward(ReviewFlowlet.class);
                //table.setValue(null);
            }
        });

        final Company company = getSite().getSiteContext().getObject(Company.class);
        final User user = ((SecurityProviderSessionImpl)
                getSite().getSecurityProvider()).getUserFromSession();
        container.removeDefaultFilters();
        container.addDefaultFilter(
                new Compare.Equal("owner.companyId", company.getCompanyId()));
        container.addDefaultFilter(
                new Compare.Equal("completed", false));
        final List<Group> groups = UserDao.getUserGroups(entityManager, company, user);

        Container.Filter groupsFilter = null;
        for (final Group group : groups) {
            final Container.Filter groupFilter =  new Compare.Equal("reviewGroup", group);
            if (groupsFilter == null) {
                groupsFilter = groupFilter;
            } else {
                groupsFilter = new Or(groupsFilter, groupFilter);
            }
        }
        if (groupsFilter != null) {
            container.addDefaultFilter(groupsFilter);
        }

        grid.refresh();
    }

    @Override
    public void enter() {
        container.getQueryView().getQueryDefinition().setBatchSize(100);
        container.refresh();
    }

}
