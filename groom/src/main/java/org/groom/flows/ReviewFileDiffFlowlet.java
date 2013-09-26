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

import com.vaadin.ui.*;
import org.groom.BlameReader;
import org.groom.dao.ReviewDao;
import org.groom.flows.reviewer.ReviewFlowlet;
import org.groom.model.*;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.client.AceAnnotation;
import org.vaadin.aceeditor.client.AceMarker;
import org.vaadin.aceeditor.client.AceRange;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.Grid;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.util.EmailUtil;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Review list Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class ReviewFileDiffFlowlet extends AbstractFlowlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The grid. */
    private Grid grid;

    private String path;

    private AceEditor editor;
    private List<BlameLine> blames;
    private FileDiff fileDiff;
    private EntityManager entityManager;
    private Review review;
    private AceEditor.SelectionChangeListener selectionChangeListener;

    @Override
    public String getFlowletKey() {
        return "review-file-diff";
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private static int findLine(final String value, final int index) {
        int line = 0;
        for( int i=0; i<index; i++ ) {
            if( value.charAt(i) == '\n' ) {
                line++;
            }
        }
        return line;
    }

    int lastCursor = 0;

    @Override
    public void initialize() {
        entityManager = getSite().getSiteContext().getObject(EntityManager.class);

        final GridLayout gridLayout = new GridLayout(1,2);
        gridLayout.setSpacing(true);
        gridLayout.setSizeFull();
        gridLayout.setColumnExpandRatio(0, 1f);
        gridLayout.setRowExpandRatio(0, 1f);
        setViewContent(gridLayout);
        editor = new AceEditor();
        editor.setSizeFull();
        editor.setReadOnly(true);
        editor.setImmediate(true);
        selectionChangeListener = new AceEditor.SelectionChangeListener() {
            @Override
            public void selectionChanged(AceEditor.SelectionChangeEvent e) {
                int cursor = e.getSelection().getCursorPosition();
                if (lastCursor != cursor && fileDiff.getReviewStatus() != null) {
                    lastCursor = cursor;
                    final int cursorLine = findLine(editor.getValue(), cursor);
                    final BlameLine blame = blames.get(cursorLine);
                    if (blame.getType() != LineChangeType.NONE) {
                        final CommentDialog commentDialog = new CommentDialog(new CommentDialog.DialogListener() {
                            @Override
                            public void onOk(final String message) {
                                final ReviewStatus reviewStatus = fileDiff.getReviewStatus();
                                final Review review = reviewStatus.getReview();
                                final Date date = new Date();
                                if (message.trim().length() > 0) {
                                    final Comment comment = new Comment(review, reviewStatus.getReviewer(),
                                            blame.getHash(), fileDiff.getPath(), blame.getFinalLine(), cursorLine,
                                            0, message, blame.getAuthorName(), blame.getCommitterName(), date, date);
                                    ReviewDao.saveComment(entityManager, comment);
                                    addComment(comment);
                                    final Company company = getSite().getSiteContext().getObject(Company.class);
                                    final Thread emailThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            EmailUtil.send(PropertiesUtil.getProperty("groom", "smtp-host"),
                                                    blame.getAuthorEmail(), company.getSupportEmailAddress(),
                                                    "You received comment on review '" + review.getTitle() + "'",
                                                    "Reviewer: " + reviewStatus.getReviewer().getFirstName()
                                                            + " " + reviewStatus.getReviewer().getLastName() + "\n" +
                                                            "Commit: " + blame.getHash() + "\n" +
                                                            "File: " + fileDiff.getPath() + "\n" +
                                                            "Original Line: " + blame.getOriginalLine() + "\n" +
                                                            "Diff line: " + cursorLine + "\n" +
                                                            blame.getType() + ":" + blame.getLine() + "\n" +
                                                            "Message: " + message);
                                        }
                                    });
                                    emailThread.start();
                                }
                            }

                            @Override
                            public void onCancel() {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });
                        int cursorPosition = editor.getCursorPosition();
                        commentDialog.setCaption("Please enter groom text for " + blame.getAuthorName()
                                + " at line: " + (cursorLine + 1));
                        UI.getCurrent().addWindow(commentDialog);
                        commentDialog.getTextArea().focus();

                    }
                }
            }
        };

        editor.addSelectionChangeListener(selectionChangeListener);
        gridLayout.addComponent(editor, 0, 0);

        if (getViewSheet().getFlowlet(ReviewFlowlet.class) != null) {
            final HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);
            gridLayout.addComponent(buttonLayout, 0, 1);

            final Button previousButton = new Button(getSite().localize("button-previous"));
            buttonLayout.addComponent(previousButton);
            previousButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                    view.previous(path);
                }
            });

            final Button nextButton = new Button(getSite().localize("button-next"));
            buttonLayout.addComponent(nextButton);
            nextButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                    view.next(path);
                }
            });
        }
    }

    private void addComment(Comment comment) {
        editor.addRowAnnotation(new AceAnnotation(
                "Groomed by reviewer: " + comment.getReviewer().getFirstName()
                        + " - " + comment.getReviewer().getLastName()
                        + " Message: " + comment.getMessage(),
                AceAnnotation.Type.warning) , comment.getDiffLine());
    }

    @Override
    public void enter() {
    }

    public void setFileDiff(final Review review, final FileDiff fileDiff, final int toLine) {
        this.review = review;
        this.fileDiff = fileDiff;
        this.path = fileDiff.getPath();

        blames = BlameReader.read(path, review.getSinceHash(), review.getUntilHash(), false);
        final List<BlameLine> reverseBlames;
        if (fileDiff.getStatus() == 'A') {
            reverseBlames = new ArrayList<BlameLine>();
        } else {
            reverseBlames = BlameReader.read(path, review.getSinceHash(), review.getUntilHash(), true);
        }

        // Inserting deletes among forward blames
        for (final BlameLine reverseBlame : reverseBlames) {
            if (reverseBlame.getType() == LineChangeType.DELETED) {
                boolean inserted = false;
                for (int i = 0; i < blames.size(); i++) {
                    final BlameLine forwardBlame = blames.get(i);
                    if ((forwardBlame.getType() == LineChangeType.NONE || forwardBlame.getType() == LineChangeType.DELETED)
                            && forwardBlame.getOriginalLine() >= reverseBlame.getOriginalLine()) {
                        blames.add(i, reverseBlame);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    blames.add(reverseBlame);
                }
            }
        }

        if (path.endsWith(".java")) {
            editor.setMode(AceMode.java);
        } else if (path.endsWith(".xml")) {
            editor.setMode(AceMode.xml);
        } else if (path.endsWith(".js")) {
            editor.setMode(AceMode.javascript);
        } else if (path.endsWith(".css")) {
            editor.setMode(AceMode.css);
        } else if (path.endsWith(".jsp")) {
            editor.setMode(AceMode.jsp);
        } else {
            editor.setMode(AceMode.asciidoc);
        }

        final StringBuilder builder = new StringBuilder();
        for (final BlameLine line : blames) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.getLine());
        }

        editor.setCaption(fileDiff.getPath());
        editor.clearRowAnnotations();
        editor.clearMarkers();
        editor.setReadOnly(false);
        editor.setValue(builder.toString());
        editor.setCursorPosition(0);
        if (toLine != 0) {
            editor.setSelectionRowCol(toLine, 0, toLine + 1, 0);
        }
        lastCursor = editor.getCursorPosition();
        editor.setReadOnly(true);
        for (int i = 0; i < blames.size(); i++) {
            final BlameLine blameLine = blames.get(i);
            if (blameLine.getType() == LineChangeType.ADDED) {
                editor.addRowAnnotation(new AceAnnotation(
                        "Added by author: " + blameLine.getAuthorName()
                        + " Commit: " + blameLine.getHash()
                        + " Summary: " + blameLine.getSummary(),
                        AceAnnotation.Type.info) , i);
                editor.addMarker(new AceRange(i, 0, i + 1, 0),
                        "marker-line-added", AceMarker.Type.line, false, AceMarker.OnTextChange.ADJUST);
            }
            if (blameLine.getType() == LineChangeType.DELETED) {
                editor.addRowAnnotation(new AceAnnotation(
                        "Deleted by author: " + blameLine.getAuthorName()
                        + " Commit: " + blameLine.getHash()
                        + " Summary: " + blameLine.getSummary(),
                        AceAnnotation.Type.info) , i);
                editor.addMarker(new AceRange(i, 0, i + 1, 0),
                        "marker-line-deleted", AceMarker.Type.line, false,  AceMarker.OnTextChange.ADJUST);
            }

        }

        final List<Comment> comments = ReviewDao.getComments(entityManager, review);
        for (final Comment comment : comments) {
            if (comment.getPath().equals(fileDiff.getPath())) {
                addComment(comment);
            }
        }
    }
}
