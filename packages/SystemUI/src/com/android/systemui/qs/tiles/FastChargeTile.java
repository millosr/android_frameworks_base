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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.quicksettings.Tile;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class FastChargeTile extends QSTileImpl<BooleanState> {
    private static final String FAST_CHARGE_FILE = "/sys/kernel/fast_charge/force_fast_charge";

    public FastChargeTile(QSHost host) {
        super(host);
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
        toggleState();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_fastcharge_label);
    }

    protected void toggleState() {
        String fastChargeState = FileUtils.readOneLine(FAST_CHARGE_FILE);
        if (fastChargeState != null) {
            boolean state = fastChargeState.contentEquals("1");
            FileUtils.writeLine(FAST_CHARGE_FILE, state ? "0" : "1");
        }
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        String fastChargeState = FileUtils.readOneLine(FAST_CHARGE_FILE);

        // state.visible = (fastChargeState != null);
        state.value = (fastChargeState != null) && (fastChargeState.contentEquals("1"));
        state.icon = ResourceIcon.get(state.value ?
                R.drawable.ic_qs_fastcharge_on : R.drawable.ic_qs_fastcharge_off);
        state.label = mContext.getString(state.value
                ? R.string.qs_tile_fastcharge : R.string.qs_tile_fastcharge_off);
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_PANEL;
    }

    @Override
    public void handleSetListening(boolean listening) {
    }
}
