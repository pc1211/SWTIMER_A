package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.example.pgyl.pekislib_a.ColorBox;
import com.example.pgyl.pekislib_a.ColorUtils;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.ImageButtonView;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;

import java.util.ArrayList;

import static com.example.pgyl.pekislib_a.ColorUtils.BUTTON_COLOR_TYPES;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODES;

public class MainCtListItemAdapter extends BaseAdapter {

    public interface onModeSelectionClickListener {
        void onModeSelectionClick();
    }

    public void setOnModeSelectionClick(onModeSelectionClickListener listener) {
        mOnModeSelectionClickListener = listener;
    }

    private onModeSelectionClickListener mOnModeSelectionClickListener;

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
    //endregion

    public MainCtListItemAdapter(Context context, StringDB stringDB) {
        super();

        this.context = context;
        this.stringDB = stringDB;
        init();
    }

    private void init() {
        mOnModeSelectionClickListener = null;
        ctRecords = null;
    }

    public void close() {
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
        if (mOnModeSelectionClickListener != null) {
            mOnModeSelectionClickListener.onModeSelectionClick();
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

    public void paintView(View rowView, int position, long nowm) {    //  Décoration proprement dite du getView
        final String BACKGROUND_COLOR = "000000";
        final String PRESSED_COLOR = "FF9A22";
        final String BUTTON_DARK_COLOR = "707070";
        final String SELECT_COLOR = "668CFF";

        int pos = position;
        MainCtListItemViewHolder viewHolder = (MainCtListItemViewHolder) rowView.getTag();

        boolean b = ctRecords.get(pos).isSelected();
        ColorBox colorBox = viewHolder.buttonModeSelection.getColorBox();
        colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX(), b ? BACKGROUND_COLOR : BUTTON_DARK_COLOR);
        colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX(), b ? SELECT_COLOR : BACKGROUND_COLOR);
        colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX()).RGBString);
        colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX()).RGBString);
        viewHolder.buttonModeSelection.updateDisplayColors();

        if (ctRecords.get(pos).getMode().equals(MODES.CHRONO) || !ctRecords.get(pos).isReset() || (ctRecords.get(pos).getTimeDef() > 0)) {
            b = ctRecords.get(pos).isRunning();
            colorBox = viewHolder.buttonStartStop.getColorBox();
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX(), b ? BACKGROUND_COLOR : BUTTON_DARK_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX(), b ? PRESSED_COLOR : BACKGROUND_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX()).RGBString);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX()).RGBString);
            viewHolder.buttonStartStop.updateDisplayColors();
            viewHolder.buttonStartStop.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonStartStop.setVisibility(View.INVISIBLE);
        }

        if (ctRecords.get(pos).isRunning() || ctRecords.get(pos).isSplitted()) {
            b = ctRecords.get(pos).isSplitted();
            colorBox = viewHolder.buttonSplit.getColorBox();
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX(), b ? BACKGROUND_COLOR : BUTTON_DARK_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX(), b ? PRESSED_COLOR : BACKGROUND_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX()).RGBString);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX()).RGBString);
            viewHolder.buttonSplit.updateDisplayColors();
            viewHolder.buttonSplit.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonSplit.setVisibility(View.INVISIBLE);
        }

        if (!ctRecords.get(pos).isRunning() && !ctRecords.get(pos).isReset()) {
            colorBox = viewHolder.buttonReset.getColorBox();
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX(), BUTTON_DARK_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX(), BACKGROUND_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX()).RGBString);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX()).RGBString);
            viewHolder.buttonReset.updateDisplayColors();
            viewHolder.buttonReset.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonReset.setVisibility(View.INVISIBLE);
        }

        if (ctRecords.get(pos).getMode().equals(MODES.TIMER) && ctRecords.get(pos).isRunning()) {
            b = ctRecords.get(pos).isClockAppAlarmOn();
            colorBox = viewHolder.buttonClockAppAlarm.getColorBox();
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX(), b ? BACKGROUND_COLOR : BUTTON_DARK_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX(), b ? PRESSED_COLOR : BACKGROUND_COLOR);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK_COLOR.INDEX()).RGBString);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK_COLOR.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT_COLOR.INDEX()).RGBString);
            viewHolder.buttonClockAppAlarm.updateDisplayColors();
            viewHolder.buttonClockAppAlarm.setVisibility(View.VISIBLE);
        } else {
            viewHolder.buttonClockAppAlarm.setVisibility(View.INVISIBLE);
        }

        viewHolder.mainCtListItemDotMatrixDisplayUpdater.displayTimeAndLabel(ctRecords.get(pos), showExpirationTime, nowm);
        viewHolder.buttonDotMatrixDisplayTimeLabel.updateDisplay();
    }

    private MainCtListItemViewHolder buildViewHolder(View rowView) {
        MainCtListItemViewHolder viewHolder = new MainCtListItemViewHolder();
        viewHolder.buttonModeSelection = rowView.findViewById(R.id.BTN_MODE_SELECTION);
        viewHolder.buttonStartStop = rowView.findViewById(R.id.BTN_START_STOP);
        viewHolder.buttonSplit = rowView.findViewById(R.id.BTN_SPLIT);
        viewHolder.buttonReset = rowView.findViewById(R.id.BTN_RESET);
        viewHolder.buttonClockAppAlarm = rowView.findViewById(R.id.BTN_CLOCK_APP_ALARM);
        viewHolder.buttonDotMatrixDisplayTimeLabel = rowView.findViewById(R.id.BTN_DOT_MATRIX_DISPLAY_TIME_LABEL);
        viewHolder.mainCtListItemDotMatrixDisplayUpdater = new MainCtListItemDotMatrixDisplayUpdater(viewHolder.buttonDotMatrixDisplayTimeLabel);
        return viewHolder;
    }

    private void setupViewHolder(MainCtListItemViewHolder viewHolder, View rowView, int position) {
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;
        final String BACK_COLOR = "000000";

        final View rowv = rowView;
        final int pos = position;

        viewHolder.buttonModeSelection.setOutlineStrokeWidthDp(0);
        viewHolder.buttonModeSelection.setPNGImageResource((ctRecords.get(pos).getMode().equals(MODES.CHRONO)) ? R.drawable.main_chrono : R.drawable.main_timer);
        viewHolder.buttonModeSelection.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonModeSelection.setCustomOnClickListener(new ImageButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonModeSelectionClick(rowv, pos);
            }
        });
        viewHolder.buttonStartStop.setOutlineStrokeWidthDp(0);
        viewHolder.buttonStartStop.setPNGImageResource(R.drawable.main_start_stop);
        viewHolder.buttonStartStop.setCustomOnClickListener(new ImageButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonStartStopClick(rowv, pos);
            }
        });
        viewHolder.buttonSplit.setOutlineStrokeWidthDp(0);
        viewHolder.buttonSplit.setPNGImageResource(R.drawable.main_split);
        viewHolder.buttonSplit.setCustomOnClickListener(new ImageButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonSplitClick(rowv, pos);
            }
        });
        viewHolder.buttonReset.setOutlineStrokeWidthDp(0);
        viewHolder.buttonReset.setPNGImageResource(R.drawable.main_reset);
        viewHolder.buttonReset.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonReset.setCustomOnClickListener(new ImageButtonView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onButtonResetClick(rowv, pos);
            }
        });
        viewHolder.buttonClockAppAlarm.setOutlineStrokeWidthDp(0);
        viewHolder.buttonClockAppAlarm.setPNGImageResource(R.drawable.main_bell);
        viewHolder.buttonClockAppAlarm.setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
        viewHolder.buttonClockAppAlarm.setCustomOnClickListener(new ImageButtonView.onCustomClickListener() {
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
        viewHolder.mainCtListItemDotMatrixDisplayUpdater.setupDimensions();
        ColorBox colorBox = viewHolder.buttonClockAppAlarm.getColorBox();
        colorBox.setColor(ColorUtils.DOT_MATRIX_COLOR_TYPES.BACK_SCREEN_COLOR.INDEX(), BACK_COLOR);
    }

    private void launchCtDisplayActivity(int idct) {
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(context, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        context.startActivity(callingIntent);
    }
}
