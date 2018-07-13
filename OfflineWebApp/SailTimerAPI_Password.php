<!DOCTYPE html>
<?php
$url = 'https://www.sailtimermaps.com/getHash.php';
$data = array('user' => 'Volker', 'password' => 'K\\u8L27CLe<S:R7Q');

// use key 'http' even if you send the request to https://...
$options = array(
    'http' => array(
        'header'  => "Content-type: application/x-www-form-urlencoded\r\n",
        'method'  => 'POST',
        'content' => http_build_query($data)
    )
);
$context  = stream_context_create($options);
$result = file_get_contents($url, false, $context);
if ($result === FALSE) {
	var_dump("Failure: ".$result);
} else {
	var_dump("Success: ".$result);
}

?>
