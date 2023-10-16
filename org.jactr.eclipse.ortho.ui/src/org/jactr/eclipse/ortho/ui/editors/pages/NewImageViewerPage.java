package org.jactr.eclipse.ortho.ui.editors.pages;

/*
 * default logging
 */
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.jactr.eclipse.ortho.ui.editors.SpaceSearchResultViewer;
import org.jactr.eclipse.ui.images.JACTRImages;
import org.jactr.tools.itr.ortho.ISliceAnalysis;

public class NewImageViewerPage extends FormPage
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
                                                .getLog(NewImageViewerPage.class);

  static private final String        ID     = NewImageViewerPage.class
                                                .getName();

  static public String getID(ISliceAnalysis analysis)
  {
    return ID + "." + analysis.getSlice().getId();
  }

  private final ISliceAnalysis _analysis;

  // private Tree _imageTree;

  private Gallery              _gallery;

  private URI                  _root;

  public NewImageViewerPage(FormEditor editor, ISliceAnalysis analysis, URI root)
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
  public void dispose()
  {
    clearContent();
    super.dispose();
  }

  @Override
  protected void createFormContent(IManagedForm managedForm)
  {
    Map<String, Object> parameters = _analysis.getSlice().getParameterValues();
    StringBuilder sb = new StringBuilder("Images for Slice #");
    sb.append(_analysis.getSlice().getId());
    sb.append(" (");
    Iterator<Map.Entry<String, Object>> itr = parameters.entrySet().iterator();
    while (itr.hasNext())
    {
      Map.Entry<String, Object> parameter = itr.next();
      sb.append(parameter.getKey()).append("=").append(parameter.getValue());
      if (itr.hasNext())
        sb.append(", ");
      else
        sb.append(")");
    }

    managedForm.getForm().setText(sb.toString());

    /*
     * and a close button
     */
    IToolBarManager mgr = managedForm.getForm().getToolBarManager();
    IAction action = new Action() {
      @Override
      public void run()
      {
        ((SpaceSearchResultViewer) getEditor())
            .hideImageViewer(NewImageViewerPage.this);
      }
    };
    action
        .setImageDescriptor(JACTRImages.getImageDescriptor(JACTRImages.CLOSE));
    action.setText("Close");
    action.setToolTipText("Close current image viewer");
    mgr.add(action);

    managedForm.getForm().updateToolBar();

    // TableWrapLayout layout = new TableWrapLayout();
    // layout.numColumns = 1;
    // managedForm.getForm().getBody().setLayout(layout);

    buildDetails(managedForm);
    buildImageViewer(managedForm);

    updateContent();
  }

  protected void buildDetails(IManagedForm managedForm)
  {

  }

  protected Gallery buildImageViewer(IManagedForm managedForm)
  {
    // Section sliceSection = managedForm.getToolkit().createSection(
    // managedForm.getForm().getBody(),
    // ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
    // | ExpandableComposite.EXPANDED | ExpandableComposite.CLIENT_INDENT);
    //
    // sliceSection.setText("Images");
    Composite body = managedForm.getForm().getBody();
    body.setLayout(new FillLayout());

    _gallery = new Gallery(body, SWT.V_SCROLL | SWT.VIRTUAL | SWT.BORDER);

    NoGroupRenderer gr = new NoGroupRenderer();
    // DefaultGalleryGroupRenderer gr = new DefaultGalleryGroupRenderer();
    gr.setMinMargin(2);
    gr.setItemSize(400, 400);
    gr.setAutoMargin(true);
    gr.setAlwaysExpanded(true);
    gr.setExpanded(true);

    _gallery.setGroupRenderer(gr);

    DefaultGalleryItemRenderer ir = new DefaultGalleryItemRenderer();

    _gallery.setItemRenderer(ir);
    _gallery.setLowQualityOnUserAction(true);
    _gallery.setVirtualGroups(true);
    _gallery.setVirtualGroupsCompatibilityMode(true);

    managedForm.getToolkit().adapt(_gallery);
    // sliceSection.setClient(_gallery);

    return _gallery;
  }

  protected URI getImageURI(String relativeURI)
  {
    return _root.resolve(relativeURI);
  }

  public void clearContent()
  {
    if (_gallery.isDisposed()) return;

    GalleryItem[] items = _gallery.getItems();

    for (GalleryItem item : items)
      if (!item.isDisposed() && item.getImage() != null)
      {
        item.getImage().dispose();
        item.setImage(null);
      }

    _gallery.removeAll();

  }

  protected void updateContent()
  {
    clearContent();

    org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
    /*
     * start loading..
     */
    GalleryItem group = new GalleryItem(_gallery, SWT.NONE);
    group.setExpanded(true);

    for (Map.Entry<String, String> image : _analysis.getImages().entrySet())
    {
      GalleryItem item = new GalleryItem(group, SWT.NONE);
      item.setText(image.getKey());

      URI imageURI = getImageURI(image.getValue());

      Image img = null;
      try
      {
        ImageData[] data = loader.load(imageURI.toURL().openStream());
        if (data.length > 0)
        {
          img = new Image(Display.getCurrent(), data[0]);
          item.setImage(img);

        }
      }
      catch (Exception e)
      {
        LOGGER.error("Could not load image ", e);
      }

    }

    // new ImageLoader(imageNodes).schedule(250);
    _gallery.pack();
  }

}
