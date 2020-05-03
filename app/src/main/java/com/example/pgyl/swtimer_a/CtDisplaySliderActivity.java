package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.MainActivity;
import com.example.pgyl.pekislib_a.PresetsActivity;
import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentFromActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentsFromActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentsFromMultipleTablesFromActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getLabels;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getTableIndex;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.isColdStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentForActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getCoeffTableNames;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotCornerRadiusCoeffTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayScrollSpeedTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;

public class CtDisplaySliderActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        NEXT_VALUE(""), CANCEL("Cancel"), VALUE(""), PRESETS("Presets"), OK("OK");

        private String valueText;

        COMMANDS(String valueText) {
            this.valueText = valueText;
        }

        public String TEXT() {
            return valueText;
        }

        public int INDEX() {
            return ordinal();
        }
    }

    private enum SHP_KEY_NAMES {VALUE_INDEX}

    private final int VALUE_INDEX_DEFAULT = 1;  //  les records dans les tables de la DB stockent leur identifiant comme 1er élément (offset 0) => les data commencent à l'offset 1
    //endregion
    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private CtDisplayDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
    private Button[] buttons;
    private SeekBar seekBarForValue;
    private int maxSeekBarForValue;
    private CtRecord currentCtRecord;
    private int firstPortraitValueIndex;
    private int firstLanscapeValueIndex;
    private int coeffIndex;
    private String[] colors;
    private String[] coeffTableNames;
    private int coeffTableIndex;
    private String[][] coeffs;
    private String[] labels;
    private String tableName;
    private String tableDescription;
    private int orientation;
    private boolean validReturnFromCalledActivity;
    private String calledActivityName;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(getIntent().getStringExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString()));
        setupOrientationLayout();
        setupButtons();
        setupSeekBarForValue();
        setupDotMatrixDisplay();
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        currentCtRecord = null;
        setCurrentsForActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_SLIDER.toString(), tableName, coeffs[coeffTableIndex]);
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + "." + getClass().getSimpleName() + SHP_FILE_NAME_SUFFIX;
        tableName = getIntent().getStringExtra(TABLE_EXTRA_KEYS.TABLE.toString());
        firstPortraitValueIndex = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.FIRST_PORTRAIT_VALUE_INDEX.toString(), NOT_FOUND);
        firstLanscapeValueIndex = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.FIRST_LANDSCAPE_VALUE_INDEX.toString(), NOT_FOUND);
        maxSeekBarForValue = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.SEEKBAR_MAX_VALUE.toString(), NOT_FOUND);
        tableDescription = getIntent().getStringExtra(TABLE_EXTRA_KEYS.DESCRIPTION.toString());
        int idct = getIntent().getIntExtra(CtDisplayActivity.CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        setupStringShelfDatabase();
        currentCtRecord = chronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), this);
        coeffTableNames = getCoeffTableNames();
        coeffs = getCurrentsFromMultipleTablesFromActivity(stringShelfDatabase, coeffTableNames, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
        coeffTableIndex = getTableIndex(coeffTableNames, tableName);
        coeffs[coeffTableIndex] = getCurrentsFromActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_SLIDER.toString(), tableName);
        labels = getLabels(stringShelfDatabase, tableName);
        colors = getCurrentsFromActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayColorsTableName());  //  Prendre les couleurs actuelles de CtDisplayActivity
        orientation = getResources().getConfiguration().orientation;

        if (isColdStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_SLIDER.toString())) {
            setStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_SLIDER.toString(), ACTIVITY_START_STATUS.HOT);
            coeffIndex = getOrientationValueIndex(orientation);
        } else {
            coeffIndex = getSHPValueIndex();
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString())) {
                    coeffs[coeffTableIndex][coeffIndex] = getCurrentFromActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), tableName, coeffIndex);
                }
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.PRESETS.toString())) {
                    coeffs[coeffTableIndex] = getCurrentsFromActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), tableName);
                }
            }
        }
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayCoeffs();
        setupMaxSeekBarForValue();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
        updateDisplayButtonTextNextValue();
        updateDisplaySeekBarForValueProgress();
        updateDisplayButtonTextValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX()) {
            calledActivityName = PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.INDEX()) {
            calledActivityName = PEKISLIB_ACTIVITIES.PRESETS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.example.pgyl.pekislib_a.R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.example.pgyl.pekislib_a.R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onButtonClick(COMMANDS command) {
        if (command.equals(COMMANDS.NEXT_VALUE)) {
            onButtonClickNextValue();
        }
        if (command.equals(COMMANDS.CANCEL)) {
            onButtonClickCancel();
        }
        if (command.equals(COMMANDS.VALUE)) {
            onButtonClickValue();
        }
        if (command.equals(COMMANDS.PRESETS)) {
            onButtonClickPresets();
        }
        if (command.equals(COMMANDS.OK)) {
            onButtonClickOK();
        }
    }

    private void onButtonClickNextValue() {
        coeffIndex = coeffIndex + 1;
        if (coeffIndex >= coeffs[coeffTableIndex].length) {
            coeffIndex = 1;
        }
        updateDisplayButtonTextNextValue();
        updateDisplaySeekBarForValueProgress();
        updateDisplayButtonTextValue();
        updateSetupDotMatrixDisplay();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
    }

    private void onButtonClickCancel() {
        finish();
    }

    private void onButtonClickValue() {
        launchInputButtonsActivity();
    }

    private void onButtonClickPresets() {
        launchPresetsActivity();
    }

    private void onButtonClickOK() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void onSeekBarForValueProgressChanged(boolean fromUser) {
        if (fromUser) {
            coeffs[coeffTableIndex][coeffIndex] = getSeekBarsForValueProgressString();
            updateDisplayButtonTextValue();
            updateSetupDotMatrixDisplay();
            rebuildDotMatrixDisplayStructure();
            updateDisplayDotMatrixDisplay();
        }
    }

    private void updateDisplayDotMatrixDisplay() {
        dotMatrixDisplayUpdater.displayTimeAndLabel(msToTimeFormatD(currentCtRecord.getTimeDefInit(), TIME_UNIT_PRECISION), currentCtRecord.getLabel());
    }

    private void updateSetupDotMatrixDisplay() {
        if (coeffIndex == getOrientationValueIndex(orientation)) {
            setupDotMatrixDisplayCoeffs();
        }
    }

    private void updateDisplayButtonTextNextValue() {
        final String SYMBOL_NEXT = " >";               //  Pour signifier qu'on peut passer au suivant en poussant sur le bouton

        buttons[COMMANDS.NEXT_VALUE.INDEX()].setText(labels[coeffIndex] + SYMBOL_NEXT);
    }

    private void updateDisplayButtonTextValue() {
        buttons[COMMANDS.VALUE.INDEX()].setText(getSeekBarsForValueProgressString());
    }

    private void updateDisplaySeekBarForValueProgress() {
        int percent = Integer.parseInt(coeffs[coeffTableIndex][coeffIndex]);  //  0..100
        seekBarForValue.setProgress(percent);
    }

    private String getSeekBarsForValueProgressString() {
        return String.valueOf(seekBarForValue.getProgress());
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putInt(SHP_KEY_NAMES.VALUE_INDEX.toString(), coeffIndex);
        shpEditor.commit();
    }

    private int getSHPValueIndex() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SHP_KEY_NAMES.VALUE_INDEX.toString(), VALUE_INDEX_DEFAULT);
    }

    public int getOrientationValueIndex(int orientation) {
        return (orientation == Configuration.ORIENTATION_PORTRAIT) ? firstPortraitValueIndex : firstLanscapeValueIndex;
    }

    private void setupOrientationLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.ctdisplayslider_p);
        } else {
            setContentView(R.layout.ctdisplayslider_l);
        }
    }

    private void setupDotMatrixDisplay() {  //  Pour Afficher HH:MM:SS.CC et éventuellement un label
        dotMatrixDisplayView = findViewById(R.id.DOT_MATRIX_DISPLAY);
    }

    private void setupDotMatrixDisplayUpdater(CtRecord currentCtRecord) {
        dotMatrixDisplayUpdater = new CtDisplayDotMatrixDisplayUpdater(dotMatrixDisplayView, currentCtRecord);
    }

    private void setupDotMatrixDisplayColors() {
        dotMatrixDisplayUpdater.setColors(colors);
    }


    private void setupDotMatrixDisplayCoeffs() {
        dotMatrixDisplayUpdater.setDotSpacingCoeff(coeffs[getTableIndex(coeffTableNames, getDotMatrixDisplayDotSpacingCoeffsTableName())][getOrientationValueIndex(orientation)]);    //  L'apparence va devoir changer
        dotMatrixDisplayUpdater.setDotCornerRadiusCoeff(coeffs[getTableIndex(coeffTableNames, getDotMatrixDisplayDotCornerRadiusCoeffTableName())][getOrientationValueIndex(orientation)]);
        dotMatrixDisplayUpdater.setScrollSpeed(coeffs[getTableIndex(coeffTableNames, getDotMatrixDisplayScrollSpeedTableName())][getOrientationValueIndex(orientation)]);
        if (tableName.equals(getDotMatrixDisplayScrollSpeedTableName())) {
            dotMatrixDisplayUpdater.startAutomatic();
        } else {
            dotMatrixDisplayUpdater.stopAutomatic();
        }
    }

    private void rebuildDotMatrixDisplayStructure() {
        dotMatrixDisplayUpdater.rebuildStructure();
    }

    private void setupButtons() {
        final String BUTTON_XML_PREFIX = "BTN_";

        buttons = new Button[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values()) {
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_XML_PREFIX + command.toString()).getInt(rid));   //  BTN_... dans le XML
                buttons[command.INDEX()].setText(command.TEXT());
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setOnClickListener(new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onButtonClick(fcommand);
                    }
                });
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupSeekBarForValue() {
        try {
            seekBarForValue = findViewById(R.id.SEEKB_VALUE);
            seekBarForValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    onSeekBarForValueProgressChanged(fromUser);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        } catch (IllegalArgumentException | SecurityException ex) {
            Logger.getLogger(InputButtonsActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupMaxSeekBarForValue() {
        seekBarForValue.setMax(maxSeekBarForValue);
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void launchInputButtonsActivity() {
        setCurrentForActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), tableName, coeffIndex, getSeekBarsForValueProgressString());
        setStartStatusOfActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, InputButtonsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), labels[coeffIndex]);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), tableName);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.INDEX.toString(), coeffIndex);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX());
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setCurrentsForActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), tableName, coeffs[coeffTableIndex]);
        setStartStatusOfActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), tableDescription);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), tableName);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplayslideractivity);
        startActivity(callingIntent);
    }

}
