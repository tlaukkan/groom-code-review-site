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

import com.vaadin.data.util.filter.Compare;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import org.groom.BlameReader;
import org.groom.model.BlameLine;
import org.groom.model.LineChangeType;
import org.groom.model.Review;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.client.AceAnnotation;
import org.vaadin.aceeditor.client.AceMarker;
import org.vaadin.aceeditor.client.AceRange;
import org.vaadin.addons.lazyquerycontainer.LazyEntityContainer;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.FieldDescriptor;
import org.vaadin.addons.sitekit.grid.FilterDescriptor;
import org.vaadin.addons.sitekit.grid.FormattingTable;
import org.vaadin.addons.sitekit.grid.Grid;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.util.ContainerUtil;

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

    private String sinceHash;

    private String untilHash;
    private AceEditor editor;

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

    @Override
    public void initialize() {
        editor = new AceEditor();
        editor.setSizeFull();
        editor.setReadOnly(true);
        setViewContent(editor);
    }

    @Override
    public void enter() {
    }

    public void setFileDiff(final String path, final String sinceHash, final String untilHash, final boolean added) {
        this.path = path;
        this.sinceHash = sinceHash;
        this.untilHash = untilHash;

        final List<BlameLine> forwardBlames = BlameReader.read(path, sinceHash, untilHash, false);
        final List<BlameLine> reverseBlames;
        if (added) {
            reverseBlames = new ArrayList<BlameLine>();
        } else {
            reverseBlames = BlameReader.read(path, sinceHash, untilHash, true);
        }

        // Inserting deletes among forward blames
        for (final BlameLine reverseBlame : reverseBlames) {
            if (reverseBlame.getType() == LineChangeType.DELETED) {
                boolean inserted = false;
                for (int i = 0; i < forwardBlames.size(); i++) {
                    final BlameLine forwardBlame = forwardBlames.get(i);
                    if ((forwardBlame.getType() == LineChangeType.NONE || forwardBlame.getType() == LineChangeType.DELETED)
                            && forwardBlame.getOriginalLine() >= reverseBlame.getOriginalLine()) {
                        forwardBlames.add(i, reverseBlame);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    forwardBlames.add(reverseBlame);
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
        for (final BlameLine line : forwardBlames) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.getLine());
        }

        editor.clearRowAnnotations();
        editor.clearMarkers();
        editor.setReadOnly(false);
        editor.setValue(builder.toString());
        editor.setReadOnly(true);
        for (int i = 0; i < forwardBlames.size(); i++) {
            final BlameLine blameLine = forwardBlames.get(i);
            if (blameLine.getType() == LineChangeType.ADDED) {
                editor.addRowAnnotation(new AceAnnotation(
                        "Author: " + blameLine.getAuthorName()
                        + " Commit: " + blameLine.getHash(),
                        AceAnnotation.Type.info) , i);
                editor.addMarker(new AceRange(i, 0, i + 1, 0),
                        "marker-line-added", AceMarker.Type.line, false, AceMarker.OnTextChange.ADJUST);
            }
            if (blameLine.getType() == LineChangeType.DELETED) {
                editor.addRowAnnotation(new AceAnnotation(
                        "Author: " + blameLine.getAuthorName()
                        + " Commit: " + blameLine.getHash(),
                        AceAnnotation.Type.info) , i);
                editor.addMarker(new AceRange(i, 0, i + 1, 0),
                        "marker-line-deleted", AceMarker.Type.line, false,  AceMarker.OnTextChange.ADJUST);
            }

        }
    }
}
