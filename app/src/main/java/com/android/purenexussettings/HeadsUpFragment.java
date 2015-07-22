/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.purenexussettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.android.purenexussettings.preferences.BaseSystemSettingSwitchBar;
import com.android.purenexussettings.preferences.SwitchBar;
import com.android.purenexussettings.utils.PackageListAdapter;
import com.android.purenexussettings.utils.PackageListAdapter.PackageItem;
import com.android.purenexussettings.preferences.BaseSystemSettingSwitchBar.SwitchBarChangeCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadsUpFragment extends PreferenceFragment implements SwitchBarChangeCallback, OnItemLongClickListener, OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final String PREF_HEADS_UP_TIME_OUT = "heads_up_time_out";
    private static final String HEADS_UP_NOTIFCATION_DECAY = "heads_up_notification_decay";
    private static final String HEADS_UP_USER_ENABLED = "heads_up_user_enabled";
    private static final String HEADS_UP_CUSTOM_VALUES = "heads_up_custom_values";
    private static final String HEADS_UP_BLACKLIST_VALUES = "heads_up_blacklist_values";

    private static final int HEADS_UP_USER_OFF = 0;
    private static final int HEADS_UP_USER_ON = 1;

    private ListPreference mHeadsUpTimeOut;

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mDndPrefList;
    private PreferenceGroup mBlacklistPrefList;
    private Preference mAddDndPref;
    private Preference mAddBlacklistPref;

    private String mDndPackageList;
    private String mBlacklistPackageList;
    private Map<String, Package> mDndPackages;
    private Map<String, Package> mBlacklistPackages;

    private BaseSystemSettingSwitchBar mEnabledSwitch;

    private ViewGroup mPrefsContainer;
    private View mDisabledText;
    private SwitchBar mSwitchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get launch-able applications
        addPreferencesFromResource(R.xml.headsup_fragment_prefs);
        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());

        Resources systemUiResources;
        try {
            systemUiResources = mPackageManager.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier("com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getActivity().getContentResolver(), HEADS_UP_NOTIFCATION_DECAY, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

        mDndPrefList = (PreferenceGroup) findPreference("dnd_applications_list");
        mDndPrefList.setOrderingAsAdded(false);

        mBlacklistPrefList = (PreferenceGroup) findPreference("blacklist_applications");
        mBlacklistPrefList.setOrderingAsAdded(false);

        mDndPackages = new HashMap<String, Package>();
        mBlacklistPackages = new HashMap<String, Package>();

        mAddDndPref = findPreference("add_dnd_packages");
        mAddBlacklistPref = findPreference("add_blacklist_packages");

        mAddDndPref.setOnPreferenceClickListener(this);
        mAddBlacklistPref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.headsup_fragment, container, false);
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
        mEnabledSwitch = new BaseSystemSettingSwitchBar(activity, mSwitchBar, HEADS_UP_USER_ENABLED, true, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        final TinkerActivity activity = (TinkerActivity) getActivity();
        if (mEnabledSwitch != null) {
            mEnabledSwitch.resume(activity);
        }

        refreshCustomApplicationPrefs();
        getListView().setOnItemLongClickListener(this);
        getActivity().invalidateOptionsMenu();

        // If running on a phone, remove padding around container
        // and the preference listview
        if ((getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
            mPrefsContainer.setPadding(0, 0, 0, 0);
            getListView().setPadding(0, 0, 0, 0);
        }
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

    /**
     * Utility classes and supporting methods
     */
    public static class MyDialogFragment extends DialogFragment
    {
        private boolean showBlackList;
        private Fragment fragBase;

        public MyDialogFragment() {}

        public void setVals(Fragment orig, boolean isBlackList) {
            showBlackList = isBlackList;
            fragBase = orig;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final Dialog dialog;
            final ListView list = new ListView(getActivity());
            list.setAdapter(((HeadsUpFragment) fragBase).mPackageAdapter);

            builder.setTitle(R.string.choose_app);
            builder.setView(list);
            dialog = builder.create();

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                    ((HeadsUpFragment) fragBase).addCustomApplicationPref(info.packageName, showBlackList ? ((HeadsUpFragment) fragBase).mBlacklistPackages : ((HeadsUpFragment) fragBase).mDndPackages);
                    dialog.cancel();
                }
            });

            return dialog;
        }
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        /**
         * Stores all the application values in one call
         * @param name
         */
        public Package(String name) {
            this.name = name;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }

            try {
                return new Package(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    private void refreshCustomApplicationPrefs() {
        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mDndPrefList != null && mBlacklistPrefList != null) {
            mDndPrefList.removeAll();
            mBlacklistPrefList.removeAll();

            for (Package pkg : mDndPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mDndPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            for (Package pkg : mBlacklistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mBlacklistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddDndPref.setOrder(0);
        mAddBlacklistPref.setOrder(0);
        // Add 'add' options
        mDndPrefList.addPreference(mAddDndPref);
        mBlacklistPrefList.addPreference(mAddBlacklistPref);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(), HEADS_UP_NOTIFCATION_DECAY, headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        }
        return false;
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary, value / 1000);
        if (value == 0) {
            mHeadsUpTimeOut.setSummary(getResources().getString(R.string.heads_up_time_out_never_summary));
        } else {
            mHeadsUpTimeOut.setSummary(summary);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddDndPref) {
            MyDialogFragment myDiag = new MyDialogFragment();
            myDiag.setVals(this, false);
            myDiag.show(getFragmentManager(), "DnD");
        }

        if (preference == mAddBlacklistPref) {
            MyDialogFragment myDiag = new MyDialogFragment();
            myDiag.setVals(this, true);
            myDiag.show(getFragmentManager(), "Black");
        }
        return true;
    }

    private void addCustomApplicationPref(String packageName, Map<String,Package> map) {
        Package pkg = map.get(packageName);
        if (pkg == null) {
            pkg = new Package(packageName);
            map.put(packageName, pkg);
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private Preference createPreferenceFromInfo(Package pkg) throws PackageManager.NameNotFoundException {
        PackageInfo info = mPackageManager.getPackageInfo(pkg.name, PackageManager.GET_META_DATA);
        Preference pref = new Preference(getActivity());

        pref.setKey(pkg.name);
        pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
        pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
        pref.setPersistent(false);
        return pref;
    }

    private void removeApplicationPref(String packageName, Map<String,Package> map) {
        if (map.remove(packageName) != null) {
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        boolean parsed = false;

        final String dndString = Settings.System.getString(getActivity().getContentResolver(), HEADS_UP_CUSTOM_VALUES);
        final String blacklistString = Settings.System.getString(getActivity().getContentResolver(), HEADS_UP_BLACKLIST_VALUES);

        if (!TextUtils.equals(mDndPackageList, dndString)) {
            mDndPackageList = dndString;
            mDndPackages.clear();
            parseAndAddToMap(dndString, mDndPackages);
            parsed = true;
        }

        if (!TextUtils.equals(mBlacklistPackageList, blacklistString)) {
            mBlacklistPackageList = blacklistString;
            mBlacklistPackages.clear();
            parseAndAddToMap(blacklistString, mBlacklistPackages);
            parsed = true;
        }

        return parsed;
    }

    private void parseAndAddToMap(String baseString, Map<String,Package> map) {
        if (baseString == null) {
            return;
        }

        final String[] array = TextUtils.split(baseString, "\\|");
        for (String item : array) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            Package pkg = Package.fromString(item);
            if (pkg != null) {
                map.put(pkg.name, pkg);
            }
        }
    }

    private void savePackageList(boolean preferencesUpdated, Map<String,Package> map) {
        String setting = map == mDndPackages ? HEADS_UP_CUSTOM_VALUES : HEADS_UP_BLACKLIST_VALUES;

        List<String> settings = new ArrayList<String>();
        for (Package app : map.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            if (TextUtils.equals(setting, HEADS_UP_CUSTOM_VALUES)) {
                mDndPackageList = value;
            } else {
                mBlacklistPackageList = value;
            }
        }
        Settings.System.putString(getActivity().getContentResolver(), setting, value);
    }

    private boolean getUserHeadsUpState() {
        return Settings.System.getIntForUser(getActivity().getContentResolver(), HEADS_UP_USER_ENABLED, HEADS_UP_USER_ON, UserHandle.USER_CURRENT) != 0;
    }

    private void setUserHeadsUpState(int val) {
        Settings.System.putIntForUser(getActivity().getContentResolver(), HEADS_UP_USER_ENABLED, val, UserHandle.USER_CURRENT);
    }

    private void updateEnabledState() {
        mPrefsContainer.setVisibility(getUserHeadsUpState() ? View.VISIBLE : View.GONE);
        mDisabledText.setVisibility(getUserHeadsUpState() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        setUserHeadsUpState(getUserHeadsUpState() ? 1 : 0);
        updateEnabledState();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(position);

        if ((mBlacklistPrefList.findPreference(pref.getKey()) != pref) && (mDndPrefList.findPreference(pref.getKey()) != pref)) {
            return false;
        }

        if (mAddDndPref == pref || mAddBlacklistPref == pref) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBlacklistPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mBlacklistPackages);
                        } else if (mDndPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mDndPackages);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }
}
