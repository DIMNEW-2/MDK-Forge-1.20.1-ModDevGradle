# Подземелья RPG Mod

Все восемь подземелий из плана (§5) реализованы одним типом структуры `rpgmod:dungeon`
(`com.closky.rpgmod.worldgen.DungeonStructure`) поверх ванильной jigsaw‑системы. Каждое подземелье — это набор
NBT‑шаблонов (`data/rpgmod/structures/<id>/*.nbt`), пулы шаблонов (`data/rpgmod/worldgen/template_pool/<id>/*.json`),
описание структуры (`worldgen/structure/<id>.json`), набор размещения (`worldgen/structure_set/<id>.json`) и тег биомов
(`tags/worldgen/biome/has_structure/<id>.json`).

## Список подземелий

| id | Уровень | Босс | Тип | Биомы | Таблицы лута |
|---|---|---|---|---|---|
| `catacombs_basic` | 5 | Король скелетов (`skeleton_king`, скелет) | подземное | весь Overworld | `chests/catacombs_basic`, `chests/catacombs_basic_boss`, `bosses/skeleton_king` |
| `bandit_camp` | 6 | Главарь бандитов (`bandit_chief`, виндикатор) | наземное | равнины, саванны, леса, луга | `chests/bandit_camp`, `..._boss`, `bosses/bandit_chief` |
| `deep_mines` | 7 | Надзиратель шахт (`mine_overseer`, хаск) | подземное | весь Overworld | `chests/deep_mines`, … |
| `abandoned_manor` | 8 | Призрак поместья (`manor_ghost`, иллюзионист) | наземное | тёмный лес, лес, болото, тайга | `chests/abandoned_manor`, … |
| `labyrinth_basic` | 9 | Минотавр лабиринта (`labyrinth_minotaur`, разоритель) | подземное | весь Overworld | `chests/labyrinth_basic`, … |
| `underground_fortress` | 10 | Владыка крепости (`fortress_lord`, визер‑скелет) | подземное | весь Overworld | `chests/underground_fortress`, … |
| `ruined_castle` | 12 | Павший король (`fallen_king`, зомби) | наземное | равнины, луга, холмы, пики | `chests/ruined_castle`, … |
| `arcane_tower` | 15 | Архимаг (`archmage`, заклинатель) | наземное | снежные биомы, тайга, горы | `chests/arcane_tower`, … |

Уровень босса = `max(уровень из таблицы, уровень зоны)`; у боссов +400 % здоровья и +50 % урона, босс‑бар в радиусе 64
блоков, лут по таблице `rpgmod:bosses/<key>` + опыт `CharacterLevel.xpForBoss(level)` всем игрокам в радиусе 48.

## Как устроен шаблонный набор

Для каждого подземелья генерируются семь частей (все ≤ 48 блоков по оси):

| Часть | Размер | Назначение |
|---|---|---|
| `start` | 11×7×11 | вход/центральная комната; jigsaw на север → пул `boss`, на восток/запад/юг → пул `corridors` |
| `corridor` | 5×5×9 | прямой коридор; задний jigsaw (юг) → `ends`, передний (север) → `rooms` |
| `room_a` | 9×6×9 | комната с двумя боковыми ответвлениями, 1–2 сундука, 1 спавнер |
| `room_b` | 13×7×9 | зал с колоннами, проход дальше, 2 сундука, 2 спавнера |
| `treasure` | 7×5×7 | тупик‑сокровищница: 3 сундука (один редкий), 1 спавнер |
| `end` | 5×5×1 | «заглушка» (fallback всех пулов) — запечатывает незавершённые проходы стеной |
| `boss_room` | 17×9×24 | зал босса с подводящим коридором, алтарь босса (`rpgmod:boss_spawner`) в центре, сундук босса (`rpgmod:loot_chest_boss`) на возвышении |

Все соединители — jigsaw‑блоки с `name = target = rpgmod:conn`, `joint = aligned`, `final_state = minecraft:air`
(у заглушки `final_state` = основной блок стен). Пулы:

* `<id>/start` → `start`
* `<id>/corridors` → `corridor` (fallback `ends`)
* `<id>/rooms` → `room_a` (вес 4), `room_b` (3), `treasure` (2), `corridor` (1) (fallback `ends`)
* `<id>/boss` → `boss_room` (fallback `ends`)
* `<id>/ends` → `end`

Параметры структуры: `size = 7`, `max_distance_from_center = 96`; подземные — `underground: true`,
`min_depth_below_surface: 14`, высота старта `uniform(-48..24)`, `terrain_adaptation: none`;
наземные — `project_start_to_heightmap: WORLD_SURFACE_WG`, `terrain_adaptation: beard_thin`.
Размещение: `random_spread` с индивидуальными `spacing/separation/salt` (см. `tools/gen_worldgen.py`).

Палитры блоков, мобы спавнеров, освещение и декор заданы в `DungeonRegistry.java` (Java) и продублированы в
`tools/gen_templates.py` (Python) — именно Python‑генератор создал NBT, лежащие в репозитории.

## Команды

Все команды требуют уровень прав 2 (кроме открытия экранов).

| Команда | Действие |
|---|---|
| `/rpgmod dungeon list` | перечислить подземелья (уровень, босс, тип) |
| `/rpgmod dungeon export <ns:name> <from> <to>` | сохранить область мира (≤ 48 блоков по оси) как шаблон в `<мир>/generated/<ns>/structures/<name>.nbt` |
| `/rpgmod dungeon import <ns:name> [pos] [rotation 0‑3]` | поставить шаблон (из `generated/` или из ресурсов мода) |
| `/rpgmod dungeon generate <id>` | сгенерировать подземелье целиком на месте игрока (делегирует `/place structure rpgmod:<id>`) |
| `/rpgmod dungeon generate_templates` | пересоздать все 56 частей Java‑генератором в `generated/rpgmod/structures` — они перекрывают встроенные шаблоны и позволяют итерировать дизайн без пересборки мода |
| `/rpgmod boss spawn <key>` | призвать босса рядом с игроком |

Поиск: `/locate structure rpgmod:<id>` или `/locate structure #rpgmod:dungeons`.

## Как сделать свой шаблон вручную

1. Постройте комнату в мире, поставьте jigsaw‑блоки в проёмах (`name/target = rpgmod:conn`, нужный пул, `final_state = minecraft:air`).
2. `/rpgmod dungeon export rpgmod:catacombs_basic/room_c ~-5 ~-1 ~-5 ~5 ~6 ~5`.
3. Скопируйте `.nbt` из `generated/rpgmod/structures/catacombs_basic/room_c.nbt` в
   `src/main/resources/data/rpgmod/structures/catacombs_basic/` и добавьте элемент в `template_pool/catacombs_basic/rooms.json`.
4. Сундуки должны быть блоками `rpgmod:loot_chest[_rare|_boss]` с NBT `LootTable` — они открываются через «казино» и
   получают уровень предметов по уровню зоны.

## Вход в подземные подземелья

У подземных подземелий (`underground: true`) к jigsaw‑частям добавляется ещё одна — `rpgmod:entrance_shaft`
(`EntranceShaftPiece`, регистрируется в `ModStructures.STRUCTURE_PIECES`). `DungeonStructure.findGenerationPoint`
после сборки jigsaw берёт стартовую комнату (первая часть), находит высоту поверхности над её центром
(`OCEAN_FLOOR_WG`) и, если выше комнаты есть хотя бы 3 блока породы, пристраивает шахту 5×5 с винтовой лестницей
вокруг центральной колонны (ступень каждые полблока, факел каждые 4 блока) от пола стартовой комнаты до поверхности.
Наверху стоит домик 7×7×3 из блоков палитры подземелья с дверным проёмом на юг — вход виден издалека и доступен без
копания (план, §4.2). Если над центром комнаты вода (озеро/океан), структура в этой точке не размещается.

## Ограничения / известные особенности

* Шахта входа строится одна на подземелье (над стартовой комнатой); остальные комнаты находят изнутри или через `/locate`.
* Генерация шаблонов детерминирована (seed = имя подземелья), поэтому все подземелья одного id имеют одинаковые комнаты;
  разнообразие достигается jigsaw‑сборкой и поворотами частей.
* `terrain_adaptation: beard_thin` сглаживает рельеф под наземными строениями, но на крутых склонах возможны
  «висящие» углы — это нормальное поведение ванильной jigsaw‑системы.
