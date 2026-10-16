<?php
// Отдаёт js/config.js: ведущему (верный ключ в cookie) — целиком, с ответами;
// всем остальным — только настройки таймера Матрицы. Запросы сюда направляет .htaccess.
require __DIR__ . '/common.php';

shield_headers('application/javascript; charset=utf-8');
header('Vary: Cookie');
if (shield_is_host()) {
    readfile(SHIELD_CONFIG);
} else {
    echo shield_public_config();
}
