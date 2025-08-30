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
import org.biologer.biologer.adapters.TimedCountViewModel;

public class FragmentTimedCountAdditionalData extends Fragment {
    TimedCountViewModel viewModel;
    private static final String TAG = "Biologer.TimedCountFr";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timed_count_additional_data, container, false);

        EditText editTextTemperature = view.findViewById(R.id.timed_count_edit_text_temperature);
        editTextTemperature.setText(String.valueOf(viewModel.getTemperatureData().getValue()));
        editTextTemperature.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    try {
                        Double temperature = Double.valueOf(new_value);
                        viewModel.setTemperatureData(temperature);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    viewModel.setTemperatureData(null);
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
        Integer cloudiness = viewModel.getCloudinessData().getValue();
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
                        viewModel.setCloudinessData(cloudiness);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    viewModel.setCloudinessData(null);
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
        Integer wind_speed = viewModel.getWindSpeedData();
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
                        viewModel.setWindSpeedData(wind_speed);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    viewModel.setWindSpeedData(null);
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
        String wind_direction = viewModel.getWindDirectionData();
        if (wind_direction != null) {
            editTextWindDirection.setText(wind_direction);
        }
        editTextWindDirection.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    viewModel.setWindDirectionData(new_value);
                } else {
                    viewModel.setWindDirectionData(null);
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
        Integer pressure = viewModel.getPressureData();
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
                        viewModel.setPressureData(pressure);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    viewModel.setPressureData(null);
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
        Integer humidity = viewModel.getHumidityData();
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
                        viewModel.setHumidityData(humidity);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid number format: " + new_value);
                    }
                } else {
                    viewModel.setHumidityData(null);
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
        String habitat = viewModel.getHabitatData();
        if (habitat != null) {
            editTextHabitat.setText(habitat);
        }
        editTextHabitat.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    viewModel.setHabitatData(new_value);
                } else {
                    viewModel.setHabitatData(null);
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
        String comment = viewModel.getCommentData();
        if (comment != null) {
            editTextComment.setText(comment);
        }
        editTextComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_value = s.toString();
                if (!new_value.isEmpty()) {
                    viewModel.setCommentData(new_value);
                } else {
                    viewModel.setCommentData(null);
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
        viewModel = new ViewModelProvider(requireActivity()).get(TimedCountViewModel.class);
    }
}
