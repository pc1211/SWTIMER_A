package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;

import java.util.ArrayList;

import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.formattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.VIA_CLOCK_APP;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayActivity;

public class MainCtListItemAdapter extends BaseAdapter {
    public interface onButtonClickListener {
        void onButtonClick(boolean needSortAndReload);
    }

    public void setOnItemButtonClick(onButtonClickListener listener) {
        mOnButtonClickListener = listener;
    }

    private onButtonClickListener mOnButtonClickListener;

    private final boolean NEED_SORT_AND_RELOAD = true;
    //region Variables
    private Context context;
    private int orientation;
    private ArrayList<CtRecord> ctRecords;
    private StringShelfDatabase stringShelfDatabase;
    private boolean showExpirationTime;
    private boolean setClockAppAlarmOnStartTimer;
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

    public void setClockAppAlarmOnStartTimer(boolean setClockAppAlarmOnStartTimer) {
        this.setClockAppAlarmOnStartTimer = setClockAppAlarmOnStartTimer;
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

    private void onCbSelectionLongClick(int pos) {
        for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
            if (i != pos) {
                ctRecords.get(i).setSelectedOff();
            } else {
                ctRecords.get(i).setSelectedOn();
            }
        }
        notifyDataSetChanged();   //  Pas besoin d'un Rebuild List
    }

    private void onButtonModeRunClick(int pos) {
        long nowm = System.currentTimeMillis();
        if (!ctRecords.get(pos).isRunning()) {
            if (!ctRecords.get(pos).start(nowm)) {
                if (setClockAppAlarmOnStartTimer) {
                    ctRecords.get(pos).setClockAppAlarmOn(VIA_CLOCK_APP);
                }
            }
        } else {
            if (!ctRecords.get(pos).stop(nowm)) {
                ctRecords.get(pos).setClockAppAlarmOff(VIA_CLOCK_APP);
            }
        }
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onButtonClick(NEED_SORT_AND_RELOAD);
        }
    }

    private void onButtonSplitResetClick(int pos) {
        boolean b;

        long nowm = System.currentTimeMillis();
        if ((ctRecords.get(pos).isRunning()) || (ctRecords.get(pos).isSplitted())) {
            ctRecords.get(pos).split(nowm);
            b = !NEED_SORT_AND_RELOAD;
        } else {
            if (!ctRecords.get(pos).reset()) {
                ctRecords.get(pos).setClockAppAlarmOff(VIA_CLOCK_APP);
            }
            b = NEED_SORT_AND_RELOAD;
        }
        if (mOnButtonClickListener != null) {
            mOnButtonClickListener.onButtonClick(b);
        }
    }

    private void onButtonClockAppAlarmClick(int pos) {
        if (ctRecords.get(pos).getMode().equals(MODE.TIMER)) {
            if (ctRecords.get(pos).isRunning()) {
                if (!ctRecords.get(pos).hasClockAppAlarm()) {
                    ctRecords.get(pos).setClockAppAlarmOn(VIA_CLOCK_APP);
                } else {
                    ctRecords.get(pos).setClockAppAlarmOff(VIA_CLOCK_APP);
                }
                if (mOnButtonClickListener != null) {
                    mOnButtonClickListener.onButtonClick(!NEED_SORT_AND_RELOAD);
                }
            }
        }
    }

    private void onTimeLabelClick(int pos) {
        launchCtDisplayActivity(ctRecords.get(pos).getIdct());
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MainCtListItemViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.mainlistitem, null);
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
        viewHolder.buttonModeRun.setImageResource(id);

        String pressedColor = (ctRecords.get(k).isRunning() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        String unpressedColor = (ctRecords.get(k).isRunning() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonModeRun.setColors(pressedColor, unpressedColor);

        pressedColor = (ctRecords.get(k).isSplitted() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        unpressedColor = (ctRecords.get(k).isSplitted() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonSplitReset.setColors(pressedColor, unpressedColor);

        pressedColor = (ctRecords.get(k).hasClockAppAlarm() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        unpressedColor = (ctRecords.get(k).hasClockAppAlarm() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonClockAppAlarm.setColors(pressedColor, unpressedColor);

        boolean needTimeUnitPrecision = ((!ctRecords.get(k).isRunning()) || (ctRecords.get(k).isSplitted()));
        TIME_UNITS tu = (needTimeUnitPrecision ? TIME_UNIT_PRECISION : TIME_UNITS.SEC);
        boolean needSpecialTimeDisplay = ((ctRecords.get(k).getMode().equals(MODE.TIMER)) && showExpirationTime);
        String timeText = (needSpecialTimeDisplay ? formattedTimeZoneLongTimeDate(ctRecords.get(k).getTimeExp(), HHmmss) : msToTimeFormatD(ctRecords.get(k).getTimeDisplay(), tu));
        String text = timeText + ((orientation == Configuration.ORIENTATION_PORTRAIT) ? CRLF : SEPARATOR) + ctRecords.get(k).getLabel();
        viewHolder.buttonTimeLabel.setText(text);
    }

    private MainCtListItemViewHolder buildViewHolder(View convertView) {
        MainCtListItemViewHolder viewHolder = new MainCtListItemViewHolder();
        viewHolder.cbSelection = convertView.findViewById(R.id.CB_SELECTION);
        viewHolder.buttonModeRun = convertView.findViewById(R.id.BTN_MODE_RUN);
        viewHolder.buttonSplitReset = convertView.findViewById(R.id.BTN_SPLIT_RESET);
        viewHolder.buttonClockAppAlarm = convertView.findViewById(R.id.BTN_CLOCK_APP_ALARM);
        viewHolder.buttonTimeLabel = convertView.findViewById(R.id.BTN_TIME_LABEL);
        return viewHolder;
    }

    private void setupViewHolder(MainCtListItemViewHolder viewHolder, int position) {
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        final int pos = position;
        viewHolder.cbSelection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCbSelectionClick(pos, isChecked);
            }
        });
        viewHolder.cbSelection.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onCbSelectionLongClick(pos);
                return false;
            }
        });
        viewHolder.buttonModeRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonModeRunClick(pos);
            }
        });
        viewHolder.buttonSplitReset.setImageResource(R.drawable.main_split);
        viewHolder.buttonSplitReset.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonSplitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonSplitResetClick(pos);
            }
        });
        viewHolder.buttonClockAppAlarm.setImageResource(R.drawable.main_bell);
        viewHolder.buttonClockAppAlarm.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonClockAppAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClockAppAlarmClick(pos);
            }
        });
        viewHolder.buttonTimeLabel.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonTimeLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimeLabelClick(pos);
            }
        });
    }

    private void launchCtDisplayActivity(int idct) {
        setStartStatusInCtDisplayActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(context, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        context.startActivity(callingIntent);
    }
}
