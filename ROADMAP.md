# RPGMood — Roadmap

> Visión: Convertir RPGMood en un plugin de atmósfera RPG completo con progresión de jugador, eventos mundiales e inmersión profunda.

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

### 1. API pública para desarrolladores
- Eventos Bukkit: `PlayerZoneChangeEvent`, `MobScaleEvent`, `PlayerDeathMessageEvent`
- Permite que RPGLoot y otros plugins hookeen sin dependencia dura

### 2. BStats activo
- Registrar plugin en https://bstats.org
- Reemplazar `BSTATS_PLUGIN_ID = 0` con el ID real
- Telemetría para entender uso y priorizar features

### 3. Scoreboard / BossBar de zona actual
- Mostrar zona actual, nivel de peligro (mob scaling), kills en sesión
- BossBar temporal al cambiar de zona (además del Title actual)
- Comando `/rpgmood toggle bossbar`

### 4. Efectos visuales en mobs escalados
- Aura de partículas según nivel:
  - Nivel 1-10: ⚪ Partículas blancas (SPELL)
  - Nivel 11-20: 🟡 Partículas amarillas (VILLAGER_HAPPY)
  - Nivel 21-30: 🟠 Partículas naranjas (LAVA)
  - Nivel 31-40: 🔴 Partículas rojas (REDSTONE) + sonido ambiente
- Nombre del mob con color dinámico según rango de nivel

---

## 🟡 Prioridad Media

### 5. Eventos climáticos dinámicos
- Niebla densa (partículas + visibilidad reducida via potion effect)
- Tormentas que afectan comportamiento de mobs
- Lluvia ácida en el Nether (daño gradual)
- Viento que empuja al jugador (Knockback simulado)

### 6. Ciclo día/noche mejorado
- Mensajes ambientales en momentos clave (amanecer, mediodía, atardecer, medianoche)
- Mob scaling bonus nocturno (+2 niveles de noche)
- Sonidos ambientales distintos (grillos de noche, pájaros de día)

### 7. Sistema de logros / insignias
- Logros por exploración: "Legend of the Plains", "Void Walker"
- Logros por combate: "Slayer of the Sunken", "Dragon's Bane"
- Almacenados en stats.yml, visibles en /diary o /rpgmood achievements

### 8. Comando `/zones`
- Lista de zonas descubiertas (con bioma, peligro, fecha de primer avistamiento)
- Marcado de zonas favoritas
- Distancia a la zona más cercana

---

## 🔵 Prioridad Baja

### 9. Nivel de jugador (RPG Progression)
- XP por matar mobs escalados, explorar zonas, sobrevivir
- Niveles que desbloquean perks pasivos
- Comando `/level` o integración en `/rpgmood`

### 10. Música ambiental por zona
- Reproducir notas musicales o sonidos largos al entrar a zonas especiales
- Compatible con resource packs que añadan música personalizada

### 11. Discord Webhook
- Anuncios de mobs de alto nivel → Discord
- Muertes narrativas → Discord #deaths
- Leaderboard semanal → Discord #leaderboard

### 12. MySQL / SQLite
- Migrar PlayerJournalService y PlayerStatsService a base de datos
- Necesario para redes multi-servidor (BungeeCord / Velocity)

### 13. Localización (i18n)
- Archivos `messages_es.yml`, `messages_en.yml`, etc.
- Sistema de fallback al inglés si falta una clave

### 14. GUI de configuración en juego
- Menú con clics para configurar zonas sin editar YAML
- Integración con `/rpgmood admin config`

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

*Última actualización: 2026-07-11*
