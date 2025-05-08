package com.ritense.iko.source

import com.ritense.iko.source.bag.search.BagSearchAdresseerbareObjecten
import com.ritense.iko.source.bag.search.BagSearchAdressen
import com.ritense.iko.source.bag.search.BagSearchBronhouders
import com.ritense.iko.source.bag.search.BagSearchLigplaatsen
import com.ritense.iko.source.bag.search.BagSearchNummeraanduidingen
import com.ritense.iko.source.bag.search.BagSearchOpenbareRuimten
import com.ritense.iko.source.bag.search.BagSearchPanden
import com.ritense.iko.source.bag.search.BagSearchStandplaatsen
import com.ritense.iko.source.bag.search.BagSearchVerblijfsobjecten
import com.ritense.iko.source.bag.search.BagSearchWoonplaatsen
import org.springframework.stereotype.Service

@Service
class SearchService {

    fun getSearches(): Map<String, String> {
        return mapOf(
            "BagSearchAdresseerbareObjecten" to BagSearchAdresseerbareObjecten.URI.drop(7),
            "BagSearchAdressen" to BagSearchAdressen.URI.drop(7),
            "BagSearchBronhouders" to BagSearchBronhouders.URI.drop(7),
            "BagSearchLigplaatsen" to BagSearchLigplaatsen.URI.drop(7),
            "BagSearchNummeraanduidingen" to BagSearchNummeraanduidingen.URI.drop(7),
            "BagSearchOpenbareRuimten" to BagSearchOpenbareRuimten.URI.drop(7),
            "BagSearchPanden" to BagSearchPanden.URI.drop(7),
            "BagSearchStandplaatsen" to BagSearchStandplaatsen.URI.drop(7),
            "BagSearchVerblijfsobjecten" to BagSearchVerblijfsobjecten.URI.drop(7),
            "BagSearchWoonplaatsen" to BagSearchWoonplaatsen.URI.drop(7)
        )
    }
}