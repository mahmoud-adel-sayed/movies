package com.example.movies.movie.list.data.source.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.movies.AppDatabase
import com.example.movies.api.MoviesApi
import com.example.movies.movie.list.data.mapper.toMovieEntities
import com.example.movies.movie.list.data.source.local.entity.MovieEntity
import com.example.movies.movie.list.data.source.local.entity.RemoteKeyEntity
import com.example.movies.util.Resource
import com.example.movies.util.safeApiCall
import retrofit2.HttpException
import java.io.IOException
import java.io.InvalidObjectException

@OptIn(ExperimentalPagingApi::class)
class MoviesPageKeyedRemoteMediator(
    private val initialPage: Int = 1,
    private val db: AppDatabase,
    private val moviesApi: MoviesApi
) : RemoteMediator<Int, MovieEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>
    ): MediatorResult {
        return try {
            // Calculate the current page to load depending on the state
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKey = getRemoteKeyByPosition(state)
                    remoteKey?.nextKey?.minus(1) ?: initialPage
                }
                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val remoteKey = getRemoteKeyForLastItem(state) ?: throw InvalidObjectException("empty")
                    remoteKey.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }
            when (val resource = safeApiCall { moviesApi.getPopularMovies(page) }) {
                is Resource.Success -> {
                    val movies = resource.data.results.toMovieEntities()
                    val endOfPaginationReached = movies.size < state.config.pageSize
                    db.withTransaction {
                        // If refreshing, clear table and start over
                        if (loadType == LoadType.REFRESH) {
                            db.remoteKeyDao().clearAll()
                            db.moviesDao().clearAll()
                        }
                        val prevKey = if (page == initialPage) null else page - 1
                        val nextKey = if (endOfPaginationReached) null else page + 1
                        val keys = movies.map {
                            RemoteKeyEntity(
                                id = it.serverId,
                                prevKey = prevKey,
                                nextKey = nextKey
                            )
                        }
                        db.remoteKeyDao().insertAll(keys)
                        db.moviesDao().insertAll(movies)
                    }
                    MediatorResult.Success(endOfPaginationReached)
                }
                is Resource.Failure -> MediatorResult.Error(Throwable(message = resource.message))
            }
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, MovieEntity>
    ): RemoteKeyEntity? {
        return state.lastItemOrNull()?.let { movie ->
            db.withTransaction {
                db.remoteKeyDao().remoteKeyById(movie.serverId)
            }
        }
    }

    private suspend fun getRemoteKeyByPosition(
        state: PagingState<Int, MovieEntity>
    ): RemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.serverId?.let { id ->
                db.withTransaction {
                    db.remoteKeyDao().remoteKeyById(id)
                }
            }
        }
    }
}