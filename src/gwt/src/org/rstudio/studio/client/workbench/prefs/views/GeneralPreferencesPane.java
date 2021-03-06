/*
 * GeneralPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.GeneralPrefs;
import org.rstudio.studio.client.workbench.prefs.model.HistoryPrefs;
import org.rstudio.studio.client.workbench.prefs.model.ProjectsPrefs;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 UIPrefs prefs,
                                 Session session,
                                 final GlobalDisplay globalDisplay,
                                 WorkbenchContext context)
   {
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      prefs_ = prefs;
      session_ = session;
      
      RVersionsInfo versionsInfo = context.getRVersionsInfo();

      if (Desktop.isDesktop())
      {
         if (Desktop.getFrame().canChooseRVersion())
         {
            rVersion_ = new TextBoxWithButton(
                  "R version:",
                  "Change...",
                  new ClickHandler()
                  {
                     public void onClick(ClickEvent event)
                     {
                        String ver = Desktop.getFrame().chooseRVersion();
                        if (!StringUtil.isNullOrEmpty(ver))
                        {
                           rVersion_.setText(ver);

                           globalDisplay.showMessage(MessageDialog.INFO,
                                 "Change R Version",
                                 "You need to quit and re-open RStudio " +
                                 "in order for this change to take effect.");
                        }
                     }
                  });
            rVersion_.setWidth("100%");
            rVersion_.setText(Desktop.getFrame().getRVersion());
            spaced(rVersion_);
            add(rVersion_);
         }
      }
      else if (versionsInfo.isMultiVersion())
      {
         rServerRVersion_ = new RVersionSelectWidget(
                                       versionsInfo.getAvailableRVersions());
         add(tight(rServerRVersion_));
         
         rememberRVersionForProjects_ = 
                        new CheckBox("Restore last used R version for projects");
         
         rememberRVersionForProjects_.setValue(true);
         Style style = rememberRVersionForProjects_.getElement().getStyle();
         style.setMarginTop(5, Unit.PX);
         style.setMarginBottom(12, Unit.PX);
         add(rememberRVersionForProjects_);
      }

      Label defaultLabel = new Label("Default working directory (when not in a project):");
      nudgeRight(defaultLabel);
      add(tight(defaultLabel));
      add(dirChooser_ = new DirectoryChooserTextBox(null, 
                                                    null,
                                                    fileDialogs_, 
                                                    fsContext_));  
      spaced(dirChooser_);
      nudgeRight(dirChooser_);
      textBoxWithChooser(dirChooser_);

      restoreLastProject_ = new CheckBox("Restore most recently opened project at startup");
      lessSpaced(restoreLastProject_);
      add(restoreLastProject_);
      
      add(checkboxPref("Restore previously open source documents at startup", prefs_.restoreSourceDocuments()));
        
      add(loadRData_ = new CheckBox("Restore .RData into workspace at startup"));
      lessSpaced(loadRData_); 
      
      saveWorkspace_ = new SelectWidget(
            "Save workspace to .RData on exit:",
            new String[] {
                  "Always",
                  "Never",
                  "Ask"
            });
      spaced(saveWorkspace_);
      add(saveWorkspace_);
      
      alwaysSaveHistory_ = new CheckBox(
            "Always save history (even when not saving .RData)");
      lessSpaced(alwaysSaveHistory_);
      add(alwaysSaveHistory_);
      
      removeHistoryDuplicates_ = new CheckBox(
                                 "Remove duplicate entries in history");
      spaced(removeHistoryDuplicates_);
      add(removeHistoryDuplicates_);

      showLastDotValue_ = new CheckBox("Show .Last.value in environment listing");
      lessSpaced(showLastDotValue_);
      add(showLastDotValue_);
      
      rProfileOnResume_ = new CheckBox("Run Rprofile when resuming suspended session");
      spaced(rProfileOnResume_);
      if (!Desktop.isDesktop())
         add(rProfileOnResume_);
           
      // The error handler features require source references; if this R
      // version doesn't support them, don't show these options. 
      if (session_.getSessionInfo().getHaveSrcrefAttribute())
      {
         add(checkboxPref(
               "Use debug error handler only when my code contains errors", 
               prefs_.handleErrorsInUserCodeOnly()));
         CheckBox chkTracebacks = checkboxPref(
               "Automatically expand tracebacks in error inspector", 
               prefs_.autoExpandErrorTracebacks());
         chkTracebacks.getElement().getStyle().setMarginBottom(15, Unit.PX);
         add(chkTracebacks);
      }
      
      // provide check for updates option in desktop mode when not
      // already globally disabled
      if (Desktop.isDesktop() && 
          !session.getSessionInfo().getDisableCheckForUpdates())
      {
         add(checkboxPref("Automatically notify me of updates to RStudio",
                          prefs_.checkForUpdates()));
      }
      
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
      rProfileOnResume_.setEnabled(false);
      showLastDotValue_.setEnabled(false);
      restoreLastProject_.setEnabled(false);
   }
   
   @Override
   protected void initialize(RPrefs rPrefs)
   {
      // general prefs
      GeneralPrefs generalPrefs = rPrefs.getGeneralPrefs();
      
      saveWorkspace_.setEnabled(true);
      loadRData_.setEnabled(true);
      dirChooser_.setEnabled(true);
      
      int saveWorkspaceIndex;
      switch (generalPrefs.getSaveAction())
      {
         case SaveAction.NOSAVE: 
            saveWorkspaceIndex = 1; 
            break;
         case SaveAction.SAVE: 
            saveWorkspaceIndex = 0; 
            break; 
         case SaveAction.SAVEASK:
         default: 
            saveWorkspaceIndex = 2; 
            break; 
      }
      saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

      loadRData_.setValue(generalPrefs.getLoadRData());
      dirChooser_.setText(generalPrefs.getInitialWorkingDirectory());
        
      // history prefs
      HistoryPrefs historyPrefs = rPrefs.getHistoryPrefs();
      
      alwaysSaveHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);
      
      alwaysSaveHistory_.setValue(historyPrefs.getAlwaysSave());
      removeHistoryDuplicates_.setValue(historyPrefs.getRemoveDuplicates());
      
      rProfileOnResume_.setValue(generalPrefs.getRprofileOnResume());
      rProfileOnResume_.setEnabled(true);
      
      showLastDotValue_.setValue(generalPrefs.getShowLastDotValue());
      showLastDotValue_.setEnabled(true);
      
      if (rServerRVersion_ != null)
         rServerRVersion_.setRVersion(generalPrefs.getDefaultRVersion());
      
      if (rememberRVersionForProjects_ != null)
      {
         rememberRVersionForProjects_.setValue(
                                   generalPrefs.getRestoreProjectRVersion()); 
      }
     
      // projects prefs
      ProjectsPrefs projectsPrefs = rPrefs.getProjectsPrefs();
      restoreLastProject_.setEnabled(true);
      restoreLastProject_.setValue(projectsPrefs.getRestoreLastProject());
   }
   

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconR();
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);
 
      if (saveWorkspace_.isEnabled())
      {
         int saveAction;
         switch (saveWorkspace_.getListBox().getSelectedIndex())
         {
            case 0: 
               saveAction = SaveAction.SAVE; 
               break; 
            case 1: 
               saveAction = SaveAction.NOSAVE; 
               break; 
            case 2:
            default: 
               saveAction = SaveAction.SAVEASK; 
               break; 
         }

         // set general prefs
         GeneralPrefs generalPrefs = GeneralPrefs.create(saveAction, 
                                                         loadRData_.getValue(),
                                                         rProfileOnResume_.getValue(),
                                                         dirChooser_.getText(),
                                                         getDefaultRVersion(),
                                                         getRestoreProjectRVersion(),
                                                         showLastDotValue_.getValue());
         rPrefs.setGeneralPrefs(generalPrefs);
         
         // set history prefs
         HistoryPrefs historyPrefs = HistoryPrefs.create(
                                          alwaysSaveHistory_.getValue(),
                                          removeHistoryDuplicates_.getValue());
         rPrefs.setHistoryPrefs(historyPrefs);
         
         
         // set projects prefs
         ProjectsPrefs projectsPrefs = ProjectsPrefs.create(
                                             restoreLastProject_.getValue());
         rPrefs.setProjectsPrefs(projectsPrefs);
      }

      return restartRequired;
   }

   @Override
   public String getName()
   {
      return "General";
   }

  
   
   private RVersionSpec getDefaultRVersion()
   {
      if (rServerRVersion_ != null)
         return rServerRVersion_.getRVersion();
      else
         return RVersionSpec.createEmpty();
   }
   
   private boolean getRestoreProjectRVersion()
   {
      if (rememberRVersionForProjects_ != null)
         return rememberRVersionForProjects_.getValue();
      else
         return false;
   }
   
    

   private static class RVersionSelectWidget extends SelectWidget
   {
      public RVersionSelectWidget(JsArray<RVersionSpec> rVersions)
      {
         super("R version for new sessions:",
               rVersionChoices(rVersions),
               rVersionValues(rVersions),
               false, 
               true, 
               false);
         
         HelpButton.addHelpButton(this, "multiple_r_versions");
      }
      
      public void setRVersion(RVersionSpec version)
      {
         if (!setValue(rVersionSpecToString(version)))
            setValue(rVersionSpecToString(RVersionSpec.createEmpty()));
      }
      
      public RVersionSpec getRVersion()
      {
         return rVersionSpecFromString(getValue());
      }
      
      
      private static String[] rVersionChoices(JsArray<RVersionSpec> rVersions)
      {
         // do we need to disambiguate identical version numbers
         boolean disambiguate = RVersionSpec.hasDuplicates(rVersions);

         // build list of choices
         ArrayList<String> choices = new ArrayList<String>();

         // always include "default" lable
         choices.add(USE_DEFAULT_VERSION);

         for (int i=0; i<rVersions.length(); i++)
         {
            RVersionSpec version = rVersions.get(i);
            String choice = "R version " + version.getVersion();
            if (disambiguate)
               choice = choice + " (" + version.getRHome() + ")";
            choices.add(choice);
         }

         return choices.toArray(new String[0]);
      }

      private static String[] rVersionValues(JsArray<RVersionSpec> rVersions)
      {
         ArrayList<String> values = new ArrayList<String>();

         values.add(rVersionSpecToString(RVersionSpec.createEmpty()));

         for (int i=0; i<rVersions.length(); i++)
            values.add(rVersionSpecToString(rVersions.get(i)));

         return values.toArray(new String[0]);
      }
      
      private static RVersionSpec rVersionSpecFromString(String str)
      {
         if (str != null)
         {
            int loc = str.indexOf(SEP);
            if (loc != -1)
            {
               String version = str.substring(0, loc);
               String rHomeDir = str.substring(loc + SEP.length());
               if (version.length() > 0 && rHomeDir.length() > 0)
                  return RVersionSpec.create(version, rHomeDir);
            }
         }
         
         // couldn't parse it
         return RVersionSpec.createEmpty();
      }
      
      private static String rVersionSpecToString(RVersionSpec version)
      {
         if (version.getVersion().length() == 0)
            return "";
         else
            return version.getVersion() + SEP + version.getRHome();
      }

      private final static String USE_DEFAULT_VERSION = "(Use System Default)";
      private final static String SEP = "::::";
   }

   
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private RVersionSelectWidget rServerRVersion_ = null;
   private CheckBox rememberRVersionForProjects_ = null;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox removeHistoryDuplicates_;
   private CheckBox restoreLastProject_;
   private CheckBox rProfileOnResume_;
   private CheckBox showLastDotValue_;
   private final UIPrefs prefs_;
   private final Session session_;
}
