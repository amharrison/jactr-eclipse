package org.jactr.eclipse.ortho.ui.editors.pages;

/*
 * default logging
 */
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.progress.UIJob;
import org.jactr.eclipse.ortho.ui.Activator;
import org.jactr.eclipse.ortho.ui.editors.SpaceSearchResultViewer;
import org.jactr.eclipse.ui.images.JACTRImages;
import org.jactr.tools.itr.ortho.ISliceAnalysis;

public class ImageViewerPage extends FormPage
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
                                                .getLog(ImageViewerPage.class);

  static private final String        ID     = ImageViewerPage.class.getName();

  static public String getID(ISliceAnalysis analysis)
  {
    return ID + "." + analysis.getSlice().getId();
  }

  private final ISliceAnalysis _analysis;

  private Tree                 _imageTree;

  private URI                  _root;

  public ImageViewerPage(FormEditor editor, ISliceAnalysis analysis, URI root)
  {
    super(editor, getID(analysis), "Slice " + analysis.getSlice().getId()
        + " images");
    _analysis = analysis;
    _root = root;
  }

  public ISliceAnalysis getSliceAnalysis()
  {
    return _analysis;
  }

  @Override
  protected void createFormContent(IManagedForm managedForm)
  {
    managedForm.getForm().setText("Images for " + _analysis.getSlice().getId());

    /*
     * and a close button
     */
    IToolBarManager mgr = managedForm.getForm().getToolBarManager();
    IAction action = new Action() {
      @Override
      public void run()
      {
        ((SpaceSearchResultViewer) getEditor())
            .hideImageViewer(ImageViewerPage.this);
      }
    };
    action
        .setImageDescriptor(JACTRImages.getImageDescriptor(JACTRImages.CLOSE));
    action.setText("Close");
    action.setToolTipText("Close current image viewer");
    mgr.add(action);

    managedForm.getForm().updateToolBar();

    TableWrapLayout layout = new TableWrapLayout();
    layout.numColumns = 1;
    managedForm.getForm().getBody().setLayout(layout);

    buildDetails(managedForm);
    _imageTree = buildImageViewer(managedForm);

    updateContent();
  }

  protected void buildDetails(IManagedForm managedForm)
  {

  }

  protected Tree buildImageViewer(IManagedForm managedForm)
  {
    Section sliceSection = managedForm.getToolkit().createSection(
        managedForm.getForm().getBody(),
        ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
            | ExpandableComposite.EXPANDED | ExpandableComposite.CLIENT_INDENT);

    sliceSection.setText("Images");

    TableWrapData layout = new TableWrapData(TableWrapData.FILL);
    layout.colspan = 1;
    layout.maxHeight = 400;
    layout.grabVertical = false;
    layout.grabHorizontal = true;

    Tree tree = managedForm.getToolkit().createTree(sliceSection, SWT.V_SCROLL);
    sliceSection.setClient(tree);

    return tree;
  }

  protected URI getImageURI(String relativeURI)
  {
    return _root.resolve(relativeURI);
  }

  protected void updateContent()
  {
    _imageTree.removeAll();

    ArrayList<TreeItem> imageNodes = new ArrayList<TreeItem>();

    for (Map.Entry<String, String> imageInfo : _analysis.getImages().entrySet())
    {
      TreeItem item = new TreeItem(_imageTree, SWT.NONE);
      item.setText(imageInfo.getKey());

      URI imageURI = getImageURI(imageInfo.getValue());
      item.setData(imageURI);

      imageNodes.add(item);
    }

    /*
     * start loading..
     */
    new ImageLoader(imageNodes).schedule(250);
  }

  private class ImageLoader extends UIJob
  {
    private Collection<TreeItem> _nodes;

    public ImageLoader(Collection<TreeItem> nodes)
    {
      super("Loading image");
      _nodes = nodes;
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor)
    {
      if (_nodes.size() == 0) return Status.OK_STATUS;

      Iterator<TreeItem> itr = _nodes.iterator();
      TreeItem node = itr.next();
      itr.remove();

      if (node.getImage() == null)
      {
        URI imageURI = (URI) node.getData();

        if (imageURI != null)
        {
          ImageRegistry registry = ((SpaceSearchResultViewer) getEditor())
              .getImageRegistry();
          Image img = registry.get(imageURI.toString());
          if (img == null)
            try
            {
              org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
              ImageData[] data = loader.load(imageURI.toURL().openStream());
              if (data.length > 0)
                img = new Image(Display.getCurrent(), data[0]);
            }
            catch (Exception e)
            {
              return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                  "Failed to load " + imageURI, e);
            }

          if (img != null)
          {
            node.setImage(img);

            // we need to relay out..
            getManagedForm().getForm().reflow(true);
          }
        }
      }

      new ImageLoader(_nodes).schedule(250);

      return Status.OK_STATUS;
    }
  }
}
