# RPGMood — Roadmap

> Visión: Convertir RPGMood en un plugin de atmósfera RPG completo con progresión de jugador, eventos mundiales, agricultura y cocina (inspirado en Harvest Moon NES/N64), e inmersión profunda.

---

## ✅ v1.8.0 — Progresión, scoreboard/bossbar de zona, clima con efecto mecánico, /zones (Completado)

- Aggro sigue la curva de nivel — mobs débiles tempranos detectan un poco menos lejos que
  vanilla, mobs fuertes tardíos igual o más, misma curva `early_game_fraction`/`parity_level`
  que vida/daño
- Scoreboard lateral opcional (`/rpgmood toggle scoreboard`) + BossBar temporal por color de
  peligro al cambiar de zona, con kills en sesión
- `/rpgmood zones [fav <nombre>]` — zonas descubiertas (bioma, peligro, fecha), favoritas,
  distancia a la zona conocida más cercana
- Bonus de mob scaling nocturno/tormenta (`mob_scaling.night_bonus`/`thunder_bonus`, +2 c/u
  por defecto) y sonidos ambientales día/noche
- Clima con efecto mecánico real, no solo mensajes: pulsos de Darkness en tormenta, empujones
  de viento con lluvia a cielo abierto, lluvia ácida periódica en el Nether
  (`nether_events.acid_rain`)
- Música por zona (`zones.yml`'s clave `music`, reproducida una vez al entrar)
- Nivel de jugador (`/rpgmood level`, visible en el menú) — XP por matar mobs escalados y
  descubrir zonas, cada 5 niveles sube el cupo de animales propios
- Panel "Admin Config" en el menú GUI (`rpgmood.admin`) — steppers para radio de spawn, curva
  de mob scaling, bonus nocturno/tormenta, toggle de efectos de clima
- `/rpgmood-farm` con permisos finos por subcomando (`.season`/`.crops`/`.recipes`/`.animal`,
  todos default `true`)

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.7.0 — Auto-merge de configs y curva de mobs pareja (Completado)

- Config auto-merge: `config.yml`/`zones.yml`/`triggers.yml`/`farming.yml` ganan las claves
  nuevas de cada versión automáticamente al iniciar/recargar, sin pisar nada que ya hayas
  personalizado (usa el mecanismo nativo de Bukkit `setDefaults`/`copyDefaults`, no un sistema
  de migración custom)
- Curva de mobs reescrita: vida y daño ahora comparten una sola curva, relativa al stat
  vanilla real de cada especie (`mob_scaling.early_game_fraction` / `parity_level`) — antes
  cada stat llegaba a "equivalente a vanilla" en un nivel distinto (4, ~9, ~21), lo que hacía
  que los mobs tempranos pegaran muchísimo más débil de lo que su vida sugería

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.6.0 — Fix de leche, claim 100% vanilla, protección de spawn, color de zonas, afijos de mobs (Completado)

- Fix: ordeñar ya no duplica/pierde buckets (bug en `handleMilk`)
- Claim de animales salvajes migrado a `EntityEnterLoveModeEvent` — el evento vanilla real,
  no un chequeo manual de "¿ítem == comida favorita?"
- Protección radial de spawn: sin hostiles dentro de `spawn_protection.radius` (64 bloques
  por defecto)
- Color de título de zona por nivel de peligro local, con la paleta de rareza de RPGLoot
  (gris/amarillo/morado/verde/dorado) — antes las zonas dinámicas salían en blanco
- Sistema de afijos de mobs (Swift, Wraith, Bleeding, Poisonous, Regenerating, Chilling),
  probabilidad creciente con el nivel, comunicado vía partícula secundaria + aviso de action
  bar una sola vez
- Paquete Java renombrado `com.ricardo.*` → `com.okereke.*` (sin cambios funcionales)

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.5.0 — Menú GUI, limpieza de comandos, mensajes de conexión (Completado)

- Menú de inventario (`/rpgmood` o `/rpgmood menu`): journal, achievements por categoría,
  zona actual (abierto a todos, no solo admins), leaderboard, farming, mis animales,
  ajustes, y panel de RPGLoot si está instalado (equipo, set activo, stats de por vida)
- Tab-completion en los 3 comandos (`/rpgmood`, `/diary`, `/rpgmood-farm`) — ninguno tenía
- `/rpgmood-farm animal info` eliminado (comando vacío, solo redirigía a usar un stick)
- Mensajes de conexión custom (join/quit) por action bar/chat + sonido, reemplazan el chat
  vanilla, con plantilla distinta para la primera vez que un jugador entra
- El toggle de Action Bar/Chat (`player_actionbar.<uuid>`) por fin es alcanzable — vivía
  muerto en `MessageService` desde la auditoría de zonas/action bar

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.4.0 — Animales 100% vanilla, sin tienda (Completado)

- Se eliminó `/rpgmood-farm animal buy` y toda la economía de compra
  (`farming.animals.purchasing`)
- Alimentar a un animal salvaje con su comida de cría vanilla (trigo/semillas) lo reclama en
  el momento — mismo trigger que el modo amor vanilla, sin tocarlo ni cancelarlo
- Las crías heredan el dueño de sus padres cuando comparten dueño (o adoptan el dueño del
  padre que sí es propio, si el otro era salvaje)
- Sin name tags ni carteles flotantes — los animales reclamados quedan `persistent` para no
  despawnear, pero visualmente no cambian nada

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.3.1 — Auditoría de zonas/action bar (Completado)

- Entrada a zona ya no usa action bar — `subtitle` + `flavor_texts` se fusionan en un pool
  y se elige una línea (rotativa, no siempre la misma) para el Subtitle del Title. Arregla
  un bug real: el flavor nunca rotaba para zonas configuradas (siempre `flavor_texts[0]`)
- `MessageService` ahora reenvía los mensajes de action bar (ambiente, farming) para que no
  se pisen entre sí ni se desvanezcan antes de tiempo, con guard de "generación" para que un
  reenvío atrasado no le gane a un mensaje más nuevo
- Todo el feedback de farming/animales (`CropListener`, `AnimalInteractListener`,
  `AnimalProductTask`, `CookingListener`) pasa ahora por `MessageService` en vez de llamar
  `sendActionBar()` directo

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.3.0 — Más logros + integración RPGLoot (Completado)

- **16 logros nuevos** (15 → 31), agrupados en categorías (Exploración/Combate/Supervivencia/
  Farming/Loot) mostradas en `/rpgmood achievements`
- **Integración con RPGLoot** — 6 logros nuevos leen las tags PDC públicas de RPGLoot
  (rareza, artefactos, sets) sin dependencia Maven en ningún sentido, mismo patrón que ya
  usa RPGLoot para leer `rpgmood:level`. RPGLoot también se tocó (aditivo, sin romper nada):
  expone si un jugador tiene un set completo activo (`rpgloot:active_set_rarity`) y trackea
  artefactos distintos encontrados (`playerstats.yml`)
- Fix: `seasons_first`/`four_seasons` no se disparaban nunca (gap heredado de v1.2.0); y
  `/rpgmood achievements` ya no depende del orden no especificado de `Set.of()`

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.2.0 — Feature Release (Completado)

- **API pública para desarrolladores** — `PlayerZoneChangeEvent`, `MobScaleEvent`, `PlayerDeathMessageEvent`, `PlayerCropHarvestEvent`
- **BStats activo** — telemetría registrada con ID real
- **Aura de partículas en mobs escalados** — color/intensidad según nivel, solo visible cerca de jugadores
- **Sistema de logros** — 15 logros (exploración, combate, farming, supervivencia), persistidos en `achievements.yml`, `/rpgmood achievements`
- **Mensajes por action bar** — `MessageService`, con toggle por jugador
- **Agricultura RPG (Harvest Moon-style)** — módulo `com.okereke.rpgmood.farming` completo:
  - Calidad de cultivos (Bronce/Plata/Oro) por fertilidad, agua cercana, clima y peligro de zona
  - Ciclo de 4 estaciones (30 días MC cada una)
  - Cocina con recetas descubribles y buffs de "Mood"
  - **Animales** (bonus, no estaba en el alcance original): comprar, alimentar y cuidar vacas/gallinas/ovejas/cabras, `/rpgmood-farm animal buy|list|info`
  - Recetas descubiertas persistidas en `recipes.yml`

Ver `CHANGELOG.md` para el detalle completo.

---

## ✅ v1.1.1 — Bugfix Release (Completado)

Todos los bugs corregidos:
- Biome bonuses para biomas variantes (MEADOW → PLAINS, etc.)
- Structure scan lag reducido
- Zone fuzzy matching sanitizado
- Diary page overflow corregido
- E/S debounced en PlayerStats
- Memory leaks en mapas de cooldown
- Biome aliases alineados entre servicios
- Death messages contextuales (killer > cause > biome > armed > fallback)
- Toggle escribe solo el toggle, no todo el config

---

## 🔵 Prioridad Baja

### 1. Discord Webhook
- Anuncios de mobs de alto nivel → Discord
- Muertes narrativas → Discord #deaths
- Leaderboard semanal → Discord #leaderboard

### 2. MySQL / SQLite
- Migrar PlayerJournalService y PlayerStatsService a base de datos
- Necesario para redes multi-servidor (BungeeCord / Velocity)

### 3. Localización (i18n)
- Archivos `messages_es.yml`, `messages_en.yml`, etc.
- Sistema de fallback al inglés si falta una clave

---

## 🚀 Visión a Largo Plazo

| Feature | Descripción |
|---------|-------------|
| Clases RPG | Guerrero/Explorador/Superviviente con perks únicos |
| Misiones diarias/semanales | Sistema de quests generados proceduralmente |
| Reputación por zona | Matar mobs suma, morir resta; afecta títulos y mensajes |
| Bosses regionales | Mobs especiales que spawnan al matar suficientes en un área |
| Mapa/zona visualization | Partículas en bordes de zona o integración con Dynmap/BlueMap |
| Facciones | Integración con FactionsUUID para territorios y bonus |
| Integración RPGLoot bidireccional | Hoy RPGMood lee tags de RPGLoot; a futuro RPGLoot podría leer logros de RPGMood (ej. +luck/rareza por tener "Slayer Legend") — mismo patrón PDC, sin dependencia nueva |

---

*Última actualización: 2026-07-12*
