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
            "BagSearchAdresseerbareObjecten" to BagSearchAdresseerbareObjecten.URI,
            "BagSearchAdressen" to BagSearchAdressen.URI,
            "BagSearchBronhouders" to BagSearchBronhouders.URI,
            "BagSearchLigplaatsen" to BagSearchLigplaatsen.URI,
            "BagSearchNummeraanduidingen" to BagSearchNummeraanduidingen.URI,
            "BagSearchOpenbareRuimten" to BagSearchOpenbareRuimten.URI,
            "BagSearchPanden" to BagSearchPanden.URI,
            "BagSearchStandplaatsen" to BagSearchStandplaatsen.URI,
            "BagSearchVerblijfsobjecten" to BagSearchVerblijfsobjecten.URI,
            "BagSearchWoonplaatsen" to BagSearchWoonplaatsen.URI
        )
    }
}