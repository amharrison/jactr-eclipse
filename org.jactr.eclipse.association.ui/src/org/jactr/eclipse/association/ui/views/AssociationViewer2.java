package org.jactr.eclipse.association.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.antlr.runtime.tree.CommonTree;
/*
 * default logging
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.layout.ILayoutAlgorithm;
import org.eclipse.gef.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef.mvc.fx.ui.actions.FitToViewportAction;
import org.eclipse.gef.mvc.fx.ui.actions.FitToViewportActionGroup;
import org.eclipse.gef.mvc.fx.ui.actions.ScrollActionGroup;
import org.eclipse.gef.mvc.fx.ui.actions.ZoomActionGroup;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.gef.zest.fx.jface.ZestFxJFaceModule;
import org.eclipse.gef.zest.fx.models.HidingModel;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.jactr.eclipse.association.ui.content.AssociationViewLabelProvider;
import org.jactr.eclipse.association.ui.content.AssociativeContentProvider;
import org.jactr.eclipse.association.ui.filter.IFilterProvider;
import org.jactr.eclipse.association.ui.filter.registry.AssociationFilterDescriptor;
import org.jactr.eclipse.association.ui.filter.registry.AssociationFilterRegistry;
import org.jactr.eclipse.association.ui.internal.Activator;
import org.jactr.eclipse.association.ui.mapper.IAssociationMapper;
import org.jactr.eclipse.association.ui.mapper.impl.DefaultAssociationMapper;
import org.jactr.eclipse.association.ui.mapper.registry.AssociationMapperDescriptor;
import org.jactr.eclipse.association.ui.mapper.registry.AssociationMapperRegistry;
import org.jactr.eclipse.association.ui.model.ModelAssociations;
import org.jactr.eclipse.core.concurrent.JobExecutor;
import org.jactr.eclipse.ui.UIPlugin;
import org.jactr.eclipse.ui.concurrent.UIJobExecutor;
import org.jactr.eclipse.ui.editor.ACTRModelEditor;
import org.jactr.io2.jactr.modelFragment.ModelFragment;

public class AssociationViewer2 extends ViewPart
{
  /**
   * Logger definition
   */
  static private final transient Log        LOGGER  = LogFactory
      .getLog(AssociationViewer2.class);

  public static final String                VIEW_ID = "org.jactr.eclipse.association.ui.views.AssociationViewer2";

  private ZestContentViewer                 _viewer;

  protected IAssociationMapper              _mapper;

  private Class<? extends ILayoutAlgorithm> _lastLayoutClass;

  private Collection<IFilterProvider>       _availableFilterProviders;

  private ZoomActionGroup                   _zoomActionGroup;

  private FitToViewportActionGroup          _fitToViewportActionGroup;

  private ScrollActionGroup                 _scrollActionGroup;

  private IEditorPart                       _currentEditor;

  public AssociationViewer2()
  {
    _lastLayoutClass = getLastLayout();
    assembleFilterProviders();
    restoreAssociationMapper();
  }

  @Override
  public void createPartControl(Composite parent)
  {
    _viewer = new ZestContentViewer(new ZestFxJFaceModule());

    _viewer.createControl(parent, SWT.NONE);
    _viewer.setContentProvider(new AssociativeContentProvider(_mapper));
    _viewer.setLayoutAlgorithm(new SpringLayoutAlgorithm());
    _viewer.setLabelProvider(
        new AssociationViewLabelProvider(_mapper, _viewer::getSelection));
    _viewer.getControl().addMouseListener(new MouseListener() {

      @Override
      public void mouseDoubleClick(MouseEvent e)
      {
        IStructuredSelection selection = (IStructuredSelection) _viewer
            .getSelection();
        Object astNode = selection.getFirstElement();

        if (astNode instanceof CommonTree)
          view((ACTRModelEditor) _currentEditor, astNode);
        else if (astNode instanceof EObject)
          view((XtextEditor) _currentEditor, astNode);
      }

      @Override
      public void mouseDown(MouseEvent e)
      {
        // TODO Auto-generated method stub

      }

      @Override
      public void mouseUp(MouseEvent e)
      {
        // TODO Auto-generated method stub

      }

    });

    setLayoutAlgorithm(_lastLayoutClass, false);

    _zoomActionGroup = new ZoomActionGroup(new FitToViewportAction());
    _viewer.getContentViewer().setAdapter(_zoomActionGroup);
    _fitToViewportActionGroup = new FitToViewportActionGroup();
    _viewer.getContentViewer().setAdapter(_fitToViewportActionGroup);
    _scrollActionGroup = new ScrollActionGroup();
    _viewer.getContentViewer().setAdapter(_scrollActionGroup);

    // contribute to toolbar
    IActionBars actionBars = getViewSite().getActionBars();
    IToolBarManager mgr = actionBars.getToolBarManager();
    _zoomActionGroup.fillActionBars(actionBars);
    mgr.add(new Separator());
    _fitToViewportActionGroup.fillActionBars(actionBars);
    mgr.add(new Separator());
    _scrollActionGroup.fillActionBars(actionBars);
  }

  @Override
  public void setFocus()
  {
    // _viewer.getControl().setFocus();
  }

  protected void restoreAssociationMapper()
  {
    String mapperClassName = Activator.getDefault().getPreferenceStore()
        .getString(VIEW_ID + ".mapper");
    if (mapperClassName.length() == 0) // null
      mapperClassName = DefaultAssociationMapper.class.getName();

    for (AssociationMapperDescriptor desc : AssociationMapperRegistry
        .getRegistry().getAllDescriptors())
      if (desc.getClassName().equals(mapperClassName))
        setAssociationMapper(desc);
  }

  protected void setAssociationMapper(AssociationMapperDescriptor descriptor)
  {
    // set
    try
    {
      IAssociationMapper assMapper = (IAssociationMapper) descriptor
          .instantiate();
      setAssociationMapper(assMapper);

      // save
      Activator.getDefault().getPreferenceStore().setValue(VIEW_ID + ".mapper",
          descriptor.getClassName());
    }
    catch (Exception e)
    {
      UIPlugin.log(IStatus.ERROR, String.format("Failed to instantiation %s ",
          descriptor.getClassName()), e);
    }

  }

  protected void setAssociationMapper(IAssociationMapper mapper)
  {
    _mapper = mapper;

    if (_viewer != null)
    {
      // I should probably test that we are in the ui thread..
      ((AssociationViewLabelProvider) _viewer.getLabelProvider())
          .setMapper(_mapper);
      _viewer.setContentProvider(new AssociativeContentProvider(_mapper));

      _viewer.setInput(_viewer.getInput());
    }
  }

  public IAssociationMapper getAssociationMapper()
  {
    return _mapper;
  }

  /**
   * creates but does not process the modelAssociation data
   * 
   * @param modelDescriptor
   *          may be null
   * @param nearestChunk
   *          may be null
   * @return
   */
  private ModelAssociations getAssociations(Object modelDescriptor,
      Object nearestChunk)
  {
    ModelAssociations rtn = null;
    if (modelDescriptor != null)
      if (nearestChunk != null)
        rtn = new ModelAssociations(modelDescriptor, getAssociationMapper(),
            _mapper.getLabel(nearestChunk));
      else
        rtn = new ModelAssociations(modelDescriptor, getAssociationMapper());
    else
      rtn = new ModelAssociations(getAssociationMapper());

    return rtn;
  }

  public void clearFilter()
  {
    HidingModel hm = _viewer.getContentViewer().getAdapter(HidingModel.class);

    // reset the filter state
    hm.getHiddenNodesUnmodifiable().forEach(node -> {
      hm.show(node);
    });
  }

  public void filter(ViewerFilter[] viewFilters)
  {
    clearFilter();

    HidingModel hm = _viewer.getContentViewer().getAdapter(HidingModel.class);

    _viewer.getContentNodeMap().entrySet().forEach((e) -> {
      for (ViewerFilter filter : viewFilters)
        if (!filter.select(_viewer, null, e.getKey())) hm.hide(e.getValue());
    });
  }

  public ModelAssociations getInput()
  {
    return (ModelAssociations) _viewer.getInput();
  }

  public void viewAll(final ACTRModelEditor editor)
  {
    view(editor, null);
  }

  public void view(ACTRModelEditor editor, Object nearestChunk)
  {
    _currentEditor = editor;
    view(editor.getCompilationUnit().getModelDescriptor(), nearestChunk,
        _lastLayoutClass);
  }

  public void viewAll(XtextEditor editor)
  {
    view(editor, null);
  }

  public void view(XtextEditor editor, Object nearestChunk)
  {
    _currentEditor = editor;
    editor.getDocument().readOnly(res -> {
      ModelFragment fragment = (ModelFragment) res.getContents().get(0);
      view(fragment, nearestChunk, _lastLayoutClass);
      return Status.OK_STATUS;
    });
  }

  public void view(final Object modelDescriptor, final Object nearestChunk,
      final Class<? extends ILayoutAlgorithm> layoutClass)
  {

    Runnable setup = () -> {

      LOGGER.debug("starting view");

      showBusy(true);

      setLayoutAlgorithm(layoutClass, false);
    };

    Runnable cleanup = () -> {
      LOGGER.debug("Done setting view");
      showBusy(false);
    };

    /*
     * our new version: 1) get the model association data (this is already
     * parallelized)
     */
    final ModelAssociations associations = getAssociations(modelDescriptor,
        nearestChunk);
    JobExecutor ex = new JobExecutor("association slave");
    final UIJobExecutor uiex = new UIJobExecutor("association slave ui");

    // setup
    CompletableFuture<Void> setupProc = CompletableFuture.runAsync(setup, uiex);

    // build the associations, but only after setup
    final CompletableFuture<Void> assProc = setupProc.thenComposeAsync((v) -> {

      return associations
          .process(Runtime.getRuntime().availableProcessors() / 2 + 1, ex);
    }, ex);

    // cleanup just in case
    assProc.exceptionally(t -> {
      UIPlugin.log(String.format("Failed to process associations in parallel"),
          t);
      uiex.execute(cleanup);
      return null;
    });

    // when this is done, we can actually set the display
    CompletableFuture<Void> setProc = assProc.thenAcceptAsync((v) -> {
      try
      {
        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Setting input for viewer"));

        _viewer.setInput(associations);

      }
      catch (Exception e)
      {
        UIPlugin.log("Failed to focus on " + nearestChunk, e);
      }
    }, uiex);

    // clean up after failure or success
    setProc.handle((v, t) -> {
      if (t != null) UIPlugin
          .log(String.format("Failed to set associations in parallel"), t);
      uiex.execute(cleanup);
      return null;
    });

  }

  @SuppressWarnings("unchecked")
  protected Class<? extends ILayoutAlgorithm> getLastLayout()
  {
    String layoutClassName = Activator.getDefault().getPreferenceStore()
        .getString(VIEW_ID + ".layout");
    if (layoutClassName.length() == 0) // null
      layoutClassName = SpringLayoutAlgorithm.class.getName();
    try
    {
      return (Class<? extends ILayoutAlgorithm>) getClass().getClassLoader()
          .loadClass(layoutClassName);
    }
    catch (Exception e)
    {
      return SpringLayoutAlgorithm.class;
    }
  }

  protected void setLayoutAlgorithm(Class clazz, final boolean forceLayout)
  {
    _lastLayoutClass = clazz;

    Activator.getDefault().getPreferenceStore().setValue(VIEW_ID + ".layout",
        clazz.getName());

    /*
     * do we need to actually change the layout?
     */
    // if (_viewer.getGraphControl().getLayoutAlgorithm() != null
    // && _viewer.getGraphControl().getLayoutAlgorithm().getClass() == clazz)
    // return;
    try
    {
      final ILayoutAlgorithm alg = (ILayoutAlgorithm) clazz.newInstance();
      // alg.setFilter(_layoutFilter);
      _viewer.setLayoutAlgorithm(alg);

//      Display.getCurrent().asyncExec(new Runnable() {
//        public void run()
//        {
//          if (forceLayout) _viewer.setInput(_viewer.getInput());
//        }
//      });
    }
    catch (Exception e)
    {

    }
  }

  private void assembleFilterProviders()
  {
    _availableFilterProviders = new ArrayList<IFilterProvider>();
    AssociationFilterRegistry afr = AssociationFilterRegistry.getRegistry();
    for (AssociationFilterDescriptor afd : afr.getAllDescriptors())
      try
      {
        _availableFilterProviders.add((IFilterProvider) afd.instantiate());
      }
      catch (Exception e)
      {
        UIPlugin
            .log(String.format("Failed to instantiation filter provider : %s",
                afd.getClassName()), e);
      }
  }

  @Override
  public void dispose()
  {
    // dispose actions
    if (_zoomActionGroup != null)
    {
      _viewer.getContentViewer().unsetAdapter(_zoomActionGroup);
      _zoomActionGroup.dispose();
      _zoomActionGroup = null;
    }
    if (_scrollActionGroup != null)
    {
      _viewer.getContentViewer().unsetAdapter(_scrollActionGroup);
      _scrollActionGroup.dispose();
      _scrollActionGroup = null;
    }
    if (_fitToViewportActionGroup != null)
    {
      _viewer.getContentViewer().unsetAdapter(_fitToViewportActionGroup);
      _fitToViewportActionGroup.dispose();
      _fitToViewportActionGroup = null;
    }
    super.dispose();
  }

}
