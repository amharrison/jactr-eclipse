package org.jactr.eclipse.association.ui.content;

import java.util.HashSet;
import java.util.Set;

/*
 * default logging
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.zest.fx.jface.IGraphContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jactr.eclipse.association.ui.mapper.IAssociationMapper;
import org.jactr.eclipse.association.ui.model.ModelAssociations;

public class AssociativeContentProvider implements IGraphContentProvider
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
                                                .getLog(AssociativeContentProvider.class);

  private IAssociationMapper         _mapper;
  private ModelAssociations          _associations;

  public AssociativeContentProvider(IAssociationMapper mapper)
  {
    _mapper = mapper;
  }

  public void dispose()
  {

  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
    _associations = (ModelAssociations) newInput;
  }

  @Override
  public Object[] getAdjacentNodes(Object node)
  {
    String chunkName = _mapper.getLabel(node);

    Set<Object> adjacentNodes = new HashSet<>();
    _associations.getOutboundAssociations(chunkName).forEach(ass -> {
      Object i = ass.getIChunk();
      if (i != null) adjacentNodes.add(i);
    });
    return adjacentNodes.toArray();
  }

  @Override
  public Object[] getNestedGraphNodes(Object node)
  {
    return null;
  }

  @Override
  public Object[] getNodes()
  {
    if (_associations == null) return new Object[] {};

    // this should be all the chunks not the links.

    return _associations.chunks(null).values().toArray();
  }

  @Override
  public boolean hasNestedGraph(Object node)
  {
    return false;
  }

}
