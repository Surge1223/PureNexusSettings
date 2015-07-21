/*
 * Copyright (C) 2015 The Pure Nexus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.purenexussettings;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.content.ContentResolver;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.purenexussettings.preferences.BaseSystemSettingSwitchBar;
import com.android.purenexussettings.preferences.BaseSystemSettingSwitchBar.SwitchBarChangeCallback;
import com.android.purenexussettings.preferences.SwitchBar;

public class NavigationBarFragment extends PreferenceFragment implements SwitchBarChangeCallback, OnPreferenceChangeListener {
    private static final String NAVIGATION_BAR = "navigation_bar_edit";
    private static final String NAVIGATION_BAR_DIMEN = "navigation_bar_dimen";
    private static final String NAVIGATION_BAR_SHOW = "navigation_bar_show";

    // kill-app long press back
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    // Navigation bar left
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";

    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String STATUS_BAR_IME_SWITCHER = "status_bar_ime_switcher";

    // kill-app long press back
    private SwitchPreference mKillAppLongPressBack;

    private Preference mNavBar;
    private Preference mNavDimen;

    private ContentResolver resolver;

    private BaseSystemSettingSwitchBar mEnabledSwitch;

    private SwitchBar mSwitchBar;
    private ViewGroup mPrefsContainer;
    private View mDisabledText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navbar_fragment_prefs);

        resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory navbarCategory = (PreferenceCategory) prefScreen.findPreference(CATEGORY_NAVBAR);

        mNavBar = prefScreen.findPreference(NAVIGATION_BAR);
        mNavDimen = prefScreen.findPreference(NAVIGATION_BAR_DIMEN);

        // kill-app long press back
        mKillAppLongPressBack = (SwitchPreference) findPreference(KILL_APP_LONGPRESS_BACK);
        mKillAppLongPressBack.setOnPreferenceChangeListener(this);
        int killAppLongPressBack = Settings.Secure.getInt(resolver, KILL_APP_LONGPRESS_BACK, 0);
        mKillAppLongPressBack.setChecked(killAppLongPressBack != 0);

        // Enable or disable NavbarImeSwitcher based on boolean: config_show_cmIMESwitcher
        //boolean showCmImeSwitcher = getResources().getBoolean(com.android.internal.R.bool.config_show_cmIMESwitcher);
        boolean showCmImeSwitcher = true;
        if (!showCmImeSwitcher) {
            Preference pref = findPreference(STATUS_BAR_IME_SWITCHER);
            if (pref != null) {
                navbarCategory.removePreference(pref);
            }
        }

        // remove if tablet
        if ((getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            navbarCategory.removePreference(findPreference(KEY_NAVIGATION_BAR_LEFT));
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.navbar_fragment, container, false);
        mSwitchBar = (SwitchBar) v.findViewById(R.id.switch_bar);
        mPrefsContainer = (ViewGroup) v.findViewById(R.id.prefs_container);
        mDisabledText = v.findViewById(R.id.disabled_text);

        View prefs = super.onCreateView(inflater, mPrefsContainer, savedInstanceState);

        if (prefs != null) {
            mPrefsContainer.addView(prefs);
        }

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        final TinkerActivity activity = (TinkerActivity) getActivity();
        mEnabledSwitch = new BaseSystemSettingSwitchBar(activity, mSwitchBar, NAVIGATION_BAR_SHOW, true, this);
    }

    public NavigationBarFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        final TinkerActivity activity = (TinkerActivity) getActivity();
        if (mEnabledSwitch != null) {
            mEnabledSwitch.resume(activity);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {

        // kill-app long press back
        if (preference == mKillAppLongPressBack) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(resolver, KILL_APP_LONGPRESS_BACK, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEnabledSwitch != null) {
            mEnabledSwitch.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mEnabledSwitch != null) {
            mEnabledSwitch.teardownSwitchBar();
        }
    }

    private boolean getNavBarState() {
        return Settings.System.getInt(getActivity().getContentResolver(), NAVIGATION_BAR_SHOW, 1) != 0;
    }

    private void setNavBarState(int val) {
        Settings.System.putInt(getActivity().getContentResolver(), NAVIGATION_BAR_SHOW, val);
    }

    private void updateEnabledState() {
        mPrefsContainer.setVisibility(getNavBarState() ? View.VISIBLE : View.GONE);
        mDisabledText.setVisibility(getNavBarState() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        setNavBarState(getNavBarState() ? 1 : 0);
        updateEnabledState();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, @NonNull Preference pref) {
        if (pref == mNavBar) {
            ((TinkerActivity)getActivity()).displayNavBar();

            return true;
        }

        if (pref == mNavDimen) {
            ((TinkerActivity)getActivity()).displayNavDimen();

            return true;
        }

        return false;
    }
}