package org.jactr.eclipse.ortho.ui.model;

/*
 * default logging
 */
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.tools.itr.ortho.ISliceAnalysis;

public class SpaceSearch implements ISpaceSearch
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
                                                .getLog(SpaceSearch.class);

  private final String               _name;

  private final long                 _time;

  private final List<ISliceAnalysis> _analyses;
  
  private final URI                  _uri;

  public SpaceSearch(String name, long timestamp, URI uri)
  {
    _uri = uri;
    _name = name;
    _time = timestamp;
    _analyses = new ArrayList<ISliceAnalysis>();
  }
  
  public URI getURI()
  {
    return _uri;
  }
  
  public String getName()
  {
    return _name;
  }

  public int getNumberOfSlices()
  {
    return _analyses.size();
  }

  public Collection<ISliceAnalysis> getSliceAnalyses()
  {
    return _analyses;
  }

  public ISliceAnalysis getSliceAnalysis(int index)
  {
    return _analyses.get(index);
  }

  public long getTimestamp()
  {
    return _time;
  }


}
