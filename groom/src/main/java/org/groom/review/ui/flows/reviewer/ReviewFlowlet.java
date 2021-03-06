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
package org.groom.review.ui.flows.reviewer;

import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import org.bubblecloud.ilves.component.flow.AbstractFlowlet;
import org.bubblecloud.ilves.component.grid.FieldDescriptor;
import org.bubblecloud.ilves.component.grid.FormattingTable;
import org.bubblecloud.ilves.component.grid.ValidatingEditor;
import org.bubblecloud.ilves.component.grid.ValidatingEditorStateListener;
import org.bubblecloud.ilves.model.User;
import org.bubblecloud.ilves.site.SecurityProviderSessionImpl;
import org.bubblecloud.ilves.util.ContainerUtil;
import org.groom.shell.FileDiffBeanQuery;
import org.groom.review.ui.ReviewFields;
import org.groom.review.dao.ReviewDao;
import org.groom.review.ui.flows.ReviewFileDiffFlowlet;
import org.groom.review.model.Comment;
import org.groom.review.model.FileDiff;
import org.groom.review.model.Review;
import org.groom.review.model.ReviewStatus;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.NestingBeanItem;

import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private Map<String,Object> queryConfiguration;

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

        reviewEditor = new ValidatingEditor(ReviewFields.getFieldDescriptors(Review.class));
        reviewEditor.setCaption("Review");
        reviewEditor.addListener((ValidatingEditorStateListener) this);
        reviewEditor.setWidth(450, Unit.PIXELS);
        reviewEditor.setReadOnly(true);

        gridLayout.addComponent(reviewEditor, 0, 0);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        gridLayout.addComponent(buttonLayout, 0, 3);


        beanQueryFactory = new BeanQueryFactory<FileDiffBeanQuery>(FileDiffBeanQuery.class);
        queryConfiguration = new HashMap<String, Object>();
        beanQueryFactory.setQueryConfiguration(queryConfiguration);

        container = new LazyQueryContainer(beanQueryFactory,"path",
                20, false);

        container.addContainerProperty("status", Character.class, null, true, false);
        container.addContainerProperty("reviewed", String.class, null, true, false);
        container.addContainerProperty("path", String.class, null, true, false);

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
        fileDiffTable.setNullSelectionAllowed(false);
        fileDiffTable.setSizeFull();
        fileDiffTable.setContainerDataSource(container);
        fileDiffTable.setVisibleColumns(new Object[]{
                "reviewed",
                "path"
        });

        //table.setColumnWidth("path", 500);
        fileDiffTable.setColumnWidth("status", 40);
        fileDiffTable.setColumnWidth("reviewed", 40);

        fileDiffTable.setColumnHeaders(new String[]{
                "",
                getSite().localize("field-path")
        });

        fileDiffTable.setColumnCollapsingAllowed(false);
        fileDiffTable.setSelectable(true);
        fileDiffTable.setMultiSelect(false);
        fileDiffTable.setImmediate(true);

        fileDiffTable.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                final String selectedPath = (String)  event.getItemId();

                if (selectedPath != null) {
                    reviewFileDiff(selectedPath);
                }
            }
        });

        fileDiffTable.setCellStyleGenerator(new Table.CellStyleGenerator() {
            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (propertyId != null && propertyId.equals("status")) {
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                            source.getItem(itemId)).getBean();
                    switch(fileDiff.getStatus()) {
                        case 'A':
                            return "added";
                        case 'D':
                            return "deleted";
                        case 'M':
                            return "modified";
                        default:
                            return "";
                    }
                } else if (propertyId != null && propertyId.equals("path")) {
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                            source.getItem(itemId)).getBean();
                    switch(fileDiff.getStatus()) {
                        case 'A':
                            return "path-added";
                        case 'D':
                            return "path-deleted";
                        case 'M':
                            return "path-modified";
                        default:
                            return "";
                    }
                } else if (propertyId != null && propertyId.equals("reviewed")) {
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                            source.getItem(itemId)).getBean();
                    if (fileDiff.isReviewed()) {
                        return "ok";
                    } else {
                        return "";
                    }
                } else {
                    return "";
                }
            }
        });

        fileDiffTable.setConverter("status", new Converter<String, Character>() {
            @Override
            public Character convertToModel(String value, Class<? extends Character> targetType,
                                            Locale locale) throws ConversionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String convertToPresentation(Character value, Class<? extends String> targetType,
                                                Locale locale) throws ConversionException {
                return "";
            }

            @Override
            public Class<Character> getModelType() {
                return Character.class;
            }

            @Override
            public Class<String> getPresentationType() {
                return String.class;
            }
        });

        fileDiffTable.setConverter("reviewed", new Converter<String, Boolean>() {
            @Override
            public Boolean convertToModel(String value, Class<? extends Boolean> targetType,
                                            Locale locale) throws ConversionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String convertToPresentation(Boolean value, Class<? extends String> targetType,
                                                Locale locale) throws ConversionException {
                return "";
            }

            @Override
            public Class<Boolean> getModelType() {
                return Boolean.class;
            }

            @Override
            public Class<String> getPresentationType() {
                return String.class;
            }
        });

        Panel fileDiffPanel = new Panel("Diffs");
        fileDiffPanel.setStyleName(ValoTheme.PANEL_BORDERLESS);
        fileDiffPanel.setSizeFull();
        fileDiffPanel.setContent(fileDiffTable);

        gridLayout.addComponent(fileDiffPanel, 1, 0, 1, 1);

        reviewStatusContainer = new LazyEntityContainer<ReviewStatus>(entityManager, true, false, false, ReviewStatus.class, 0,
        new String[] {"reviewer.emailAddress"},
        new boolean[] {true}, "reviewStatusId");
        final List<FieldDescriptor> fieldDescriptors = ReviewFields.getFieldDescriptors(ReviewStatus.class);
        ContainerUtil.addContainerProperties(reviewStatusContainer, fieldDescriptors);
        final Table reviewerStatusesTable = new FormattingTable();
        org.bubblecloud.ilves.component.grid.Grid grid = new org.bubblecloud.ilves.component.grid.Grid(reviewerStatusesTable, reviewStatusContainer);
        grid.setFields(fieldDescriptors);
        reviewerStatusesTable.setColumnCollapsed("reviewStatusId", true);
        reviewerStatusesTable.setColumnCollapsed("created", true);
        reviewerStatusesTable.setColumnCollapsed("modified", true);
        reviewerStatusesTable.setSelectable(false);

        reviewerStatusesTable.setCellStyleGenerator(new Table.CellStyleGenerator() {
            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (propertyId != null && propertyId.equals("completed")) {
                    final ReviewStatus reviewStatus = ((NestingBeanItem<ReviewStatus>)
                            source.getItem(itemId)).getBean();
                    if (reviewStatus.isCompleted()) {
                        return "ok";
                    } else {
                        return "";
                    }
                } else {
                    return "";
                }
            }
        });

        reviewerStatusesTable.setConverter("completed", new Converter<String, Boolean>() {
            @Override
            public Boolean convertToModel(String value, Class<? extends Boolean> targetType,
                                          Locale locale) throws ConversionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String convertToPresentation(Boolean value, Class<? extends String> targetType,
                                                Locale locale) throws ConversionException {
                return "";
            }

            @Override
            public Class<Boolean> getModelType() {
                return Boolean.class;
            }

            @Override
            public Class<String> getPresentationType() {
                return String.class;
            }
        });

        Panel reviewerPanel = new Panel("Reviewers");
        reviewerPanel.setStyleName(ValoTheme.PANEL_BORDERLESS);
        reviewerPanel.setHeight(300, Unit.PIXELS);
        reviewerPanel.setContent(grid);

        gridLayout.addComponent(reviewerPanel, 0, 2);

        commentContainer = new LazyEntityContainer<Comment>(entityManager, true, false, false, Comment.class, 0,
                new String[] {"path", "line"},
                new boolean[] {true, true}, "reviewCommentId");
        final List<FieldDescriptor> commentFieldDescriptors = ReviewFields.getFieldDescriptors(Comment.class);
        ContainerUtil.addContainerProperties(commentContainer, commentFieldDescriptors);
        final Table commentTable = new FormattingTable();
        org.bubblecloud.ilves.component.grid.Grid commentGrid = new org.bubblecloud.ilves.component.grid.Grid(commentTable, commentContainer);
        commentTable.setNullSelectionAllowed(false);
        commentGrid.setSizeFull();
        commentGrid.setFields(commentFieldDescriptors);
        commentTable.setImmediate(true);
        commentTable.setColumnCollapsed("reviewCommentId", true);
        commentTable.setColumnCollapsed("created", true);
        commentTable.setColumnCollapsed("modified", true);
        commentTable.setColumnCollapsed("committer", true);
        commentTable.setColumnCollapsed("path", true);
        commentTable.setColumnCollapsed("line", true);
        commentTable.setColumnCollapsed("hash", true);

        commentTable.setCellStyleGenerator(new Table.CellStyleGenerator() {
            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (propertyId != null && propertyId.equals("severity")) {
                    final Comment comment = ((NestingBeanItem<Comment>)
                            source.getItem(itemId)).getBean();
                    switch(comment.getSeverity()) {
                        case 1:
                            return "kudo";
                        case -1:
                            return "warning";
                        case -2:
                            return "red-flag";
                        default:
                            return "";
                    }
                } else {
                    return "";
                }
            }
        });

        commentTable.setConverter("severity", new Converter<String, Integer>() {
            @Override
            public Integer convertToModel(String value, Class<? extends Integer> targetType,
                                          Locale locale) throws ConversionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String convertToPresentation(Integer value, Class<? extends String> targetType,
                                                Locale locale) throws ConversionException {
                return "";
            }

            @Override
            public Class<Integer> getModelType() {
                return Integer.class;
            }

            @Override
            public Class<String> getPresentationType() {
                return String.class;
            }
        });

        Panel commentPanel = new Panel("Review Comments");
        commentPanel.setStyleName(ValoTheme.PANEL_BORDERLESS);
        commentPanel.setHeight(300, Unit.PIXELS);
        commentPanel.setContent(commentGrid);

        gridLayout.addComponent(commentPanel, 1, 2, 1, 2);

        commentTable.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                final String commentId = (String) event.getItemId();

                if (commentId != null) {
                    final Comment comment = ((NestingBeanItem<Comment>) commentTable.getItem(commentId)).getBean();
                    final String path = comment.getPath();
                    final FileDiff fileDiff = ((NestingBeanItem<FileDiff>) fileDiffTable.getItem(path)).getBean();
                    fileDiff.setReviewed(true);
                    ReviewDao.saveReviewStatus(entityManager, reviewStatus);
                    final char status = fileDiff.getStatus();
                    if (status == 'A' || status == 'M') {
                        final ReviewFileDiffFlowlet view = getFlow().forward(ReviewFileDiffFlowlet.class);
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
                final ReviewCommentDialog commentDialog = new ReviewCommentDialog(
                        new ReviewCommentDialog.DialogListener() {
                    @Override
                    public void onOk(final String message) {
                        reviewStatus.setReviewComment(message);
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

    private void reviewFileDiff(String selectedPath) {
        final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                fileDiffTable.getItem(selectedPath)).getBean();
        fileDiff.setReviewed(true);
        ReviewDao.saveReviewStatus(entityManager, reviewStatus);
        final ReviewFileDiffFlowlet view;
        final char status = fileDiff.getStatus();
        if (status == 'A' || status == 'M') {
            if (disableViewChange) {
                view = getFlow().getFlowlet(ReviewFileDiffFlowlet.class);
            } else {
                view = getFlow().forward(ReviewFileDiffFlowlet.class);

            }
            view.setFileDiff(review, fileDiff, 0);
        } else {
            fileDiffTable.refreshRowCache();
        }
    }

    public void next(final String path) {
        final String selectedPath = path != null ? path : (String) fileDiffTable.getValue();
        final String nextPath = (String) fileDiffTable.nextItemId(selectedPath);
        if (nextPath != null) {
            final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                    fileDiffTable.getItem(nextPath)).getBean();
            final char status = fileDiff.getStatus();
            if (status == 'A' || status == 'M') {
                disableViewChange = true;
                fileDiffTable.select(nextPath);
                reviewFileDiff(nextPath);
                disableViewChange = false;
            } else {
                next(nextPath);
            }
        }
    }

    public void previous(final String path) {
        final String selectedPath = path != null ? path : (String) fileDiffTable.getValue();
        final String prevPath = (String) fileDiffTable.prevItemId(selectedPath);
        if (prevPath != null) {
            final FileDiff fileDiff = ((NestingBeanItem<FileDiff>)
                    fileDiffTable.getItem(prevPath)).getBean();
            final char status = fileDiff.getStatus();
            if (status == 'A' || status == 'M') {
                disableViewChange = true;
                fileDiffTable.select(prevPath);
                reviewFileDiff(prevPath);
                disableViewChange = false;
            } else {
                previous(prevPath);
            }
        }
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

        queryConfiguration.put("repository", review.getRepository());
        queryConfiguration.put("status", reviewStatus);

        container.addContainerFilter(new Compare.Equal("range", this.review.getSinceHash() + " " + this.review.getUntilHash()));
        container.refresh();
        this.review.setDiffCount(container.size());
        reviewEditor.setItem(new BeanItem<Review>(this.review), newEntity);
    }

    @Override
    public void editorStateChanged(final ValidatingEditor source) {
    }

    @Override
    public void enter() {
        reviewStatusContainer.getQueryView().getQueryDefinition().setBatchSize(50);
        reviewStatusContainer.refresh();
        commentContainer.getQueryView().getQueryDefinition().setBatchSize(50);
        commentContainer.refresh();
        entityManager.refresh(reviewStatus);
        completeButton.setEnabled(!reviewStatus.isCompleted() && reviewStatus.getProgress() == 100);
        reopenButton.setEnabled(reviewStatus.isCompleted());
    }

}
