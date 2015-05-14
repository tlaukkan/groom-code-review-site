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
package org.groom.review.ui.flows;

import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;
import org.bubblecloud.ilves.component.flow.AbstractFlowlet;
import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.util.EmailUtil;
import org.groom.shell.BlameReader;
import org.groom.review.dao.ReviewDao;
import org.groom.review.ui.flows.reviewer.ReviewFlowlet;
import org.groom.review.model.*;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.client.AceAnnotation;
import org.vaadin.aceeditor.client.AceMarker;
import org.vaadin.aceeditor.client.AceRange;

import javax.persistence.EntityManager;
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
    private Button groomButton;
    private HorizontalLayout editorLayout;

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

        if (getFlow().getFlowlet(ReviewFlowlet.class) != null) {
            final HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);
            gridLayout.addComponent(buttonLayout, 0, 0);

            final Button previousButton = new Button(getSite().localize("button-previous-diff"));
            previousButton.setClickShortcut(ShortcutAction.KeyCode.ARROW_LEFT);
            previousButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(previousButton);
            previousButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getFlow().getFlowlet(ReviewFlowlet.class);
                    view.previous(path);
                }
            });

            final Button nextButton = new Button(getSite().localize("button-next-diff"));
            nextButton.setClickShortcut(ShortcutAction.KeyCode.ARROW_RIGHT);
            nextButton.setHtmlContentAllowed(true);
            buttonLayout.addComponent(nextButton);
            nextButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent clickEvent) {
                    final ReviewFlowlet view = getFlow().getFlowlet(ReviewFlowlet.class);
                    view.next(path);
                }
            });

            final Button scrollToPreviousChangeButton = new Button(getSite().localize("button-scroll-to-previous-change"));
            scrollToPreviousChangeButton.setClickShortcut(ShortcutAction.KeyCode.ARROW_UP);
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
                            final ReviewFlowlet view = getFlow().getFlowlet(ReviewFlowlet.class);
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
            scrollToNextChangeButton.setClickShortcut(ShortcutAction.KeyCode.ARROW_DOWN);
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
                            final ReviewFlowlet view = getFlow().getFlowlet(ReviewFlowlet.class);
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


            groomButton = getSite().getButton("groom");
            groomButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
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
                                            EmailUtil.send(
                                                    blame.getAuthorEmail(), reviewStatus.getReviewer().getEmailAddress(),
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
        groomButton.focus();
    }

    public void setFileDiff(final Review review, final FileDiff fileDiff, final int toLine) {

        if (editorLayout != null) {
            gridLayout.removeComponent(editorLayout);
        }

        editor = new AceEditor();
        editorLayout = new HorizontalLayout();
        editorLayout.setWidth(100, Unit.PERCENTAGE);
        editorLayout.setHeight(UI.getCurrent().getPage().getBrowserWindowHeight() - 300, Unit.PIXELS);
        editorLayout.addComponent(editor);


        final Company company = getSite().getSiteContext().getObject(Company.class);
        editor.setThemePath(company.getUrl() + "/../static/ace");
        editor.setModePath(company.getUrl() + "/../static/ace");
        editor.setWorkerPath(company.getUrl() + "/../static/ace");
        editor.setSizeFull();
        editor.setImmediate(true);
        editor.addSelectionChangeListener(selectionChangeListener);

        this.review = review;
        this.fileDiff = fileDiff;
        this.path = fileDiff.getPath();

        final char status = fileDiff.getStatus();
        final String sinceHash = review.getSinceHash();
        final String untilHash = review.getUntilHash();

        blames = BlameReader.readBlameLines(review.getRepository().getPath(), path, status, sinceHash, untilHash);

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
        for (int i = 0; i < blames.size(); i++) {
            final BlameLine line = blames.get(i);
            builder.append(line.getLine());
            if (i < blames.size() - 1) {
                builder.append('\n');
            }
            /*switch (line.getType()) {
                case ADDED:
                    builder.append("[+]");
                    break;
                case DELETED:
                    builder.append("[-]");
                    break;
                default:
                    break;
            }*/
        }

        editor.setCaption("Progress: " + fileDiff.getReviewStatus().getProgress() + "% - "
                + "Path : " + fileDiff.getPath());
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
        gridLayout.addComponent(editorLayout, 0, 1);
        scrollToRow(toLine);

    }

}
