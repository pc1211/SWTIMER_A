package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.SymbolButtonView;

import java.util.ArrayList;

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

    public interface onStartStopResetClickListener {
        void onStartStopResetClick(long nowm, long timeAcc);
    }

    public void setOnItemStartStopResetClick(onStartStopResetClickListener listener) {
        mOnStartStopResetClickListener = listener;
    }

    private onStartStopResetClickListener mOnStartStopResetClickListener;

    //region Variables
    private Context context;
    private ArrayList<CtRecord> ctRecords;
    private StringDB stringDB;
    private boolean showExpirationTime;
    private boolean setClockAppAlarmOnStartTimer;
    private MainCtListItemDotMatrixDisplayUpdater mainCtListItemDotMatrixDisplayUpdater;
    //endregion

    public MainCtListItemAdapter(Context context, StringDB stringDB) {
        super();

        this.context = context;
        this.stringDB = stringDB;
        init();
    }

    private void init() {
        mOnCheckBoxClickListener = null;
        ctRecords = null;
        setupMainCtListItemDotMatrixDisplayUpdater();
    }

    public void close() {
        mainCtListItemDotMatrixDisplayUpdater.close();
        mainCtListItemDotMatrixDisplayUpdater = null;
        ctRecords = null;
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

    private void onButtonModeSelectionClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        ctRecords.get(pos).setSelectedOn(!ctRecords.get(pos).isSelected());   //  Invert selection
        if (mOnCheckBoxClickListener != null) {
            mOnCheckBoxClickListener.onCheckBoxClick();
        }
        paintView(rowv, pos, nowm);
    }

    private void onButtonStartStopClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        if (!ctRecords.get(pos).isRunning()) {
            ctRecords.get(pos).start(nowm, setClockAppAlarmOnStartTimer);
        } else {
            ctRecords.get(pos).stop(nowm);
        }
        if (mOnStartStopResetClickListener != null) {
            mOnStartStopResetClickListener.onStartStopResetClick(nowm, ctRecords.get(pos).getTimeAcc());
        }
        paintView(rowv, pos, nowm);
    }

    private void onButtonSplitClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        ctRecords.get(pos).split(nowm);
        paintView(rowv, pos, nowm);
    }

    private void onButtonResetClick(View rowv, int pos) {
        long nowm = System.currentTimeMillis();
        ctRecords.get(pos).reset();
        if (mOnStartStopResetClickListener != null) {
            mOnStartStopResetClickListener.onStartStopResetClick(nowm, ctRecords.get(pos).getTimeAcc());
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

    public void paintView(View rowView, int position, long nowm) {    //  Décoration proprement dite du getView7
        final String COLOR_A_1 = "668CFF";
        final String COLOR_A_2 = "000000";
        final String COLOR_B_1 = "000000";
        final String COLOR_B_2 = "C0C0C0";
        final String COLOR_B_3 = "FF9A22";

        int pos = position;
        MainCtListItemViewHolder viewHolder = (MainCtListItemViewHolder) rowView.getTag();

        boolean b = ctRecords.get(pos).isSelected();   //  A
        String frontColor = (b ? COLOR_A_2 : COLOR_A_1);
        String backColor = (b ? COLOR_A_1 : COLOR_A_2);
        String extraColor = frontColor;
        viewHolder.buttonModeSelection.setColors(frontColor, backColor, extraColor);

        if (ctRecords.get(pos).getMode().equals(MODES.CHRONO) || !ctRecords.get(pos).isReset() || (ctRecords.get(pos).getTimeDef() > 0)) {
            frontColor = COLOR_B_1;    //  B
            extraColor = frontColor;
            backColor = (ctRecords.get(pos).isRunning() ? COLOR_B_3 : COLOR_B_2);
            viewHolder.buttonStartStop.setColors(frontColor, backColor, extraColor);
            viewHolder.buttonStartStop.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonStartStop.setVisibility(View.INVISIBLE);
        }

        if (ctRecords.get(pos).isRunning() || ctRecords.get(pos).isSplitted()) {
            backColor = (ctRecords.get(pos).isSplitted() ? COLOR_B_3 : COLOR_B_2);
            viewHolder.buttonSplit.setColors(frontColor, backColor, extraColor);
            viewHolder.buttonSplit.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonSplit.setVisibility(View.INVISIBLE);
        }

        if (!ctRecords.get(pos).isRunning() && !ctRecords.get(pos).isReset()) {
            backColor = COLOR_B_2;
            viewHolder.buttonReset.setColors(frontColor, backColor, extraColor);
            viewHolder.buttonReset.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonReset.setVisibility(View.INVISIBLE);
        }

        if (ctRecords.get(pos).getMode().equals(MODES.TIMER) && ctRecords.get(pos).isRunning()) {
            backColor = (ctRecords.get(pos).isClockAppAlarmOn() ? COLOR_B_3 : COLOR_B_2);
            viewHolder.buttonClockAppAlarm.setColors(frontColor, backColor, extraColor);
            viewHolder.buttonClockAppAlarm.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonClockAppAlarm.setVisibility(View.INVISIBLE);
        }

        mainCtListItemDotMatrixDisplayUpdater.displayTimeAndLabel(viewHolder.buttonDotMatrixDisplayTimeLabel, ctRecords.get(pos), showExpirationTime, nowm);
    }

    private MainCtListItemViewHolder buildViewHolder(View rowView) {
        MainCtListItemViewHolder viewHolder = new MainCtListItemViewHolder();
        viewHolder.buttonModeSelection = rowView.findViewById(R.id.STATE_BTN_MODE_SELECTION);
        viewHolder.buttonStartStop = rowView.findViewById(R.id.STATE_BTN_START_STOP);
        viewHolder.buttonSplit = rowView.findViewById(R.id.STATE_BTN_SPLIT);
        viewHolder.buttonReset = rowView.findViewById(R.id.STATE_BTN_RESET);
        viewHolder.buttonClockAppAlarm = rowView.findViewById(R.id.STATE_BTN_CLOCK_APP_ALARM);
        viewHolder.buttonDotMatrixDisplayTimeLabel = rowView.findViewById(R.id.BTN_DOT_MATRIX_DISPLAY_TIME_LABEL);
        return viewHolder;
    }

    private void setupViewHolder(MainCtListItemViewHolder viewHolder, View rowView, int position) {
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;
        final float STATE_BUTTON_SYMBOL_SIZE_COEFF = 0.75f;   //  Pour que le symbole ne frôle pas les bords de sa View

        final View rowv = rowView;
        final int pos = position;

        viewHolder.buttonModeSelection.setSVGImageResource((ctRecords.get(pos).getMode().equals(MODES.CHRONO)) ? R.raw.ct_chrono : R.raw.ct_timer);
        viewHolder.buttonModeSelection.setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
        viewHolder.buttonModeSelection.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonModeSelection.setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonModeSelectionClick(rowv, pos);
            }
        });
        viewHolder.buttonStartStop.setSVGImageResource(R.raw.ct_start_stop);
        viewHolder.buttonStartStop.setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
        viewHolder.buttonStartStop.setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonStartStopClick(rowv, pos);
            }
        });
        viewHolder.buttonSplit.setSVGImageResource(R.raw.ct_split);
        viewHolder.buttonSplit.setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
        viewHolder.buttonSplit.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonSplit.setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonSplitClick(rowv, pos);
            }
        });
        viewHolder.buttonReset.setSVGImageResource(R.raw.ct_reset);
        viewHolder.buttonReset.setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
        viewHolder.buttonReset.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonReset.setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonResetClick(rowv, pos);
            }
        });
        viewHolder.buttonClockAppAlarm.setSVGImageResource(R.raw.ct_bell);
        viewHolder.buttonClockAppAlarm.setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
        viewHolder.buttonClockAppAlarm.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonClockAppAlarm.setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
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

    private void setupMainCtListItemDotMatrixDisplayUpdater() {
        mainCtListItemDotMatrixDisplayUpdater = new MainCtListItemDotMatrixDisplayUpdater();
    }

    private void launchCtDisplayActivity(int idct) {
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(context, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        context.startActivity(callingIntent);
    }
}
