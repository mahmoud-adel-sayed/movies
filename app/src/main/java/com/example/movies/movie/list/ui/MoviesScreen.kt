package com.example.movies.movie.list.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.movies.R
import com.example.movies.common.ui.TopAppBar
import com.example.movies.movie.list.domain.model.Movie
import com.example.movies.theme.AppTheme
import com.example.movies.util.NetworkImage

@Composable
fun MoviesScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onMovieClick: (movieId: Long) -> Unit,
    moviesViewModel: MoviesViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(id = R.string.popular_movies),
                onUpPressed = onBackPressed
            )
        }
    ) { innerPaddingModifier ->
        MoviesContent(
            modifier = modifier.padding(innerPaddingModifier),
            movies = moviesViewModel.getPopularMovies().collectAsLazyPagingItems(),
            onMovieClick = onMovieClick
        )
    }
}

@Composable
private fun MoviesContent(
    modifier: Modifier = Modifier,
    movies: LazyPagingItems<Movie>,
    onMovieClick: (movieId: Long) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier.padding(16.dp),
        columns = GridCells.Fixed(2)
    ) {
        itemsIndexed(
            items = movies.itemSnapshotList
        ) { index: Int, movie: Movie? ->
            movie?.let {
                MovieCard(
                    index = index,
                    movie = it,
                    onMovieClick = onMovieClick
                )
            }
        }
        when (val state = movies.loadState.refresh) {
            is LoadState.NotLoading -> Unit
            is LoadState.Error -> error(message = state.error.message ?: "")
            is LoadState.Loading -> loading(modifier = Modifier.fillMaxSize())
        }
        when (movies.loadState.append) {
            is LoadState.NotLoading -> Unit
            is LoadState.Error -> Unit
            is LoadState.Loading -> loading(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun LazyGridScope.error(
    message: String
) {
    item {
        Text(
            text = message,
            style = AppTheme.typography.h6,
            color = AppTheme.colors.error
        )
    }
}

private fun LazyGridScope.loading(
    modifier: Modifier = Modifier
) {
    item {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MovieCard(
    index: Int,
    movie: Movie,
    onMovieClick: (movieId: Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 270.dp)
            .padding(
                end = if (index % 2 == 0) 16.dp else 0.dp,
                bottom = 16.dp
            )
            .clickable(onClick = {
                onMovieClick(movie.id)
            })
            .testTag("${MOVIE_ITEM_ID}_${index}_card"),
        contentColor = AppTheme.colors.onSurface
    ) {
        Column {
            NetworkImage(
                modifier = Modifier.fillMaxWidth(),
                url = IMAGE_BASE_URL + movie.posterUrl,
                contentScale = ContentScale.FillBounds,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = movie.title,
                style = AppTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = movie.releaseDate,
                style = AppTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w342"
private const val MOVIE_ITEM_ID = "movie_item"