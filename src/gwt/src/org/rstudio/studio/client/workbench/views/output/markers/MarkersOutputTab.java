/*
 * MarkersOutputTab.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.markers;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.output.markers.events.ShowMarkersEvent;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

public class MarkersOutputTab extends DelayLoadWorkbenchTab<MarkersOutputPresenter>
{
   public abstract static class Shim 
            extends DelayLoadTabShim<MarkersOutputPresenter, MarkersOutputTab>
            implements ShowMarkersEvent.Handler
   {
      abstract void initialize(MarkersState state);
      abstract void onDismiss();
   }

   static interface Binder extends CommandBinder<Commands, Shim>
   {}

   @Inject
   public MarkersOutputTab(final Shim shim,
                          EventBus events,
                          Commands commands,
                          final Session session)
   {
      super("Markers", shim);
      shim_ = shim;

      events.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         @Override
         public void onSessionInit(SessionInitEvent sie)
         {
            MarkersState state = session.getSessionInfo().getMarkersState();
            shim.initialize(state);
         }
      });

      GWT.<Binder>create(Binder.class).bind(commands, shim);
      
      events.addHandler(ShowMarkersEvent.TYPE, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }
   
   public void onDismiss()
   {
      shim_.onDismiss();
   }
   
   private final Shim shim_;
}