/*
 * Created on Jul 8, 2006 Copyright (C) 2001-5, Anthony Harrison anh23@pitt.edu
 * (jactr.org) This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version. This library is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jactr.eclipse.ui.editor;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jactr.eclipse.ui.editor.config.ACTRSourceViewerConfiguration;
import org.jactr.eclipse.ui.editor.config.LispSourceViewerConfiguration;
import org.jactr.eclipse.ui.editor.document.LispDocumentProvider;

public class LispModelEditor extends ACTRModelEditor
{
  
  LispDocumentProvider              _provider;
  
  
  @Override
  protected ACTRSourceViewerConfiguration createSourceViewerConfiguration()
  {
    return new LispSourceViewerConfiguration(this);
  }

  @Override
  public IDocumentProvider getDocumentProvider()
  {
    if (_provider == null) _provider = new LispDocumentProvider();
    return _provider;
  }
}