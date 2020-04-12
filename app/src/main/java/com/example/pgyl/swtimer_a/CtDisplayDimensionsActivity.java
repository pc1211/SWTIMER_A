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
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDimensionsInterDotCoeffLandscapeIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDimensionsInterDotCoeffPortraitIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDimensionsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentOrDefaultColorsInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentOrDefaultDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getDotMatrixDisplayDimensionsLabels;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.isColdStartStatusInCtDisplayDimensionsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayDimensionsActivity;

public class CtDisplayDimensionsActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        NEXT_DIMENSION(""), CANCEL("Cancel"), DIMENSION_VALUE(""), PRESETS("Presets"), OK("OK");

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

    private enum SHP_KEY_NAMES {DIMENSION_INDEX}

    private final int DIMENSION_INDEX_DEFAULT_VALUE = 1;  //  les records de dimension dans les tables de la DB stockent leur identifiant comme 1er élément (offset 0) => les dimensions commencent à l'offset 1
    //endregion
    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private CtDisplayDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
    private Button[] buttons;
    private SeekBar dimensionSeekBar;
    private CtRecord currentCtRecord;
    private int dotMatrixDisplayDimensionIndex;
    private String[] colors;
    private String[] dotMatrixDisplayDimensions;
    private String[] dotMatrixDisplayDimensionsLabels;
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
        getActionBar().setTitle("Set Dimensions (inter dot size)");
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
        setCurrentDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity(stringShelfDatabase, dotMatrixDisplayDimensions);
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
        dotMatrixDisplayDimensions = getCurrentOrDefaultDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity(stringShelfDatabase);
        dotMatrixDisplayDimensionsLabels = getDotMatrixDisplayDimensionsLabels(stringShelfDatabase);
        orientation = getResources().getConfiguration().orientation;

        if (isColdStartStatusInCtDisplayDimensionsActivity(stringShelfDatabase)) {
            setStartStatusInCtDisplayDimensionsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.HOT);
            dotMatrixDisplayDimensionIndex = getOrientationDotMatrixDisplayDimensionIndex();
        } else {
            dotMatrixDisplayDimensionIndex = getSHPDimensionIndex();
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (returnsFromInputButtonsActivity()) {
                    String dimensionText = getCurrentEntryInInputButtonsActivity(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName(), dotMatrixDisplayDimensionIndex);
                    dotMatrixDisplayDimensions[dotMatrixDisplayDimensionIndex] = dimensionText;
                }
                if (returnsFromPresetsActivity()) {
                    dotMatrixDisplayDimensions = getCurrentPresetInPresetsActivity(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName());
                }
            }
        }
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayInterDotSize();
        rebuildDotMatrixDisplayDimensions();
        updateDisplayDotMatrixDisplay();
        updateDisplayButtonTextNextDimension();
        updateDisplaySeekBarsProgress();
        updateDisplayButtonTextDimensionValue();
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
        if (command.equals(COMMANDS.NEXT_DIMENSION)) {
            onButtonClickNextDimension();
        }
        if (command.equals(COMMANDS.CANCEL)) {
            onButtonClickCancel();
        }
        if (command.equals(COMMANDS.DIMENSION_VALUE)) {
            onButtonClickDimensionValue();
        }
        if (command.equals(COMMANDS.PRESETS)) {
            onButtonClickPresets();
        }
        if (command.equals(COMMANDS.OK)) {
            onButtonClickOK();
        }
    }

    private void onButtonClickNextDimension() {
        dotMatrixDisplayDimensionIndex = dotMatrixDisplayDimensionIndex + 1;
        if (dotMatrixDisplayDimensionIndex >= dotMatrixDisplayDimensions.length) {
            dotMatrixDisplayDimensionIndex = 1;
        }
        updateDisplayButtonTextNextDimension();
        updateDisplaySeekBarsProgress();
        updateDisplayButtonTextDimensionValue();
        if (dotMatrixDisplayDimensionIndexMatchesCurrentOrientation()) {
            setupDotMatrixDisplayInterDotSize();
            rebuildDotMatrixDisplayDimensions();
            rebuildDotMatrixDisplayDrawParameters();
            updateDisplayDotMatrixDisplay();
        }
    }

    private void onButtonClickCancel() {
        finish();
    }

    private void onButtonClickDimensionValue() {
        setCurrentEntryInInputButtonsActivity(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName(), dotMatrixDisplayDimensionIndex, getSeekBarsProgressString());
        launchInputButtonsActivity();
    }

    private void onButtonClickPresets() {
        setCurrentPresetInPresetsActivity(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName(), dotMatrixDisplayDimensions);
        launchPresetsActivity();
    }

    private void onButtonClickOK() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void onSeekBarProgressChanged(boolean fromUser) {
        if (fromUser) {
            dotMatrixDisplayDimensions[dotMatrixDisplayDimensionIndex] = getSeekBarsProgressString();
            updateDisplayButtonTextDimensionValue();
            if (dotMatrixDisplayDimensionIndexMatchesCurrentOrientation()) {
                setupDotMatrixDisplayInterDotSize();
                rebuildDotMatrixDisplayDimensions();
                rebuildDotMatrixDisplayDrawParameters();
                updateDisplayDotMatrixDisplay();
            }
        }
    }

    private void updateDisplayDotMatrixDisplay() {
        dotMatrixDisplayUpdater.displayTime(msToTimeFormatD(currentCtRecord.getTimeDefInit(), TIME_UNIT_PRECISION));
    }

    private void updateDisplayButtonTextNextDimension() {
        final String SYMBOL_NEXT = " >";               //  Pour signifier qu'on peut passer au suivant en poussant sur le bouton

        buttons[COMMANDS.NEXT_DIMENSION.INDEX()].setText(dotMatrixDisplayDimensionsLabels[dotMatrixDisplayDimensionIndex] + SYMBOL_NEXT);
    }

    private void updateDisplayButtonTextDimensionValue() {
        buttons[COMMANDS.DIMENSION_VALUE.INDEX()].setText(getSeekBarsProgressString());
    }

    private boolean dotMatrixDisplayDimensionIndexMatchesCurrentOrientation() {
        return (((orientation == Configuration.ORIENTATION_PORTRAIT) && (dotMatrixDisplayDimensionIndex == getDotMatrixDisplayDimensionsInterDotCoeffPortraitIndex()))
                || ((orientation == Configuration.ORIENTATION_LANDSCAPE) && (dotMatrixDisplayDimensionIndex == getDotMatrixDisplayDimensionsInterDotCoeffLandscapeIndex())));
    }

    private void updateDisplaySeekBarsProgress() {
        int percent = Integer.parseInt(dotMatrixDisplayDimensions[dotMatrixDisplayDimensionIndex]);  //  0..100
        dimensionSeekBar.setProgress(percent);
    }

    private int getOrientationDotMatrixDisplayDimensionIndex() {
        return (orientation == Configuration.ORIENTATION_PORTRAIT) ? getDotMatrixDisplayDimensionsInterDotCoeffPortraitIndex() : getDotMatrixDisplayDimensionsInterDotCoeffLandscapeIndex();
    }

    private String getSeekBarsProgressString() {
        return String.valueOf(dimensionSeekBar.getProgress());
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putInt(SHP_KEY_NAMES.DIMENSION_INDEX.toString(), dotMatrixDisplayDimensionIndex);
        shpEditor.commit();
    }

    private int getSHPDimensionIndex() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SHP_KEY_NAMES.DIMENSION_INDEX.toString(), DIMENSION_INDEX_DEFAULT_VALUE);
    }

    private void setupOrientationLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.ctdisplaydimensions_p);
        } else {
            setContentView(R.layout.ctdisplaydimensions_l);
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

    private void setupDotMatrixDisplayInterDotSize() {
        dotMatrixDisplayUpdater.setInterDotSizeCoeff(dotMatrixDisplayDimensions[getOrientationDotMatrixDisplayDimensionIndex()]);    //  L'apparence va devoir changer
    }

    private void rebuildDotMatrixDisplayDimensions() {
        dotMatrixDisplayUpdater.rebuildDimensions();
    }

    private void rebuildDotMatrixDisplayDrawParameters() {
        dotMatrixDisplayUpdater.rebuildDrawParameters();
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
            dimensionSeekBar = findViewById(R.id.SEEKB_DIMENSION);
            dimensionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), dotMatrixDisplayDimensionsLabels[dotMatrixDisplayDimensionIndex]);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayDimensionsTableName());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.INDEX.toString(), dotMatrixDisplayDimensionIndex);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX());
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setStartStatusInPresetsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), "Inter dot coeffs");
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayDimensionsTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplaydimensionsactivity);
        startActivity(callingIntent);
    }

    private boolean returnsFromInputButtonsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString()));
    }

    private boolean returnsFromPresetsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.PRESETS.toString()));
    }

}
