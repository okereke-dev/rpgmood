# RPGMood — Roadmap

> Visión: Convertir RPGMood en un plugin de atmósfera RPG completo con progresión de jugador, eventos mundiales, agricultura y cocina (inspirado en Harvest Moon NES/N64), e inmersión profunda.

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

---

*Última actualización: 2026-07-12*
