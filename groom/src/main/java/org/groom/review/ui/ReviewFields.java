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
package org.groom.review.ui;

import com.vaadin.ui.CheckBox;
import org.bubblecloud.ilves.component.field.UserField;
import org.bubblecloud.ilves.component.formatter.TimestampConverter;
import org.bubblecloud.ilves.model.Group;
import org.bubblecloud.ilves.model.User;
import org.groom.review.ui.fields.RepositoryField;
import org.groom.review.model.Comment;
import com.vaadin.data.Validator;
import com.vaadin.ui.TextField;
import org.groom.review.model.Repository;
import org.groom.review.model.Review;
import org.groom.review.model.ReviewStatus;
import org.groom.review.ui.validators.PathValidator;
import org.groom.review.ui.validators.UrlValidator;

import org.bubblecloud.ilves.component.grid.FieldDescriptor;
import org.bubblecloud.ilves.component.field.GroupField;
import org.bubblecloud.ilves.component.field.TimestampField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AgoControl Site field descriptors.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class ReviewFields {

    /**
     * Private default constructor to disable construction.
     */
    private ReviewFields() {
    }

    /**
     * Flag reflecting whether initialization of field descriptors has been done
     * for JVM.
     */
    private static boolean initialized = false;

    /**
     * Map of entity class field descriptors.
     */
    private static Map<Class<?>, List<FieldDescriptor>> fieldDescriptors = new HashMap<Class<?>, List<FieldDescriptor>>();

    /**
     * Adds a field descriptor for given entity class.
     * @param entityClass The entity class.
     * @param fieldDescriptor The field descriptor to add.
     */
    public static void add(final Class<?> entityClass, final FieldDescriptor fieldDescriptor) {
        if (!fieldDescriptors.containsKey(entityClass)) {
            fieldDescriptors.put(entityClass, new ArrayList<FieldDescriptor>());
        }
        fieldDescriptors.get(entityClass).add(fieldDescriptor);
    }

    /**
     * Adds a field descriptor for given entity class.
     * @param entityClass The entity class.
     * @param fieldDescriptor The field descriptor to add.
     * @param validator The field validator.
     */
    public static void add(final Class<?> entityClass, final FieldDescriptor fieldDescriptor, final Validator validator) {
        fieldDescriptor.addValidator(validator);
        add(entityClass, fieldDescriptor);
    }

    /**
     * Gets field descriptors for given entity class.
     * @param entityClass The entity class.
     * @return an unmodifiable list of field descriptors.
     */
    public static List<FieldDescriptor> getFieldDescriptors(final Class<?> entityClass) {
        return Collections.unmodifiableList(fieldDescriptors.get(entityClass));
    }

    /**
     * Initialize field descriptors if not done yet.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ReviewFields.add(Repository.class, new FieldDescriptor(
                "repositoryId", "Repository ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        ReviewFields.add(Repository.class, new FieldDescriptor(
                "path", "Path",
                TextField.class, null,
                300, null, User.class, null,
                false, true, true), new PathValidator());
        ReviewFields.add(Repository.class, new FieldDescriptor(
                "url", "URL",
                TextField.class, null,
                300, null, User.class, null,
                false, true, true), new UrlValidator());
        ReviewFields.add(Repository.class, new FieldDescriptor(
                "created", "Created", TimestampField.class, new TimestampConverter(), 150, null, Date.class, null, true,
                true, true));
        ReviewFields.add(Repository.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null,
                true, true, false));

        ReviewFields.add(Review.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null, true,
                true, true));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null,
                true, true, true));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "reviewId", "Review ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "repository", "Repository",
                RepositoryField.class, null,
                200, null, Repository.class, null,
                true, true, false));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "author", "Author",
                UserField.class, null,
                200, null, User.class, null,
                true, true, false));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "sinceHash", "Since Hash",
                TextField.class, null,
                70, null, String.class, "",
                true, true, true));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "untilHash", "Until Hash",
                TextField.class, null,
                70, null, String.class, "",
                true, true, true));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "title", "Title",
                TextField.class, null,
                -1, null, String.class, "",
                false, true, true));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "diffCount", "Diffs",
                TextField.class, null,
                50, null, Integer.class, "",
                true, true, false));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "completed", "Completed",
                CheckBox.class, null,
                70, null, Boolean.class, false,
                false, true, false));
        ReviewFields.add(Review.class, new FieldDescriptor(
                "reviewGroup", "Review Group",
                GroupField.class, null, 200, null, Group.class,
                null, false, true, true));

        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "completed", "",
                CheckBox.class, null,
                15, null, Boolean.class, false,
                false, true, false));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null, true,
                true, true));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null,
                true, true, true));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "reviewStatusId", "Review Status ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "reviewer", "Reviewer",
                TextField.class, null,
                200, null, User.class, null,
                true, true, false));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "progress", "Progress",
                TextField.class, null,
                30, null, Integer.class, null,
                true, true, false));
        ReviewFields.add(ReviewStatus.class, new FieldDescriptor(
                "comment", "Message",
                TextField.class, null,
                -1, null, String.class, "",
                false, true, true));

        ReviewFields.add(Comment.class, new FieldDescriptor(
                "severity", "",
                TextField.class, null,
                15, null, Integer.class, null,
                true, true, false));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "message", "Message",
                TextField.class, null,
                -1, null, String.class, "",
                false, true, true));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null, true,
                true, true));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, new TimestampConverter(),
                130, null, Date.class, null,
                true, true, true));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "reviewCommentId", "Comment ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "author", "Change Author",
                TextField.class, null,
                200, null, String.class, "",
                false, true, true));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "hash", "Hash",
                TextField.class, null,
                50, null, String.class, null,
                true, true, false));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "reviewer", "Reviewer",
                TextField.class, null,
                200, null, User.class, null,
                true, true, false));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "path", "path",
                TextField.class, null,
                400, null, String.class, "",
                false, true, true));
        ReviewFields.add(Comment.class, new FieldDescriptor(
                "line", "Line",
                TextField.class, null,
                30, null, Integer.class, null,
                true, true, false));

    }
}
