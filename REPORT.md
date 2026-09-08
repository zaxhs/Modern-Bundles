# Modern Bundles 1.0.0 – implementation och verifiering

## Resultat

Projektet bygger en enda NeoForge-JAR för Minecraft 1.21.1, NeoForge
21.1.249 och Java 21. Samma JAR kan installeras bara på servern, bara på
klienten eller på båda sidor.

Alla färgade och ofärgade bundles är fortfarande `minecraft:bundle`. Modden
registrerar inga items, blocks, menus, recipe serializers eller andra
gameplay-registerposter. Därför skapas inga modspecifika registry-ID:n som en
vanilla/omodifierad klient måste känna till.

## Arkitektur

### Färg och färgning

De 16 shapeless-recepten använder bara vanillaingredienser och ger
`minecraft:bundle` med följande vanillakomponenter:

- `minecraft:custom_data` med `modernbundles.color`
- `minecraft:custom_model_data` 1–16
- `minecraft:custom_name` med färgnamn

`ShapelessRecipeMixin` kör efter vanillas `ShapelessRecipe.assemble` och
ersätter bara resultatet när inmatningen är exakt en bundle och rätt dye för
ett av Modern Bundles-recepten. Den kopierar ingående bundle med count 1 och
lägger på den nya färgen. Bundleinnehåll och övriga datakomponenter bevaras.
Ett namn som modden själv skapade uppdateras vid omfärgning; ett namn som
spelaren själv har satt bevaras.

Färgning är därmed serverauktoritativ. En klient utan modden behöver inte
förstå någon ny itemtyp eller komponenttyp. Den kan visa föremålet som en
vanlig bundle och kan ignorera `custom_model_data` visuellt.

### Val, scrollning och uttag

Den valda internstacken lagras transient i vanillas `BundleContents` genom en
mixin. Den serialiseras inte till item-NBT/datacomponents och är inte ett nytt
registerobjekt. Vanillas beteende används när den förbättrade vägen inte är
förhandlad.

När båda sidor har modden kan klienten:

- scrolla bland innehållet i en bundle
- se modern tooltip och markerad internstack
- ta ut vald internstack
- dra från en bundle till en målslot

Servern är alltid auktoritativ. Varje payload innehåller container-ID, slot
och en snapshot av förväntat bundleinnehåll. Servern kontrollerar bland annat
spelare, spectator-status, att menyn fortfarande är giltig, slotgränser,
bundletyp, exakt aktuellt innehåll och valt index. En gammal eller felriktad
request avslås och containern synkas om.

En aktiv selection är dessutom knuten till den aktuella `ServerPlayer`,
container-ID:t, slotten och den faktiska bundleinstansen. State rensas vid
felmatchning eller när den förbättrade vägen inte får användas. En spelares
val kan därför inte återanvändas för en annan spelare eller bundle.

### Nätverk och optional-client

Payloadregistreringen använder NeoForges valfria nätverksregistrering:

```java
event.registrar("1").optional()
```

Klienten skickar endast selection-/transfer-payload när den aktiva
anslutningen annonserar respektive kanal via `hasChannel`. Servern kontrollerar
också kanalstödet innan förbättrat selection-beteende används. Utan gemensam
kanal faller interaktionen tillbaka till vanilla och ingen okänd payload
skickas.

Klientklasser ligger i klientpaket. Klientmixin är listade separat i
`modernbundles.mixins.json`, och klientevent registreras bara på `Dist.CLIENT`.
Den dedikerade serverstarten bekräftade att distributionen inte försöker ladda
Minecrafts klientklasser.

## Återanvänt material och licenser

Back with the Bundle av Sythiex användes som huvudsaklig MIT-licensierad bas
för bundleinteraktion, selection, tooltip, scrollning, nätverk, tester och
visuella assets. Dess MIT-licens finns i `LICENSE`.

Bundle Backport-ish av Josiah Fu konsulterades som sekundär MIT-licensierad
referens för färgrecept och uttag av vald internstack. Dess licens finns i
`LICENSE_BundleBackportish`.

`NOTICE` beskriver härkomsten. De Minecraft-assets som följde med den primära
referensen omfattas inte av MIT-licenserna; Mojangs ägande och relevanta
användningsvillkor anges uttryckligen. Alla tre filer packas i JAR:en under
`META-INF`.

## Utförd verifiering

- `gradlew.bat clean test build --no-daemon`: lyckades.
- JUnit: 77 tester, 0 failures, 0 errors, 0 skipped.
- Testerna täcker bland annat färgmetadata, recept-/registrygrunden,
  selection, scrollackumulering, stale snapshots, payloadgränser,
  slotöverföring, capabilities och tooltip-layout.
- En första dedikerad serverstart hittade fel format på `custom_name` i de 16
  recepten. Recepten korrigerades, följt av full clean test/build och ny
  serverstart.
- Korrigerad dedikerad serverstart: Modern Bundles 1.0.0 laddades på NeoForge
  21.1.249, RecipeManager laddade 1306 recept (16 fler än felkörningens 1290),
  och servern nådde `Done (2.549s)`. Inga recipe parsing-, mixin-, ERROR- eller
  FATAL-rader fanns i den korrigerade loggen. Testservern avslutades därefter
  manuellt från Gradles PTY.
- En klientstart nådde huvudmenyn, laddade Modern Bundles, mod-resurser,
  texturatlaser, OpenAL och renderbackend och avslutades rent. Inga mixin-,
  exception-, ERROR- eller FATAL-rader fanns i klientens `latest.log`.
- Slut-JAR: `modernbundles-1.0.0.jar`, 218782 byte.
- SHA-256:
  `ED0DB874BDA7E8A023D5D30022EFA9C89D1249AE2B1C91C20A4B48455BCA17DD`

Detta verifierar kompilering, tester, paketering, dedikerad klassladdning,
receptparsing och en lokal klientstart. Det är inte samma sak som att ha
genomfört den kompletta nätverksmatrisen med riktiga spelare.

## Inte verifierat live

Följande måste fortfarande testas manuellt innan produktionssättning:

1. vanilla klient -> moddad server
2. moddad klient -> vanilla server
3. moddad klient -> moddad server
4. färgning av tom bundle
5. färgning/omfärgning av fylld bundle utan innehållsförlust
6. scrollning, vald extraction och drag-to-slot

Inte heller en befintlig produktionsvärld eller en full servermodpack har
startats. De två levererade referens-ZIP:arna har inte ändrats.

## Rekommenderad manuell testplan

Använd separata testinstanser och en kopia av eventuell värld.

### 1. Vanilla klient -> moddad server

- Lägg JAR:en endast i testserverns `mods`-mapp.
- Anslut med en ren NeoForge-/vanillakompatibel 1.21.1-klient utan Modern
  Bundles.
- Bekräfta att login lyckas utan channel- eller registry-fel.
- Skaffa en vanlig bundle, färga den, lägg i och ta ur föremål samt logga ut
  och in igen.
- Förväntat: bundlen fungerar som `minecraft:bundle`; färgen kan se vanilla
  ut utan klientens modeller, men data och innehåll ska vara intakta.

### 2. Moddad klient -> vanilla server

- Lägg JAR:en endast i klientens `mods`-mapp och anslut till en omoddad
  1.21.1-server.
- Hovra en bundle, scrolla över den och använd normala bundleinteraktioner.
- Förväntat: anslutningen lyckas, ingen Modern Bundles-payload skickas och
  serverns vanillabeteende fortsätter fungera.

### 3. Moddad klient -> moddad server

- Lägg samma JAR på båda sidor.
- Bekräfta att modern tooltip visas, att scrollhjulet flyttar markeringen och
  att uttag tar den markerade internstacken.
- Testa även drag-to-slot med giltig och full/ogiltig målslot.
- Förväntat: giltiga operationer synkas och felaktiga mål ändrar inget.

### 4. Färgning och persistens

- Färga en tom bundle i alla 16 färger och kontrollera modell/namn.
- Fyll en bundle med flera olika stacks, färga och omfärga den.
- Ge en bundle ett eget namn i städ och omfärga den.
- Starta om servern och kontrollera allt igen.
- Förväntat: exakt innehåll och eget namn bevaras; modgenererat färgnamn
  uppdateras; färgmetadata överlever omstart.

### 5. Stale- och flerpersonstest

- Låt två spelare arbeta i samma container. Ändra eller flytta en bundle
  mellan scroll och klick.
- Förväntat: en stale request avslås och resynkas; en spelares markerade val
  påverkar inte den andra spelarens bundle.

## Kända begränsningar

- Modern färgmodell kräver klientmodden eller ett kompatibelt resource pack.
  En server-only-installation ger fortfarande färgdata och säkra vanillaitems,
  men kan inte tvinga en omoddad klient att visa de inkluderade modellerna.
- Modden ersätter `assets/minecraft/models/item/bundle.json` på moddade
  klienter. Ett annat resource pack/mod som ersätter samma modell kan vinna
  resursordningen eller behöva en kompatibilitetslösning.
- Genererade färgnamn är engelska literals. Spelarsatta namn bevaras.
- Selection är medvetet transient. Efter innehållsändring, felmatchning eller
  anslutning utan gemensam kanal måste spelaren välja igen.
- Livebeteende med andra mods som mixar samma bundle-/containerkod är inte
  verifierat.
