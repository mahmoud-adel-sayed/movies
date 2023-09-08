package com.example.movies

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.movies.movie.list.data.source.local.dao.MoviesDao
import com.example.movies.movie.list.data.source.local.entity.MovieEntity

@Database(
    entities = [MovieEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moviesDao(): MoviesDao
}