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
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.groom.FileDiffBeanQuery;
import org.groom.GroomFields;
import org.groom.dao.ReviewDao;
import org.groom.flows.CommentDialog;
import org.groom.flows.ReviewFileDiffFlowlet;
import org.groom.model.Comment;
import org.groom.model.FileDiff;
import org.groom.model.Review;
import org.groom.model.ReviewStatus;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.NestingBeanItem;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.*;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.site.SecurityProviderSessionImpl;
import org.vaadin.addons.sitekit.util.ContainerUtil;

import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private LazyQueryContainer container;
    private BeanQueryFactory<FileDiffBeanQuery> beanQueryFactory;
    private ReviewStatus reviewStatus;
    private LazyEntityContainer<ReviewStatus> reviewStatusContainer;
    private LazyEntityContainer<Comment> commentContainer;
    private Table fileDiffTable;
    private Button completeButton;
    private Button reopenButton;

    private boolean disableViewChange = false;

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

        final GridLayout gridLayout = new GridLayout(2, 4);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        //gridLayout.setRowExpandRatio(0, 0.5f);
        gridLayout.setRowExpandRatio(1, 1f);
        gridLayout.setColumnExpandRatio(1, 1f);
        setViewContent(gridLayout);

        reviewEditor = new ValidatingEditor(GroomFields.getFieldDescriptors(Review.class));
        reviewEditor.setCaption("Review");
        reviewEditor.addListener((ValidatingEditorStateListener) this);
        reviewEditor.setWidth(400, Unit.PIXELS);
        reviewEditor.setReadOnly(true);

        gridLayout.addComponent(reviewEditor, 0, 0);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        gridLayout.addComponent(buttonLayout, 0, 3);


        beanQueryFactory = new BeanQueryFactory<FileDiffBeanQuery>(FileDiffBeanQuery.class);

        container = new LazyQueryContainer(beanQueryFactory,"path",
                20, false);

        container.addContainerProperty("status", Character.class, null, true, false);
        container.addContainerProperty("path", String.class, null, true, false);
        container.addContainerProperty("reviewed", String.class, null, true, false);

        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fileDiffTable = new Table() {
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

        fileDiffTable.setSizeFull();
        fileDiffTable.setContainerDataSource(container);
        fileDiffTable.setVisibleColumns(new Object[]{
                "status",
                "path",
                "reviewed"
        });

        fileDiffTable.setColumnWidth("status", 20);
        //table.setColumnWidth("path", 500);

        fileDiffTable.setColumnHeaders(new String[]{
                getSite().localize("field-status"),
                getSite().localize("field-path"),
                getSite().localize("field-reviewed")
        });

        fileDiffTable.setColumnCollapsingAllowed(false);
        fileDiffTable.setSelectable(true);
        fileDiffTable.setMultiSelect(false);
        fileDiffTable.setImmediate(true);

        fileDiffTable.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
                final String selectedPath = (String) fileDiffTable.getValue();

                if (selectedPath != null) {
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                            fileDiffTable.getItem(selectedPath)).getBean();
                    fileDiff.setReviewed(true);
                    ReviewDao.saveReviewStatus(entityManager, reviewStatus);
                    final ReviewFileDiffFlowlet view;
                    if (disableViewChange) {
                        view = getViewSheet().getFlowlet(ReviewFileDiffFlowlet.class);
                    } else {
                        view = getViewSheet().forward(ReviewFileDiffFlowlet.class);
                    }

                    view.setFileDiff(review, fileDiff, 0);
                }
            }
        });

        gridLayout.addComponent(fileDiffTable, 1, 0, 1, 1);

        reviewStatusContainer = new LazyEntityContainer<ReviewStatus>(entityManager, true, false, false, ReviewStatus.class, 1000,
        new String[] {"reviewer.emailAddress"},
        new boolean[] {true}, "reviewStatusId");
        final List<FieldDescriptor> fieldDescriptors = GroomFields.getFieldDescriptors(ReviewStatus.class);
        ContainerUtil.addContainerProperties(reviewStatusContainer, fieldDescriptors);
        final Table reviewerStatusesTable = new FormattingTable();
        Grid grid = new Grid(reviewerStatusesTable, reviewStatusContainer);
        grid.setFields(fieldDescriptors);
        reviewerStatusesTable.setColumnCollapsed("reviewStatusId", true);
        reviewerStatusesTable.setColumnCollapsed("created", true);
        reviewerStatusesTable.setColumnCollapsed("modified", true);
        reviewerStatusesTable.setSelectable(false);
        gridLayout.addComponent(grid, 0, 1);

        commentContainer = new LazyEntityContainer<Comment>(entityManager, true, false, false, Comment.class, 1000,
                new String[] {"path", "line"},
                new boolean[] {true, true}, "commentId");
        final List<FieldDescriptor> commentFieldDescriptors = GroomFields.getFieldDescriptors(Comment.class);
        ContainerUtil.addContainerProperties(commentContainer, commentFieldDescriptors);
        final Table commentTable = new FormattingTable();
        Grid commentGrid = new Grid(commentTable, commentContainer);
        commentGrid.setHeight(200, Unit.PIXELS);
        commentGrid.setFields(commentFieldDescriptors);
        commentTable.setImmediate(true);
        commentTable.setColumnCollapsed("commentId", false);
        commentTable.setColumnCollapsed("created", true);
        commentTable.setColumnCollapsed("committer", true);
        gridLayout.addComponent(commentGrid, 0, 2, 1, 2);

        commentTable.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
                final String commentId = (String) commentTable.getValue();

                if (commentId != null) {
                    final Comment comment = ((NestingBeanItem<Comment>) commentTable.getItem(commentId)).getBean();
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>) fileDiffTable.getItem(comment.getPath())).getBean();
                    fileDiff.setReviewed(true);
                    ReviewDao.saveReviewStatus(entityManager, reviewStatus);
                    final char status = fileDiff.getStatus();
                    if (status == 'A' || status == 'M') {
                        final ReviewFileDiffFlowlet view = getViewSheet().forward(ReviewFileDiffFlowlet.class);
                        view.setFileDiff(review, fileDiff, comment.getDiffLine());
                    }

                }
            }
        });

        completeButton = new Button(getSite().localize("button-complete"));
        completeButton.setImmediate(true);
        buttonLayout.addComponent(completeButton);
        completeButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final CommentDialog commentDialog = new CommentDialog(new CommentDialog.DialogListener() {
                    @Override
                    public void onOk(final String message) {
                        reviewStatus.setComment(message);
                        reviewStatus.setCompleted(true);
                        ReviewDao.saveReviewStatus(entityManager, reviewStatus);
                        enter();
                    }

                    @Override
                    public void onCancel() {
                    }
                });
                commentDialog.setCaption("Please enter final comment.");
                UI.getCurrent().addWindow(commentDialog);
                commentDialog.getTextArea().focus();
            }
        });

        reopenButton = new Button(getSite().localize("button-reopen"));
        reopenButton.setImmediate(true);
        buttonLayout.addComponent(reopenButton);
        reopenButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                reviewStatus.setCompleted(false);
                ReviewDao.saveReviewStatus(entityManager, reviewStatus);
                enter();
            }
        });
    }

    public void next() {
        disableViewChange = true;
        final String selectedPath = (String) fileDiffTable.getValue();
        final String nextPath = (String) fileDiffTable.nextItemId(selectedPath);
        if (nextPath != null) {
            fileDiffTable.select(nextPath);
        }
        disableViewChange = false;
    }

    public void previous() {
        disableViewChange = true;
        final String selectedPath = (String) fileDiffTable.getValue();
        final String prevPath = (String) fileDiffTable.prevItemId(selectedPath);
        if (prevPath != null) {
            fileDiffTable.select(prevPath);
        }
        disableViewChange = false;
    }

    /**
     * Edit an existing review.
     * @param review review to be edited.
     * @param newEntity true if review to be edited is new.
     */
    public void edit(final Review review, final boolean newEntity) {
        this.review = review;

        reviewStatusContainer.removeDefaultFilters();
        reviewStatusContainer.addDefaultFilter(new Compare.Equal("review", review));
        reviewStatusContainer.refresh();

        commentContainer.removeDefaultFilters();
        commentContainer.addDefaultFilter(new Compare.Equal("review", review));
        commentContainer.refresh();

        final User user = ((SecurityProviderSessionImpl)
                getSite().getSecurityProvider()).getUserFromSession();

        reviewStatus = ReviewDao.getReviewStatus(entityManager, user, this.review);
        if (reviewStatus == null) {
            int coverageSize = review.getDiffCount() / 8;
            if (review.getDiffCount() % 8 > 0) {
                coverageSize++;
            }
            byte[] coverage = new byte[coverageSize];
            for (int i = 0; i < coverageSize; i++) {
                coverage[i] = 0;
            }
            final Date created = new Date();
            reviewStatus = new ReviewStatus(review, user, "", false, coverage, created, created);
            ReviewDao.saveReviewStatus(entityManager, reviewStatus);
            reviewStatusContainer.refresh();
        }

        final Map<String, Object> queryConfiguration = new HashMap<String, Object>();
        queryConfiguration.put("status", reviewStatus);
        beanQueryFactory.setQueryConfiguration(queryConfiguration);

        container.addContainerFilter(new Compare.Equal("range", this.review.getSinceHash() + ".." + this.review.getUntilHash()));
        container.refresh();
        this.review.setDiffCount(container.size());
        reviewEditor.setItem(new BeanItem<Review>(this.review), newEntity);

        enter();
    }

    @Override
    public void editorStateChanged(final ValidatingEditor source) {
    }

    @Override
    public void enter() {
        reviewStatusContainer.refresh();
        commentContainer.refresh();
        entityManager.refresh(reviewStatus);
        completeButton.setEnabled(!reviewStatus.isCompleted() && reviewStatus.getProgress() == 100);
        reopenButton.setEnabled(reviewStatus.isCompleted());
    }

}
