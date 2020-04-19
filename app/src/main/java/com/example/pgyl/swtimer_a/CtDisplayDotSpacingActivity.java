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
import static com.example.pgyl.pekislib_a.DotMatrixDisplayView.DOT_FORM;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentEntryInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentEntryInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInInputButtonsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInPresetsActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getOrientationDotMatrixDisplayDotSpacingCoeffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentOrDefaultColorsInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentOrDefaultDotMatrixDisplayDotForm;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getDotMatrixDisplayDotSpacingCoeffsLabels;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.isColdStartStatusInCtDisplayDotSpacingActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayDotSpacingActivity;

public class CtDisplayDotSpacingActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        NEXT_DOT_SPACING_COEFF(""), CANCEL("Cancel"), DOT_SPACING_COEFF_VALUE(""), PRESETS("Presets"), OK("OK");

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

    private enum SHP_KEY_NAMES {DOT_SPACING_COEFF_INDEX}

    private final int DOT_SPACING_COEFF_INDEX_DEFAULT_VALUE = 1;  //  les records d'espacement dans les tables de la DB stockent leur identifiant comme 1er élément (offset 0) => les espacements commencent à l'offset 1
    //endregion
    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private CtDisplayDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
    private Button[] buttons;
    private SeekBar dotSpacingCoeffSeekBar;
    private CtRecord currentCtRecord;
    private int dotSpacingCoeffIndex;
    private String[] colors;
    private String[] dotSpacingCoeffs;
    private String[] dotSpacingCoeffsLabels;
    private DOT_FORM dotForm;
    int orientation;
    private boolean validReturnFromCalledActivity;
    private String calledActivity;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle("Set Dot Spacing");
        setupOrientationLayout();
        setupButtons();
        setupSeekBars();
        setupDotMatrixDisplay();
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        currentCtRecord = null;
        setCurrentDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity(stringShelfDatabase, dotSpacingCoeffs);
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + "." + getClass().getSimpleName() + SHP_FILE_NAME_SUFFIX;
        setupStringShelfDatabase();
        int idct = getIntent().getIntExtra(CtDisplayActivity.CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        currentCtRecord = chronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), this);
        colors = getCurrentOrDefaultColorsInCtDisplayActivity(stringShelfDatabase)[getColorTableIndex(getDotMatrixDisplayColorsTableName())];  //  Prendre les couleurs actuelles de CtDisplayActivity
        dotSpacingCoeffs = getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity(stringShelfDatabase);
        dotSpacingCoeffsLabels = getDotMatrixDisplayDotSpacingCoeffsLabels(stringShelfDatabase);
        dotForm = getCurrentOrDefaultDotMatrixDisplayDotForm(stringShelfDatabase);
        orientation = getResources().getConfiguration().orientation;

        if (isColdStartStatusInCtDisplayDotSpacingActivity(stringShelfDatabase)) {
            setStartStatusInCtDisplayDotSpacingActivity(stringShelfDatabase, ACTIVITY_START_STATUS.HOT);
            dotSpacingCoeffIndex = getOrientationDotMatrixDisplayDotSpacingCoeffIndex(orientation);
        } else {
            dotSpacingCoeffIndex = getSHPDotSpacingCoeffIndex();
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (returnsFromInputButtonsActivity()) {
                    String dotSpacingCoeffText = getCurrentEntryInInputButtonsActivity(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName(), dotSpacingCoeffIndex);
                    dotSpacingCoeffs[dotSpacingCoeffIndex] = dotSpacingCoeffText;
                }
                if (returnsFromPresetsActivity()) {
                    dotSpacingCoeffs = getCurrentPresetInPresetsActivity(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName());
                }
            }
        }
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayDotForm();
        setupDotMatrixDisplayDotSpacingCoeff();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
        updateDisplayButtonTextNextDotSpacingCoeff();
        updateDisplaySeekBarsProgress();
        updateDisplayButtonTextDotSpacingCoeffValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.PRESETS.toString();
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
        if (command.equals(COMMANDS.NEXT_DOT_SPACING_COEFF)) {
            onButtonClickNextDotSpacingCoeff();
        }
        if (command.equals(COMMANDS.CANCEL)) {
            onButtonClickCancel();
        }
        if (command.equals(COMMANDS.DOT_SPACING_COEFF_VALUE)) {
            onButtonClickDotSpacingCoeffValue();
        }
        if (command.equals(COMMANDS.PRESETS)) {
            onButtonClickPresets();
        }
        if (command.equals(COMMANDS.OK)) {
            onButtonClickOK();
        }
    }

    private void onButtonClickNextDotSpacingCoeff() {
        dotSpacingCoeffIndex = dotSpacingCoeffIndex + 1;
        if (dotSpacingCoeffIndex >= dotSpacingCoeffs.length) {
            dotSpacingCoeffIndex = 1;
        }
        updateDisplayButtonTextNextDotSpacingCoeff();
        updateDisplaySeekBarsProgress();
        updateDisplayButtonTextDotSpacingCoeffValue();
        if (dotSpacingCoeffIndex == getOrientationDotMatrixDisplayDotSpacingCoeffIndex(orientation)) {
            setupDotMatrixDisplayDotSpacingCoeff();
            rebuildDotMatrixDisplayStructure();
            updateDisplayDotMatrixDisplay();
        }
    }

    private void onButtonClickCancel() {
        finish();
    }

    private void onButtonClickDotSpacingCoeffValue() {
        setCurrentEntryInInputButtonsActivity(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName(), dotSpacingCoeffIndex, getSeekBarsProgressString());
        launchInputButtonsActivity();
    }

    private void onButtonClickPresets() {
        setCurrentPresetInPresetsActivity(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName(), dotSpacingCoeffs);
        launchPresetsActivity();
    }

    private void onButtonClickOK() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void onSeekBarProgressChanged(boolean fromUser) {
        if (fromUser) {
            dotSpacingCoeffs[dotSpacingCoeffIndex] = getSeekBarsProgressString();
            updateDisplayButtonTextDotSpacingCoeffValue();
            if (dotSpacingCoeffIndex == getOrientationDotMatrixDisplayDotSpacingCoeffIndex(orientation)) {
                setupDotMatrixDisplayDotSpacingCoeff();
                rebuildDotMatrixDisplayStructure();
                updateDisplayDotMatrixDisplay();
            }
        }
    }

    private void updateDisplayDotMatrixDisplay() {
        dotMatrixDisplayUpdater.displayTime(msToTimeFormatD(currentCtRecord.getTimeDefInit(), TIME_UNIT_PRECISION));
    }

    private void updateDisplayButtonTextNextDotSpacingCoeff() {
        final String SYMBOL_NEXT = " >";               //  Pour signifier qu'on peut passer au suivant en poussant sur le bouton

        buttons[COMMANDS.NEXT_DOT_SPACING_COEFF.INDEX()].setText(dotSpacingCoeffsLabels[dotSpacingCoeffIndex] + SYMBOL_NEXT);
    }

    private void updateDisplayButtonTextDotSpacingCoeffValue() {
        buttons[COMMANDS.DOT_SPACING_COEFF_VALUE.INDEX()].setText(getSeekBarsProgressString());
    }

    private void updateDisplaySeekBarsProgress() {
        int percent = Integer.parseInt(dotSpacingCoeffs[dotSpacingCoeffIndex]);  //  0..100
        dotSpacingCoeffSeekBar.setProgress(percent);
    }

    private String getSeekBarsProgressString() {
        return String.valueOf(dotSpacingCoeffSeekBar.getProgress());
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putInt(SHP_KEY_NAMES.DOT_SPACING_COEFF_INDEX.toString(), dotSpacingCoeffIndex);
        shpEditor.commit();
    }

    private int getSHPDotSpacingCoeffIndex() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SHP_KEY_NAMES.DOT_SPACING_COEFF_INDEX.toString(), DOT_SPACING_COEFF_INDEX_DEFAULT_VALUE);
    }

    private void setupOrientationLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.ctdisplaydotspacing_p);
        } else {
            setContentView(R.layout.ctdisplaydotspacing_l);
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

    private void setupDotMatrixDisplayDotSpacingCoeff() {
        dotMatrixDisplayUpdater.setDotSpacingCoeff(dotSpacingCoeffs[getOrientationDotMatrixDisplayDotSpacingCoeffIndex(orientation)]);    //  L'apparence va devoir changer
    }

    private void setupDotMatrixDisplayDotForm() {
        dotMatrixDisplayUpdater.setDotForm(dotForm);
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

    private void setupSeekBars() {
        try {
            dotSpacingCoeffSeekBar = findViewById(R.id.SEEKB_DOT_SPACING_COEFF);
            dotSpacingCoeffSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    onSeekBarProgressChanged(fromUser);
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

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void launchInputButtonsActivity() {
        setStartStatusInInputButtonsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, InputButtonsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), dotSpacingCoeffsLabels[dotSpacingCoeffIndex]);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayDotSpacingCoeffsTableName());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.INDEX.toString(), dotSpacingCoeffIndex);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX());
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setStartStatusInPresetsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), "Dot spacing coeffs");
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayDotSpacingCoeffsTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplaydotspacingactivity);
        startActivity(callingIntent);
    }

    private boolean returnsFromInputButtonsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString()));
    }

    private boolean returnsFromPresetsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.PRESETS.toString()));
    }

}
