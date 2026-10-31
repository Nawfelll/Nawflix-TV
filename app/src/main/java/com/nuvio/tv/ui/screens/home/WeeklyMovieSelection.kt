package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape

/** Nawfel's Halloween selection for the 1.0.5 release, in display order. */
internal val weeklyMovieSelection: List<MetaPreview> = listOf(
    weeklyMovie("tt37287335", "Obsession", "2026", "Un souhait amoureux tourne à l'obsession terrifiante."),
    weeklyMovie("tt0077651", "Halloween : La Nuit des masques", "1978", "Michael Myers revient à Haddonfield la nuit d'Halloween."),
    weeklyMovie("tt0081505", "Shining", "1980", "Un hôtel isolé, un hiver sans fin et une famille qui bascule."),
    weeklyMovie("tt0084787", "The Thing", "1982", "En Antarctique, une créature peut prendre l'apparence de chacun."),
    weeklyMovie("tt0070047", "L'Exorciste", "1973", "Une famille affronte une présence inexplicable et terrifiante."),
    weeklyMovie("tt0072271", "Massacre à la tronçonneuse", "1974", "Un voyage au Texas vire au cauchemar."),
    weeklyMovie("tt0117571", "Scream", "1996", "Un tueur masqué joue avec les règles du cinéma d'horreur."),
    weeklyMovie("tt0087800", "Les Griffes de la nuit", "1984", "Freddy Krueger transforme les rêves en pièges mortels."),
    weeklyMovie("tt0076786", "Suspiria", "1977", "Une école de danse cache un secret aux couleurs hypnotiques."),
    weeklyMovie("tt0078748", "Alien, le huitième passager", "1979", "Dans l'espace, l'équipage du Nostromo n'est plus seul.")
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
    genres = listOf("Horreur"),
    imdbId = id,
    landscapePoster = "https://images.metahub.space/background/medium/$id/img",
    sourceAddonBaseUrl = "https://v3-cinemeta.strem.io"
)
