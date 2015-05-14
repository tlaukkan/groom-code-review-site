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

import com.vaadin.data.Property;
import com.vaadin.ui.*;
import org.bubblecloud.ilves.component.flow.AbstractFlowlet;
import org.bubblecloud.ilves.component.grid.FieldDescriptor;
import org.bubblecloud.ilves.component.grid.FilterDescriptor;
import org.bubblecloud.ilves.component.grid.FormattingTable;
import org.bubblecloud.ilves.component.grid.Grid;
import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.util.ContainerUtil;
import org.groom.review.ui.ReviewFields;
import org.groom.review.model.Review;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Review list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class ReviewsFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The container. */
    private LazyEntityContainer<Review> container;
    /** The grid. */
    private Grid grid;
    /** The review button. */
    private Button reviewButton;

    @Override
    public String getFlowletKey() {
        return "reviews-management";
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
        final List<FieldDescriptor> fieldDescriptors = ReviewFields.getFieldDescriptors(Review.class);

        final List<FilterDescriptor> filterDefinitions = new ArrayList<FilterDescriptor>();

        filterDefinitions.add(new FilterDescriptor("title", "title", "Title", new TextField(),
                200, "like", String.class, ""));


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

        final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        container = new LazyEntityContainer<Review>(entityManager, true, false, false, Review.class, 1000,
                new String[] {"created"},
                new boolean[] {true}, "reviewId");

        ContainerUtil.addContainerProperties(container, fieldDescriptors);

        final Table table = new FormattingTable();
        grid = new Grid(table, container);
        grid.setFields(fieldDescriptors);
        grid.setFilters(filterDefinitions);
        grid.setWidth(100, Unit.PERCENTAGE);
        grid.setHeight(UI.getCurrent().getPage().getBrowserWindowHeight() - 235, Unit.PIXELS);

        table.setColumnCollapsed("reviewId", true);

        gridLayout.addComponent(grid, 0, 1);

        /*final Button addButton = getSite().getButton("add");
        buttonLayout.addComponent(addButton);
        addButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Review review = new Review();
                review.setCreated(new Date());
                review.setModified(review.getCreated());
                review.setOwner((Company) getSite().getSiteContext().getObject(Company.class));
                final ReviewFlowlet reviewView = getViewSheet().forward(ReviewFlowlet.class);
                reviewView.edit(review, true);
            }
        });*/

        final Button editButton = getSite().getButton("edit");
        buttonLayout.addComponent(editButton);
        editButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Review entity = container.getEntity(grid.getSelectedItemId());
                final ReviewFlowlet reviewView = getFlow().forward(ReviewFlowlet.class);
                reviewView.edit(entity, false);
            }
        });

        table.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                editButton.setEnabled(table.getValue() != null);
            }
        });


        reviewButton = new Button("Report");
        reviewButton.setImmediate(true);
        buttonLayout.addComponent(reviewButton);
        reviewButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Review entity = container.getEntity(grid.getSelectedItemId());
                final Company company = getSite().getSiteContext().getObject(Company.class);
                getUI().getPage().open(company.getUrl() + "/../report?reviewId=" + entity.getReviewId(), "_blank");
            }
        });

        /*final Button removeButton = getSite().getButton("remove");
        buttonLayout.addComponent(removeButton);
        removeButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                container.removeItem(grid.getSelectedItemId());
                container.commit();
            }
        });*/

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
