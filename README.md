# CalorieTracker

Трекер калорий и БЖУ (в духе YAZIO) на Kotlin + Jetpack Compose.

## Возможности
- **Дневник** по дням и приёмам пищи: кольцо калорий (съедено / осталось / цель) по приёмам пищи, БЖУ с нормами, трекер воды, фото к каждому приёму пищи (камера или галерея), подробности записи по тапу
- **Поиск продуктов** в трёх источниках: мои продукты → встроенная справочная база (189 продуктов с КБЖУ: молочка, мясо, крупы, соусы, готовые блюда…) → онлайн-поиск по названию в Open Food Facts (магазинные продукты с марками)
- **Карточка продукта**: КБЖУ на 100 г и на порцию, клетчатка, сахара, насыщенные жиры, соль, Nutri-Score, доля от дневной нормы, быстрые порции
- **Сканер штрихкодов** (CameraX + ML Kit + Open Food Facts); если штрихкода нет в базе — поиск по названию
- **ИИ-ввод** через Groq: блюдо раскладывается на ингредиенты с граммовками; ингредиенты из справочной базы пересчитываются по ней, граммы можно поправить
- **Рецепты**: 24 встроенных рецепта с КБЖУ на порцию, пошаговым приготовлением и пересчётом ингредиентов на нужное число порций + свои рецепты
- **Рацион** на 1/3/7 дней: с ИИ (с учётом пожеланий) или офлайн из рецептов приложения; блюда добавляются в дневник одним нажатием
- **Калькуляторы**: норма калорий (Миффлин — Сан Жеор), БЖУ под цель, ИМТ, норма воды, % жира (метод ВМС США), идеальный вес, калории тренировки
- **Статистика** за 7/30/90 дней: калории с линией цели и скользящим средним, сравнение с прошлым периодом, серия дней, вес, распределение по приёмам пищи, БЖУ, топ продуктов
- **Темы**: системная / светлая / тёмная, 6 акцентных цветов, Material You (Android 12+)
- Всё хранится локально (Room + DataStore); в сеть уходят только запросы к Open Food Facts и Groq

## Скриншоты
| Дневник | Продукт | Рецепт (тёмная) |
|---|---|---|
| ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_diaryLight.png) | ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_foodDetails.png) | ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_recipeDetailsDark.png) |

| Статистика | Рацион | Калькулятор |
|---|---|---|
| ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_statsLight.png) | ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_planScreen.png) | ![](app/src/test/snapshots/images/com.example.calorietracker.ui_ScreenshotTest_calculatorCalories.png) |

Скриншоты рендерятся без эмулятора через [Paparazzi](https://github.com/cashapp/paparazzi):
```
ANDROID_HOME=/путь/к/sdk ./gradlew recordPaparazziDebug   # перезаписать
ANDROID_HOME=/путь/к/sdk ./gradlew verifyPaparazziDebug   # сравнить с сохранёнными
ANDROID_HOME=/путь/к/sdk ./gradlew testDebugUnitTest       # все тесты: логика, база, миграция БД
```

Встроенная база и рецепты лежат в `app/src/main/assets/` (`foods_ru.json`, `recipes_ru.json`).
При изменении увеличь `SEED_VERSION` в `data/Seeder.kt` — приложение перезагрузит встроенные записи,
не трогая данные пользователя.

## Сборка

Через Android Studio (самый простой способ):
1. File → Open → выбрать эту папку
2. Дать Android Studio подтянуть Gradle-обёртку и зависимости (нужен интернет)
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`

Через терминал (нужен JDK 17+ и Android SDK, путь к SDK — в `local.properties` как `sdk.dir=...` или в `ANDROID_HOME`):
```
./gradlew assembleDebug
```

Через GitHub Actions (без установки чего-либо): при каждом пуше workflow `Build APK`
собирает release-APK (сжатый R8) и публикует его в **Releases** репозитория.
Последняя версия всегда по ссылке:
`https://github.com/spaiwer777-art/test/releases/latest/download/CalorieTracker.apk`

### Постоянный ключ подписи (чтобы обновления ставились поверх)
Без него каждая CI-сборка подписана новым одноразовым ключом, и перед установкой
новой версии старую придётся удалить (вместе с дневником). Один раз:
1. Создать ключ: `keytool -genkeypair -keystore release.jks -alias calorietracker -keyalg RSA -keysize 2048 -validity 10000`
2. В репозитории: Settings → Secrets and variables → Actions → New repository secret:
   - `SIGNING_KEYSTORE_BASE64` — вывод `base64 -w0 release.jks`
   - `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD` — пароли из шага 1
   - `SIGNING_KEY_ALIAS` — `calorietracker`
3. Сам `release.jks` хранить у себя (не в репозитории) — он же понадобится для Google Play.

## Установка на телефон
1. Скачать `app-debug.apk` на телефон
2. Открыть файл и разрешить «Установку из неизвестных источников» для браузера/файлового менеджера
3. В приложении: ⚙ Настройки → вставить свой Groq API-ключ → «Сохранить ключ»

API-ключ не хранится в коде и репозитории — только локально на телефоне.

## Структура проекта
```
app/src/main/java/com/example/calorietracker/
  data/        Room-сущности, DAO, миграции, репозитории, настройки, калькуляторы,
               сопоставление ингредиентов ИИ со справочной базой, генератор рациона
  network/     Open Food Facts API, Groq API
  viewmodel/   ViewModel-ы экранов
  ui/          Compose-экраны и навигация
  ui/components/  кольцо калорий, полоски БЖУ, выбор приёма пищи
  ui/theme/    темы, акцентные цвета, цвета графиков
  MainActivity.kt
```

## Известные ограничения
- Нормы БЖУ в дневнике — пропорция 20/30/50 от цели; точный расчёт по весу — в калькуляторе «БЖУ под цель»
- Справочная база — средние значения; для конкретной марки точнее Open Food Facts или упаковка
- ИИ-оценка калорий приблизительная — точность зависит от модели Groq
