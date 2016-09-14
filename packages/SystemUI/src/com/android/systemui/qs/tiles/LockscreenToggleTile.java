/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.systemui.qs.tiles;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.quicksettings.Tile;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class LockscreenToggleTile extends QSTileImpl<BooleanState>
        implements KeyguardMonitor.Callback {

    public static final String ACTION_APPLY_LOCKSCREEN_STATE =
            "com.android.systemui.qs.tiles.action.APPLY_LOCKSCREEN_STATE";

    private static final Intent LOCK_SCREEN_SETTINGS =
            new Intent("android.settings.LOCK_SCREEN_SETTINGS");

    private KeyguardViewMediator mKeyguardViewMediator;
    private KeyguardMonitor mKeyguard;
    private boolean mVolatileState;
    private boolean mKeyguardBound;
    private final ActivityStarter mActivityStarter;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mKeyguardViewMediator != null) {
                mKeyguardBound = mKeyguardViewMediator.isKeyguardBound();
                applyLockscreenState();
                refreshState();
            }
        }
    };

    public LockscreenToggleTile(QSHost host) {
        super(host);

        mKeyguard = Dependency.get(KeyguardMonitor.class);
        mKeyguardViewMediator =
                ((SystemUIApplication)
                        mContext.getApplicationContext()).getComponent(KeyguardViewMediator.class);
        mVolatileState = true;
        mKeyguardBound = mKeyguardViewMediator.isKeyguardBound();
        mActivityStarter = Dependency.get(ActivityStarter.class);

        applyLockscreenState();
        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_APPLY_LOCKSCREEN_STATE));
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (listening) {
            mKeyguard.addCallback(this);
        } else {
            mKeyguard.removeCallback(this);
        }
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleClick() {
        if (!mKeyguard.isShowing() || !mKeyguard.isSecure()) {
            mVolatileState = !mVolatileState;
            applyLockscreenState();
            refreshState();
	}
    }

    @Override
    protected void handleLongClick() {
        mActivityStarter.postStartActivityDismissingKeyguard(LOCK_SCREEN_SETTINGS, 0);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_lockscreen_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean lockscreenEnforced = mKeyguardViewMediator.lockscreenEnforcedByDevicePolicy();
        final boolean lockscreenEnabled = lockscreenEnforced
                || mVolatileState
                || mKeyguardViewMediator.getKeyguardEnabledInternal();

        state.label = mHost.getContext().getString(lockscreenEnforced
                ? R.string.quick_settings_lockscreen_label_enforced
                : R.string.quick_settings_lockscreen_label);
        state.contentDescription = mHost.getContext().getString(lockscreenEnabled
                ? R.string.accessibility_quick_settings_lock_screen_on
                : R.string.accessibility_quick_settings_lock_screen_off);

        state.value = lockscreenEnabled;
        state.icon = ResourceIcon.get(lockscreenEnabled
                ? R.drawable.ic_qs_lock_screen_on
                : R.drawable.ic_qs_lock_screen_off);

        state.state = (mKeyguard.isShowing() && mKeyguard.isSecure()) ? Tile.STATE_UNAVAILABLE
            : Tile.STATE_ACTIVE;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_PANEL;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(
                    R.string.accessibility_quick_settings_lock_screen_changed_on);
        } else {
            return mContext.getString(
                    R.string.accessibility_quick_settings_lock_screen_changed_off);
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onKeyguardShowingChanged() {
        refreshState();
    }

    private void applyLockscreenState() {
        if (!mKeyguardBound) {
            // do nothing yet
            return;
        }

        mKeyguardViewMediator.setKeyguardEnabledInternal(mVolatileState);
    }
}
