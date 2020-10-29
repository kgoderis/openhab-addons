/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.internal.ets.profiles;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.osgi.service.component.annotations.Component;

/**
 * A factory and advisor for KNX profiles.
 *
 * This {@link ProfileFactory} implementation handles all KNX {@link Profile}s.
 *
 * @author Karel Goderis - Initial contribution
 *
 */
@NonNullByDefault
@Component(service = ProfileFactory.class)
public class KNXProfileFactory implements ProfileFactory, ProfileTypeProvider {

    private static final Set<ProfileType> SUPPORTED_PROFILE_TYPES = Stream
            .of(KNXProfiles.CONTROL_TYPE, KNXProfiles.LISTEN_TYPE).collect(Collectors.toSet());

    private static final Set<ProfileTypeUID> SUPPORTED_PROFILE_TYPE_UIDS = Stream
            .of(KNXProfiles.CONTROL, KNXProfiles.LISTEN).collect(Collectors.toSet());

    @Nullable
    @Override
    public Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext context) {
        if (KNXProfiles.CONTROL.equals(profileTypeUID)) {
            return new KNXControlProfile(callback);
        } else if (KNXProfiles.LISTEN.equals(profileTypeUID)) {
            return new KNXListenProfile(callback);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return SUPPORTED_PROFILE_TYPES;
    }

    @Override
    public @NonNull Collection<@NonNull ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return SUPPORTED_PROFILE_TYPE_UIDS;
    }
}
