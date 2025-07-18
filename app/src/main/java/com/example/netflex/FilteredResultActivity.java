package com.example.netflex;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netflex.APIServices.ApiClient;
import com.example.netflex.APIServices.CountryAPIService;
import com.example.netflex.APIServices.FilmAPIService;
import com.example.netflex.APIServices.GenreAPIService;
import com.example.netflex.adapter.FilmAdapter;
import com.example.netflex.model.Country;
import com.example.netflex.model.Film;
import com.example.netflex.model.Genre;
import com.example.netflex.responseAPI.FilmResponse;
import com.example.netflex.resonseAPI.GenreResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilteredResultActivity extends AppCompatActivity {

    private RecyclerView recyclerResults;
    private List<Genre> genres = new ArrayList<>();
    private List<Country> countries = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_filtered_result);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupBottomNavigation();

        recyclerResults = findViewById(R.id.recyclerFiltered);
        recyclerResults.setLayoutManager(new GridLayoutManager(this, 3));

        // Nhận dữ liệu từ Intent
        String genreIdStr = getIntent().getStringExtra("genreId");
        String countryIdStr = getIntent().getStringExtra("countryId");
        int year = getIntent().getIntExtra("year", -1);

        UUID genreId = genreIdStr != null ? UUID.fromString(genreIdStr) : null;
        UUID countryId = countryIdStr != null ? UUID.fromString(countryIdStr) : null;
        Integer selectedYear = (year != -1) ? year : null;

        // Gọi API lấy dữ liệu lọc
        FilmAPIService apiService = ApiClient.getRetrofit().create(FilmAPIService.class);
        apiService.getFilms(1, genreId, countryId, selectedYear).enqueue(new Callback<FilmResponse>() {
            @Override
            public void onResponse(Call<FilmResponse> call, Response<FilmResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Film> films = response.body().items;
                    recyclerResults.setAdapter(new FilmAdapter(films));
                }
            }

            @Override
            public void onFailure(Call<FilmResponse> call, Throwable t) {
                Log.e("FILTER_RESULT", "Failed to fetch results", t);
            }
        });

        // Code cho phần Lọc
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
    }

    // Fetch Filter Film
    private void fetchFilteredFilms(UUID genreId, UUID countryId, Integer year) {
        FilmAPIService apiService = ApiClient.getRetrofit().create(FilmAPIService.class);
        int page = 1;

        Integer yearParam = year != null ? year.intValue() : null;

        Call<FilmResponse> call = apiService.getFilms(page, genreId, countryId, yearParam);
        call.enqueue(new Callback<FilmResponse>() {
            @Override
            public void onResponse(Call<FilmResponse> call, Response<FilmResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Film> films = response.body().items;
                    Log.d("FILM_API", "Filtered films count: " + films.size());

                    // Gán dữ liệu vào RecyclerView
                    setupFilmRecyclerView(recyclerResults, films);
                } else {
                    Log.e("FILM_API", "Response code: " + response.code());

                    FilmResponse body = response.body();
                    if (body == null || body.items == null) {
                        Log.e("FILM_API", "Body or items null");
                    }

                }
            }

            @Override
            public void onFailure(Call<FilmResponse> call, Throwable t) {
                Log.e("FILM_API", "Filter API failed", t);
            }
        });
    }

    private void setupFilmRecyclerView(RecyclerView recyclerView, List<Film> films) {
        LinearLayoutManager layoutManager = new GridLayoutManager(this, 3);
        //recyclerResults.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new FilmAdapter(films));
    }

    private void showFilterDialog() {
        fetchGenresAndCountries(() -> {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter, null);

            ChipGroup chipGroupGenre = view.findViewById(R.id.chipGroupGenre);
            Spinner spinnerCountry = view.findViewById(R.id.spinnerCountry);
            Spinner spinnerYear = view.findViewById(R.id.spinnerYear);

            Button btnFilm = view.findViewById(R.id.btnFilm);
            Button btnSeries = view.findViewById(R.id.btnSeries);

            // Màu mặc định và màu khi chọn
            int selectedColor = Color.parseColor("#3399FF");
            int defaultColor = Color.parseColor("#444444");

            btnFilm.setOnClickListener(v -> {
                btnFilm.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                btnSeries.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
                // TODO: Đánh dấu đang chọn lọc phim
            });

            btnSeries.setOnClickListener(v -> {
                btnFilm.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
                btnSeries.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                // TODO: Đánh dấu đang chọn lọc series
            });

            // Tạo danh sách năm
            List<Integer> years = new ArrayList<>();
            years.add(-1); // -1 tượng trưng cho "All Years"
            for (int y = 2025; y >= 1990; y--) {
                years.add(y);
            }

            ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<Integer>(
                    this,
                    android.R.layout.simple_spinner_item,
                    years
            ) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view;
                    if (getItem(position) != null && getItem(position) == -1) {
                        text.setText("All Years");
                    } else {
                        text.setText(String.valueOf(getItem(position)));
                    }
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(18);
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView text = (TextView) view;
                    if (getItem(position) != null && getItem(position) == -1) {
                        text.setText("All Years");
                    } else {
                        text.setText(String.valueOf(getItem(position)));
                    }
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(20);
                    return view;
                }
            };
            spinnerYear.setAdapter(yearAdapter);
            spinnerYear.setSelection(0);

            // Country adapter
            Country allCountry = new Country();
            allCountry.id = null;
            allCountry.name = "All Countries";

            List<Country> countryListWithAll = new ArrayList<>();
            countryListWithAll.add(allCountry);
            countryListWithAll.addAll(countries);

            ArrayAdapter<Country> countryAdapter = new ArrayAdapter<Country>(
                    this,
                    android.R.layout.simple_spinner_item,
                    countryListWithAll
            ) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view;
                    Country item = getItem(position);
                    text.setText(item != null ? item.name : "All Countries");
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(18);
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView text = (TextView) view;
                    Country item = getItem(position);
                    text.setText(item != null ? item.name : "All Countries");
                    text.setTextColor(Color.WHITE);
                    text.setTextSize(20);
                    return view;
                }
            };
            spinnerCountry.setAdapter(countryAdapter);
            spinnerCountry.setSelection(0);

            // Tạo các Chip động cho Genre
            if (genres != null) {
                chipGroupGenre.removeAllViews();
                for (Genre genre : genres) {
                    Chip chip = new Chip(this);
                    chip.setText(genre.name);
                    chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#BBBBBB")));
                    chip.setChipStrokeWidth(3f);
                    chip.setTextColor(Color.WHITE);
                    chip.setTextSize(18);
                    chip.setHeight(150);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#444444")));
                    chip.setClickable(true);
                    chip.setCheckable(true);

                    chip.setShapeAppearanceModel(
                            chip.getShapeAppearanceModel().toBuilder()
                                    .setAllCornerSizes(50f)
                                    .build()
                    );

                    // Khi được chọn => đổi màu nền
                    chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                        if (isChecked) {
                            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3399FF"))); // xanh nước biển nhạt
                        } else {
                            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#444444"))); // màu gốc
                        }
                    });

                    chipGroupGenre.addView(chip);
                }
            }

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(view);
            bottomSheetDialog.show();

            // OPTIONAL: Chiếm 90% chiều cao màn hình nếu muốn
            View parent = (View) view.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setPeekHeight((int)(getResources().getDisplayMetrics().heightPixels * 0.8), true);

            // Gán nút Apply
            view.findViewById(R.id.btnApply).setOnClickListener(v -> {
                Integer selectedYear = (Integer) spinnerYear.getSelectedItem();
                Country selectedCountry = (Country) spinnerCountry.getSelectedItem();

                // Lấy Chip Genre được chọn
                int selectedChipId = chipGroupGenre.getCheckedChipId();
                Genre selectedGenre = null;
                if (selectedChipId != -1) {
                    Chip selectedChip = chipGroupGenre.findViewById(selectedChipId);
                    selectedGenre = genres.stream()
                            .filter(g -> g.name.equals(selectedChip.getText().toString()))
                            .findFirst().orElse(null);
                }

                fetchFilteredFilms(
                        selectedGenre != null ? selectedGenre.id : null,
                        selectedCountry != null ? selectedCountry.id : null,
                        selectedYear != -1 ? selectedYear : null
                );
                bottomSheetDialog.dismiss();
            });


            // Nút Reset
            view.findViewById(R.id.btnReset).setOnClickListener(v -> {
                chipGroupGenre.clearCheck();
                spinnerYear.setSelection(0);
                spinnerCountry.setSelection(0);
            });
        });
    }

    private void fetchGenresAndCountries(Runnable onComplete) {
        GenreAPIService genreAPI = ApiClient.getRetrofit().create(GenreAPIService.class);
        CountryAPIService countryAPI = ApiClient.getRetrofit().create(CountryAPIService.class);

        genreAPI.getGenres().enqueue(new Callback<GenreResponse>() {
            @Override
            public void onResponse(Call<GenreResponse> call, Response<GenreResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genres = response.body().genres;
                } else {
                    Log.e("API_ERROR", "Genres response failed");
                }

                // Tiếp tục gọi Country
                countryAPI.getCountries().enqueue(new Callback<List<Country>>() {
                    @Override
                    public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            countries = response.body();
                        } else {
                            Log.e("API_ERROR", "Countries response failed");
                        }
                        // Luôn gọi onComplete để hiển thị dialog
                        runOnUiThread(onComplete);
                    }

                    @Override
                    public void onFailure(Call<List<Country>> call, Throwable t) {
                        Log.e("API_ERROR", "Failed to fetch countries", t);
                        runOnUiThread(onComplete); // vẫn gọi dialog
                    }
                });
            }

            @Override
            public void onFailure(Call<GenreResponse> call, Throwable t) {
                Log.e("API_ERROR", "Failed to fetch genres", t);
                // Nếu genre lỗi thì vẫn gọi country để lấy tiếp
                countryAPI.getCountries().enqueue(new Callback<List<Country>>() {
                    @Override
                    public void onResponse(Call<List<Country>> call, Response<List<Country>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            countries = response.body();
                        }
                        runOnUiThread(onComplete);
                    }

                    @Override
                    public void onFailure(Call<List<Country>> call, Throwable t) {
                        Log.e("API_ERROR", "Failed to fetch countries", t);
                        runOnUiThread(onComplete);
                    }
                });
            }
        });
    }


    // Điều hướng thanh bottom
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                Intent intent = new Intent(FilteredResultActivity.this, HomeActivity.class);
                //bottomNavigationView.setSelectedItemId(R.id.menu_home);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.menu_explore) {
                return true;
            } else if (itemId == R.id.menu_new) {
                // TODO: Mở New & Hot
                return true;
            } else if (itemId == R.id.menu_History) {
                // TODO: Mở History
                return true;
            } else if (itemId == R.id.menu_profile) {
                Intent intent = new Intent(FilteredResultActivity.this, UserProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.menu_explore);

    }
}