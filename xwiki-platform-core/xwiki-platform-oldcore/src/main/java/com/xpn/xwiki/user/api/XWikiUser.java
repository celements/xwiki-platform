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
package com.xpn.xwiki.user.api;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class XWikiUser
{
    /**
     * Class used to store user properties.
     */
    private static final String USER_CLASS = "XWiki.XWikiUsers";

    /**
     * The name of the property that store the active status of the user.
     */
    public static final String ACTIVE_PROPERTY = "active";

    /**
     * The name of the property that store the information if an email was checked for the user.
     */
    public static final String EMAIL_CHECKED_PROPERTY = "email_checked";

    /**
     * @see com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    private ContextualLocalizationManager localization;

    private Logger logger = LoggerFactory.getLogger(XWikiUser.class);

    private String fullName;

    private DocumentReference userReference;

    private boolean main;

    /**
     * Create a XWikiUser from its document reference and infer if the user is global or not based on the wiki part of
     * this reference. See {@link #isMain()} for more information.
     * @param userReference the document reference of the user.
     * @since 11.6RC1
     */
    @Unstable
    public XWikiUser(DocumentReference userReference)
    {
        this(userReference, XWiki.DEFAULT_MAIN_WIKI.equals(userReference.getWikiReference().getName()));
    }

    /**
     * Create a XWikiUser from its document reference and set the main flag. (see {@link #isMain()}).
     * @param userReference the document reference of the user.
     * @param main true if the user is global (i.e. registered in the main wiki)
     * @since 11.6RC1
     */
    @Unstable
    public XWikiUser(DocumentReference userReference, boolean main)
    {
        this.userReference = userReference;
        setUser(getLocalEntityReferenceSerializer().serialize(userReference));
        setMain(main);
    }

    /**
     * Create a XWikiUser for the given user.
     * @param user the full name of the user on the form <tt>XWiki.Foo</tt>.
     * @deprecated since 11.6RC1 use {@link #XWikiUser(DocumentReference)}.
     */
    @Deprecated
    public XWikiUser(String user)
    {
        this(user, false);
    }

    /**
     * Create a XWikiUser for the given user.
     * @param user the full name of the user on the form <tt>XWiki.Foo</tt>.
     * @param main true if the user is global (i.e. registered in the main wiki)
     * @deprecated since 11.6RC1 use {@link #XWikiUser(DocumentReference, boolean)}.
     */
    @Deprecated
    public XWikiUser(String user, boolean main)
    {
        setUser(user);
        setMain(main);
    }

    /**
     *
     * @return user fullname
     * @deprecated since 11.6RC1 use {@link #getFullName()}.
     */
    @Deprecated
    public String getUser()
    {
        return this.fullName;
    }

    /**
     * @return the fullname of the user like <tt>XWiki.Foo</tt>.
     */
    public String getFullName()
    {
        return this.fullName;
    }

    private DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        if (currentMixedDocumentReferenceResolver == null) {
            currentMixedDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        }
        return currentMixedDocumentReferenceResolver;
    }

    private EntityReferenceSerializer<String> getLocalEntityReferenceSerializer()
    {
        if (localEntityReferenceSerializer == null) {
            localEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }
        return localEntityReferenceSerializer;
    }

    private ContextualLocalizationManager getLocalization()
    {
        if (this.localization == null) {
            this.localization = Utils.getComponent(ContextualLocalizationManager.class);
        }

        return this.localization;
    }

    private String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(getLocalization().getTranslationPlain(key, parameters), key);
    }

    public DocumentReference getUserReference()
    {
        if (this.userReference == null) {
            this.userReference = getCurrentMixedDocumentReferenceResolver().resolve(getUser());
        }
        return this.userReference;
    }

    private DocumentReference getUserClassReference(WikiReference userDocWiki)
    {
        return getCurrentMixedDocumentReferenceResolver().resolve(userDocWiki.getName() + ":" + USER_CLASS);
    }

    private boolean isGuest()
    {
        return XWikiRightService.isGuest(getUserReference());
    }

    private boolean isSuperAdmin()
    {
        return XWikiRightService.isSuperAdmin(getUserReference());
    }

    private XWikiDocument getUserDocument(XWikiContext context) throws XWikiException
    {
        return context.getWiki().getDocument(getUserReference(), context);
    }

    /**
     * @param context used to retrieve the user document.
     * @return true if the user is email have been checked before.
     *          This always returns true if the user is the guest or superadmin user.
     * @since 11.8RC1
     */
    @Unstable
    public boolean isEmailChecked(XWikiContext context)
    {
        boolean isChecked;
        // These users are necessarily active. Note that superadmin might be main-wiki-prefixed when in a subwiki.
        if (isGuest() || isSuperAdmin()) {
            isChecked = true;
        } else {
            try {
                XWikiDocument userdoc = getUserDocument(context);
                isChecked = userdoc.getIntValue(getUserClassReference(userdoc.getDocumentReference().getWikiReference()),
                    EMAIL_CHECKED_PROPERTY) != 0;
            } catch (XWikiException e) {
                this.logger.error("Error while checking email_checked status of user [{}]", getUser(), e);
                isChecked = true;
            }
        }
        return isChecked;
    }

    /**
     * @param checked true if the email address was checked for the user. False if it wasn't checked.
     * @param context used to retrieve the user document.
     * @since 11.8RC1
     */
    @Unstable
    public void setEmailChecked(boolean checked, XWikiContext context)
    {
        // We don't modify any information for guest and superadmin.
        if (!isGuest() && !isSuperAdmin()) {
            int checkedFlag = (checked) ? 1 : 0;
            try {
                XWikiDocument userdoc = getUserDocument(context);
                userdoc.setIntValue(getUserClassReference(userdoc.getDocumentReference().getWikiReference()),
                    ACTIVE_PROPERTY, checkedFlag);
                context.getWiki().saveDocument(userdoc, localizePlainOrKey("core.users." +
                    (checked ? "email_checked" : "email_unchecked") + ".saveComment"), context);
            } catch (XWikiException e) {
                this.logger.error("Error while setting email_checked status of user [{}]", getUser(), e);
            }
        }
    }

    /**
     * @param context used to retrieve the user document.
     * @return true if the user is disabled (i.e. its active property is set to 0).
     *         This always returns false if the user is the guest or superadmin user.
     * @since 11.6RC1
     */
    @Unstable
    public boolean isDisabled(XWikiContext context)
    {
        boolean disabled;
        // These users are necessarily active. Note that superadmin might be main-wiki-prefixed when in a subwiki.
        if (isGuest() || isSuperAdmin()) {
            disabled = false;
        } else {
            try {
                XWikiDocument userdoc = getUserDocument(context);
                disabled = userdoc.getIntValue(getUserClassReference(userdoc.getDocumentReference().getWikiReference()),
                    ACTIVE_PROPERTY) == 0;
            } catch (XWikiException e) {
                this.logger.error("Error while checking active status of user [{}]", getUser(), e);
                disabled = false;
            }
        }
        return disabled;
    }

    /**
     * @param disable true if the user disabled the account. False to enable the account.
     * @param context used to retrieve the user document.
     * @since 11.6RC1
     */
    @Unstable
    public void setDisabled(boolean disable, XWikiContext context)
    {
        // We don't modify any information for guest and superadmin.
        if (!isGuest() && !isSuperAdmin()) {
            int activeFlag = (disable) ? 0 : 1;
            try {
                XWikiDocument userdoc = getUserDocument(context);
                userdoc.setIntValue(getUserClassReference(userdoc.getDocumentReference().getWikiReference()),
                    ACTIVE_PROPERTY, activeFlag);
                context.getWiki().saveDocument(userdoc,
                    localizePlainOrKey("core.users." + (disable ? "disable" : "enable") + ".saveComment"), context);
            } catch (XWikiException e) {
                this.logger.error("Error while setting active status of user [{}]", getUser(), e);
            }
        }
    }

    /**
     * @param context used to retrieve the user document.
     * @return true if the user exists.
     * @since 11.6RC1
     */
    @Unstable
    public boolean exists(XWikiContext context)
    {
        boolean exists = false;
        try {
            XWikiDocument userdoc = getUserDocument(context);
            exists = !userdoc.isNew();
        } catch (XWikiException e) {
            this.logger.error("Error while checking existing status of user [{}]", getUser(), e);
        }
        return exists;
    }

    public void setUser(String user)
    {
        this.fullName = user;
    }

    /**
     * Check if the user belongs to a group or not. This method only check direct membership (no recursive checking) in
     * the current wiki.
     *
     * @param groupName The group to check.
     * @param context The current {@link XWikiContext context}.
     * @return <tt>true</tt> if the user does belong to the specified group, false otherwise or if an exception occurs.
     * @throws XWikiException If an error occurs when checking the groups.
     * @since Platform-1.3
     */
    public boolean isUserInGroup(String groupName, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isEmpty(getUser())) {
            XWikiGroupService groupService = context.getWiki().getGroupService(context);

            DocumentReference groupReference = getCurrentMixedDocumentReferenceResolver().resolve(groupName);

            Collection<DocumentReference> groups =
                groupService.getAllGroupsReferencesForMember(getUserReference(), 0, 0, context);

            if (groups.contains(groupReference)) {
                return true;
            }
        }

        return false;
    }

    /**
     * See if the user is global (i.e. registered in the main wiki) or local to a virtual wiki.
     *
     * @return <tt>true</tt> if the user is global, false otherwise or if an exception occurs.
     */
    public boolean isMain()
    {
        return this.main;
    }

    public void setMain(boolean main)
    {
        this.main = main;
    }

    @Override
    public String toString()
    {
        return getUser();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj)) {
            return true;
        }

        boolean equals;
        if (obj instanceof XWikiUser) {
            XWikiUser otherUser = (XWikiUser) obj;
            equals = otherUser.main == this.main && this.fullName.equals(otherUser.fullName);
        } else {
            equals = false;
        }

        return equals;
    }
}
