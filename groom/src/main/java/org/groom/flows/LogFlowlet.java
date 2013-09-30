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
package org.groom.flows;

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.groom.CommitBeanQuery;
import org.groom.Shell;
import org.groom.dao.ReviewDao;
import org.groom.flows.admin.ReviewFlowlet;
import org.groom.flows.admin.ReviewRangeDialog;
import org.groom.model.Commit;
import org.groom.model.Repository;
import org.groom.model.Review;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.NestingBeanItem;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.SecurityProviderSessionImpl;

import javax.persistence.EntityManager;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Repository list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class LogFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    private TextField sinceField;
    private TextField untilField;
    private EntityManager entityManager;
    private ComboBox repositoryField;
    private Repository repository;
    private Map<String,Object> queryConfiguration;

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
        entityManager = getSite().getSiteContext().getObject(EntityManager.class);

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
                    if (!(Character.isLetter(c) || Character.isDigit(c) || "-./".indexOf(c) != -1)) {
                        throw new InvalidValueException("" + c);
                    }
                }
            }
        };

        repositoryField = new ComboBox(getSite().localize("field-repository"));
        repositoryField.setNullSelectionAllowed(false);
        repositoryField.setTextInputAllowed(true);
        repositoryField.setNewItemsAllowed(false);
        repositoryField.setInvalidAllowed(false);
        final List<Repository> repositories =
                ReviewDao.getRepositories(entityManager, (Company) getSite().getSiteContext().getObject(Company.class));

        for (final Repository repository : repositories) {
            repositoryField.addItem(repository);
            repositoryField.setItemCaption(repository, repository.getPath());
            if (repositoryField.getItemIds().size() == 1) {
                repositoryField.setValue(repository);
            }
        }
        filterLayout.addComponent(repositoryField);

        sinceField = new TextField(getSite().localize("field-since"));
        sinceField.setValue("master");
        sinceField.setValidationVisible(true);
        sinceField.addValidator(validator);
        filterLayout.addComponent(sinceField);

        untilField = new TextField(getSite().localize("field-until"));
        untilField.setValidationVisible(true);
        untilField.addValidator(validator);
        filterLayout.addComponent(untilField);

        final BeanQueryFactory<CommitBeanQuery> beanQueryFactory =
                new BeanQueryFactory<CommitBeanQuery>(CommitBeanQuery.class);
        queryConfiguration = new HashMap<String, Object>();
        beanQueryFactory.setQueryConfiguration(queryConfiguration);

        final LazyQueryContainer container = new LazyQueryContainer(beanQueryFactory,"hash",
                20, false);

        container.addContainerFilter(new Compare.Equal("branch", sinceField.getValue()));
        container.addContainerProperty("hash", String.class, null, true, false);
        container.addContainerProperty("committerDate", Date.class, null, true, false);
        container.addContainerProperty("committer", String.class, null, true, false);
        container.addContainerProperty("authorDate", Date.class, null, true, false);
        container.addContainerProperty("author", String.class, null, true, false);
        container.addContainerProperty("tags", String.class, null, true, false);
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
                "tags",
                "subject"});

        table.setColumnWidth("hash", 50);
        table.setColumnWidth("committerDate", 120);
        table.setColumnWidth("committer", 100);
        table.setColumnWidth("authorDate", 120);
        table.setColumnWidth("author", 100);
        table.setColumnWidth("tags", 100);

        table.setColumnHeaders(new String[]{
                getSite().localize("field-hash"),
                getSite().localize("field-committer-date"),
                getSite().localize("field-committer"),
                getSite().localize("field-author-date"),
                getSite().localize("field-author"),
                getSite().localize("field-tags"),
                getSite().localize("field-subject")
        });

        table.setColumnCollapsingAllowed(true);
        table.setColumnCollapsed("authorDate", true);
        table.setColumnCollapsed("author", true);
        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setImmediate(true);

        gridLayout.addComponent(table, 0, 2);

        final Button refreshButton = new Button(getSite().localize("button-refresh"));
        buttonLayout.addComponent(refreshButton);
        refreshButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        refreshButton.addStyleName("default");
        refreshButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                repository = (Repository) repositoryField.getValue();

                if (repository != null && sinceField.isValid() && untilField.isValid()) {
                    queryConfiguration.put("repository", repository);
                    final StringBuilder range = new StringBuilder(sinceField.getValue());
                    if (untilField.getValue().length() > 0) {
                        range.append("..");
                        range.append(untilField);
                    }
                    container.removeAllContainerFilters();
                    container.addContainerFilter(new Compare.Equal("range", range.toString()));
                    container.refresh();
                }
            }
        });

        final Button fetchButton = new Button("Fetch");
        buttonLayout.addComponent(fetchButton);
        fetchButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Repository repository = (Repository) repositoryField.getValue();
                Notification.show("Executed fetch. " + Shell.execute("git fetch", repository.getPath()));
            }
        });


        final Button addReviewButton = getSite().getButton("add-review");
        addReviewButton.setEnabled(false);
        buttonLayout.addComponent(addReviewButton);
        addReviewButton.addClickListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                final Object[] selection = ((Set) table.getValue()).toArray();
                if (selection != null && selection.length == 2) {
                    final String hashOne = (String) selection[0];
                    final String hashTwo = (String) selection[1];
                    final Commit commitOne = ((NestingBeanItem<Commit>)container.getItem(hashOne)).getBean();
                    final Commit commitTwo = ((NestingBeanItem<Commit>)container.getItem(hashTwo)).getBean();

                    final Commit sinceCommit;
                    final Commit untilCommit;
                    if (commitOne.getCommitterDate().getTime() > commitTwo.getCommitterDate().getTime()) {
                        sinceCommit = commitTwo;
                        untilCommit = commitOne;
                    } else {
                        sinceCommit = commitOne;
                        untilCommit = commitTwo;
                    }
                    createReview(sinceCommit.getHash(), untilCommit.getHash());
                } else {
                    final ReviewRangeDialog dialog = new ReviewRangeDialog(
                            new ReviewRangeDialog.DialogListener() {
                        @Override
                        public void onOk(String sinceHash, String untilHash) {
                            if (sinceHash.length() > 0 && untilHash.length() > 0) {
                                createReview(sinceHash, untilHash);
                            }
                        }

                        @Override
                        public void onCancel() {
                        }
                    }, ((selection != null && selection.length == 1) ?
                            (String) selection[0] : ""), "");
                    dialog.setCaption("Please enter final comment.");
                    UI.getCurrent().addWindow(dialog);
                    dialog.getSinceField().focus();
                }


            }
        });

        table.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
                final Set selection = (Set) table.getValue();
                addReviewButton.setEnabled(selection != null && (
                        selection.size() == 1 || selection.size() == 2));
            }
        });

    }

    private void createReview(String sinceHash, String untilHash) {
        final Review review = new Review();
        review.setCreated(new Date());
        review.setModified(review.getCreated());
        review.setSinceHash(sinceHash);
        review.setUntilHash(untilHash);
        review.setOwner((Company) getSite().getSiteContext().getObject(Company.class));
        review.setRepository(repository);
        review.setAuthor(((SecurityProviderSessionImpl)
                getSite().getSecurityProvider()).getUserFromSession());
        final ReviewFlowlet reviewView = getViewSheet().forward(ReviewFlowlet.class);
        reviewView.edit(review, true);
    }

    @Override
    public void enter() {
    }

}
