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
    private GridLayout gridLayout;

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
        for( int i=0; i<Math.min(value.length(),index); i++ ) {
            if( value.charAt(i) == '\n' ) {
                line++;
            }
        }
        return line;
    }

    @Override
    public void initialize() {
        entityManager = getSite().getSiteContext().getObject(EntityManager.class);

        gridLayout = new GridLayout(1,2);
        gridLayout.setSpacing(true);
        gridLayout.setSizeFull();
        gridLayout.setColumnExpandRatio(0, 1f);
        gridLayout.setRowExpandRatio(1, 1f);
        setViewContent(gridLayout);

        selectionChangeListener = new AceEditor.SelectionChangeListener() {
            @Override
            public void selectionChanged(AceEditor.SelectionChangeEvent e) {

            }
        };

        //gridLayout.addComponent(editor, 0, 0);

        if (getViewSheet().getFlowlet(ReviewFlowlet.class) != null) {
            final HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);
            gridLayout.addComponent(buttonLayout, 0, 0);

            final Button previousButton = new Button(getSite().localize("button-previous-diff"));
            previousButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(previousButton);
            previousButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                    view.previous(path);
                }
            });

            final Button nextButton = new Button(getSite().localize("button-next-diff"));
            nextButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(nextButton);
            nextButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                    view.next(path);
                }
            });

            final Button scrollToPreviousChangeButton = new Button(getSite().localize("button-scroll-to-previous-change"));
            scrollToPreviousChangeButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(scrollToPreviousChangeButton);
            scrollToPreviousChangeButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    int cursorLine = getScrolledTowRow();

                    for (int i = cursorLine; i >= 0; i--) {
                        cursorLine = i;
                        if (blames.get(i).getType() == LineChangeType.NONE) {
                            break;
                        }
                    }
                    for (int i = cursorLine; i >= 0; i--) {
                        cursorLine = i;
                        if (i == 0 | blames.get(i).getType() != LineChangeType.NONE) {
                            break;
                        }
                    }
                    for (int i = cursorLine; i >= 0; i--) {
                        if (i == 0) {
                            final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                            //view.previous(path);
                            scrollToRow(0);
                            break;
                        }
                        if (blames.get(i).getType() == LineChangeType.NONE) {
                            scrollToRow(i + 1);
                            break;
                        }
                    }
                }
            });

            final Button scrollToNextChangeButton = new Button(getSite().localize("button-scroll-to-next-change"));
            scrollToNextChangeButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(scrollToNextChangeButton);
            scrollToNextChangeButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    int cursorLine = getScrolledTowRow();

                    for (int i = cursorLine; i < blames.size(); i++) {
                        cursorLine = i;
                        if (blames.get(i).getType() == LineChangeType.NONE) {
                            break;
                        }
                    }
                    for (int i = cursorLine; i < blames.size(); i++) {
                        if (i == blames.size() - 1) {
                            final ReviewFlowlet view = getViewSheet().getFlowlet(ReviewFlowlet.class);
                            //view.next(path);
                            scrollToRow(i);
                            break;
                        }
                        if (blames.get(i).getType() != LineChangeType.NONE) {
                            scrollToRow(i);
                            break;
                        }
                    }
                }
            });

            final Button scrollToCursorButton = new Button("Scroll to Cursor");
            scrollToCursorButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(scrollToCursorButton);
            scrollToCursorButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    if (editor.getSelection() == null || editor.getCursorPosition() < 0) {
                        return;
                    }
                    int cursor = editor.getCursorPosition();
                    if (fileDiff.getReviewStatus() != null) {
                        final int cursorLine = findLine(editor.getValue(), cursor);
                        scrollToRow(cursorLine);
                    }
                }
            });


            final Button groomButton = getSite().getButton("groom");
            groomButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(groomButton);
            groomButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    if (editor.getSelection() == null || editor.getCursorPosition() < 0) {
                        return;
                    }
                    int cursor = editor.getCursorPosition();
                    if (fileDiff.getReviewStatus() != null) {
                        final int cursorLine = findLine(editor.getValue(), cursor);
                        final BlameLine blame = blames.get(cursorLine);

                        final CommentDialog commentDialog = new CommentDialog(new CommentDialog.DialogListener() {
                            @Override
                            public void onOk(final String message, final int severity) {
                                final ReviewStatus reviewStatus = fileDiff.getReviewStatus();
                                final Review review = reviewStatus.getReview();
                                final Date date = new Date();
                                if (message.trim().length() > 0) {
                                    final Comment comment = new Comment(review, reviewStatus.getReviewer(),
                                            blame.getHash(), fileDiff.getPath(), blame.getFinalLine(), cursorLine,
                                            severity, message, blame.getAuthorName(), blame.getCommitterName(), date, date);
                                    ReviewDao.saveComment(entityManager, comment);
                                    addComment(comment);
                                    final Company company = getSite().getSiteContext().getObject(Company.class);
                                    final Thread emailThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String severity;
                                            switch (comment.getSeverity()) {
                                                case 1:
                                                    severity = "Kudo";
                                                    break;
                                                case -1:
                                                    severity = "Warning";
                                                    break;
                                                case -2:
                                                    severity = "Red Flag";
                                                    break;
                                                default:
                                                    severity = Integer.toString(comment.getSeverity());
                                                    if (!severity.startsWith("-")) {
                                                        severity = "+" + severity;
                                                    }
                                            }
                                            EmailUtil.send(PropertiesUtil.getProperty("groom", "smtp-host"),
                                                    blame.getAuthorEmail(), company.getSupportEmailAddress(),
                                                    severity + " from review '" + review.getTitle() + "'",
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
            });

        }
    }

    public int getScrolledTowRow() {
        return scrolledTowRow;
    }

    private int scrolledTowRow = 0;

    private void scrollToRow(int i) {
        int scrollToRow = i - 3;
        if (scrollToRow < 0) {
            scrollToRow = 0;
        }
        scrolledTowRow = i;
        editor.scrollToRow(scrollToRow);
        editor.setCursorRowCol(i, 0);
    }

    private void addComment(Comment comment) {
        final AceAnnotation.Type type;
        switch (comment.getSeverity()) {
            case -1:
                type = AceAnnotation.Type.warning;
                break;
            case -2:
                type = AceAnnotation.Type.error;
                break;
            default:
                type = AceAnnotation.Type.info;
        }
        editor.addRowAnnotation(new AceAnnotation(
                "Groomed by reviewer: " + comment.getReviewer().getFirstName()
                        + " - " + comment.getReviewer().getLastName()
                        + " Message: " + comment.getMessage(),
                type) , comment.getDiffLine());
    }

    @Override
    public void enter() {
    }

    public void setFileDiff(final Review review, final FileDiff fileDiff, final int toLine) {

        if (editor != null) {
            gridLayout.removeComponent(editor);
        }

        editor = new AceEditor();
        editor.setThemePath("/static/ace");
        editor.setModePath("/static/ace");
        editor.setWorkerPath("/static/ace");
        editor.setSizeFull();
        editor.setImmediate(true);
        editor.addSelectionChangeListener(selectionChangeListener);

        this.review = review;
        this.fileDiff = fileDiff;
        this.path = fileDiff.getPath();

        final char status = fileDiff.getStatus();
        final String sinceHash = review.getSinceHash();
        final String untilHash = review.getUntilHash();

        blames = BlameReader.readBlameLines(path, status, sinceHash, untilHash);

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
        editor.setValue(builder.toString());
        editor.setReadOnly(true);
        BlameLine lastLine = null;
        int lastIndex = -1;
        for (int i = 0; i < blames.size(); i++) {
            final BlameLine blameLine = blames.get(i);
            if (lastLine != null
                    && (!blameLine.getHash().equals(lastLine.getHash())
                    || !blameLine.getType().equals(lastLine.getType()))) {

                if (lastLine.getType() == LineChangeType.ADDED) {
                    editor.addMarker(new AceRange(lastIndex, 0, i, 0),
                            "marker-line-added", AceMarker.Type.line, false,  AceMarker.OnTextChange.REMOVE);
                } else if (lastLine.getType() == LineChangeType.DELETED) {
                    editor.addMarker(new AceRange(lastIndex, 0, i, 0),
                            "marker-line-deleted", AceMarker.Type.line, false,  AceMarker.OnTextChange.REMOVE);
                }

                lastLine = null;
                lastIndex = -1;
            }
            if (lastLine == null
                    && (blameLine.getType() == LineChangeType.ADDED
                    || blameLine.getType() == LineChangeType.DELETED)) {
                lastLine = blameLine;
                lastIndex = i;

                if (blameLine.getType() == LineChangeType.ADDED) {
                    editor.addRowAnnotation(new AceAnnotation(
                            "Added by author: " + blameLine.getAuthorName()
                                + " Commit: " + blameLine.getHash()
                                + " Summary: " + blameLine.getSummary(),
                        AceAnnotation.Type.info) , i);
                } else if (blameLine.getType() == LineChangeType.DELETED) {
                    editor.addRowAnnotation(new AceAnnotation(
                            "Deleted by author: " + blameLine.getAuthorName()
                                    + " Commit: " + blameLine.getHash()
                                    + " Summary: " + blameLine.getSummary(),
                            AceAnnotation.Type.info) , i);
                }
            }
        }

        if (lastLine != null) {

            if (lastLine.getType() == LineChangeType.ADDED) {
                editor.addMarker(new AceRange(lastIndex, 0, blames.size(), 0),
                        "marker-line-added", AceMarker.Type.line, false,  AceMarker.OnTextChange.REMOVE);
            } else if (lastLine.getType() == LineChangeType.DELETED) {
                editor.addMarker(new AceRange(lastIndex, 0, blames.size(), 0),
                        "marker-line-deleted", AceMarker.Type.line, false,  AceMarker.OnTextChange.REMOVE);
            }

        }

        final List<Comment> comments = ReviewDao.getComments(entityManager, review);
        for (final Comment comment : comments) {
            if (comment.getPath().equals(fileDiff.getPath())) {
                addComment(comment);
            }
        }

        /*if (toLine != 0) {
            editor.setSelectionRowCol(toLine, 0, toLine + 1, 0);
        }*/
        gridLayout.addComponent(editor, 0, 1);
        scrollToRow(toLine);

    }

}
