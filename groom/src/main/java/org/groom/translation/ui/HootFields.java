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
package org.groom.translation.ui;

import com.vaadin.data.Validator;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import org.bubblecloud.ilves.component.field.TimestampField;
import org.bubblecloud.ilves.component.formatter.TimestampConverter;
import org.bubblecloud.ilves.component.grid.FieldDescriptor;
import org.groom.translation.model.Entry;

import java.util.*;

/**
 * AgoControl Site field descriptors.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class HootFields {

    /**
     * Private default constructor to disable construction.
     */
    private HootFields() {
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

        HootFields.add(Entry.class, new FieldDescriptor(
                "entryId", "Entry ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));

        HootFields.add(Entry.class, new FieldDescriptor(
                "path", "Path",
                TextField.class, null,
                250, null, String.class, "",
                true, true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "basename", "Basename",
                TextField.class, null,
                100, null, String.class, "",
                true, true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "language", "Language",
                TextField.class, null,
                75, null, String.class, "",
                true, true, false));
        HootFields.add(Entry.class, new FieldDescriptor(
                "country", "Country",
                TextField.class, null,
                75, null, String.class, "",
                true, true, false));
        HootFields.add(Entry.class, new FieldDescriptor(
                "key", "Key",
                TextField.class, null,
                -1, null, String.class, "",
                true, true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "value", "Value",
                TextArea.class, null,
                300, null, String.class, "",
                false, true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "author", "Author",
                TextField.class, null,
                100, null, String.class, "",
                true, true, false));

        HootFields.add(Entry.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, new TimestampConverter(),
                150, null, Date.class, null, true,
                true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, new TimestampConverter(),
                150, null, Date.class, null,
                true, true, true));
        HootFields.add(Entry.class, new FieldDescriptor(
                "deleted", "Deleted",
                TimestampField.class, new TimestampConverter(),
                150, null, Date.class, null,
                true, true, true));

    }
}
