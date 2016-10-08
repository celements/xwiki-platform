/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.security.authorization;

import java.util.concurrent.Callable;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Allow executing some code with the right of a provided user.
 *
 * @version $Id$
 * @since 8.3RC1
 */
@Role
@Unstable
public interface AuthorExecutor
{
    /**
     * Execute the passed {@link Callable} with the rights of the passed user.
     *
     * @param callable the the task to execute
     * @param authorReference the user to check rights on
     * @return computed result
     * @throws Exception if unable to compute a result
     * @param <V> the result type of method <tt>call</tt>
     */
    <V> V call(Callable<V> callable, DocumentReference authorReference) throws Exception;

    /**
     * Setup the context so that following code is executed with provided user rights.
     * 
     * <pre>
     * {@code
     * try (AutoCloseable context = this.executor.before(author)) {
     *   ...
     * }
     * }
     * </pre>
     *
     * @param authorReference the user to check rights on
     * @return the context to restore
     * @see #after(AutoCloseable)
     */
    AutoCloseable before(DocumentReference authorReference);

    /**
     * Restore the context to it's previous state as defined by the provided {@link AutoCloseable}.
     *
     * @param context the context to restore
     * @see #before(DocumentReference)
     */
    void after(AutoCloseable context);
}
