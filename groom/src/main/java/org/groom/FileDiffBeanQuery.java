package org.groom;

import com.vaadin.data.Container;
import com.vaadin.data.util.filter.Compare;
import org.groom.model.Commit;
import org.groom.model.FileDiff;
import org.groom.model.Repository;
import org.groom.model.ReviewStatus;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlaukkan
 * Date: 23.9.2013
 * Time: 18:51
 * To change this template use File | Settings | File Templates.
 */
public class FileDiffBeanQuery extends AbstractBeanQuery<FileDiff> {

    private String range = null;
    private String[] lines;
    private final Repository repository;

    public FileDiffBeanQuery(QueryDefinition definition,
                             Map<String, Object> queryConfiguration, Object[] sortPropertyIds,
                             boolean[] sortStates) {
        super(definition, queryConfiguration, sortPropertyIds, sortStates);
        repository = (Repository) queryConfiguration.get("repository");
        for (final Container.Filter filter : definition.getFilters()) {
            if (filter instanceof Compare.Equal) {
                final Compare.Equal equal = (Compare.Equal) filter;
                if (equal.getPropertyId().equals("range")) {
                    range = (String) equal.getValue();
                }
            }
        }
    }

    @Override
    protected FileDiff constructBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        if (repository == null) {
            return 0;
        }
        if (lines == null) {
            if (range == null) {
                return 0;
            }
            final String result = Shell.execute("git diff --raw -l --ignore-all-space " + range + " -- | more", repository.getPath());
            if (result.length() == 0) {
                return 0;
            }
            lines = result.split("\n");
        }
        return lines.length;

    }

    @Override
    protected List<FileDiff> loadBeans(int startIndex, int count) {
        final ArrayList<FileDiff> fileDiffs = new ArrayList<FileDiff>();
        for (int i = startIndex; i < startIndex + count; i++) {
            final String line = lines[i];
            if (line.length() < 38) {
                continue;
            }
            final char status = line.charAt(37);
            if (line.length() < 40) {
                continue;
            }
            final String path = line.substring(39).trim();
            final ReviewStatus reviewStatus;
            if (getQueryConfiguration() != null) {
                reviewStatus = (ReviewStatus) getQueryConfiguration().get("status");
            } else {
                reviewStatus = null;
            }
            final FileDiff fileDiff = new FileDiff(status, path, i, reviewStatus);
            fileDiffs.add(fileDiff);
        }
        return fileDiffs;
    }

    @Override
    protected void saveBeans(List<FileDiff> commits, List<FileDiff> commits2, List<FileDiff> commits3) {
        throw new UnsupportedOperationException();
    }
}
