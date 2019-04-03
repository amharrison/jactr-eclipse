package org.jactr.core.module.declarative.basic.type;

/*
 * default logging
 */
import java.util.Collection;
import java.util.Collections;

 
import org.slf4j.LoggerFactory;
import org.jactr.core.chunktype.IChunkType;
import org.jactr.core.chunktype.ISymbolicChunkType;
import org.jactr.core.chunktype.basic.BasicSymbolicChunkType;

public class DefaultSymbolicChunkTypeFactory implements
    ISymbolicChunkTypeFactory
{
  /**
   * Logger definition
   */
  static private final transient org.slf4j.Logger LOGGER = LoggerFactory
                                                .getLogger(DefaultSymbolicChunkTypeFactory.class);

  public ISymbolicChunkType newSymbolicChunkType()
  {
    return new BasicSymbolicChunkType();
  }

  public void bind(ISymbolicChunkType symbolic, IChunkType wrapper,
      Collection<IChunkType> parents)
  {
    BasicSymbolicChunkType ct = (BasicSymbolicChunkType) symbolic;
    ct.bind(wrapper, parents);
  }

  public void unbind(ISymbolicChunkType symbolic)
  {
    BasicSymbolicChunkType ct = (BasicSymbolicChunkType) symbolic;
    ct.bind(null, Collections.EMPTY_LIST);
  }

  public void merge(ISymbolicChunkType master, ISymbolicChunkType mergie)
  {
    // noop

  }

  public void dispose(ISymbolicChunkType symbolic)
  {
    symbolic.dispose();
  }

}
