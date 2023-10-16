package org.jactr.eclipse.ortho.ui.editors.pages;

/*
 * default logging
 */
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.ide.IDE;
import org.jactr.eclipse.ortho.ui.Activator;
import org.jactr.eclipse.ortho.ui.editors.SpaceSearchResultViewer;
import org.jactr.eclipse.ortho.ui.model.ISpaceSearch;
import org.jactr.eclipse.ui.images.JACTRImages;
import org.jactr.tools.itr.ortho.ISliceAnalysis;

public class SummaryPage extends FormPage
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER        = LogFactory
                                                       .getLog(SummaryPage.class);

  static public final String         ID            = SummaryPage.class
                                                       .getName();

  static public final String         FLAGGED_COLOR = "flaggedColor";

  static public final String         VIEW_ALL      = "view-all";

  private ISpaceSearch               _spaceSearch;

  private Table                      _sliceTable;

  private Table                      _fitsTable;

  private FormText                   _detailsText;

  private FormText                   _detailsDescription;

  private Text                       _notes;

  private ISliceAnalysis             _selection;

  private HyperlinkAdapter           _linkOpener;

  private boolean                    _isDirty      = false;

  private boolean                    _isUpdating   = false;

  public SummaryPage(FormEditor editor)
  {
    super(editor, ID, "Results");

    _linkOpener = new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent event)
      {
        if (_spaceSearch == null) return;

        if (VIEW_ALL.equals(event.getHref().toString()))
          ((SpaceSearchResultViewer) getEditor()).show(_selection);
        else
          try
          {
            URI source = _spaceSearch.getURI();
            URI newURI = source.resolve(event.getHref().toString());

            if (LOGGER.isDebugEnabled())
              LOGGER.debug("attempting to go to " + newURI);

            IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
            IEditorRegistry registry = PlatformUI.getWorkbench()
                .getEditorRegistry();
            IEditorDescriptor desc = registry
                .getDefaultEditor(newURI.getPath());

            String editorId = IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID;
            if (desc != null) editorId = desc.getId();

            IDE.openEditor(page, newURI, editorId, true);
          }
          catch (Exception e)
          {
            LOGGER.error("Failed to process uri " + event.getHref());
          }

      }
    };
  }

  @Override
  protected void createFormContent(IManagedForm managedForm)
  {
    Activator.getDefault().getFormColors()
        .createColor(FLAGGED_COLOR, 255, 255, 0);

    TableWrapLayout layout = new TableWrapLayout();
    layout.numColumns = 2;

    managedForm.getForm().getBody().setLayout(layout);

    buildHeader(managedForm);
    _sliceTable = buildSliceList(managedForm);
    _fitsTable = buildSliceFitTable(managedForm);
    _detailsText = buildDetails(managedForm);
    _notes = buildSliceNotes(managedForm);

    updateContents(_spaceSearch);
  }

  @Override
  public void setFocus()
  {
    _sliceTable.setFocus();
  }

  protected Image getSystemImage(String extension)
  {
    Image image = getImage(extension);
    if (image != null) return image;

    Program program = Program.findProgram(extension);
    if (program == null) return null;
    ImageData data = program.getImageData();
    if (data == null) return null;

    image = new Image(Display.getCurrent(), program.getImageData());
    setImage(extension, image);

    return image;
  }

  @Override
  public boolean isDirty()
  {
    return _isDirty | super.isDirty();
  }

  public void reset()
  {
    _isDirty = false;
    ((SpaceSearchResultViewer) getEditor()).editorDirtyStateChanged();
  }

  @Override
  public void doSave(IProgressMonitor monitor)
  {
    if (_selection != null) saveNotes(_selection);
    super.doSave(monitor);
  }

  protected Image getImage(String key)
  {
    return ((SpaceSearchResultViewer) getEditor()).getImageRegistry().get(key);
  }

  protected void setImage(String key, ImageDescriptor imgData)
  {
    ((SpaceSearchResultViewer) getEditor()).getImageRegistry()
        .put(key, imgData);
  }

  protected void setImage(String key, Image image)
  {
    ((SpaceSearchResultViewer) getEditor()).getImageRegistry().put(key, image);
  }

  protected void buildHeader(IManagedForm managedForm)
  {
    managedForm.getForm().setText("Summary for ");
  }

  protected Table buildSliceList(IManagedForm managedForm)
  {
    Section sliceSection = managedForm.getToolkit().createSection(
        managedForm.getForm().getBody(),
        ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
            | ExpandableComposite.EXPANDED);

    TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
    td.colspan = 1;
    td.rowspan = 1;
    td.maxWidth = 200;
    td.grabHorizontal = true;
    td.grabVertical = true;

    // GridData td = new GridData();

    sliceSection.setLayoutData(td);
    sliceSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e)
      {
        getManagedForm().reflow(true);
      }
    });

    sliceSection.setText("Slices");

    final Table table = managedForm.getToolkit().createTable(sliceSection,
        SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
    sliceSection.setClient(table);
    table.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e)
      {
        widgetSelected(e);
        // if (_selection != null)
        // ((SpaceSearchResultViewer) getEditor()).show(_selection);
        if (table.getSelectionCount() > 0)
        {
          SpaceSearchResultViewer viewer = (SpaceSearchResultViewer) getEditor();

          for (TableItem selectedItem : table.getSelection())
          {
            ISliceAnalysis analysis = (ISliceAnalysis) selectedItem.getData();
            viewer.show(analysis);
          }
        }
      }

      public void widgetSelected(SelectionEvent e)
      {
        if (table.getSelectionCount() > 0)
          selected(table.getSelection()[0].getData());
        else
          selected(null);
      }

    });
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    return table;
  }

  protected Table buildSliceFitTable(IManagedForm managedForm)
  {
    Section fitSection = managedForm.getToolkit().createSection(
        managedForm.getForm().getBody(),
        ExpandableComposite.TITLE_BAR | Section.DESCRIPTION
            | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

    TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
    td.colspan = 1;
    td.rowspan = 1;
    td.grabHorizontal = true;
    // td.grabVertical = true;
    fitSection.setLayoutData(td);
    fitSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e)
      {
        getManagedForm().reflow(true);
      }
    });

    fitSection.setText("Fit Details");
    fitSection.setDescription("Model fit statistics for this block of runs");

    Table fitTable = managedForm.getToolkit().createTable(fitSection,
        SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
    fitSection.setClient(fitTable);
    fitTable.setHeaderVisible(true);
    fitTable.setLinesVisible(true);

    return fitTable;
  }

  protected Text buildSliceNotes(IManagedForm managedForm)
  {
    Section fitSection = managedForm.getToolkit().createSection(
        managedForm.getForm().getBody(),
        Section.DESCRIPTION | ExpandableComposite.TITLE_BAR
            | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

    TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB,
        TableWrapData.FILL_GRAB);
    td.colspan = 1;
    td.rowspan = 1;
    td.grabHorizontal = true;
    td.grabVertical = true;
    td.heightHint = 250;
    fitSection.setLayoutData(td);
    fitSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e)
      {
        getManagedForm().reflow(true);
      }
    });

    fitSection.setText("Slice notes");
    fitSection.setDescription("Searchable notes for this block of runs");

    Text text = managedForm.getToolkit().createText(fitSection, "",
        SWT.MULTI | SWT.WRAP);
    text.setEditable(true);
    text.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e)
      {
        if (!_isUpdating)
        {
          _isDirty = true;
          ((SpaceSearchResultViewer) getEditor()).editorDirtyStateChanged();
        }
      }
    });

    fitSection.setClient(text);

    return text;
  }

  protected FormText buildDetails(IManagedForm managedForm)
  {
    Section sliceSection = managedForm.getToolkit().createSection(
        managedForm.getForm().getBody(),
        ExpandableComposite.CLIENT_INDENT | ExpandableComposite.TITLE_BAR
            | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

    TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
    td.colspan = 1;
    td.rowspan = 1;
    td.grabHorizontal = true;
    // td.grabVertical = true;

    sliceSection.setLayoutData(td);
    sliceSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e)
      {
        getManagedForm().reflow(true);
      }
    });

    _detailsDescription = managedForm.getToolkit().createFormText(sliceSection,
        false);

    sliceSection.setText("Models & Details");
    sliceSection.setDescriptionControl(_detailsDescription);

    FormText text = managedForm.getToolkit()
        .createFormText(sliceSection, false);
    sliceSection.setClient(text);

    text.addHyperlinkListener(_linkOpener);

    return text;
  }

  @Override
  protected void setInput(IEditorInput input)
  {
    super.setInput(input);
    _spaceSearch = ((SpaceSearchResultViewer) getEditor()).getSpaceSearch();
  }

  protected void selected(Object data)
  {
    if (_selection != null) saveNotes(_selection);

    _selection = (ISliceAnalysis) data;

    _isUpdating = true;

    updateNotes(_selection);
    updateFitTable(_selection);
    updateDetails(_selection);

    _isUpdating = false;

    getManagedForm().getForm().reflow(true);
  }

  protected void saveNotes(ISliceAnalysis slice)
  {
    slice.setNotes(_notes.getText());
  }

  protected void updateNotes(ISliceAnalysis slice)
  {
    String notes = slice.getNotes();
    if (notes == null) notes = "";
    _notes.setText(notes);
  }

  protected void updateContents(ISpaceSearch space)
  {
    ScrolledForm form = getManagedForm().getForm();
    DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG,
        DateFormat.LONG);

    form.setText("Results for " + space.getName() + " ("
        + space.getNumberOfSlices() + ") run on "
        + format.format(new Date(space.getTimestamp())));

    updateSliceList(space);

    form.reflow(true);
  }

  protected void updateSliceList(ISpaceSearch space)
  {
    _sliceTable.removeAll();
    /*
     * first we need to snag all the columns (aka, parameters)
     */
    Set<String> columnNames = new TreeSet<String>();
    for (ISliceAnalysis analysis : space.getSliceAnalyses())
      columnNames.addAll(analysis.getSlice().getParameterValues().keySet());

    /*
     * column time
     */
    new TableColumn(_sliceTable, SWT.NONE).setText("Slice");
    for (String columnName : columnNames)
    {
      TableColumn column = new TableColumn(_sliceTable, SWT.NONE);
      column.setText(columnName);
      column.setMoveable(true);
    }

    /*
     * and add the items
     */
    TableItem first = null;
    for (ISliceAnalysis analysis : space.getSliceAnalyses())
    {
      String[] text = new String[columnNames.size() + 1];
      text[0] = analysis.getSlice().getId() + "";
      int i = 1;
      for (String columnName : columnNames)
        text[i++] = analysis.getSlice().getParameterValues().get(columnName)
            .toString();

      TableItem item = new TableItem(_sliceTable, SWT.NONE);
      item.setText(text);
      item.setData(analysis);

      if (first == null) first = item;

      if (analysis.isFlagged())
        item.setBackground(Activator.getDefault().getFormColors()
            .getColor(FLAGGED_COLOR));
    }

    for (int i = 0; i < columnNames.size() + 1; i++)
      _sliceTable.getColumn(i).pack();

    if (first != null)
    {
      _sliceTable.setSelection(0);
      selected(first.getData());
    }
  }

  protected void updateFitTable(ISliceAnalysis selection)
  {
    _fitsTable.removeAll();

    for (TableColumn column : _fitsTable.getColumns())
      column.dispose();

    if (selection == null) return;

    Map<String, Map<String, String>> fitData = selection.getFitStatistics();
    /*
     * first we need to know all the columns..
     */
    Set<String> columnNames = new TreeSet<String>();
    for (Map<String, String> fits : fitData.values())
      columnNames.addAll(fits.keySet());

    new TableColumn(_fitsTable, SWT.NONE).setText("Measure");

    for (String columnName : columnNames)
    {
      TableColumn column = new TableColumn(_fitsTable, SWT.NONE);
      column.setText(columnName);
      column.setMoveable(true);
    }

    for (String rowLabel : fitData.keySet())
    {
      TableItem item = new TableItem(_fitsTable, SWT.NONE);
      Map<String, String> data = fitData.get(rowLabel);
      String[] row = new String[columnNames.size() + 1];
      row[0] = rowLabel;
      int i = 1;
      boolean flagged = false;
      for (String columnName : columnNames)
      {
        if (columnName.equalsIgnoreCase("flag")
            || columnName.equalsIgnoreCase("flagged"))
          flagged = "true".equalsIgnoreCase(data.get(columnName));

        row[i++] = data.containsKey(columnName) ? data.get(columnName) : "";
      }

      item.setText(row);
      if (flagged)
        item.setBackground(Activator.getDefault().getFormColors()
            .getColor(FLAGGED_COLOR));
    }

    for (int i = 0; i < columnNames.size() + 1; i++)
      _fitsTable.getColumn(i).pack();
  }

  protected void updateDetails(ISliceAnalysis selection)
  {
    if (selection == null)
    {
      _detailsText.setText("", false, false);
      _detailsDescription.setText("", false, false);
      return;
    }

    String prefix = selection.getWorkingDirectory();
    StringBuilder text = new StringBuilder("<form>");
    text.append("<p>The following models were executed:</p>");
    /*
     * models..
     */
    for (Map.Entry<String, String> model : selection.getModels().entrySet())
    {
      Image modelImg = JACTRImages.getImage(JACTRImages.MODEL);

      String url = prefix + "/" + model.getValue();
      text.append("<li style=\"image\" value=\"model\"><a href=\"").append(url)
          .append("\">").append(model.getKey()).append("</a></li>");

      _detailsText.setImage("model", modelImg);
    }

    /*
     * details
     */
    text.append("<br/>");
    Map<String, String> details = selection.getDetails();
    if (details.size() != 0)
    {
      text.append("<p>Analyses returned the following additional details:</p>");
      for (Map.Entry<String, String> entry : details.entrySet())
      {
        String url = prefix + "/" + entry.getValue();
        String ext = url.substring(url.lastIndexOf('.') + 1);
        Image img = getSystemImage(ext);

        if (img != null)
        {
          text.append("<li style=\"image\" value=\"").append(url).append("\">");
          _detailsText.setImage(url, img);
        }
        else
          text.append("<li>");

        text.append("<a href=\"").append(url).append("\">")
            .append(entry.getKey()).append("</a></li>");
      }
    }

    /*
     * iamges
     */
    text.append("<br/>");
    Map<String, String> images = selection.getImages();
    if (images.size() != 0)
    {
      text.append("<p>Analyses returned the following images:</p>");
      text.append("<li style=\"image\" value=\"all_images\"><a href=\"")
          .append(VIEW_ALL).append("\">View all images</a></li>");
      _detailsText.setImage("all_images", getSystemImage("png"));

      for (Map.Entry<String, String> entry : images.entrySet())
      {
        String url = prefix + "/" + entry.getValue();
        String ext = url.substring(url.lastIndexOf('.') + 1);
        Image img = getSystemImage(ext);

        if (img != null)
        {
          text.append("<li bindent=\"10\" style=\"image\" value=\"")
              .append(ext).append("\">");
          _detailsText.setImage(ext, img);
        }
        else
          text.append("<li bindent=\"10\" >");

        text.append("<a href=\"").append(url).append("\">")
            .append(entry.getKey()).append("</a></li>");
      }
    }

    text.append("</form>");
    _detailsText.setText(text.toString(), true, true);
  }
}
