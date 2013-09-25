package org.groom.dao;

import org.groom.model.Review;
import org.groom.model.ReviewStatus;
import org.vaadin.addons.sitekit.model.User;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 25.9.2013
 * Time: 8:14
 * To change this template use File | Settings | File Templates.
 */
public class ReviewDao {
    public static void saveReviewStatus(final EntityManager entityManager, final ReviewStatus reviewStatus) {
        entityManager.getTransaction().begin();
        try {
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
}
