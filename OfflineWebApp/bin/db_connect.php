<?php
//======================================================================================== 
// DB configuration variables and DB functions
//
//    (c) KaiserWare, Volker Petersen, January 2016
//======================================================================================== 

$localhostlist = array('127.0.0.1', '::1', 'localhost');
if( in_array( $_SERVER['REMOTE_ADDR'], $localhostlist) ) {
    // connection used if DB access thru localhost
    define("DB_HOST", 'www.southmetrochorale.org');
} else {
    // connection used if DB access thru internet hosting site 
    define("DB_HOST", 'localhost');             // host
}

define("HTTP", ($_SERVER["SERVER_NAME"] == "localhost")
   ? "../"
   : "http://www.southmetrochorale.org/"
);

// Define email addresses, DB logon info, and DB table constants
define("DOMAIN", 'http://www.southmetrochorale.org');
define("ADMIN", str_rot13("nqzva@fbhguzrgebpubenyr.bet"));  // obfuscate email address - ADMIN
define("WEBMASTER", str_rot13('nqzva@fbhguzrgebpubenyr.bet'));   // obfuscate email address - webmaster
define("VOLKER", str_rot13('ibyxre.crgrefra01@tznvy.pbz')); // obfuscate email address - Volker
define("EMAILHOST", "mail.southmetrochorale.org");          // email server host at Hostmonster
define("EMAILUSER", ADMIN);                            		// email user name
define("EMAILPWD", "smc_admin");                              // email pwd for WEBMASTER at Hostmonster
define("DB_DATABASE", 'southme1_offline_wind_records');     // Name of the database
define("DB_USER", 'southme1_sudo');                         // DB user
define("DB_PASSWORD", 'Vesret7713');                        // DB pw
define("WIND", 'wind');                                     // Wind entry Table name
define("RACES", 'races');                                   // Race Table name

function warning_handler($errno, $errstr) {   // warning handler for the mysqli DB connection
    throw new Exception('MySQL DB Warning: ' . $errstr);
}

// Connecting to database
function connect() {
    // connecting to mysqli
    try {
        //set_error_handler("warning_handler", E_WARNING);  // to also catch warnings, not just errors    
        $mysqli = new mysqli(DB_HOST, DB_USER, DB_PASSWORD, DB_DATABASE);
        if (mysqli_connect_errno()) {
            //printf("Connect failed: %s\n", mysqli_connect_error());
            return FALSE;
        }
    } 
    catch (Exception $e) {
        $msg = "<p>Website failed to connect to the MySQL database on the host site: ".DB_HOST.".<br />";
        $msg.= "[error: " . $e->getMessage() . "].<br />Aborting the website display.";
        $msg.= "<br />Please contact <a href='".VOLKER."?Subject=";
        $msg.= "Offline Wind Website Failure: ".$e->getMessage()."' target='_top'>Volker</a> for support.</p>";
        die($msg);
    }
    // return database handler
    return $mysqli;
}

//======================================================================================== 
//  function to write data to the console.log
//======================================================================================== 
function console($data) {
    if(is_array($data) || is_object($data)) {
        echo("<script>console.log('PHP: ".json_encode($data)."');</script>");
    } else {
        echo("<script>console.log('PHP: ".$data."');</script>");
    }
}

//======================================================================================== 
//  esc_url
//  function to sanitize (escape) the output from the PHP_SELF server variable
//======================================================================================== 
function esc_url($url) {
 
    if ('' == $url) {
        return $url;
    }
 
    $url = preg_replace('|[^a-z0-9-~+_.?#=!&;,/:%@$\|*\'()\\x80-\\xff]|i', '', $url);
 
    $strip = array('%0d', '%0a', '%0D', '%0A');
    $url = (string) $url;
 
    $count = 1;
    while ($count) {
        $url = str_replace($strip, '', $url, $count);
    }
 
    $url = str_replace(';//', '://', $url);
 
    $url = htmlentities($url);
 
    $url = str_replace('&amp;', '&#038;', $url);
    $url = str_replace("'", '&#039;', $url);
 
    if ($url[0] !== '/') {
        // We're only interested in relative links from $_SERVER['PHP_SELF']
        return '';
    } else {
        return $url;
    }
}

//======================================================================================== 
//  headingDelta($from, $to)
//  function to compute the difference in degrees between $from and $to ranging
//  between -180.0 - 0.0 - +180.0
//======================================================================================== 
function headingDelta($from, $to) {
    if ($from > 360.0 or $from < 0.0) {
        $from = ($from % 360.0);
    }
    if ($to > 360.0 or $to < 0.0) {
        $to = ($to % 360.0);
    }

    $diff = $to - $from;
    $absDiff = abs($diff);
    if ($absDiff <= 180.0) {
        if ($absDiff == 180.0) $diff = $absDiff;
        return round((float)$diff, 1);
    } elseif ($to > $from) {
        return round((float)($absDiff - 360.0), 1);
    } else {
        return round((float)(360.0 - $absDiff), 1);
    }
}

//======================================================================================== 
//  fetch_wind()
//  function to fetch the MYSQL data from the DB table 'wind'
//======================================================================================== 
function fetch_wind($race, $windFlag) {
    $mysqli = connect();
    if ($race == "") {
        $sql_query = "SELECT * FROM ".WIND." ORDER BY Date ASC LIMIT 5000;";
        //echo('alert("no race_id");');
    } else {
        $sql_query = "SELECT * FROM ".WIND." WHERE RACE_ID='".$race."' ORDER BY Date ASC LIMIT 5000;";
    }
    $query = $mysqli->query($sql_query);
    if (!$query) {
        $msg = " Web page call aborted: DB query error : " . $sql_query . " | " . $mysqli->error . " | " . $mysqli->host_info;
        die($msg);  // display error msg and abort page display
    }

    $avgTWDx = 0.0;
    $avgTWDy = 0.0;
    $avgAWDx = 0.0;
    $avgAWDy = 0.0;
    $ctr = 0;
    $rows = [];
    // compute the average TWD and AWD
    while($row =$query->fetch_assoc()) {
        $avgTWDx = $avgTWDx + cos(deg2rad($row['TWD']));
        $avgTWDy = $avgTWDy + sin(deg2rad($row['TWD']));
        $avgAWDx = $avgAWDx + cos(deg2rad($row['AWD']));
        $avgAWDy = $avgAWDy + sin(deg2rad($row['AWD']));
        $ctr += 1;
        array_push($rows, $row);
    }
    if ($ctr > 1) {
        $avgTWDx = $avgTWDx / (1.0*$ctr);
        $avgTWDy = $avgTWDy / (1.0*$ctr);
        $avgAWDx = $avgAWDx / (1.0*$ctr);
        $avgAWDy = $avgAWDy / (1.0*$ctr);
    }

    if ( (abs($avgTWDx) < 0.0000001) and (abs($avgTWDy) < 0.0000001) ) {
        $avgTWD = 0.0;
    } else {
        $avgTWD = (360.0+rad2deg(atan2($avgTWDy, $avgTWDx))) % 360.0;
    }

    if ( (abs($avgAWDx) < 0.0000001) and (abs($avgAWDy) < 0.0000001) ) {
        $avgAWD = 0.0;
    } else {
        $avgAWD = (360.0+rad2deg(atan2($avgAWDy, $avgAWDx))) % 360.0;
    }


    if ($windFlag == "js") {
        $array = "var wind = new Array();\r";
        $array.= "avgAWDall = ".$avgAWD.";\r";
        $array.= "avgTWDall = ".$avgTWD.";\r";
    }
    if ($windFlag == "chart") {
        $array = "js_wind = new Array();\r";
        $array.= "avgAWDall = ".$avgAWD.";\r";
        $array.= "avgTWDall = ".$avgTWD.";\r";
    }
    if ($windFlag == "php" or $windFlag == "json") {
        $array = array(
            "avgAWDall" => $avgAWD,
            "avgTWDall" => $avgTWD,
            "wind"     => array()
        );
    }    
    $zero = 0.0;
    //$query = $mysqli->query($sql_query);
    //while($row = $query->fetch_assoc()) {
    foreach ($rows as $row) {
        $deltaTWD = headingDelta($avgTWD, (float)$row["TWD"]);
        $deltaAWD = headingDelta($avgAWD, (float)$row["AWD"]);
        if ($windFlag == "js") {
            $d = "'".date("Y-m-d H:i:s", strtotime($row["Date"]))."', '".number_format($deltaTWD, 0)."', '";
            $d.= number_format($row["TWS"], 1)."', '".$row["Quadrant"]."', '".$row["Race_ID"]."'";
            $array .= "wind.push([" . $d ."]);\r";
        } 
        if ($windFlag == "php") {
            $d = array((float)date(strtotime($row["Date"])), (float)$deltaTWD, (float)$row["TWS"], (float)$row["Quadrant"], (int)$row["Race_ID"]);
            array_push($array["wind"], $d);
        }
        if ($windFlag == "chart") {
            $raw = (float)$row['TWD'];
            //console("avg: ".$avgTWD."  TWD:".$raw." delta: ".$deltaTWD);
            $AWS = round((float)$row["AWS"], 1);
            $TWS = round((float)$row["TWS"], 1);
            // javascript global variable "js_wind" is defined in "offline_header.php"
            if ($row["Quadrant"] == 1) {
                $array .= "js_wind.push(['".$row["Date"]."', ".$deltaTWD.", ".$zero.", ".$zero.", ".$zero.", ".$deltaTWD.", ".$TWS.", ".$deltaAWD.", ".$AWS."]);\r";
            }
            if ($row["Quadrant"] == 2) {
                $array .= "js_wind.push(['".$row["Date"]."', ".$zero.", ".$deltaTWD.", ".$zero.", ".$zero.", ".$deltaTWD.", ".$TWS.", ".$deltaAWD.", ".$AWS."]);\r";
            }
            if ($row["Quadrant"] == 3) {
                $array .= "js_wind.push(['".$row["Date"]."', ".$zero.", ".$zero.", ".$deltaTWD.", ".$zero.", ".$deltaTWD.", ".$TWS.", ".$deltaAWD.", ".$AWS."]);\r";
            }
            if ($row["Quadrant"] == 4) {
                $array .= "js_wind.push(['".$row["Date"]."', ".$zero.", ".$zero.", ".$zero.", ".$deltaTWD.", ".$deltaTWD.", ".$TWS.", ".$deltaAWD.", ".$AWS."]);\r";
            }
        }
        if ($windFlag == "json") {
            if ($row["Quadrant"] == 1) {
                $d = array((string)$row["Date"], $deltaTWD, $zero, $zero, $zero, $deltaTWD);
            }
            if ($row["Quadrant"] == 2) {
                $d = array((string)$row["Date"], $zero, $deltaTWD, $zero, $zero, $deltaTWD);
            }
            if ($row["Quadrant"] == 3) {
                $d = array((string)$row["Date"], $zero, $zero, $deltaTWD, $zero, $deltaTWD);
            }
            if ($row["Quadrant"] == 4) {
                $d = array((string)$row["Date"], $zero, $zero, $zero, $deltaTWD, $deltaTWD);
            }
            array_push($array["wind"], $d);
        }
    }
    return $array;
}

//======================================================================================== 
//  fetch_races()
//  function to fetch the MYSQL data from the DB table 'race'
//======================================================================================== 
function fetch_races() {
    $mysqli = connect();
    $sql_query = "SELECT * FROM ".RACES." ORDER BY RACE_ID DESC;";
    $query = $mysqli->query($sql_query);
    if (!$query) {
        $msg = " Web page call aborted: DB query error : " . $sql_query . " | " . $mysqli->error . " | " . $mysqli->host_info;
        die($msg);  // display error msg and abort page display
    }

    $race_js = "var race = new Array();\r";
    $ctr = 0;
    while($row = $query->fetch_assoc()) {
        if ($ctr == 0) {
            $GLOBALS['$race_id'] = $row["Race_ID"];
        }
        $d = "'".$row["Name"]."', '".number_format($row["avgTWS"], 1)."', '".number_format($row["avgTWD"], 0)."', '";
        $d.= number_format($row["WWD_LAT"], 4)."', '".number_format($row["WWD_LON"], 4)."', '".number_format($row["LWD_LAT"], 4);
        $d.= "', '".number_format($row["LWD_LON"], 4)."', '".$row["Race_ID"]."'";
        $race_js .= "race.push([" . $d ."]);\r";
        $ctr += 1;
    }
    //console("in fetch_races() race id: ".$GLOBALS['$race_id']);
    return $race_js;
}
?>