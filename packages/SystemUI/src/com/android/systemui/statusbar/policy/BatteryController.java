/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "BatteryController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final int PERCENTAGE_MODE_OFF = 0;
    public static final int PERCENTAGE_MODE_INSIDE = 1;
    public static final int PERCENTAGE_MODE_OUTSIDE = 2;

    private Context mContext;
    private final ArrayList<BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final PowerManager mPowerManager;

    private int mLevel;
    private boolean mPluggedIn;
    private boolean mCharging;
    private boolean mCharged;
    private boolean mPowerSave;
    private int mPercentageMode;

    private ContentObserver mSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updatePercentageMode();
            firePercentageModeChanged();
        }
    };

    public BatteryController(Context context) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);
        context.registerReceiver(this, filter);
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT),
                false, mSettingObserver);

        updatePowerSave();
        updatePercentageMode();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel="); pw.println(mLevel);
        pw.print("  mPluggedIn="); pw.println(mPluggedIn);
        pw.print("  mCharging="); pw.println(mCharging);
        pw.print("  mCharged="); pw.println(mCharged);
        pw.print("  mPowerSave="); pw.println(mPowerSave);
        pw.print("  mPercentageMode="); pw.println(mPercentageMode);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
        cb.onPercentageModeChanged(mPercentageMode);
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            mLevel = (int)(100f
                    * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            mPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            mCharged = status == BatteryManager.BATTERY_STATUS_FULL;
            mCharging = mCharged || status == BatteryManager.BATTERY_STATUS_CHARGING;

            fireBatteryLevelChanged();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) {
            updatePowerSave();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING)) {
            setPowerSave(intent.getBooleanExtra(PowerManager.EXTRA_POWER_SAVE_MODE, false));
        }
    }

    public boolean isPowerSave() {
        return mPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(mPowerManager.isPowerSaveMode());
    }

    public int getPercentageMode() {
        return mPercentageMode;
    }

    private void updatePercentageMode() {
        mPercentageMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
    }

    private void setPowerSave(boolean powerSave) {
        if (powerSave == mPowerSave) return;
        mPowerSave = powerSave;
        if (DEBUG) Log.d(TAG, "Power save is " + (mPowerSave ? "on" : "off"));
        firePowerSaveChanged();
    }

    private void fireBatteryLevelChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
        }
    }

    private void firePowerSaveChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onPowerSaveChanged();
        }
    }

    private void firePercentageModeChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onPercentageModeChanged(mPercentageMode);
        }
    }

    public interface BatteryStateChangeCallback {
        void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging);
        void onPowerSaveChanged();
        void onPercentageModeChanged(int percentageMode);
    }

}
