/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brixcms.web.seam.https;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.https.HttpsConfig;
import org.apache.wicket.protocol.https.RequireHttps;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.component.IBookmarkablePageRequestTarget;
import org.apache.wicket.request.target.component.IPageRequestTarget;
import org.jboss.seam.wicket.SeamWebRequestCycleProcessor;

/**
 * Created by IntelliJ IDEA.
 * User: korbinianbachl
 * Date: 21.10.11
 * Time: 11:44
 */
public class SeamHttpsRequestCylceProcessor extends SeamWebRequestCycleProcessor
{
	private final HttpsConfig portConfig;

	/**
	 * Constructor
	 *
	 * @param httpsConfig
	 *            configuration
	 */
	public SeamHttpsRequestCylceProcessor(HttpsConfig httpsConfig)
	{
        super();
		portConfig = httpsConfig;
	}

	/**
	 * @return configuration
	 */
	public HttpsConfig getConfig()
	{
		return portConfig;
	}

	/**
	 * Checks if the class has a {@link org.apache.wicket.protocol.https.RequireHttps} annotation
	 *
	 * @param klass
	 * @return true if klass has the annotation
	 */
	private boolean hasSecureAnnotation(Class<?> klass)
	{
		for (Class<?> c : klass.getInterfaces())
		{
			if (hasSecureAnnotation(c))
			{
				return true;
			}
		}
		if (klass.getAnnotation(RequireHttps.class) != null)
		{
			return true;
		}
		if (klass.getSuperclass() != null)
		{
			return hasSecureAnnotation(klass.getSuperclass());
		}
		else
		{
			return false;
		}
	}

	/**
	 * Gets page class from a request target
	 *
	 * @param target
	 * @return page class if there is one, null otherwise
	 */
	private Class<?> getPageClass(IRequestTarget target)
	{
		if (target instanceof IPageRequestTarget)
		{
			return ((IPageRequestTarget)target).getPage().getClass();
		}
		else if (target instanceof IBookmarkablePageRequestTarget)
		{
			return ((IBookmarkablePageRequestTarget)target).getPageClass();
		}
		else
		{
			return null;
		}
	}

	/** @deprecated use checkSecureIncoming */
	@Deprecated
	protected IRequestTarget checkSecure(IRequestTarget target)
	{
		return checkSecureIncoming(target);
	}

	protected IRequestTarget checkSecureIncoming(IRequestTarget target)
	{

		if (target != null && target instanceof SeamSwitchProtocolRequestTarget)
		{
			return target;
		}
		if (portConfig == null)
		{
			return target;
		}

		Class<?> pageClass = getPageClass(target);
		if (pageClass != null)
		{
			IRequestTarget redirect = null;
			if (hasSecureAnnotation(pageClass))
			{
				redirect = SeamSwitchProtocolRequestTarget.requireProtocol(SeamSwitchProtocolRequestTarget.Protocol.HTTPS);
			}
			else
			{
				redirect = SeamSwitchProtocolRequestTarget.requireProtocol(SeamSwitchProtocolRequestTarget.Protocol.HTTP);
			}
			if (redirect != null)
			{
				return redirect;
			}

		}
		return target;
	}

	protected IRequestTarget checkSecureOutgoing(IRequestTarget target)
	{

		if (target != null && target instanceof SeamSwitchProtocolRequestTarget)
		{
			return target;
		}
		if (portConfig == null)
		{
			return target;
		}

		Class<?> pageClass = getPageClass(target);
		if (pageClass != null)
		{
			IRequestTarget redirect = null;
			if (hasSecureAnnotation(pageClass))
			{
				redirect = SeamSwitchProtocolRequestTarget.requireProtocol(SeamSwitchProtocolRequestTarget.Protocol.HTTPS, target);
			}
			else
			{
				redirect = SeamSwitchProtocolRequestTarget.requireProtocol(SeamSwitchProtocolRequestTarget.Protocol.HTTP, target);
			}
			if (redirect != null)
			{
				return redirect;
			}

		}
		return target;
	}


	/** {@inheritDoc} */
	@Override
	public IRequestTarget resolve(RequestCycle rc, RequestParameters rp)
	{
		if (portConfig != null && portConfig.isPreferStateful())
		{
			// we need to persist the session before a redirect to https so the session lasts across
			// both http and https calls.
			Session.get().bind();
		}

		IRequestTarget target = super.resolve(rc, rp);
		return checkSecure(target);
	}

	/** {@inheritDoc} */
	@Override
	public void respond(RequestCycle requestCycle)
	{
		IRequestTarget requestTarget = requestCycle.getRequestTarget();
		if (requestTarget != null)
		{
			IRequestTarget secured = checkSecureOutgoing(requestTarget);
			if (secured != requestTarget)
			{
				requestCycle.setRequestTarget(secured);
				// respond will be called again because we called setrequesttarget(), so we do not
				// process it this time
				return;
			}
		}
		super.respond(requestCycle);
	}
}