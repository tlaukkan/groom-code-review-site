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
package org.groom.translation.service;

import org.apache.log4j.Logger;
import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.model.Group;
import org.bubblecloud.ilves.model.User;
import org.bubblecloud.ilves.security.CompanyDao;
import org.bubblecloud.ilves.security.UserDao;
import org.bubblecloud.ilves.util.EmailUtil;
import org.bubblecloud.ilves.util.PropertiesUtil;
import org.groom.model.Repository;
import org.groom.review.dao.ReviewDao;
import org.groom.shell.Shell;
import org.groom.translation.model.Entry;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class which synchronizes bundles to database and back.
 *
 * @author Tommi S.E. Laukkanen
 */
public class TranslationSynchronizer {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(TranslationSynchronizer.class);

    /**
     * The entity manager.
     */
    private final EntityManager entityManager;
    /**
     * Synchronization thread.
     */
    private final Thread thread;
    /**
     * Shutdown requested.
     */
    private boolean shutdown = false;

    private static long lastTimeMillis;

    public static void startSynchronize() {
        TranslationSynchronizer.lastTimeMillis = 0;
    }

    /**
     * Constructor which starts synchronizer.
     *
     * @param entityManager the entity manager.
     */

    public TranslationSynchronizer(final EntityManager entityManager) {
        this.entityManager = entityManager;

        final long synchronizePeriodMillis = Long.parseLong(PropertiesUtil.getProperty("hoot",
                "synchronize-period-millis"));

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lastTimeMillis = System.currentTimeMillis();
                lastTimeMillis = lastTimeMillis - lastTimeMillis % synchronizePeriodMillis;

                while (!shutdown) {

                    synchronize();

                    while (System.currentTimeMillis() < lastTimeMillis + synchronizePeriodMillis) {
                        try {
                            Thread.sleep(100);
                        } catch (final InterruptedException e) {
                            LOGGER.debug(e);
                            if (shutdown) {
                                return;
                            }
                        }
                    }
                    lastTimeMillis = System.currentTimeMillis();
                    lastTimeMillis = lastTimeMillis - lastTimeMillis % synchronizePeriodMillis;

                }
            }
        });
        thread.start();
    }

    /**
     * Synchronizes bundles and database.
     */
    private void synchronize() {
        entityManager.clear();

        final List<Company> companies = CompanyDao.getCompanies(entityManager);
        for (final Company company : companies) {
            final List<Repository> repositories = ReviewDao.getRepositories(entityManager, company);
            for (final Repository repository : repositories) {
                synchronizeRepository(repository);
            }
        }

    }

    /**
     * Synchronizes bundles and database for given repository.
     */
    private void synchronizeRepository(final Repository repository) {
        final String absoluteRepositoryPathPrefix = PropertiesUtil.getProperty("groom", "repository-path") + "/translation";

        final String relativeRepositoryPath = "translation/" + repository.getPath();

        if (!new File(absoluteRepositoryPathPrefix).exists()) {
            new File(absoluteRepositoryPathPrefix).mkdir();
            LOGGER.info(Shell.execute("git clone " + repository.getUrl() + " " + repository.getPath(), "translation"));
        }
        LOGGER.info(Shell.execute("git fetch", relativeRepositoryPath));
        LOGGER.info(Shell.execute("git reset --hard", relativeRepositoryPath));

        final String absoluteRepositoryPath = absoluteRepositoryPathPrefix + "/" + repository.getPath();
        final String bundleCharacterSet = PropertiesUtil.getProperty("hoot", "bundle-character-set");
        final String[] prefixes = repository.getBundlePrefixes().split(",");

        final Company company = repository.getOwner();
        for (final String prefix : prefixes) {
            final File baseBundle = new File(absoluteRepositoryPath + "/" + prefix + ".properties");
            if (!baseBundle.exists()) {
                LOGGER.info("Base bundle does not exist: " + baseBundle.getAbsolutePath());
                continue;
            }

            LOGGER.info("Base bundle exists: " + baseBundle.getAbsolutePath());
            String baseName = baseBundle.getName().substring(0, baseBundle.getName().length() - 11);
            if (baseName.indexOf('_') >= 0) {
                baseName = baseName.substring(0, baseName.indexOf('_'));
            }

            final File bundleDirectory = baseBundle.getParentFile();
            final String bundleDirectoryPath = bundleDirectory.getAbsolutePath();

            LOGGER.info("Basename: " + baseName);
            LOGGER.info("Path: " + bundleDirectoryPath);

            final Set<Object> keys;
            final Properties baseBundleProperties;
            try {
                baseBundleProperties = new Properties();
                final FileInputStream baseBundleInputStream = new FileInputStream(baseBundle);
                baseBundleProperties.load(new InputStreamReader(baseBundleInputStream, bundleCharacterSet));
                keys = baseBundleProperties.keySet();
                baseBundleInputStream.close();
            } catch (Exception e) {
                LOGGER.error("Error reading bundle: " + baseName, e);
                continue;
            }

            final Map<String, List<String>> missingKeys = new HashMap<String, List<String>>();
            final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

            for (final File candidate : bundleDirectory.listFiles()) {
                if (candidate.getName().startsWith(baseName) && candidate.getName().endsWith(".properties")) {

                    final String name = candidate.getName().split("\\.")[0];
                    final String[] parts = name.split("_");

                    String candidateBaseName = parts[0];
                    if (candidateBaseName.equals(baseName)) {
                        String language = "";
                        String country = "";
                        if (parts.length > 1) {
                            language = parts [1];
                            if (parts.length > 2) {
                                country = parts[2];
                            }
                        }

                        LOGGER.info("Bundle basename: '" + candidateBaseName
                                + "' language: '" + language + "' country: '" + country + "'");

                        entityManager.getTransaction().begin();
                        try {
                            final Properties properties = new Properties();
                            final FileInputStream bundleInputStream = new FileInputStream(candidate);
                            properties.load(new InputStreamReader(bundleInputStream, bundleCharacterSet));
                            bundleInputStream.close();

                            final TypedQuery<Entry> query = entityManager.createQuery("select e from Entry as e where " +
                                    "e.path=:path and e.basename=:basename and " +
                                    "e.language=:language and e.country=:country order by e.key", Entry.class);
                            query.setParameter("path", bundleDirectoryPath);
                            query.setParameter("basename", baseName);
                            query.setParameter("language", language);
                            query.setParameter("country", country);
                            final List<Entry> entries = query.getResultList();
                            final Set<String> existingKeys = new HashSet<String>();

                            for (final Entry entry : entries) {
                                if (keys.contains(entry.getKey())) {
                                    if (entry.getValue().length() == 0 && properties.containsKey(entry.getKey()) &&
                                            ((String) properties.get(entry.getKey())).length() > 0) {
                                        entry.setValue((String) properties.get(entry.getKey()));
                                        entityManager.persist(entry);
                                    }

                                }
                                existingKeys.add(entry.getKey());
                            }

                            for (final Object obj : keys) {
                                final String key = (String) obj;

                                final String value;
                                if (properties.containsKey(key)) {
                                    value = (String) properties.get(key);
                                } else {
                                    value = "";
                                }

                                if (!existingKeys.contains(key)) {
                                    final Entry entry = new Entry();
                                    entry.setOwner(company);
                                    entry.setRepository(repository);
                                    entry.setPath(bundleDirectoryPath);
                                    entry.setBasename(baseName);
                                    entry.setLanguage(language);
                                    entry.setCountry(country);
                                    entry.setKey(key);
                                    entry.setValue(value);
                                    entry.setCreated(new Date());
                                    entry.setModified(entry.getCreated());
                                    entityManager.persist(entry);

                                    final String locale = entry.getLanguage() + "_" + entry.getCountry();

                                    if (!missingKeys.containsKey(locale)) {
                                        missingKeys.put(locale, new ArrayList<String>());
                                    }

                                    missingKeys.get(locale).add(entry.getKey());
                                }

                            }
                            entityManager.getTransaction().commit();

                            final FileOutputStream fileOutputStream = new FileOutputStream(candidate, false);
                            final OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream,
                                    bundleCharacterSet);
                            final PrintWriter printWriter = new PrintWriter(writer);

                            for (final Entry entry : query.getResultList()) {
                                if (entry.getDeleted() == null) {
                                    printWriter.print("# Modified: ");
                                    printWriter.print(format.format(entry.getModified()));
                                    if (entry.getAuthor() != null) {
                                        printWriter.print(" Author: ");
                                        printWriter.print(entry.getAuthor());
                                    }
                                    printWriter.println();
                                    printWriter.print(entry.getKey());
                                    printWriter.print("=");
                                    final String value = entry.getValue().replace("\n", "\\n");
                                    printWriter.println(value);
                                }
                            }

                            printWriter.flush();
                            printWriter.close();
                            fileOutputStream.close();
                        } catch (Exception e) {
                            if (entityManager.getTransaction().isActive()) {
                                entityManager.getTransaction().rollback();
                            }
                            LOGGER.error("Error reading bundle: " + baseName, e);
                            continue;
                        }
                    }
                }
            }

            for (final String locale : missingKeys.keySet()) {
                final List<String> keySet = missingKeys.get(locale);

                final String subject = "Please translate " + locale;
                String content = "Missing keys are: ";
                for (final String key : keySet) {
                    content += key + "\n";
                }

                final Group group = UserDao.getGroup(entityManager, company, locale);

                if (group != null) {
                    final List<User> users = UserDao.getGroupMembers(entityManager, company, group);
                    for (final User user : users) {
                        LOGGER.info("Sending translation request to " + user.getEmailAddress() + " for " + locale +
                                " keys " + keySet);
                        EmailUtil.send(user.getEmailAddress(), company.getSupportEmailAddress(), subject, content);
                    }
                }
            }


        }

        LOGGER.info(Shell.execute("git commit -a -m 'Translations.'", relativeRepositoryPath));
        LOGGER.info(Shell.execute("git push origin master", relativeRepositoryPath));
    }

    /**
     * Executes requested shell command.
     *
     * @param cmd the shell command to execute
     */
    private void executeShellCommand(final String cmd) {
        LOGGER.debug("Executing shell command: " + cmd);
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(new String[] {"/bin/sh", "-c", cmd});
            process.waitFor();

            String line;

            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = error.readLine()) != null){
                LOGGER.error(line);
            }
            error.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null){
                LOGGER.info(line);
            }

            input.close();

            LOGGER.debug("Executed shell command: " + cmd);
        } catch (final Throwable t) {
            LOGGER.error("Error executing shell command: " + cmd, t);
        }
    }

    /**
     * Shutdown.
     */
    public final void shutdown() {
        shutdown = true;
        try {
            thread.interrupt();
            thread.join();
        } catch (final InterruptedException e) {
            LOGGER.debug(e);
        }
    }

}
