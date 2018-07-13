<?php
require 'bin/db_connect.php';
require "bin/class.smtp.php";
require "bin/class.phpmailer.php";

//======================================================================================== 
// Storing new wind record and returns TRUE/FALSE
//======================================================================================== 
function storeWindRecord($db, $date, $awd, $aws, $twd, $tws, $sog, $lat, $lon, $quadrant, $status, $race_id) {
    // Insert wind into database

    $cc="','";

    $sql_insert = "REPLACE INTO wind (Date, AWD, AWS, TWD, TWS, SOG, LAT, LON, Quadrant, Status, Race_ID)";
    $sql_insert .= " VALUES ('".$date.$cc.$awd.$cc.$aws.$cc.$twd.$cc.$tws.$cc.$sog.$cc.$lat.$cc.$lon.$cc.$quadrant.$cc.$status.$cc.$race_id."')";

    //console($sql_insert);
	
    return $db->query($sql_insert);

}

//======================================================================================== 
// Storing new Wind FIFO Queue record and returns TRUE/FALSE
//======================================================================================== 
function storeFIFORecord($db, $date, $twd, $smoothedTWD, $avgSmoothedTWD, $cog, $sog, $tack) {
    // Insert wind into database

    $sql_insert = "INSERT INTO windFIFO (Date, TWD, smoothedTWD, avgSmoothedTWD, COG, SOG, tack)";
    $sql_insert .= " VALUES ('".$date."','".$twd."', '".$smoothedTWD."','".$avgSmoothedTWD."','".$cog."','".$sog."','".$tack."');";
	//console($sql_insert);

    return $db->query($sql_insert);

}

//======================================================================================== 
// Getting all wind records
//======================================================================================== 
function getAllWindRecords($db) {
    $result = $db->query("select * FROM wind");
    return $result;
}

function removeslashes($string) {
    $string=implode("",explode("\\",$string));
    return stripslashes(trim($string));
}


//======================================================================================== 
// email debug info
//======================================================================================== 
function email_debug_info($message) {
	if(is_array($message) || is_object($message)) {
		$message = json_encode($message);
	}
	$mail = new PHPMailer(TRUE);	// initiate phpMailer with the TRUE param means to throw exceptions on errors
	$mail->IsSMTP();				// telling the class to use SMTP
	$mail->isHTML(TRUE);
	$mail->Port     = 25;
	$mail->SMTPAuth = TRUE; // enable SMTP authentication
	$mail->Host     = EMAILHOST;
	$mail->Username = WEBMASTER;
	$mail->Password = EMAILPWD;
	$mail->From     = VOLKER;
	$mail->AddAddress(VOLKER);
	$mail->FromName = "Sailing Race App";
	$mail->Subject  = "Sailing Race App - SQLite sync debug info";
	$mail->Body     = $message;
	if ($mail->Send()) {
		$ret = "Email sent";
	} else {
		$ret = "Email was not sent due to error: ". $mail->ErrorInfo;
	}
}


//======================================================================================== 
// Save all wind records
//======================================================================================== 
function save_all_wind_records($mysqli, $json) {
	//console("save_all_wind_records() mysqli ". ($mysqli !== FALSE) );
	if ($mysqli !== FALSE) {
		//Remove Slashes
		$json = removeslashes($json);

		//Decode JSON into an Array
		$data = json_decode($json, TRUE);

		//Utility array to create response JSON
		$a=array();
		$errors = 0;
		$flag = TRUE;
		$a["results"] = array();
		$a["results"]["status"] = "PHP error";
		$a["results"]["post"] = array();

		// Loop through the JSON array and insert data read from JSON into the web server MySQL DB
		// the dictionary keys are defined in the Android App Class sqlWindDataHelper.java in the class WindEntry
		foreach ($data as $key => $record) {
			//Store Wind record in the MySQL DB
			$date_str = date("Y-m-d H:i:s", (float)$key);
			$res = storeWindRecord($mysqli, $date_str, $record['AWD'], $record['AWS'], $record['TWD'], $record['TWS'], $record['SOG'], $record['LAT'], $record['LON'], $record['Quadrant'], 1, $record['Race_ID']);
			//if MySL write fails, add that record ID $key to the JSON return response array
			//console($res);
			if($res === FALSE) {
    			array_push($a["results"]["post"], $key);
				$errors = $errors + 1;
				$flag = FALSE;
			}
		}
		$mysqli->close();
		$a["results"]["status"] = "success";
		$a["results"]["error"] = $errors;
	} else {
		$a["results"]["status"] = "DB error";
		$a["results"]["error"] = -1;
		$flag = FALSE;
	}

	// for debugging purposes
	if ($flag === FALSE) {
		if ($a["results"]["error"] > 0) {
			email_debug_info("Error writing ".$a["results"]["error"]." records and size of [post]=".sizeof($a["results"]["post"]));
		} elseif ($a["results"]["error"] == -1) {
			email_debug_info("DB connection problem and size of [post]=".sizeof($a["results"]["post"]));
		}
	}

	//Post JSON response back to Android Application
	return $a;
}

//======================================================================================== 
// Save a single race record
//======================================================================================== 
function save_race_record($mysqli, $json) {
	//Util array to create response JSON
	$a=array();
	$a["results"] = array();
	$a["results"]["status"] = "PHP error";
	$a["results"]["post"] = array();
	if ($mysqli !== FALSE) {
		//Remove Slashes
		$json = removeslashes($json);

		//Decode JSON into an Array
		$race = json_decode($json, TRUE);

		//Store the Race record in the MySQL DB
	    $sql_insert = "REPLACE INTO races (Race_ID, WWD_LAT, WWD_LON, LWD_LAT, LWD_LON, CTR_LAT, CTR_LON, avgTWS, avgTWD, Name)";
	    $sql_insert .= " VALUES ('".$race['Race_ID']."','".$race['WWD_LAT']."','".$race['WWD_LON']."','".$race['LWD_LAT'];
	    $sql_insert .= "','".$race['LWD_LON']."','".$race['CTR_LAT']."','".$race['CTR_LON']."','".$race['avgTWS'];
	    $sql_insert .= "','".$race['avgTWD']."','".$race['Name']."')";
		
		if($mysqli->query($sql_insert) === TRUE) {
			$a["results"]['status'] = 'success';
			$a["results"]['post'] = 'Inserted 1 race into the MySQL DB (Offline-RACE) using statement='.$sql_insert;
			$a["results"]["error"] = 0;
		} else {
			$a["results"]['status'] = 'Insert error';
			$a["results"]['post'] = "MySQL DB statement=".$sql_insert." failed with error=".$mysqli->error;
			$a["results"]["error"] = -1;
		}

		$mysqli->close();
	} else {
		$a["results"]["status"] = "DB error";
		$a["results"]["post"] = 'Could not connect to the MYSQL database Offline-RACE.';
		$a["results"]["error"] = -1;
	}

	if ($a["results"]['status'] != 'success') {
		email_debug_info($a);
	}
	return $a;
}

//======================================================================================== 
// Save the FIFO Queue wind data
//======================================================================================== 
function save_fifo_queue($mysqli, $json) {
	$flag = TRUE;
	if ($mysqli !== FALSE) {
		//Remove Slashes
		$json = removeslashes($json);

		//Delete all existing rows from the table windFIFO
		$delete_all = "TRUNCATE TABLE windFIFO";
	    $mysqli->query($delete_all);

		//Decode JSON into an Array
		$data = json_decode($json, TRUE);
		//console("json in function:");
		//console($data);

		//Utility array to create response JSON
		$a=array();
		$a["results"] = array();
		$a["results"]["status"] = "PHP error";
		$a["results"]["post"] = array();
        $flag = TRUE;

		// Loop through the JSON array and insert data read from JSON into the web server MySQL DB
		// the dictionary keys are defined in the Android App Class sqlWindDataHelper.java in the class windFIFO
		foreach ($data as $key => $record) {
			//Store WindFIFO record in the MySQL DB
			$date_str = date("Y-m-d H:i:s", (float)$key);
			//console("date=".$date_str." TWD=".$record['TWD']." smoothTWD=".$record['smoothedTWD']." avgTWD=".$record['avgSmoothedTWD']." COG=".$record['COG']." SOG=".$record['SOG']." Tack=".$record['tack']);
			$res = storeFIFORecord($mysqli, $date_str, $record['TWD'], $record['smoothedTWD'], $record['avgSmoothedTWD'], $record['COG'], $record['SOG'], $record['tack']);
			//if MySL write fails, add that record ID $key) to the JSON return response array
			if($res === FALSE) {
    			array_push($a["results"]["post"], $key);
				$a["results"]["error"] = 'Error writting record(s) to the MYSQL database.';
				$a["results"]["status"] = "DB error";
    			$flag = FALSE;
			}
		}
		$mysqli->close();
		if ($flag) {
			$a["results"]["status"] = "success";
		}
		//Post JSON response back to Android Application
	} else {
		$a["results"]["status"] = "DB error";
		$a["results"]["error"] = 'Could not connect to the MYSQL database Offline-FIFO.';
		$flag = FALSE;
	}

	// for debugging purposes
	if ($flag === FALSE) {
		email_debug_info($a["results"]["error"]);
	}

	return $a;
}

//======================================================================================== 
// end of functions definitions.
//======================================================================================== 


// connecting to the web server MYSQL database
$mysqli = connect();

// Test Data ==========================================
//
$windJSON = '{"1451695820": {"AWD":"66","AWS":"9", "TWD":"65","TWS":"8", "SOG":"5.1","LAT":"44.950","LON":"-93.550","Quadrant":"4","Status":"0", "Race_ID": "1452125800"},';
$windJSON.= ' "1451695821": {"AWD":"71","AWS":"12","TWD":"70","TWS":"11","SOG":"5.2","LAT":"44.948","LON":"-93.547","Quadrant":"4","Status":"0", "Race_ID": "1452125800"},';
$windJSON.= ' "1451695822": {"AWD":"76","AWS":"15","TWD":"75","TWS":"14","SOG":"5.3","LAT":"44.946","LON":"-93.544","Quadrant":"4","Status":"0", "Race_ID": "1452125800"},';
$windJSON.= ' "1451695823": {"AWD":"81","AWS":"18","TWD":"80","TWS":"17","SOG":"5.4","LAT":"44.944","LON":"-93.541","Quadrant":"4","Status":"0", "Race_ID": "1452125800"}}';

$raceJSON='{"Race_ID":1452124800,"avgTWD":74.66666666666667,"avgTWS":17,"WWD_LAT":55.656684999999996,"WWD_LON":-93.34395599999999,"LWD_LAT":55.63155644,"LWD_LON":-93.36927579,"CTR_LAT":44.6315571392648,"CTR_LON":-93.38193568500002,"Name":"2016-01-06 00"}';

$fifoJSON='{"1468974227": {"TWD":"0","smoothedTWD":"287.577","avgSmoothedTWD":"289.944","COG":"0","SOG":"0", "tack": "Stbd"}, 
	"1468975222": {"TWD":"10","smoothedTWD":"297.577","avgSmoothedTWD":"299.944","COG":"10","SOG":"5", "tack": "Stbd"}}';

//$_POST["raceJSON"] = $raceJSON;
//console("raceJSON POST:");
//console($_POST["raceJSON"]);

//$_POST["windJSON"] = $windJSON;
//console("windJSON POST:");
//console($_POST["windJSON"]);

//$_POST["fifoJSON"] = $fifoJSON;
//console("fifoJSON POST:");
//console(json_encode($_POST["fifoJSON"], TRUE));

// END OF TEST DATA=======================================

if (isset($_POST["windJSON"])) {
	$response = save_all_wind_records($mysqli, $_POST["windJSON"]);
} elseif (isset($_POST["raceJSON"])) {
	$response = save_race_record($mysqli, $_POST["raceJSON"]);
} elseif (isset($_POST["fifoJSON"])) {
	$response = save_fifo_queue($mysqli, $_POST["fifoJSON"]);
} else {
	$response = array();
	$response["status"] = "error";
	$response["post"] = "This script requires a URL post with keys 'windJSON', 'raceJSON', or 'fifo_JSON'. ";
	$response["post"].= "Post=".implode(";", $_POST)."<<< end of data >>>";
	//var_dump($response);
}

//Post JSON response back to Android Application
echo json_encode($response);
?>