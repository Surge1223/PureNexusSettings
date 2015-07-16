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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class NavBarDimen extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String PREF_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String PREF_NAVIGATION_BAR_WIDTH = "navigation_bar_width";
    private static final String NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String NAVIGATION_BAR_WIDTH = "navigation_bar_width";

    ListPreference mNavigationBarHeight;
    ListPreference mNavigationBarHeightLandscape;
    ListPreference mNavigationBarWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navbardimen_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mNavigationBarHeight = (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        mNavigationBarHeightLandscape = (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE);

        // checks if phone or not
        if ( (getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE ) {
            prefSet.removePreference(mNavigationBarHeightLandscape);
            mNavigationBarHeightLandscape = null;
        } else {
            mNavigationBarHeightLandscape.setOnPreferenceChangeListener(this);
        }

        mNavigationBarWidth = (ListPreference) findPreference(PREF_NAVIGATION_BAR_WIDTH);

        // checks if tablet or not
        if ( (getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE ) {
            prefSet.removePreference(mNavigationBarWidth);
            mNavigationBarWidth = null;
        } else {
            mNavigationBarWidth.setOnPreferenceChangeListener(this);
        }

        updateDimensionValues();
    }

    private void updateDimensionValues() {
        int resID = 0;

        int navigationBarHeight = Settings.System.getInt(getActivity().getContentResolver(), NAVIGATION_BAR_HEIGHT, -1);
        if (navigationBarHeight == -1) {
            resID = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            navigationBarHeight = (int) (getResources().getDimensionPixelSize(resID) / getResources().getDisplayMetrics().density);
        }

        mNavigationBarHeight.setValue(String.valueOf(navigationBarHeight));
        mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());

        if (mNavigationBarHeightLandscape != null) {
            int navigationBarHeightLandscape = Settings.System.getInt(getActivity().getContentResolver(), NAVIGATION_BAR_HEIGHT_LANDSCAPE, -1);
            if (navigationBarHeightLandscape == -1) {
                resID = getResources().getIdentifier("navigation_bar_height_landscape", "dimen", "android");
                navigationBarHeightLandscape = (int) (getResources().getDimensionPixelSize(resID) / getResources().getDisplayMetrics().density);
            }
            mNavigationBarHeightLandscape.setValue(String.valueOf(navigationBarHeightLandscape));
            mNavigationBarHeightLandscape.setSummary(mNavigationBarHeightLandscape.getEntry());
        }

        if (mNavigationBarWidth != null) {
            int navigationBarWidth = Settings.System.getInt(getActivity().getContentResolver(), NAVIGATION_BAR_WIDTH, -1);
            if (navigationBarWidth == -1) {
                resID = getResources().getIdentifier("navigation_bar_width", "dimen", "android");
                navigationBarWidth = (int) (getResources().getDimensionPixelSize(resID) / getResources().getDisplayMetrics().density);
            }
            mNavigationBarWidth.setValue(String.valueOf(navigationBarWidth));
            mNavigationBarWidth.setSummary(mNavigationBarWidth.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavigationBarWidth) {
            Settings.System.putInt(getActivity().getContentResolver(), NAVIGATION_BAR_WIDTH, Integer.parseInt((String) newValue));
            updateDimensionValues();
            return true;
        } else if (preference == mNavigationBarHeight) {
            Settings.System.putInt(getActivity().getContentResolver(), NAVIGATION_BAR_HEIGHT, Integer.parseInt((String) newValue));
            updateDimensionValues();
            return true;
        } else if (preference == mNavigationBarHeightLandscape) {
            Settings.System.putInt(getActivity().getContentResolver(), NAVIGATION_BAR_HEIGHT_LANDSCAPE, Integer.parseInt((String) newValue));
            updateDimensionValues();
            return true;
        }
        return false;
    }
}
