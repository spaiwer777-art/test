# CalorieTracker

Трекер калорий и БЖУ (в духе YAZIO) на Kotlin + Jetpack Compose.

## Возможности
- Дневник питания по приёмам пищи (завтрак/обед/ужин/перекус)
- Свой список продуктов + ручное добавление
- Сканер штрихкодов (CameraX + ML Kit) с поиском продукта через [Open Food Facts](https://world.openfoodfacts.org) — бесплатно, без ключа
- Быстрый ввод текстом с ИИ-оценкой калорий и БЖУ через [Groq](https://console.groq.com) (бесплатный OpenAI-совместимый API — нужен свой ключ, вставляется в Настройках)
- Цель по калориям в день
- Всё хранится локально на телефоне (Room + DataStore), кроме двух сетевых запросов выше

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
  data/        Room-сущности, DAO, база данных, репозитории, настройки
  network/     Open Food Facts API, Groq API
  viewmodel/   ViewModel-ы экранов
  ui/          Compose-экраны и навигация
  MainActivity.kt
```

## Известные ограничения MVP
- Нет графиков истории по неделям/месяцам
- Нет расчёта БЖУ-целей по формулам (только общая цель по калориям)
- ИИ-оценка калорий приблизительная — точность зависит от модели Groq
