/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.afterlife.afterlab.fragments;

import android.database.ContentObserver;
import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.afterlife.AfterlifeUtils.QSLayoutUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.afterlife.ThemeUtils;

import com.android.settingslib.widget.LayoutPreference;
import com.afterlife.support.preference.ProperSeekBarPreference;
import com.afterlife.support.preference.SystemSettingSwitchPreference;
import com.afterlife.support.preference.SystemSettingListPreference;
import com.afterlife.support.preference.SecureSettingListPreference;

public class QsTileLayoutSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_QS_HIDE_LABEL = "qs_tile_label_hide";
    private static final String KEY_QS_VERTICAL_LAYOUT = "qs_tile_vertical_layout";
    private static final String KEY_QS_COLUMN_PORTRAIT = "qs_layout_columns";
    private static final String KEY_QS_ROW_PORTRAIT = "qs_layout_rows";
    private static final String KEY_QQS_ROW_PORTRAIT = "qqs_layout_rows";
    private static final String KEY_QS_UI_STYLE  = "qs_ui_style";
    private static final String KEY_QS_SHAPE = "qs_tile_shape";
    private static final String KEY_APPLY_CHANGE_BUTTON = "apply_change_button";
    private static final String overlayThemeTarget  = "com.android.systemui";

    private Context mContext;

    private ProperSeekBarPreference mQsColumns;
    private ProperSeekBarPreference mQsRows;
    private ProperSeekBarPreference mQqsRows;
    private SystemSettingListPreference mQsUI;
    private Handler mHandler;
    private ThemeUtils mThemeUtils;

    private Button mApplyChange;

    private SystemSettingSwitchPreference mHide;
    private SystemSettingSwitchPreference mVertical;
    private SecureSettingListPreference mQsShape;

    private int[] currentValue = new int[2];

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.qs_tile_layout);
        
        mThemeUtils = new ThemeUtils(getActivity());

        mQsUI = (SystemSettingListPreference) findPreference(KEY_QS_UI_STYLE);
        mCustomSettingsObserver.observe();
    }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

        mQsColumns = (ProperSeekBarPreference) findPreference(KEY_QS_COLUMN_PORTRAIT);
        mQsColumns.setOnPreferenceChangeListener(this);

        mQsRows = (ProperSeekBarPreference) findPreference(KEY_QS_ROW_PORTRAIT);
        mQsRows.setOnPreferenceChangeListener(this);

        mQqsRows = (ProperSeekBarPreference) findPreference(KEY_QQS_ROW_PORTRAIT);
        mQqsRows.setOnPreferenceChangeListener(this);

        mContext = getContext();

        LayoutPreference preference = findPreference(KEY_APPLY_CHANGE_BUTTON);
        mApplyChange = (Button) preference.findViewById(R.id.apply_change);
        mApplyChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApplyChange.isEnabled()) {
                    final int[] newValue = {
                        mQsRows.getValue() * 10 + mQsColumns.getValue(),
                        mQqsRows.getValue() * 10 + mQsColumns.getValue()
                    };
                    Settings.System.putIntForUser(getContentResolver(),
                            Settings.System.QS_LAYOUT, newValue[0], UserHandle.USER_CURRENT);
                    Settings.System.putIntForUser(getContentResolver(),
                            Settings.System.QQS_LAYOUT, newValue[1], UserHandle.USER_CURRENT);
                    if (QSLayoutUtils.updateLayout(mContext)) {
                        currentValue[0] = newValue[0];
                        currentValue[1] = newValue[1];
                        mApplyChange.setEnabled(false);
                    } else {
                        Settings.System.putIntForUser(getContentResolver(),
                                Settings.System.QS_LAYOUT, currentValue[0], UserHandle.USER_CURRENT);
                        Settings.System.putIntForUser(getContentResolver(),
                                Settings.System.QQS_LAYOUT, currentValue[1], UserHandle.USER_CURRENT);
                        Toast.makeText(mContext, R.string.qs_apply_change_failed, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        initPreference();

        final boolean hideLabel = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_TILE_LABEL_HIDE, 0, UserHandle.USER_CURRENT) == 1;

        mHide = (SystemSettingSwitchPreference) findPreference(KEY_QS_HIDE_LABEL);
        mHide.setOnPreferenceChangeListener(this);
        
        int qsStyleVal = Settings.System.getInt(getContentResolver(), KEY_QS_UI_STYLE, 0);

        mVertical = (SystemSettingSwitchPreference) findPreference(KEY_QS_VERTICAL_LAYOUT);
        mVertical.setOnPreferenceChangeListener(this);
        boolean enable = !hideLabel && qsStyleVal == 0;
         mVertical.setEnabled(enable);
        mQsShape = (SecureSettingListPreference) findPreference(KEY_QS_SHAPE);
        int qsShapeVal = Settings.Secure.getInt(getContentResolver(), KEY_QS_SHAPE, 0);
        mQsShape.setValue(String.valueOf(qsShapeVal));
        mQsShape.setSummary(mQsShape.getEntry());
        mQsShape.setOnPreferenceChangeListener(this);
        mQsShape.setEnabled(qsStyleVal == 1);

        mQsUI = (SystemSettingListPreference) findPreference(KEY_QS_UI_STYLE);
        mQsUI.setValue(String.valueOf(qsStyleVal));
        mQsUI.setSummary(mQsUI.getEntry());
        mQsUI.setOnPreferenceChangeListener(this);
        
      }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHide) {
            boolean hideLabel = (Boolean) newValue;
            int qsStyleVal = Settings.System.getInt(getContentResolver(), KEY_QS_UI_STYLE, 0);
            QSLayoutUtils.updateLayout(mContext);
            mVertical.setEnabled(!hideLabel && qsStyleVal == 0);
            } else if (preference == mVertical) {
            QSLayoutUtils.updateLayout(mContext);
        } else if (preference == mQsColumns) {
            int qs_columns = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + qs_columns ||
                currentValue[1] != mQqsRows.getValue() * 10 + qs_columns
            );
        } else if (preference == mQsRows) {
            int qs_rows = Integer.parseInt(newValue.toString());
            mQqsRows.setMax(qs_rows - 1);
            if (mQqsRows.getValue() > qs_rows - 1) {
                mQqsRows.setValue(qs_rows - 1);
            }
            mApplyChange.setEnabled(
                currentValue[0] != qs_rows * 10 + mQsColumns.getValue() ||
                currentValue[1] != mQqsRows.getValue() * 10 + mQsColumns.getValue()
            );
        } else if (preference == mQqsRows) {
            int qqs_rows = Integer.parseInt(newValue.toString());
            mApplyChange.setEnabled(
                currentValue[0] != mQsRows.getValue() * 10 + mQsColumns.getValue() ||
                currentValue[1] != qqs_rows * 10 + mQsColumns.getValue()
            );
        } else if (preference == mQsUI) {
            mCustomSettingsObserver.observe();
            int value = Integer.parseInt((String) newValue);
             SystemSettingListPreference qsStyle = mQsUI;
             int index = qsStyle.findIndexOfValue((String) newValue);
             Settings.System.putInt(getContentResolver(), KEY_QS_UI_STYLE, value);
             qsStyle.setSummary(qsStyle.getEntries()[index]);
             if (value == 0) {
                 mQsShape.setEnabled(false);
                 mVertical.setEnabled(true);
                 Settings.System.putIntForUser(getContentResolver(),
                         Settings.System.QS_LAYOUT, 42, UserHandle.USER_CURRENT);
                 Settings.System.putIntForUser(getContentResolver(),
                         Settings.System.QQS_LAYOUT, 22, UserHandle.USER_CURRENT);
                 mQsColumns.setValue(42 % 10);
                 mQsRows.setValue(22 / 10);
                 QSLayoutUtils.updateLayout(mContext);
             } else {
                 mQsShape.setEnabled(true);
                 mVertical.setEnabled(false);
                 Settings.System.putIntForUser(getContentResolver(),
                         Settings.System.QS_LAYOUT, 35, UserHandle.USER_CURRENT);
                 Settings.System.putIntForUser(getContentResolver(),
                         Settings.System.QQS_LAYOUT, 15, UserHandle.USER_CURRENT);
                 mQsColumns.setValue(35 % 10);
                 mQsRows.setValue(35 / 10);
                 QSLayoutUtils.updateLayout(mContext);
             }
             mQqsRows.setMax(mQsRows.getValue() - 1);
         } else if (preference == mQsShape) {
             int value = Integer.parseInt((String) newValue);
             SecureSettingListPreference qsShape = mQsShape;
             int index = qsShape.findIndexOfValue((String) newValue);
             Settings.Secure.putInt(getContentResolver(), KEY_QS_SHAPE, value);
             qsShape.setSummary(qsShape.getEntries()[index]);
        }
        return true;
    }
    
    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_UI_STYLE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.QS_UI_STYLE))) {
                updateQsStyle();
            }
        }
    }
    
    private void updateQsStyle() {
        ContentResolver resolver = getActivity().getContentResolver();

        boolean isA11Style = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_UI_STYLE , 1, UserHandle.USER_CURRENT) == 1;

	String qsUIStyleCategory = "android.theme.customization.qs_ui";

	/// reset all overlays before applying
	resetQsOverlays(qsUIStyleCategory);

	if (isA11Style) {
	    setQsStyle("com.android.system.qs.ui.A11", qsUIStyleCategory);
	}
    }

    public void resetQsOverlays(String category) {
        mThemeUtils.setOverlayEnabled(category, overlayThemeTarget);
    }

    public void setQsStyle(String overlayName, String category) {
        mThemeUtils.setOverlayEnabled(category, overlayName);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.AFTERLIFE;
    }

    private void initPreference() {
        final int index_qs = Settings.System.getIntForUser(getContentResolver(),
            Settings.System.QS_LAYOUT, 42, UserHandle.USER_CURRENT);
        final int index_qqs = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QQS_LAYOUT, 22, UserHandle.USER_CURRENT);
        mQsColumns.setValue(index_qs % 10);
        mQsRows.setValue(index_qs / 10);
        mQqsRows.setValue(index_qqs / 10);
        mQqsRows.setMax(mQsRows.getValue() - 1);
        currentValue[0] = index_qs;
        currentValue[1] = index_qqs;
    }
}
