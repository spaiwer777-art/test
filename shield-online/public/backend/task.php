<?php
// Задание по QR-коду фигурки: /q/<код>/... -> zadaniya/<папка>/... (код из zadaniya/kody.txt).
// Папка zadaniya закрыта от прямого доступа, поэтому задание открывается только по коду с наклейки.
require __DIR__ . '/common.php';

$code = isset($_GET['code']) ? strtoupper((string) $_GET['code']) : '';
$file = isset($_GET['file']) ? (string) $_GET['file'] : '';
if ($file === '') $file = 'ZADANIE.html';

$codes = shield_codes();
if (!preg_match('/^[A-Z0-9]+$/', $code) || !isset($codes[$code])) {
    shield_text(404, '404: задание не найдено');
    exit;
}
$base = realpath(SHIELD_QUESTS . '/' . $codes[$code]);
$full = $base ? realpath($base . '/' . $file) : false;
if (!$base || !$full || strpos($full, $base . DIRECTORY_SEPARATOR) !== 0 || !is_file($full)) {
    shield_text(404, '404: файл не найден');
    exit;
}
$types = array(
    'html' => 'text/html; charset=utf-8', 'css' => 'text/css; charset=utf-8',
    'js' => 'application/javascript; charset=utf-8', 'jpg' => 'image/jpeg', 'jpeg' => 'image/jpeg',
    'png' => 'image/png', 'svg' => 'image/svg+xml', 'woff2' => 'font/woff2', 'woff' => 'font/woff',
);
$ext = strtolower(pathinfo($full, PATHINFO_EXTENSION));
if (!isset($types[$ext])) {
    shield_text(404, '404');
    exit;
}
shield_headers($types[$ext]);
header('Content-Length: ' . filesize($full));
readfile($full);
