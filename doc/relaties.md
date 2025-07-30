# Relaties

Een **relatie** binnen een profiel is een extra request naar een ander endpoint, waarmee aanvullende data wordt opgehaald. Deze extra data kan worden gebruikt om je profiel te verrijken. Relaties beïnvloeden de structuur van de uiteindelijke JSON-respons. Met de `transform`-optie kun je de gecombineerde data naar wens omvormen.

> **Let op**  
> In onderstaande voorbeelden gaan we uit van een eenvoudige response van een endpoint in de vorm:  
> `{ "X": "data" }`

## Zonder relatie (alleen A)

Wanneer een profiel alleen een request uitvoert naar endpoint **A** en er geen relaties zijn gedefinieerd, wordt de response van endpoint A rechtstreeks als JSON teruggegeven:

```json
{ "A": "data" }
```

## Relatie van A naar B

Wanneer er een relatie wordt toegevoegd van **A naar B**, worden beide responses opgenomen in een genest JSON-object. De data van endpoint A wordt geplaatst onder de `left`-property en de data van endpoint B onder `right`.

```json
{
  "left": { "A": "data" },
  "right": { "B": "data" }
}
```

Dit geeft je de mogelijkheid om met de `transform`-optie de twee datasets samen te voegen tot één gewenst formaat.

## Meerdere relaties: A → B en A → C

Wanneer er meerdere relaties worden toegevoegd — bijvoorbeeld van **A naar B** en van **A naar C** — dan worden de responses van **B** en **C** samengevoegd onder de `right`-property. De response van A blijft onder `left`.

```json
{
  "left": { "A": "data" },
  "right": {
    "B": "data",
    "C": "data"
  }
}
```

Je kunt daarna met de `transform`-optie bepalen hoe deze gecombineerde data wordt samengebracht in de uiteindelijke JSON-structuur.
