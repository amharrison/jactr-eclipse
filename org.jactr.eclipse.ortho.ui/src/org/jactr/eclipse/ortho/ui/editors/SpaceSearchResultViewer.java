package org.jactr.eclipse.ortho.ui.editors;

/*
 * default logging
 */
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jactr.eclipse.ortho.ui.editors.pages.NewImageViewerPage;
import org.jactr.eclipse.ortho.ui.editors.pages.SummaryPage;
import org.jactr.eclipse.ortho.ui.model.ISpaceSearch;
import org.jactr.eclipse.ortho.ui.model.SpaceSearch;
import org.jactr.eclipse.ortho.ui.parser.SpaceSearchParser;
import org.jactr.tools.itr.ortho.ISliceAnalysis;
import org.jactr.tools.itr.ortho.impl.SliceAnalysis;

public class SpaceSearchResultViewer extends FormEditor
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER            = LogFactory
                                                           .getLog(SpaceSearchResultViewer.class);

  private SpaceSearch                _spaceSearch;

  private ImageRegistry              _imageRegistry;

  private IResourceChangeListener    _resourceListener = new IResourceChangeListener() {

                                                         public void resourceChanged(
                                                             IResourceChangeEvent event)
                                                         {
                                                           IResource resource = getResource();
                                                           if (resource == null)
                                                             return;
                                                           if (resource
                                                               .equals(event
                                                                   .getResource()))
                                                             close(false);
                                                         }

                                                       };

  private SummaryPage                _summaryPage;

  @Override
  protected FormToolkit createToolkit(Display display)
  {
    _imageRegistry = new ImageRegistry();
    return new FormToolkit(org.jactr.eclipse.ortho.ui.Activator.getDefault()
        .getFormColors());
  }

  @Override
  protected void addPages()
  {
    try
    {
      addPage(_summaryPage = new SummaryPage(this));

    }
    catch (PartInitException e)
    {

    }
  }

  @Override
  public void dispose()
  {
    /*
     * make sure we dispose of the image resources of any imageViewers
     */
    for (int i = 0; i < getPageCount(); i++)
    {
      IEditorPart editor = getEditor(i);
      if (editor instanceof NewImageViewerPage)
        ((NewImageViewerPage) editor).clearContent();
    }

    ResourcesPlugin.getWorkspace().removeResourceChangeListener(
        _resourceListener);
    _imageRegistry.dispose();
    super.dispose();
  }

  @Override
  public void doSave(IProgressMonitor monitor)
  {
    IFile file = (IFile) getResource();

    commitPages(true);
    _summaryPage.doSave(monitor);
    _summaryPage.reset();

    try
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.print("<analyses date=\"");
      pw.print(DateFormat
          .getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(
              new Date(_spaceSearch.getTimestamp())));
      pw.print("\" name=\"");
      pw.print(_spaceSearch.getName());
      pw.println("\">");
      pw.println("");

      /*
       * dump all the completed analyses
       */
      for (ISliceAnalysis analysis : _spaceSearch.getSliceAnalyses())
      {
        ((SliceAnalysis) analysis).write(pw);
        pw.println();
      }

      /*
       * dump the rest of the file
       */
      pw.println("</analyses>");
      pw.close();

      file.setContents(new ByteArrayInputStream(sw.toString().getBytes()),
          IResource.FORCE | IResource.KEEP_HISTORY, monitor);
    }
    catch (Exception e)
    {
      LOGGER.error("Failed to save ", e);
    }
  }

  @Override
  public void doSaveAs()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isSaveAsAllowed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  public void show(ISliceAnalysis... analyses)
  {
    for (ISliceAnalysis analysis : analyses)
      show(analysis);
  }

  public void show(ISliceAnalysis analysis)
  {
    String id = NewImageViewerPage.getID(analysis);
    NewImageViewerPage page = (NewImageViewerPage) findPage(id);
    if (page == null)
    {
      page = new NewImageViewerPage(this, analysis, _spaceSearch.getURI()
          .resolve(
          analysis.getSlice().getId() + "/"));
      try
      {
        addPage(page);
      }
      catch (PartInitException e)
      {
        // TODO Auto-generated catch block
        LOGGER.error("SpaceSearchResultViewer.show threw PartInitException : ",
            e);
        return;
      }
    }


    setActivePage(id);
  }

  public void hideImageViewer(FormPage page)
  {
    int index = page.getIndex();
    if (page instanceof NewImageViewerPage)
      ((NewImageViewerPage) page).clearContent();

    removePage(index);
  }

  public ISpaceSearch getSpaceSearch()
  {
    return _spaceSearch;
  }

  public ImageRegistry getImageRegistry()
  {
    return _imageRegistry;
  }

  private IResource getResource()
  {
    if (getEditorInput() instanceof IFileEditorInput)
      return ((IFileEditorInput) getEditorInput()).getFile();
    return null;
  }

  @Override
  protected void setInput(IEditorInput input)
  {
    if (input instanceof IURIEditorInput)
      try
      {
        SpaceSearchParser parser = new SpaceSearchParser();
        _spaceSearch = parser.parse(((IURIEditorInput) input).getURI());
        setPartName(_spaceSearch.getName());

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG,
            DateFormat.LONG);

        setContentDescription(String.format("%s (%d slices)", format
            .format(new Date(_spaceSearch.getTimestamp())), _spaceSearch
            .getNumberOfSlices()));
      }
      catch (Exception e)
      {
        _spaceSearch = null;
      }

    if (input instanceof IFileEditorInput)
      ResourcesPlugin.getWorkspace().addResourceChangeListener(
          _resourceListener,
          IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

    super.setInput(input);
  }

}
