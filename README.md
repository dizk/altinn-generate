# Altinn Generate

Et proof of concept for "Zero-Mapping Altinn" - generer Kotlin dataklasser fra [Altinn Studio](https://docs.altinn.studio/) datamodeller for å unngå mapping-kode i Altinn-apper.

## Konsept: Zero-Mapping Altinn

Altinn Studio er tett knyttet til datamodellen og brukes som verktøy for å generere en **view model** - modellen som brukes til å modellere brukergrensesnitt. Selv om view modellen i seg selv er "uviktig" (den er ikke business domain modellen), er den avgjørende fordi Altinn-appen (frontend-teknologi) er tett knyttet til den.

Ved å generere Kotlin-modeller fra Altinn Studio-datamodellen unngår vi å skrive logikk og mapping i C# Altinn-appen.

**Flyt:**
```
Business Domain (Backend) → View Model (Altinn Studio) → Business Domain (Backend)
                ↓                                                ↑
          (map & prefill)                                  (map & process)
```

1. **Definer view model i Altinn Studio** - UI-modellen for brukergrensesnittet
2. **Generer Kotlin-modeller** - Backend får typesikre modeller av view modellen
3. **Backend sender prefill** - Map fra business domain til view model (i Kotlin)
4. **Altinn-app mottar data** - Direkte deserialisering uten mapping: `JsonSerializer.Deserialize<Datamodell>(response)`
5. **Backend mottar submit** - Map fra view model tilbake til business domain (i Kotlin)

**Resultat:** Altinn-appen (frontend) inneholder kun datamodell og GUI - all mapping og forretningslogikk er i backend.

## Hvordan Det Fungerer

1. Eksporter JSON Schema fra Altinn Studio
2. Plasser den i `src/main/resources/schema/`
3. Kjør `./gradlew build` - OpenAPI Generator genererer Kotlin-modeller
4. Bruk modellene i backend for prefill og mottak av data

## Eksempel: Backend-tjeneste med Prefill

**Backend (Kotlin):**
```kotlin
fun getPrefillData(): String {
    val user = fetchUserFromInternalDatabase()

    // Map business model til Altinn datamodell (view model)
    val altinnModel = Model(
        innloggetBruker = Aktor(
            fornavn = user.firstName,
            etternavn = user.lastName,
            adresse = Adresse(
                gateadresse = user.streetAddress,
                postnr = user.postalCode,
                poststed = user.city
            )
        )
    )

    return Json.encodeToString(altinnModel)
}
```

**Altinn-app [custom prefill](https://docs.altinn.studio/nb/altinn-studio/v8/guides/development/prefill/custom/) (C#):**
```csharp
public async Task DataCreation(Instance instance, object data, Dictionary<string, string> prefill)
{
    if (data is Datamodell skjema)
    {
        var response = await httpClient.GetStringAsync("backend-url");

        // Ingen mapping - direkte deserialisering
        skjema = JsonSerializer.Deserialize<Datamodell>(response);
    }
}
```

**Backend mottar data (Kotlin):**
```kotlin
fun processSubmission(jsonData: String) {
    // Samme datamodell returneres
    val altinnModel = Json.decodeFromString<Model>(jsonData)

    // Map fra Altinn datamodell (view model) til business model og prosesser videre
    mapToBusinessDomainAndProcess(altinnModel)
}
```

## Fordeler

- ✅ **Ingen mapping-kode i Altinn-app** - Kun `JsonSerializer.Deserialize<Datamodell>(response)`
- ✅ **Altinn Studio genererer view modellen** - Tett knyttet til brukergrensesnitt
- ✅ **Type-sikkerhet i backend** - Kompileringstidsfeil hvis view modellen endres
- ✅ **Altinn-app kun for UI** - All mapping og forretningslogikk i backend
- ✅ **Enkel testing** - Backend-mapping mellom business domain og view model kan testes uavhengig

## Kom i Gang

```bash
# Generer modeller og kompiler
./gradlew build
```

Genererte modeller: `build/generated/openapi/src/main/kotlin/com/github/dizk/models/`

## Ressurser

- [Altinn Studio](https://docs.altinn.studio/nb/altinn-studio/v8/)
- [Altinn Studio Datamodellering](https://docs.altinn.studio/nb/altinn-studio/v8/reference/data/data-modeling/)
- [Custom Prefill](https://docs.altinn.studio/nb/altinn-studio/v8/guides/development/prefill/custom/)
