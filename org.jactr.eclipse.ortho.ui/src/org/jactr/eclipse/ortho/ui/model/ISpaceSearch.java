package org.jactr.eclipse.ortho.ui.model;

/*
 * default logging
 */
import java.net.URI;
import java.util.Collection;

import org.jactr.tools.itr.ortho.ISliceAnalysis;

public interface ISpaceSearch
{

  public String getName();
  
  public long getTimestamp();

  public ISliceAnalysis getSliceAnalysis(int index);

  public int getNumberOfSlices();

  public Collection<ISliceAnalysis> getSliceAnalyses();
  
  public URI getURI();
}
