package org.jactr.eclipse.association.ui.content;

/*
 * default logging
 */
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.fx.nodes.PolyBezierInterpolator;
import org.eclipse.gef.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef.zest.fx.ZestProperties;
import org.eclipse.gef.zest.fx.jface.IGraphAttributesProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jactr.eclipse.association.ui.mapper.IAssociationMapper;
import org.jactr.eclipse.association.ui.model.Association;
import org.jactr.eclipse.ui.content.ACTRLabelProvider;
import org.jactr.eclipse.ui.images.JACTRImages;

public class AssociationViewLabelProvider extends ACTRLabelProvider
    implements IColorProvider, IGraphAttributesProvider
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
      .getLog(AssociationViewLabelProvider.class);

  private NumberFormat               _format;

  private Color                      _incoming;

  private Color                      _outgoing;

  private Color                      _default;

  private Color                      _selected;

  private Color                      _background;

  private IAssociationMapper         _mapper;

  private Supplier<ISelection>       _selectionProvider;

  public AssociationViewLabelProvider(IAssociationMapper mapper,
      Supplier<ISelection> selectionProvider)
  {
    _mapper = mapper;
    _format = NumberFormat.getNumberInstance();
    _format.setMinimumFractionDigits(2);
    _format.setMaximumFractionDigits(2);
    _selectionProvider = selectionProvider;

    _incoming = new Color(Display.getCurrent(), new RGB(0, 128, 0));
    _outgoing = new Color(Display.getCurrent(), new RGB(128, 0, 0));
    _default = new Color(Display.getCurrent(), new RGB(255, 255, 255));
    _selected = new Color(Display.getCurrent(), new RGB(0, 0, 128));
    _background = new Color(Display.getCurrent(), new RGB(0, 0, 128));
  }

  public void setMapper(IAssociationMapper mapper)
  {
    _mapper = mapper;
  }

  @Override
  public void dispose()
  {
    _incoming.dispose();
    _outgoing.dispose();
    _default.dispose();
    _selected.dispose();
    super.dispose();
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof EObject)
      return JACTRImages.getImage(JACTRImages.CHUNK);
    return super.getImage(element);
  }

  @Override
  public String getText(Object element)
  {
    if (element instanceof Association) return _mapper.getLabel(element);
    if (element instanceof CommonTree) return _mapper.getLabel(element);
    if (element instanceof EObject) return _mapper.getLabel(element);
    return "";
  }

  public Color getColor(Object rel)
  {
    // if (proceedsCurrentProduction(rel)) return _positiveProceed;
    // if (followsCurrentProduction(rel)) return _positiveFollow;

    // double score = getScore(rel);
    // if (score > 0) return _positiveFollow;
    // if (score < 0) return _negative;
    IStructuredSelection selection = (IStructuredSelection) _selectionProvider
        .get();
    Object obj = selection.getFirstElement();
    /*
     * could be a commonTree, in which case, we color the out going and in
     * coming differently if it is an association, we just use the default
     * highlight color. if there is no selection, we return null
     */

    if (rel instanceof Association)
    {
      Association ass = (Association) rel;
      if (ass.getIChunk().equals(selection)) return _outgoing;
      if (ass.getJChunk().equals(selection)) return _incoming;
    }

    if (obj == rel) return _selected;

    return _default;
  }

  public IFigure getTooltip(Object element)
  {
    if (element instanceof Association)
      return new Label(_mapper.getToolTip(element));
    if (element instanceof CommonTree)
      return new Label(_mapper.getToolTip(element));
    if (element instanceof EObject)
      return new Label(_mapper.getToolTip(element));
    return null;
  }

  @Override
  public Map<String, Object> getEdgeAttributes(Object sourceNode,
      Object targetNode)
  {
    return Collections.<String, Object> singletonMap(
        ZestProperties.INTERPOLATOR__E, new PolyBezierInterpolator());
  }

  @Override
  public Map<String, Object> getGraphAttributes()
  {
    return Collections.<String, Object> singletonMap(
        ZestProperties.LAYOUT_ALGORITHM__G, new SpringLayoutAlgorithm());
  }

  @Override
  public Map<String, Object> getNestedGraphAttributes(Object nestingNode)
  {
    return null;
  }

  @Override
  public Map<String, Object> getNodeAttributes(Object node)
  {
    return Collections.emptyMap();
  }

  @Override
  public Color getForeground(Object element)
  {
    return getColor(element);
  }

  @Override
  public Color getBackground(Object element)
  {

    return _background;
  }

}
