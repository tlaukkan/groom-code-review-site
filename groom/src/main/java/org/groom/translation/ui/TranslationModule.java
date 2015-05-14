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

import org.bubblecloud.ilves.security.DefaultRoles;
import org.bubblecloud.ilves.site.*;
import org.bubblecloud.ilves.site.view.valo.DefaultValoView;
import org.groom.translation.model.EntryFlowViewlet;

/**
 * Customer module adds support for code reviews.
 *
 * @author Tommi S.E. Laukkanen
 */
public class TranslationModule implements SiteModule {

    @Override
    public void initialize() {
        final SiteDescriptor siteDescriptor = DefaultSiteUI.getContentProvider().getSiteDescriptor();

        TranslationFields.initialize();

        final NavigationVersion navigationVersion = siteDescriptor.getNavigation().getProductionVersion();

        navigationVersion.addRootPage("review", "translation");
        final ViewDescriptor groom = new ViewDescriptor("translation", "Hoot", DefaultValoView.class);
        groom.setViewerRoles(DefaultRoles.ADMINISTRATOR, DefaultRoles.USER);
        siteDescriptor.getViewDescriptors().add(groom);

        navigationVersion.addChildPage("translation", "translate");
        final ViewDescriptor translate = new ViewDescriptor("translate", DefaultValoView.class);
        translate.setViewerRoles("administrator");
        siteDescriptor.getViewDescriptors().add(translate);
        translate.setViewletClass(Slot.CONTENT, EntryFlowViewlet.class);
        siteDescriptor.getViewDescriptors().add(translate);
    }

    @Override
    public void injectDynamicContent(final SiteDescriptor dynamicSiteDescriptor) {
    }
}
