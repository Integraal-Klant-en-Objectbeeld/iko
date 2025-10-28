# Aggregated Data Profiles (ADP)

## Properties

- TODO

## Relations

### Source endpoint mapping

A source endpoint mapping is a `jq` expression that maps the previous call to an endpoint to a new endpoint prameters. So for 
example if we have a call to `BRP` and we get a persons information back, and if we want to do a call to `BAG` we need to
map the `verblijfplaats.verblijfadres.postcode` and `verblijfplaats.verblijfadres.huisnummer` van `BRP` to the parameters 
`huisnummer` and `postcode` of `BAG`. We can do this by creation in `source endpoint mapping` `jq` expression below:

```
{ 
    postcode: .verblijfplaats.verblijfadres.postcode, 
    huisnummer: .verblijfplaats.verblijfadres.huisnummer 
}
```

#### Array of source endpoint mappings

You can also create an array in the `source endpoint mapping` with help of the `jq` expression, IKO will then run the relation
for each element in the array of the `source endpoint mapping`. The below example will cause the relation to be executed 3 times,
once for each element in the array and the relation will thus also return an array of results.

```
[
    { 
        postcode: .verblijfplaats.verblijfadres.postcode, 
        huisnummer: .verblijfplaats.verblijfadres.huisnummer 
    },
    { 
        postcode: .verblijfplaats.verblijfadres.postcode, 
        huisnummer: .verblijfplaats.verblijfadres.huisnummer 
    },
    { 
        postcode: .verblijfplaats.verblijfadres.postcode, 
        huisnummer: .verblijfplaats.verblijfadres.huisnummer 
    }
]
```
