package org.jactr.eclipse.association.ui.model;

/*
 * default logging
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;

import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.xtext.EcoreUtil2;
import org.jactr.core.chunk.four.ISubsymbolicChunk4;
import org.jactr.core.concurrent.ExecutorServices;
import org.jactr.eclipse.association.ui.mapper.IAssociationMapper;
import org.jactr.io.antlr3.builder.JACTRBuilder;
import org.jactr.io.antlr3.misc.ASTSupport;
import org.jactr.io2.jactr.modelFragment.ChunkDef;
import org.jactr.io2.jactr.modelFragment.ModelFragment;
import org.jactr.io2.jactr.modelFragment.Parameter;

public class ModelAssociations
{
  /**
   * Logger definition
   */
  static private final transient Log                             LOGGER = LogFactory
      .getLog(ModelAssociations.class);

  private ConcurrentSkipListMap<String, Collection<Association>> _jChunks;

  private ConcurrentSkipListMap<String, Collection<Association>> _iChunks;

  private Collection<Association>                                _associations;

  private IAssociationMapper                                     _mapper;

  private Object                                                 _modelDescriptor;

  private String                                                 _focus;

  public ModelAssociations(Object modelDescriptor, IAssociationMapper mapper,
      String focalChunk)
  {
    this(mapper);
    _modelDescriptor = modelDescriptor;
    _focus = focalChunk;
  }

  public ModelAssociations(Object modelDescriptor, IAssociationMapper mapper)
  {
    this(modelDescriptor, mapper, null);
  }

  public ModelAssociations(IAssociationMapper mapper)
  {
    _mapper = mapper;
    _jChunks = new ConcurrentSkipListMap<String, Collection<Association>>();
    _iChunks = new ConcurrentSkipListMap<String, Collection<Association>>();
    _associations = Collections
        .synchronizedCollection(new HashSet<Association>());
  }

  public Object getModelDescriptor()
  {
    return _modelDescriptor;
  }

  public void process()
  {
    try
    {
      process(Runtime.getRuntime().availableProcessors(),
          ExecutorServices.getExecutor(ExecutorServices.POOL)).get();
    }
    catch (Exception e)
    {
      LOGGER.error("Failed to extract model associations", e);
    }
  }

  /**
   * @param maximumProcesses
   * @param executor
   * @return
   */
  public CompletableFuture<Void> process(final int maximumProcesses,
      final Executor executor)
  {
    if (_modelDescriptor instanceof CommonTree)
    {
      /*
       * first a runnable for the map of all chunks
       */
      CompletableFuture<Map<String, CommonTree>> allChunksFuture = CompletableFuture
          .supplyAsync(() -> {
            Map<String, CommonTree> map = ASTSupport.getMapOfTrees(
                (CommonTree) _modelDescriptor, JACTRBuilder.CHUNK);
            if (LOGGER.isDebugEnabled()) LOGGER.debug(
                String.format("Extracted map of trees [%d]", map.size()));
            return map;
          }, executor);

      /*
       * once that is done, we can split
       */
      CompletableFuture<Void> subProcs = allChunksFuture
          .thenComposeAsync((m) -> {

            Collection<CompletableFuture<Void>> submitted = new ArrayList<CompletableFuture<Void>>();
            List<CommonTree> allChunks = new ArrayList<>();

            int blockSize = m.size() / maximumProcesses;
            final Map<String, CommonTree> allChunksMap = m;

            if (LOGGER.isDebugEnabled())
              LOGGER.debug(String.format("Subdividing %s chunks into %d blocks",
                  m.size(), maximumProcesses));
            try
            {
              allChunks.addAll(m.values());

              for (int i = 0; i < maximumProcesses; i++)
              {
                //
                final Collection<CommonTree> subList = allChunks
                    .subList(i * blockSize, i * blockSize + blockSize);
                submitted.add(CompletableFuture.runAsync(() -> {
                  process(subList, allChunksMap);
                }, executor));
              }
            }
            catch (Exception e)
            {
              LOGGER.error("Failed to dispatch subprocesses ", e);
            }

            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                submitted.toArray(new CompletableFuture[submitted.size()]));

            allDone.handle((v, t) -> {
              if (t != null)
                LOGGER.error("Failed to complete all processes", t);
              else
                LOGGER.debug("processing complete");
              return null;
            });

            return allDone;
          }, executor);
      return subProcs;
    }
    else
    {
      // new io
      /*
       * first a runnable for the map of all chunks
       */
      CompletableFuture<Map<String, ChunkDef>> allChunksFuture = CompletableFuture
          .supplyAsync(() -> {

            Map<String, ChunkDef> map = new TreeMap<>();
            EcoreUtil2.getAllContentsOfType((ModelFragment) _modelDescriptor,
                ChunkDef.class).forEach(cd -> {
                  map.put(cd.getName(), cd);
                });

            return map;
          }, executor);

      /*
       * once that is done, we can split
       */
      CompletableFuture<Void> subProcs = allChunksFuture
          .thenComposeAsync((m) -> {

            Collection<CompletableFuture<Void>> submitted = new ArrayList<CompletableFuture<Void>>();
            List<ChunkDef> allChunks = new ArrayList<>();

            int blockSize = m.size() / maximumProcesses;
            final Map<String, ChunkDef> allChunksMap = m;

            if (LOGGER.isDebugEnabled())
              LOGGER.debug(String.format("Subdividing %s chunks into %d blocks",
                  m.size(), maximumProcesses));
            try
            {
              allChunks.addAll(m.values());

              for (int i = 0; i < maximumProcesses; i++)
              {
                //
                final Collection<ChunkDef> subList = allChunks
                    .subList(i * blockSize, i * blockSize + blockSize);
                submitted.add(CompletableFuture.runAsync(() -> {
                  processIO2(subList, allChunksMap);
                }, executor));
              }
            }
            catch (Exception e)
            {
              LOGGER.error("Failed to dispatch subprocesses ", e);
            }

            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                submitted.toArray(new CompletableFuture[submitted.size()]));

            allDone.handle((v, t) -> {
              if (t != null)
                LOGGER.error("Failed to complete all processes", t);
              else
                LOGGER.debug("processing complete");
              return null;
            });

            return allDone;
          }, executor);
      return subProcs;

    }

  }

  private void processIO2(Collection<ChunkDef> chunksToProcess,
      Map<String, ChunkDef> allChunks)
  {
    if (LOGGER.isDebugEnabled()) LOGGER
        .debug(String.format("Processing %d chunks", chunksToProcess.size()));

    for (ChunkDef jChunk : chunksToProcess)
      processIO2(jChunk, allChunks);

  }

  private void processIO2(ChunkDef jChunk, Map<String, ChunkDef> allChunks)
  {
    if (jChunk.getParameters() != null)
    {
      Optional<Parameter> linkParam = jChunk.getParameters().getParameter()
          .stream().filter(p -> {
            return ISubsymbolicChunk4.LINKS.equals(p.getName());
          }).findFirst();

      linkParam.ifPresent(p -> {
        String allLinks = p.getValue().getString();

        try
        {
          for (Association association : _mapper.extractAssociations(allLinks,
              jChunk, allChunks))

            if (_focus == null
                || _focus.equals(
                    _mapper.getLabel(association.getJChunk()))
                || _focus.equals(
                    _mapper.getLabel(association.getIChunk())))
              addAssociation(association);

        }
        catch (Exception e)
        {
          if (LOGGER.isWarnEnabled()) LOGGER.warn(
              String.format("Failed to extract link info from %s", allLinks),
              e);
        }
      });
    }
  }

  private void process(Collection<CommonTree> chunksToProcess,
      Map<String, CommonTree> allChunks)
  {
    Map<String, CommonTree> recycledParameters = new TreeMap<String, CommonTree>();

    if (LOGGER.isDebugEnabled()) LOGGER
        .debug(String.format("Processing %d chunks", chunksToProcess.size()));

    for (CommonTree jChunk : chunksToProcess)
      process(ASTSupport.getName(jChunk), jChunk, recycledParameters,
          allChunks);
  }

  private void process(String jChunkName, CommonTree jChunk,
      Map<String, CommonTree> recycledParameters,
      Map<String, CommonTree> allChunks)
  {
    recycledParameters.clear();

    recycledParameters = ASTSupport.getMapOfTrees(jChunk,
        JACTRBuilder.PARAMETER, recycledParameters);

    String linkKey = ISubsymbolicChunk4.LINKS.toLowerCase();

    if (!recycledParameters.containsKey(linkKey)) return;

    String allLinks = recycledParameters.get(linkKey).getChild(1).getText();

    try
    {
      for (Association association : _mapper.extractAssociations(allLinks,
          jChunk, allChunks))
        if (_focus == null
            || _focus.equals(
                ASTSupport.getName((CommonTree) association.getJChunk()))
            || _focus.equals(
                ASTSupport.getName((CommonTree) association.getIChunk())))
        {
          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("adding Link: j:%s i:%s str:%.2f",
                ((CommonTree) association.getJChunk()).toStringTree(),
                ((CommonTree) association.getIChunk()).toStringTree(),
                association.getStrength()));

          addAssociation(association);
        }

    }
    catch (Exception e)
    {
      if (LOGGER.isWarnEnabled()) LOGGER.warn(
          String.format("Failed to extract link info from %s", allLinks), e);
    }

  }

  public void addAssociation(Association association)
  {
    String j = _mapper.getLabel(association.getJChunk())
        .toLowerCase();
    String i = _mapper.getLabel(association.getIChunk())
        .toLowerCase();

    // if (LOGGER.isDebugEnabled())
    // LOGGER.debug(String.format("Adding j:%s i:%s %d %.2f", j, i,
    // association.getCount(), association.getStrength()));

    add(j, association, _jChunks);
    add(i, association, _iChunks);
    _associations.add(association);
  }

  public Collection<Association> getOutboundAssociations(String jChunkName)
  {
    return get(jChunkName, _jChunks);
  }

  public Collection<Association> getInboundAssociations(String iChunkName)
  {
    return get(iChunkName, _iChunks);
  }

  public Set<String> getChunkNames(Set<String> container)
  {
    if (container == null) container = new TreeSet<>();
    container.addAll(_iChunks.keySet());
    container.addAll(_jChunks.keySet());
    return container;
  }

  public Map<String, Object> chunks(Map<String, Object> container)
  {
    if (container == null) container = new TreeMap<>();
    final Map<String, Object> fContainer = container;

    _associations.forEach(ass -> {
      fContainer.putIfAbsent(_mapper.getLabel(ass.getIChunk()),
          ass.getIChunk());
      fContainer.putIfAbsent(_mapper.getLabel(ass.getJChunk()),
          ass.getJChunk());
    });
    return container;
  }

  protected Collection<Association> get(String keyName,
      Map<String, Collection<Association>> map)
  {
    Collection<Association> rtn = map.get(keyName.toLowerCase());
    if (rtn == null)
      rtn = Collections.EMPTY_LIST;
    else
      rtn = Collections.unmodifiableCollection(rtn);
    return rtn;
  }

  public Association[] getAssociations()
  {
    return _associations.toArray(new Association[0]);
  }

  private void add(String name, Association association,
      ConcurrentSkipListMap<String, Collection<Association>> container)
  {
    List<Association> addIfMissing = new ArrayList<>();
    Collection<Association> collection = container.putIfAbsent(name,
        addIfMissing);

    // we already had a collection attached
    if (collection == null) collection = addIfMissing;

    collection.add(association);
  }
}
