package org.jactr.eclipse.ortho.ui.parser;

/*
 * default logging
 */
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.eclipse.ortho.ui.model.SpaceSearch;
import org.jactr.tools.itr.ortho.ISliceAnalysis;
import org.jactr.tools.itr.ortho.impl.Slice;
import org.jactr.tools.itr.ortho.impl.SliceAnalysis;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SpaceSearchParser
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER        = LogFactory
                                                       .getLog(SpaceSearchParser.class);

  static private final String        NAME_ATTR     = "name";

  static private final String        VALUE_ATTR    = "value";

  static private final String        DATE_ATTR     = "date";

  static private final String        SLICE_TAG     = "slice";

  static private final String        ID_ATTR       = "id";

  static private final String        PARAMETER_TAG = "parameter";

  static private final String        URL_ATTR      = "url";

  static private final String        FLAG_ATTR     = "flag";

  static private final String        LABEL_ATTR    = "label";

  static private final String        FIT_TAG       = "fit";

  static private final String        DETAIL_TAG    = "detail";

  static private final String        MODEL_TAG     = "model";

  static private final String        IMAGE_TAG     = "image";

  static private final String        NOTES_TAG     = "notes";

  public SpaceSearchParser()
  {

  }

  public SpaceSearch parse(URI uri) throws Exception
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = factory.newDocumentBuilder();
    Document doc = parser.parse(uri.toURL().openStream());

    return process(doc, uri);
  }

  protected SpaceSearch process(Document document, URI uri)
  {
    String name = document.getDocumentElement().getAttribute(NAME_ATTR);
    long dateLong = 0;
    try
    {
      String dateStr = document.getDocumentElement().getAttribute(DATE_ATTR);
      DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
          DateFormat.LONG);
      Date date = format.parse(dateStr);
      dateLong = date.getTime();
    }
    catch (Exception e)
    {
      LOGGER.error("failed to parse date ", e);
    }

    SpaceSearch space = new SpaceSearch(name, dateLong, uri);

    NodeList slices = document.getDocumentElement().getElementsByTagName(
        SLICE_TAG);
    for (int i = 0; i < slices.getLength(); i++)
      space.getSliceAnalyses().add(createSlice((Element) slices.item(i)));

    return space;
  }

  protected ISliceAnalysis createSlice(Element sliceNode)
  {
    long id = 0;
    try
    {
      id = Long.parseLong(sliceNode.getAttribute(ID_ATTR));
    }
    catch (NumberFormatException nfe)
    {

    }


    Slice slice = new Slice(id, 0, 0);

    NodeList list = sliceNode.getElementsByTagName(PARAMETER_TAG);
    for (int i = 0; i < list.getLength(); i++)
    {
      Element p = (Element) list.item(i);
      slice.setProperty(p.getAttribute(NAME_ATTR), p.getAttribute(VALUE_ATTR));
    }

    SliceAnalysis analysis = new SliceAnalysis(slice, id + "");

    list = sliceNode.getElementsByTagName(MODEL_TAG);
    for (int i = 0; i < list.getLength(); i++)
    {
      Element model = (Element) list.item(i);
      analysis.addModel(model.getAttribute(NAME_ATTR), model
          .getAttribute(URL_ATTR));
    }

    list = sliceNode.getElementsByTagName(DETAIL_TAG);
    for (int i = 0; i < list.getLength(); i++)
    {
      Element detail = (Element) list.item(i);
      analysis.addDetail(detail.getAttribute(LABEL_ATTR), detail
          .getAttribute(URL_ATTR));
    }

    list = sliceNode.getElementsByTagName(IMAGE_TAG);
    for (int i = 0; i < list.getLength(); i++)
    {
      Element detail = (Element) list.item(i);
      analysis.addImage(detail.getAttribute(LABEL_ATTR), detail
          .getAttribute(URL_ATTR));
    }

    list = sliceNode.getElementsByTagName(FIT_TAG);
    for (int i = 0; i < list.getLength(); i++)
    {
      Element detail = (Element) list.item(i);
      String label = detail.getAttribute(LABEL_ATTR);
      boolean flagged = false;
      try
      {
        flagged = Boolean.parseBoolean(detail.getAttribute(FLAG_ATTR));
      }
      catch (Exception e)
      {

      }
      /*
       * now we need all the other attributes
       */
      Map<String, String> attrs = new TreeMap<String, String>();
      NamedNodeMap nnm = detail.getAttributes();
      for (int j = 0; j < nnm.getLength(); j++)
      {
        Node attrNode = nnm.item(j);
        String attrName = attrNode.getNodeName();
        if (LABEL_ATTR.equalsIgnoreCase(attrName)
            || FLAG_ATTR.equalsIgnoreCase(attrName))
          continue;
        else
          attrs.put(attrName, attrNode.getNodeValue());
      }

      analysis.addFitStatistics(label, attrs, flagged);
    }

    StringBuilder notes = new StringBuilder();
    extractNotes(sliceNode, notes);
    analysis.setNotes(notes.toString().trim());

    return analysis;
  }

  private void extractNotes(Node node, StringBuilder notes)
  {
    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++)
    {
      Node child = list.item(i);
      if (child.getNodeName().equalsIgnoreCase("#cdata-section"))
        notes.append(child.getTextContent());
      else if (child.hasChildNodes()) extractNotes(child, notes);
    }
  }

}
