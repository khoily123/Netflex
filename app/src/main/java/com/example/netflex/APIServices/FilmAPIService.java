package com.example.netflex.APIServices;

import com.example.netflex.responseAPI.FilmResponse;
import com.example.netflex.viewModels.FilmDetailViewModel;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FilmAPIService {
    @GET("api/film")
    Call<FilmResponse> getFilms(
            @Query("page") int page,
            @Query("genreId") UUID genreId,
            @Query("countryId") UUID countryId,
            @Query("year") Integer year,
            @Query("keyword") String keyword
    );
    @GET("api/film/latest")
    Call<FilmResponse> getLatestFilms(@Query("page") int page);

    @GET("api/Film/{filmId}")
    Call<FilmDetailViewModel> getFilm(@Path("filmId") String filmId);
}

