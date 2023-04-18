package org.jactr.eclipse.association.ui.handler;

/*
 * default logging
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.jactr.eclipse.association.ui.views.AssociationViewer2;
import org.jactr.eclipse.core.CorePlugin;
import org.jactr.eclipse.ui.editor.ACTRModelEditor;
import org.jactr.io.antlr3.builder.JACTRBuilder;

public class VisualizeHandler extends AbstractHandler
{
  static private final transient Log LOGGER = LogFactory
      .getLog(VisualizeHandler.class);

  static public final String         ALL    = "org.jactr.eclipse.association.command.visualize.all";

  static public final String         IN     = "org.jactr.eclipse.association.command.visualize.radial.in";

  public Object execute(ExecutionEvent event) throws ExecutionException
  {
    String type = event.getCommand().getId();
    AssociationViewer2 viewer = openViewer();
    IEditorPart editor = HandlerUtil.getActiveEditor(event);

    if (LOGGER.isDebugEnabled()) LOGGER.debug("Executing " + type);

    if (editor instanceof ACTRModelEditor)
    {
      if (ALL.equals(type))
        viewer.viewAll((ACTRModelEditor) editor);
      else
        viewer.view((ACTRModelEditor) editor,
            ((ACTRModelEditor) editor).getNearestAST(JACTRBuilder.CHUNK));
    }
    else
    {
      // xtext editor for new io
      XtextEditor xeditor = (XtextEditor) editor;
      viewer.viewAll(xeditor);
    }

    return null;
  }

  protected AssociationViewer2 openViewer()
  {
    try
    {
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
          .getActivePage();
      AssociationViewer2 viewer = (AssociationViewer2) page
          .showView(AssociationViewer2.VIEW_ID);
      return viewer;
    }
    catch (PartInitException e)
    {
      CorePlugin.error("VisualizeHandler.openViewer threw PartInitException : ",
          e);
      return null;
    }
  }
}
