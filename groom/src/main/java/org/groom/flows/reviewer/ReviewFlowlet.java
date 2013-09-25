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

import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import org.groom.FileDiffBeanQuery;
import org.groom.GroomFields;
import org.groom.dao.ReviewDao;
import org.groom.flows.ReviewFileDiffFlowlet;
import org.groom.model.FileDiff;
import org.groom.model.Review;
import org.groom.model.ReviewStatus;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.NestingBeanItem;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.ValidatingEditor;
import org.vaadin.addons.sitekit.grid.ValidatingEditorStateListener;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.site.SecurityProviderSessionImpl;

import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Review edit flow.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class ReviewFlowlet extends AbstractFlowlet implements ValidatingEditorStateListener {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The review manager. */
    private EntityManager entityManager;
    /** The review flow. */
    private Review review;

    /** The review form. */
    private ValidatingEditor reviewEditor;
    /** The save button. */
    private Button saveButton;
    /** The discard button. */
    private Button discardButton;
    private LazyQueryContainer container;

    @Override
    public String getFlowletKey() {
        return "review";
    }

    @Override
    public boolean isDirty() {
        return reviewEditor.isModified();
    }

    @Override
    public boolean isValid() {
        return reviewEditor.isValid();
    }

    @Override
    public void initialize() {
        entityManager = getSite().getSiteContext().getObject(EntityManager.class);

        final GridLayout gridLayout = new GridLayout(2, 2);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(0, 1f);
        gridLayout.setColumnExpandRatio(1, 1f);
        setViewContent(gridLayout);

        reviewEditor = new ValidatingEditor(GroomFields.getFieldDescriptors(Review.class));
        reviewEditor.setCaption("Review");
        reviewEditor.addListener((ValidatingEditorStateListener) this);
        reviewEditor.setWidth(400, Unit.PIXELS);
        gridLayout.addComponent(reviewEditor, 0, 0);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        gridLayout.addComponent(buttonLayout, 0, 1);

        final BeanQueryFactory<FileDiffBeanQuery> beanQueryFactory =
                new BeanQueryFactory<FileDiffBeanQuery>(FileDiffBeanQuery.class);

        container = new LazyQueryContainer(beanQueryFactory,"path",
                20, false);

        container.addContainerProperty("status", Character.class, null, true, false);
        container.addContainerProperty("path", String.class, null, true, false);

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
                "status",
                "path"
        });

        table.setColumnWidth("status", 20);
        //table.setColumnWidth("path", 500);

        table.setColumnHeaders(new String[]{
                getSite().localize("field-status"),
                getSite().localize("field-path")
        });

        table.setColumnCollapsingAllowed(false);
        table.setSelectable(true);
        table.setMultiSelect(false);
        table.setImmediate(true);

        table.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
                final String selectedPath = (String) table.getValue();

                if (selectedPath != null) {
                    final char status = ((NestingBeanItem<FileDiff>) table.getItem(selectedPath)).getBean().getStatus();
                    if (status == 'A' || status == 'M') {
                        final ReviewFileDiffFlowlet view = getViewSheet().forward(ReviewFileDiffFlowlet.class);
                        view.setFileDiff(selectedPath, review.getSinceHash(), review.getUntilHash(), status == 'A');
                    }
                }
            }
        });

        gridLayout.addComponent(table, 1, 0);

        saveButton = new Button("Save");
        saveButton.setImmediate(true);
        buttonLayout.addComponent(saveButton);
        saveButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                reviewEditor.commit();
                entityManager.getTransaction().begin();
                try {
                    review = entityManager.merge(review);
                    //review.setAuthor(getSite().getSecurityProvider().getUser());
                    review.setModified(new Date());
                    entityManager.persist(review);
                    entityManager.getTransaction().commit();
                    entityManager.detach(review);
                    reviewEditor.discard();
                } catch (final Throwable t) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    throw new RuntimeException("Failed to save review: " + review, t);
                }
            }
        });

        discardButton = new Button("Discard");
        discardButton.setImmediate(true);
        buttonLayout.addComponent(discardButton);
        discardButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                reviewEditor.discard();
            }
        });

    }

    /**
     * Edit an existing review.
     * @param review review to be edited.
     * @param newEntity true if review to be edited is new.
     */
    public void edit(final Review review, final boolean newEntity) {
        this.review = review;
        container.addContainerFilter(new Compare.Equal("range", this.review.getSinceHash() + ".." + this.review.getUntilHash()));
        container.refresh();
        this.review.setDiffCount(container.size());
        reviewEditor.setReadOnly(true);
        reviewEditor.setItem(new BeanItem<Review>(this.review), newEntity);

        final User user = ((SecurityProviderSessionImpl)
                getSite().getSecurityProvider()).getUserFromSession();

        ReviewStatus reviewStatus = ReviewDao.getReviewStatus(entityManager, user, this.review);
        if (reviewStatus == null) {
            int coverageSize = review.getDiffCount() / 8;
            if (review.getDiffCount() % 8 > 0) {
                coverageSize++;
            }
            final Date created = new Date();
            reviewStatus = new ReviewStatus(review, user, "", false, new byte[coverageSize], created, created);
            ReviewDao.saveReviewStatus(entityManager, reviewStatus);
        }

        //reviewEditor.setReadOnly(true);
    }

    @Override
    public void editorStateChanged(final ValidatingEditor source) {
        if (isDirty()) {
            if (isValid()) {
                saveButton.setEnabled(true);
            } else {
                saveButton.setEnabled(false);
            }
            discardButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
            discardButton.setEnabled(false);
        }
    }

    @Override
    public void enter() {
    }

}
