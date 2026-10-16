<?php
// Общие функции PHP-бэкенда квеста «Протокол „Щит“» (для обычного хостинга с PHP).
// Состояние игры хранится в backend/data/state.json; каждый запрос берёт файл под блокировку,
// поэтому одновременные запросы с телефонов не портят друг другу данные.

define('SHIELD_ROOT', dirname(__DIR__));                 // папка сайта (public)
define('SHIELD_DATA', __DIR__ . '/data');
define('SHIELD_CONFIG', SHIELD_ROOT . '/js/config.js');
define('SHIELD_QUESTS', SHIELD_ROOT . '/zadaniya');
define('SHIELD_DEFAULT_KEY', 'ПОМЕНЯЙТЕ-МЕНЯ');

function shield_now() {
    return (int) round(microtime(true) * 1000);
}

// ---------- ключ ведущего ----------

function shield_host_key() {
    $k = @include __DIR__ . '/key.php';
    return is_string($k) ? trim($k) : '';
}

function shield_key_configured() {
    $k = shield_host_key();
    return $k !== '' && $k !== SHIELD_DEFAULT_KEY;
}

function shield_is_host() {
    if (!shield_key_configured()) return false;
    $c = isset($_COOKIE['host']) ? $_COOKIE['host'] : '';
    return is_string($c) && $c !== '' && hash_equals(shield_host_key(), $c);
}

// ---------- значения по умолчанию из config.js ----------

function shield_config_text() {
    static $text = null;
    if ($text === null) {
        $text = @file_get_contents(SHIELD_CONFIG);
        if ($text === false) $text = '';
    }
    return $text;
}

function shield_num($name, $default) {
    if (preg_match('/' . $name . '\s*:\s*(\d+)/u', shield_config_text(), $m)) return (int) $m[1];
    return $default;
}

function shield_str($name, $default) {
    if (preg_match('/' . $name . '\s*:\s*"([^"]*)"/u', shield_config_text(), $m)) return $m[1];
    return $default;
}

function shield_stage_count() {
    $n = preg_match_all('/vulnerability\s*:/u', shield_config_text());
    return $n ? $n : 11;
}

function shield_default_settings() {
    return array(
        'teamName' => shield_str('teamName', 'Отдел ИБ'),
        'totalMinutes' => shield_num('totalMinutes', 60),
        'insideSeconds' => shield_num('insideSeconds', 60),
        'cooldownSeconds' => shield_num('cooldownSeconds', 300),
        'warnSeconds' => shield_num('warnSeconds', 10),
        'runnersPerVisit' => shield_num('runnersPerVisit', 2),
        'autoPenaltySeconds' => shield_num('autoPenaltySeconds', 60),
        'wrongPenaltySeconds' => shield_num('wrongPenaltySeconds', 60),
        'maxWrongBeforeLock' => shield_num('maxWrongBeforeLock', 3),
        'lockSeconds' => shield_num('lockSeconds', 30),
        'hintPenaltySeconds' => shield_num('hintPenaltySeconds', 120),
    );
}

// config.js без ответов: только блок matrix (для телефонов и для всех без ключа).
function shield_public_config() {
    $inner = '';
    if (preg_match('/matrix\s*:\s*\{[^{}]*\}/u', shield_config_text(), $m)) $inner = $m[0];
    return "// Для устройств без ключа ведущего: только настройки таймера Матрицы, без ответов.\n" .
        "window.QUEST_CONFIG = { " . $inner . " };\n";
}

// ---------- коды QR-наклеек ----------

function shield_codes() {
    $codes = array();
    $file = SHIELD_QUESTS . '/kody.txt';
    if (!is_file($file)) return $codes;
    foreach (file($file, FILE_IGNORE_NEW_LINES) as $line) {
        $t = trim($line);
        if ($t === '' || $t[0] === '#') continue;
        $parts = preg_split('/\s+/', $t, 2);
        if (count($parts) === 2 && preg_match('/^[A-Za-z0-9]+$/', $parts[0])) {
            $codes[strtoupper($parts[0])] = trim($parts[1]);
        }
    }
    return $codes;
}

// ---------- состояние ----------

function shield_fresh_run() {
    return array('phase' => 'ready', 'since' => shield_now(), 'runners' => array(),
        'visits' => 0, 'violations' => 0, 'lastOverMs' => 0);
}

function shield_fresh_state() {
    $n = shield_stage_count();
    return array(
        'boot' => bin2hex(random_bytes(4)),
        'events' => array(),
        'nextEventId' => 1,
        'stateJson' => 'null',
        'stateAt' => 0,
        'players' => array(),
        'captain' => array('token' => '', 'name' => '', 'seen' => 0),
        'run' => shield_fresh_run(),
        'setup' => array('stages' => range(1, $n), 'settings' => shield_default_settings()),
    );
}

// Открыть состояние под эксклюзивной блокировкой, дать функции его изменить и сохранить.
function shield_with_state($fn) {
    if (!is_dir(SHIELD_DATA)) @mkdir(SHIELD_DATA, 0775, true);
    $fh = @fopen(SHIELD_DATA . '/state.json', 'c+');
    if (!$fh) {
        shield_text(500, 'Не удалось открыть backend/data/state.json — проверьте права на запись в папку backend/data.');
        exit;
    }
    flock($fh, LOCK_EX);
    $raw = stream_get_contents($fh);
    $st = $raw ? json_decode($raw, true) : null;
    if (!is_array($st)) $st = shield_fresh_state();
    $before = $raw;
    $result = $fn($st);
    $after = json_encode($st, JSON_UNESCAPED_UNICODE);
    if ($after !== $before) {
        ftruncate($fh, 0);
        rewind($fh);
        fwrite($fh, $after);
        fflush($fh);
    }
    flock($fh, LOCK_UN);
    fclose($fh);
    return $result;
}

// ---------- ответы ----------

function shield_headers($type) {
    header('Content-Type: ' . $type);
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
}

function shield_json($data) {
    shield_headers('application/json; charset=utf-8');
    echo is_string($data) ? $data : json_encode($data, JSON_UNESCAPED_UNICODE);
}

function shield_text($code, $text) {
    http_response_code($code);
    shield_headers('text/plain; charset=utf-8');
    echo $text;
}
