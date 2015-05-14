package org.groom.review.report;

import com.vaadin.server.VaadinSession;
import org.bubblecloud.ilves.site.DefaultSiteUI;
import org.groom.shell.BlameReader;
import org.groom.shell.Shell;
import org.groom.review.dao.ReviewDao;
import org.groom.review.model.*;

import java.io.*;
import java.util.List;
import javax.persistence.EntityManager;
import javax.servlet.*;
import javax.servlet.http.*;

public class ReportServlet extends HttpServlet {
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        final VaadinSession vaadinSession = (VaadinSession) request.getSession().getAttribute(
                "com.vaadin.server.VaadinSession.Vaadin Application Servlet");
        if (vaadinSession == null) {
            return;
        }
        final List<String> roles = (List<String>) vaadinSession.getAttribute("roles");
        if (roles == null || !roles.contains("administrator")) {
            return;
        }

        final String reviewId = request.getParameter("reviewId");
        if (reviewId == null || reviewId.length() == 0) {
            return;
        }

        final EntityManager entityManager = DefaultSiteUI.getEntityManagerFactory().createEntityManager();

        final Review review = ReviewDao.getReview(entityManager, reviewId);
        if (review == null) {
            return;
        }

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("Review Report");

        out.println();
        out.println("_Summary_");
        out.println();

        out.println("Title: " + review.getTitle());
        out.println("Created: " + review.getModified());
        out.println("Modified: " + review.getModified());
        out.println("Author: " + review.getAuthor().getFirstName() + " " + review.getAuthor().getLastName());
        out.println("Since Hash: " + review.getSinceHash());
        out.println("Until Hash: " + review.getUntilHash());
        out.println("File Changes: " + review.getDiffCount());
        out.println("Completed: " + review.isCompleted());

        out.println();
        out.println("_Reviewers_");
        out.println();

        final List<ReviewStatus> reviewStatuses = ReviewDao.getReviewStatuses(entityManager, review);
        for (final ReviewStatus reviewStatus : reviewStatuses) {
            out.println("Reviewer: " + reviewStatus.getReviewer().getFirstName() + " " + reviewStatus.getReviewer().getLastName() + " (Progress: " + reviewStatus.getProgress() + "% Completed: " + reviewStatus.isCompleted() + ")");
        }

        out.println();
        out.println("_Comments_");
        out.println();

        final List<Comment> comments = ReviewDao.getComments(entityManager, review);
        for (final Comment comment : comments) {
            out.println(comment.getPath());
            out.println("Line: " +comment.getLine());
            out.println("Commit: " + comment.getHash());
            out.println("Reviewer: " + comment.getReviewer().getFirstName() + " " + comment.getReviewer().getLastName());
            out.println("Change author: " + comment.getAuthor());
            final String severity;
            switch (comment.getSeverity()) {
                case 1:
                    severity = "+1";
                    break;
                case -1:
                    severity = "Warning";
                    break;
                case -2:
                    severity = "Red Flag";
                    break;
                default:
                    severity = Integer.toString(comment.getSeverity());
            }
            out.println("Severity: " + severity);
            out.println("Message: " + comment.getMessage());
            out.println();
        }

        out.println();
        out.println("_File Changes_");
        out.println();

        final String result = Shell.execute(
                "git diff -l --ignore-all-space --name-status " + review.getSinceHash() + " " + review.getUntilHash() + " -- | more",
                review.getRepository().getPath());
        out.println(result);

        out.println();
        out.println("_Diffs_");
        out.println();

        final String[] lines = result.split("\n");

        for (int i = 0; i < lines.length; i++) {
            final char status = lines[i].charAt(0);
            final String path = lines[i].substring(1).trim();
            out.println(status + " " + path);
            out.println();
            final List<BlameLine> blames = BlameReader.readBlameLines(review.getRepository().getPath(),
                    path, status, review.getSinceHash(), review.getUntilHash());
            boolean lastLineWasChange = false;
            for (final BlameLine blame : blames) {
                if (blame.getType() != LineChangeType.NONE) {
                    lastLineWasChange = true;
                    if (blame.getType().equals(LineChangeType.ADDED)) {
                        out.print("A|");
                        out.print(blame.getHash());
                        out.print("|");
                        out.print(String.format("%05d", blame.getFinalLine()));
                        out.print("|");
                        out.println(blame.getLine());
                    }
                    if (blame.getType().equals(LineChangeType.DELETED)) {
                        out.print("D|");
                        out.print(blame.getHash());
                        out.print("|");
                        out.print(String.format("%05d", blame.getOriginalLine()));
                        out.print("|");
                        out.println(blame.getLine());
                    }
                } else {
                    if (lastLineWasChange && i != lines.length - 1) {
                        out.println();
                    }
                    lastLineWasChange = false;
                }
            }
            if (lastLineWasChange && i != lines.length - 1) {
                out.println();
            }
        }
    }
}