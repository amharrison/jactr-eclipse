package org.jactr.eclipse.ortho.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jactr.eclipse.runtime.launching.ACTRLaunchConfigurationUtils;
import org.jactr.eclipse.runtime.launching.iterative.IterativeSession;
import org.jactr.eclipse.runtime.launching.session.ISessionListener;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin
{

  // The plug-in ID
  public static final String                 PLUGIN_ID = "org.jactr.eclipse.ortho.ui";

  // The shared instance
  private static Activator                   plugin;

  private FormColors                         _formColors;

  private ISessionListener<IterativeSession> _iterativeSessionListener;

  /**
   * The constructor
   */
  public Activator()
  {
  }

  /*
   * (non-Javadoc)
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void start(BundleContext context) throws Exception
  {
    super.start(context);
    plugin = this;
    _formColors = new FormColors(PlatformUI.getWorkbench().getDisplay());
    _formColors.markShared();

    _iterativeSessionListener = new ISessionListener<IterativeSession>() {

      public void sessionClosed(IterativeSession session, boolean normal)
      {
        /*
         * look for report/report.orthoxml
         */
        IPath root = session.getRelativeWorkingDirectory();
        final IPath path = root.append("report").append("report.orthoxml");
        String projectName = "unknown";
        try
        {
          IProject project = ACTRLaunchConfigurationUtils.getProject(session
              .getConfiguration());

          /*
           * it is possible to listen to a session that is not in the workspace
           */
          if (project == null) return;

          projectName = project.getName();

          /*
           * refresh so we can find it..
           */
          project.getFolder(root).refreshLocal(IResource.DEPTH_INFINITE,
              new NullProgressMonitor());

          final IFile file = project.getFile(path);

          if (file.exists())
          {
            Runnable runner = new Runnable() {
              public void run()
              {
                /*
                 * open it up
                 */
                try
                {
                  IWorkbenchPage page = PlatformUI.getWorkbench()
                      .getActiveWorkbenchWindow().getActivePage();
                  IEditorRegistry registry = PlatformUI.getWorkbench()
                      .getEditorRegistry();
                  IEditorDescriptor desc = registry.getDefaultEditor(file
                      .getName());

                  String editorId = IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID;
                  if (desc != null) editorId = desc.getId();

                  IDE.openEditor(page, file, editorId, true);
                }
                catch (Exception e)
                {
                  Activator.getDefault().getLog().log(
                      new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                          "Could not open " + path, e));
                }
              }
            };

            Display.getDefault().asyncExec(runner);
          }
          else
            Activator.getDefault().getLog().log(
                new Status(IStatus.INFO, Activator.PLUGIN_ID, "Could not find "
                    + path + " in " + projectName));
        }
        catch (Exception e)
        {
          Activator.getDefault().getLog().log(
              new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not open "
                  + path, e));
        }
      }

      public void sessionDestroyed(IterativeSession session)
      {

      }

      public void sessionOpened(IterativeSession session)
      {

      }

    };

    IterativeSession.getIterativeSessionTracker().addListener(
        _iterativeSessionListener);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void stop(BundleContext context) throws Exception
  {
    _formColors.dispose();
    _formColors = null;
    plugin = null;
    IterativeSession.getIterativeSessionTracker().removeListener(
        _iterativeSessionListener);
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault()
  {
    return plugin;
  }

  public FormColors getFormColors()
  {
    return _formColors;
  }
}
