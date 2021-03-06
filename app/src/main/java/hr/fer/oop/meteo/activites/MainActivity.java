package hr.fer.oop.meteo.activites;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;

import hr.fer.oop.meteo.R;
import hr.fer.oop.meteo.net.RestFactory;
import hr.fer.oop.meteo.net.RestInterface;
import hr.fer.oop.meteo.util.Clock;

public class MainActivity extends AppCompatActivity {

    private ListView listCity;
    private ProgressBar loadSpinner;

    PlaceAdapter pa = null;

    Clock clkDate1 = null;
    Clock clkDate2 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listCity = findViewById(R.id.cities);

        final TextView date = findViewById(R.id.chose_date_string);
        final Button choseDate = findViewById(R.id.chose_date);

        final TextView date2 = findViewById(R.id.chose_date_string2);
        final Button choseDate2 = findViewById(R.id.chose_date2);

        final Switch rangeOn = findViewById(R.id.range);

        final Button request = findViewById(R.id.request);

        choseDate.setOnClickListener((View v) -> {
            Clock clk = new Clock();

            final DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        clkDate1 = new Clock(year, month, dayOfMonth);
                        date.setText(clkDate1.toString());
                        choseDate2.setEnabled(true);
                        request.setEnabled(true);

                        if (!rangeOn.isChecked()) rangeOn.setVisibility(View.VISIBLE);
                    }, clk.getYear(), clk.getMonth(), clk.getDay());
            datePickerDialog.getDatePicker().setMaxDate(clk.getDateInMillis());
            datePickerDialog.show();
            request.setVisibility(View.VISIBLE);
        });

        choseDate2.setOnClickListener((View v) -> {
            Clock clk = new Clock();

            final DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        clkDate2 = new Clock(year, month, dayOfMonth);
                        date2.setText(clkDate2.toString());
                        request.setEnabled(true);
                        request.setVisibility(View.VISIBLE);
                    }, clk.getYear(), clk.getMonth(), clk.getDay());
            datePickerDialog.getDatePicker().setMinDate(clkDate1.getDateInMillis());
            datePickerDialog.getDatePicker().setMaxDate(clk.getDateInMillis());
            datePickerDialog.show();
        });

        rangeOn.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                if (clkDate2 == null) request.setEnabled(false);
                date2.setVisibility(View.VISIBLE);
                choseDate2.setVisibility(View.VISIBLE);
            } else {
                clkDate2 = null;
                if (clkDate1 == null) request.setEnabled(false);
                date2.setText("");
                date2.setVisibility(View.INVISIBLE);
                choseDate2.setVisibility(View.INVISIBLE);
            }
        });

        request.setOnClickListener((View v) -> {
            @SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    loadSpinner = findViewById(R.id.pBar);
                    loadSpinner.setVisibility(View.VISIBLE);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    RestInterface rest = RestFactory.getInstance();
                    if (clkDate2 != null) {
                        rest.newPlaces(clkDate1.toString(), clkDate2.toString());
                    } else {
                        rest.newPlaces(clkDate1.toString());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void finished) {
                    @SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, List<String>> task2 = new AsyncTask<Void, Void, List<String>>() {

                        @Override
                        protected List<String> doInBackground(Void... voids) {
                            RestInterface rest = RestFactory.getInstance();
                            if (clkDate2 != null) {
                                // rest.newPlaces(clkDate1.toString(), clkDate2.toString());
                                return rest.getPlacesByDates(clkDate1.toString(), clkDate2.toString());
                            } else {
                                //rest.newPlaces(clkDate1.toString());
                                return rest.getPlacesByDate(clkDate1.toString());
                            }
                        }

                        protected void onPostExecute(List<String> places) {
                            loadSpinner.setVisibility(View.INVISIBLE);
                            Collections.sort(places);
                            updatePlacesList(places);
                        }
                    };
                    task2.execute();
                }
            };
            task.execute();
        });

        listCity.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {

            Object itemAtPosition = parent.getItemAtPosition(position);
            String place = (String) itemAtPosition;
            Intent intent = new Intent(MainActivity.this, ChosenCityActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("chosenCity", place);
            intent.putExtra("date1", clkDate1.toString());
            if (clkDate2 != null) intent.putExtra("date2", clkDate2.toString());
            startActivity(intent);
        });

    }

    private void updatePlacesList(List<String> places) {
        this.pa = new PlaceAdapter(this,
                android.R.layout.simple_list_item_1, places);
        listCity.setAdapter(this.pa);
    }

    private class PlaceAdapter extends ArrayAdapter<String> {

        public PlaceAdapter(Context context, int textViewResourceId, List<String> places) {
            super(context, textViewResourceId, places);
        }
    }
}
