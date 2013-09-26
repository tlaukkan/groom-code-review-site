package org.groom;

import com.vaadin.data.Container;
import com.vaadin.data.util.filter.Compare;
import org.groom.model.Commit;
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
public class CommitBeanQuery extends AbstractBeanQuery<Commit> {

    private String range = null;

    public CommitBeanQuery(QueryDefinition definition,
        Map<String, Object> queryConfiguration, Object[] sortPropertyIds,
        boolean[] sortStates) {
        super(definition, queryConfiguration, sortPropertyIds, sortStates);
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
    protected Commit constructBean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        // "git rev-list --count master"
        final String result = Shell.execute("git rev-list " + range + " -- | wc -l");
        if (result.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(result.trim());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected List<Commit> loadBeans(int startIndex, int count) {
        // git log --skip=10 --max-count=20 --pretty=format:"%h|%ad|%cd|%an|%cn|%s%d" --date=iso master

        // git log --pretty=format:"%h|%ad|%an|%cd|%cn|%s%d" --date=iso master

        final String result = Shell.execute(
                "git log --skip=" + startIndex
                        + " --max-count=" + count + " --pretty=format:\"%h|%ad|%cd|%an|%cn|%s%d\" --date=iso " + range + " --");

        final String[] lines = result.split("\n");
        final ArrayList<Commit> commits = new ArrayList<Commit>();
        for (final String line : lines) {
            /*if (line.length() < 8 || line.charAt(7) != '|') {
                continue;
            }*/
            final String[] parts = line.split("\\|");

            final String hash;
            if (parts.length >= 1) {
                hash = parts[0];
            } else {
                hash = "";
            }

            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");
            final Date authorDate;
            if (parts.length >= 2) {
                DateTime dateTime = fmt.parseDateTime(parts[1]);
                authorDate = dateTime.toLocalDateTime().toDate();
            } else {
                authorDate = new Date(0L);
            }

            final Date committerDate;
            if (parts.length >= 3) {
                DateTime dateTime = fmt.parseDateTime(parts[2]);
                committerDate = dateTime.toLocalDateTime().toDate();
            } else {
                committerDate = new Date(0L);
            }

            final String author;
            if (parts.length >= 4) {
                author = parts[3];
            } else {
                author = "";
            }

            final String committer;
            if (parts.length >= 5) {
                committer = parts[4];
            } else {
                committer = "";
            }

            final String subject;
            if (parts.length >= 6) {
                subject = parts[5];
            } else {
                subject = "";
            }

            commits.add(new Commit(authorDate, author, committerDate, committer, hash, subject));
        }

        return commits;
    }

    @Override
    protected void saveBeans(List<Commit> commits, List<Commit> commits2, List<Commit> commits3) {
        throw new UnsupportedOperationException();
    }
}
