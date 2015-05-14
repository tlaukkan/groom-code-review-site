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
package org.groom.module;

import org.bubblecloud.ilves.security.DefaultRoles;
import org.bubblecloud.ilves.site.*;
import org.bubblecloud.ilves.site.view.valo.DefaultValoView;
import org.groom.module.flows.LogFlowViewlet;
import org.groom.module.flows.admin.RepositoryFlowViewlet;
import org.groom.module.flows.admin.ReviewFlowViewlet;
import org.groom.module.flows.reviewer.DashboardViewlet;

/**
 * Customer module adds support for code reviews.
 *
 * @author Tommi S.E. Laukkanen
 */
public class GroomModule implements SiteModule {

    @Override
    public void initialize() {
        final SiteDescriptor siteDescriptor = DefaultSiteUI.getContentProvider().getSiteDescriptor();

        GroomFields.initialize();

        final NavigationVersion navigationVersion = siteDescriptor.getNavigation().getProductionVersion();

        navigationVersion.addRootPage("login", "groom");
        final ViewDescriptor groom = new ViewDescriptor("groom", "Groom", DefaultValoView.class);
        groom.setViewerRoles(DefaultRoles.ADMINISTRATOR, DefaultRoles.USER);
        siteDescriptor.getViewDescriptors().add(groom);

        navigationVersion.addChildPage("groom", "groom", "dashboard");
        final ViewDescriptor dashboard = new ViewDescriptor("dashboard", "Dashboard", DefaultValoView.class);
        dashboard.setViewerRoles(DefaultRoles.ADMINISTRATOR, DefaultRoles.USER);
        dashboard.setViewletClass("content", DashboardViewlet.class);
        siteDescriptor.getViewDescriptors().add(dashboard);

        navigationVersion.addChildPage("groom", "dashboard", "log");
        final ViewDescriptor log = new ViewDescriptor("log", "Log", DefaultValoView.class);
        log.setViewerRoles(DefaultRoles.ADMINISTRATOR);
        log.setViewletClass("content", LogFlowViewlet.class);
        siteDescriptor.getViewDescriptors().add(log);

        navigationVersion.addChildPage("groom", "log", "reviews");
        final ViewDescriptor reviews = new ViewDescriptor("reviews", "Reviews", DefaultValoView.class);
        reviews.setViewerRoles(DefaultRoles.ADMINISTRATOR);
        reviews.setViewletClass("content", ReviewFlowViewlet.class);
        siteDescriptor.getViewDescriptors().add(reviews);

        navigationVersion.addChildPage("groom", "reviews", "repositories");
        final ViewDescriptor repositories = new ViewDescriptor("repositories", "Repositories", DefaultValoView.class);
        repositories.setViewerRoles(DefaultRoles.ADMINISTRATOR);
        repositories.setViewletClass("content", RepositoryFlowViewlet.class);
        siteDescriptor.getViewDescriptors().add(repositories);
    }

    @Override
    public void injectDynamicContent(final SiteDescriptor dynamicSiteDescriptor) {
    }
}
