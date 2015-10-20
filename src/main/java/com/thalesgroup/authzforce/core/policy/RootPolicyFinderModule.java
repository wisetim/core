/**
 * Copyright (C) 2011-2015 Thales Services SAS.
 *
 * This file is part of AuthZForce.
 *
 * AuthZForce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthZForce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthZForce.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.thalesgroup.authzforce.core.policy;

import java.io.Closeable;
import java.io.IOException;

import com.sun.xacml.ParsingException;
import com.thalesgroup.authz.model.ext._3.AbstractPolicyFinder;
import com.thalesgroup.authzforce.core.EvaluationContext;
import com.thalesgroup.authzforce.core.Expression;
import com.thalesgroup.authzforce.core.IndeterminateEvaluationException;
import com.thalesgroup.authzforce.core.JaxbBoundPdpExtension;
import com.thalesgroup.authzforce.core.combining.CombiningAlgRegistry;

/**
 * This is the interface that all modules responsible for finding the root/top-level policy to
 * evaluate.
 * <p>
 * Implements {@link Closeable} because it may may use resources external to the JVM such as a
 * cache, a disk, a connection to a remote server, etc. for retrieving the root policy and any
 * policy referenced by it. Therefore, these resources must be released by calling {@link #close()}
 * when it is no longer needed.
 * 
 */
public abstract class RootPolicyFinderModule implements Closeable
{
	protected final Expression.Factory expressionFactory;
	protected final RefPolicyFinder refPolicyFinder;

	/**
	 * Creates instance
	 * 
	 * @param expressionFactory
	 *            Expression factory
	 * @param refPolicyFinder
	 *            referenced policy finder; null iff Policy references not supported
	 */
	protected RootPolicyFinderModule(Expression.Factory expressionFactory, RefPolicyFinder refPolicyFinder)
	{
		assert expressionFactory != null;
		this.expressionFactory = expressionFactory;
		this.refPolicyFinder = refPolicyFinder;
	}

	/**
	 * RootPolicyFinderModule factory
	 * 
	 * @param <CONF_T>
	 *            type of configuration (XML-schema-derived) of the module (initialization
	 *            parameter)
	 * 
	 */
	public static abstract class Factory<CONF_T extends AbstractPolicyFinder> extends JaxbBoundPdpExtension<CONF_T>
	{
		/**
		 * Create RootPolicyFinderModule instance
		 * 
		 * @param conf
		 *            module configuration
		 * @param expressionFactory
		 *            Expression factory for parsing Expressions in the root policy(set)
		 * @param combiningAlgRegistry
		 *            registry of combining algorithms for instantiating algorithms used in the root
		 *            policy(set)
		 * @param refPolicyFinder
		 *            ReferencedPolicyFinder used for resolving Policy(Set)(Id)References in root
		 *            policy; may be null if support of PolicyReferences is disabled or this
		 *            RootPolicyFinder module already supports these.
		 * 
		 * @return the module instance
		 */
		public abstract RootPolicyFinderModule getInstance(CONF_T conf, Expression.Factory expressionFactory, CombiningAlgRegistry combiningAlgRegistry, RefPolicyFinder refPolicyFinder);
	}

	/**
	 * Tries to find one and only one matching policy given the request represented by the context
	 * data. If no policies are found, null must be returned.
	 * 
	 * @param context
	 *            the representation of the request
	 * 
	 * @return the result of looking for a matching policy, null if none found matching the request
	 * @throws ParsingException
	 *             Error parsing a policy before matching. The policy finder module may parse
	 *             policies lazily or on the fly, i.e. only when the policies are requested/looked
	 *             for.
	 * @throws IndeterminateEvaluationException
	 *             if error determining the one policy matching the {@code context}, e.g. if more
	 *             than one policy is found
	 */
	public abstract IPolicy findPolicy(EvaluationContext context) throws IndeterminateEvaluationException, ParsingException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.thalesgroup.authzforce.core.policy.RootPolicyFinderModule#invalidateCache()
	 */
	@Override
	public void close() throws IOException
	{
		expressionFactory.close();
		if (refPolicyFinder != null)
		{
			refPolicyFinder.close();
		}
	}

	/**
	 * Root policy finder module that resolves statically the root policy when it is initialized,
	 * i.e. it is context-independent. Concretely, this means for any given context:
	 * 
	 * <pre>
	 * this.findPolicy(context) == this.findPolicy(null)
	 * </pre>
	 */
	public static abstract class Static extends RootPolicyFinderModule
	{
		protected Static(Expression.Factory defaultExpressionFactory, RefPolicyFinder refPolicyFinder)
		{
			super(defaultExpressionFactory, refPolicyFinder);
		}

		/**
		 * Get the statically resolved root policy
		 * 
		 * @return root policy
		 */
		public abstract IPolicy getRootPolicy();

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.thalesgroup.authzforce.core.policy.RootPolicyFinderModule#findPolicy(com.thalesgroup
		 * .authzforce.core.test .EvaluationCtx)
		 */
		@Override
		public final IPolicy findPolicy(EvaluationContext context) throws IndeterminateEvaluationException, ParsingException
		{
			return getRootPolicy();
		}

	}

}