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
package com.xpn.xwiki.store;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.isCacheConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Validate {@link XWikiCacheStore} behavior.
 *
 * @version $Id: 69a6ba5eb309a545f911ab9c29af9f57be80b28b $
 */
@ComponentList({
    UidStringEntityReferenceSerializer.class,
    DefaultModelConfiguration.class,
    CurrentStringDocumentReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    DefaultSymbolScheme.class
})
public class XWikiCacheStoreTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private Cache<XWikiDocument> cache;

    private Cache<Boolean> existCache;

    @Before
    public void before() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(RemoteObservationManagerContext.class);
        this.oldcore.getMocker().registerMockComponent(ObservationManager.class);

        CacheManager cacheManager = this.oldcore.getMocker().registerMockComponent(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.<XWikiDocument>createNewCache(isCacheConfiguration("xwiki.store.pagecache")))
            .thenReturn(cache);
        existCache = mock(Cache.class);
        when(cacheManager.<Boolean>createNewCache(isCacheConfiguration("xwiki.store.pageexistcache")))
            .thenReturn(existCache);
    }

    @Test
    public void testLoadXWikiDoc() throws Exception
    {
        // Set current wiki
        this.oldcore.getXWikiContext().setWikiId("wiki");

        // Save a document
        DocumentReference reference = new DocumentReference("otherwiki", "space", "page");
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(reference), this.oldcore.getXWikiContext());

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        XWikiDocument existingDocument =
            store.loadXWikiDoc(new XWikiDocument(reference), this.oldcore.getXWikiContext());

        assertFalse(existingDocument.isNew());
        verify(this.cache).set(eq("4:wiki5:space4:page"), any(XWikiDocument.class));
        verify(this.existCache).set("4:wiki5:space4:page", Boolean.TRUE);
        verify(this.cache).get(anyString());
        verify(this.existCache).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);

        XWikiDocument notExistingDocument = store.loadXWikiDoc(
            new XWikiDocument(new DocumentReference("otherwiki", "space", "nopage")), this.oldcore.getXWikiContext());

        assertTrue(notExistingDocument.isNew());

        // Make sure only the existing document has been put in the cache
        verify(this.cache).set(eq("4:wiki5:space4:page"), any(XWikiDocument.class));
        verify(this.existCache).set("4:wiki5:space4:page", Boolean.TRUE);
        verify(this.existCache).set("4:wiki5:space6:nopage", Boolean.FALSE);
        verify(this.cache, times(2)).get(anyString());
        verify(this.existCache, times(2)).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }

    @Test
    @Deprecated
    public void testGetKey_XWikiDocument() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId("xwikiDB");
        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        DocumentReference docRef = new DocumentReference("testWiki", "space", "page");
        XWikiDocument theDoc = new XWikiDocument(docRef);
        assertEquals("8:testWiki5:space4:page", store.getKey(theDoc));
    }

    @Test
    public void testGetKey_XWikiDocument_Context() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId("xwikiDB");
        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        DocumentReference docRef = new DocumentReference("testWiki", "space", "page");
        XWikiDocument theDoc = new XWikiDocument(docRef);
        assertEquals("7:xwikiDB5:space4:page", store.getKey(theDoc, context));
    }

    @Test
    @Deprecated
    public void testGetKey_XWikiDocument_String_Context() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId("xwikiDB");
        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        assertEquals("7:xwikiDB5:space4:page2:de", store.getKey("testWiki:space.page", "de", context));
    }

    @Test
    @Deprecated
    public void testGetKey_Strings() throws Exception
    {
        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), this.oldcore.getXWikiContext());

        assertEquals("7:xwikiDB5:space4:page2:de", store.getKey("xwikiDB", "testWiki:space.page", "de"));
    }

    @Test
    public void testLoadXWikiDoc_wikiIdFromContext() throws Exception
    {
        String wikiId = "xwikiDB";
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId(wikiId);
        // Save a document
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(reference), context);

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), context);

        XWikiDocument existingDocument = store.loadXWikiDoc(new XWikiDocument(reference), context);
        String cacheKey = store.getKey(existingDocument, context);

        assertFalse(existingDocument.isNew());
        // Make sure the existing document has been put in the cache with the correct cache key
        verify(this.cache).set(eq(cacheKey), any(XWikiDocument.class));
        verify(this.existCache).set(cacheKey, Boolean.TRUE);
        verify(this.cache).get(anyString());
        verify(this.existCache).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }

    @Test
    public void testSaveDocument_wikiIdFromContext() throws Exception
    {
        String wikiId = "xwikiDB";
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId(wikiId);
        // Save a document
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), context);

        store.saveXWikiDoc(new XWikiDocument(reference), context);
        String cacheKey = store.getKey(new XWikiDocument(reference), context);

        // Make sure the old values has been removed from the cache with the correct cache key
        verify(this.cache).remove(eq(cacheKey));
        verify(this.existCache).remove(cacheKey);

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }

    @Test
    public void testExists_wikiIdFromContext() throws Exception
    {
        String wikiId = "xwikiDB";
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setWikiId(wikiId);

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        XWikiDocument doc = new XWikiDocument(reference);
        when(this.oldcore.getMockStore().exists(doc, context)).thenReturn(true);

        XWikiCacheStore store = new XWikiCacheStore(this.oldcore.getMockStore(), context);

        boolean exists = store.exists(doc, context);
        String cacheKey = store.getKey(new XWikiDocument(reference), context);

        assertTrue(exists);
        // Make sure the exists value has been put in the cache with the correct cache key
        verify(this.existCache).set(cacheKey, Boolean.TRUE);
        verify(this.existCache).get(anyString());

        verifyNoMoreInteractions(this.cache);
        verifyNoMoreInteractions(this.existCache);
    }

}
