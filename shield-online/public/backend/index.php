<?php
// API квеста для PHP-хостинга. Запросы /api/<маршрут> сюда направляет .htaccess.
// Повторяет API server.js (Node) один в один, поэтому страницы сайта не знают, какой бэкенд работает.

require __DIR__ . '/common.php';

$route = isset($_GET['route']) ? trim((string) $_GET['route'], '/') : '';
$method = isset($_SERVER['REQUEST_METHOD']) ? strtoupper($_SERVER['REQUEST_METHOD']) : 'GET';
$rawBody = $method === 'POST' ? (string) file_get_contents('php://input') : '';
$body = $rawBody !== '' ? json_decode($rawBody, true) : null;
$isHost = shield_is_host();

function clean_name($value) {
    $name = preg_replace('/[\x00-\x1F<>]/u', '', trim((string) $value));
    if (function_exists('mb_substr')) {
        if (mb_strlen($name, 'UTF-8') > 24) $name = mb_substr($name, 0, 24, 'UTF-8');
    } elseif (preg_match('/^.{0,24}/su', $name, $m)) {
        $name = $m[0];
    }
    return $name;
}

function matrix_cfg($st) {
    $s = $st['setup']['settings'];
    return array(
        'inside' => (int) (!empty($s['insideSeconds']) ? $s['insideSeconds'] : 60) * 1000,
        'cool' => (int) (!empty($s['cooldownSeconds']) ? $s['cooldownSeconds'] : 300) * 1000,
        'warn' => (int) (!empty($s['warnSeconds']) ? $s['warnSeconds'] : 10) * 1000,
        'runners' => (int) (!empty($s['runnersPerVisit']) ? $s['runnersPerVisit'] : 2),
    );
}

function update_run(&$st) {
    $cfg = matrix_cfg($st);
    $now = shield_now();
    $run = &$st['run'];
    if ($run['phase'] === 'inside' && $now - $run['since'] >= $cfg['inside']) {
        $run['phase'] = 'overtime';
        $run['since'] = $run['since'] + $cfg['inside'];
    }
    if ($run['phase'] === 'cooldown' && $now - $run['since'] >= $cfg['cool']) {
        $run['phase'] = 'ready';
        $run['since'] = $now;
        $run['runners'] = array();
    }
}

function run_object(&$st, $playerId) {
    update_run($st);
    $cfg = matrix_cfg($st);
    $run = $st['run'];
    $now = shield_now();
    $elapsed = $now - $run['since'];
    $remaining = 0;
    $over = 0;
    if ($run['phase'] === 'inside') $remaining = $cfg['inside'] - $elapsed;
    elseif ($run['phase'] === 'cooldown') $remaining = $cfg['cool'] - $elapsed;
    elseif ($run['phase'] === 'overtime') $over = $elapsed;
    $list = array();
    foreach ($st['players'] as $id => $p) {
        $list[] = array('id' => (string) $id, 'name' => $p['name'], 'runs' => $p['runs'],
            'online' => ($now - $p['seen']) < 15000);
    }
    $runners = array();
    foreach ($run['runners'] as $id) {
        if (isset($st['players'][$id])) $runners[] = array('id' => $id, 'name' => $st['players'][$id]['name']);
    }
    $cap = $st['captain'];
    return array(
        'boot' => $st['boot'],
        'phase' => $run['phase'],
        'remainingMs' => $remaining,
        'overMs' => $over,
        'insideMs' => $cfg['inside'],
        'cooldownMs' => $cfg['cool'],
        'warnMs' => $cfg['warn'],
        'maxRunners' => $cfg['runners'],
        'runners' => $runners,
        'visits' => $run['visits'],
        'violations' => $run['violations'],
        'lastOverMs' => $run['lastOverMs'],
        'players' => $list,
        'captain' => $cap['token'] !== '',
        'captainName' => $cap['name'],
        'captainOnline' => ($now - $cap['seen']) < 15000,
        'isRunner' => in_array($playerId, $run['runners'], true),
    );
}

function is_captain($st, $body) {
    return is_array($body) && $st['captain']['token'] !== '' && isset($body['token'])
        && hash_equals($st['captain']['token'], (string) $body['token']);
}

function add_event(&$st, $obj) {
    $obj['id'] = $st['nextEventId']++;
    $st['events'][] = $obj;
    if (count($st['events']) > 200) array_shift($st['events']);
}

function need_post($method) {
    if ($method !== 'POST') { shield_text(405, 'POST only'); exit; }
}

function need_host($isHost) {
    if (!$isHost) { shield_text(403, 'Только ведущий (нужен ключ ведущего)'); exit; }
}

// ---------- главный экран (терминал защиты) ----------
// Полное состояние игры хранит сервер, поэтому терминал можно открыть на любом компьютере.
// Главный экран один: другой компьютер ведущего может забрать его себе (terminal/claim),
// тогда прежний получает отказ на сохранение и показывает заставку «экран перенесён».

define('TERMINAL_ONLINE_MS', 15000);

function valid_terminal_id($v) {
    return is_string($v) && preg_match('/^[a-z0-9]{8,32}$/', $v);
}

function &terminal_of(&$st) {
    if (!isset($st['terminal']) || !is_array($st['terminal'])) {
        $st['terminal'] = array('owner' => '', 'addr' => '', 'seen' => 0, 'game' => 'null',
            'clientNow' => 0, 'savedAt' => 0);
    }
    return $st['terminal'];
}

function terminal_info(&$st) {
    $t = &terminal_of($st);
    $now = shield_now();
    $online = $t['owner'] !== '' && $t['seen'] > 0 && $now - $t['seen'] < TERMINAL_ONLINE_MS;
    return '{"owner":' . json_encode($t['owner']) .
        ',"addr":' . json_encode($t['addr']) .
        ',"ownerOnline":' . ($online ? 'true' : 'false') .
        ',"ownerAgoMs":' . ($t['seen'] ? $now - $t['seen'] : -1) .
        ',"ageMs":' . ($t['savedAt'] ? $now - $t['savedAt'] : -1) .
        ',"clientNow":' . (int) $t['clientNow'] .
        ',"game":' . $t['game'] . '}';
}

function client_addr() {
    return isset($_SERVER['REMOTE_ADDR']) ? (string) $_SERVER['REMOTE_ADDR'] : '';
}

switch ($route) {
    case 'host':
        shield_json(array('configured' => shield_key_configured(), 'host' => $isHost));
        break;

    case 'info':
        // Пустой список: страницы возьмут адрес, с которого их открыли (это и есть адрес хостинга).
        shield_with_state(function (&$st) { shield_json(array('boot' => $st['boot'], 'urls' => array())); });
        break;

    case 'gate':
        if ($method === 'POST') { shield_json(array('ok' => true)); break; }
        shield_with_state(function (&$st) {
            $r = run_object($st, '');
            $names = array();
            foreach ($r['runners'] as $x) $names[] = $x['name'];
            shield_json(array('boot' => $st['boot'], 'ageMs' => 0, 'gate' => array(
                'phase' => $r['phase'], 'remainingMs' => $r['remainingMs'], 'overMs' => $r['overMs'],
                'runners' => $names, 'visits' => $r['visits'], 'violations' => $r['violations'])));
        });
        break;

    case 'event':
        need_post($method);
        shield_with_state(function (&$st) use ($body) {
            add_event($st, is_array($body) ? $body : array());
            shield_json(array('ok' => true));
        });
        break;

    case 'events':
        $since = isset($_GET['since']) ? (int) $_GET['since'] : 0;
        shield_with_state(function (&$st) use ($since) {
            $list = array();
            foreach ($st['events'] as $e) if ($e['id'] > $since) $list[] = $e;
            shield_json(array('boot' => $st['boot'], 'events' => $list));
        });
        break;

    case 'setup':
        if ($method === 'POST') {
            need_host($isHost);
            if (!is_array($body)) { shield_text(400, 'Bad JSON'); break; }
            shield_with_state(function (&$st) use ($body) {
                $n = shield_stage_count();
                if (isset($body['stages']) && is_array($body['stages'])) {
                    $ids = array();
                    foreach ($body['stages'] as $x) { $x = (int) $x; if ($x >= 1 && $x <= $n) $ids[$x] = $x; }
                    if ($ids) { sort($ids); $st['setup']['stages'] = array_values($ids); }
                }
                if (isset($body['settings']) && is_array($body['settings'])) {
                    foreach (shield_default_settings() as $k => $def) {
                        if (!isset($body['settings'][$k]) || $body['settings'][$k] === '') continue;
                        $v = $body['settings'][$k];
                        $st['setup']['settings'][$k] = is_int($def) ? (int) $v : clean_name($v);
                    }
                }
                shield_json(array('ok' => true));
            });
        } else {
            shield_with_state(function (&$st) {
                shield_json(array('stages' => $st['setup']['stages'], 'settings' => $st['setup']['settings']));
            });
        }
        break;

    case 'state':
        if ($method === 'POST') {
            need_host($isHost);
            if ($body === null) { shield_text(400, 'Bad JSON'); break; }
            shield_with_state(function (&$st) use ($rawBody) {
                $st['stateJson'] = $rawBody;
                $st['stateAt'] = shield_now();
                shield_json(array('ok' => true));
            });
        } else {
            shield_with_state(function (&$st) {
                $age = $st['stateAt'] ? shield_now() - $st['stateAt'] : -1;
                shield_json('{"boot":"' . $st['boot'] . '","ageMs":' . $age . ',"state":' . $st['stateJson'] . '}');
            });
        }
        break;

    case 'terminal':
        need_host($isHost);
        shield_with_state(function (&$st) { shield_json(terminal_info($st)); });
        break;

    case 'terminal/claim':
        // Сделать этот компьютер главным экраном (в том числе забрать его у другого компьютера).
        need_post($method);
        need_host($isHost);
        if (!is_array($body) || !isset($body['id']) || !valid_terminal_id($body['id'])) {
            shield_text(400, 'Нужен id терминала');
            break;
        }
        shield_with_state(function (&$st) use ($body) {
            $t = &terminal_of($st);
            $t['owner'] = $body['id'];
            $t['addr'] = client_addr();
            $t['seen'] = shield_now();
            unset($t);
            shield_json(terminal_info($st));
        });
        break;

    case 'terminal/save':
        need_post($method);
        need_host($isHost);
        if (!is_array($body) || !isset($body['id']) || !valid_terminal_id($body['id'])
            || !isset($body['game']) || !is_array($body['game']) || strlen($rawBody) > 1048576) {
            shield_text(400, 'Bad JSON');
            break;
        }
        shield_with_state(function (&$st) use ($body) {
            $t = &terminal_of($st);
            // Главный экран уже на другом компьютере — этот должен остановиться.
            if ($t['owner'] !== '' && $t['owner'] !== $body['id']) {
                http_response_code(409);
                shield_json(array('moved' => true, 'addr' => $t['addr']));
                return;
            }
            if ($t['owner'] === '') $t['addr'] = client_addr();
            $t['owner'] = $body['id'];
            $t['seen'] = shield_now();
            // Пустой массив PHP закодировал бы как [], а не {} — объект игры храним как объект.
            $t['game'] = json_encode((object) $body['game'], JSON_UNESCAPED_UNICODE);
            $t['clientNow'] = isset($body['now']) ? (int) $body['now'] : shield_now();
            $t['savedAt'] = shield_now();
            shield_json(array('ok' => true));
        });
        break;

    case 'terminal/release':
        // Терминал закрыли: главный экран свободен, следующий компьютер займёт его без вопроса.
        need_post($method);
        if ($isHost && is_array($body) && isset($body['id'])) {
            shield_with_state(function (&$st) use ($body) {
                $t = &terminal_of($st);
                if ($t['owner'] !== '' && $t['owner'] === (string) $body['id']) $t['seen'] = 0;
            });
        }
        shield_json(array('ok' => true));
        break;

    case 'join':
        need_post($method);
        $name = is_array($body) && isset($body['name']) ? clean_name($body['name']) : '';
        if ($name === '') { shield_text(400, 'Нужно имя'); break; }
        $id = is_array($body) && isset($body['id']) && preg_match('/^[a-z0-9]{8}$/', (string) $body['id'])
            ? (string) $body['id'] : bin2hex(random_bytes(4));
        shield_with_state(function (&$st) use ($id, $name) {
            if (isset($st['players'][$id])) {
                $st['players'][$id]['name'] = $name;
                $st['players'][$id]['seen'] = shield_now();
            } else {
                $st['players'][$id] = array('name' => $name, 'seen' => shield_now(), 'runs' => 0);
            }
            shield_json(array('id' => $id, 'name' => $name));
        });
        break;

    case 'run':
        $pid = isset($_GET['id']) && preg_match('/^[a-z0-9]{8}$/', (string) $_GET['id']) ? (string) $_GET['id'] : '';
        $capTok = isset($_GET['captain']) ? (string) $_GET['captain'] : '';
        shield_with_state(function (&$st) use ($pid, $capTok) {
            if ($pid !== '' && isset($st['players'][$pid])) $st['players'][$pid]['seen'] = shield_now();
            if ($capTok !== '' && $st['captain']['token'] !== '' && hash_equals($st['captain']['token'], $capTok)) {
                $st['captain']['seen'] = shield_now();
            }
            shield_json(run_object($st, $pid));
        });
        break;

    case 'captain/claim':
        need_post($method);
        shield_with_state(function (&$st) use ($body) {
            if ($st['captain']['token'] !== '' && !is_captain($st, $body)) {
                shield_text(409, 'Капитан уже назначен');
                return;
            }
            if ($st['captain']['token'] === '') $st['captain']['token'] = bin2hex(random_bytes(6));
            if (is_array($body) && isset($body['name']) && clean_name($body['name']) !== '') {
                $st['captain']['name'] = clean_name($body['name']);
            }
            $st['captain']['seen'] = shield_now();
            shield_json(array('token' => $st['captain']['token'], 'name' => $st['captain']['name']));
        });
        break;

    case 'captain/reset':
        need_host($isHost);
        shield_with_state(function (&$st) {
            $st['captain'] = array('token' => '', 'name' => '', 'seen' => 0);
            shield_json(array('ok' => true));
        });
        break;

    case 'run/send':
        shield_with_state(function (&$st) use ($body) {
            if (!is_captain($st, $body)) { shield_text(403, 'Только капитан'); return; }
            update_run($st);
            if ($st['run']['phase'] !== 'ready') { shield_text(409, 'Матрица сейчас закрыта'); return; }
            $cfg = matrix_cfg($st);
            $ids = array();
            $wanted = isset($body['runners']) && is_array($body['runners']) ? $body['runners'] : array();
            foreach ($wanted as $x) { $x = (string) $x; if (isset($st['players'][$x])) $ids[$x] = $x; }
            $ids = array_values($ids);
            if (count($ids) < 1 || count($ids) > $cfg['runners']) {
                shield_text(400, 'Выберите от 1 до ' . $cfg['runners'] . ' игроков');
                return;
            }
            $st['run']['phase'] = 'assigned';
            $st['run']['since'] = shield_now();
            $st['run']['runners'] = $ids;
            shield_json(run_object($st, ''));
        });
        break;

    case 'run/cancel':
        shield_with_state(function (&$st) use ($body) {
            if (!is_captain($st, $body)) { shield_text(403, 'Только капитан'); return; }
            if ($st['run']['phase'] === 'assigned') {
                $st['run']['phase'] = 'ready';
                $st['run']['since'] = shield_now();
                $st['run']['runners'] = array();
            }
            shield_json(run_object($st, ''));
        });
        break;

    case 'run/enter':
    case 'run/exit':
        shield_with_state(function (&$st) use ($body, $route) {
            $pid = is_array($body) && isset($body['id']) ? (string) $body['id'] : '';
            $allowed = is_captain($st, $body) || ($pid !== '' && in_array($pid, $st['run']['runners'], true));
            if (!$allowed) { shield_text(403, 'Только бегуны или капитан'); return; }
            update_run($st);
            $now = shield_now();
            $run = &$st['run'];
            if ($route === 'run/enter') {
                if ($run['phase'] === 'assigned') {
                    $run['phase'] = 'inside';
                    $run['since'] = $now;
                    $run['visits']++;
                    $run['lastOverMs'] = 0;
                    foreach ($run['runners'] as $id) {
                        if (isset($st['players'][$id])) $st['players'][$id]['runs']++;
                    }
                }
            } else {
                if ($run['phase'] === 'inside') {
                    $run['phase'] = 'cooldown';
                    $run['since'] = $now;
                } elseif ($run['phase'] === 'overtime') {
                    $over = $now - $run['since'];
                    $run['violations']++;
                    $run['lastOverMs'] = $over;
                    $run['phase'] = 'cooldown';
                    $run['since'] = $now;
                    add_event($st, array('type' => 'violation', 'overSeconds' => (int) ceil($over / 1000),
                        'visits' => $run['visits']));
                }
            }
            unset($run);
            shield_json(run_object($st, $pid));
        });
        break;

    case 'run/open':
        need_host($isHost);
        shield_with_state(function (&$st) {
            $st['run']['phase'] = 'ready';
            $st['run']['since'] = shield_now();
            $st['run']['runners'] = array();
            shield_json(array('ok' => true));
        });
        break;

    case 'players/reset':
        need_host($isHost);
        shield_with_state(function (&$st) {
            $st['players'] = array();
            $st['captain'] = array('token' => '', 'name' => '', 'seen' => 0);
            $st['run'] = shield_fresh_run();
            shield_json(array('ok' => true));
        });
        break;

    case 'qr':
        need_host($isHost);
        $codes = shield_codes();
        asort($codes);
        $items = array();
        foreach ($codes as $code => $folder) $items[] = array('code' => $code, 'folder' => $folder);
        shield_json(array('urls' => array(), 'items' => $items));
        break;

    default:
        shield_text(404, 'Unknown API');
}
