package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pgyl.pekislib_a.Constants.ACTIVITY_START_TYPE;
import com.example.pgyl.pekislib_a.CustomImageButton;
import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.ArrayList;

import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_TABLES;
import static com.example.pgyl.pekislib_a.Constants.TABLE_ACTIVITY_INFOS_DATA_FIELDS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.convertMsToHms;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.USE_CLOCK_APP;

public class MainCtListItemAdapter extends BaseAdapter {
    public interface onButtonClickListener {
        void onButtonClick();
    }

    public void setOnItemButtonClick(onButtonClickListener listener) {
        mOnButtonClickListener = listener;
    }

    private onButtonClickListener mOnButtonClickListener;

    //region Variables
    private Context context;
    private int orientation;
    private ArrayList<CtRecord> ctRecords;
    private StringShelfDatabase stringShelfDatabase;
    private boolean showExpirationTime;
    //endregion

    public MainCtListItemAdapter(Context context, StringShelfDatabase stringShelfDatabase) {
        super();

        this.context = context;
        this.stringShelfDatabase = stringShelfDatabase;
        init();
    }

    private void init() {
        mOnButtonClickListener = null;
        orientation = context.getResources().getConfiguration().orientation;
    }

    public void close() {
        stringShelfDatabase = null;
        context = null;
    }

    public void setItems(ArrayList<CtRecord> ctRecords) {
        this.ctRecords = ctRecords;
    }

    public void setShowExpirationTime(boolean showExpirationTime) {
        this.showExpirationTime = showExpirationTime;
    }

    @Override
    public int getCount() {
        int ret = 0;
        if (ctRecords != null) {
            ret = ctRecords.size();
        }
        return ret;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    private void onCbSelectionClick(int pos, boolean isChecked) {
        if (isChecked) {
            ctRecords.get(pos).setSelectedOn();
        } else {
            ctRecords.get(pos).setSelectedOff();
        }
    }

    private void onBtnModeRunClick(int pos) {
        long nowm = System.currentTimeMillis();
        if (!ctRecords.get(pos).isRunning()) {
            ctRecords.get(pos).start(nowm);
        } else {
            if (!ctRecords.get(pos).stop(nowm)) {
                ctRecords.get(pos).setClockAppAlarmOff(USE_CLOCK_APP);
            }
        }
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onButtonClick();
        }
    }

    private void onBtnSplitClick(int pos) {
        long nowm = System.currentTimeMillis();
        if ((ctRecords.get(pos).isRunning()) || (ctRecords.get(pos).isSplitted())) {
            ctRecords.get(pos).split(nowm);
        } else {
            if (!ctRecords.get(pos).reset()) {
                ctRecords.get(pos).setClockAppAlarmOff(USE_CLOCK_APP);
            }
        }
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onButtonClick();
        }
    }

    private void onBtnClockAppAlarmClick(int pos) {
        if (ctRecords.get(pos).getMode().equals(MODE.TIMER)) {
            if (ctRecords.get(pos).isRunning()) {
                if (!ctRecords.get(pos).hasClockAppAlarm()) {
                    ctRecords.get(pos).setClockAppAlarmOn(USE_CLOCK_APP);
                } else {
                    ctRecords.get(pos).setClockAppAlarmOff(USE_CLOCK_APP);
                }
                if (mOnButtonClickListener != null) {
                    mOnButtonClickListener.onButtonClick();
                }
            }
        }
    }

    private void onTimeMessageClick(int pos) {
        launchCtDisplayActivity(ctRecords.get(pos).getIdct());
    }

    private void onTimeMessageLongClick(int pos) {
        for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
            if (i != pos) {
                ctRecords.get(i).setSelectedOff();
            } else {
                ctRecords.get(i).setSelectedOn();
            }
        }
        notifyDataSetChanged();   //  Pas besoin d'un Rebuild List
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MainCtListItemViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                convertView = inflater.inflate(R.layout.mainlistitem_p, null);
            } else {
                convertView = inflater.inflate(R.layout.mainlistitem_l, null);
            }
            viewHolder = buildViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MainCtListItemViewHolder) convertView.getTag();
        }
        setupViewHolder(viewHolder, position);
        paintView(convertView, position);
        return convertView;
    }

    public void paintView(View view, int index) {    //  Décoration proprement dite du getView
        final String LIGHT_ON_UNPRESSED_COLOR = "FF9A22";
        final String LIGHT_ON_PRESSED_COLOR = "995400";
        final String LIGHT_OFF_PRESSED_COLOR = "666666";
        final String SEPARATOR = " - ";
        TIMEUNITS tu;
        int id;

        id = 0;
        int k = index;
        MainCtListItemViewHolder viewHolder = (MainCtListItemViewHolder) view.getTag();
        viewHolder.cbSelection.setChecked(ctRecords.get(k).isSelected());
        if (ctRecords.get(k).getMode().equals(MODE.CHRONO)) {
            id = R.drawable.main_chrono;
        }
        if (ctRecords.get(k).getMode().equals(MODE.TIMER)) {
            id = R.drawable.main_timer;
        }
        viewHolder.btnModeRun.setImageResource(id);
        if (ctRecords.get(k).isRunning()) {
            viewHolder.btnModeRun.setUnpressedColor(LIGHT_ON_UNPRESSED_COLOR);
            viewHolder.btnModeRun.setPressedColor(LIGHT_ON_PRESSED_COLOR);
        } else {
            viewHolder.btnModeRun.setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            viewHolder.btnModeRun.setPressedColor(LIGHT_OFF_PRESSED_COLOR);
        }
        viewHolder.btnModeRun.updateColor();
        if (ctRecords.get(k).isSplitted()) {
            viewHolder.btnSplit.setUnpressedColor(LIGHT_ON_UNPRESSED_COLOR);
            viewHolder.btnSplit.setPressedColor(LIGHT_ON_PRESSED_COLOR);
        } else {
            viewHolder.btnSplit.setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            viewHolder.btnSplit.setPressedColor(LIGHT_OFF_PRESSED_COLOR);
        }
        viewHolder.btnSplit.updateColor();
        if (ctRecords.get(k).hasClockAppAlarm()) {
            viewHolder.btnClockAppAlarm.setUnpressedColor(LIGHT_ON_UNPRESSED_COLOR);
            viewHolder.btnClockAppAlarm.setPressedColor(LIGHT_ON_PRESSED_COLOR);
        } else {
            viewHolder.btnClockAppAlarm.setUnpressedColor(BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
            viewHolder.btnClockAppAlarm.setPressedColor(LIGHT_OFF_PRESSED_COLOR);
        }
        viewHolder.btnClockAppAlarm.updateColor();
        long timeDisplay = ctRecords.get(k).getTimeDisplay();
        if ((ctRecords.get(k).isRunning()) && (!ctRecords.get(k).isSplitted())) {
            tu = TIMEUNITS.SEC;
        } else {
            tu = TIMEUNITS.CS;
        }
        if (ctRecords.get(k).getMode().equals(MODE.TIMER)) {
            if (showExpirationTime) {
                timeDisplay = ctRecords.get(k).getTimeZoneExpirationTime();
                tu = TIMEUNITS.SEC;
            }
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewHolder.tvTime.setText(convertMsToHms(timeDisplay, tu));
            viewHolder.tvMessage.setText(ctRecords.get(k).getMessage());
        } else {
            viewHolder.tvTimeMessage.setText(convertMsToHms(timeDisplay, tu) + SEPARATOR + ctRecords.get(k).getMessage());
        }
    }

    private MainCtListItemViewHolder buildViewHolder(View convertView) {
        MainCtListItemViewHolder viewHolder = new MainCtListItemViewHolder();
        viewHolder.cbSelection = (CheckBox) convertView.findViewById(R.id.CB_SELECTION);
        viewHolder.btnModeRun = (CustomImageButton) convertView.findViewById(R.id.BTN_MODE_RUN);
        viewHolder.btnSplit = (CustomImageButton) convertView.findViewById(R.id.BTN_SPLIT);
        viewHolder.btnClockAppAlarm = (CustomImageButton) convertView.findViewById(R.id.BTN_CLOCK_APP_ALARM);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewHolder.ltTimeMessage = (LinearLayout) convertView.findViewById(R.id.LT_TIME_MESSAGE);
            viewHolder.tvTime = (TextView) convertView.findViewById(R.id.TV_TIME);
            viewHolder.tvMessage = (TextView) convertView.findViewById(R.id.TV_MESSAGE);
        } else {
            viewHolder.tvTimeMessage = (TextView) convertView.findViewById(R.id.TV_TIME_MESSAGE);
        }
        return viewHolder;
    }

    private void setupViewHolder(MainCtListItemViewHolder viewHolder, int position) {
        final int pos = position;
        viewHolder.cbSelection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCbSelectionClick(pos, isChecked);
            }
        });
        viewHolder.btnModeRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnModeRunClick(pos);
            }
        });
        viewHolder.btnSplit.setImageResource(R.drawable.main_split);
        viewHolder.btnSplit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnSplitClick(pos);
            }
        });
        viewHolder.btnClockAppAlarm.setImageResource(R.drawable.main_bell);
        viewHolder.btnClockAppAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnClockAppAlarmClick(pos);
            }
        });
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewHolder.ltTimeMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTimeMessageClick(pos);
                }
            });
            viewHolder.ltTimeMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onTimeMessageLongClick(pos);
                    return false;
                }
            });
        } else {
            viewHolder.tvTimeMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTimeMessageClick(pos);
                }
            });
            viewHolder.tvTimeMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onTimeMessageLongClick(pos);
                    return false;
                }
            });
        }
    }

    private void launchCtDisplayActivity(int idct) {
        setColdStartForCtDisplay();
        Intent callingIntent = new Intent(context, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        ((Activity) context).startActivityForResult(callingIntent, SWTIMER_ACTIVITIES.CTDISPLAY.ordinal());
    }

    private void setColdStartForCtDisplay() {
        stringShelfDatabase.insertOrReplaceFieldById(PEKISLIB_TABLES.ACTIVITY_INFOS.toString(), SWTIMER_ACTIVITIES.CTDISPLAY.toString(), TABLE_ACTIVITY_INFOS_DATA_FIELDS.START_TYPE.INDEX(), ACTIVITY_START_TYPE.COLD.toString());
    }

}
