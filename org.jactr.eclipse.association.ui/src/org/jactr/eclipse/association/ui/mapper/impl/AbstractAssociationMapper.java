package org.jactr.eclipse.association.ui.mapper.impl;

/*
 * default logging
 */
import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.jactr.eclipse.association.ui.mapper.IAssociationMapper;
import org.jactr.eclipse.association.ui.model.Association;
import org.jactr.io.antlr3.misc.ASTSupport;

public abstract class AbstractAssociationMapper implements IAssociationMapper
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
                                                .getLog(AbstractAssociationMapper.class);

  public AbstractAssociationMapper()
  {
  }

  public String getLabel(Object assOrElement)
  {
    if(assOrElement instanceof Association)
      return getLabel((Association)assOrElement);
    if(assOrElement instanceof CommonTree)
      return getLabel((CommonTree)assOrElement);
    if(assOrElement instanceof EObject)
      return getLabel((EObject) assOrElement);
    if (assOrElement instanceof String) return (String) assOrElement;
    return "";
  }


  public String getLabel(Association association)
  {
    return String.format("%.2f", association.getStrength());
  }


  public String getLabel(CommonTree element)
  {
    return ASTSupport.getName(element);
  }


  public String getLabel(EObject eobject)
  {
    EStructuralFeature feature = eobject.eClass().getEStructuralFeature("name");
    return eobject.eGet(feature).toString();
  }

  public String getToolTip(Object assOrElement)
  {
    if(assOrElement instanceof Association)
      return getToolTip((Association)assOrElement);
    if(assOrElement instanceof CommonTree)
      return getToolTip((CommonTree)assOrElement);
    if(assOrElement instanceof EObject)
      return getToolTip((EObject) assOrElement);
    if (assOrElement instanceof String) return (String) assOrElement;
    return "";
  }


  public String getToolTip(EObject eobject)
  {
    return getLabel(eobject);
  }


  public String getToolTip(Association association)
  {
    return String.format("Strength %.2f", association.getStrength());
  }


  public String getToolTip(CommonTree element)
  {
    return ASTSupport.getName(element);
  }


}
