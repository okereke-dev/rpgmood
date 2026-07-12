# RPGMood — Roadmap

> Visión: Convertir RPGMood en un plugin de atmósfera RPG completo con progresión de jugador, eventos mundiales, agricultura y cocina (inspirado en Harvest Moon NES/N64), e inmersión profunda.

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
- **Agricultura RPG (Harvest Moon-style)** — módulo `com.ricardo.rpgmood.farming` completo:
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

## 🏆 Prioridad Alta (Próximo release)

### 1. Scoreboard / BossBar de zona actual
- Mostrar zona actual, nivel de peligro (mob scaling), kills en sesión
- BossBar temporal al cambiar de zona (además del Title actual)
- Comando `/rpgmood toggle bossbar`

---

## 🟡 Prioridad Media

### 2. Eventos climáticos dinámicos
- Niebla densa (partículas + visibilidad reducida via potion effect)
- Tormentas que afectan comportamiento de mobs
- Lluvia ácida en el Nether (daño gradual)
- Viento que empuja al jugador (Knockback simulado)

### 3. Ciclo día/noche mejorado
- Mensajes ambientales en momentos clave (amanecer, mediodía, atardecer, medianoche)
- Mob scaling bonus nocturno (+2 niveles de noche)
- Sonidos ambientales distintos (grillos de noche, pájaros de día)

### 4. Comando `/zones`
- Lista de zonas descubiertas (con bioma, peligro, fecha de primer avistamiento)
- Marcado de zonas favoritas
- Distancia a la zona más cercana

---

## 🔵 Prioridad Baja

### 5. Nivel de jugador (RPG Progression)
- XP por matar mobs escalados, explorar zonas, sobrevivir
- Niveles que desbloquean perks pasivos
- Comando `/level` o integración en `/rpgmood`

### 6. Música ambiental por zona
- Reproducir notas musicales o sonidos largos al entrar a zonas especiales
- Compatible con resource packs que añadan música personalizada

### 7. Discord Webhook
- Anuncios de mobs de alto nivel → Discord
- Muertes narrativas → Discord #deaths
- Leaderboard semanal → Discord #leaderboard

### 8. MySQL / SQLite
- Migrar PlayerJournalService y PlayerStatsService a base de datos
- Necesario para redes multi-servidor (BungeeCord / Velocity)

### 9. Localización (i18n)
- Archivos `messages_es.yml`, `messages_en.yml`, etc.
- Sistema de fallback al inglés si falta una clave

### 10. GUI de configuración en juego
- Menú con clics para configurar zonas sin editar YAML
- Integración con `/rpgmood admin config`

### 11. Permisos por subcomando en `/rpgmood-farm`
- Hoy `rpgmood-farm` está gateado como un solo permiso (`rpgmood.player.farming`)
- Separar en nodos finos (ej. `rpgmood.player.farming.animal.buy`) si algún server lo necesita

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
