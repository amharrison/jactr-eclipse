package org.jactr.eclipse.runtime.ui.sync;

/*
 * default logging
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonreality.net.handler.IMessageHandler;
import org.commonreality.net.session.ISessionInfo;
import org.jactr.eclipse.runtime.launching.norm.ACTRSession;
import org.jactr.eclipse.runtime.launching.session.AbstractSession;
import org.jactr.eclipse.runtime.session.ISession;
import org.jactr.eclipse.runtime.session.impl.Session2SessionAdapter;
import org.jactr.eclipse.runtime.session.manager.ISessionManagerListener;
import org.jactr.eclipse.ui.UIPlugin;
import org.jactr.eclipse.ui.concurrent.UIJobExecutor;
import org.jactr.tools.async.sync.SynchronizationMessage;

public class SynchronizationSessionListener implements ISessionManagerListener
{
  /**
   * Logger definition
   */
  static private final transient Log LOGGER = LogFactory
      .getLog(SynchronizationSessionListener.class);

  public void sessionAdded(ISession session)
  {
    try
    {
      ACTRSession actr = getACTRSession(session);
      if (actr != null) // we haven't connected yet..
      actr.getShadowController().getDefaultHandlers().put(
          SynchronizationMessage.class,
          new IMessageHandler<SynchronizationMessage>() {

            @Override
            public void accept(ISessionInfo<?> t, SynchronizationMessage u)
            {
              synchronizationMessageReceived(t, u);
            }
          });
    }
    catch (Exception e)
    {
      UIPlugin.log(e);
    }

  }

  protected void synchronizationMessageReceived(ISessionInfo<?> session,
      SynchronizationMessage message)
  {
    /*
     * a round about message and response to mark us as having processed the
     * data up to this point.
     */
    if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Synchronizing"));

    Runnable synch = new Runnable() {
      public void run()
      {
        try
        {
          if (session.isConnected()) session.write(new SynchronizationMessage(message));
        }
        catch (Exception e)
        {
          LOGGER.error(e);
        }
      }
    };
    UIJobExecutor executor = new UIJobExecutor("Synchronizing with runtime");
    executor.execute(synch);
//    UIPlugin.getDefault().getLog().info("Synchronizing with runtime");

    // run this on the display thread when we get a chance.
//    PlatformUI.getWorkbench().getDisplay().asyncExec(synch);
  }

  public void sessionRemoved(ISession session)
  {
    // TODO Auto-generated method stub

  }

  protected ACTRSession getACTRSession(ISession session)
  {
    if (session instanceof Session2SessionAdapter)
    {
      Session2SessionAdapter s2sa = (Session2SessionAdapter) session;
      AbstractSession as = s2sa.getOldSession();
      if (as instanceof ACTRSession)
      {
        ACTRSession actr = (ACTRSession) as;
        return actr;
      }
    }
    return null;
  }

}
