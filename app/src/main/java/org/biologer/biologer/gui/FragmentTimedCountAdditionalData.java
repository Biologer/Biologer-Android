package org.biologer.biologer.gui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.biologer.biologer.R;
import org.biologer.biologer.adapters.WeatherDataViewModel;

public class FragmentTimedCountAdditionalData extends Fragment {
    WeatherDataViewModel weatherDataViewModel;
    private static final String TAG = "Biologer.TimedCountFr";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timed_count_additional_data, container, false);

        EditText editTextTemperature = view.findViewById(R.id.timed_count_edit_text_temperature);
        editTextTemperature.setText(String.valueOf(weatherDataViewModel.getTemperatureData().getValue()));
        editTextTemperature.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Double temperature = Double.valueOf(new_value);
                        weatherDataViewModel.setTemperatureData(temperature);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    weatherDataViewModel.setTemperatureData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextCloudiness = view.findViewById(R.id.timed_count_edit_text_cloudiness);
        Integer cloudiness = weatherDataViewModel.getCloudinessData().getValue();
        if (cloudiness != null) {
            editTextCloudiness.setText(String.valueOf(cloudiness));
        }
        editTextCloudiness.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Integer cloudiness = Integer.valueOf(new_value);
                        weatherDataViewModel.setCloudinessData(cloudiness);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    weatherDataViewModel.setCloudinessData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextWindSpeed = view.findViewById(R.id.timed_count_edit_text_wind_speed);
        Integer wind_speed = weatherDataViewModel.getWindSpeedData().getValue();
        if (wind_speed != null) {
            editTextWindSpeed.setText(String.valueOf(wind_speed));
        }
        editTextWindSpeed.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Integer wind_speed = Integer.valueOf(new_value);
                        weatherDataViewModel.setWindSpeedData(wind_speed);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    weatherDataViewModel.setWindSpeedData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextWindDirection = view.findViewById(R.id.timed_count_edit_text_wind_direction);
        String wind_direction = weatherDataViewModel.getWindDirectionData().getValue();
        if (wind_direction != null) {
            editTextWindDirection.setText(wind_direction);
        }
        editTextWindDirection.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    weatherDataViewModel.setWindDirectionData(new_value);
                } else {
                    weatherDataViewModel.setWindDirectionData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextPressure = view.findViewById(R.id.timed_count_edit_text_pressure);
        Integer pressure = weatherDataViewModel.getPressureData().getValue();
        if (pressure != null) {
            editTextPressure.setText(String.valueOf(pressure));
        }
        editTextPressure.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Integer pressure = Integer.valueOf(new_value);
                        weatherDataViewModel.setPressureData(pressure);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    weatherDataViewModel.setPressureData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextHumidity = view.findViewById(R.id.timed_count_edit_text_humidity);
        Integer humidity = weatherDataViewModel.getHumidityData().getValue();
        if (humidity != null) {
            editTextHumidity.setText(String.valueOf(humidity));
        }
        editTextHumidity.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Integer humidity = Integer.valueOf(new_value);
                        weatherDataViewModel.setHumidityData(humidity);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    weatherDataViewModel.setHumidityData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextHabitat = view.findViewById(R.id.timed_count_edit_text_habitat);
        String habitat = weatherDataViewModel.getHabitatData().getValue();
        if (habitat != null) {
            editTextHabitat.setText(habitat);
        }
        editTextHabitat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    weatherDataViewModel.setHabitatData(new_value);
                } else {
                    weatherDataViewModel.setHabitatData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        EditText editTextComment = view.findViewById(R.id.timed_count_edit_text_comment);
        String comment = weatherDataViewModel.getCommentData().getValue();
        if (comment != null) {
            editTextComment.setText(comment);
        }
        editTextComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    weatherDataViewModel.setCommentData(new_value);
                } else {
                    weatherDataViewModel.setCommentData(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        // Return the inflated view
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherDataViewModel = new ViewModelProvider(requireActivity()).get(WeatherDataViewModel.class);
    }
}
