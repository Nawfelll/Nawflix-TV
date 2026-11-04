package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape

/** The four films shown in the Rege Cine Club reference supplied by Nawfel. */
internal val regeCineClubSelection: List<MetaPreview> = listOf(
    regeMovie("tt36107185", "La Gradiva", "2026"),
    regeMovie("tt0066993", "The Devils", "1971"),
    regeMovie("tt0457430", "Le Labyrinthe de Pan", "2006"),
    regeMovie("tt4262980", "Shin Godzilla", "2016")
)

private fun regeMovie(id: String, title: String, year: String): MetaPreview = MetaPreview(
    id = id,
    type = ContentType.MOVIE,
    name = title,
    poster = "https://images.metahub.space/poster/small/$id/img",
    posterShape = PosterShape.POSTER,
    background = "https://images.metahub.space/background/medium/$id/img",
    logo = null,
    description = "Sélection du Rege Ciné Club par Regelegorila.",
    releaseInfo = year,
    imdbRating = null,
    genres = emptyList(),
    imdbId = id,
    landscapePoster = "https://images.metahub.space/background/medium/$id/img",
    sourceAddonBaseUrl = "https://v3-cinemeta.strem.io"
)
