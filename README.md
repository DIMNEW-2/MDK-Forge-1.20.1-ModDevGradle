# RPG Mod (Forge 1.20.1, ModDevGradle)

Кооперативный RPG‑оверхол для Minecraft 1.20.1 / Forge 47.x, собранный на этом форке MDK (ModDevGradle Legacy).
Реализация следует техническому плану из [`plan.md`](plan.md).

* **Mod id:** `rpgmod`, пакет `com.closky.rpgmod`, Java 17.
* **Зависимости:** только Forge + ванильный Minecraft (никаких сторонних модов, Tectonic‑подобная генерация сделана
  собственными JSON‑переопределениями).
* **Локализация:** `en_us` + `ru_ru` (545 ключей, всё из кода вынесено в lang‑файлы).

## Что внутри

| Система | Реализация |
|---|---|
| **Оружие и лут** | Уровень предмета, `WeaponClass`, `WeaponMaterial`, случайные бонусы в NBT, динамические тултипы, редкость. Оружие не крафтится (ванильные рецепты лука/арбалета/мечей отключены условием `rpgmod:config`, пока `lootOnlyFromChests=true`), добывается из сундуков и боссов. Ковка в кузнечном столе: шаблон + оружие + материал → бонус материала. |
| **7 тиров × 5 классов оружия** | Мечи (рывок), булавы (оглушение, AoE‑удар), копья (дальность 4.5, бросок с подбором), луки (тройной выстрел, кадры натяжения), арбалеты (болты, веер из трёх), бомбы 6 типов (без разрушения блоков по умолчанию), стрелы 7 эффектов и болты 6 эффектов — собственные сущности снарядов. |
| **Генерация мира** | Overworld высотой −64…640 (`dimension_type`), `noise_settings` с усиленными горами, увеличенными континентами и климатическими зонами, переопределённые density‑функции `depth/continents/erosion`, ванильные данжи/шахты/храмы/особняки отключены пустыми `structure_set`, monster rooms убраны biome‑модификатором. |
| **8 подземелий** | Jigsaw‑структуры `rpgmod:dungeon` с процедурно сгенерированными NBT‑шаблонами (56 частей), боссами, алтарями, таблицами лута. Подробнее — [`docs/dungeons.md`](docs/dungeons.md). Команды `/rpgmod dungeon export|import|list|generate|generate_templates`. |
| **Боссы** | 8 боссов (ванильные мобы с усилением, босс‑бар, титул, лут `bosses/<key>`, опыт группе). |
| **Сундуки‑казино** | `LootChestBlock` трёх тиров; сервер один раз бросает лут, клиент крутит барабаны (`CasinoScreen`), «Забрать» — пакет на сервер, сундук исчезает. Джекпот, уровень зоны, защита от повторного открытия. |
| **Магия** | 4 школы, 18 заклинаний (data‑driven переопределения в `data/rpgmod/spells`), посохи, свитки, мана, зелья маны, HUD. |
| **Прокачка** | Уровень персонажа до 100, 6 характеристик, классы по полному комплекту брони (8 классов), древо навыков из 56 узлов (`data/rpgmod/skill_tree`), сброс за блок изумруда. Экраны: K — навыки, O — персонаж, R — карта отношений, V — каст, G — выбор заклинания. |
| **Уровни мобов** | Capability на мобах, уровень по расстоянию/измерению/глубине, масштабирование здоровья и урона, таблички `[Ур. N]`. |
| **Деревни и политика** | `SettlementManager` (SavedData): классы деревень, экономика, репутация игрока, отношения, союзы, войны с физическими набегами, дипломатические события, карта отношений (`RelationsMapScreen`). |
| **Сеть / данные** | 13 пакетов через `SimpleChannel`, capabilities `PlayerRpgData`/`MobRpgData` с переносом при смерти, `ForgeConfigSpec` (common + client). |

## Сборка

В песочнице, где писался код, нет JDK и доступа к Maven Forge, поэтому проект **не компилировался локально** —
первая сборка должна пройти на вашей машине или в GitHub Actions (`.github/workflows/build.yml`):

```bash
./gradlew build          # сборка jar → build/libs/rpgmod-0.1.0.jar
./gradlew test           # JUnit‑тесты логики отношений/войн/древа навыков
./gradlew runClient      # запуск клиента
```

Если компилятор найдёт расхождения с API Forge 47.1.3 (исходники сверялись с веткой 1.20.1 MinecraftForge и ванильными
сигнатурами по памяти), они будут точечными — все файлы прошли синтаксическую проверку и проверку перекрёстных ссылок
(`tools/check_java.py`).

## Инструменты генерации (Python 3, Pillow)

Все ресурсы, кроме Java, порождаются скриптами из `tools/` и закоммичены в репозиторий:

| Скрипт | Что делает |
|---|---|
| `tools/gen_textures.py [--sheet]` | 193 PNG: предметы, блоки, броня (layer_1/2), GUI, HUD, снаряды, частицы, логотип. Контрольный лист в `build/texture_sheet.png`. |
| `tools/gen_resources.py` | Модели/blockstates, `sounds.json`, частицы, `lang/en_us.json` + `ru_ru.json`, теги, таблицы лута, рецепты, древо навыков, переопределения заклинаний. |
| `tools/gen_worldgen.py` | `dimension_type`, `noise_settings`, density‑функции, шумы, структуры/пулы/structure_set, теги биомов, отключение ванильных структур. |
| `tools/gen_templates.py` | 56 NBT‑шаблонов подземелий (через `tools/nbtlite.py`), тот же алгоритм, что и Java‑команда `generate_templates`. |
| `tools/validate_data.py` | Перекрёстная проверка данных: все ссылки на предметы/блоки/модели/текстуры/шаблоны/пулы/ключи локализации. |
| `tools/check_java.py` | Синтаксис (tree‑sitter) + резолв статических и инстанс‑обращений к классам мода. |

```bash
pip install pillow tree-sitter tree-sitter-java
python3 tools/gen_textures.py && python3 tools/gen_resources.py && python3 tools/gen_worldgen.py && python3 tools/gen_templates.py
python3 tools/validate_data.py && python3 tools/check_java.py
```

## Конфигурация

`config/rpgmod-common.toml` — опыт и уровни, уровни мобов (`blocksPerMobLevel`, `maxMobLevel`, `damageReductionCap`),
`lootOnlyFromChests`, казино (`casinoMinClaimDelayTicks`), магия, поселения (`enableSettlementSimulation`,
`enableVillageWars`, `villageWarFrequency`, `relationDecayRate`), переключатели спецатак оружия, `bombsDestroyBlocks=false`.
`config/rpgmod-client.toml` — таблички уровней мобов, HUD, анимация казино.

## Документация

* [`docs/dungeons.md`](docs/dungeons.md) — устройство подземелий, пулы, команды, как добавить свой шаблон.
* [`docs/textures_todo.md`](docs/textures_todo.md) — перечень всех текстур и что стоит дорисовать вручную.
* [`plan.md`](plan.md) — исходное техническое задание.

## Лицензия и маппинги

Код мода — по лицензии из `gradle.properties` (`mod_license`). Проект использует официальные маппинги Mojang
(см. `TEMPLATE_LICENSE.txt` и https://github.com/NeoForged/NeoForm/blob/main/Mojang.md) и
[ModDevGradle Legacy](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md).
