package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;

import java.util.ArrayList;

import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODES;

public class MainCtListItemAdapter extends BaseAdapter {

    public interface onCheckBoxClickListener {
        void onCheckBoxClick();
    }

    public void setOnItemCheckBoxClick(onCheckBoxClickListener listener) {
        mOnCheckBoxClickListener = listener;
    }

    private onCheckBoxClickListener mOnCheckBoxClickListener;

    //region Variables
    private Context context;
    private ArrayList<CtRecord> ctRecords;
    private StringDB stringDB;
    private boolean showExpirationTime;
    private boolean setClockAppAlarmOnStartTimer;
    private MainCtListItemDotMatrixDisplayUpdater mainCtListItemDotMatrixDisplayUpdater;
    //endregion

    public MainCtListItemAdapter(Context context, StringDB stringDB, MainCtListItemDotMatrixDisplayUpdater mainCtListItemDotMatrixDisplayUpdater) {
        super();

        this.context = context;
        this.stringDB = stringDB;
        this.mainCtListItemDotMatrixDisplayUpdater = mainCtListItemDotMatrixDisplayUpdater;
        init();
    }

    private void init() {
        mOnCheckBoxClickListener = null;
    }

    public void close() {
        stringDB = null;
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
        return (ctRecords != null) ? ctRecords.size() : 0;
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
        ctRecords.get(pos).setSelectedOn(isChecked);
        if (mOnCheckBoxClickListener != null) {
            mOnCheckBoxClickListener.onCheckBoxClick();
        }
    }

    private void onCbSelectionLongClick(int pos) {
        for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
            ctRecords.get(i).setSelectedOn(i == pos);
        }
        notifyDataSetChanged();
        if (mOnCheckBoxClickListener != null) {
            mOnCheckBoxClickListener.onCheckBoxClick();
        }
    }

    private void onButtonModeRunClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        if (!ctRecords.get(pos).isRunning()) {
            ctRecords.get(pos).start(nowm, setClockAppAlarmOnStartTimer);
        } else {
            ctRecords.get(pos).stop(nowm);
        }
        paintView(rowv, pos, nowm);
    }

    private void onButtonSplitResetClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        if ((ctRecords.get(pos).isRunning()) || (ctRecords.get(pos).isSplitted())) {
            ctRecords.get(pos).split(nowm);
        } else {
            ctRecords.get(pos).reset();
        }
        paintView(rowv, pos, nowm);
    }

    private void onButtonClockAppAlarmClick(int pos) {
        ctRecords.get(pos).setClockAppAlarmOn(!ctRecords.get(pos).isClockAppAlarmOn());
    }

    private void onTimeLabelClick(int pos) {
        launchCtDisplayActivity(ctRecords.get(pos).getIdct());
    }

    @Override
    public View getView(final int position, View rowView, ViewGroup parent) {   //  Viewholder pattern non utilisé à cause de la custom view DotMatrixDisplayView (ses variables globales ne sont pas récupérées par un getTag())
        LayoutInflater inflater = LayoutInflater.from(context);
        rowView = inflater.inflate(R.layout.mainctlistitem, null);
        MainCtListItemViewHolder viewHolder = buildViewHolder(rowView);
        rowView.setTag(viewHolder);

        setupViewHolder(viewHolder, rowView, position);
        long nowm = System.currentTimeMillis();
        paintView(rowView, position, nowm);
        return rowView;
    }

    public void paintView(View rowView, int position, long nowm) {    //  Décoration proprement dite du getView
        final String LIGHT_ON_UNPRESSED_COLOR = "FF9A22";
        final String LIGHT_ON_PRESSED_COLOR = "995400";
        final String LIGHT_OFF_PRESSED_COLOR = "666666";

        int pos = position;
        MainCtListItemViewHolder viewHolder = (MainCtListItemViewHolder) rowView.getTag();

        viewHolder.cbSelection.setChecked(ctRecords.get(pos).isSelected());

        int id = 0;
        if (ctRecords.get(pos).getMode().equals(MODES.CHRONO)) {
            id = R.drawable.main_chrono;
        }
        if (ctRecords.get(pos).getMode().equals(MODES.TIMER)) {
            id = R.drawable.main_timer;
        }
        viewHolder.buttonModeRun.setImageResource(id);

        String pressedColor = (ctRecords.get(pos).isRunning() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        String unpressedColor = (ctRecords.get(pos).isRunning() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonModeRun.setColors(pressedColor, unpressedColor);

        pressedColor = (ctRecords.get(pos).isSplitted() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        unpressedColor = (ctRecords.get(pos).isSplitted() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonSplitReset.setColors(pressedColor, unpressedColor);

        pressedColor = (ctRecords.get(pos).isClockAppAlarmOn() ? LIGHT_ON_PRESSED_COLOR : LIGHT_OFF_PRESSED_COLOR);
        unpressedColor = (ctRecords.get(pos).isClockAppAlarmOn() ? LIGHT_ON_UNPRESSED_COLOR : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR());
        viewHolder.buttonClockAppAlarm.setColors(pressedColor, unpressedColor);

        mainCtListItemDotMatrixDisplayUpdater.displayTimeAndLabel(viewHolder.buttonDotMatrixDisplayTimeLabel, ctRecords.get(pos), showExpirationTime, nowm);
    }

    private MainCtListItemViewHolder buildViewHolder(View rowView) {
        MainCtListItemViewHolder viewHolder = new MainCtListItemViewHolder();
        viewHolder.cbSelection = rowView.findViewById(R.id.CB_SELECTION);
        viewHolder.buttonModeRun = rowView.findViewById(R.id.BTN_MODE_RUN);
        viewHolder.buttonSplitReset = rowView.findViewById(R.id.BTN_SPLIT_RESET);
        viewHolder.buttonClockAppAlarm = rowView.findViewById(R.id.BTN_CLOCK_APP_ALARM);
        viewHolder.buttonDotMatrixDisplayTimeLabel = rowView.findViewById(R.id.BTN_DOT_MATRIX_DISPLAY_TIME_LABEL);
        return viewHolder;
    }

    private void setupViewHolder(MainCtListItemViewHolder viewHolder, View rowView, int position) {
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        final View rowv = rowView;
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
                onButtonModeRunClick(rowv, pos);
            }
        });
        viewHolder.buttonSplitReset.setImageResource(R.drawable.main_split);
        viewHolder.buttonSplitReset.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonSplitReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonSplitResetClick(rowv, pos);
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
        viewHolder.buttonDotMatrixDisplayTimeLabel.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonDotMatrixDisplayTimeLabel.setOnCustomClickListener(new DotMatrixDisplayView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onTimeLabelClick(pos);
            }
        });
        mainCtListItemDotMatrixDisplayUpdater.setupDimensions(viewHolder.buttonDotMatrixDisplayTimeLabel);
        mainCtListItemDotMatrixDisplayUpdater.setupBackColor(viewHolder.buttonDotMatrixDisplayTimeLabel);
    }

    private void launchCtDisplayActivity(int idct) {
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(context, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        context.startActivity(callingIntent);
    }
}
