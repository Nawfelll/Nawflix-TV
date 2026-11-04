package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape

/** Nawfel's weekly selection for the 1.0.8 release, in display order. */
internal val weeklyMovieSelection: List<MetaPreview> = listOf(
    weeklyMovie("tt1392214", "Prisoners", "2013", "Deux fillettes disparaissent. Un père et un enquêteur cherchent la vérité."),
    weeklyMovie("tt0084787", "The Thing", "1982", "En Antarctique, une créature peut prendre l'apparence de chacun."),
    weeklyMovie("tt0353969", "Memories of Murder", "2003", "Deux enquêteurs traquent un tueur dans une province sud-coréenne."),
    weeklyMovie("tt1139797", "Morse", "2008", "Un garçon solitaire se lie à une mystérieuse voisine."),
    weeklyMovie("tt4263482", "The Witch", "2015", "Une famille isolée affronte la peur et le doute en Nouvelle-Angleterre.")
)

private fun weeklyMovie(id: String, title: String, year: String, summary: String): MetaPreview = MetaPreview(
    id = id,
    type = ContentType.MOVIE,
    name = title,
    poster = "https://images.metahub.space/poster/small/$id/img",
    posterShape = PosterShape.POSTER,
    background = "https://images.metahub.space/background/medium/$id/img",
    logo = null,
    description = summary,
    releaseInfo = year,
    imdbRating = null,
    genres = emptyList(),
    imdbId = id,
    landscapePoster = "https://images.metahub.space/background/medium/$id/img",
    sourceAddonBaseUrl = "https://v3-cinemeta.strem.io"
)
