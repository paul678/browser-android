package com.linkbubble.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

import java.util.TreeMap;

/*
 * This class exists solely because Android's PreferenceScreen implementation doesn't do anything
 * when the Up button is touched, and we need to go back in that case given our use of the Up button.
 */
public class SettingsDefaultAppsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        setContentView(R.layout.activity_settings_default_apps);
        setTitle(R.string.preference_more_title);

        // This is a bit messy, but setDisplayHomeAsUpEnabled() for the Home button press event to be received, so
        // enable it but tell it not to draw. Then we can have L's nice looking Up button.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View view = Util.getActionBarUpView(this);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

    static public class SettingsDefaultAppsFragment extends SettingsBaseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_default_apps);

            PreferenceScreen rootPreferenceScreen = (PreferenceScreen) findPreference("preference_default_apps_root");

            Preference defaultBrowserPreference = findPreference(Settings.PREFERENCE_DEFAULT_BROWSER);
            defaultBrowserPreference.setSummary(Settings.get().getDefaultBrowserLabel());
            Drawable defaultBrowserIcon = Settings.get().getDefaultBrowserIcon(getActivity());
            if (defaultBrowserIcon != null) {
                setPreferenceIcon(defaultBrowserPreference, defaultBrowserIcon);
            }
            defaultBrowserPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    AlertDialog alertDialog = ActionItem.getDefaultBrowserAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                        @Override
                        public void onSelected(ActionItem actionItem) {
                            Settings.get().setDefaultBrowser(actionItem.getLabel(), actionItem.mPackageName);
                            preference.setSummary(Settings.get().getDefaultBrowserLabel());
                            Drawable defaultBrowserIcon = Settings.get().getDefaultBrowserIcon(getActivity());
                            if (defaultBrowserIcon != null) {
                                setPreferenceIcon(preference, defaultBrowserIcon);
                            }
                        }
                    });
                    Util.showThemedDialog(alertDialog);
                    return true;
                }
            });

            configureDefaultAppsList();

        }


        private void configureDefaultAppsList() {
            PreferenceCategory preferenceCategory = (PreferenceCategory)findPreference("preference_category_other_apps");
            preferenceCategory.removeAll();

            Preference noticePreference = new Preference(getActivity());

            PackageManager packageManager = getActivity().getPackageManager();
            TreeMap<String, ComponentName> defaultAppsMap = Settings.get().getDefaultAppsMap();
            if (defaultAppsMap != null && defaultAppsMap.size() > 0) {
                noticePreference.setSummary(R.string.preference_default_apps_notice_summary);
                preferenceCategory.addPreference(noticePreference);

                for (String key : defaultAppsMap.keySet()) {
                    ComponentName componentName = defaultAppsMap.get(key);
                    try {
                        ActivityInfo info = packageManager.getActivityInfo(componentName, 0);
                        final CharSequence label = info.loadLabel(packageManager);
                        final String host = key;
                        Preference preference = new Preference(getActivity());
                        preference.setTitle(label);
                        setPreferenceIcon(preference, info.loadIcon(packageManager));
                        preference.setSummary(key);
                        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @SuppressLint("StringFormatMatches")        // Lint incorrectly flags this because 2 items are the same.
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Resources resources = getActivity().getResources();
                                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                                alertDialog.setIcon(Util.getAlertIcon(getActivity()));
                                alertDialog.setTitle(R.string.remove_default_title);
                                alertDialog.setMessage(String.format(resources.getString(R.string.remove_default_message), label, host, host));
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.action_remove),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Settings.get().removeDefaultApp(host);
                                                configureDefaultAppsList();
                                            }
                                        });
                                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.action_cancel),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        });
                                Util.showThemedDialog(alertDialog);
                                return true;
                            }
                        });
                        preferenceCategory.addPreference(preference);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                noticePreference.setSummary(R.string.preference_default_apps_notice_no_defaults_summary);
                preferenceCategory.addPreference(noticePreference);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            configureDefaultAppsList();
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);

            if (preference instanceof ListPreference) {
                ListPreference listPref = (ListPreference) preference;
                preference.setSummary(listPref.getEntry());
            }
        }
    }

}