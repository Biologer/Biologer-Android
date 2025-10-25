package org.biologer.biologer.gui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.biologer.biologer.viewmodels.TimedCountViewModel;
import org.biologer.biologer.databinding.FragmentTimedCountAdditionalDataBinding;

public class FragmentTimedCountAdditionalData extends Fragment {
    private static final String TAG = "Biologer.TimedCountFr";
    private FragmentTimedCountAdditionalDataBinding binding;
    TimedCountViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimedCountAdditionalDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.editTextTemperature.setText(String.valueOf(viewModel.getTemperatureData().getValue()));
        binding.editTextTemperature.addTextChangedListener(new TextWatcher() {
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

        Integer cloudiness = viewModel.getCloudinessData().getValue();
        if (cloudiness != null) {
            binding.editTextCloudiness.setText(String.valueOf(cloudiness));
        }
        binding.editTextCloudiness.addTextChangedListener(new TextWatcher() {
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

        Integer wind_speed = viewModel.getWindSpeedData();
        if (wind_speed != null) {
            binding.editTextWindSpeed.setText(String.valueOf(wind_speed));
        }
        binding.editTextWindSpeed.addTextChangedListener(new TextWatcher() {
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

        String wind_direction = viewModel.getWindDirectionData();
        if (wind_direction != null) {
            binding.editTextWindDirection.setText(wind_direction);
        }
        binding.editTextWindDirection.addTextChangedListener(new TextWatcher() {
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

        Integer pressure = viewModel.getPressureData();
        if (pressure != null) {
            binding.edittextPressure.setText(String.valueOf(pressure));
        }
        binding.edittextPressure.addTextChangedListener(new TextWatcher() {
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

        Integer humidity = viewModel.getHumidityData();
        if (humidity != null) {
            binding.editTextHumidity.setText(String.valueOf(humidity));
        }
        binding.editTextHumidity.addTextChangedListener(new TextWatcher() {
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

        String habitat = viewModel.getHabitatData();
        if (habitat != null) {
            binding.editTextHabitat.setText(habitat);
        }
        binding.editTextHabitat.addTextChangedListener(new TextWatcher() {
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

        String comment = viewModel.getCommentData();
        if (comment != null) {
            binding.editTextComment.setText(comment);
        }
        binding.editTextComment.addTextChangedListener(new TextWatcher() {
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
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TimedCountViewModel.class);
    }
}
