package org.groom.review.dao;

import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.model.User;
import org.groom.review.model.Comment;
import org.groom.model.Repository;
import org.groom.review.model.Review;
import org.groom.review.model.ReviewStatus;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 25.9.2013
 * Time: 8:14
 * To change this template use File | Settings | File Templates.
 */
public class ReviewDao {

    public static List<Repository> getRepositories(final EntityManager entityManager, final Company owner) {
        final Query query =
                entityManager.createQuery("select e from Repository as e where e.owner=:owner order by e.path");
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    public static Review getReview(final EntityManager entityManager, final String reviewId) {
        final Query query =
                entityManager.createQuery("select e from Review as e where e.reviewId=:reviewId");
        query.setParameter("reviewId", reviewId);
        final List<Review> reviews = query.getResultList();
        if (reviews.size() == 0) {
            return null;
        }
        return reviews.get(0);
    }

    public static void saveReviewStatus(final EntityManager entityManager, final ReviewStatus reviewStatus) {
        entityManager.getTransaction().begin();
        try {
            reviewStatus.setModified(new Date());
            entityManager.persist(reviewStatus);
        } catch (final RuntimeException t) {
            entityManager.getTransaction().rollback();
            throw t;
        }
        entityManager.getTransaction().commit();
    }

    public static ReviewStatus getReviewStatus(final EntityManager entityManager, final User user, final Review review) {
        final Query query =
                entityManager.createQuery("select e from ReviewStatus as e where e.reviewer=:user and e.review=:review");
        query.setParameter("user", user);
        query.setParameter("review", review);
        final List<ReviewStatus> reviewStatuses = query.getResultList();
        if (reviewStatuses.size() == 0) {
            return null;
        }
        return reviewStatuses.get(0);
    }

    public static void saveComment(final EntityManager entityManager, final Comment comment) {
        entityManager.getTransaction().begin();
        try {
            entityManager.persist(comment);
        } catch (final RuntimeException t) {
            entityManager.getTransaction().rollback();
            throw t;
        }
        entityManager.getTransaction().commit();
    }

    public static List<ReviewStatus> getReviewStatuses(final EntityManager entityManager, final Review review) {
        final Query query =
                entityManager.createQuery("select e from ReviewStatus as e where e.review=:review order by e.reviewer.firstName, e.reviewer.lastName");
        query.setParameter("review", review);
        return query.getResultList();
    }

    public static List<Comment> getComments(final EntityManager entityManager, final Review review) {
        final Query query =
                entityManager.createQuery("select e from Comment as e where e.review=:review order by e.path, e.line");
        query.setParameter("review", review);
        return query.getResultList();
    }

}
